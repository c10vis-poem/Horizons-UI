package com.horizons.core.voice

import android.content.res.AssetManager
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.util.Log
import com.k2fsa.sherpa.onnx.OfflineTts
import com.k2fsa.sherpa.onnx.OfflineTtsConfig
import com.k2fsa.sherpa.onnx.OfflineTtsKokoroModelConfig
import com.k2fsa.sherpa.onnx.OfflineTtsModelConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

/**
 * On-device TTS via Sherpa-ONNX → Kokoro v0.19 (English), bundled as an APK
 * asset and loaded straight from [AssetManager] — no filesystem copy step.
 * Replaces the Android TextToSpeech broker (SystemTtsClient).
 * call init() once the model directory is ready (KokoroModelManager.state == Ready).
 */
class SherpaOnnxTtsClient(
    private val assetManager: AssetManager,
    private val modelDir: String,
) {

    @Volatile private var engine: OfflineTts? = null
    @Volatile private var audioTrack: AudioTrack? = null
    @Volatile private var stopRequested = false

    var voiceId: String = DEFAULT_VOICE
    var speed: Float = 1.0f

    fun init() {
        val config = OfflineTtsConfig(
            model = OfflineTtsModelConfig(
                kokoro = OfflineTtsKokoroModelConfig(
                    model  = "$modelDir/model.onnx",
                    voices = "$modelDir/voices.bin",
                    tokens = "$modelDir/tokens.txt",
                    dataDir = "$modelDir/espeak-ng-data",
                    lang = "en-us",
                ),
                numThreads = 2,
                debug = false,
            ),
        )
        engine = OfflineTts(assetManager = assetManager, config = config)
        Log.i(TAG, "Sherpa-ONNX TTS ready — ${engine?.numSpeakers()} voices @ ${engine?.sampleRate()}Hz")
    }

    suspend fun speak(text: String) = withContext(Dispatchers.IO) {
        val tts = engine ?: run {
            Log.w(TAG, "speak() called before init() — TTS model not ready yet")
            return@withContext
        }
        if (text.isBlank()) return@withContext
        stopRequested = false

        try {
            val sid = ENGLISH_VOICES.indexOfFirst { it.id == voiceId }.coerceAtLeast(0)
            val generated = tts.generate(text = text, sid = sid, speed = speed)
            if (stopRequested || generated.samples.isEmpty()) return@withContext

            val pcm = ShortArray(generated.samples.size) { i ->
                (generated.samples[i].coerceIn(-1f, 1f) * 32767f).toInt().toShort()
            }
            val sr = generated.sampleRate

            val track = AudioTrack(
                AudioManager.STREAM_MUSIC, sr,
                AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT,
                pcm.size * 2, AudioTrack.MODE_STATIC,
            )
            audioTrack = track
            track.write(pcm, 0, pcm.size)
            track.play()

            val endMs = System.currentTimeMillis() + pcm.size.toLong() * 1000L / sr + 300L
            while (!stopRequested && System.currentTimeMillis() < endMs) delay(40L)

            cleanupTrack()
        } catch (e: CancellationException) {
            cleanupTrack(); throw e
        } catch (e: Throwable) {
            Log.e(TAG, "TTS speak error", e)
            cleanupTrack()
        }
    }

    fun stop() {
        stopRequested = true
        cleanupTrack()
    }

    fun shutdown() {
        stop()
        engine?.release()
        engine = null
    }

    private fun cleanupTrack() {
        val t = audioTrack ?: return
        audioTrack = null
        runCatching { t.pause(); t.flush(); t.stop(); t.release() }
    }

    data class KokoroVoice(val id: String, val label: String)

    companion object {
        const val TAG = "SherpaOnnxTts"
        const val DEFAULT_VOICE = "af_heart"

        // English voices in kokoro-multi-lang-v1_0.  Order == SID (0-indexed).
        val ENGLISH_VOICES = listOf(
            KokoroVoice("af_alloy",    "American Female · Alloy"),
            KokoroVoice("af_aoede",    "American Female · Aoede"),
            KokoroVoice("af_bella",    "American Female · Bella"),
            KokoroVoice("af_heart",    "American Female · Heart"),
            KokoroVoice("af_jessica",  "American Female · Jessica"),
            KokoroVoice("af_kore",     "American Female · Kore"),
            KokoroVoice("af_nicole",   "American Female · Nicole"),
            KokoroVoice("af_nova",     "American Female · Nova"),
            KokoroVoice("af_river",    "American Female · River"),
            KokoroVoice("af_sarah",    "American Female · Sarah"),
            KokoroVoice("af_sky",      "American Female · Sky"),
            KokoroVoice("am_adam",     "American Male · Adam"),
            KokoroVoice("am_echo",     "American Male · Echo"),
            KokoroVoice("am_eric",     "American Male · Eric"),
            KokoroVoice("am_fenrir",   "American Male · Fenrir"),
            KokoroVoice("am_liam",     "American Male · Liam"),
            KokoroVoice("am_michael",  "American Male · Michael"),
            KokoroVoice("am_onyx",     "American Male · Onyx"),
            KokoroVoice("am_puck",     "American Male · Puck"),
            KokoroVoice("am_santa",    "American Male · Santa"),
            KokoroVoice("bf_alice",    "British Female · Alice"),
            KokoroVoice("bf_emma",     "British Female · Emma"),
            KokoroVoice("bf_isabella", "British Female · Isabella"),
            KokoroVoice("bf_lily",     "British Female · Lily"),
            KokoroVoice("bm_daniel",   "British Male · Daniel"),
            KokoroVoice("bm_fable",    "British Male · Fable"),
            KokoroVoice("bm_george",   "British Male · George"),
            KokoroVoice("bm_lewis",    "British Male · Lewis"),
        )
    }
}
