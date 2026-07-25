package com.horizons.core.llm

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

/**
 * LlmRuntime backed by the Termux-side llama.cpp daemon (`llamad`).
 *
 * The GGML plane: Gemma 4 12B IT QAT GGUF served by llama-server at
 * 127.0.0.1:8081, supervised by termux-services (see
 * aesop/deploy/phone/daemons/). Loopback is shared across app sandboxes,
 * so this app talks straight into the Termux daemon — no adb, no root.
 *
 * Port discipline (aesop/protocol/bridge-protocol.md): 8080 is ort_engine
 * (NPU, app-owned — see NpuClient), 8091 is the media daemon, 8081 is THIS
 * runtime's daemon, 8765 is the aesopd WebSocket bridge.
 *
 * Wire protocol: OpenAI-compatible POST /v1/chat/completions with SSE
 * streaming. llama-server applies the GGUF's own chat template server-side —
 * never hand-roll Gemma turn markers here.
 */
class TermuxLlmRuntime(
    private val baseUrl: String = "http://127.0.0.1:8081",
) : LlmRuntime {

    private val _backendStatus = MutableStateFlow("GGML · Termux llamad (not probed)")
    override val backendStatus: StateFlow<String> = _backendStatus.asStateFlow()

    private val _perfMetrics = MutableStateFlow<LlmRuntime.PerfMetrics?>(null)
    override val perfMetrics: StateFlow<LlmRuntime.PerfMetrics?> = _perfMetrics.asStateFlow()

    override fun preWarm() {
        Thread {
            _backendStatus.value = if (healthy()) READY_STATUS
            else "GGML · Termux llamad DOWN — run: bash ~/aesop/deploy/phone/boot.sh"
        }.start()
    }

    private fun healthy(): Boolean = try {
        val conn = URL("$baseUrl/health").openConnection() as HttpURLConnection
        conn.connectTimeout = 2000
        conn.readTimeout = 2000
        val ok = conn.responseCode == 200
        conn.disconnect()
        ok
    } catch (e: Exception) {
        false
    }

    override fun stream(prompt: String): Flow<String> = flow {
        val startNanos = System.nanoTime()
        var firstTokenNanos = -1L
        var tokenCount = 0

        val conn = URL("$baseUrl/v1/chat/completions").openConnection() as HttpURLConnection
        try {
            conn.requestMethod = "POST"
            conn.doOutput = true
            conn.setRequestProperty("Content-Type", "application/json")
            conn.connectTimeout = 5000
            conn.readTimeout = 300_000 // 12B on CPU: patience is a feature

            val body = JSONObject()
                .put("model", "local")
                .put("stream", true)
                .put("temperature", 0.7)
                .put("max_tokens", 1024)
                .put(
                    "messages",
                    org.json.JSONArray().put(
                        JSONObject().put("role", "user").put("content", prompt)
                    )
                )
            conn.outputStream.use { it.write(body.toString().toByteArray()) }

            if (conn.responseCode != 200) {
                _backendStatus.value = "GGML · Termux llamad DOWN — run: bash ~/aesop/deploy/phone/boot.sh"
                emit("[Termux llamad unreachable (HTTP ${conn.responseCode}). Start it: bash ~/aesop/deploy/phone/boot.sh]")
                return@flow
            }
            _backendStatus.value = READY_STATUS

            BufferedReader(InputStreamReader(conn.inputStream)).use { reader ->
                while (true) {
                    val line = reader.readLine() ?: break
                    if (!line.startsWith("data:")) continue
                    val data = line.removePrefix("data:").trim()
                    if (data == "[DONE]") break
                    val token = try {
                        JSONObject(data)
                            .optJSONArray("choices")
                            ?.optJSONObject(0)
                            ?.optJSONObject("delta")
                            ?.optString("content")
                            ?.takeIf { it.isNotEmpty() }
                    } catch (e: Exception) {
                        null
                    }
                    if (token != null) {
                        if (firstTokenNanos < 0) firstTokenNanos = System.nanoTime()
                        tokenCount++
                        emit(token)
                    }
                }
            }

            if (tokenCount > 0 && firstTokenNanos > 0) {
                val totalSec = (System.nanoTime() - firstTokenNanos) / 1e9
                _perfMetrics.value = LlmRuntime.PerfMetrics(
                    firstTokenMs = (firstTokenNanos - startNanos) / 1_000_000,
                    tokensPerSec = if (totalSec > 0) tokenCount / totalSec else 0.0,
                    tokenCount = tokenCount,
                )
            }
        } catch (e: Exception) {
            Log.w(TAG, "stream failed", e)
            _backendStatus.value = "GGML · Termux llamad DOWN — run: bash ~/aesop/deploy/phone/boot.sh"
            emit("[Termux llamad unreachable: ${e.message}. Start it: bash ~/aesop/deploy/phone/boot.sh]")
        } finally {
            conn.disconnect()
        }
    }.flowOn(Dispatchers.IO)

    companion object {
        private const val TAG = "TermuxLlmRuntime"

        // "Adreno 830" prefix = the UI's readiness contract (see LlmRuntime doc).
        // Honest too: the Termux llama.cpp build targets Adreno via OpenCL when
        // available, CPU big-cores otherwise — either way it's the GGML plane.
        private const val READY_STATUS = "Adreno 830 · GGML llama.cpp (Termux · Gemma 4 12B)"
    }
}
