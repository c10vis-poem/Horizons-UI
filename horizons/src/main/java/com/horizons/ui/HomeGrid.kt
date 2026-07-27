package com.horizons.ui

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import kotlin.math.roundToInt
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.horizons.HorizonsApplication
import com.horizons.Panel
import com.horizons.ui.theme.GoogleSansCode
import com.horizons.ui.theme.HorizonsColors
import com.horizons.ui.theme.Orbitron
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun HomeGrid(
    onTileClick: (Panel) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val app = context.applicationContext as HorizonsApplication
    val backendStatus by app.llmRuntime.backendStatus.collectAsState()

    val npuReady = backendStatus.startsWith("Hexagon HTP") || backendStatus.startsWith("Adreno 830")

    val stars = remember { generateStars(180) }
    var goatTaps by remember { mutableIntStateOf(0) }
    var showGoat by remember { mutableStateOf(false) }
    var goatReason by remember { mutableStateOf<String?>(null) }

    // Goat watchdog — if the runtime comes back broken, the goat delivers the news
    LaunchedEffect(backendStatus) {
        val bad = listOf("error", "fail", "crash", "dead", "unavailable")
        if (bad.any { backendStatus.contains(it, ignoreCase = true) }) {
            goatReason = backendStatus
            showGoat = true
            playGoatBleat()
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        // ── Astral chart background ─────────────────────────────────────────
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawAstralBackground(stars)
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()      // top pad for the notification/status bar
                .navigationBarsPadding(), // bottom pad for the gesture bar
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(12.dp))

            // ── Banner — chunky monospace logo, one-line motto (spec §1) ────
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .clickable {
                        goatTaps++
                        if (goatTaps >= 7) {
                            goatReason = null
                            showGoat = true
                            goatTaps = 0
                            playGoatBleat()
                        }
                    },
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                // Logo — Orbitron ExtraBold 800 at 45sp, the specified face.
                // Orbitron sets much wider than the platform monospace it
                // replaces, so letterSpacing drops to 0 to buy back the width
                // this wordmark needs to stay on one line.
                Text(
                    "MØ[)u14R_11(",
                    fontFamily = Orbitron,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 45.sp,
                    letterSpacing = 0.sp,
                    color = HorizonsColors.PrimaryTeal,
                    maxLines = 1,
                    softWrap = false,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
                // Motto — ONE line, two font runs: "Pioneer_Tech," in Google
                // Sans Code, "(Next-Gen Certified)" in Orbitron to echo the
                // wordmark above it. Both runs share the same size/weight so
                // the switch reads as a typeface change, not an emphasis jump.
                // Single narrow space between runs — squeezed tight on purpose.
                Text(
                    buildAnnotatedString {
                        withStyle(SpanStyle(fontFamily = GoogleSansCode, fontWeight = FontWeight.Normal)) {
                            append("*Pioneer_Tech,")
                        }
                        append(" ")
                        withStyle(SpanStyle(fontFamily = Orbitron, fontWeight = FontWeight.Normal)) {
                            append("(Next-Gen Certified)")
                        }
                    },
                    fontSize = 16.sp,
                    letterSpacing = 0.sp,
                    color = HorizonsColors.PrimaryTeal,
                    maxLines = 1,
                    softWrap = false,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(2.dp))
                // Version — bottom-right
                Text(
                    "HORIZONS // V4",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.sp,
                    color = HorizonsColors.PrimaryTeal.copy(alpha = 0.45f),
                    textAlign = TextAlign.End,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            Spacer(Modifier.height(4.dp))
            HorizontalDivider(
                color = HorizonsColors.NebulaPurple.copy(alpha = 0.15f),
                modifier = Modifier.padding(horizontal = 24.dp),
            )

            // ── Clock-face wheel: 6 tiles + center Router hub (spec §1/§3) ──
            ClockWheel(
                onTileClick = onTileClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
            )


            // ── Input bar (ABOVE config nodes per spec §7) ─────────────────
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onTap = { onTileClick(Panel.Chat) },
                        )
                    },
                shape = RoundedCornerShape(24.dp),
                color = HorizonsColors.Surface,
                border = BorderStroke(1.dp, HorizonsColors.PrimaryTeal.copy(alpha = 0.35f)),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        "⊕",
                        fontSize = 16.sp,
                        color = HorizonsColors.PrimaryTeal,
                    )
                    Spacer(Modifier.width(12.dp))
                    Text(
                        "tap_or_hold  ask //",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 13.sp,
                        color = HorizonsColors.PrimaryTeal.copy(alpha = 0.4f),
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        "↑",
                        fontSize = 18.sp,
                        color = HorizonsColors.PrimaryTeal,
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            // ── System Status Nodes (BELOW chat bar per spec §8) ───────────
            Surface(
                color = HorizonsColors.Surface.copy(alpha = 0.5f),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            ) {
                // Padding trimmed 10→4 and the label gap 10→4. The orbs keep
                // their full size; all the height came back out of dead space.
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(vertical = 4.dp),
                ) {
                    Text(
                        "// SYSTEM_STATUS",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 9.sp,
                        color = HorizonsColors.PrimaryTeal.copy(alpha = 0.35f),
                    )
                    Spacer(Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                    ) {
                        StatusDot("ASR", HorizonsColors.StatusAsr, active = true)
                        StatusDot("LLM", HorizonsColors.StatusLlm, active = npuReady)
                        StatusDot("TTS", HorizonsColors.StatusTts, active = true)
                        StatusDot("MLLM", HorizonsColors.StatusMllm, active = false)
                        StatusDot("VAG", HorizonsColors.StatusVag, active = false)
                    }
                }
            }

            Spacer(Modifier.height(6.dp))
        }

        // ── Goat Easter egg overlay ─────────────────────────────────────────
        if (showGoat) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF000000).copy(alpha = 0.94f))
                    .clickable { showGoat = false },
                contentAlignment = Alignment.Center,
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(32.dp),
                ) {
                    Text("🐐", fontSize = 96.sp, textAlign = TextAlign.Center)
                    Spacer(Modifier.height(12.dp))
                    Text(
                        if (goatReason != null) "// GOAT_SAYS_NO" else "// GOAT_UNLOCKED",
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = if (goatReason != null) HorizonsColors.TileSettings
                        else HorizonsColors.PrimaryTeal,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(Modifier.height(4.dp))
                    if (goatReason != null) {
                        Text(
                            "runtime came back wrong:",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp,
                            color = HorizonsColors.TileSettings.copy(alpha = 0.7f),
                            textAlign = TextAlign.Center,
                        )
                        Text(
                            goatReason ?: "",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            color = HorizonsColors.PrimaryTeal.copy(alpha = 0.6f),
                            textAlign = TextAlign.Center,
                        )
                    } else {
                        Text(
                            "*Pioneer_Tech approved",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp,
                            color = HorizonsColors.PrimaryTeal.copy(alpha = 0.55f),
                            textAlign = TextAlign.Center,
                        )
                        Text(
                            "(Next-Gen Certified)",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            color = HorizonsColors.PrimaryTeal.copy(alpha = 0.4f),
                            textAlign = TextAlign.Center,
                        )
                    }
                    Spacer(Modifier.height(32.dp))
                    Text(
                        "[ tap anywhere to dismiss ]",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 9.sp,
                        color = HorizonsColors.PrimaryTeal.copy(alpha = 0.25f),
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
    }
}

// ── Clock-face wheel — 12/2/4/6/8/10 tile ring + center Router hub ──────────

private data class WheelTile(
    val panel: Panel,
    val name: String,
    val slug: String,
    val subtitle: String,
    val cmd: String,
    val color: Color,
    val type: TileType,
    val xFrac: Float,
    val yFrac: Float,
)

@Composable
private fun ClockWheel(
    onTileClick: (Panel) -> Unit,
    modifier: Modifier = Modifier,
) {
    // 12 Monitor · 2 Chat · 4 Settings · 6 Terminal · 8 Archives · 10 Horizons
    val tiles = listOf(
        WheelTile(Panel.Monitor,   "MONITOR",  "/cognito",   "library",   "\$_browser", HorizonsColors.TileMonitor,   TileType.MONITOR,   0.500f, 0.130f),
        WheelTile(Panel.Chat,      "CHAT",     "/interface", "tools",     "\$_model",   HorizonsColors.TileChat,      TileType.CHAT,      0.792f, 0.285f),
        WheelTile(Panel.Settings,  "SETTINGS", "/config",    "vault",     "\$_utils",   HorizonsColors.TileSettings,  TileType.SETTINGS,  0.792f, 0.595f),
        WheelTile(Panel.Terminal,  "TERMINAL", "/shell",     "commands",  "\$_bash",    HorizonsColors.TileTerminal,  TileType.TERMINAL,  0.500f, 0.750f),
        WheelTile(Panel.Artifacts, "ARCHIVES", "/logs",      "artifacts", "\$_files",   HorizonsColors.TileArtifacts, TileType.ARTIFACTS, 0.208f, 0.595f),
        WheelTile(Panel.Horizons,  "HORIZONS", "/about",     "credits",   "\$_.home",   HorizonsColors.TileHorizons,  TileType.HORIZONS,  0.208f, 0.285f),
    )
    val hubX = 0.5f
    // Raised from 0.44: the crystal's apex should meet the bottom edge of the
    // flanking tiles, so the hub reads as centred inside the wheel rather than
    // sagging below it.
    val hubY = 0.415f

    BoxWithConstraints(modifier = modifier) {
        val w = maxWidth
        val h = maxHeight
        val tileW = 114.dp
        val tileH = 138.dp

        // Plasma cords behind everything — one per tile, in the tile's color
        Canvas(modifier = Modifier.fillMaxSize()) {
            val hub = Offset(hubX * size.width, hubY * size.height)
            tiles.forEach { t ->
                drawPlasmaCord(Offset(t.xFrac * size.width, t.yFrac * size.height), hub, t.color)
            }
        }

        // Center Router hub — violet crystal + white sun aura
        val hubSize = 150.dp
        Box(
            modifier = Modifier
                .size(hubSize)
                .offset(x = w * hubX - hubSize / 2, y = h * hubY - hubSize / 2)
                .clickable { onTileClick(Panel.Router) },
            contentAlignment = Alignment.Center,
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) { drawCoreHubCrystal() }
        }

        // Hub label — // CORE_HUB (top), ROUTER (white), $_Statio.
        // Shrink-wrapped to its text and pushed clear of the crystal: at the
        // old 160dp width and +58dp offset this plate sat over the lower half
        // of the gem and masked the violet aura bleeding out of it. The plate
        // is meant to read as a small tag under the pedestal, not a panel.
        Box(
            modifier = Modifier
                .width(160.dp)
                .offset(x = w * hubX - 80.dp, y = h * hubY + 66.dp),
            contentAlignment = Alignment.TopCenter,
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF0A0518).copy(alpha = 0.88f))
                    .border(
                        width = 1.dp,
                        color = HorizonsColors.TileRouter.copy(alpha = 0.30f),
                        shape = RoundedCornerShape(8.dp),
                    )
                    .padding(horizontal = 10.dp, vertical = 2.dp),
            ) {
                Text(
                    "// CORE_HUB",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 8.sp,
                    color = HorizonsColors.TileRouter.copy(alpha = 0.6f),
                )
                Text(
                    "ROUTER",
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    letterSpacing = 1.sp,
                    color = Color.White,
                )
                Text(
                    "\$_Statio",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 8.sp,
                    color = HorizonsColors.TileRouter.copy(alpha = 0.7f),
                )
            }
        }

        // The six tiles, placed on the clock face
        tiles.forEach { t ->
            TileCard(
                name = t.name,
                slug = t.slug,
                subtitle = t.subtitle,
                color = t.color,
                tileType = t.type,
                cmdHint = t.cmd,
                onClick = { onTileClick(t.panel) },
                modifier = Modifier
                    .width(tileW)
                    .height(tileH)
                    .offset(x = w * t.xFrac - tileW / 2, y = h * t.yFrac - tileH / 2),
            )
        }
    }
}

// ── Goat bleat — synthesized, no asset needed ───────────────────────────────

private fun playGoatBleat() {
    Thread {
        try {
            val sr = 22050
            val durSec = 0.7f
            val n = (sr * durSec).toInt()
            val buf = ShortArray(n)
            var phase = 0f
            for (i in 0 until n) {
                val t = i.toFloat() / sr
                // Vibrato around 260Hz gives the "meh-eh-eh" warble
                val freq = 260f + 45f * sin(2f * PI.toFloat() * 9f * t)
                phase += 2f * PI.toFloat() * freq / sr
                val saw = 2f * ((phase / (2f * PI.toFloat())) % 1f) - 1f
                val tremolo = 0.55f + 0.45f * sin(2f * PI.toFloat() * 11f * t)
                val envelope = (1f - t / durSec).coerceIn(0f, 1f) *
                    (t * 40f).coerceAtMost(1f)
                buf[i] = (saw * tremolo * envelope * 8500f).toInt().toShort()
            }
            val track = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build(),
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setSampleRate(sr)
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build(),
                )
                .setTransferMode(AudioTrack.MODE_STATIC)
                .setBufferSizeInBytes(n * 2)
                .build()
            track.write(buf, 0, n)
            track.play()
            Thread.sleep((durSec * 1000).toLong() + 150)
            track.release()
        } catch (_: Exception) {
            // Sound is garnish — never let it crash the UI
        }
    }.start()
}

// ── Astral chart background ─────────────────────────────────────────────────

private data class Star(val x: Float, val y: Float, val radius: Float, val alpha: Float)

private fun generateStars(count: Int): List<Star> {
    val rng = java.util.Random(42)
    return List(count) {
        Star(
            x = rng.nextFloat(),
            y = rng.nextFloat(),
            radius = 0.5f + rng.nextFloat() * 1.5f,
            alpha = 0.15f + rng.nextFloat() * 0.7f,
        )
    }
}

private fun DrawScope.drawAstralBackground(stars: List<Star>) {
    // Near-black base — matches the standby screen's deep black, NOT gray
    drawRect(
        brush = Brush.verticalGradient(
            colors = listOf(Color(0xFF080C10), Color(0xFF0A0E12), Color(0xFF060A0E)),
        ),
    )

    val cx = size.width / 2f
    val cy = size.height * 0.42f

    // Stars — pinpoint field, subtle, like the standby screen
    stars.forEach { star ->
        val isTeal = star.alpha > 0.5f
        val color = if (isTeal) Color(0xFF2DD4D9).copy(alpha = star.alpha * 0.45f)
        else Color.White.copy(alpha = star.alpha * 0.40f)
        drawCircle(
            color = color,
            radius = star.radius,
            center = Offset(star.x * size.width, star.y * size.height),
        )
    }

    // Faint telemetry rings around center hub
    val ringColor = Color(0xFF2DD4D9).copy(alpha = 0.03f)
    for (i in 1..4) {
        val r = 80f + i * 65f
        drawCircle(
            color = ringColor,
            radius = r,
            center = Offset(cx, cy),
            style = Stroke(width = 0.6f),
        )
    }

    // Extra telemetry circle clusters at different positions (spec §2)
    val extraRingColor = Color(0xFF2DD4D9).copy(alpha = 0.025f)
    val c1x = size.width * 0.18f
    val c1y = size.height * 0.22f
    for (i in 1..3) {
        drawCircle(extraRingColor, radius = 30f + i * 28f, center = Offset(c1x, c1y), style = Stroke(0.5f))
    }
    val c2x = size.width * 0.82f
    val c2y = size.height * 0.72f
    for (i in 1..3) {
        drawCircle(extraRingColor, radius = 22f + i * 24f, center = Offset(c2x, c2y), style = Stroke(0.5f))
    }

    // (Plasma cords are drawn per-tile in ClockWheel now.)
}

// ── Plasma cord — glowing tube from a tile-node into the hub ────────────────

private fun DrawScope.drawPlasmaCord(from: Offset, hub: Offset, c: Color) {
    // Curved bezier — organic flow, not straight protractor lines
    val midX = (from.x + hub.x) / 2f
    val midY = (from.y + hub.y) / 2f
    val dx = hub.x - from.x
    val dy = hub.y - from.y
    // Perpendicular offset for curve bulge
    val perpX = -dy * 0.18f
    val perpY = dx * 0.18f
    val cp1 = Offset(from.x + dx * 0.3f + perpX, from.y + dy * 0.3f + perpY)
    val cp2 = Offset(from.x + dx * 0.7f + perpX * 0.5f, from.y + dy * 0.7f + perpY * 0.5f)

    // 4 stacked glow layers on the curved path
    val widths = floatArrayOf(22f, 10f, 4f, 1.6f)
    val alphas = floatArrayOf(0.05f, 0.10f, 0.18f, 0.35f)
    for (layer in widths.indices) {
        val path = Path().apply {
            moveTo(from.x, from.y)
            cubicTo(cp1.x, cp1.y, cp2.x, cp2.y, hub.x, hub.y)
        }
        drawPath(path, c.copy(alpha = alphas[layer]), style = Stroke(width = widths[layer], cap = StrokeCap.Round))
    }

    // Beads along the bezier curve
    val steps = 7
    for (i in 1 until steps) {
        val t = i.toFloat() / steps
        val ti = 1f - t
        // Cubic bezier point
        val bx = ti * ti * ti * from.x + 3f * ti * ti * t * cp1.x + 3f * ti * t * t * cp2.x + t * t * t * hub.x
        val by = ti * ti * ti * from.y + 3f * ti * ti * t * cp1.y + 3f * ti * t * t * cp2.y + t * t * t * hub.y
        val nodeAlpha = 0.25f + sin(t * PI.toFloat()) * 0.30f
        val nodePos = Offset(bx, by)
        drawCircle(c.copy(alpha = nodeAlpha * 0.75f), 3.5f, nodePos)
        drawCircle(c.copy(alpha = nodeAlpha * 0.25f), 7f, nodePos, style = Stroke(0.8f))
    }

    // Anchor node at the tile end
    drawCircle(c.copy(alpha = 0.50f), 4f, from)
    drawCircle(c.copy(alpha = 0.18f), 8f, from, style = Stroke(1f))
}

// ── 3D Hexagonal crystal ────────────────────────────────────────────────────

private fun DrawScope.drawCoreHubCrystal() {
    val cx = size.width / 2f
    val cy = size.height / 2f
    // SAME color + facet graphics as before — only SHRUNK (was 0.19/0.34/0.10)
    val W   = size.minDimension * 0.105f  // half-width of front face
    val H   = size.minDimension * 0.19f   // body half-height
    val SD  = size.minDimension * 0.055f  // side-face depth (45° perspective)
    val capH = W * 0.50f
    val ox  = cx - SD * 0.2f

    // White sun aura permeating from underneath (spec §3: dome → white sun)
    for (layer in 4 downTo 0) {
        val glowR = W * (2.4f + layer * 1.1f)
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color.White.copy(alpha = 0.13f - layer * 0.024f), Color.Transparent),
                center = Offset(cx, cy),
                radius = glowR,
            ),
            center = Offset(cx, cy),
            radius = glowR,
        )
    }
    // Violet ambient glow (unchanged from the prior build)
    for (layer in 3 downTo 0) {
        val glowR = W * (2.8f + layer * 0.7f)
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color(0xFFAA77FF).copy(alpha = 0.10f - layer * 0.02f),
                    Color.Transparent,
                ),
                center = Offset(cx, cy),
                radius = glowR,
            ),
            center = Offset(cx, cy),
            radius = glowR,
        )
    }

    val bodyTop  = cy - H * 0.30f
    val bodyBot  = cy + H * 0.52f
    val peakY    = bodyTop - capH
    val botTipY  = bodyBot + capH * 0.55f

    // Front face of prism body
    val frontFace = Path().apply {
        moveTo(ox - W, bodyTop)
        lineTo(ox + W, bodyTop)
        lineTo(ox + W, bodyBot)
        lineTo(ox - W, bodyBot)
        close()
    }
    drawPath(frontFace, Color(0xFF8855CC).copy(alpha = 0.24f))
    drawPath(frontFace, Color(0xFFAA77FF).copy(alpha = 0.50f), style = Stroke(width = 1.5f))

    // Right side face (foreshortened at 45°)
    val sideFace = Path().apply {
        moveTo(ox + W,        bodyTop)
        lineTo(ox + W + SD,   bodyTop - SD * 0.45f)
        lineTo(ox + W + SD,   bodyBot - SD * 0.45f)
        lineTo(ox + W,        bodyBot)
        close()
    }
    drawPath(sideFace, Color(0xFFCC99FF).copy(alpha = 0.14f))
    drawPath(sideFace, Color(0xFFAA77FF).copy(alpha = 0.38f), style = Stroke(width = 1f))

    // Top cap — front facet (broad, 30°)
    val capFront = Path().apply {
        moveTo(ox - W,        bodyTop)
        lineTo(ox + W,        bodyTop)
        lineTo(ox + W * 0.25f, peakY)
        lineTo(ox - W * 0.25f, peakY)
        close()
    }
    drawPath(capFront, Color(0xFFBB99FF).copy(alpha = 0.32f))
    drawPath(capFront, Color(0xFFCC99FF).copy(alpha = 0.60f), style = Stroke(width = 1.2f))

    // Top cap — right side facet
    val capSide = Path().apply {
        moveTo(ox + W,                bodyTop)
        lineTo(ox + W + SD,           bodyTop - SD * 0.45f)
        lineTo(ox + W * 0.25f + SD * 0.75f, peakY - SD * 0.22f)
        lineTo(ox + W * 0.25f,        peakY)
        close()
    }
    drawPath(capSide, Color(0xFFDD99FF).copy(alpha = 0.20f))
    drawPath(capSide, Color(0xFFBB88FF).copy(alpha = 0.48f), style = Stroke(width = 1f))

    // Bottom taper (crystal termination)
    val botTaper = Path().apply {
        moveTo(ox - W, bodyBot)
        lineTo(ox + W, bodyBot)
        lineTo(ox + W + SD, bodyBot - SD * 0.45f)
        lineTo(ox + W * 0.1f + SD * 0.5f, botTipY - SD * 0.15f)
        lineTo(ox, botTipY)
        lineTo(ox - W * 0.1f, botTipY)
        lineTo(ox - W, bodyBot)
        close()
    }
    drawPath(botTaper, Color(0xFF7744AA).copy(alpha = 0.20f))
    drawPath(botTaper, Color(0xFFAA77FF).copy(alpha = 0.32f), style = Stroke(width = 1f))

    // Inner glow core
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                Color(0xFFEECCFF).copy(alpha = 0.58f),
                Color(0xFFAA77FF).copy(alpha = 0.20f),
                Color.Transparent,
            ),
            center = Offset(ox, cy - H * 0.05f),
            radius = W * 0.85f,
        ),
        center = Offset(ox, cy - H * 0.05f),
        radius = W * 0.85f,
    )

    // Specular highlight streak (upper-left of front face)
    drawLine(
        color = Color.White.copy(alpha = 0.38f),
        start = Offset(ox - W * 0.55f, bodyTop + 2f),
        end = Offset(ox - W * 0.20f, bodyTop - capH * 0.45f),
        strokeWidth = 1.3f,
        cap = StrokeCap.Round,
    )
}

// ── Canvas-drawn tile icons ─────────────────────────────────────────────────

private enum class TileType { HORIZONS, MONITOR, CHAT, ARTIFACTS, TERMINAL, SETTINGS }

private fun DrawScope.drawTileIcon(type: TileType, color: Color) {
    val cx = size.width / 2f
    val cy = size.height / 2f
    val r = size.minDimension / 2f * 0.8f

    when (type) {
        TileType.HORIZONS -> {
            // Amber sun with rays + blue horizon line + pale pinkish-purple arch
            val lineY = cy + r * 0.22f
            val sunY = lineY - r * 0.38f
            val sunR = r * 0.17f
            // Pale pinkish-purple arch (sky dome)
            drawArc(
                color = Color(0xFFCC99CC),
                startAngle = 180f,
                sweepAngle = 180f,
                useCenter = false,
                topLeft = Offset(cx - r * 0.78f, lineY - r * 0.78f),
                size = Size(r * 1.56f, r * 1.0f),
                style = Stroke(width = 1.5f, cap = StrokeCap.Round),
            )
            // Blue horizon line
            drawLine(
                color = Color(0xFF40C4FF),
                start = Offset(cx - r, lineY),
                end = Offset(cx + r, lineY),
                strokeWidth = 2f,
                cap = StrokeCap.Round,
            )
            // Amber sun disc
            drawCircle(color = Color(0xFFF5C518), radius = sunR, center = Offset(cx, sunY))
            // Sun rays (8)
            for (i in 0 until 8) {
                val ang = i * 45f * PI.toFloat() / 180f
                drawLine(
                    color = Color(0xFFF5C518).copy(alpha = 0.68f),
                    start = Offset(cx + cos(ang) * (sunR + 2.5f), sunY + sin(ang) * (sunR + 2.5f)),
                    end   = Offset(cx + cos(ang) * (sunR + r * 0.20f), sunY + sin(ang) * (sunR + r * 0.20f)),
                    strokeWidth = 1.5f,
                    cap = StrokeCap.Round,
                )
            }
        }
        TileType.MONITOR -> {
            // Display glyph — rounded rect + 2 inner lines + caret tail + PC badge.
            // (spec §2: this is the icon the old build wrongly showed on CHAT; PC, not AI)
            val bubbleW = r * 1.5f
            val bubbleH = r * 1.0f
            val bubbleTop = cy - bubbleH * 0.55f
            drawRoundRect(
                color = color,
                topLeft = Offset(cx - bubbleW / 2f, bubbleTop),
                size = Size(bubbleW, bubbleH),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(r * 0.22f),
                style = Stroke(width = 2f),
            )
            // 2 inner lines (left-aligned)
            val lInset = bubbleW * 0.16f
            val lY1 = bubbleTop + bubbleH * 0.36f
            val lY2 = bubbleTop + bubbleH * 0.62f
            drawLine(color.copy(alpha = 0.75f), Offset(cx - bubbleW / 2f + lInset, lY1), Offset(cx + bubbleW / 2f - lInset * 1.4f, lY1), 1.6f, StrokeCap.Round)
            drawLine(color.copy(alpha = 0.55f), Offset(cx - bubbleW / 2f + lInset, lY2), Offset(cx + bubbleW / 2f - lInset * 2.2f, lY2), 1.6f, StrokeCap.Round)
            // Caret tail below center (the inverted-V stand/tail)
            val tailY = bubbleTop + bubbleH
            drawLine(color, Offset(cx - r * 0.34f, tailY + r * 0.34f), Offset(cx, tailY), 2f, StrokeCap.Round)
            drawLine(color, Offset(cx, tailY), Offset(cx + r * 0.34f, tailY + r * 0.34f), 2f, StrokeCap.Round)
            // "PC" badge — filled circle, top-right
            val badgeC = Offset(cx + bubbleW / 2f - r * 0.14f, bubbleTop + r * 0.02f)
            val badgeR = r * 0.30f
            drawCircle(color, badgeR, badgeC)
            drawContext.canvas.nativeCanvas.apply {
                val paint = android.graphics.Paint().apply {
                    this.color = HorizonsColors.IconBackplate.toArgb()
                    textSize = badgeR * 1.05f
                    typeface = android.graphics.Typeface.create(
                        android.graphics.Typeface.MONOSPACE, android.graphics.Typeface.BOLD,
                    )
                    textAlign = android.graphics.Paint.Align.CENTER
                    isAntiAlias = true
                }
                val fm = paint.fontMetrics
                drawText("PC", badgeC.x, badgeC.y - (fm.ascent + fm.descent) / 2f, paint)
            }
        }
        TileType.CHAT -> {
            // Clean speech bubble (spec §2: simple bubble, not hub-and-spoke)
            val bubW = r * 1.6f
            val bubH = r * 1.05f
            val bubTop = cy - bubH * 0.6f
            drawRoundRect(
                color = color,
                topLeft = Offset(cx - bubW / 2f, bubTop),
                size = Size(bubW, bubH),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(r * 0.3f),
                style = Stroke(width = 2f),
            )
            // Tail (bottom-left)
            val tail = Path().apply {
                moveTo(cx - r * 0.25f, bubTop + bubH - 1f)
                lineTo(cx - r * 0.5f, bubTop + bubH + r * 0.32f)
                lineTo(cx + r * 0.02f, bubTop + bubH - 1f)
                close()
            }
            drawPath(tail, color)
            // 3 dots inside
            val dotY = bubTop + bubH * 0.5f
            for (i in -1..1) {
                drawCircle(color.copy(alpha = 0.8f), r * 0.08f, Offset(cx + i * r * 0.4f, dotY))
            }
        }
        TileType.ARTIFACTS -> {
            // Stacked documents / clipboard
            val docW = r * 1.2f
            val docH = r * 1.4f
            // Back page
            drawRoundRect(
                color = color.copy(alpha = 0.3f),
                topLeft = Offset(cx - docW / 2f + 4f, cy - docH / 2f - 3f),
                size = Size(docW, docH),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(r * 0.1f),
                style = Stroke(width = 1.5f),
            )
            // Front page
            drawRoundRect(
                color = color,
                topLeft = Offset(cx - docW / 2f - 2f, cy - docH / 2f + 3f),
                size = Size(docW, docH),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(r * 0.1f),
                style = Stroke(width = 2f),
            )
            // Lines on front page
            val lineStartX = cx - docW / 2f + 6f
            val lineEndX = cx + docW / 2f - 10f
            for (i in 0..2) {
                val ly = cy - docH / 2f + 16f + i * 8f
                drawLine(
                    color = color.copy(alpha = 0.4f),
                    start = Offset(lineStartX, ly),
                    end = Offset(lineEndX, ly),
                    strokeWidth = 1.2f,
                )
            }
        }
        TileType.TERMINAL -> {
            // Terminal window with >_ prompt
            val winW = r * 1.6f
            val winH = r * 1.2f
            val winTop = cy - winH / 2f
            drawRoundRect(
                color = color,
                topLeft = Offset(cx - winW / 2f, winTop),
                size = Size(winW, winH),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(r * 0.15f),
                style = Stroke(width = 2f),
            )
            // Title bar dots
            val dotY = winTop + 5f
            for (i in 0..2) {
                drawCircle(
                    color = color.copy(alpha = 0.5f),
                    radius = 1.8f,
                    center = Offset(cx - winW / 2f + 8f + i * 6f, dotY),
                )
            }
            // Divider under title bar
            drawLine(
                color = color.copy(alpha = 0.3f),
                start = Offset(cx - winW / 2f, winTop + 10f),
                end = Offset(cx + winW / 2f, winTop + 10f),
                strokeWidth = 0.8f,
            )
            // >_ cursor
            val promptY = cy + 2f
            drawLine(
                color = color,
                start = Offset(cx - r * 0.3f, promptY - 4f),
                end = Offset(cx - r * 0.05f, promptY + 2f),
                strokeWidth = 2f,
                cap = StrokeCap.Round,
            )
            drawLine(
                color = color,
                start = Offset(cx + r * 0.05f, promptY + 2f),
                end = Offset(cx + r * 0.3f, promptY + 2f),
                strokeWidth = 2f,
                cap = StrokeCap.Round,
            )
        }
        TileType.SETTINGS -> {
            // Sun/flash icon: solid circle + yellow bolt + dashed ring + blocky rays
            val sunR = r * 0.38f
            val ringR = r * 0.62f
            val rayCount = 12
            val rayInner = r * 0.70f
            val rayOuter = r * 0.92f
            val rayWidth = 3.2f

            // Solid pink center circle
            drawCircle(color = color, radius = sunR, center = Offset(cx, cy))

            // Dashed circle ring — draw arc segments between the rays
            val ringOval = androidx.compose.ui.geometry.Rect(
                cx - ringR, cy - ringR, cx + ringR, cy + ringR,
            )
            for (i in 0 until rayCount) {
                val startAngle = i * 360f / rayCount + 360f / rayCount * 0.25f
                val sweepAngle = 360f / rayCount * 0.5f
                drawArc(
                    color = color.copy(alpha = 0.55f),
                    startAngle = startAngle,
                    sweepAngle = sweepAngle,
                    useCenter = false,
                    topLeft = ringOval.topLeft,
                    size = Size(ringOval.width, ringOval.height),
                    style = Stroke(width = 1.5f, cap = StrokeCap.Butt),
                )
            }

            // Blocky rectangular ray/tick marks at each position
            for (i in 0 until rayCount) {
                val angle = (i * 360f / rayCount) * PI.toFloat() / 180f
                drawLine(
                    color = color,
                    start = Offset(cx + cos(angle) * rayInner, cy + sin(angle) * rayInner),
                    end = Offset(cx + cos(angle) * rayOuter, cy + sin(angle) * rayOuter),
                    strokeWidth = rayWidth,
                    cap = StrokeCap.Butt,
                )
            }

            // Yellow lightning bolt inside the circle
            val bolt = Path().apply {
                moveTo(cx + sunR * 0.10f, cy - sunR * 0.70f)
                lineTo(cx - sunR * 0.25f, cy + sunR * 0.05f)
                lineTo(cx + sunR * 0.05f, cy + sunR * 0.05f)
                lineTo(cx - sunR * 0.10f, cy + sunR * 0.70f)
            }
            drawPath(bolt, Color(0xFFF5C518), style = Stroke(width = 2f, cap = StrokeCap.Round))
        }
    }
}

// ── Tile card — icon protrudes above the card top + backlit glow ────────────

@Composable
private fun TileCard(
    name: String,
    slug: String,
    subtitle: String,
    color: Color,
    tileType: TileType,
    cmdHint: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val cardBg = if (tileType == TileType.TERMINAL) HorizonsColors.TerminalCardBg
                 else Color(0xFF0A0E11)
    val iconProtrude = 24.dp
    val iconSize = 60.dp

    Box(modifier = modifier.clickable(onClick = onClick)) {
        // The card body — starts below the protruding icon
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = iconProtrude)
                .drawBehind {
                    // Backlit glow radiating from icon position (top-center) — VIBRANT
                    val glowBrush = Brush.radialGradient(
                        colors = listOf(
                            color.copy(alpha = 0.30f),
                            color.copy(alpha = 0.12f),
                            color.copy(alpha = 0.03f),
                            Color.Transparent,
                        ),
                        center = Offset(size.width / 2f, 0f),
                        radius = size.width * 1.1f,
                    )
                    drawRect(glowBrush)
                },
            shape = RoundedCornerShape(10.dp),
            color = cardBg,
            border = BorderStroke(1.dp, color.copy(alpha = 0.28f)),
        ) {
            Column(
                modifier = Modifier.fillMaxSize().padding(start = 8.dp, end = 8.dp, top = iconProtrude + 6.dp, bottom = 6.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                // TITLE & SUBTITLE block — scooted down together, bright title
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Text(
                        name,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 13.sp,
                        letterSpacing = 2.sp,
                        color = color,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                    )
                    Spacer(Modifier.height(2.dp))
                    // /slug · subtitle — muted contrast
                    Text(
                        "$slug  ·  $subtitle",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 8.sp,
                        color = Color.LightGray.copy(alpha = 0.70f),
                        textAlign = TextAlign.Center,
                        maxLines = 2,
                    )
                }
                HorizontalDivider(color = color.copy(alpha = 0.18f))
                Spacer(Modifier.height(3.dp))
                // $_prompt + ⚙ gear
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        cmdHint,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 8.sp,
                        color = color.copy(alpha = 0.85f),
                    )
                    Text(
                        "⚙",
                        fontSize = 10.sp,
                        color = Color.Gray,
                    )
                }
            }
        }

        // Icon — protruding above the card, centered, with backlit glow
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset(y = 0.dp),
            contentAlignment = Alignment.Center,
        ) {
            // Backlit glow behind the icon
            Canvas(modifier = Modifier.size(iconSize + 12.dp)) {
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            color.copy(alpha = 0.30f),
                            color.copy(alpha = 0.08f),
                            Color.Transparent,
                        ),
                        center = Offset(size.width / 2f, size.height / 2f),
                        radius = size.minDimension / 2f,
                    ),
                    center = Offset(size.width / 2f, size.height / 2f),
                    radius = size.minDimension / 2f,
                )
            }
            // The icon itself
            Canvas(modifier = Modifier.size(iconSize)) {
                drawTileIcon(tileType, color)
            }
        }
    }
}

// ── Status dot ──────────────────────────────────────────────────────────────

@Composable
private fun StatusDot(label: String, color: Color, active: Boolean) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Canvas(modifier = Modifier.size(36.dp)) {
            val dotR = size.minDimension / 2f * 0.72f
            val center = Offset(size.width / 2f, size.height / 2f)
            if (active) {
                // Outer glow halo
                for (i in 4 downTo 0) {
                    drawCircle(
                        color = color.copy(alpha = 0.06f + i * 0.03f),
                        radius = dotR + i * 4f,
                        center = center,
                    )
                }
                // Core sphere — 3D glossy gradient (bright center → darker edge)
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            color.copy(alpha = 0.95f),
                            color,
                            color.copy(alpha = 0.6f),
                        ),
                        center = Offset(center.x - dotR * 0.2f, center.y - dotR * 0.2f),
                        radius = dotR * 1.4f,
                    ),
                    radius = dotR,
                    center = center,
                )
                // Specular highlight — upper-left for 3D gloss
                drawCircle(
                    Color.White.copy(alpha = 0.55f),
                    dotR * 0.32f,
                    Offset(center.x - dotR * 0.30f, center.y - dotR * 0.30f),
                )
                // Secondary glint
                drawCircle(
                    Color.White.copy(alpha = 0.20f),
                    dotR * 0.15f,
                    Offset(center.x - dotR * 0.10f, center.y - dotR * 0.50f),
                )
            } else {
                // Inactive — muted sphere, keeps 3D shape
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            color.copy(alpha = 0.20f),
                            color.copy(alpha = 0.10f),
                            color.copy(alpha = 0.04f),
                        ),
                        center = Offset(center.x - dotR * 0.2f, center.y - dotR * 0.2f),
                        radius = dotR * 1.4f,
                    ),
                    radius = dotR,
                    center = center,
                )
            }
        }
        Spacer(Modifier.height(3.dp))
        Text(
            label,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            fontSize = 10.sp,
            color = if (active) color else color.copy(alpha = 0.25f),
        )
    }
}
