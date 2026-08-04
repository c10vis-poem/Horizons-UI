package com.horizons.core.voice

import android.content.Context
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream
import java.io.File
import java.io.IOException
import java.util.concurrent.TimeUnit

sealed class KokoroSetupState {
    object Idle : KokoroSetupState()
    data class Downloading(val percent: Int, val totalMb: Float) : KokoroSetupState()
    object Extracting : KokoroSetupState()
    object Ready : KokoroSetupState()
    data class Error(val message: String) : KokoroSetupState()
}

/**
 * Downloads and extracts the Kokoro multi-lang v1.0 model (~200 MB compressed).
 * Exposes [state] for UI progress.  Call [ensureReady] once at app start.
 */
class KokoroModelManager(private val context: Context, private val scope: CoroutineScope) {

    private val _state = MutableStateFlow<KokoroSetupState>(KokoroSetupState.Idle)
    val state: StateFlow<KokoroSetupState> = _state.asStateFlow()

    val modelDir: String
        get() = File(context.filesDir, "sherpa_tts/kokoro-multi-lang-v1_0").absolutePath

    fun ensureReady() {
        val cur = _state.value
        if (cur is KokoroSetupState.Ready || cur is KokoroSetupState.Downloading || cur is KokoroSetupState.Extracting) return
        scope.launch(Dispatchers.IO) { checkOrDownload() }
    }

    private fun isComplete(): Boolean {
        val dir = File(modelDir)
        return dir.isDirectory
            && File(dir, "model.onnx").exists()
            && File(dir, "voices.bin").exists()
            && File(dir, "tokens.txt").exists()
            && File(dir, "espeak-ng-data").isDirectory
    }

    private fun checkOrDownload() {
        if (isComplete()) { _state.value = KokoroSetupState.Ready; return }
        downloadAndExtract()
    }

    private fun downloadAndExtract() {
        val tmpFile = File(context.cacheDir, "kokoro.tar.bz2")
        try {
            val client = OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(10, TimeUnit.MINUTES)
                .build()

            _state.value = KokoroSetupState.Downloading(0, 0f)
            val request = Request.Builder().url(MODEL_URL).build()

            client.newCall(request).execute().use { resp ->
                if (!resp.isSuccessful) throw IOException("HTTP ${resp.code}")
                val body = resp.body ?: throw IOException("empty body")
                val totalBytes = body.contentLength()
                val totalMb = if (totalBytes > 0) totalBytes / (1024f * 1024f) else 0f
                var downloaded = 0L

                tmpFile.outputStream().buffered().use { out ->
                    body.byteStream().use { src ->
                        val buf = ByteArray(65_536)
                        var n: Int
                        while (src.read(buf).also { n = it } != -1) {
                            out.write(buf, 0, n)
                            downloaded += n
                            if (totalBytes > 0) {
                                _state.value = KokoroSetupState.Downloading(
                                    percent = (downloaded * 100L / totalBytes).toInt(),
                                    totalMb = totalMb,
                                )
                            }
                        }
                    }
                }
            }

            _state.value = KokoroSetupState.Extracting
            val destDir = File(context.filesDir, "sherpa_tts")
            destDir.mkdirs()
            Log.i(TAG, "Extracting Kokoro model archive…")

            BZip2CompressorInputStream(tmpFile.inputStream().buffered()).use { bz ->
                TarArchiveInputStream(bz).use { tar ->
                    var entry = tar.nextEntry
                    while (entry != null) {
                        val dest = File(destDir, entry.name)
                        if (entry.isDirectory) dest.mkdirs()
                        else { dest.parentFile?.mkdirs(); dest.outputStream().use { tar.copyTo(it) } }
                        entry = tar.nextEntry
                    }
                }
            }
            tmpFile.delete()

            if (isComplete()) {
                Log.i(TAG, "Kokoro model ready at $modelDir")
                _state.value = KokoroSetupState.Ready
            } else {
                // Partial/corrupt extraction — clear it so the NEXT launch re-downloads
                // cleanly instead of retrying isComplete() against leftover debris forever.
                File(context.filesDir, "sherpa_tts").deleteRecursively()
                _state.value = KokoroSetupState.Error("Extraction incomplete — expected files missing")
            }
        } catch (e: Throwable) {
            Log.e(TAG, "Kokoro download/extract failed", e)
            tmpFile.delete()
            // Same reasoning: an interrupted download/extract (network drop, OOM, crash
            // mid-tar) leaves a partial sherpa_tts/ that will never satisfy isComplete(),
            // so every future boot re-downloads 200MB and re-extracts on top of the debris.
            File(context.filesDir, "sherpa_tts").deleteRecursively()
            _state.value = KokoroSetupState.Error(e.message ?: "Unknown error")
        }
    }

    companion object {
        const val TAG = "KokoroModelManager"
        const val MODEL_URL =
            "https://github.com/k2-fsa/sherpa-onnx/releases/download/tts-models/kokoro-multi-lang-v1_0.tar.bz2"
    }
}
