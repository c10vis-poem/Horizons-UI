package com.horizons.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.horizons.Panel

/* ==================================================================================
 * HomeGrid — Horizons V4 home dock.
 *
 * Written from scratch (2026-07-26) against the Gemini `HomeGridSim.tsx` reference
 * build: DEFAULT_CONFIG for the numbers, the JSX render tree for the geometry.
 * Nothing is carried over from any previous HomeGrid revision — the prior one is
 * preserved verbatim at wiki/originals/HomeGrid.kt.session21-original.
 *
 * The three coordinate spaces below are lifted from the reference unchanged, so
 * every shape can be checked against the source SVG line by line:
 *
 *   backdrop (stars / telemetry / plasma cords) ...... 400 x 600 viewBox, stretched
 *   router crystal ................................... 100 x 100 viewBox
 *   every tile glyph .................................  36 x  36 viewBox
 * ================================================================================== */

// ---------------------------------------------------------------------------------
// DEFAULT_CONFIG — HomeGridSim.tsx:13-37. Values are dp/sp as authored.
// ---------------------------------------------------------------------------------
private val CARD_W = 114.dp
private val CARD_H = 138.dp
private val ICON_SZ = 60.dp
private val TITLE_SP = 14.sp
private val SLOGAN_SP = 13.sp
private val LOGO_SP = 44.sp
private const val CRYSTAL_SCALE = 1.20f
private const val PLASMA_CURVE = 0.5f
private const val PLASMA_THICKNESS = 3.5f
private val STATUS_NODE = 36.dp
private val BG_DARK = Color(0xFF020406)
private const val STARS = 180
private const val TELEMETRY_CLUSTERS = 4

// Tile palette — HomeGridSim.tsx:385-452.
private val C_MONITOR = Color(0xFF2DD4D9)
private val C_CHAT = Color(0xFF4FE9A6)
private val C_SETTINGS = Color(0xFFFF5577)
private val C_TERMINAL = Color(0xFF00FF41)
private val C_ARCHIVES = Color(0xFFE8A838)
private val C_HORIZONS = Color(0xFF40C4FF)

private val CARD_BG = Color(0xFF0A0E11).copy(alpha = 0.95f)
private val TERMINAL_BG = Color(0xFF060A07)
private val AMBER = Color(0xFFF5C518)
private val VIOLET = Color(0xFFA855F7)
private val SLATE_950 = Color(0xFF020617)

private val MONO = FontFamily.Monospace

private enum class Glyph { MONITOR, CHAT, SETTINGS, TERMINAL, ARCHIVES, HORIZONS }

private data class Tile(
    val name: String,
    val slug: String,
    val sub: String,
    val cmd: String,
    val color: Color,
    val bg: Color,
    val glyph: Glyph,
    val panel: Panel,
)

private val TILES = listOf(
    Tile("MONITOR", "/cognito", "library", "\$_browser", C_MONITOR, CARD_BG, Glyph.MONITOR, Panel.Monitor),
    Tile("CHAT", "/interface", "tools", "\$_model", C_CHAT, CARD_BG, Glyph.CHAT, Panel.Chat),
    Tile("SETTINGS", "/config", "vault", "\$_utils", C_SETTINGS, CARD_BG, Glyph.SETTINGS, Panel.Settings),
    Tile("TERMINAL", "/shell", "commands", "\$_bash", C_TERMINAL, TERMINAL_BG, Glyph.TERMINAL, Panel.Terminal),
    Tile("ARCHIVES", "/logs", "artifacts", "\$_files", C_ARCHIVES, CARD_BG, Glyph.ARCHIVES, Panel.Artifacts),
    Tile("HORIZONS", "/about", "credits", "\$.home", C_HORIZONS, CARD_BG, Glyph.HORIZONS, Panel.Horizons),
)

// ---------------------------------------------------------------------------------
// Root
// ---------------------------------------------------------------------------------

@Composable
fun HomeGrid(
    onTileClick: (Panel) -> Unit,
    modifier: Modifier = Modifier,
) {
    val measurer = rememberTextMeasurer()

    Box(
        modifier
            .fillMaxSize()
            .background(BG_DARK)
            .statusBarsPadding()
            .navigationBarsPadding(),
    ) {
        // Astral backdrop spans the whole dock, exactly as the reference SVG does
        // (absolute inset-0 over the screen body) — HomeGridSim.tsx:137-242.
        AstralBackdrop(Modifier.fillMaxSize())

        Column(
            Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp),
        ) {
            HeaderBanner()
            ClockWheel(
                onTileClick = onTileClick,
                measurer = measurer,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(vertical = 8.dp),
            )
            ChatBar()
            Spacer(Modifier.height(8.dp))
            StatusNodes()
            Spacer(Modifier.height(8.dp))
        }
    }
}

// ---------------------------------------------------------------------------------
// Backdrop — stars, telemetry rings, plasma cords. 400 x 600 viewBox.
// ---------------------------------------------------------------------------------

/** One plasma cord: tile attach point -> crystal pedestal socket. Sim lines 191-197. */
private data class Cord(
    val tileX: Float, val tileY: Float,
    val nodeX: Float, val nodeY: Float,
    val color: Color,
    val curveX: Float, val curveY: Float,
)

private val CORDS = listOf(
    Cord(200f, 138f, 200f, 266f, C_MONITOR, 0f, 0f),      // 12:00 Monitor
    Cord(275f, 195f, 223f, 273f, C_CHAT, 20f, 12f),       //  2:00 Chat
    Cord(275f, 338f, 223f, 288f, C_SETTINGS, 20f, -12f),  //  4:00 Settings
    Cord(200f, 362f, 200f, 294f, C_TERMINAL, 0f, 0f),     //  6:00 Terminal
    Cord(125f, 338f, 177f, 288f, C_ARCHIVES, -20f, -12f), //  8:00 Archives
    Cord(125f, 195f, 177f, 273f, C_HORIZONS, -20f, 12f),  // 10:00 Horizons
)

@Composable
private fun AstralBackdrop(modifier: Modifier = Modifier) {
    // Deterministic star field, same integer hash as the reference (sim:139-157).
    val stars = remember {
        List(minOf(STARS, 160)) { i ->
            val x = ((i * 37 + 13) % 100) * 4f
            val y = ((i * 59 + 7) % 100) * 6f
            val fg = i % 5 == 0
            val mid = i % 3 == 0
            val r = if (fg) 1.5f else if (mid) 1.0f else 0.6f
            val a = if (fg) 0.8f else if (mid) 0.45f else 0.2f
            Triple(Offset(x, y), r to a, i % 4 == 0)
        }
    }

    Canvas(modifier) {
        val sx = size.width / 400f
        val sy = size.height / 600f
        fun p(x: Float, y: Float) = Offset(x * sx, y * sy)
        // Radii live in the 400-wide space; scale on the narrower axis so circles
        // stay circular instead of smearing with the stretch.
        val ru = minOf(sx, sy)

        // ---- star field -------------------------------------------------------
        stars.forEach { (pos, ra, teal) ->
            val (r, a) = ra
            drawCircle(
                color = if (teal) C_MONITOR else Color.White,
                radius = r * ru,
                center = p(pos.x, pos.y),
                alpha = a,
            )
        }

        // ---- telemetry rings ---------------------------------------------------
        fun ring(cx: Float, cy: Float, r: Float, w: Float, alpha: Float, dash: FloatArray? = null) {
            drawCircle(
                color = C_MONITOR,
                radius = r * ru,
                center = p(cx, cy),
                alpha = alpha,
                style = Stroke(
                    width = w * ru,
                    pathEffect = dash?.let {
                        PathEffect.dashPathEffect(floatArrayOf(it[0] * ru, it[1] * ru), 0f)
                    },
                ),
            )
        }

        // around the hub (sim:160-162)
        ring(200f, 240f, 65f, 0.6f, 0.18f, floatArrayOf(3f, 3f))
        ring(200f, 240f, 105f, 0.5f, 0.12f, floatArrayOf(6f, 4f))
        ring(200f, 240f, 145f, 0.5f, 0.08f)

        // extra clusters scattered off-hub (sim:165-188)
        if (TELEMETRY_CLUSTERS >= 1) {
            ring(72f, 110f, 24f, 0.6f, 0.20f)
            ring(72f, 110f, 38f, 0.5f, 0.12f, floatArrayOf(2f, 2f))
        }
        if (TELEMETRY_CLUSTERS >= 2) {
            ring(328f, 390f, 20f, 0.6f, 0.20f)
            ring(328f, 390f, 32f, 0.5f, 0.12f)
        }
        if (TELEMETRY_CLUSTERS >= 3) {
            ring(328f, 110f, 18f, 0.5f, 0.18f, floatArrayOf(4f, 2f))
            ring(72f, 390f, 22f, 0.5f, 0.15f)
        }
        if (TELEMETRY_CLUSTERS >= 4) {
            ring(200f, 55f, 28f, 0.5f, 0.15f, floatArrayOf(5f, 3f))
            ring(200f, 510f, 25f, 0.5f, 0.15f, floatArrayOf(3f, 3f))
        }

        // ---- plasma cords (sim:198-241) ---------------------------------------
        CORDS.forEach { c ->
            val ctrlX = (c.tileX + c.nodeX) / 2f + c.curveX * PLASMA_CURVE * 2.2f
            val ctrlY = (c.tileY + c.nodeY) / 2f + c.curveY * PLASMA_CURVE * 2.2f

            val path = Path().apply {
                val s = p(c.tileX, c.tileY)
                val ctl = p(ctrlX, ctrlY)
                val e = p(c.nodeX, c.nodeY)
                moveTo(s.x, s.y)
                quadraticTo(ctl.x, ctl.y, e.x, e.y)
            }

            // soft "melt" bloom where the cord meets the tile corner
            drawCircle(c.color, 8f * ru, p(c.tileX, c.tileY), alpha = 0.18f)
            drawCircle(c.color, 5f * ru, p(c.tileX, c.tileY), alpha = 0.28f)

            // outer neon halo -> main plasma tube -> inner white laser core
            drawPath(
                path, c.color, alpha = 0.25f,
                style = Stroke(maxOf(3.2f, PLASMA_THICKNESS * 2f) * ru, cap = StrokeCap.Round),
            )
            drawPath(
                path, c.color, alpha = 0.9f,
                style = Stroke(maxOf(1.5f, PLASMA_THICKNESS) * ru, cap = StrokeCap.Round),
            )
            drawPath(
                path, Color.White, alpha = 0.95f,
                style = Stroke(0.8f * ru, cap = StrokeCap.Round),
            )

            // terminating socket glow dots at both ends
            drawCircle(c.color, 2.5f * ru, p(c.tileX, c.tileY))
            drawCircle(Color.White, 1.2f * ru, p(c.tileX, c.tileY))
            drawCircle(c.color, 3f * ru, p(c.nodeX, c.nodeY))
            drawCircle(Color.White, 1.5f * ru, p(c.nodeX, c.nodeY))
        }
    }
}

// ---------------------------------------------------------------------------------
// Header banner (sim:245-266)
// ---------------------------------------------------------------------------------

@Composable
private fun HeaderBanner() {
    Column(
        Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(4.dp))

        // Logo: two weights sharing one baseline — "MØ[)u14R" then a smaller "_11(".
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                "MØ[)u14R",
                color = Color(0xFF5EEAD4),
                fontSize = LOGO_SP * 0.75f,
                fontFamily = MONO,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.5.sp,
            )
            Text(
                "_11(",
                color = C_MONITOR,
                fontSize = LOGO_SP * 0.58f,
                fontFamily = MONO,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.5.sp,
            )
        }

        Spacer(Modifier.height(2.dp))

        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "*Pioneer_Tech,",
                color = Color(0xFF5EEAD4),
                fontSize = SLOGAN_SP,
                fontFamily = MONO,
                fontWeight = FontWeight.Bold,
            )
            Text(
                "(Next-Gen Certified)",
                color = Color(0xFF99F6E4),
                fontSize = SLOGAN_SP,
                fontFamily = MONO,
                fontWeight = FontWeight.ExtraBold,
            )
        }

        Text(
            "HORIZONS // V4",
            color = C_MONITOR.copy(alpha = 0.5f),
            fontSize = 9.sp,
            fontFamily = MONO,
            textAlign = TextAlign.End,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp, end = 4.dp),
        )

        Spacer(Modifier.height(6.dp))
        // purple-500/15 hairline closing the header block
        Box(
            Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(VIOLET.copy(alpha = 0.15f)),
        )
    }
}

// ---------------------------------------------------------------------------------
// Clock wheel — hub at centre, six tiles pinned to the edges (sim:269-625)
// ---------------------------------------------------------------------------------

@Composable
private fun ClockWheel(
    onTileClick: (Panel) -> Unit,
    measurer: TextMeasurer,
    modifier: Modifier = Modifier,
) {
    Box(modifier, contentAlignment = Alignment.Center) {
        RouterHub(onClick = { onTileClick(Panel.Router) })

        // Anchors mirror the reference's Tailwind classes:
        //   monitor  top-7  left-1/2      chat     top-16    right-3
        //   settings bottom-20 right-3    terminal bottom-4  left-1/2
        //   archives bottom-20 left-3     horizons top-16    left-3
        TileAt(TILES[0], Alignment.TopCenter, 0.dp, 28.dp, onTileClick, measurer)
        TileAt(TILES[1], Alignment.TopEnd, (-12).dp, 64.dp, onTileClick, measurer)
        TileAt(TILES[2], Alignment.BottomEnd, (-12).dp, (-80).dp, onTileClick, measurer)
        TileAt(TILES[3], Alignment.BottomCenter, 0.dp, (-16).dp, onTileClick, measurer)
        TileAt(TILES[4], Alignment.BottomStart, 12.dp, (-80).dp, onTileClick, measurer)
        TileAt(TILES[5], Alignment.TopStart, 12.dp, 64.dp, onTileClick, measurer)
    }
}

@Composable
private fun BoxScope.TileAt(
    tile: Tile,
    align: Alignment,
    dx: Dp,
    dy: Dp,
    onTileClick: (Panel) -> Unit,
    measurer: TextMeasurer,
) {
    TileCard(
        tile = tile,
        measurer = measurer,
        onClick = { onTileClick(tile.panel) },
        modifier = Modifier
            .align(align)
            .offset(x = dx, y = dy),
    )
}

// ---------------------------------------------------------------------------------
// Tile card — glyph protrudes above the card with a backlit aura, no plate or
// border around the glyph itself (sim:456-622)
// ---------------------------------------------------------------------------------

@Composable
private fun TileCard(
    tile: Tile,
    measurer: TextMeasurer,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // Reserve the protrusion above the card so the glyph is never clipped.
    val protrude = ICON_SZ / 2

    Box(
        modifier
            .width(CARD_W)
            .height(CARD_H + protrude),
        contentAlignment = Alignment.TopCenter,
    ) {
        // ---- card body -------------------------------------------------------
        Box(
            Modifier
                .align(Alignment.BottomCenter)
                .width(CARD_W)
                .height(CARD_H)
                .clip(RoundedCornerShape(12.dp))
                .background(tile.bg)
                .border(1.dp, tile.color.copy(alpha = 0.27f), RoundedCornerShape(12.dp))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onClick,
                )
                .padding(start = 8.dp, end = 8.dp, bottom = 8.dp, top = protrude + 6.dp),
        ) {
            Column(
                Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    tile.name,
                    color = tile.color,
                    fontSize = TITLE_SP,
                    fontFamily = MONO,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    maxLines = 1,
                    textAlign = TextAlign.Center,
                )

                Text(
                    "${tile.slug} · ${tile.sub}",
                    color = Color(0xFF94A3B8),
                    fontSize = 8.sp,
                    fontFamily = MONO,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    textAlign = TextAlign.Center,
                )

                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(tile.color.copy(alpha = 0.13f)),
                )

                // bordered prompt strip: $_command  ⚙
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(4.dp))
                        .background(tile.color.copy(alpha = 0.06f))
                        .border(1.dp, tile.color.copy(alpha = 0.2f), RoundedCornerShape(4.dp))
                        .padding(horizontal = 5.dp, vertical = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        tile.cmd,
                        color = tile.color,
                        fontSize = 8.sp,
                        fontFamily = MONO,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                    )
                    Text(
                        "⚙",
                        color = tile.color.copy(alpha = 0.6f),
                        fontSize = 8.sp,
                        fontFamily = MONO,
                        maxLines = 1,
                    )
                }
            }
        }

        // ---- protruding glyph + backlit aura ---------------------------------
        Box(
            Modifier
                .align(Alignment.TopCenter)
                .size(ICON_SZ),
            contentAlignment = Alignment.Center,
        ) {
            Canvas(Modifier.fillMaxSize()) {
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            tile.color.copy(alpha = 0.42f),
                            tile.color.copy(alpha = 0.16f),
                            Color.Transparent,
                        ),
                        center = center,
                        radius = size.minDimension / 2f,
                    ),
                    radius = size.minDimension / 2f,
                )
            }
            Canvas(Modifier.size(ICON_SZ * 0.9f)) {
                drawGlyph(tile.glyph, measurer)
            }
        }
    }
}

// ---------------------------------------------------------------------------------
// Tile glyphs — 36 x 36 viewBox, one-for-one with the reference SVGs (sim:481-575)
// ---------------------------------------------------------------------------------

private fun DrawScope.drawGlyph(glyph: Glyph, measurer: TextMeasurer) {
    val u = size.minDimension / 36f
    fun p(x: Float, y: Float) = Offset(x * u, y * u)
    fun line(x1: Float, y1: Float, x2: Float, y2: Float, c: Color, w: Float, a: Float = 1f) =
        drawLine(c, p(x1, y1), p(x2, y2), w * u, StrokeCap.Round, alpha = a)

    when (glyph) {
        // --- HORIZONS: violet dome, amber sun + rays, blue horizon line --------
        Glyph.HORIZONS -> {
            drawArc(
                color = Color(0xFFBB88FF),
                startAngle = 180f, sweepAngle = 180f, useCenter = false,
                topLeft = p(5f, 10f), size = Size(26f * u, 26f * u),
                style = Stroke(2.2f * u, cap = StrokeCap.Round),
            )
            line(18f, 3f, 18f, 8.5f, AMBER, 2f)
            line(8.5f, 7.5f, 12.5f, 11.5f, AMBER, 2f)
            line(27.5f, 7.5f, 23.5f, 11.5f, AMBER, 2f)
            drawArc(
                color = AMBER,
                startAngle = 180f, sweepAngle = 180f, useCenter = false,
                topLeft = p(12f, 17f), size = Size(12f * u, 12f * u),
                style = Stroke(2f * u),
            )
            drawCircle(AMBER, 2.5f * u, p(18f, 19f))
            line(3f, 23f, 33f, 23f, C_HORIZONS, 2.2f)
        }

        // --- MONITOR: display + stand, PC badge upper-right --------------------
        Glyph.MONITOR -> {
            drawRoundRect(Color(0xFF0A0E11), p(4f, 6f), Size(28f * u, 18f * u), CornerRadius(3f * u))
            drawRoundRect(
                C_MONITOR, p(4f, 6f), Size(28f * u, 18f * u), CornerRadius(3f * u),
                style = Stroke(2f * u),
            )
            line(8f, 12f, 22f, 12f, C_MONITOR, 1.2f, 0.8f)
            line(8f, 17f, 18f, 17f, C_MONITOR, 1.2f, 0.6f)
            val stand = Path().apply {
                moveTo(p(12f, 28f).x, p(12f, 28f).y)
                lineTo(p(18f, 24f).x, p(18f, 24f).y)
                lineTo(p(24f, 28f).x, p(24f, 28f).y)
            }
            drawPath(stand, C_MONITOR, style = Stroke(2f * u, cap = StrokeCap.Round))
            drawRoundRect(C_MONITOR, p(23f, 4f), Size(10f * u, 8f * u), CornerRadius(2f * u))
            val pc = measurer.measure(
                AnnotatedString("PC"),
                TextStyle(
                    color = Color(0xFF0A0E11),
                    fontSize = (5.5f * u).toSp(),
                    fontFamily = MONO,
                    fontWeight = FontWeight.Black,
                ),
            )
            drawText(pc, topLeft = p(28f, 8f) - Offset(pc.size.width / 2f, pc.size.height / 2f))
        }

        // --- CHAT: rounded speech bubble, tail, two lines ----------------------
        Glyph.CHAT -> {
            drawRoundRect(
                C_CHAT, p(4f, 5f), Size(28f * u, 20f * u), CornerRadius(5f * u),
                style = Stroke(2.5f * u),
            )
            val tail = Path().apply {
                moveTo(p(10f, 25f).x, p(10f, 25f).y)
                lineTo(p(8f, 31f).x, p(8f, 31f).y)
                lineTo(p(16f, 25f).x, p(16f, 25f).y)
                close()
            }
            drawPath(tail, C_CHAT)
            line(10f, 11f, 24f, 11f, C_CHAT, 2f)
            line(10f, 16f, 19f, 16f, C_CHAT, 2f)
        }

        // --- TERMINAL: matrix-green window, chrome dots, prompt caret ----------
        Glyph.TERMINAL -> {
            drawRoundRect(TERMINAL_BG, p(4f, 6f), Size(28f * u, 20f * u), CornerRadius(3f * u))
            drawRoundRect(
                C_TERMINAL, p(4f, 6f), Size(28f * u, 20f * u), CornerRadius(3f * u),
                style = Stroke(2f * u),
            )
            drawCircle(C_TERMINAL, 1.2f * u, p(8f, 10f))
            drawCircle(C_TERMINAL, 1.2f * u, p(12f, 10f))
            drawCircle(C_TERMINAL, 1.2f * u, p(16f, 10f))
            line(4f, 14f, 32f, 14f, C_TERMINAL, 0.8f, 0.4f)
            val caret = Path().apply {
                moveTo(p(8f, 18f).x, p(8f, 18f).y)
                lineTo(p(13f, 21f).x, p(13f, 21f).y)
                lineTo(p(8f, 24f).x, p(8f, 24f).y)
            }
            drawPath(caret, C_TERMINAL, style = Stroke(1.8f * u, cap = StrokeCap.Round))
            line(15f, 24f, 22f, 24f, C_TERMINAL, 1.8f)
        }

        // --- ARCHIVES: back document + overlapping "A" badge document ----------
        Glyph.ARCHIVES -> {
            fun doc(x: Float, y: Float, w: Float, h: Float) {
                drawRoundRect(Color(0xFF0A0E11), p(x, y), Size(w * u, h * u), CornerRadius(3f * u))
                drawRoundRect(
                    C_ARCHIVES, p(x, y), Size(w * u, h * u), CornerRadius(3f * u),
                    style = Stroke(2.2f * u),
                )
            }
            doc(5f, 2f, 18f, 24f)
            line(9f, 7f, 18f, 7f, C_ARCHIVES, 1.8f)
            line(9f, 12f, 18f, 12f, C_ARCHIVES, 1.8f)
            line(9f, 17f, 14f, 17f, C_ARCHIVES, 1.8f)
            doc(15f, 12f, 16f, 21f)
            val a = measurer.measure(
                AnnotatedString("A"),
                TextStyle(
                    color = C_ARCHIVES,
                    fontSize = (13f * u).toSp(),
                    fontWeight = FontWeight.Black,
                ),
            )
            drawText(a, topLeft = p(23f, 22.5f) - Offset(a.size.width / 2f, a.size.height / 2f))
        }

        // --- SETTINGS: ticked dial, crimson core, yellow bolt ------------------
        Glyph.SETTINGS -> {
            for (deg in 0 until 360 step 45) {
                val rad = Math.toRadians(deg.toDouble())
                val cosR = kotlin.math.cos(rad).toFloat()
                val sinR = kotlin.math.sin(rad).toFloat()
                line(
                    18f + 10.5f * cosR, 18f + 10.5f * sinR,
                    18f + 13.5f * cosR, 18f + 13.5f * sinR,
                    C_SETTINGS, 2.2f,
                )
            }
            drawCircle(
                C_SETTINGS, 10.5f * u, p(18f, 18f), alpha = 0.8f,
                style = Stroke(
                    1f * u,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(3f * u, 2f * u), 0f),
                ),
            )
            drawCircle(C_SETTINGS, 7.5f * u, p(18f, 18f))
            val bolt = Path().apply {
                moveTo(p(18.5f, 12f).x, p(18.5f, 12f).y)
                lineTo(p(14.2f, 18f).x, p(14.2f, 18f).y)
                lineTo(p(17.2f, 18f).x, p(17.2f, 18f).y)
                lineTo(p(15.8f, 24f).x, p(15.8f, 24f).y)
                lineTo(p(21.8f, 17f).x, p(21.8f, 17f).y)
                lineTo(p(18.8f, 17f).x, p(18.8f, 17f).y)
                close()
            }
            drawPath(bolt, AMBER)
        }
    }
}

// ---------------------------------------------------------------------------------
// Router hub — 3D hexagonal faceted crystal on a glowing pedestal (sim:271-382)
// ---------------------------------------------------------------------------------

@Composable
private fun RouterHub(onClick: () -> Unit) {
    val crystalSize = 110.dp * CRYSTAL_SCALE

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = null,
            onClick = onClick,
        ),
    ) {
        Box(contentAlignment = Alignment.Center) {
            // violet/white sun aura permeating out from inside the crystal
            Canvas(Modifier.size(crystalSize * 1.45f)) {
                drawCircle(
                    brush = Brush.radialGradient(
                        colorStops = arrayOf(
                            0.0f to Color(0xFFC084FC).copy(alpha = 0.40f),
                            0.45f to Color(0xFF7C3AED).copy(alpha = 0.30f),
                            1.0f to Color.Transparent,
                        ),
                        center = center,
                        radius = size.minDimension / 2f,
                    ),
                    radius = size.minDimension / 2f,
                )
            }
            Canvas(Modifier.size(crystalSize)) { drawCrystal() }
        }

        // hub label plate
        Column(
            Modifier
                .offset(y = (-6).dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFF0A0518).copy(alpha = 0.9f))
                .border(1.dp, VIOLET.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                .padding(horizontal = 10.dp, vertical = 2.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                "// CORE_HUB",
                color = Color(0xFFC4B5FD),
                fontSize = 9.sp,
                fontFamily = MONO,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                "ROUTER",
                color = Color.White,
                fontSize = 13.sp,
                fontFamily = MONO,
                fontWeight = FontWeight.Black,
                letterSpacing = 2.sp,
            )
            Text(
                "\$_Statio",
                color = Color(0xFFC4B5FD).copy(alpha = 0.8f),
                fontSize = 8.sp,
                fontFamily = MONO,
            )
        }
    }
}

/** 100 x 100 viewBox — pedestal, six socket nodes, faceted gem, inner sun. */
private fun DrawScope.drawCrystal() {
    val u = size.minDimension / 100f
    fun p(x: Float, y: Float) = Offset(x * u, y * u)
    fun poly(vararg pts: Float): Path = Path().apply {
        moveTo(pts[0] * u, pts[1] * u)
        var i = 2
        while (i < pts.size) {
            lineTo(pts[i] * u, pts[i + 1] * u)
            i += 2
        }
        close()
    }

    val platTop = Rect(12f * u, 62f * u, 88f * u, 86f * u)   // cx50 cy74 rx38 ry12
    val platBot = Rect(12f * u, 66f * u, 88f * u, 90f * u)   // cx50 cy78 rx38 ry12

    // ---- pedestal cylinder wall (band between the two discs) ------------------
    val wall = Path().apply {
        moveTo(platTop.right, 74f * u)
        arcTo(platTop, 0f, 180f, false)
        lineTo(platBot.left, 78f * u)
        arcTo(platBot, 180f, -180f, false)
        close()
    }
    drawPath(wall, Color(0xFF130924))
    drawPath(wall, Color(0xFF7E22CE), style = Stroke(0.8f * u))

    // ---- lower dark disc ------------------------------------------------------
    drawOval(Color(0xFF0A0518), platBot.topLeft, platBot.size)
    drawOval(C_MONITOR, platBot.topLeft, platBot.size, alpha = 0.6f, style = Stroke(0.8f * u))

    // ---- glowing glass top disc ----------------------------------------------
    drawOval(
        brush = Brush.verticalGradient(
            colorStops = arrayOf(
                0.0f to C_MONITOR.copy(alpha = 0.45f),
                0.5f to Color(0xFF7E22CE).copy(alpha = 0.35f),
                1.0f to Color(0xFF1E1035).copy(alpha = 0.80f),
            ),
            startY = platTop.top,
            endY = platTop.bottom,
        ),
        topLeft = platTop.topLeft,
        size = platTop.size,
    )
    drawOval(
        brush = Brush.horizontalGradient(
            colorStops = arrayOf(
                0.0f to C_MONITOR.copy(alpha = 0.8f),
                0.5f to VIOLET.copy(alpha = 0.9f),
                1.0f to C_MONITOR.copy(alpha = 0.8f),
            ),
            startX = platTop.left,
            endX = platTop.right,
        ),
        topLeft = platTop.topLeft,
        size = platTop.size,
        style = Stroke(1.6f * u),
    )
    // concentric rings etched on the glass
    drawOval(
        C_MONITOR, Offset(19f * u, 65f * u), Size(62f * u, 18f * u), alpha = 0.85f,
        style = Stroke(1f * u, pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f * u, 2f * u), 0f)),
    )
    drawOval(Color.White.copy(alpha = 0.08f), Offset(28f * u, 68f * u), Size(44f * u, 12f * u))
    drawOval(VIOLET, Offset(28f * u, 68f * u), Size(44f * u, 12f * u), style = Stroke(0.8f * u))

    // ---- six perimeter socket nodes (the cords terminate here) ----------------
    listOf(
        50f to 63f, 68f to 68f, 68f to 80f,
        50f to 85f, 32f to 80f, 32f to 68f,
    ).forEach { (nx, ny) ->
        drawCircle(C_MONITOR, 4.5f * u, p(nx, ny), alpha = 0.25f)
        drawCircle(C_MONITOR, 2.4f * u, p(nx, ny), alpha = 0.95f)
        drawCircle(Color.White, 1f * u, p(nx, ny))
    }

    // ---- faceted gem body -----------------------------------------------------
    fun facetGradient(path: Path, from: Color, to: Color): Brush {
        val b = path.getBounds()
        return Brush.linearGradient(listOf(from, to), start = b.topLeft, end = b.bottomRight)
    }

    // back facet shadow
    drawPath(poly(50f, 10f, 74f, 30f, 68f, 22f), Color(0xFF3B0764), alpha = 0.6f)

    val frontLeft = poly(26f, 30f, 50f, 35f, 50f, 72f, 28f, 67f)
    drawPath(
        frontLeft,
        facetGradient(frontLeft, Color(0xFF9333EA).copy(alpha = 0.85f), Color(0xFF581C87).copy(alpha = 0.9f)),
    )
    drawPath(frontLeft, Color(0xFFC084FC), style = Stroke(0.8f * u))

    val frontRight = poly(50f, 35f, 74f, 30f, 72f, 67f, 50f, 72f)
    drawPath(
        frontRight,
        facetGradient(frontRight, VIOLET.copy(alpha = 0.9f), Color(0xFF6B21A8).copy(alpha = 0.95f)),
    )
    drawPath(frontRight, Color(0xFFE9D5FF), style = Stroke(0.8f * u))

    val rightSide = poly(74f, 30f, 80f, 24f, 78f, 60f, 72f, 67f)
    drawPath(rightSide, Color(0xFF4C1D95), alpha = 0.9f)
    drawPath(rightSide, VIOLET, alpha = 0.9f, style = Stroke(0.7f * u))

    val capLeft = poly(50f, 10f, 26f, 30f, 50f, 35f)
    drawPath(
        capLeft,
        facetGradient(capLeft, Color(0xFFC084FC).copy(alpha = 0.95f), Color(0xFF7E22CE).copy(alpha = 0.9f)),
    )
    drawPath(capLeft, Color(0xFFE9D5FF), style = Stroke(1f * u))

    val capRight = poly(50f, 10f, 50f, 35f, 74f, 30f)
    drawPath(
        capRight,
        facetGradient(capRight, Color(0xFFE9D5FF).copy(alpha = 0.95f), Color(0xFF9333EA).copy(alpha = 0.9f)),
    )
    drawPath(capRight, Color.White, style = Stroke(1f * u))

    // ---- white sun burning inside the gem -------------------------------------
    drawCircle(
        brush = Brush.radialGradient(
            colorStops = arrayOf(
                0.00f to Color.White,
                0.30f to Color(0xFFE9D5FF).copy(alpha = 0.9f),
                0.65f to VIOLET.copy(alpha = 0.6f),
                1.00f to Color.Transparent,
            ),
            center = p(50f, 50f),
            radius = 22f * u,
        ),
        radius = 22f * u,
        center = p(50f, 50f),
        alpha = 0.95f,
    )
    drawCircle(Color.White, 4.5f * u, p(50f, 50f))

    // sharp specular streak down the top-left cap
    drawLine(
        Color.White, p(48f, 12f), p(30f, 28f),
        strokeWidth = 1.5f * u, cap = StrokeCap.Round, alpha = 0.9f,
    )
}

// ---------------------------------------------------------------------------------
// Chat bar — sits ABOVE the status nodes (sim:628-657)
// ---------------------------------------------------------------------------------

@Composable
private fun ChatBar() {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(CircleShape)
            .background(SLATE_950)
            .border(1.dp, C_MONITOR.copy(alpha = 0.4f), CircleShape)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("⊕", color = C_MONITOR, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            Text(
                "tap_or_hold  ask //",
                color = Color(0xFF5EEAD4).copy(alpha = 0.8f),
                fontSize = 11.sp,
                fontFamily = MONO,
            )
        }
        Text("↑", color = C_MONITOR, fontSize = 14.sp, fontWeight = FontWeight.Bold)
    }
}

// ---------------------------------------------------------------------------------
// System status — five 3D glossy spheres (sim:660-704)
// ---------------------------------------------------------------------------------

private data class StatusNode(val label: String, val color: Color, val active: Boolean)

private val STATUS = listOf(
    StatusNode("ASR", C_TERMINAL, true),
    StatusNode("LLM", C_HORIZONS, true),
    StatusNode("TTS", C_ARCHIVES, true),
    StatusNode("MLLM", Color(0xFFAA77FF), false),
    StatusNode("VAG", C_SETTINGS, false),
)

@Composable
private fun StatusNodes() {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(SLATE_950.copy(alpha = 0.9f))
            .border(1.dp, C_MONITOR.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            "// SYSTEM_STATUS",
            color = C_MONITOR.copy(alpha = 0.5f),
            fontSize = 9.sp,
            fontFamily = MONO,
            fontWeight = FontWeight.Bold,
            letterSpacing = 2.sp,
        )
        Spacer(Modifier.height(8.dp))
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            STATUS.forEach { node ->
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Canvas(Modifier.size(STATUS_NODE)) {
                        val r = size.minDimension / 2f
                        // glossy sphere lit from 35% / 35%
                        val lit = Offset(size.width * 0.35f, size.height * 0.35f)
                        if (node.active) {
                            drawCircle(node.color.copy(alpha = 0.35f), r * 1.02f, center)
                            drawCircle(
                                brush = Brush.radialGradient(
                                    colorStops = arrayOf(
                                        0.0f to node.color,
                                        0.60f to node.color.copy(alpha = 0.6f),
                                        1.0f to Color.Black,
                                    ),
                                    center = lit,
                                    radius = r * 1.35f,
                                ),
                                radius = r,
                                center = center,
                            )
                            // upper-left specular glint
                            drawCircle(
                                Color.White.copy(alpha = 0.7f),
                                radius = r * 0.16f,
                                center = Offset(size.width * 0.34f, size.height * 0.32f),
                            )
                        } else {
                            drawCircle(
                                brush = Brush.radialGradient(
                                    colorStops = arrayOf(
                                        0.0f to node.color.copy(alpha = 0.27f),
                                        0.80f to node.color.copy(alpha = 0.07f),
                                        1.0f to Color.Black,
                                    ),
                                    center = lit,
                                    radius = r * 1.35f,
                                ),
                                radius = r,
                                center = center,
                            )
                        }
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(
                        node.label,
                        color = if (node.active) node.color else Color(0xFF475569),
                        fontSize = 10.sp,
                        fontFamily = MONO,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }
    }
}
