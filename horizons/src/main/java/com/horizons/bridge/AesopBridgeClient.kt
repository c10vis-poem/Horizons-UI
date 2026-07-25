package com.horizons.bridge

import android.util.Log
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.callbackFlow
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONObject
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicLong

/**
 * WebSocket client for `aesopd`, the AESOP bridge daemon in Termux
 * (ws://127.0.0.1:8765). This is the control/event plane between the app
 * and the Termux harness — streamed LLM tokens from either data plane
 * (GGML 8081 / NPU 8080), Termux-side TTS, and plane health.
 *
 * Wire spec: aesop/protocol/bridge-protocol.md (version 1). One JSON object
 * per text frame; requests correlate to replies/streams via "id".
 *
 * Lifecycle: call [connect] from a long-lived owner (CliffordService).
 * Reconnects with exponential backoff (1s → 30s cap) until [shutdown].
 * No UI dependencies — observers consume [events] / [status].
 */
class AesopBridgeClient(
    private val url: String = "ws://127.0.0.1:8765",
) {

    data class PlaneStatus(
        val bridge: Boolean,
        val ggml: Boolean,
        val npu: Boolean,
        val media: Boolean,
    )

    private val client = OkHttpClient.Builder()
        .pingInterval(20, TimeUnit.SECONDS)
        .readTimeout(0, TimeUnit.MILLISECONDS) // WS: no read timeout
        .build()

    private val _connected = MutableStateFlow(false)
    val connected: StateFlow<Boolean> = _connected.asStateFlow()

    private val _planes = MutableStateFlow<PlaneStatus?>(null)
    val planes: StateFlow<PlaneStatus?> = _planes.asStateFlow()

    /** Every inbound frame, parsed. Collect for voice/status/agent events. */
    private val _events = MutableSharedFlow<JSONObject>(extraBufferCapacity = 256)
    val events: SharedFlow<JSONObject> = _events.asSharedFlow()

    private val idCounter = AtomicLong(0)
    private val pending = ConcurrentHashMap<String, (JSONObject) -> Unit>()

    @Volatile private var ws: WebSocket? = null
    @Volatile private var shuttingDown = false
    @Volatile private var backoffMs = 1_000L

    fun connect() {
        if (shuttingDown) return
        val req = Request.Builder().url(url).build()
        client.newWebSocket(req, listener)
    }

    fun shutdown() {
        shuttingDown = true
        ws?.close(1000, "app shutdown")
        ws = null
    }

    private val listener = object : WebSocketListener() {
        override fun onOpen(webSocket: WebSocket, response: Response) {
            Log.i(TAG, "bridge connected: $url")
            ws = webSocket
            backoffMs = 1_000L
            _connected.value = true
            send(JSONObject().put("type", "hello").put("role", "ui"))
            requestStatus()
        }

        override fun onMessage(webSocket: WebSocket, text: String) {
            val msg = try { JSONObject(text) } catch (e: Exception) { return }
            val id = msg.optString("id")
            when (msg.optString("type")) {
                "status" -> msg.optJSONObject("planes")?.let { p ->
                    _planes.value = PlaneStatus(
                        bridge = p.optBoolean("bridge"),
                        ggml = p.optBoolean("ggml"),
                        npu = p.optBoolean("npu"),
                        media = p.optBoolean("media"),
                    )
                }
            }
            if (id.isNotEmpty()) pending[id]?.invoke(msg)
            _events.tryEmit(msg)
        }

        override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
            Log.w(TAG, "bridge lost (${t.message}); retry in ${backoffMs}ms")
            _connected.value = false
            ws = null
            scheduleReconnect()
        }

        override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
            _connected.value = false
            ws = null
            if (!shuttingDown) scheduleReconnect()
        }
    }

    private fun scheduleReconnect() {
        if (shuttingDown) return
        val delay = backoffMs
        backoffMs = (backoffMs * 2).coerceAtMost(30_000L)
        Thread {
            Thread.sleep(delay)
            connect()
        }.start()
    }

    private fun send(obj: JSONObject): Boolean = ws?.send(obj.toString()) == true

    fun requestStatus() {
        send(JSONObject().put("id", nextId()).put("type", "status.get"))
    }

    /** Termux-side Kokoro TTS. The app's own media daemon (8091) is the
     *  primary voice; this path lets the harness speak when that's down. */
    fun speak(text: String) {
        send(JSONObject().put("id", nextId()).put("type", "tts.speak").put("text", text))
    }

    /**
     * Stream a generation through the bridge. backend "ggml" = Gemma 4 12B
     * (Termux llamad, 8081); "npu" = ort_engine (app NPU daemon, 8080).
     * Emits token strings; completes on llm.done; throws on bridge error.
     */
    fun generate(
        prompt: String,
        backend: String = "ggml",
        maxTokens: Int = 1024,
        temperature: Double = 0.7,
    ): Flow<String> = callbackFlow {
        val id = nextId()
        pending[id] = { msg ->
            when (msg.optString("type")) {
                "llm.token" -> trySend(msg.optString("token"))
                "llm.done" -> { pending.remove(id); close() }
                "error" -> {
                    pending.remove(id)
                    close(RuntimeException(msg.optString("error")))
                }
            }
        }
        val sent = send(
            JSONObject()
                .put("id", id).put("type", "llm.generate")
                .put("prompt", prompt).put("backend", backend)
                .put("max_tokens", maxTokens).put("temperature", temperature)
        )
        if (!sent) {
            pending.remove(id)
            close(RuntimeException("bridge not connected"))
        }
        awaitClose { pending.remove(id) }
    }

    private fun nextId(): String = "h${idCounter.incrementAndGet()}"

    companion object {
        private const val TAG = "AesopBridgeClient"
    }
}
