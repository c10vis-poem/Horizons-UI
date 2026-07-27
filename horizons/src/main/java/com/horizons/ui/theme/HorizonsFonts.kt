package com.horizons.ui.theme

import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import com.horizons.R

/**
 * Brand typefaces, vendored from the Google Fonts mirror in
 * `c10vis-poem/Merovingian-fonts` (OFL — license texts under `licenses/`).
 *
 * Both are *variable* fonts carrying a single `wght` axis, so one .ttf backs
 * every weight. Each weight is pinned by an XML `<font-family>` in `res/font`
 * that sets `fontVariationSettings`, and the families below map those
 * resources onto Compose weights.
 *
 * The XML detour is deliberate: Compose's `FontVariation` API would express
 * the same thing in Kotlin, but it is `@ExperimentalTextApi` and
 * this module compiles opt-in violations as errors. The resource route is
 * plain platform API (variation settings need API 26; minSdk here is 31) and
 * carries no experimental surface.
 *
 * Only the weights actually in use are declared. A request outside a family's
 * declared set resolves to the nearest one rather than synthesising, so add a
 * resource here instead of reaching for a weight that isn't listed.
 */

/** Orbitron — the `MØ[)u14R_11(` wordmark. Axis wght 400..900; banner uses 800. */
val Orbitron = FontFamily(
    Font(R.font.orbitron_regular, FontWeight.Normal),
    Font(R.font.orbitron_extrabold, FontWeight.ExtraBold),
)

/**
 * Google Sans Code — the `*Pioneer_Tech` strapline and brand monospace text.
 * Axis wght 300..800. The strapline runs Normal (400); Light (300) is the
 * thinner alternative.
 */
val GoogleSansCode = FontFamily(
    Font(R.font.google_sans_code_light, FontWeight.Light),
    Font(R.font.google_sans_code_regular, FontWeight.Normal),
    Font(R.font.google_sans_code_bold, FontWeight.Bold),
)
