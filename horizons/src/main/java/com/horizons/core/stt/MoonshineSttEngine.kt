package com.horizons.core.stt

import android.content.Context
import android.util.Log
import com.horizons.core.state.AppStateStore
import com.k2fsa.sherpa.onnx.FeatureConfig
import com.k2fsa.sherpa.onnx.OfflineModelConfig
import com.k2fsa.sherpa.onnx.OfflineMoonshineModelConfig
import com.k2fsa.sherpa.onnx.OfflineRecognizer
import com.k2fsa.sherpa.onnx.OfflineRecognizerConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.File

/**
 * STT in-process, on the sherpa-onnx AAR the app already ships for Kokoro TTS.
 *
 * This replaces the hop through a media daemon on 127.0.0.1:8091 that nothing in
 * the app ever bound — see DaemonSttClient. With no listener there, every
 * transcribe() returned "" and the voice loop silently fell back to the LLM's own
 * audio path, which reads like a model problem but isn't one.
 *
 * The LLM-runs-as-a-daemon rule is scoped to the LLM path. The voice layer runs
 * in-process by design: Kokoro TTS already does, and Moonshine small is a ~190 MB
 * CPU model, not a multi-GB NPU graph.
 *
 * ## Model files
 *
 * Nothing is downloaded. The files come from wherever the user put them — a
 * HuggingFace copy, a GitHub fork, or a directory already on the device — matching
 * the same "user is the loader" rule the Router follows. [modelDir] is read from
 * AppStateStore under [KEY_MOONSHINE_DIR]; if unset, the candidate directories
 * below are searched in order, which mirrors greenLight()'s asset lookup.
 *
 * A Moonshine directory holds four ONNX graphs plus tokens.txt:
 *   preprocess · encode · uncached_decode · cached_decode · tokens.txt
 *
 * Each graph resolves as <part>.onnx or <part>.int8.onnx, because the int8
 * bundles (csukuangfj/sherpa-onnx-moonshine-base-en-int8) ship encode.int8.onnx
 * while only preprocess.onnx and tokens.txt keep a single spelling.
 *
 * Note this is NOT the Optimum/transformers.js ONNX export, which ships
 * encoder_model.onnx / decoder_model.onnx / tokenizer.json — that layout has no
 * preprocess graph and a tokenizer JSON rather than tokens.txt, so it cannot be
 * renamed into place.
 *
 * If they aren't all present the engine stays not-ready and reports which are
 * missing, rather than failing at transcribe() time with an empty string.
 */
class MoonshineSttEngine(
    private val context: Context,
    private val appState: AppStateStore,
) : SttEngine {

    private val _ready = MutableStateFlow(false)
    override val ready: StateFlow<Boolean> = _ready.asStateFlow()

    private val _status = MutableStateFlow("STT · Moonshine (no model)")
    override val status: StateFlow<String> = _status.asStateFlow()

    @Volatile private var recognizer: OfflineRecognizer? = null

    /** Directories searched when the user hasn't pinned one explicitly. */
    private fun candidateDirs(): List<File> = listOf(
        File(context.filesDir, "sherpa_stt/moonshine"),
        File(context.filesDir, "moonshine"),
        File("/storage/emulated/0/Download/moonshine"),
        File("/storage/emulated/0/Download"),
    )

    /** The directory holding all five Moonshine files, or null if none qualifies. */
    fun resolveModelDir(): File? {
        appState.get(KEY_MOONSHINE_DIR)?.takeIf { it.isNotBlank() }?.let { pinned ->
            val dir = File(pinned)
            return if (missingFiles(dir).isEmpty()) dir else null
        }
        return candidateDirs().firstOrNull { missingFiles(it).isEmpty() }
    }

    /**
     * Resolve one model part. The int8 bundles ship encode.int8.onnx while the
     * float ones ship encode.onnx, and only preprocess/tokens have a single
     * spelling — so match on either rather than a fixed filename.
     */
    private fun partFile(dir: File, part: String): File? =
        listOf("$part.onnx", "$part.int8.onnx", "$part.fp16.onnx")
            .map { File(dir, it) }
            .firstOrNull { it.canRead() }

    /** Logical parts with no readable file, by name. Empty means loadable. */
    private fun missingFiles(dir: File): List<String> {
        if (!dir.isDirectory) return REQUIRED_PARTS + TOKENS_FILE
        val missing = REQUIRED_PARTS.filter { partFile(dir, it) == null }.toMutableList()
        if (!File(dir, TOKENS_FILE).canRead()) missing += TOKENS_FILE
        return missing
    }

    override fun init() {
        if (recognizer != null) return
        val dir = resolveModelDir()
        if (dir == null) {
            val probe = candidateDirs().first()
            _status.value = "STT · Moonshine (missing: ${missingFiles(probe).joinToString(", ")})"
            _ready.value = false
            return
        }
        try {
            val config = OfflineRecognizerConfig(
                featConfig = FeatureConfig(sampleRate = 16000, featureDim = 80),
                modelConfig = OfflineModelConfig(
                    moonshine = OfflineMoonshineModelConfig(
                        preprocessor = partFile(dir, "preprocess")!!.absolutePath,
                        encoder = partFile(dir, "encode")!!.absolutePath,
                        uncachedDecoder = partFile(dir, "uncached_decode")!!.absolutePath,
                        cachedDecoder = partFile(dir, "cached_decode")!!.absolutePath,
                    ),
                    tokens = File(dir, TOKENS_FILE).absolutePath,
                    modelType = "moonshine",
                    numThreads = 2,
                    debug = false,
                ),
            )
            // assetManager stays null: every path above is an absolute file path on
            // disk, not an entry inside the APK's assets.
            recognizer = OfflineRecognizer(assetManager = null, config = config)
            _ready.value = true
            _status.value = "STT · Moonshine small (in-process)"
            Log.i(TAG, "Moonshine recognizer ready from ${dir.absolutePath}")
        } catch (e: Throwable) {
            recognizer = null
            _ready.value = false
            _status.value = "STT · Moonshine failed: ${e.message}"
            Log.e(TAG, "Moonshine init failed", e)
        }
    }

    override suspend fun transcribe(pcm: ShortArray, sampleRate: Int): String =
        withContext(Dispatchers.IO) {
            if (pcm.isEmpty()) return@withContext ""
            if (recognizer == null) init()
            val rec = recognizer ?: return@withContext ""

            try {
                // sherpa wants float samples in [-1, 1]; AudioRecord gives 16-bit PCM.
                val samples = FloatArray(pcm.size) { pcm[it] / 32768.0f }
                val stream = rec.createStream()
                try {
                    stream.acceptWaveform(samples, sampleRate)
                    rec.decode(stream)
                    rec.getResult(stream).text.trim()
                } finally {
                    stream.release()
                }
            } catch (e: Throwable) {
                Log.e(TAG, "Moonshine transcribe failed", e)
                ""
            }
        }

    override fun close() {
        recognizer?.release()
        recognizer = null
        _ready.value = false
        _status.value = "STT · Moonshine (stopped)"
    }

    companion object {
        const val TAG = "MoonshineStt"

        /** AppStateStore key holding a user-pinned Moonshine model directory. */
        const val KEY_MOONSHINE_DIR = "stt.moonshine.dir"

        /**
         * Model parts, by logical name. Each resolves to <part>.onnx or
         * <part>.int8.onnx — the int8 bundles (sherpa-onnx-moonshine-*-int8)
         * carry the suffix on everything except preprocess.
         */
        val REQUIRED_PARTS = listOf(
            "preprocess",
            "encode",
            "uncached_decode",
            "cached_decode",
        )

        const val TOKENS_FILE = "tokens.txt"
    }
}
