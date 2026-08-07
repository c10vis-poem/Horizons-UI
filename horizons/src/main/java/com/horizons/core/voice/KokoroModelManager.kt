package com.horizons.core.voice

import android.content.Context
import android.util.Log

sealed class KokoroSetupState {
    object Idle : KokoroSetupState()
    /** Bundled asset files are present and readable. */
    object Ready : KokoroSetupState()
    /** The APK was built without the voice pack — a CI/build defect, not a user state. */
    data class Missing(val missing: List<String>) : KokoroSetupState()
}

/**
 * Resolves the Kokoro v0.19 (English) TTS model bundled directly in the APK's
 * assets at `sherpa_tts/kokoro-en-v0_19/` — fetched at CI build time from
 * Hugging Face (see `.github/workflows/build-apk.yml`), never downloaded by
 * the app itself and never dependent on the user importing anything.
 *
 * ## Why this is no longer a device-folder resolver
 *
 * This class used to search a handful of device folders for a user-imported
 * Kokoro multi-lang v1.0 directory. That model is multi-lingual (v1.0+) and
 * requires a `lang`/`lexicon` parameter that was never supplied — sherpa-onnx's
 * native init responded by calling `exit(-1)` on every cold boot, invisible to
 * every Kotlin try/catch because it's a real process exit, not a thrown
 * exception. Bundling the older, English-only v0.19 model (which has no such
 * requirement) as an APK asset removes both problems: no import step, and no
 * missing-parameter crash class.
 *
 * [com.k2fsa.sherpa.onnx.OfflineTts] loads straight from
 * [android.content.res.AssetManager] via its `assetManager` constructor
 * param — see `SherpaOnnxTtsClient.init()`. There is no filesystem copy step.
 */
class KokoroModelManager(private val context: Context) {

    @Volatile
    private var _state: KokoroSetupState = KokoroSetupState.Idle
    val state: KokoroSetupState get() = _state

    /** Asset-relative path prefix handed to `SherpaOnnxTtsClient`. */
    val modelDir: String get() = ASSET_DIR

    /**
     * Cheap `AssetManager.list()` check — no I/O beyond a directory listing.
     * Guards against a CI build that shipped without the voice pack; a
     * missing asset is a build defect, not a user-fixable state.
     */
    fun refresh(): KokoroSetupState {
        val missing = missingAssets()
        _state = if (missing.isEmpty()) {
            Log.i(TAG, "Kokoro model bundled at assets/$ASSET_DIR")
            KokoroSetupState.Ready
        } else {
            Log.w(TAG, "Kokoro voice pack missing from APK assets: $missing")
            KokoroSetupState.Missing(missing)
        }
        return _state
    }

    private fun missingAssets(): List<String> {
        val listed = runCatching { context.assets.list(ASSET_DIR)?.toSet() }.getOrNull() ?: emptySet()
        return REQUIRED.filterNot { it in listed }
    }

    companion object {
        const val TAG = "KokoroModelManager"
        const val ASSET_DIR = "sherpa_tts/kokoro-en-v0_19"
        val REQUIRED = listOf("model.onnx", "voices.bin", "tokens.txt", "espeak-ng-data")
    }
}
