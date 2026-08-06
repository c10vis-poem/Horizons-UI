package com.horizons

import android.app.ActivityManager
import android.app.Application
import android.os.Process
import com.horizons.audio.AudioRecorder
import com.horizons.audio.VadFactory
import com.horizons.audio.VoiceLoopController
import com.horizons.core.llm.CloudLlmRuntime
import com.horizons.core.llm.LlmRuntime
import com.horizons.core.llm.NpuClient
import com.horizons.core.log.CrashRecorder
import com.horizons.core.perf.GameModeBoost
import com.horizons.core.perf.GameModeBoost.gameBoosted
import com.horizons.core.agent.AgentLoop
import com.horizons.core.shell.TaskerBridge
import com.horizons.core.state.AppStateStore
import com.horizons.core.state.ChatHistoryStore
import com.horizons.core.state.ArchiveStore
import com.horizons.core.state.RouterConfigStore
import com.horizons.core.state.RuntimeDefStore
import com.horizons.core.state.SavedCommandStore
import com.horizons.core.stt.DaemonSttClient
import com.horizons.core.voice.KokoroModelManager
import com.horizons.core.voice.KokoroSetupState
import com.horizons.core.voice.SherpaOnnxTtsClient
import com.horizons.provider.SettingsStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.nio.ByteBuffer
import java.nio.ByteOrder

data class ChatMessage(val role: String, val text: String)

/** Three operating modes for continuous vs. one-shot AI interaction. */
enum class ChatMode {
    /** Mode A — Live Screen Share: continuous MediaProjection + audio loop + LLM stream (FGS). */
    A,
    /** Mode B — Live Chat: audio loop + LLM stream, no screen capture (FGS). */
    B,
    /** Mode C — One-shot: manual attach or dock capture; single LLM call. Default. */
    C,
}

/**
 * Application singleton. All AI inference runs on-device via
 * Qwen3.5-9B via ort_engine daemon (Hexagon HTP v79).
 * TTS: Sherpa-ONNX -> Kokoro multi-lang v1.0 (28 English voices), no Android TTS broker.
 */
class HorizonsApplication : Application() {

    val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    lateinit var appState: AppStateStore
        private set

    val settingsStore: SettingsStore by lazy { SettingsStore(this) }
    val chatHistory: ChatHistoryStore by lazy { ChatHistoryStore(this) }
    val savedCommands: SavedCommandStore by lazy { SavedCommandStore(this) }
    val routerConfigs: RouterConfigStore by lazy { RouterConfigStore(this) }
    val archive: ArchiveStore by lazy { ArchiveStore(this) }
    val runtimeDefs: RuntimeDefStore by lazy { RuntimeDefStore(this) }
    val tasker: TaskerBridge by lazy { TaskerBridge(this) }

    // -- Agentic loop -- LLM + full Android API tool registry --
    // Uses a lambda provider so swapping llmRuntime is transparent.
    val agentLoop: AgentLoop by lazy { AgentLoop(this, { llmRuntime }, tasker, appState) }

    private val _engineError = MutableStateFlow<String?>(null)
    val engineError: StateFlow<String?> = _engineError.asStateFlow()

    // -- Kokoro model resolver -- finds the TTS model in a device folder. Never downloads. --
    val kokoroManager: KokoroModelManager by lazy { KokoroModelManager(this, appState) }

    // -- Sherpa-ONNX TTS (Kokoro voices, no Android TextToSpeech broker) --
    val tts: SherpaOnnxTtsClient by lazy { SherpaOnnxTtsClient(kokoroManager.modelDir) }

    // -- Persisted voice settings exposed for RouterPane --
    val ttsVoiceId: MutableStateFlow<String> by lazy {
        MutableStateFlow(appState.get(AppStateStore.KEY_TTS_VOICE) ?: SherpaOnnxTtsClient.DEFAULT_VOICE)
    }
    val ttsSpeed: MutableStateFlow<Float> by lazy {
        MutableStateFlow(appState.get(AppStateStore.KEY_TTS_SPEED)?.toFloatOrNull() ?: 1.0f)
    }

    // -- Shared AudioRecorder --
    val audioRecorder: AudioRecorder by lazy { AudioRecorder(this) }

    // -- STT via the media daemon (Whisper) -- never in-process; models run detached --
    val stt: DaemonSttClient by lazy { DaemonSttClient(appState) }

    // -- Pending screen capture (dock button -> stored here -> chat ask) --
    val pendingScreenJpeg = MutableStateFlow<ByteArray?>(null)

    // -- LLM runtimes -- daemon first, cloud fallback --
    @Volatile private var _npuClient: NpuClient? = null
    val cloudRuntime: CloudLlmRuntime by lazy { CloudLlmRuntime(appState) }

    val llmRuntime: LlmRuntime get() {
        _npuClient?.let { return it }
        if (cloudRuntime.isConfigured) return cloudRuntime
        return _fallbackRuntime
    }

    val isNpuActive: Boolean get() = _npuClient != null

    private val _fallbackRuntime = object : LlmRuntime {
        override val backendStatus = MutableStateFlow("Adreno 830 · no backend")
        override fun stream(prompt: String) = flow<String> {
            emit("[No inference backend available — start the on-device daemon or add a cloud API key in Settings]")
        }
    }

    // -- Chat mode (A/B/C) -- updated by FGS services on start/stop --
    val chatMode = MutableStateFlow(ChatMode.C)

    // -- Voice loop (Mode B) --
    val voiceLoop: VoiceLoopController by lazy {
        VoiceLoopController(
            scope = scope,
            recorder = audioRecorder,
            tts = tts,
            engineStreamAudio = { pcm ->
                // VAD → media-daemon STT (Whisper) → LLM (text reasoning) → TTS.
                // STT runs in the daemon, not here; the LLM only ever sees text.
                flow {
                    val text = transcribeAudio(pcm, AudioRecorder.SAMPLE_RATE)
                    if (text.isNotBlank()) emitAll(llmRuntime.stream(text))
                }.gameBoosted(this@HorizonsApplication)
            },
            vad = VadFactory.create(this@HorizonsApplication),
        )
    }

    // -- Model test (Diagnostics tab) --
    val modelTestResult = MutableStateFlow<String?>(null)

    fun testModel() {
        if (_chatBusy.value) return
        modelTestResult.value = "testing..."
        scope.launch {
            var out = ""
            llmRuntime.stream("Say hi in one word.").collect { out += it }
            modelTestResult.value = "backend: ${llmRuntime.backendStatus.value}\n\n" +
                out.take(300).ifBlank { "(empty response)" }
        }
    }

    // -- Chat state --
    private val _chatMessages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val chatMessages: StateFlow<List<ChatMessage>> = _chatMessages.asStateFlow()

    private val _chatBusy = MutableStateFlow(false)
    val chatBusy: StateFlow<Boolean> = _chatBusy.asStateFlow()

    private var _activeSessionId: String = java.util.UUID.randomUUID().toString()
    val activeSessionId: String get() = _activeSessionId
    private var _activeSessionMode: String = "standard"

    fun newChatSession() {
        saveCurrentSession()
        _chatMessages.value = emptyList()
        _activeSessionId = java.util.UUID.randomUUID().toString()
        _activeSessionMode = "standard"
    }

    fun loadSession(sessionId: String) {
        saveCurrentSession()
        val session = chatHistory.getSession(sessionId) ?: return
        _chatMessages.value = session.messages
        _activeSessionId = session.id
        _activeSessionMode = session.mode
    }

    fun saveCurrentSession() {
        val msgs = _chatMessages.value
        if (msgs.isEmpty()) return
        scope.launch {
            chatHistory.save(
                com.horizons.core.state.ChatSession(
                    id = _activeSessionId,
                    messages = msgs,
                    mode = _activeSessionMode,
                )
            )
        }
    }

    fun setSessionMode(mode: String) { _activeSessionMode = mode }

    fun sendChat(prompt: String) {
        if (_chatBusy.value) return
        _chatMessages.value = _chatMessages.value + ChatMessage("user", prompt)
        _chatBusy.value = true
        GameModeBoost.enterHotLoop(this)
        scope.launch {
            val assistantIdx = _chatMessages.value.size
            _chatMessages.value = _chatMessages.value + ChatMessage("assistant", "")
            var reply = ""
            var lastNanos = System.nanoTime()
            try {
                llmRuntime.stream(prompt).collect { chunk ->
                    val now = System.nanoTime()
                    GameModeBoost.reportWork(now - lastNanos)
                    lastNanos = now
                    reply += chunk
                    val list = _chatMessages.value.toMutableList()
                    list[assistantIdx] = ChatMessage("assistant", reply)
                    _chatMessages.value = list
                }
            } finally {
                _chatBusy.value = false
                GameModeBoost.exitHotLoop(this@HorizonsApplication)
                saveCurrentSession()
            }
        }
    }

    fun stopAll() {
        tts.stop()
        voiceLoop.stop()
        _chatBusy.value = false
    }

    /** Screen Q&A: JPEG -> Qwen3.5-9B vision (ort_engine) -> streaming answer in chat. */
    fun screenAsk(jpegBytes: ByteArray, question: String) {
        if (_chatBusy.value) return
        val q = question.ifBlank { "What is on this screen?" }
        _chatMessages.value = _chatMessages.value + ChatMessage("user", "[Screen] $q")
        _chatBusy.value = true
        GameModeBoost.enterHotLoop(this)
        scope.launch {
            val assistantIdx = _chatMessages.value.size
            _chatMessages.value = _chatMessages.value + ChatMessage("assistant", "")
            var reply = ""
            try {
                llmRuntime.streamImage(jpegBytes, q).collect { chunk ->
                    reply += chunk
                    val list = _chatMessages.value.toMutableList()
                    list[assistantIdx] = ChatMessage("assistant", reply)
                    _chatMessages.value = list
                }
                if (reply.isNotBlank()) scope.launch { tts.speak(reply.stripForTts()) }
            } finally {
                _chatBusy.value = false
                GameModeBoost.exitHotLoop(this@HorizonsApplication)
                saveCurrentSession()
            }
        }
    }

    /**
     * Voice STT: PCM -> media daemon (Whisper), never in-process, model-independent.
     * Falls back to the active LLM's audio path only if the media daemon is down
     * and a model that accepts audio is loaded.
     */
    suspend fun transcribeAudio(pcm: ShortArray, sampleRate: Int): String {
        // Media daemon (Whisper) first; returns "" if the daemon isn't reachable.
        val text = stt.transcribe(pcm, sampleRate)
        if (text.isNotBlank()) return text
        // Fallback only if the media daemon is down and an audio-capable LLM is active.
        val wav = pcmToWav(pcm, sampleRate)
        var result = ""
        llmRuntime.streamAudio(wav, "Transcribe the audio accurately.").collect { result += it }
        return result.trim()
    }

    private fun isMainProcess(): Boolean {
        val pid = Process.myPid()
        val am = getSystemService(ACTIVITY_SERVICE) as? ActivityManager ?: return true
        return am.runningAppProcesses?.firstOrNull { it.pid == pid }
            ?.processName == packageName
    }

    override fun onCreate() {
        com.horizons.core.diag.Breadcrumb.install(this)
        com.horizons.core.diag.Breadcrumb.drop("onCreate_enter")
        super.onCreate()
        com.horizons.core.diag.Breadcrumb.drop("onCreate_after_super")

        if (!isMainProcess()) {
            com.horizons.core.diag.Breadcrumb.drop("onCreate_skip_non_main_process")
            appState = AppStateStore(this)
            return
        }

        try {
            CrashRecorder(this).install()
            com.horizons.core.diag.Breadcrumb.drop("crashrecorder_installed")

            // Consolidate crashes + logged errors into one adb-pullable failure
            // report at externalFilesDir/failures/ (see FailureMonitor / FAILURES.md).
            com.horizons.core.diag.FailureMonitor.install(this)
            com.horizons.core.diag.Breadcrumb.drop("failuremonitor_installed")

            appState = AppStateStore(this)
            com.horizons.core.diag.Breadcrumb.drop("appstate_loaded")

            // CLIFFORD FGS -- separate process. Failure here shouldn't kill main.
            try {
                com.horizons.fgs.CliffordService.start(this)
                com.horizons.core.diag.Breadcrumb.drop("clifford_started")
            } catch (e: Throwable) {
                com.horizons.core.diag.Breadcrumb.drop("clifford_failed: ${e.javaClass.simpleName}: ${e.message}")
            }

            try {
                cloudRuntime.refreshStatus()
                com.horizons.core.diag.Breadcrumb.drop("cloud_refreshed")
            } catch (e: Throwable) {
                com.horizons.core.diag.Breadcrumb.drop("cloud_refresh_failed: ${e.javaClass.simpleName}: ${e.message}")
            }

            // -- TTS: resolve Kokoro from a device folder, never download. --
            //
            // This used to call kokoroManager.ensureReady(), which pulled ~200 MB
            // from GitHub, bzip2-decompressed it, untarred it, and only then let
            // tts.init() load the ONNX in-process. All of that ran on every boot
            // of an app whose core law is "boots empty, boots stable", and it is a
            // prime suspect for the unexplained ~90 s first crash.
            //
            // Now: a filesystem check on the IO dispatcher. If the model is there
            // the TTS engine initialises; if it is not, the app boots clean and
            // says which files are missing. No model is the normal boot state.
            scope.launch(Dispatchers.IO) {
                val state = runCatching { kokoroManager.refresh() }.getOrElse { e ->
                    com.horizons.core.diag.Breadcrumb.drop("kokoro_refresh_failed: ${e.javaClass.simpleName}: ${e.message}")
                    return@launch
                }
                when (state) {
                    is KokoroSetupState.Ready -> {
                        com.horizons.core.diag.Breadcrumb.drop("kokoro_resolved")
                        try {
                            tts.voiceId = ttsVoiceId.value
                            tts.speed   = ttsSpeed.value
                            tts.init()
                            com.horizons.core.diag.Breadcrumb.drop("tts_inited")
                        } catch (e: Throwable) {
                            com.horizons.core.diag.Breadcrumb.drop("tts_init_failed: ${e.javaClass.simpleName}: ${e.message}")
                        }
                    }
                    is KokoroSetupState.Missing ->
                        com.horizons.core.diag.Breadcrumb.drop("kokoro_absent: missing=${state.missing.joinToString(",")}")
                    KokoroSetupState.Idle -> Unit
                }
            }

            // -- STT: probe the media daemon so stt.ready reflects connectivity --
            scope.launch { runCatching { stt.probe() } }

            scope.launch { ttsVoiceId.collect { id -> tts.voiceId = id; appState.put(AppStateStore.KEY_TTS_VOICE, id) } }
            scope.launch { ttsSpeed.collect  { sp -> tts.speed   = sp; appState.put(AppStateStore.KEY_TTS_SPEED, sp.toString()) } }

            com.horizons.core.diag.Breadcrumb.drop("onCreate_exit_ok")
        } catch (e: Throwable) {
            com.horizons.core.diag.Breadcrumb.drop("onCreate_threw: ${e.javaClass.simpleName}: ${e.message}")
            throw e
        }
    }

    fun activateNpuRuntime() {
        if (_npuClient == null) _npuClient = NpuClient()
    }

    /**
     * Path of the model the user has EXPLICITLY plugged in (KEY_ACTIVE_MODEL),
     * or null. Pin-only by canon: "daemons stay dumb, the user is the loader."
     * A landed file is acknowledged in the Monitor library but NEVER auto-grabbed
     * — there is deliberately no auto-detection fallback here. This feeds the
     * launcher (CliffordService) and greenLight, so auto-detecting a model would
     * auto-launch a daemon at boot, which is the regression this removes. The
     * Monitor's plug-in picker lists available files via its own scan, not this.
     */
    fun resolveNpuModelPath(): String? {
        val pinned = appState.get(com.horizons.core.state.AppStateStore.KEY_ACTIVE_MODEL) ?: return null
        val f = java.io.File(pinned)
        return if (f.canRead()) f.absolutePath else null
    }

    companion object {
        private const val TAG = "HorizonsApp"

        private fun String.stripForTts(): String =
            replace(Regex("[*_`#~\\[\\]|>]+"), "")
                .replace(Regex("\\n{3,}"), "\n\n")
                .trim()

        fun pcmToWav(pcm: ShortArray, sampleRate: Int): ByteArray {
            val dataBytes = pcm.size * 2
            return ByteBuffer.allocate(44 + dataBytes).order(ByteOrder.LITTLE_ENDIAN).apply {
                put("RIFF".toByteArray()); putInt(36 + dataBytes)
                put("WAVE".toByteArray())
                put("fmt ".toByteArray()); putInt(16)
                putShort(1); putShort(1)
                putInt(sampleRate); putInt(sampleRate * 2)
                putShort(2); putShort(16)
                put("data".toByteArray()); putInt(dataBytes)
                for (s in pcm) putShort(s)
            }.array()
        }
    }
}
