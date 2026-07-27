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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import com.horizons.R
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.horizons.Panel
import kotlin.math.sqrt

/* ==================================================================================
 * HomeGrid — Horizons V5 home dock.
 *
 * V5 is a SEPARATE build from V4 (branch claude/homegrid-v4-scratch, PR #30), which
 * is left untouched and still installable. Geometry, glyphs and palette trace to the
 * reference build's HomeGridSim.tsx; this revision applies the operator's on-device
 * notes taken against the V4 screenshot.
 *
 * Kept from V4 — operator: "what you did better":
 *   · the deeper black background, and the way the colours pop off it
 *   · tile card proportions and the type inside them
 *
 * Changed in V5 — the operator's punch list:
 *   · status panel shrunk hard, chat bar trimmed  -> chat bar drops down the screen
 *   · top tiles raised, bottom tiles dropped      -> gap closed, hub gets real room
 *   · header hairline pulled up
 *   · crystal enlarged, ROUTER plate tightened toward reference proportions
 *   · icons enlarged, with a real back-glow; tile borders now glow too
 *   · logo brackets no longer tower over the letters
 *   · PLASMA CORDS ARE NOW MEASURED rather than hardcoded. They attach to the real
 *     tile edges and run to the real pedestal sockets, finishing *underneath* the
 *     platform. This is the structural fix: in V4 the cords sat at fixed 400x600
 *     viewBox coordinates while the hub was laid out independently, so every
 *     spacing change desynced them. They now derive from measured layout bounds
 *     and stay attached no matter how the rest of the dock is retuned.
 *
 * Coordinate spaces still carried from the reference:
 *   star field / telemetry rings ..... 400 x 600 viewBox, stretched
 *   router crystal ................... 100 x 100 viewBox
 *   every tile glyph .................  36 x  36 viewBox
 * ================================================================================== */

// ---------------------------------------------------------------------------------
// Tunables — everything the operator asked to move lives in this block.
// ---------------------------------------------------------------------------------
private val CARD_W = 114.dp                   // kept: operator likes the tile size
private val CARD_H = 138.dp                   // kept
private val ICON_SZ = 68.dp                   // up from 60
private const val CRYSTAL_SCALE = 1.42f       // up from 1.20
private val STATUS_NODE = 28.dp               // was 36 — "no reason for it to be that fat"

/* Type sizes are declared in dp, not sp, and converted per-density below.
 *
 * This dock is fixed geometry: cards are a hard 114 x 138dp and the labels have to
 * fit inside them. Sizing in sp makes every label track the SYSTEM font-scale
 * setting, so on a phone with large text turned on the titles overflowed and got
 * clipped — "TERMINAL" rendered as "TERMINA", "HORIZONS" as "HORIZON", and the
 * "commands" subtitle vanished entirely. Deriving from dp keeps the type honest to
 * the layout at any accessibility setting. */
private val TITLE_DP = 14.dp
private val SUB_DP = 8.dp
private val CMD_DP = 8.dp
/* Banner sizing. The wordmark renders at LOGO_DP * 0.75 and the "_11(" tail at
 * LOGO_DP * 0.58, so LOGO_DP is NOT the rendered size — at the old 44dp the
 * wordmark was landing at only 33dp, which the operator read as shrunken.
 * 56dp puts the top line at 42dp rendered (0.75 x 56), the size asked for, with
 * the tail following proportionally at ~32dp. SLOGAN_DP scales by the same 1.27
 * so the three lines keep the proportions the operator said were already right. */
private val SLOGAN_DP = 16.dp
private val LOGO_DP = 56.dp
private val STATUS_LABEL_DP = 9.dp

/** Bleed room around each card so its border glow can spill OUTWARD. The card keeps
 *  its CARD_W x CARD_H footprint; only the box around it grows, and the offsets
 *  below already subtract it, so the cards land where they did before. Cord anchors
 *  are measured off the card body, so they follow this automatically. */
private val GLOW_PAD = 10.dp

/* Tile placement inside the clock wheel.
 *
 * The centre tiles sit further out than the side pairs; that stagger is what makes
 * this read as a clock face instead of two rows.
 *
 * A caution about references, since it cost a round: the repo's
 * 33-prior-build-full-home-target.png is a DIFFERENT, older design (ARTIFACTS not
 * ARCHIVES, chat bar below the status panel) and its stagger is 0.656 x card
 * height. The build actually being matched here is tighter than that, nearer
 * 0.35-0.45. Do not tune these off 33-prior-build.
 *
 * Top stagger is 64dp (~0.46 x CARD_H) — comfortably more than the 44dp the
 * operator called "crowded inward", without inheriting the older build's much
 * wider spread. Bottom stagger is 72dp. Both are read off a screenshot rather
 * than measured, so they are the likeliest thing still needing a nudge.
 * Bottom/side numbers carry a +GLOW_PAD correction for the bleed room. */
/* All six tiles pulled INWARD by TILE_INSET (3 x TITLE_DP = 42dp), per the
 * operator: "stack three lines of text on top of each other, that's how far you
 * move all six tiles inward toward the centre". Top tiles move down, bottom tiles
 * move up; the stagger between centre and side tiles is preserved because the same
 * inset applies to both. This also un-clips the TERMINAL card, which was running
 * off the bottom edge and losing its "$_bash" prompt box. */
private val TILE_INSET = 42.dp

private val TOP_MID_Y = 0.dp + TILE_INSET
private val TOP_SIDE_Y = 64.dp + TILE_INSET      // 64dp stagger vs the centre tile
private val BOT_MID_Y = 26.dp - TILE_INSET       // 16dp visual + GLOW_PAD
private val BOT_SIDE_Y = (-46).dp - TILE_INSET   // -56dp visual + GLOW_PAD -> 72dp stagger
private val SIDE_X = 0.dp            // 10dp visual - GLOW_PAD

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

/** Banner faces, confirmed on-device and superseding the Audiowide guess this file
 *  shipped with: Orbitron (SIL OFL) for the wordmark AND for "(Next-Gen Certified)"
 *  in the strapline — the operator wanted that phrase echoing the logo — and Google
 *  Sans Code (OFL) for "Pioneer_Tech," which the operator wanted thinner than the
 *  wordmark. Each is a single variable-font file; the two weights below are pinned
 *  via XML font-family resources (res/font XML files -> fontVariationSettings), not
 *  Compose's FontVariation API, which is @ExperimentalTextApi and this module
 *  compiles opt-in violations as errors. Body copy stays monospace. */
private val ORBITRON = FontFamily(
    Font(R.font.orbitron_regular, FontWeight.Normal),
    Font(R.font.orbitron_extrabold, FontWeight.ExtraBold),
)
private val GOOGLE_SANS_CODE = FontFamily(
    Font(R.font.google_sans_code_light, FontWeight.Light),
    Font(R.font.google_sans_code_regular, FontWeight.Normal),
    Font(R.font.google_sans_code_bold, FontWeight.Bold),
)

private enum class Glyph { MONITOR, CHAT, SETTINGS, TERMINAL, ARCHIVES, HORIZONS }

private data class Tile(
    val id: String,
    val name: String,
    val slug: String,
    val sub: String,
    val cmd: String,
    val color: Color,
    val bg: Color,
    val glyph: Glyph,
    val panel: Panel,
    /** Where on the card body its cord attaches, as a fraction of the body box. */
    val anchorFx: Float,
    val anchorFy: Float,
    /** Matching pedestal socket, in the crystal's 100 x 100 viewBox. */
    val socketX: Float,
    val socketY: Float,
)

private val TILES = listOf(
    Tile("monitor", "MONITOR", "/cognito", "library", "\$_browser", C_MONITOR, CARD_BG,
        Glyph.MONITOR, Panel.Monitor, 0.50f, 1f, 50f, 63f),      // 12:00
    Tile("chat", "CHAT", "/interface", "tools", "\$_model", C_CHAT, CARD_BG,
        Glyph.CHAT, Panel.Chat, 0.22f, 1f, 68f, 68f),            //  2:00
    Tile("settings", "SETTINGS", "/config", "vault", "\$_utils", C_SETTINGS, CARD_BG,
        Glyph.SETTINGS, Panel.Settings, 0.22f, 0f, 68f, 80f),    //  4:00
    Tile("terminal", "TERMINAL", "/shell", "commands", "\$_bash", C_TERMINAL, TERMINAL_BG,
        Glyph.TERMINAL, Panel.Terminal, 0.50f, 0f, 50f, 85f),    //  6:00
    Tile("archives", "ARCHIVES", "/logs", "artifacts", "\$_files", C_ARCHIVES, CARD_BG,
        Glyph.ARCHIVES, Panel.Artifacts, 0.78f, 0f, 32f, 80f),   //  8:00
    Tile("horizons", "HORIZONS", "/about", "credits", "\$.home", C_HORIZONS, CARD_BG,
        Glyph.HORIZONS, Panel.Horizons, 0.78f, 1f, 32f, 68f),    // 10:00
)

/** Centre of the pedestal disc in the crystal viewBox.
 *
 *  Each cord ends at ITS OWN socket node, nudged only a hair toward this centre —
 *  just far enough that the round stroke cap slips under the platform disc instead
 *  of poking out past the node. It must stay small: pulling the ends further in
 *  makes all six converge on one point in the middle, which is wrong. The six nodes
 *  sit spaced around the platform rim and each cord terminates beneath its own. */
private const val PLATFORM_CX = 50f
private const val PLATFORM_CY = 74f
private const val CORD_TUCK = 0.08f

// ---------------------------------------------------------------------------------
// Root
// ---------------------------------------------------------------------------------

@Composable
fun HomeGrid(
    onTileClick: (Panel) -> Unit,
    modifier: Modifier = Modifier,
) {
    val measurer = rememberTextMeasurer()

    // Measured layout, all in root coordinates. The cord canvas subtracts its own
    // origin so it can draw with them directly.
    var cordOrigin by remember { mutableStateOf(Offset.Zero) }
    var hubBounds by remember { mutableStateOf<Rect?>(null) }
    val anchors = remember { mutableStateMapOf<String, Offset>() }

    Box(
        modifier
            .fillMaxSize()
            .background(BG_DARK)
            .statusBarsPadding()
            .navigationBarsPadding(),
    ) {
        // Layer 1 — static star field + telemetry rings.
        AstralBackdrop(Modifier.fillMaxSize())

        // Layer 2 — plasma cords. Drawn beneath the content layer, which is exactly
        // what makes them vanish under the pedestal at the socket nodes.
        Canvas(
            Modifier
                .fillMaxSize()
                .onGloballyPositioned { c ->
                    val o = c.boundsInRoot().topLeft
                    if (o != cordOrigin) cordOrigin = o
                },
        ) {
            val hub = hubBounds ?: return@Canvas
            val hubCentre = Offset(hub.center.x - cordOrigin.x, hub.center.y - cordOrigin.y)

            TILES.forEach { tile ->
                val startRoot = anchors[tile.id] ?: return@forEach
                val start = Offset(startRoot.x - cordOrigin.x, startRoot.y - cordOrigin.y)

                val sxF = tile.socketX + (PLATFORM_CX - tile.socketX) * CORD_TUCK
                val syF = tile.socketY + (PLATFORM_CY - tile.socketY) * CORD_TUCK
                val end = Offset(
                    hub.left - cordOrigin.x + sxF / 100f * hub.width,
                    hub.top - cordOrigin.y + syF / 100f * hub.height,
                )

                drawCord(start, end, hubCentre, tile.color)
            }
        }

        // Layer 3 — content.
        Column(
            Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp),
        ) {
            HeaderBanner()
            ClockWheel(
                onTileClick = onTileClick,
                measurer = measurer,
                onHubBounds = { r -> if (r != hubBounds) hubBounds = r },
                onAnchor = { id, p -> if (anchors[id] != p) anchors[id] = p },
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(vertical = 4.dp),
            )
            ChatBar()
            Spacer(Modifier.height(6.dp))
            StatusNodes()
            Spacer(Modifier.height(4.dp))
        }
    }
}

// ---------------------------------------------------------------------------------
// Plasma cord — bows away from the hub, four passes, tucks under the platform.
// ---------------------------------------------------------------------------------

private fun DrawScope.drawCord(start: Offset, end: Offset, hubCentre: Offset, color: Color) {
    val dx = end.x - start.x
    val dy = end.y - start.y
    val span = sqrt(dx * dx + dy * dy)

    /* Cubic, not quadratic. A single control point gives one flat bow, which is why
     * the previous pass read as straight runs. Two control points let the cord leave
     * the tile perpendicular to its edge and arrive at the socket along the hub's
     * outward radius — so it rounds off into the corner instead of aiming at it.
     *
     *   c1: straight out of the tile, vertically, away from the card
     *   c2: backed off the socket along the outward radius from the hub centre */
    val c1 = Offset(start.x, start.y + (if (dy > 0f) 1f else -1f) * span * 0.42f)

    val ax = end.x - hubCentre.x
    val ay = end.y - hubCentre.y
    val alen = sqrt(ax * ax + ay * ay)
    val c2 = if (alen > 0.01f) {
        Offset(end.x + ax / alen * span * 0.30f, end.y + ay / alen * span * 0.30f)
    } else {
        Offset(end.x, end.y - span * 0.30f)
    }

    val path = Path().apply {
        moveTo(start.x, start.y)
        cubicTo(c1.x, c1.y, c2.x, c2.y, end.x, end.y)
    }

    val w = size.minDimension / 400f   // keep stroke weight screen-proportional

    // soft "melt" bloom where the cord meets the tile
    drawCircle(color, 9f * w, start, alpha = 0.18f)
    drawCircle(color, 5.5f * w, start, alpha = 0.30f)

    // outer neon halo -> main tube -> inner white laser core
    drawPath(path, color, alpha = 0.25f, style = Stroke(7f * w, cap = StrokeCap.Round))
    drawPath(path, color, alpha = 0.90f, style = Stroke(3.5f * w, cap = StrokeCap.Round))
    drawPath(path, Color.White, alpha = 0.95f, style = Stroke(0.9f * w, cap = StrokeCap.Round))

    // socket dot at the tile end only — the far end is hidden beneath the platform
    drawCircle(color, 2.6f * w, start)
    drawCircle(Color.White, 1.3f * w, start)
}

// ---------------------------------------------------------------------------------
// Backdrop — stars + telemetry rings. 400 x 600 viewBox.
// ---------------------------------------------------------------------------------

@Composable
private fun AstralBackdrop(modifier: Modifier = Modifier) {
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
        val ru = minOf(sx, sy)

        stars.forEach { (pos, ra, teal) ->
            val (r, a) = ra
            drawCircle(
                color = if (teal) C_MONITOR else Color.White,
                radius = r * ru,
                center = p(pos.x, pos.y),
                alpha = a,
            )
        }

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

        ring(200f, 240f, 65f, 0.6f, 0.18f, floatArrayOf(3f, 3f))
        ring(200f, 240f, 105f, 0.5f, 0.12f, floatArrayOf(6f, 4f))
        ring(200f, 240f, 145f, 0.5f, 0.08f)

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
    }
}

// ---------------------------------------------------------------------------------
// Header banner
// ---------------------------------------------------------------------------------

@Composable
private fun HeaderBanner() {
    val d = LocalDensity.current
    val big = with(d) { (LOGO_DP * 0.75f).toSp() }
    val small = with(d) { (LOGO_DP * 0.58f).toSp() }
    val sloganSp = with(d) { SLOGAN_DP.toSp() }

    Column(
        Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(2.dp))

        // Plain strings here, not logoText()'s bracket-shrink treatment: that
        // helper compensates for monospace brackets/parens running full
        // ascender-to-descender, which is a monospace-specific problem Orbitron
        // doesn't have. The operator approved this wordmark exactly as-is —
        // one uniform size, no per-glyph scaling — so this matches that build.
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                "MØ[)u14R",
                color = Color(0xFF5EEAD4),
                fontSize = big,
                lineHeight = big * 1.05f,
                fontFamily = ORBITRON,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 0.sp,
            )
            Text(
                "_11(",
                color = C_MONITOR,
                fontSize = small,
                lineHeight = small * 1.05f,
                fontFamily = ORBITRON,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 0.sp,
            )
        }

        Spacer(Modifier.height(1.dp))

        // Split-font strapline: "Pioneer_Tech," in Google Sans Code (thinner —
        // Normal, not the wordmark's ExtraBold), "(Next-Gen Certified)" in
        // Orbitron to echo the logo above it. Gap tightened per operator note.
        Row(
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "*Pioneer_Tech,",
                color = Color(0xFF5EEAD4),
                fontSize = sloganSp,
                lineHeight = sloganSp * 1.05f,
                fontFamily = GOOGLE_SANS_CODE,
                fontWeight = FontWeight.Normal,
            )
            Text(
                "(Next-Gen Certified)",
                color = Color(0xFF99F6E4),
                fontSize = sloganSp,
                lineHeight = sloganSp * 1.05f,
                fontFamily = ORBITRON,
                fontWeight = FontWeight.ExtraBold,
            )
        }

        Text(
            "HORIZONS // V4",
            color = C_MONITOR.copy(alpha = 0.5f),
            fontSize = with(d) { 9.dp.toSp() },
            lineHeight = with(d) { 9.dp.toSp() } * 1.05f,
            fontFamily = MONO,
            maxLines = 1,
            textAlign = TextAlign.End,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 2.dp, end = 4.dp),
        )

        Spacer(Modifier.height(3.dp))
        Box(
            Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(VIOLET.copy(alpha = 0.15f)),
        )
    }
}

// ---------------------------------------------------------------------------------
// Clock wheel
// ---------------------------------------------------------------------------------

@Composable
private fun ClockWheel(
    onTileClick: (Panel) -> Unit,
    measurer: TextMeasurer,
    onHubBounds: (Rect) -> Unit,
    onAnchor: (String, Offset) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier, contentAlignment = Alignment.Center) {
        RouterHub(onClick = { onTileClick(Panel.Router) }, onHubBounds = onHubBounds)

        TileAt(TILES[0], Alignment.TopCenter, 0.dp, TOP_MID_Y, onTileClick, measurer, onAnchor)
        TileAt(TILES[1], Alignment.TopEnd, -SIDE_X, TOP_SIDE_Y, onTileClick, measurer, onAnchor)
        TileAt(TILES[2], Alignment.BottomEnd, -SIDE_X, BOT_SIDE_Y, onTileClick, measurer, onAnchor)
        TileAt(TILES[3], Alignment.BottomCenter, 0.dp, BOT_MID_Y, onTileClick, measurer, onAnchor)
        TileAt(TILES[4], Alignment.BottomStart, SIDE_X, BOT_SIDE_Y, onTileClick, measurer, onAnchor)
        TileAt(TILES[5], Alignment.TopStart, SIDE_X, TOP_SIDE_Y, onTileClick, measurer, onAnchor)
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
    onAnchor: (String, Offset) -> Unit,
) {
    TileCard(
        tile = tile,
        measurer = measurer,
        onClick = { onTileClick(tile.panel) },
        onAnchor = onAnchor,
        modifier = Modifier
            .align(align)
            .offset(x = dx, y = dy),
    )
}

// ---------------------------------------------------------------------------------
// Tile card
// ---------------------------------------------------------------------------------

@Composable
private fun TileCard(
    tile: Tile,
    measurer: TextMeasurer,
    onClick: () -> Unit,
    onAnchor: (String, Offset) -> Unit,
    modifier: Modifier = Modifier,
) {
    val protrude = ICON_SZ / 2

    Box(
        modifier
            .width(CARD_W + GLOW_PAD * 2)
            .height(CARD_H + protrude + GLOW_PAD),
        contentAlignment = Alignment.TopCenter,
    ) {
        // ---- outward border glow --------------------------------------------
        // Concentric rounded-rect strokes stepping OUTWARD from the card edge into
        // the bleed room. Nothing is drawn inside the card, so the edge reads as
        // lit from behind rather than as an inner rim.
        Canvas(Modifier.fillMaxSize()) {
            val pad = GLOW_PAD.toPx()
            val l = pad
            val t = protrude.toPx()
            val w = CARD_W.toPx()
            val h = CARD_H.toPx()
            val r12 = 12.dp.toPx()
            for (i in 4 downTo 1) {
                val e = i * (pad / 4f)
                drawRoundRect(
                    tile.color,
                    Offset(l - e, t - e),
                    Size(w + e * 2f, h + e * 2f),
                    CornerRadius(r12 + e),
                    alpha = 0.16f / i,
                    style = Stroke(pad / 2.2f),
                )
            }
        }

        // ---- card body -------------------------------------------------------
        Box(
            Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = GLOW_PAD)
                .width(CARD_W)
                .height(CARD_H)
                .onGloballyPositioned { c ->
                    val b = c.boundsInRoot()
                    onAnchor(
                        tile.id,
                        Offset(b.left + tile.anchorFx * b.width, b.top + tile.anchorFy * b.height),
                    )
                }
                .clip(RoundedCornerShape(12.dp))
                .background(tile.bg)
                .border(1.dp, tile.color.copy(alpha = 0.6f), RoundedCornerShape(12.dp))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onClick,
                ),
        ) {
            val d = LocalDensity.current
            Column(
                Modifier
                    .fillMaxSize()
                    .padding(start = 5.dp, end = 5.dp, bottom = 7.dp, top = protrude + 6.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    tile.name,
                    color = tile.color,
                    fontSize = with(d) { TITLE_DP.toSp() },
                    lineHeight = with(d) { TITLE_DP.toSp() } * 1.05f,
                    fontFamily = MONO,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp,
                    maxLines = 1,
                    softWrap = false,
                    textAlign = TextAlign.Center,
                )

                // Two lines by design — the operator's note was that "commands" had
                // gone missing, not that it should be dropped. Wrapping keeps the
                // full slug + descriptor on every tile regardless of string length.
                Text(
                    "${tile.slug} · ${tile.sub}",
                    color = Color(0xFF94A3B8),
                    fontSize = with(d) { SUB_DP.toSp() },
                    fontFamily = MONO,
                    fontWeight = FontWeight.Medium,
                    maxLines = 2,
                    lineHeight = with(d) { (SUB_DP + 2.dp).toSp() },
                    textAlign = TextAlign.Center,
                )

                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(tile.color.copy(alpha = 0.13f)),
                )

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
                        fontSize = with(d) { CMD_DP.toSp() },
                        lineHeight = with(d) { CMD_DP.toSp() } * 1.05f,
                        fontFamily = MONO,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        softWrap = false,
                    )
                    Text(
                        "⚙",
                        color = tile.color.copy(alpha = 0.6f),
                        fontSize = with(d) { CMD_DP.toSp() },
                        lineHeight = with(d) { CMD_DP.toSp() } * 1.05f,
                        fontFamily = MONO,
                        maxLines = 1,
                    )
                }
            }
        }

        // ---- protruding glyph + back-glow ------------------------------------
        Box(
            Modifier
                .align(Alignment.TopCenter)
                .size(ICON_SZ),
            contentAlignment = Alignment.Center,
        ) {
            Canvas(Modifier.fillMaxSize()) {
                drawCircle(
                    brush = Brush.radialGradient(
                        colorStops = arrayOf(
                            0.00f to tile.color.copy(alpha = 0.55f),
                            0.45f to tile.color.copy(alpha = 0.26f),
                            0.75f to tile.color.copy(alpha = 0.09f),
                            1.00f to Color.Transparent,
                        ),
                        center = center,
                        radius = size.minDimension / 2f,
                    ),
                    radius = size.minDimension / 2f,
                )
            }
            Canvas(Modifier.size(ICON_SZ * 0.88f)) {
                drawGlyph(tile.glyph, measurer)
            }
        }
    }
}

// ---------------------------------------------------------------------------------
// Tile glyphs — 36 x 36 viewBox
// ---------------------------------------------------------------------------------

private fun DrawScope.drawGlyph(glyph: Glyph, measurer: TextMeasurer) {
    val u = size.minDimension / 36f
    fun p(x: Float, y: Float) = Offset(x * u, y * u)
    fun line(x1: Float, y1: Float, x2: Float, y2: Float, c: Color, w: Float, a: Float = 1f) =
        drawLine(c, p(x1, y1), p(x2, y2), w * u, StrokeCap.Round, alpha = a)

    when (glyph) {
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
// Router hub
// ---------------------------------------------------------------------------------

@Composable
private fun RouterHub(onClick: () -> Unit, onHubBounds: (Rect) -> Unit) {
    val crystalSize = 110.dp * CRYSTAL_SCALE

    /* The hub used to be a Column of [crystal, label]. Centring that Column centres
     * the PAIR, which pushes the crystal itself above centre by half the label's
     * height — the operator's "router hub is way off centred to the top".
     *
     * Now the Box is sized by the crystal alone, so the crystal is what gets
     * centred, and the label hangs off the bottom edge without displacing it. */
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = null,
            onClick = onClick,
        ),
    ) {
        Box(contentAlignment = Alignment.Center) {
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
            // This canvas IS the 100 x 100 viewBox. The cord layer reads its bounds
            // to place the pedestal sockets, so the two can never drift apart.
            Canvas(
                Modifier
                    .size(crystalSize)
                    .onGloballyPositioned { onHubBounds(it.boundsInRoot()) },
            ) { drawCrystal() }
        }

        // ROUTER plate — hangs below the crystal without displacing it.
        //
        // The plate was rendering roughly 3x taller than its own text needed,
        // reading as an oversized empty box. Cause: Text() merges only the
        // parameters you pass onto Material3's default style, and this Column
        // never set lineHeight — so every line kept the THEME's body-text line
        // height (tuned for ~14-16sp) even though the actual text here is 6-11dp.
        // A 7dp-tall line of text was still reserving a ~16sp-tall box around
        // itself, and three of those stacked boxes is what made the plate balloon.
        // Every line below pins lineHeight to its own size so the box hugs the
        // glyphs instead of the theme's default leading.
        // Scaled up 50% from 7/11/6dp to 10/16/9dp, with padding and corner
        // radius following, per the operator: the plate had been tightened past
        // the point of legibility.
        //
        // Offset moved 0.80 -> 0.97 x crystalSize. The pedestal's lower disc
        // bottoms out at 0.90 x crystalSize (platBot in drawCrystal's 100-unit
        // viewBox), so at 0.80 the plate was riding ON the platform and hiding
        // the socket nodes behind it. 0.97 drops it clear with ~10dp of air, so
        // all of the lit sockets stay visible — the operator called those out
        // specifically as something to keep.
        val d = LocalDensity.current
        Column(
            Modifier
                .align(Alignment.TopCenter)
                .offset(y = crystalSize * 0.97f)
                .clip(RoundedCornerShape(9.dp))
                .background(Color(0xFF0A0518).copy(alpha = 0.92f))
                .border(1.dp, VIOLET.copy(alpha = 0.35f), RoundedCornerShape(9.dp))
                .padding(horizontal = 10.dp, vertical = 5.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                "// CORE_HUB",
                color = Color(0xFFC4B5FD),
                fontSize = with(d) { 10.dp.toSp() },
                lineHeight = with(d) { 11.dp.toSp() },
                fontFamily = MONO,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
            )
            Text(
                "ROUTER",
                color = Color.White,
                fontSize = with(d) { 16.dp.toSp() },
                lineHeight = with(d) { 17.dp.toSp() },
                fontFamily = MONO,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.5.sp,
                maxLines = 1,
                modifier = Modifier.padding(top = 2.dp),
            )
            Text(
                "\$_Statio",
                color = Color(0xFFC4B5FD).copy(alpha = 0.8f),
                fontSize = with(d) { 9.dp.toSp() },
                lineHeight = with(d) { 10.dp.toSp() },
                fontFamily = MONO,
                maxLines = 1,
                modifier = Modifier.padding(top = 2.dp),
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

    val platTop = Rect(12f * u, 62f * u, 88f * u, 86f * u)
    val platBot = Rect(12f * u, 66f * u, 88f * u, 90f * u)

    val wall = Path().apply {
        moveTo(platTop.right, 74f * u)
        arcTo(platTop, 0f, 180f, false)
        lineTo(platBot.left, 78f * u)
        arcTo(platBot, 180f, -180f, false)
        close()
    }
    drawPath(wall, Color(0xFF130924))
    drawPath(wall, Color(0xFF7E22CE), style = Stroke(0.8f * u))

    drawOval(Color(0xFF0A0518), platBot.topLeft, platBot.size)
    drawOval(C_MONITOR, platBot.topLeft, platBot.size, alpha = 0.6f, style = Stroke(0.8f * u))

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
    drawOval(
        C_MONITOR, Offset(19f * u, 65f * u), Size(62f * u, 18f * u), alpha = 0.85f,
        style = Stroke(1f * u, pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f * u, 2f * u), 0f)),
    )
    drawOval(Color.White.copy(alpha = 0.08f), Offset(28f * u, 68f * u), Size(44f * u, 12f * u))
    drawOval(VIOLET, Offset(28f * u, 68f * u), Size(44f * u, 12f * u), style = Stroke(0.8f * u))

    listOf(
        50f to 63f, 68f to 68f, 68f to 80f,
        50f to 85f, 32f to 80f, 32f to 68f,
    ).forEach { (nx, ny) ->
        drawCircle(C_MONITOR, 4.5f * u, p(nx, ny), alpha = 0.25f)
        drawCircle(C_MONITOR, 2.4f * u, p(nx, ny), alpha = 0.95f)
        drawCircle(Color.White, 1f * u, p(nx, ny))
    }

    fun facetGradient(path: Path, from: Color, to: Color): Brush {
        val b = path.getBounds()
        return Brush.linearGradient(listOf(from, to), start = b.topLeft, end = b.bottomRight)
    }

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

    drawLine(
        Color.White, p(48f, 12f), p(30f, 28f),
        strokeWidth = 1.5f * u, cap = StrokeCap.Round, alpha = 0.9f,
    )
}

// ---------------------------------------------------------------------------------
// Chat bar — V5: trimmed vertically
// ---------------------------------------------------------------------------------

@Composable
private fun ChatBar() {
    val d = LocalDensity.current
    Row(
        Modifier
            .fillMaxWidth()
            .clip(CircleShape)
            .background(SLATE_950)
            .border(1.dp, C_MONITOR.copy(alpha = 0.4f), CircleShape)
            .padding(horizontal = 14.dp, vertical = 7.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("⊕", color = C_MONITOR, fontSize = with(d) { 14.dp.toSp() }, fontWeight = FontWeight.Bold)
            Text(
                "tap_or_hold  ask //",
                color = Color(0xFF5EEAD4).copy(alpha = 0.8f),
                fontSize = with(d) { 11.dp.toSp() },
                fontFamily = MONO,
                maxLines = 1,
            )
        }
        Text("↑", color = C_MONITOR, fontSize = with(d) { 14.dp.toSp() }, fontWeight = FontWeight.Bold)
    }
}

// ---------------------------------------------------------------------------------
// System status — V5: box shrunk hard, spheres left alone
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
    // Slimmed down again: the header line and the labels now sit on the same row as
    // the spheres rather than stacking above them, the spheres are 28dp instead of
    // 36, and the padding is halved. That is most of the height the operator wanted
    // back off the bottom of the screen.
    val d = LocalDensity.current
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(SLATE_950.copy(alpha = 0.9f))
            .border(1.dp, C_MONITOR.copy(alpha = 0.35f), RoundedCornerShape(10.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            "// SYSTEM_STATUS",
            color = C_MONITOR.copy(alpha = 0.5f),
            fontSize = with(d) { 8.dp.toSp() },
            lineHeight = with(d) { 8.dp.toSp() } * 1.05f,
            fontFamily = MONO,
            fontWeight = FontWeight.Bold,
            letterSpacing = 2.sp,
            maxLines = 1,
        )
        Spacer(Modifier.height(2.dp))
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            STATUS.forEach { node ->
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Canvas(Modifier.size(STATUS_NODE)) {
                        val r = size.minDimension / 2f
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
                    Spacer(Modifier.height(2.dp))
                    Text(
                        node.label,
                        color = if (node.active) node.color else Color(0xFF475569),
                        fontSize = with(d) { STATUS_LABEL_DP.toSp() },
                        lineHeight = with(d) { STATUS_LABEL_DP.toSp() } * 1.05f,
                        fontFamily = MONO,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                    )
                }
            }
        }
    }
}
