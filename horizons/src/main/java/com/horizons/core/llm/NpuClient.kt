package com.horizons.core.llm

import android.util.Log
import com.horizons.core.shell.DaemonLauncher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.onEach
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

/**
 * LlmRuntime backed by the local ONNX-RT/QNN native engine daemon.
 *
 * The daemon (ort_engine) must be running on 127.0.0.1:DaemonLauncher.ENGINE_PORT.
 * Launch via DaemonLauncher. Install binary via NativeBinaryInstaller.
 *
 * Wire protocol:
 *   POST /api/v1/generate   {"prompt":"…","temperature":0.7,"max_tokens":2048,"stream":true,
 *                             "image_b64":"…"}   ← image_b64 optional, base64 JPEG
 *   → chunked SSE:          data: {"token":"…","index":N}
 *   GET  /health            → 200 when ready
 *
 * Vision lives in THIS same daemon/process as the LLM (session-16 decision — model +
 * vision share one socket; STT/TTS are a separate media daemon, see DaemonSttClient).
 * ort_engine doesn't decode the image yet (VLM path pending GenieX's libgeniex_vlm),
 * but the wire contract already carries it so the client side doesn't need to change
 * again when that lands.
 *
 * Think-token shim: <think>…</think> blocks are collapsed to a single "[Thinking…]" emit.
 * The daemon may suppress them natively in future; this shim is a Kotlin-side safety net.
 */
class NpuClient(
    /** Port of the daemon this client talks to — comes from the plated config's
     *  RuntimeDef, so a config running geniex on :18181 is reached there rather
     *  than at the ort_engine default. */
    private val port: Int = DaemonLauncher.ENGINE_PORT,
    /** Health endpoint path, likewise from the RuntimeDef (ort_engine uses
     *  /health, geniex uses /v1/models). */
    private val healthPath: String = "/health",
) : LlmRuntime {

    private val _backendStatus = MutableStateFlow("Hexagon HTP · NPU (ort_engine daemon)")
    override val backendStatus: StateFlow<String> = _backendStatus.asStateFlow()

    private val _perfMetrics = MutableStateFlow<LlmRuntime.PerfMetrics?>(null)
    override val perfMetrics: StateFlow<LlmRuntime.PerfMetrics?> = _perfMetrics.asStateFlow()

    override fun stream(prompt: String): Flow<String> = timed(rawStream(prompt, null))

    /** Vision request — same daemon/socket as text (see class doc). */
    override fun streamImage(jpeg: ByteArray, prompt: String): Flow<String> {
        val imageB64 = android.util.Base64.encodeToString(jpeg, android.util.Base64.NO_WRAP)
        val q = prompt.ifBlank { "What is on this screen?" }
        return timed(rawStream(q, imageB64))
    }

    private fun timed(source: Flow<String>): Flow<String> {
        val startNanos = System.nanoTime()
        var firstTokenNanos = -1L
        var tokenCount = 0
        return source.onEach {
            tokenCount++
            if (firstTokenNanos < 0) firstTokenNanos = System.nanoTime()
            val elapsedSec = (System.nanoTime() - firstTokenNanos) / 1_000_000_000.0
            _perfMetrics.value = LlmRuntime.PerfMetrics(
                firstTokenMs = (firstTokenNanos - startNanos) / 1_000_000,
                tokensPerSec = if (elapsedSec > 0) tokenCount / elapsedSec else 0.0,
                tokenCount = tokenCount,
            )
        }
    }

    private fun rawStream(prompt: String, imageB64: String?): Flow<String> = flow {
        when (daemonHealthCode()) {
            200 -> { /* ready — fall through to the generate request below */ }
            503 -> {
                // Daemon is up but the model isn't loaded yet (still loading, or the
                // model failed to load). Not a crash — an honest, actionable message.
                emit("[NPU model not ready — the daemon is up but no model is loaded. " +
                     "Import a compiled model, then try again.]")
                return@flow
            }
            else -> {
                emit("[NPU daemon not reachable at 127.0.0.1:$port]")
                return@flow
            }
        }

        val conn = URL("http://127.0.0.1:$port/api/v1/generate")
            .openConnection() as HttpURLConnection
        try {
            conn.requestMethod = "POST"
            conn.doOutput      = true
            conn.setRequestProperty("Content-Type", "application/json")
            conn.setRequestProperty("Accept", "text/event-stream")
            conn.connectTimeout = 5_000
            conn.readTimeout    = 120_000

            val body = JSONObject().apply {
                put("prompt",      prompt)
                put("temperature", 0.7)
                put("max_tokens",  2048)
                put("stream",      true)
                if (imageB64 != null) put("image_b64", imageB64)
            }.toString().toByteArray()
            conn.outputStream.use { it.write(body) }

            var inThink    = false
            var thinkShown = false

            BufferedReader(InputStreamReader(conn.inputStream)).use { reader ->
                var line = reader.readLine()
                while (line != null) {
                    val token = if (line.startsWith("data: ") && line.length > 6) {
                        try { JSONObject(line.substring(6)).optString("token", "") }
                        catch (_: Exception) { line.substring(6) }
                    } else {
                        line = reader.readLine(); continue
                    }

                    if (token.isEmpty()) { line = reader.readLine(); continue }

                    when {
                        !inThink && "<think>" in token -> {
                            val before = token.substringBefore("<think>")
                            if (before.isNotEmpty()) emit(before)
                            inThink    = true
                            thinkShown = false
                        }
                        inThink && "</think>" in token -> {
                            inThink = false
                            val after = token.substringAfter("</think>")
                            if (after.isNotEmpty()) emit(after)
                        }
                        inThink -> {
                            if (!thinkShown) { emit("[Thinking…]"); thinkShown = true }
                        }
                        else -> emit(token)
                    }

                    line = reader.readLine()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "NPU stream error", e)
            emit("\n[NPU stream error: ${e.message}]")
        } finally {
            conn.disconnect()
        }
    }.flowOn(Dispatchers.IO)

    /** /health status: 200 ready · 503 up-but-model-not-ready · -1 unreachable. */
    private fun daemonHealthCode(): Int = try {
        val conn = URL("http://127.0.0.1:$port$healthPath")
            .openConnection() as HttpURLConnection
        conn.connectTimeout = 1_000
        conn.requestMethod  = "GET"
        val code = conn.responseCode
        conn.disconnect()
        code
    } catch (_: Exception) { -1 }

    companion object { private const val TAG = "NpuClient" }
}
