package com.horizons.ui.theme

import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import com.horizons.R

/**
 * Brand typefaces, vendored from the Google Fonts mirror in
 * `c10vis-poem/Merovingian-fonts` (OFL — license texts under `licenses/`).
 *
 * Both files are *variable* fonts carrying a single `wght` axis, so one .ttf
 * covers every weight. Compose picks the instance through
 * [FontVariation.Settings]; asking for a weight outside a font's axis range
 * gets clamped rather than synthesised, which is why each family below only
 * declares the weights its axis actually contains.
 *
 * minSdk is 31, comfortably past the API 26 floor for variation settings.
 */
private fun wght(w: Int) = FontVariation.Settings(FontVariation.weight(w))

/**
 * Orbitron — the `MØ[)u14R_11(` wordmark. Axis: wght 400..900.
 * The banner uses ExtraBold (800) at 45sp.
 */
val Orbitron = FontFamily(
    Font(R.font.orbitron_variable, FontWeight.Normal, variationSettings = wght(400)),
    Font(R.font.orbitron_variable, FontWeight.Medium, variationSettings = wght(500)),
    Font(R.font.orbitron_variable, FontWeight.SemiBold, variationSettings = wght(600)),
    Font(R.font.orbitron_variable, FontWeight.Bold, variationSettings = wght(700)),
    Font(R.font.orbitron_variable, FontWeight.ExtraBold, variationSettings = wght(800)),
    Font(R.font.orbitron_variable, FontWeight.Black, variationSettings = wght(900)),
)

/**
 * Google Sans Code — the `*Pioneer_Tech` strapline and any monospace UI text
 * that wants the brand face instead of the platform default. Axis: wght 300..800.
 * The strapline runs Normal (400); drop it to Light (300) for a thinner line.
 */
val GoogleSansCode = FontFamily(
    Font(R.font.google_sans_code, FontWeight.Light, variationSettings = wght(300)),
    Font(R.font.google_sans_code, FontWeight.Normal, variationSettings = wght(400)),
    Font(R.font.google_sans_code, FontWeight.Medium, variationSettings = wght(500)),
    Font(R.font.google_sans_code, FontWeight.SemiBold, variationSettings = wght(600)),
    Font(R.font.google_sans_code, FontWeight.Bold, variationSettings = wght(700)),
    Font(R.font.google_sans_code, FontWeight.ExtraBold, variationSettings = wght(800)),
)
