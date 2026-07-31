package com.horizons.ui

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.horizons.HorizonsApplication
import com.horizons.Panel
import com.horizons.ui.theme.HorizonsColors
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

    LaunchedEffect(backendStatus) {
        val bad = listOf("error", "fail", "crash", "dead", "unavailable")
        if (bad.any { backendStatus.contains(it, ignoreCase = true) }) {
            goatReason = backendStatus
            showGoat = true
            playGoatBleat()
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawAstralBackground(stars)
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(12.dp))

            // Banner
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
                Text(
                    "MØ[)u14R_11(",
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Black,
                    fontSize = 44.sp,
                    letterSpacing = 1.sp,
                    color = HorizonsColors.PrimaryTeal,
                    maxLines = 1,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
                Text(
                    "*Pioneer_Tech,  (Next-Gen Certified)",
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = HorizonsColors.PrimaryTeal,
                    maxLines = 1,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(2.dp))
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
            Spacer(Modifier.height(16.dp))

            // Top row: Horizons · Monitor · Chat
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                TileCard(
                    name = "HORIZONS",
                    slug = "/about",
                    subtitle = "credits",
                    color = HorizonsColors.TileHorizons,
                    tileType = TileType.HORIZONS,
                    cmdHint = "\$_.home",
                    onClick = { onTileClick(Panel.Horizons) },
                    modifier = Modifier.weight(1f),
                )
                Spacer(Modifier.width(8.dp))
                TileCard(
                    name = "MONITOR",
                    slug = "/cognito",
                    subtitle = "library",
                    color = HorizonsColors.TileMonitor,
                    tileType = TileType.MONITOR,
                    cmdHint = "\$_browser",
                    onClick = { onTileClick(Panel.Monitor) },
                    modifier = Modifier.weight(1f),
                )
                Spacer(Modifier.width(8.dp))
                TileCard(
                    name = "CHAT",
                    slug = "/interface",
                    subtitle = "tools",
                    color = HorizonsColors.TileChat,
                    tileType = TileType.CHAT,
                    cmdHint = "\$_model",
                    onClick = { onTileClick(Panel.Chat) },
                    modifier = Modifier.weight(1f),
                )
            }

            Spacer(Modifier.height(16.dp))

            // Center Router hub
            Box(
                modifier = Modifier.size(140.dp),
                contentAlignment = Alignment.Center,
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    drawCoreHubCrystal()
                }
                Box(
                    modifier = Modifier
                        .size(90.dp)
                        .clickable { onTileClick(Panel.Router) },
                    contentAlignment = Alignment.Center,
                ) {}
            }
            Text(
                "// CORE_HUB",
                fontFamily = FontFamily.Monospace,
                fontSize = 9.sp,
                color = HorizonsColors.TileRouter.copy(alpha = 0.6f),
            )
            Text(
                "ROUTER",
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = Color.White,
            )
            Text(
                "\$_Statio",
                fontFamily = FontFamily.Monospace,
                fontSize = 9.sp,
                color = HorizonsColors.TileRouter.copy(alpha = 0.7f),
            )

            Spacer(Modifier.height(16.dp))

            // Bottom row: Settings · Terminal · Archives
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                TileCard(
                    name = "SETTINGS",
                    slug = "/config",
                    subtitle = "vault",
                    color = HorizonsColors.TileSettings,
                    tileType = TileType.SETTINGS,
                    cmdHint = "\$_utils",
                    onClick = { onTileClick(Panel.Settings) },
                    modifier = Modifier.weight(1f),
                )
                Spacer(Modifier.width(8.dp))
                TileCard(
                    name = "TERMINAL",
                    slug = "/shell",
                    subtitle = "commands",
                    color = HorizonsColors.TileTerminal,
                    tileType = TileType.TERMINAL,
                    cmdHint = "\$_bash",
                    onClick = { onTileClick(Panel.Terminal) },
                    modifier = Modifier.weight(1f),
                )
                Spacer(Modifier.width(8.dp))
                TileCard(
                    name = "ARCHIVES",
                    slug = "/logs",
                    subtitle = "artifacts",
                    color = HorizonsColors.TileArtifacts,
                    tileType = TileType.ARTIFACTS,
                    cmdHint = "\$_files",
                    onClick = { onTileClick(Panel.Artifacts) },
                    modifier = Modifier.weight(1f),
                )
            }

            Spacer(Modifier.weight(1f))

            // Chat bar (ABOVE status nodes)
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .clickable { onTileClick(Panel.Chat) },
                shape = RoundedCornerShape(24.dp),
                color = HorizonsColors.Surface,
                border = BorderStroke(1.dp, HorizonsColors.PrimaryTeal.copy(alpha = 0.35f)),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("⊕", fontSize = 16.sp, color = HorizonsColors.PrimaryTeal)
                    Spacer(Modifier.width(12.dp))
                    Text(
                        "tap_or_hold  ask //",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 13.sp,
                        color = HorizonsColors.PrimaryTeal.copy(alpha = 0.4f),
                        modifier = Modifier.weight(1f),
                    )
                    Text("↑", fontSize = 18.sp, color = HorizonsColors.PrimaryTeal)
                }
            }

            Spacer(Modifier.height(8.dp))

            // Status nodes (BELOW chat bar)
            Surface(
                color = Color(0xFF0A0E12).copy(alpha = 0.85f),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(vertical = 10.dp),
                ) {
                    Text(
                        "// SYSTEM_STATUS",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 9.sp,
                        color = HorizonsColors.PrimaryTeal.copy(alpha = 0.35f),
                    )
                    Spacer(Modifier.height(8.dp))
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

            Spacer(Modifier.height(12.dp))
        }

        // Goat Easter egg overlay
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

// Goat bleat — synthesized, no asset needed

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
        } catch (_: Exception) {}
    }.start()
}

// Astral chart background

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
    drawRect(
        brush = Brush.verticalGradient(
            colors = listOf(Color(0xFF080C10), Color(0xFF0A0E12), Color(0xFF060A0E)),
        ),
    )

    val cx = size.width / 2f
    val cy = size.height * 0.42f

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

    // Telemetry rings around center hub
    val ringColor = Color(0xFF2DD4D9).copy(alpha = 0.03f)
    for (i in 1..4) {
        drawCircle(ringColor, radius = 80f + i * 65f, center = Offset(cx, cy), style = Stroke(0.6f))
    }

    // Extra telemetry circle clusters
    val extraRingColor = Color(0xFF2DD4D9).copy(alpha = 0.025f)
    for (i in 1..3) {
        drawCircle(extraRingColor, radius = 30f + i * 28f, center = Offset(size.width * 0.18f, size.height * 0.22f), style = Stroke(0.5f))
    }
    for (i in 1..3) {
        drawCircle(extraRingColor, radius = 22f + i * 24f, center = Offset(size.width * 0.82f, size.height * 0.72f), style = Stroke(0.5f))
    }
    for (i in 1..2) {
        drawCircle(extraRingColor, radius = 18f + i * 22f, center = Offset(size.width * 0.75f, size.height * 0.15f), style = Stroke(0.4f))
    }

    // Plasma cords from each tile position to center hub
    val hub = Offset(cx, cy)
    val topY = size.height * 0.20f
    val botY = size.height * 0.60f
    val left = size.width * 0.16f
    val mid = cx
    val right = size.width * 0.84f

    drawPlasmaCord(Offset(left, topY), hub, HorizonsColors.TileHorizons)
    drawPlasmaCord(Offset(mid, topY), hub, HorizonsColors.TileMonitor)
    drawPlasmaCord(Offset(right, topY), hub, HorizonsColors.TileChat)
    drawPlasmaCord(Offset(left, botY), hub, HorizonsColors.TileSettings)
    drawPlasmaCord(Offset(mid, botY), hub, HorizonsColors.TileTerminal)
    drawPlasmaCord(Offset(right, botY), hub, HorizonsColors.TileArtifacts)
}

// Plasma cord — glowing curved tube from tile to hub

private fun DrawScope.drawPlasmaCord(from: Offset, hub: Offset, c: Color) {
    val dx = hub.x - from.x
    val dy = hub.y - from.y
    val perpX = -dy * 0.18f
    val perpY = dx * 0.18f
    val cp1 = Offset(from.x + dx * 0.3f + perpX, from.y + dy * 0.3f + perpY)
    val cp2 = Offset(from.x + dx * 0.7f + perpX * 0.5f, from.y + dy * 0.7f + perpY * 0.5f)

    val widths = floatArrayOf(22f, 10f, 4f, 1.6f)
    val alphas = floatArrayOf(0.05f, 0.10f, 0.18f, 0.35f)
    for (layer in widths.indices) {
        val path = Path().apply {
            moveTo(from.x, from.y)
            cubicTo(cp1.x, cp1.y, cp2.x, cp2.y, hub.x, hub.y)
        }
        drawPath(path, c.copy(alpha = alphas[layer]), style = Stroke(width = widths[layer], cap = StrokeCap.Round))
    }

    val steps = 7
    for (i in 1 until steps) {
        val t = i.toFloat() / steps
        val ti = 1f - t
        val bx = ti * ti * ti * from.x + 3f * ti * ti * t * cp1.x + 3f * ti * t * t * cp2.x + t * t * t * hub.x
        val by = ti * ti * ti * from.y + 3f * ti * ti * t * cp1.y + 3f * ti * t * t * cp2.y + t * t * t * hub.y
        val nodeAlpha = 0.25f + sin(t * PI.toFloat()) * 0.30f
        drawCircle(c.copy(alpha = nodeAlpha * 0.75f), 3.5f, Offset(bx, by))
        drawCircle(c.copy(alpha = nodeAlpha * 0.25f), 7f, Offset(bx, by), style = Stroke(0.8f))
    }

    drawCircle(c.copy(alpha = 0.50f), 4f, from)
    drawCircle(c.copy(alpha = 0.18f), 8f, from, style = Stroke(1f))
}

// 3D Hexagonal crystal

private fun DrawScope.drawCoreHubCrystal() {
    val cx = size.width / 2f
    val cy = size.height / 2f
    val W = size.minDimension * 0.14f
    val H = size.minDimension * 0.26f
    val SD = size.minDimension * 0.075f
    val capH = W * 0.50f
    val ox = cx - SD * 0.2f

    // White sun aura
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
    // Violet ambient glow
    for (layer in 3 downTo 0) {
        val glowR = W * (2.8f + layer * 0.7f)
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color(0xFFAA77FF).copy(alpha = 0.10f - layer * 0.02f), Color.Transparent),
                center = Offset(cx, cy),
                radius = glowR,
            ),
            center = Offset(cx, cy),
            radius = glowR,
        )
    }

    val bodyTop = cy - H * 0.30f
    val bodyBot = cy + H * 0.52f
    val peakY = bodyTop - capH
    val botTipY = bodyBot + capH * 0.55f

    val frontFace = Path().apply {
        moveTo(ox - W, bodyTop); lineTo(ox + W, bodyTop)
        lineTo(ox + W, bodyBot); lineTo(ox - W, bodyBot); close()
    }
    drawPath(frontFace, Color(0xFF8855CC).copy(alpha = 0.24f))
    drawPath(frontFace, Color(0xFFAA77FF).copy(alpha = 0.50f), style = Stroke(1.5f))

    val sideFace = Path().apply {
        moveTo(ox + W, bodyTop); lineTo(ox + W + SD, bodyTop - SD * 0.45f)
        lineTo(ox + W + SD, bodyBot - SD * 0.45f); lineTo(ox + W, bodyBot); close()
    }
    drawPath(sideFace, Color(0xFFCC99FF).copy(alpha = 0.14f))
    drawPath(sideFace, Color(0xFFAA77FF).copy(alpha = 0.38f), style = Stroke(1f))

    val capFront = Path().apply {
        moveTo(ox - W, bodyTop); lineTo(ox + W, bodyTop)
        lineTo(ox + W * 0.25f, peakY); lineTo(ox - W * 0.25f, peakY); close()
    }
    drawPath(capFront, Color(0xFFBB99FF).copy(alpha = 0.32f))
    drawPath(capFront, Color(0xFFCC99FF).copy(alpha = 0.60f), style = Stroke(1.2f))

    val capSide = Path().apply {
        moveTo(ox + W, bodyTop); lineTo(ox + W + SD, bodyTop - SD * 0.45f)
        lineTo(ox + W * 0.25f + SD * 0.75f, peakY - SD * 0.22f)
        lineTo(ox + W * 0.25f, peakY); close()
    }
    drawPath(capSide, Color(0xFFDD99FF).copy(alpha = 0.20f))
    drawPath(capSide, Color(0xFFBB88FF).copy(alpha = 0.48f), style = Stroke(1f))

    val botTaper = Path().apply {
        moveTo(ox - W, bodyBot); lineTo(ox + W, bodyBot)
        lineTo(ox + W + SD, bodyBot - SD * 0.45f)
        lineTo(ox + W * 0.1f + SD * 0.5f, botTipY - SD * 0.15f)
        lineTo(ox, botTipY); lineTo(ox - W * 0.1f, botTipY)
        lineTo(ox - W, bodyBot); close()
    }
    drawPath(botTaper, Color(0xFF7744AA).copy(alpha = 0.20f))
    drawPath(botTaper, Color(0xFFAA77FF).copy(alpha = 0.32f), style = Stroke(1f))

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

    drawLine(
        color = Color.White.copy(alpha = 0.38f),
        start = Offset(ox - W * 0.55f, bodyTop + 2f),
        end = Offset(ox - W * 0.20f, bodyTop - capH * 0.45f),
        strokeWidth = 1.3f,
        cap = StrokeCap.Round,
    )
}

// Canvas-drawn tile icons

private enum class TileType { HORIZONS, MONITOR, CHAT, ARTIFACTS, TERMINAL, SETTINGS }

private fun DrawScope.drawTileIcon(type: TileType, color: Color) {
    val cx = size.width / 2f
    val cy = size.height / 2f
    val r = size.minDimension / 2f * 0.8f

    when (type) {
        TileType.HORIZONS -> {
            val lineY = cy + r * 0.22f
            val sunY = lineY - r * 0.38f
            val sunR = r * 0.17f
            drawArc(
                color = Color(0xFFCC99CC),
                startAngle = 180f, sweepAngle = 180f, useCenter = false,
                topLeft = Offset(cx - r * 0.78f, lineY - r * 0.78f),
                size = Size(r * 1.56f, r * 1.0f),
                style = Stroke(width = 1.5f, cap = StrokeCap.Round),
            )
            drawLine(Color(0xFF40C4FF), Offset(cx - r, lineY), Offset(cx + r, lineY), 2f, StrokeCap.Round)
            drawCircle(color = Color(0xFFF5C518), radius = sunR, center = Offset(cx, sunY))
            for (i in 0 until 8) {
                val ang = i * 45f * PI.toFloat() / 180f
                drawLine(
                    Color(0xFFF5C518).copy(alpha = 0.68f),
                    Offset(cx + cos(ang) * (sunR + 2.5f), sunY + sin(ang) * (sunR + 2.5f)),
                    Offset(cx + cos(ang) * (sunR + r * 0.20f), sunY + sin(ang) * (sunR + r * 0.20f)),
                    1.5f, StrokeCap.Round,
                )
            }
        }
        TileType.MONITOR -> {
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
            val lInset = bubbleW * 0.16f
            drawLine(color.copy(alpha = 0.75f), Offset(cx - bubbleW / 2f + lInset, bubbleTop + bubbleH * 0.36f), Offset(cx + bubbleW / 2f - lInset * 1.4f, bubbleTop + bubbleH * 0.36f), 1.6f, StrokeCap.Round)
            drawLine(color.copy(alpha = 0.55f), Offset(cx - bubbleW / 2f + lInset, bubbleTop + bubbleH * 0.62f), Offset(cx + bubbleW / 2f - lInset * 2.2f, bubbleTop + bubbleH * 0.62f), 1.6f, StrokeCap.Round)
            val tailY = bubbleTop + bubbleH
            drawLine(color, Offset(cx - r * 0.34f, tailY + r * 0.34f), Offset(cx, tailY), 2f, StrokeCap.Round)
            drawLine(color, Offset(cx, tailY), Offset(cx + r * 0.34f, tailY + r * 0.34f), 2f, StrokeCap.Round)
            val badgeC = Offset(cx + bubbleW / 2f - r * 0.14f, bubbleTop + r * 0.02f)
            val badgeR = r * 0.30f
            drawCircle(color, badgeR, badgeC)
            drawContext.canvas.nativeCanvas.apply {
                val paint = android.graphics.Paint().apply {
                    this.color = HorizonsColors.IconBackplate.toArgb()
                    textSize = badgeR * 1.05f
                    typeface = android.graphics.Typeface.create(android.graphics.Typeface.MONOSPACE, android.graphics.Typeface.BOLD)
                    textAlign = android.graphics.Paint.Align.CENTER
                    isAntiAlias = true
                }
                val fm = paint.fontMetrics
                drawText("PC", badgeC.x, badgeC.y - (fm.ascent + fm.descent) / 2f, paint)
            }
        }
        TileType.CHAT -> {
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
            val tail = Path().apply {
                moveTo(cx - r * 0.25f, bubTop + bubH - 1f)
                lineTo(cx - r * 0.5f, bubTop + bubH + r * 0.32f)
                lineTo(cx + r * 0.02f, bubTop + bubH - 1f)
                close()
            }
            drawPath(tail, color)
            val dotY = bubTop + bubH * 0.5f
            for (i in -1..1) {
                drawCircle(color.copy(alpha = 0.8f), r * 0.08f, Offset(cx + i * r * 0.4f, dotY))
            }
        }
        TileType.ARTIFACTS -> {
            val docW = r * 1.2f
            val docH = r * 1.4f
            drawRoundRect(color.copy(alpha = 0.3f), Offset(cx - docW / 2f + 4f, cy - docH / 2f - 3f), Size(docW, docH), androidx.compose.ui.geometry.CornerRadius(r * 0.1f), style = Stroke(1.5f))
            drawRoundRect(color, Offset(cx - docW / 2f - 2f, cy - docH / 2f + 3f), Size(docW, docH), androidx.compose.ui.geometry.CornerRadius(r * 0.1f), style = Stroke(2f))
            for (i in 0..2) {
                drawLine(color.copy(alpha = 0.4f), Offset(cx - docW / 2f + 6f, cy - docH / 2f + 16f + i * 8f), Offset(cx + docW / 2f - 10f, cy - docH / 2f + 16f + i * 8f), 1.2f)
            }
        }
        TileType.TERMINAL -> {
            val winW = r * 1.6f
            val winH = r * 1.2f
            val winTop = cy - winH / 2f
            drawRoundRect(color, Offset(cx - winW / 2f, winTop), Size(winW, winH), androidx.compose.ui.geometry.CornerRadius(r * 0.15f), style = Stroke(2f))
            for (i in 0..2) {
                drawCircle(color.copy(alpha = 0.5f), 1.8f, Offset(cx - winW / 2f + 8f + i * 6f, winTop + 5f))
            }
            drawLine(color.copy(alpha = 0.3f), Offset(cx - winW / 2f, winTop + 10f), Offset(cx + winW / 2f, winTop + 10f), 0.8f)
            val promptY = cy + 2f
            drawLine(color, Offset(cx - r * 0.3f, promptY - 4f), Offset(cx - r * 0.05f, promptY + 2f), 2f, StrokeCap.Round)
            drawLine(color, Offset(cx + r * 0.05f, promptY + 2f), Offset(cx + r * 0.3f, promptY + 2f), 2f, StrokeCap.Round)
        }
        TileType.SETTINGS -> {
            val sunR = r * 0.38f
            val ringR = r * 0.62f
            val rayCount = 12
            drawCircle(color = color, radius = sunR, center = Offset(cx, cy))
            val ringOval = androidx.compose.ui.geometry.Rect(cx - ringR, cy - ringR, cx + ringR, cy + ringR)
            for (i in 0 until rayCount) {
                val startAngle = i * 360f / rayCount + 360f / rayCount * 0.25f
                drawArc(color.copy(alpha = 0.55f), startAngle, 360f / rayCount * 0.5f, false, ringOval.topLeft, Size(ringOval.width, ringOval.height), style = Stroke(1.5f, cap = StrokeCap.Butt))
            }
            for (i in 0 until rayCount) {
                val angle = (i * 360f / rayCount) * PI.toFloat() / 180f
                drawLine(color, Offset(cx + cos(angle) * r * 0.70f, cy + sin(angle) * r * 0.70f), Offset(cx + cos(angle) * r * 0.92f, cy + sin(angle) * r * 0.92f), 3.2f, StrokeCap.Butt)
            }
            val bolt = Path().apply {
                moveTo(cx + sunR * 0.10f, cy - sunR * 0.70f)
                lineTo(cx - sunR * 0.25f, cy + sunR * 0.05f)
                lineTo(cx + sunR * 0.05f, cy + sunR * 0.05f)
                lineTo(cx - sunR * 0.10f, cy + sunR * 0.70f)
            }
            drawPath(bolt, Color(0xFFF5C518), style = Stroke(2f, cap = StrokeCap.Round))
        }
    }
}

// Tile card — icon protrudes above card, backlit glow

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
    val iconProtrude = 22.dp
    val iconSize = 44.dp

    Box(modifier = modifier.clickable(onClick = onClick)) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = iconProtrude)
                .drawBehind {
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
                modifier = Modifier.fillMaxSize().padding(start = 8.dp, end = 8.dp, top = iconProtrude + 2.dp, bottom = 6.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    name,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    letterSpacing = 2.sp,
                    color = color,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                )
                Text(
                    "$slug  ·  $subtitle",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 8.sp,
                    color = color.copy(alpha = 0.50f),
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                )
                Spacer(Modifier.weight(1f))
                HorizontalDivider(color = color.copy(alpha = 0.15f))
                Spacer(Modifier.height(3.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(cmdHint, fontFamily = FontFamily.Monospace, fontSize = 8.sp, color = color.copy(alpha = 0.75f))
                    Text("⚙", fontSize = 10.sp, color = color.copy(alpha = 0.45f))
                }
            }
        }

        Box(
            modifier = Modifier.align(Alignment.TopCenter).offset(y = 0.dp),
            contentAlignment = Alignment.Center,
        ) {
            Canvas(modifier = Modifier.size(iconSize + 12.dp)) {
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(color.copy(alpha = 0.30f), color.copy(alpha = 0.08f), Color.Transparent),
                        center = Offset(size.width / 2f, size.height / 2f),
                        radius = size.minDimension / 2f,
                    ),
                    center = Offset(size.width / 2f, size.height / 2f),
                    radius = size.minDimension / 2f,
                )
            }
            Canvas(modifier = Modifier.size(iconSize)) {
                drawTileIcon(tileType, color)
            }
        }
    }
}

// Status dot — 3D glossy sphere

@Composable
private fun StatusDot(label: String, color: Color, active: Boolean) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Canvas(modifier = Modifier.size(42.dp)) {
            val dotR = size.minDimension / 2f * 0.72f
            val center = Offset(size.width / 2f, size.height / 2f)
            if (active) {
                for (i in 4 downTo 0) {
                    drawCircle(color.copy(alpha = 0.06f + i * 0.03f), dotR + i * 4f, center)
                }
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(color.copy(alpha = 0.95f), color, color.copy(alpha = 0.6f)),
                        center = Offset(center.x - dotR * 0.2f, center.y - dotR * 0.2f),
                        radius = dotR * 1.4f,
                    ),
                    radius = dotR, center = center,
                )
                drawCircle(Color.White.copy(alpha = 0.55f), dotR * 0.32f, Offset(center.x - dotR * 0.30f, center.y - dotR * 0.30f))
                drawCircle(Color.White.copy(alpha = 0.20f), dotR * 0.15f, Offset(center.x - dotR * 0.10f, center.y - dotR * 0.50f))
            } else {
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(color.copy(alpha = 0.20f), color.copy(alpha = 0.10f), color.copy(alpha = 0.04f)),
                        center = Offset(center.x - dotR * 0.2f, center.y - dotR * 0.2f),
                        radius = dotR * 1.4f,
                    ),
                    radius = dotR, center = center,
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
