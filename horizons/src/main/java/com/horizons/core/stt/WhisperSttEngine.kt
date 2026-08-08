package com.horizons.core.stt

import android.content.Context
import android.util.Log
import com.k2fsa.sherpa.onnx.FeatureConfig
import com.k2fsa.sherpa.onnx.OfflineModelConfig
import com.k2fsa.sherpa.onnx.OfflineRecognizer
import com.k2fsa.sherpa.onnx.OfflineRecognizerConfig
import com.k2fsa.sherpa.onnx.OfflineWhisperModelConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

/**
 * STT in-process, on the sherpa-onnx AAR the app already ships for Kokoro TTS —
 * Whisper base.en, not Moonshine. The operator's standing model choice (Whisper:
 * near-identical size to Moonshine tiny, materially better accuracy) predates
 * this app's in-process STT rewrite; [MoonshineSttEngine] picked Moonshine when
 * it replaced the dead media-daemon hop and never carried the earlier decision
 * forward. This class replaces it — same [SttEngine] contract, same in-process
 * rationale, correct model.
 *
 * Bundled straight into the APK's assets at [ASSET_DIR], fetched at CI build
 * time from the operator's own fork (`Mer0vin8ian/sherpa-onnx-whisper-base.en`,
 * your-fork-first) — mirrors [com.horizons.core.voice.KokoroModelManager]:
 * no device-folder import step, nothing for the user to place by hand.
 */
class WhisperSttEngine(private val context: Context) : SttEngine {

    private val _ready = MutableStateFlow(false)
    override val ready: StateFlow<Boolean> = _ready.asStateFlow()

    private val _status = MutableStateFlow("STT · Whisper (not loaded)")
    override val status: StateFlow<String> = _status.asStateFlow()

    @Volatile private var recognizer: OfflineRecognizer? = null

    /** Cheap `AssetManager.list()` check — mirrors KokoroModelManager.refresh(). */
    private fun missingAssets(): List<String> {
        val listed = runCatching { context.assets.list(ASSET_DIR)?.toSet() }.getOrNull() ?: emptySet()
        return REQUIRED.filterNot { it in listed }
    }

    override fun init() {
        if (recognizer != null) return
        val missing = missingAssets()
        if (missing.isNotEmpty()) {
            _status.value = "STT · Whisper (missing from APK assets: ${missing.joinToString(", ")})"
            _ready.value = false
            return
        }
        try {
            val config = OfflineRecognizerConfig(
                featConfig = FeatureConfig(sampleRate = 16000, featureDim = 80),
                modelConfig = OfflineModelConfig(
                    whisper = OfflineWhisperModelConfig(
                        encoder = "$ASSET_DIR/$ENCODER",
                        decoder = "$ASSET_DIR/$DECODER",
                        language = "en",
                        task = "transcribe",
                    ),
                    tokens = "$ASSET_DIR/$TOKENS",
                    modelType = "whisper",
                    numThreads = 2,
                    debug = false,
                ),
            )
            recognizer = OfflineRecognizer(assetManager = context.assets, config = config)
            _ready.value = true
            _status.value = "STT · Whisper base.en (in-process)"
            Log.i(TAG, "Whisper recognizer ready from assets/$ASSET_DIR")
        } catch (e: Throwable) {
            recognizer = null
            _ready.value = false
            _status.value = "STT · Whisper failed: ${e.message}"
            Log.e(TAG, "Whisper init failed", e)
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
                Log.e(TAG, "Whisper transcribe failed", e)
                ""
            }
        }

    override fun close() {
        recognizer?.release()
        recognizer = null
        _ready.value = false
        _status.value = "STT · Whisper (stopped)"
    }

    companion object {
        const val TAG = "WhisperStt"
        const val ASSET_DIR = "sherpa_stt/whisper-base-en"
        const val ENCODER = "base.en-encoder.int8.onnx"
        const val DECODER = "base.en-decoder.int8.onnx"
        const val TOKENS = "base.en-tokens.txt"
        val REQUIRED = listOf(ENCODER, DECODER, TOKENS)
    }
}
