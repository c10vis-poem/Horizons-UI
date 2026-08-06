package com.horizons.ui.panels

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings as AndroidSettings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.horizons.HorizonsApplication
import com.horizons.ModelImportActivity
import com.horizons.Panel
import com.horizons.core.state.AppStateStore
import com.horizons.core.state.RouterConfig
import com.horizons.core.storage.StorageScanner
import com.horizons.ui.VaultDoorBackground
import com.horizons.ui.theme.HorizonsColors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

private val Accent = HorizonsColors.TileSettings
private val ReadyGreen = Color(0xFF4CAF50)

@Composable
fun SettingsPane(
    onBack: () -> Unit,
    onNavigate: (Panel) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val ctx = LocalContext.current
    val app = ctx.applicationContext as HorizonsApplication
    val settings by app.settingsStore.snapshot.collectAsState()
    val creds by app.appState.snapshot.collectAsState()
    val scope = rememberCoroutineScope()

    Box(modifier.fillMaxSize()) {
        VaultDoorBackground()
        SelectionContainer {
            Column(
                Modifier
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
                        "SETTINGS",
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        color = Accent,
                    )
                    Text(
                        "  / vault",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        color = Accent.copy(alpha = 0.5f),
                    )
                }

                HorizontalDivider(color = Accent.copy(alpha = 0.2f))

                // ── Import Files ────────────────────────────────────────────
                SectionLabel("Import")
                Text(
                    "Import models, runtimes, and libraries. Files land here, then export to Router when ready.",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                )

                var importStatus by remember { mutableStateOf<String?>(null) }
                var importTick by remember { mutableStateOf(0) }

                val filePickerLauncher = rememberLauncherForActivityResult(
                    ActivityResultContracts.OpenDocument()
                ) { uri ->
                    if (uri == null) return@rememberLauncherForActivityResult
                    val name = queryDisplayName(ctx, uri) ?: uri.lastPathSegment ?: "imported_file"
                    val lower = name.lowercase()
                    val isModel = ModelImportActivity.MODEL_EXTENSIONS.any { lower.endsWith(it) }
                    val isRuntime = lower.startsWith("ort_engine") ||
                        (lower.startsWith("libonnxruntime") && lower.endsWith(".so")) ||
                        (lower.startsWith("libqnn") && lower.endsWith(".so")) ||
                        lower.contains("geniex")
                    if (!isModel && !isRuntime) {
                        importStatus = "Unsupported file: $name"
                        return@rememberLauncherForActivityResult
                    }
                    val canonical = when {
                        isModel -> name
                        lower.startsWith("ort_engine") -> com.horizons.core.shell.DaemonLauncher.ENGINE_BINARY
                        lower.startsWith("libonnxruntime") -> "libonnxruntime.so"
                        lower.startsWith("libqnnhtpv79skel") -> "libQnnHtpV79Skel.so"
                        lower.startsWith("libqnnhtp") -> "libQnnHtp.so"
                        lower.startsWith("libqnnsystem") -> "libQnnSystem.so"
                        else -> name
                    }
                    val modelsDir = File(app.filesDir, "models")
                    val destDir = if (isModel) modelsDir else app.filesDir
                    val executable = isRuntime && canonical == com.horizons.core.shell.DaemonLauncher.ENGINE_BINARY
                    importStatus = "Importing $canonical…"
                    scope.launch {
                        try {
                            withContext(Dispatchers.IO) {
                                destDir.mkdirs()
                                val dest = File(destDir, canonical)
                                ctx.contentResolver.openInputStream(uri)?.use { input ->
                                    dest.outputStream().use { out -> input.copyTo(out) }
                                } ?: throw IllegalStateException("Cannot open input stream")
                                if (executable) dest.setExecutable(true, true)
                            }
                            importStatus = "Imported $canonical → ${destDir.name}/"
                            importTick++
                        } catch (e: Exception) {
                            importStatus = "Import failed: ${e.message}"
                        }
                    }
                }

                Button(
                    onClick = { filePickerLauncher.launch(arrayOf("*/*")) },
                    colors = ButtonDefaults.buttonColors(containerColor = Accent),
                ) {
                    Text("Browse for model or runtime file…", fontFamily = FontFamily.Monospace, fontSize = 12.sp)
                }

                importStatus?.let { msg ->
                    Text(
                        msg,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    )
                }

                // ── Scan device storage ────────────────────────────────────
                //
                // Bulk companion to the single-file picker above. The operator's
                // GenieX SDK, model shards, and .so libraries usually live in a
                // folder on internal storage (e.g. LeGRAND_REPOSITORY/) — the
                // single-file "Open with" flow makes those invisible to the app
                // because they were never dispatched to it. This walks common
                // storage roots and lists what it finds, so each file gets a
                // one-tap import into the app's own directories.
                Spacer(Modifier.height(4.dp))
                Text(
                    "Or scan device storage for existing models / libraries.",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                )

                var scanResults by remember { mutableStateOf<List<StorageScanner.Group>>(emptyList()) }
                var scanning by remember { mutableStateOf(false) }
                var scanError by remember { mutableStateOf<String?>(null) }
                var scanTick by remember { mutableStateOf(0) }
                val canScan = remember(scanTick) { StorageScanner.canScan() }

                if (!canScan) {
                    Text(
                        "MANAGE_EXTERNAL_STORAGE is required. Grant it in Android Settings, then return.",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp,
                        color = Color(0xFFF5C518),
                    )
                    Button(
                        onClick = {
                            try {
                                val intent = Intent(AndroidSettings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
                                    data = Uri.parse("package:${ctx.packageName}")
                                }
                                ctx.startActivity(intent)
                            } catch (_: Throwable) {
                                ctx.startActivity(Intent(AndroidSettings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION))
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF5C518)),
                    ) {
                        Text(
                            "Grant storage access →",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp,
                            color = Color.Black,
                        )
                    }
                    Button(
                        onClick = { scanTick++ },
                        colors = ButtonDefaults.buttonColors(containerColor = Accent.copy(alpha = 0.3f)),
                    ) {
                        Text(
                            "I've granted it — check again",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            color = Accent,
                        )
                    }
                } else {
                    Button(
                        onClick = {
                            scanning = true
                            scanError = null
                            scope.launch {
                                try {
                                    scanResults = StorageScanner.scan()
                                    if (scanResults.isEmpty()) {
                                        scanError = "Scanned Download, Documents, LeGRAND_REPOSITORY and root — no model or library files found."
                                    }
                                } catch (e: Throwable) {
                                    scanError = "Scan failed: ${e.message}"
                                } finally {
                                    scanning = false
                                }
                            }
                        },
                        enabled = !scanning,
                        colors = ButtonDefaults.buttonColors(containerColor = Accent),
                    ) {
                        Text(
                            if (scanning) "Scanning…"
                            else if (scanResults.isEmpty()) "Scan device storage"
                            else "Re-scan",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp,
                        )
                    }

                    scanError?.let { err ->
                        Text(
                            err,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.sp,
                            color = Color(0xFFFF6B6B),
                        )
                    }

                    scanResults.forEach { group ->
                        Spacer(Modifier.height(4.dp))
                        Text(
                            group.dir.replace(Environment.getExternalStorageDirectory().absolutePath, "/sdcard"),
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Accent.copy(alpha = 0.7f),
                        )
                        Surface(
                            color = HorizonsColors.Surface,
                            shape = MaterialTheme.shapes.medium,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Column(Modifier.padding(vertical = 6.dp)) {
                                group.files.forEach { file ->
                                    var rowStatus by remember(file.absolutePath) { mutableStateOf<String?>(null) }
                                    Row(
                                        Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                    ) {
                                        Column(Modifier.weight(1f)) {
                                            Text(
                                                file.name,
                                                fontFamily = FontFamily.Monospace,
                                                fontSize = 11.sp,
                                                color = Color.White,
                                            )
                                            Text(
                                                "${file.kind.label} · ${file.sizeMb} MB",
                                                fontFamily = FontFamily.Monospace,
                                                fontSize = 9.sp,
                                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                                            )
                                            rowStatus?.let { s ->
                                                Text(
                                                    s,
                                                    fontFamily = FontFamily.Monospace,
                                                    fontSize = 9.sp,
                                                    color = ReadyGreen,
                                                )
                                            }
                                        }
                                        TextButton(onClick = {
                                            rowStatus = "importing…"
                                            scope.launch {
                                                val dest = StorageScanner.importInto(ctx, file)
                                                rowStatus = if (dest != null) "imported" else "failed"
                                                if (dest != null) importTick++
                                            }
                                        }) {
                                            Text(
                                                "Import",
                                                fontFamily = FontFamily.Monospace,
                                                fontSize = 11.sp,
                                                color = Accent,
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(Modifier.height(4.dp))
                Button(
                    onClick = { onNavigate(Panel.Router) },
                    colors = ButtonDefaults.buttonColors(containerColor = HorizonsColors.TileRouter),
                ) {
                    Text("Export to Router →", fontFamily = FontFamily.Monospace, fontSize = 12.sp)
                }

                HorizontalDivider(color = Accent.copy(alpha = 0.2f))

                // ── Key Vault ───────────────────────────────────────────────
                SectionLabel("Key Vault")
                Text(
                    "Encrypted storage for API tokens, credentials, and keys.",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                )

                TokenField(
                    label = "HuggingFace token",
                    value = creds[AppStateStore.KEY_HF_TOKEN] ?: "",
                    onSave = { app.appState.put(AppStateStore.KEY_HF_TOKEN, it) },
                    onRemove = { app.appState.remove(AppStateStore.KEY_HF_TOKEN) },
                )
                TokenField(
                    label = "GitHub token",
                    value = creds[AppStateStore.KEY_GITHUB_TOKEN] ?: "",
                    onSave = { app.appState.put(AppStateStore.KEY_GITHUB_TOKEN, it) },
                    onRemove = { app.appState.remove(AppStateStore.KEY_GITHUB_TOKEN) },
                )
                TokenField(
                    label = "SambaNova API key",
                    value = creds[AppStateStore.KEY_API_SAMBANOVA] ?: "",
                    onSave = { app.appState.put(AppStateStore.KEY_API_SAMBANOVA, it) },
                    onRemove = { app.appState.remove(AppStateStore.KEY_API_SAMBANOVA) },
                )
                TokenField(
                    label = "OpenRouter API key",
                    value = creds[AppStateStore.KEY_API_OPENROUTER] ?: "",
                    onSave = { app.appState.put(AppStateStore.KEY_API_OPENROUTER, it) },
                    onRemove = { app.appState.remove(AppStateStore.KEY_API_OPENROUTER) },
                )
                TokenField(
                    label = "QAI Hub API key",
                    value = creds[AppStateStore.KEY_API_QAI_HUB] ?: "",
                    onSave = { app.appState.put(AppStateStore.KEY_API_QAI_HUB, it) },
                    onRemove = { app.appState.remove(AppStateStore.KEY_API_QAI_HUB) },
                )

                creds.entries
                    .filter { e ->
                        !e.key.startsWith("router.") &&
                            !e.key.startsWith("memory.") &&
                            !e.key.startsWith("settings.") &&
                            e.key != AppStateStore.KEY_HF_TOKEN &&
                            e.key != AppStateStore.KEY_GITHUB_TOKEN &&
                            e.key != AppStateStore.KEY_API_SAMBANOVA &&
                            e.key != AppStateStore.KEY_API_OPENROUTER &&
                            e.key != AppStateStore.KEY_API_QAI_HUB &&
                            e.key != com.horizons.core.llm.CloudLlmRuntime.KEY_CLOUD_MODEL &&
                            e.key != com.horizons.core.llm.CloudLlmRuntime.KEY_CLOUD_ENDPOINT
                    }
                    .sortedBy { it.key }
                    .forEach { (key, value) ->
                        TokenField(
                            label = key,
                            value = value,
                            onSave = { app.appState.put(key, it) },
                            onRemove = { app.appState.remove(key) },
                        )
                    }

                var newLabel by remember { mutableStateOf("") }
                var newValue by remember { mutableStateOf("") }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    OutlinedTextField(
                        value = newLabel,
                        onValueChange = { newLabel = it },
                        label = { Text("Key name", color = Accent.copy(alpha = 0.4f)) },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        textStyle = TextStyle(fontFamily = FontFamily.Monospace, color = Color.White, fontSize = 12.sp),
                        colors = settingsFieldColors(),
                    )
                    OutlinedTextField(
                        value = newValue,
                        onValueChange = { newValue = it },
                        label = { Text("Value", color = Accent.copy(alpha = 0.4f)) },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        textStyle = TextStyle(fontFamily = FontFamily.Monospace, color = Color.White, fontSize = 12.sp),
                        colors = settingsFieldColors(),
                    )
                }

                if (newLabel.isNotBlank() && newValue.isNotBlank()) {
                    Button(
                        onClick = {
                            app.appState.put(newLabel.trim(), newValue.trim())
                            newLabel = ""
                            newValue = ""
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Accent),
                    ) {
                        Text("Add to vault", fontFamily = FontFamily.Monospace, fontSize = 12.sp)
                    }
                }

                HorizontalDivider(color = Accent.copy(alpha = 0.2f))

                // ── Cloud Endpoints ─────────────────────────────────────────
                SectionLabel("Cloud Endpoints")

                PathField(
                    label = "Cloud model ID (e.g. qwen/qwen-2.5-7b-instruct)",
                    value = creds[com.horizons.core.llm.CloudLlmRuntime.KEY_CLOUD_MODEL] ?: "",
                    onSave = {
                        app.appState.put(com.horizons.core.llm.CloudLlmRuntime.KEY_CLOUD_MODEL, it)
                        app.cloudRuntime.refreshStatus()
                    },
                )
                PathField(
                    label = "Custom endpoint (blank = auto from API key)",
                    value = creds[com.horizons.core.llm.CloudLlmRuntime.KEY_CLOUD_ENDPOINT] ?: "",
                    onSave = {
                        app.appState.put(com.horizons.core.llm.CloudLlmRuntime.KEY_CLOUD_ENDPOINT, it)
                        app.cloudRuntime.refreshStatus()
                    },
                )

                HorizontalDivider(color = Accent.copy(alpha = 0.2f))

                // ── Engine Config ───────────────────────────────────────────
                SectionLabel("Engine Config")

                Text(
                    "System prompt override",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp,
                    color = Accent.copy(alpha = 0.7f),
                )
                OutlinedTextField(
                    value = settings.systemPromptOverride,
                    onValueChange = { v -> app.settingsStore.update { it.copy(systemPromptOverride = v) } },
                    label = { Text("Leave blank for engine default", color = Accent.copy(alpha = 0.3f)) },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 120.dp),
                    maxLines = 10,
                    textStyle = TextStyle(fontFamily = FontFamily.Monospace, color = Color.White, fontSize = 12.sp),
                    colors = settingsFieldColors(),
                )

                Text(
                    "Default backend ID",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp,
                    color = Accent.copy(alpha = 0.7f),
                )
                OutlinedTextField(
                    value = settings.defaultBackendId,
                    onValueChange = { v -> app.settingsStore.update { it.copy(defaultBackendId = v) } },
                    label = { Text("Backend ID (blank = auto)", color = Accent.copy(alpha = 0.3f)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    textStyle = TextStyle(fontFamily = FontFamily.Monospace, color = Color.White, fontSize = 12.sp),
                    colors = settingsFieldColors(),
                )

                HorizontalDivider(color = Accent.copy(alpha = 0.2f))

                // ── Debug + Verbosity ───────────────────────────────────────
                SectionLabel("Debug")

                Text(
                    "Log level: ${settings.debugLogLevel}  (0=off  1=verbose  2=trace)",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp,
                    color = Accent.copy(alpha = 0.7f),
                )
                Slider(
                    value = settings.debugLogLevel.toFloat(),
                    onValueChange = { v -> app.settingsStore.update { it.copy(debugLogLevel = v.toInt()) } },
                    valueRange = 0f..2f,
                    steps = 1,
                    modifier = Modifier.fillMaxWidth(),
                    colors = sliderColors(),
                )

                val verbosityLabels = listOf("Concise", "Normal", "Detailed", "Verbose")
                val currentVerbosity = creds["settings.verbosity"] ?: "Normal"
                val verbosityIndex = verbosityLabels.indexOf(currentVerbosity).coerceAtLeast(0)

                Text(
                    "Response detail: $currentVerbosity",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp,
                    color = Accent.copy(alpha = 0.7f),
                )
                Slider(
                    value = verbosityIndex.toFloat(),
                    onValueChange = { v ->
                        val label = verbosityLabels[v.toInt().coerceIn(0, 3)]
                        app.appState.put("settings.verbosity", label)
                    },
                    valueRange = 0f..3f,
                    steps = 2,
                    modifier = Modifier.fillMaxWidth(),
                    colors = sliderColors(),
                )
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    verbosityLabels.forEach { label ->
                        Text(
                            label,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 9.sp,
                            color = if (label == currentVerbosity) Accent else Accent.copy(alpha = 0.35f),
                        )
                    }
                }

                HorizontalDivider(color = Accent.copy(alpha = 0.2f))

                // ── Memory ──────────────────────────────────────────────────
                SectionLabel("Memory")
                Text(
                    "Persistent key-value memory retained by the assistant.",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                )

                val memoryEntries = creds.filter { it.key.startsWith("memory.") }

                if (memoryEntries.isEmpty()) {
                    Text(
                        "(no memory entries)",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        color = Accent.copy(alpha = 0.4f),
                    )
                } else {
                    memoryEntries.forEach { (key, value) ->
                        Surface(
                            color = HorizonsColors.Surface,
                            shape = MaterialTheme.shapes.medium,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Row(
                                Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Column(Modifier.weight(1f)) {
                                    Text(
                                        key.removePrefix("memory."),
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Accent,
                                    )
                                    Text(
                                        value,
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 11.sp,
                                        color = Color.White.copy(alpha = 0.8f),
                                    )
                                }
                                TextButton(onClick = { app.appState.remove(key) }) {
                                    Text(
                                        "Remove",
                                        fontSize = 10.sp,
                                        fontFamily = FontFamily.Monospace,
                                        color = Color(0xFFFF6B6B),
                                    )
                                }
                            }
                        }
                    }
                }

                var newMemKey by remember { mutableStateOf("") }
                var newMemVal by remember { mutableStateOf("") }

                Surface(
                    color = HorizonsColors.Surface,
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = newMemKey,
                            onValueChange = { newMemKey = it },
                            label = { Text("Key", color = Accent.copy(alpha = 0.4f)) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            textStyle = TextStyle(fontFamily = FontFamily.Monospace, color = Color.White, fontSize = 12.sp),
                            colors = settingsFieldColors(),
                        )
                        OutlinedTextField(
                            value = newMemVal,
                            onValueChange = { newMemVal = it },
                            label = { Text("Value", color = Accent.copy(alpha = 0.4f)) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            textStyle = TextStyle(fontFamily = FontFamily.Monospace, color = Color.White, fontSize = 12.sp),
                            colors = settingsFieldColors(),
                        )
                        if (newMemKey.isNotBlank() && newMemVal.isNotBlank()) {
                            TextButton(onClick = {
                                app.appState.put("memory.${newMemKey.trim()}", newMemVal.trim())
                                newMemKey = ""
                                newMemVal = ""
                            }) {
                                Text("Add", fontSize = 10.sp, fontFamily = FontFamily.Monospace, color = Accent)
                            }
                        }
                    }
                }

                HorizontalDivider(color = Accent.copy(alpha = 0.2f))

                // ── Chat History Retention ───────────────────────────────────
                SectionLabel("Chat History")

                var retentionDays by remember { mutableFloatStateOf(app.chatHistory.retentionDays.toFloat()) }

                Text(
                    "Keep chats for ${retentionDays.toInt()} days",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp,
                    color = Accent.copy(alpha = 0.7f),
                )
                Slider(
                    value = retentionDays,
                    onValueChange = { v ->
                        retentionDays = v
                        app.chatHistory.retentionDays = v.toInt()
                    },
                    onValueChangeFinished = {
                        scope.launch { app.chatHistory.pruneOld() }
                    },
                    valueRange = 7f..90f,
                    steps = 82,
                    modifier = Modifier.fillMaxWidth(),
                    colors = sliderColors(),
                )
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text("7 days", fontFamily = FontFamily.Monospace, fontSize = 9.sp, color = Accent.copy(alpha = 0.4f))
                    Text("90 days", fontFamily = FontFamily.Monospace, fontSize = 9.sp, color = Accent.copy(alpha = 0.4f))
                }

                HorizontalDivider(color = Accent.copy(alpha = 0.2f))

                // ── Permissions ─────────────────────────────────────────────
                SectionLabel("Permissions")

                Surface(
                    color = HorizonsColors.Surface,
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(Modifier.padding(12.dp)) {
                        PermissionRow(
                            "RECORD_AUDIO",
                            ContextCompat.checkSelfPermission(ctx, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED,
                        )
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            PermissionRow(
                                "POST_NOTIFICATIONS",
                                ContextCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED,
                            )
                        }
                        PermissionRow(
                            "MANAGE_EXTERNAL_STORAGE",
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                                Environment.isExternalStorageManager()
                            } else {
                                ContextCompat.checkSelfPermission(ctx, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
                            },
                        )
                        PermissionRow(
                            "SYSTEM_ALERT_WINDOW",
                            AndroidSettings.canDrawOverlays(ctx),
                        )
                    }
                }

                HorizontalDivider(color = Accent.copy(alpha = 0.2f))

                // ── System Registrations ────────────────────────────────────
                SectionLabel("System Registrations")
                Text(
                    "These require manual setup via Android Settings.",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                )

                Surface(
                    color = HorizonsColors.Surface,
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(Modifier.padding(12.dp)) {
                        RegistrationRow("System TTS (Kokoro)", "Check System > TTS")
                        RegistrationRow("Default assistant", "Check System > Assist app")
                        RegistrationRow("Accessibility service", "Check System > Accessibility")
                        RegistrationRow("Notification listener", "Check System > Notification access")
                    }
                }

                // ── Cross-nav ───────────────────────────────────────────────
                HorizontalDivider(color = Accent.copy(alpha = 0.2f))

                Surface(
                    color = HorizonsColors.Surface,
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.fillMaxWidth().clickable { onNavigate(Panel.Terminal) },
                ) {
                    Row(
                        Modifier.fillMaxWidth().padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            "Saved scripts and prompts",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp,
                            color = Color.White.copy(alpha = 0.85f),
                        )
                        Text(
                            "TERMINAL →",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = HorizonsColors.TileTerminal,
                        )
                    }
                }

                Surface(
                    color = HorizonsColors.Surface,
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.fillMaxWidth().clickable { onNavigate(Panel.Artifacts) },
                ) {
                    Row(
                        Modifier.fillMaxWidth().padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            "Failure logs · crash reports · diagnostics",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp,
                            color = Color.White.copy(alpha = 0.85f),
                        )
                        Text(
                            "ARCHIVES →",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = HorizonsColors.TileArtifacts,
                        )
                    }
                }

                Spacer(Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun SectionLabel(title: String) {
    Text(
        title,
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Bold,
        fontSize = 14.sp,
        color = Accent,
    )
}

@Composable
private fun settingsFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = Accent.copy(alpha = 0.6f),
    unfocusedBorderColor = Accent.copy(alpha = 0.2f),
    cursorColor = Accent,
)

@Composable
private fun sliderColors() = SliderDefaults.colors(
    thumbColor = Accent,
    activeTrackColor = Accent,
    inactiveTrackColor = Accent.copy(alpha = 0.15f),
)

@Composable
private fun TokenField(
    label: String,
    value: String,
    onSave: (String) -> Unit,
    onRemove: () -> Unit = {},
) {
    var draft by remember(value) { mutableStateOf(value) }
    var visible by remember { mutableStateOf(false) }

    Surface(
        color = HorizonsColors.Surface,
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(12.dp)) {
            OutlinedTextField(
                value = draft,
                onValueChange = { draft = it },
                label = { Text(label, color = Accent.copy(alpha = 0.4f)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
                textStyle = TextStyle(fontFamily = FontFamily.Monospace, color = Color.White, fontSize = 12.sp),
                colors = settingsFieldColors(),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                TextButton(onClick = { visible = !visible }) {
                    Text(
                        if (visible) "Hide" else "Show",
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        color = Accent,
                    )
                }
                if (draft != value) {
                    TextButton(onClick = { onSave(draft.trim()) }) {
                        Text("Save", fontSize = 10.sp, fontFamily = FontFamily.Monospace, color = Accent)
                    }
                }
                if (value.isNotBlank()) {
                    TextButton(onClick = onRemove) {
                        Text("Remove", fontSize = 10.sp, fontFamily = FontFamily.Monospace, color = Color(0xFFFF6B6B))
                    }
                }
            }
        }
    }
}

@Composable
private fun PathField(
    label: String,
    value: String,
    onSave: (String) -> Unit,
) {
    var draft by remember(value) { mutableStateOf(value) }

    Surface(
        color = HorizonsColors.Surface,
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(12.dp)) {
            OutlinedTextField(
                value = draft,
                onValueChange = { draft = it },
                label = { Text(label, color = Accent.copy(alpha = 0.4f)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                textStyle = TextStyle(fontFamily = FontFamily.Monospace, color = Color.White, fontSize = 12.sp),
                colors = settingsFieldColors(),
            )
            if (draft != value) {
                TextButton(onClick = { onSave(draft.trim()) }) {
                    Text("Save", fontSize = 10.sp, fontFamily = FontFamily.Monospace, color = Accent)
                }
            }
        }
    }
}

@Composable
private fun PermissionRow(name: String, granted: Boolean) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            name,
            fontFamily = FontFamily.Monospace,
            fontSize = 12.sp,
            color = Color.White.copy(alpha = 0.8f),
        )
        Text(
            if (granted) "GRANTED" else "DENIED",
            fontFamily = FontFamily.Monospace,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = if (granted) Color(0xFF4CAF50) else Color(0xFFFF6B6B),
        )
    }
}

@Composable
private fun RegistrationRow(name: String, status: String) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            name,
            fontFamily = FontFamily.Monospace,
            fontSize = 12.sp,
            color = Color.White.copy(alpha = 0.8f),
        )
        Text(
            status,
            fontFamily = FontFamily.Monospace,
            fontSize = 11.sp,
            color = Accent.copy(alpha = 0.7f),
        )
    }
}

private fun queryDisplayName(ctx: android.content.Context, uri: Uri): String? {
    ctx.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
        val idx = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
        if (idx >= 0 && cursor.moveToFirst()) return cursor.getString(idx)
    }
    return null
}
