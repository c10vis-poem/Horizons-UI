package com.horizons.core.voice

import android.content.Context
import android.util.Log
import com.horizons.core.state.AppStateStore
import java.io.File

sealed class KokoroSetupState {
    object Idle : KokoroSetupState()
    /** Resolved on disk; every required file is present. */
    object Ready : KokoroSetupState()
    /** A directory was searched but files are absent. [missing] names them. */
    data class Missing(val missing: List<String>) : KokoroSetupState()
}

/**
 * Resolves the Kokoro multi-lang v1.0 TTS model from a device folder. It does not
 * download anything.
 *
 * ## Why this no longer downloads
 *
 * This class used to pull a ~200 MB tar.bz2 from GitHub and extract it, and
 * [ensureReady] was called unconditionally from `HorizonsApplication.onCreate()`.
 * That put a 200 MB network fetch, a bzip2 decompress, and a full tar extract on
 * the boot path of an app whose core law is "boots empty, boots stable" — and
 * `SherpaOnnxTtsClient.init()` then loaded the ONNX in-process straight after.
 * It is the largest single thing the app did at startup and a prime suspect for
 * the unexplained ~90 s first crash.
 *
 * The download also contradicted the residency model: weights live in their own
 * clean device folder and load by absolute path, drag-and-drop swappable, nothing
 * large shipping or fetched by the APK. Storage cost is identical either way —
 * the only thing downloading bought was a boot-time failure mode, plus partial
 * extraction debris that made every subsequent boot re-download 200 MB on top of it.
 *
 * This mirrors [com.horizons.core.stt.MoonshineSttEngine]: the user is the loader,
 * the app resolves what is already there and reports what is not.
 *
 * ## Model files
 *
 * A Kokoro directory holds `model.onnx`, `voices.bin`, `tokens.txt`, and the
 * `espeak-ng-data/` directory. Fetch it once, by hand, from
 * `k2-fsa/sherpa-onnx` releases (`kokoro-multi-lang-v1_0.tar.bz2`) or any copy,
 * and unpack it into one of [candidateDirs] — or pin an explicit path under
 * [KEY_KOKORO_DIR].
 */
class KokoroModelManager(
    private val context: Context,
    private val appState: AppStateStore? = null,
) {

    @Volatile
    private var _state: KokoroSetupState = KokoroSetupState.Idle
    val state: KokoroSetupState get() = _state

    /** Directories searched when the user hasn't pinned one explicitly. */
    private fun candidateDirs(): List<File> = listOf(
        File(context.filesDir, "sherpa_tts/kokoro-multi-lang-v1_0"),
        File(context.filesDir, "kokoro"),
        File("/storage/emulated/0/Download/kokoro-multi-lang-v1_0"),
        File("/storage/emulated/0/Download/kokoro"),
    )

    /** Required entries. `espeak-ng-data` is a directory; the rest are files. */
    private fun missingFiles(dir: File): List<String> {
        if (!dir.isDirectory) return REQUIRED
        return REQUIRED.filterNot { name ->
            val f = File(dir, name)
            if (name == "espeak-ng-data") f.isDirectory else f.isFile
        }
    }

    /** The directory holding a complete Kokoro model, or null if none qualifies. */
    fun resolveModelDir(): File? {
        appState?.get(KEY_KOKORO_DIR)?.takeIf { it.isNotBlank() }?.let { pinned ->
            val dir = File(pinned)
            return if (missingFiles(dir).isEmpty()) dir else null
        }
        return candidateDirs().firstOrNull { missingFiles(it).isEmpty() }
    }

    /**
     * Absolute path handed to `SherpaOnnxTtsClient`. Falls back to the first
     * candidate so construction never fails; [state] is the source of truth for
     * whether anything is actually there.
     */
    val modelDir: String
        get() = (resolveModelDir() ?: candidateDirs().first()).absolutePath

    /**
     * Cheap filesystem check — no network, no extraction. Safe to call at boot,
     * though callers should still keep it off the main thread out of habit.
     * Replaces the old `ensureReady()`, which downloaded.
     */
    fun refresh(): KokoroSetupState {
        val dir = resolveModelDir()
        _state = if (dir != null) {
            Log.i(TAG, "Kokoro model resolved at ${dir.absolutePath}")
            KokoroSetupState.Ready
        } else {
            val probed = appState?.get(KEY_KOKORO_DIR)?.takeIf { it.isNotBlank() }
                ?.let { File(it) } ?: candidateDirs().first()
            val missing = missingFiles(probed)
            Log.i(TAG, "Kokoro model not present; missing in ${probed.absolutePath}: $missing")
            KokoroSetupState.Missing(missing)
        }
        return _state
    }

    companion object {
        const val TAG = "KokoroModelManager"

        /** Pin an explicit Kokoro directory. */
        const val KEY_KOKORO_DIR = "tts.kokoro_dir"

        val REQUIRED = listOf("model.onnx", "voices.bin", "tokens.txt", "espeak-ng-data")

        /**
         * Where to get the model by hand. Recorded, not fetched — the app does not
         * download weights.
         */
        const val MODEL_SOURCE_URL =
            "https://github.com/k2-fsa/sherpa-onnx/releases/download/tts-models/kokoro-multi-lang-v1_0.tar.bz2"
    }
}
