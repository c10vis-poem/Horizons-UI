package com.horizons.ui

import android.graphics.BitmapFactory
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.Text
import com.horizons.ui.theme.HorizonsColors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// The screensaver picture is loaded from device storage at runtime, so it can
// be swapped without rebuilding the APK. Drop any image at one of these paths:
private val SAVER_IMAGE_CANDIDATES = listOf(
    "/storage/emulated/0/Pictures/Horizons/screensaver.jpg",
    "/storage/emulated/0/Pictures/Horizons/screensaver.jpeg",
    "/storage/emulated/0/Pictures/Horizons/screensaver.png",
    "/storage/emulated/0/Pictures/Horizons/screensaver.webp",
)

private data class SaverStar(val x: Float, val y: Float, val r: Float, val a: Float, val speed: Float)

private fun generateSaverStars(count: Int): List<SaverStar> {
    val rng = java.util.Random(211)
    return List(count) {
        SaverStar(
            x = rng.nextFloat(),
            y = rng.nextFloat(),
            r = 0.5f + rng.nextFloat() * 1.8f,
            a = 0.1f + rng.nextFloat() * 0.5f,
            speed = 0.2f + rng.nextFloat() * 0.8f,
        )
    }
}

@Composable
fun IdleScreensaver(
    onWake: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var image by remember { mutableStateOf<ImageBitmap?>(null) }
    var clock by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        image = withContext(Dispatchers.IO) {
            try {
                SAVER_IMAGE_CANDIDATES
                    .map(::File)
                    .firstOrNull { it.exists() && it.canRead() }
                    ?.let { BitmapFactory.decodeFile(it.absolutePath)?.asImageBitmap() }
            } catch (_: Exception) {
                null
            }
        }
        val fmt = SimpleDateFormat("HH:mm", Locale.US)
        while (true) {
            clock = fmt.format(Date())
            delay(1_000)
        }
    }

    val transition = rememberInfiniteTransition(label = "saver")
    val drift by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(90_000, easing = LinearEasing)),
        label = "drift",
    )
    val stars = remember { generateSaverStars(90) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .clickable(onClick = onWake),
    ) {
        val img = image
        if (img != null) {
            Image(
                bitmap = img,
                contentDescription = "screensaver",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
            // Dim wash so the clock stays readable over any picture
            Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.25f)))
        } else {
            // No picture on device yet — drifting starfield fallback
            Canvas(Modifier.fillMaxSize()) {
                stars.forEach { s ->
                    val x = ((s.x + drift * s.speed) % 1f) * size.width
                    drawCircle(
                        color = if (s.a > 0.4f) HorizonsColors.PrimaryTeal.copy(alpha = s.a * 0.7f)
                        else Color.White.copy(alpha = s.a * 0.6f),
                        radius = s.r,
                        center = Offset(x, s.y * size.height),
                    )
                }
            }
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(24.dp),
        ) {
            Text(
                clock,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                fontSize = 44.sp,
                color = Color.White.copy(alpha = 0.85f),
            )
            Text(
                "// STANDBY",
                fontFamily = FontFamily.Monospace,
                fontSize = 13.sp,
                color = HorizonsColors.PrimaryTeal.copy(alpha = 0.5f),
            )
            Spacer(Modifier.height(2.dp))
            Text(
                "tap to wake",
                fontFamily = FontFamily.Monospace,
                fontSize = 12.sp,
                color = Color.White.copy(alpha = 0.25f),
            )
        }
    }
}
