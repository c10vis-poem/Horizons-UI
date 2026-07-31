package com.horizons.ui.panels

import android.os.Environment
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.horizons.HorizonsApplication
import com.horizons.ModelImportActivity
import com.horizons.core.state.ConfigStatus
import com.horizons.core.state.allGreen
import com.horizons.core.state.greenLight
import com.horizons.ui.OscilloscopeBackground
import com.horizons.ui.browser.BrowserPane
import com.horizons.ui.theme.HorizonsColors
import java.io.File

private val Accent = HorizonsColors.TileMonitor
private val ReadyGreen = Color(0xFF4CAF50)
private val WarningAmber = Color(0xFFE8A838)
private val ErrorRed = Color(0xFFFF5577)

/**
 * Full-screen views reachable from the Monitor's screen. Console is the
 * default face; the others pop out over it and return with ←.
 */
private enum class MonitorPopout(val label: String) {
    Console("CONSOLE"),
    Terminal("TERMINAL"),
    Browser("BROWSER"),
}

@Composable
fun MonitorPane(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val ctx = LocalContext.current
    val app = ctx.applicationContext as HorizonsApplication
    val backendStatus by app.llmRuntime.backendStatus.collectAsState()
    val configs by app.routerConfigs.configs.collectAsState()
    val clipboardManager = LocalClipboardManager.current

    val modelsDir = File(app.filesDir, "models")
    val downloadDir = File(
        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).path
    )

    val modelFiles = remember(configs.hashCode()) {
        val files = mutableListOf<File>()
        listOf(modelsDir, downloadDir).forEach { dir ->
            if (dir.isDirectory) {
                dir.listFiles()?.filter { f ->
                    f.isFile && ModelImportActivity.MODEL_EXTENSIONS.any {
                        f.name.lowercase().endsWith(it)
                    }
                }?.let { files.addAll(it) }
            }
        }
        files.sortedByDescending { it.lastModified() }
    }

    val runtimeFiles = remember(configs.hashCode()) {
        val files = mutableListOf<File>()
        listOf(app.filesDir, downloadDir).forEach { dir ->
            if (dir.isDirectory) {
                dir.listFiles()?.filter { f ->
                    f.isFile && ModelImportActivity.RUNTIME_FILES.any {
                        f.name.equals(it, ignoreCase = true)
                    }
                }?.let { files.addAll(it) }
            }
        }
        files.sortedBy { it.name }
    }

    // The Monitor's screen has pop-out full-screen views: the console (default),
    // the main browser, and the terminal. Each is one "tab" on the screen — a
    // plain swap, no nested tab framework.
    var popout by remember { mutableStateOf(MonitorPopout.Console) }

    Box(modifier = modifier.fillMaxSize()) {
        OscilloscopeBackground()
        if (popout != MonitorPopout.Console) {
            Column(Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(start = 4.dp, top = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(onClick = { popout = MonitorPopout.Console }) {
                        Text("←", fontSize = 20.sp, color = Accent)
                    }
                    Text(
                        popout.label,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = Accent,
                    )
                }
                when (popout) {
                    MonitorPopout.Browser -> BrowserPane(
                        app = app,
                        accent = Accent,
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                    )
                    // Full-screen terminal without leaving the Monitor. Terminal
                    // remains its own tile at 6:00; this is an express lane.
                    MonitorPopout.Terminal -> TerminalPanel(
                        onBack = { popout = MonitorPopout.Console },
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                    )
                    MonitorPopout.Console -> Unit
                }
            }
            return@Box
        }
        SelectionContainer {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                // ── Header ──────────────────────────────────────────────────
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(onClick = onBack) {
                        Text("←", fontSize = 20.sp, color = Accent)
                    }
                    Text(
                        "MONITOR",
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        color = Accent,
                    )
                    Text(
                        "  / console",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        color = Accent.copy(alpha = 0.5f),
                    )
                    Spacer(Modifier.weight(1f))
                    // Pop-out tabs on the Monitor's screen.
                    listOf(MonitorPopout.Terminal, MonitorPopout.Browser).forEach { target ->
                        Surface(
                            color = Accent.copy(alpha = 0.15f),
                            shape = MaterialTheme.shapes.small,
                            modifier = Modifier
                                .padding(start = 6.dp)
                                .clickable { popout = target },
                        ) {
                            Text(
                                target.label,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                color = Accent,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            )
                        }
                    }
                }

                HorizontalDivider(color = Accent.copy(alpha = 0.2f))

                // ── System Status Dashboard ─────────────────────────────────
                Text(
                    "System Status",
                    style = MaterialTheme.typography.titleMedium,
                    color = Accent,
                    fontFamily = FontFamily.Monospace,
                )

                val isNoBackend = backendStatus.contains("no backend")
                val isCloud = backendStatus.contains("Cloud")
                val isNpu = backendStatus.contains("Hexagon") || backendStatus.contains("NPU")
                val activeModel = app.resolveNpuModelPath()

                val readyCount = configs.count { it.isReady || it.status == ConfigStatus.RUNNING }
                val runningCount = configs.count { it.status == ConfigStatus.RUNNING }
                val incompleteCount = configs.count {
                    !it.isReady && it.status != ConfigStatus.ARCHIVED && it.status != ConfigStatus.SLEEPING
                }

                Surface(
                    color = HorizonsColors.Surface,
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        StatusRow(
                            label = "Backend",
                            value = backendStatus,
                            color = when {
                                isNoBackend -> ErrorRed
                                isCloud -> WarningAmber
                                isNpu -> ReadyGreen
                                else -> Accent
                            },
                        )
                        StatusRow(
                            label = "Active model",
                            value = activeModel?.substringAfterLast("/") ?: "none",
                            color = if (activeModel != null) ReadyGreen else ErrorRed,
                        )
                        StatusRow(
                            label = "Router configs",
                            value = "$readyCount ready / $runningCount running / $incompleteCount incomplete",
                            color = if (readyCount > 0) ReadyGreen else WarningAmber,
                        )
                        StatusRow(
                            label = "Models on disk",
                            value = "${modelFiles.size} files",
                            color = if (modelFiles.isNotEmpty()) ReadyGreen else WarningAmber,
                        )
                        StatusRow(
                            label = "Runtimes on disk",
                            value = if (runtimeFiles.isEmpty()) "none" else runtimeFiles.joinToString { it.name },
                            color = if (runtimeFiles.isNotEmpty()) ReadyGreen else WarningAmber,
                        )
                    }
                }

                HorizontalDivider(color = Accent.copy(alpha = 0.2f))

                // ── Model Library ───────────────────────────────────────────
                Text(
                    "Model Library",
                    style = MaterialTheme.typography.titleMedium,
                    color = Accent,
                    fontFamily = FontFamily.Monospace,
                )
                Text(
                    "Browse models and runtimes on disk. Tap to copy path.",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                )

                if (modelFiles.isEmpty() && runtimeFiles.isEmpty()) {
                    Surface(
                        color = HorizonsColors.Surface,
                        shape = MaterialTheme.shapes.medium,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(
                            "No model or runtime files found.\n" +
                                "Import via Settings (vault) or use \"Open with → Horizons\" on any\n" +
                                ".gguf / .onnx / .bin / .dlc / .pte / .tflite / .qnn file.",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                            modifier = Modifier.padding(16.dp),
                        )
                    }
                }

                if (runtimeFiles.isNotEmpty()) {
                    Text(
                        "RUNTIMES",
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp,
                        color = Accent.copy(alpha = 0.6f),
                    )
                    runtimeFiles.forEach { file ->
                        val sizeMb = file.length() / (1024 * 1024)
                        val isExec = file.canExecute()
                        LibraryFileCard(
                            name = file.name,
                            path = file.parent ?: "",
                            sizeInfo = "${sizeMb} MB",
                            compatInfo = when {
                                file.name.startsWith("ort_engine") ->
                                    "Runtime: ort_engine (ORT+QNN)" + if (isExec) " ✓ executable" else " ✗ not executable"
                                file.name.startsWith("libonnxruntime") -> "Lib: ONNX Runtime"
                                file.name.startsWith("libQnn") -> "Lib: QNN (Hexagon HTP)"
                                else -> "Runtime component"
                            },
                            compatColor = ReadyGreen,
                            onCopy = {
                                clipboardManager.setText(AnnotatedString(file.absolutePath))
                            },
                        )
                    }
                }

                if (modelFiles.isNotEmpty()) {
                    Text(
                        "MODELS",
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp,
                        color = Accent.copy(alpha = 0.6f),
                    )
                    // pinRev bumps on every plug/unplug so the rows recompose
                    var pinRev by remember { mutableStateOf(0) }
                    val pinnedPath = remember(pinRev) {
                        app.appState.get(com.horizons.core.state.AppStateStore.KEY_ACTIVE_MODEL)
                    }
                    val resolvedPath = remember(pinRev) { app.resolveNpuModelPath() }
                    modelFiles.forEach { file ->
                        val sizeMb = file.length() / (1024 * 1024)
                        val ext = file.extension.lowercase()
                        val pinned = pinnedPath == file.absolutePath
                        val active = resolvedPath == file.absolutePath

                        val (compat, compatClr) = when (ext) {
                            "gguf" -> "Compatible: GenieX (GGML backend)" to ReadyGreen
                            "onnx" -> "Compatible: ort_engine (ORT+QNN EP)" to ReadyGreen
                            "bin" -> "Compatible: ort_engine / GenieX" to ReadyGreen
                            "dlc" -> "Compatible: SNPE / QNN direct" to Accent
                            "pte" -> "Compatible: ExecuTorch" to Accent
                            "tflite" -> "Compatible: TFLite" to Accent
                            "qnn" -> "Compatible: QNN context binary" to ReadyGreen
                            else -> "Unknown format" to WarningAmber
                        }

                        LibraryFileCard(
                            name = file.name,
                            path = file.parent ?: "",
                            sizeInfo = when {
                                pinned -> "◉ PLUGGED IN"
                                active -> "● ACTIVE (auto)"
                                else -> "${sizeMb} MB · detected"
                            },
                            compatInfo = compat + when {
                                pinned -> " — plugged in by you"
                                active -> " — auto-selected (no pin set)"
                                else -> " — landed, awaiting plug-in"
                            },
                            compatColor = if (active) ReadyGreen else compatClr,
                            highlighted = active,
                            onCopy = {
                                clipboardManager.setText(AnnotatedString(file.absolutePath))
                            },
                            plugLabel = if (pinned) "[ UNPLUG ]" else "[ PLUG IN ]",
                            onPlug = {
                                if (pinned) {
                                    app.appState.remove(com.horizons.core.state.AppStateStore.KEY_ACTIVE_MODEL)
                                } else {
                                    app.appState.put(
                                        com.horizons.core.state.AppStateStore.KEY_ACTIVE_MODEL,
                                        file.absolutePath,
                                    )
                                }
                                pinRev++
                            },
                        )
                    }
                }

                HorizontalDivider(color = Accent.copy(alpha = 0.2f))

                // ── Runtime Definitions — shipped from Terminal, checked here,
                //    handed to the Router (the fuse box) only when all green ──
                Text(
                    "Runtime Definitions",
                    style = MaterialTheme.typography.titleMedium,
                    color = Accent,
                    fontFamily = FontFamily.Monospace,
                )

                val runtimeDefs by app.runtimeDefs.defs.collectAsState()
                var handedOff by remember { mutableStateOf<String?>(null) }

                runtimeDefs.forEach { def ->
                    val checks = remember(def.id, handedOff) {
                        def.greenLight(ctx, app.resolveNpuModelPath())
                    }
                    val green = checks.allGreen
                    Surface(
                        color = HorizonsColors.Surface,
                        shape = MaterialTheme.shapes.medium,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    def.name,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = if (green) ReadyGreen else Accent,
                                    modifier = Modifier.weight(1f),
                                )
                                Text(
                                    if (green) "ALL GREEN" else
                                        "${checks.count { !it.ok }} RED",
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 10.sp,
                                    color = if (green) ReadyGreen else WarningAmber,
                                )
                            }
                            Text(
                                ":${def.port}${def.healthPath} · ${def.notes.ifBlank { "no notes" }}",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 9.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                            )
                            checks.forEach { check ->
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        if (check.ok) "●" else "○",
                                        fontSize = 10.sp,
                                        color = if (check.ok) ReadyGreen else WarningAmber,
                                    )
                                    Spacer(Modifier.width(6.dp))
                                    Text(
                                        check.label,
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 10.sp,
                                        color = MaterialTheme.colorScheme.onSurface.copy(
                                            alpha = if (check.ok) 0.7f else 0.9f,
                                        ),
                                        modifier = Modifier.weight(1f),
                                    )
                                    Text(
                                        check.detail.takeLast(28),
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 8.sp,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                                        maxLines = 1,
                                    )
                                }
                            }
                            if (green) {
                                Text(
                                    if (handedOff == def.id) "✓ HANDED TO ROUTER" else "[ HAND TO ROUTER ]",
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 10.sp,
                                    color = ReadyGreen,
                                    modifier = Modifier
                                        .padding(top = 4.dp)
                                        .clickable(enabled = handedOff != def.id) {
                                            app.routerConfigs.add(
                                                com.horizons.core.state.RouterConfig(
                                                    name = def.name,
                                                    runtime = def.name,
                                                    backend = def.binaryName,
                                                    model = app.resolveNpuModelPath() ?: "",
                                                    endpoint = "127.0.0.1:${def.port}${def.healthPath}",
                                                ),
                                            )
                                            handedOff = def.id
                                        }
                                        .padding(vertical = 2.dp),
                                )
                            } else {
                                Text(
                                    "fix the red lights before this can reach the fuse box",
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 9.sp,
                                    color = WarningAmber.copy(alpha = 0.6f),
                                    modifier = Modifier.padding(top = 4.dp),
                                )
                            }
                        }
                    }
                }

                HorizontalDivider(color = Accent.copy(alpha = 0.2f))

                // ── Compatibility Guide ─────────────────────────────────────
                Text(
                    "Compatibility Guide",
                    style = MaterialTheme.typography.titleMedium,
                    color = Accent,
                    fontFamily = FontFamily.Monospace,
                )

                Surface(
                    color = HorizonsColors.Surface,
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        CompatRow("GenieX", "GGML backend", ".gguf")
                        CompatRow("GenieX", "QAIRT backend", ".dlc / .qnn")
                        CompatRow("ort_engine", "ORT + QNN EP", ".onnx / .bin")
                        CompatRow("Cloud API", "OpenRouter / custom", "API key + endpoint")
                        CompatRow("Terminal", "Custom script", "Bash / harness")
                        CompatRow("CLI", "On-device", "No model needed")
                    }
                }

                HorizontalDivider(color = Accent.copy(alpha = 0.2f))

                // ── Console ─────────────────────────────────────────────────
                Text(
                    "Console",
                    style = MaterialTheme.typography.titleMedium,
                    color = Accent,
                    fontFamily = FontFamily.Monospace,
                )

                var consoleInput by remember { mutableStateOf("") }
                var consoleOutput by remember { mutableStateOf("ready. type 'help' for commands.") }

                Surface(
                    color = HorizonsColors.IconBackplate,
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 80.dp),
                ) {
                    Text(
                        "> $consoleOutput",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        color = HorizonsColors.TileTerminal,
                        modifier = Modifier.padding(12.dp),
                    )
                }

                OutlinedTextField(
                    value = consoleInput,
                    onValueChange = { consoleInput = it },
                    label = { Text("command") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = {
                        val cmd = consoleInput.trim()
                        consoleOutput = when {
                            cmd == "help" -> "commands: status, models, runtimes, configs, compat"
                            cmd == "status" -> buildString {
                                append("backend: $backendStatus\n")
                                append("active model: ${activeModel?.substringAfterLast("/") ?: "none"}\n")
                                append("models: ${modelFiles.size} files\n")
                                append("runtimes: ${runtimeFiles.size} files\n")
                                append("router configs: ${configs.size} total, $readyCount ready, $runningCount running")
                            }
                            cmd == "models" -> if (modelFiles.isEmpty()) "no model files found"
                                else modelFiles.joinToString("\n") { "${it.name} (${it.length() / (1024 * 1024)} MB)" }
                            cmd == "runtimes" -> if (runtimeFiles.isEmpty()) "no runtime files found"
                                else runtimeFiles.joinToString("\n") { "${it.name} (${it.length() / (1024 * 1024)} MB)" }
                            cmd == "configs" -> if (configs.isEmpty()) "no router configs"
                                else configs.joinToString("\n") { "${it.name} [${it.status.name}] - ${it.runtime.ifBlank { "no runtime" }}" }
                            cmd == "compat" -> "gguf->GenieX | onnx/bin->ort_engine | dlc/qnn->QNN | cloud->API key+endpoint"
                            cmd.startsWith("load ") -> "use Router to load configs — this is a read-only console"
                            else -> "unknown: $cmd — type 'help'"
                        }
                        consoleInput = ""
                    }),
                )

                Spacer(Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun StatusRow(label: String, value: String, color: Color) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top,
    ) {
        Text(
            label,
            fontFamily = FontFamily.Monospace,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            modifier = Modifier.width(120.dp),
        )
        Text(
            value,
            fontFamily = FontFamily.Monospace,
            fontSize = 11.sp,
            color = color,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun LibraryFileCard(
    name: String,
    path: String,
    sizeInfo: String,
    compatInfo: String,
    compatColor: Color,
    highlighted: Boolean = false,
    onCopy: () -> Unit,
    plugLabel: String? = null,
    onPlug: (() -> Unit)? = null,
) {
    var copied by remember { mutableStateOf(false) }
    Surface(
        color = if (highlighted) Accent.copy(alpha = 0.1f) else HorizonsColors.Surface,
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                onCopy()
                copied = true
            },
    ) {
        Column(Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    name,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (highlighted) Accent else MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    modifier = Modifier.weight(1f).padding(end = 8.dp),
                )
                Text(
                    if (copied) "COPIED" else sizeInfo,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.sp,
                    color = if (copied) ReadyGreen
                        else if (highlighted) ReadyGreen
                        else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                )
            }
            Text(
                compatInfo,
                fontFamily = FontFamily.Monospace,
                fontSize = 10.sp,
                color = compatColor.copy(alpha = 0.8f),
            )
            Text(
                path,
                fontFamily = FontFamily.Monospace,
                fontSize = 9.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
            )
            if (plugLabel != null && onPlug != null) {
                Text(
                    plugLabel,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (highlighted) WarningAmber else ReadyGreen,
                    modifier = Modifier
                        .padding(top = 6.dp)
                        .clickable(onClick = onPlug)
                        .padding(vertical = 2.dp),
                )
            }
        }
    }
}

@Composable
private fun CompatRow(runtime: String, backend: String, formats: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            runtime,
            fontFamily = FontFamily.Monospace,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = Accent,
            modifier = Modifier.width(90.dp),
        )
        Text(
            backend,
            fontFamily = FontFamily.Monospace,
            fontSize = 10.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            modifier = Modifier.width(110.dp),
        )
        Text(
            formats,
            fontFamily = FontFamily.Monospace,
            fontSize = 10.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
        )
    }
}
