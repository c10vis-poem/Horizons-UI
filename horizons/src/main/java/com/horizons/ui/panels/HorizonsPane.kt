package com.horizons.ui.panels

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.horizons.HorizonsApplication
import com.horizons.ui.AstralSpaceBackground
import com.horizons.ui.theme.HorizonsColors

@Composable
fun HorizonsPane(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val ctx = LocalContext.current
    val app = ctx.applicationContext as HorizonsApplication
    val backendStatus by app.llmRuntime.backendStatus.collectAsState()

    Box(modifier = modifier.fillMaxSize()) {
    AstralSpaceBackground()
    SelectionContainer {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Text("←", fontSize = 20.sp, color = HorizonsColors.TileHorizons)
            }
            Text(
                "HORIZONS",
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                color = HorizonsColors.TileHorizons,
            )
            Text(
                "  / home",
                fontFamily = FontFamily.Monospace,
                fontSize = 12.sp,
                color = HorizonsColors.TileHorizons.copy(alpha = 0.5f),
            )
        }

        HorizontalDivider(color = HorizonsColors.TileHorizons.copy(alpha = 0.2f))

        // Build info
        Surface(
            color = HorizonsColors.Surface,
            shape = MaterialTheme.shapes.medium,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                InfoRow("Model", "MO)u14R_11(")
                InfoRow("Build", "build ${com.horizons.BuildConfig.GIT_SHA}")
                InfoRow("Version", "v${com.horizons.BuildConfig.VERSION_NAME} (${com.horizons.BuildConfig.VERSION_CODE})")
                InfoRow("Backend", backendStatus)
                InfoRow("Architecture", "Snapdragon 8 Elite · SM8750")
                InfoRow("NPU", "Hexagon HTP v79")
                InfoRow("GPU", "Adreno 830")
            }
        }

        HorizontalDivider(color = HorizonsColors.TileHorizons.copy(alpha = 0.2f))

        Text(
            "Open Source Credits",
            style = MaterialTheme.typography.titleMedium,
            color = HorizonsColors.TileHorizons,
            fontFamily = FontFamily.Monospace,
        )

        Surface(
            color = HorizonsColors.Surface,
            shape = MaterialTheme.shapes.medium,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                CreditRow("Qwen3.5-9B", "Apache 2.0", "Qwen Team")
                CreditRow("ONNX Runtime", "MIT", "Microsoft")
                CreditRow("QNN SDK", "Qualcomm License", "Qualcomm")
                CreditRow("Sherpa-ONNX", "Apache 2.0", "k2-fsa")
                CreditRow("Kokoro TTS", "Apache 2.0", "Kokoro Team")
            }
        }

        HorizontalDivider(color = HorizonsColors.TileHorizons.copy(alpha = 0.2f))

        Text(
            "System Condition",
            style = MaterialTheme.typography.titleMedium,
            color = HorizonsColors.TileHorizons,
            fontFamily = FontFamily.Monospace,
        )

        Surface(
            color = HorizonsColors.Surface,
            shape = MaterialTheme.shapes.medium,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                val modelPath = app.resolveNpuModelPath()
                InfoRow("Model file", modelPath ?: "not found")
                InfoRow("Runtime", "ort_engine · QNN Execution Provider")
                InfoRow("Daemon", "CliffordService · FGS specialUse")
            }
        }

        Spacer(Modifier.height(24.dp))
    }
    }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            label,
            fontFamily = FontFamily.Monospace,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
        )
        Text(
            value,
            fontFamily = FontFamily.Monospace,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
private fun CreditRow(name: String, license: String, author: String) {
    Text(
        "$name · $license · $author",
        fontFamily = FontFamily.Monospace,
        fontSize = 13.sp,
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
    )
}
