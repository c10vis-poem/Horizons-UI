package com.horizons.ui.panels

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.horizons.HorizonsApplication
import com.horizons.core.shell.DaemonLauncher
import com.horizons.core.state.ConfigStatus
import com.horizons.core.state.RouterConfig
import com.horizons.core.state.allGreen
import com.horizons.core.state.greenLight
import com.horizons.ui.CircuitTraceBackground
import com.horizons.ui.theme.HorizonsColors
import kotlinx.coroutines.launch

private val Accent = HorizonsColors.TileRouter
private val ReadyGreen = Color(0xFF4CAF50)
private val SleepAmber = Color(0xFFE8A838)

@Composable
fun RouterPane(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val ctx = LocalContext.current
    val app = ctx.applicationContext as HorizonsApplication
    val scope = rememberCoroutineScope()
    val configs by app.routerConfigs.configs.collectAsState()
    val backendStatus by app.llmRuntime.backendStatus.collectAsState()

    val activeConfigs = configs.filter { it.status != ConfigStatus.ARCHIVED }
    val readyConfigs = activeConfigs.filter { it.isReady || it.status == ConfigStatus.RUNNING }
    val incompleteConfigs = activeConfigs.filter { !it.isReady && it.status != ConfigStatus.RUNNING && it.status != ConfigStatus.SLEEPING }
    val sleepingConfigs = activeConfigs.filter { it.status == ConfigStatus.SLEEPING }

    var showNewConfig by remember { mutableStateOf(false) }
    var fuseBlocked by remember { mutableStateOf<String?>(null) }

    // The fuse box gate: a config whose runtime has a definition can only
    // switch on if every green-light check passes right now — re-validated
    // at flip time, not trusted from when Monitor handed it over.
    fun switchOn(config: com.horizons.core.state.RouterConfig, preWarm: Boolean = false) {
        val def = app.runtimeDefs.defs.value.firstOrNull { it.name == config.runtime }
        if (def != null) {
            val checks = def.greenLight(ctx, app.resolveNpuModelPath())
            if (!checks.allGreen) {
                fuseBlocked = "'${config.name}' blocked — red lights: " +
                    checks.filter { !it.ok }.joinToString(", ") { it.label } +
                    ". Check Monitor / console."
                return
            }
        }
        fuseBlocked = null
        app.routerConfigs.setStatus(config.id, ConfigStatus.RUNNING)

        // Actually start the thing. Until now this function set a status label and
        // stopped, so "RUNNING" meant nothing had been launched — the only daemon
        // ever started was CliffordService's hardcoded ort_engine, regardless of
        // which config was plated. Everything needed is already on the RuntimeDef;
        // nothing consumed it.
        //
        // Configs with no matching def (cloud endpoints, PWA, terminal harnesses)
        // have no local binary to launch and fall through untouched.
        if (def != null) {
            val args = def.argsTemplate
                .replace("{model}", app.resolveNpuModelPath().orEmpty())
                .replace("{port}", def.port.toString())
                .trim()
                .split(Regex("\\s+"))
                .filter { it.isNotBlank() }

            scope.launch {
                val launcher = DaemonLauncher(ctx, def.binaryName)
                if (!launcher.isRunning()) {
                    launcher.launch(args).onFailure { e ->
                        fuseBlocked = "'${config.name}' — ${def.binaryName} failed to start: " +
                            "${e.message}. Check Monitor / console."
                        app.routerConfigs.setStatus(config.id, ConfigStatus.READY)
                        return@launch
                    }
                }
                // Retarget the chat runtime at THIS config's endpoint, so a config
                // running geniex on :18181 isn't answered by whatever sits on :8080.
                app.activateNpuRuntime(def.port, def.healthPath)
                if (preWarm) app.llmRuntime.preWarm()
            }
        } else if (preWarm) {
            app.llmRuntime.preWarm()
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        CircuitTraceBackground()
        SelectionContainer {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onBack) {
                    Text("←", fontSize = 20.sp, color = Accent)
                }
                Text(
                    "ROUTER",
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    color = Accent,
                )
                Text(
                    "  / plate",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp,
                    color = Accent.copy(alpha = 0.5f),
                )
            }

            HorizontalDivider(color = Accent.copy(alpha = 0.2f))

            // Active runtime status
            Surface(
                color = HorizonsColors.Surface,
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    Modifier.fillMaxWidth().padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column {
                        Text(
                            "Active Backend",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.sp,
                            color = Accent.copy(alpha = 0.5f),
                        )
                        Text(
                            backendStatus,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                    StatusPill(
                        text = if (backendStatus.contains("no backend")) "IDLE" else "LIVE",
                        active = !backendStatus.contains("no backend"),
                    )
                }
            }

            // ── Ready to Run ────────────────────────────────────────────────────
            RouterSection("Ready to Run")

            if (readyConfigs.isEmpty()) {
                Surface(
                    color = HorizonsColors.Surface,
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        "No plated configurations.\nBuild one in Settings, then export here — or create one below.",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                        modifier = Modifier.padding(16.dp),
                    )
                }
            }

            fuseBlocked?.let { msg ->
                Surface(
                    color = Color(0xFF2A1010),
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        "⚡ FUSE BOX: $msg",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp,
                        color = Color(0xFFFF6666),
                        modifier = Modifier.padding(12.dp),
                    )
                }
            }

            readyConfigs.forEach { config ->
                MealCard(
                    config = config,
                    onRun = { switchOn(config, preWarm = true) },
                    onSleep = { app.routerConfigs.setStatus(config.id, ConfigStatus.SLEEPING) },
                    onArchive = { app.routerConfigs.setStatus(config.id, ConfigStatus.ARCHIVED) },
                    onDelete = { app.routerConfigs.remove(config.id) },
                )
            }

            // ── On Deck (Sleeping) ──────────────────────────────────────────────
            if (sleepingConfigs.isNotEmpty()) {
                HorizontalDivider(color = Accent.copy(alpha = 0.2f))
                RouterSection("On Deck")

                sleepingConfigs.forEach { config ->
                    MealCard(
                        config = config,
                        onRun = { switchOn(config) },
                        onSleep = null,
                        onArchive = { app.routerConfigs.setStatus(config.id, ConfigStatus.ARCHIVED) },
                        onDelete = { app.routerConfigs.remove(config.id) },
                    )
                }
            }

            // ── Incomplete ──────────────────────────────────────────────────────
            if (incompleteConfigs.isNotEmpty()) {
                HorizontalDivider(color = Accent.copy(alpha = 0.2f))
                RouterSection("Needs Attention")

                incompleteConfigs.forEach { config ->
                    IncompleteCard(
                        config = config,
                        onDelete = { app.routerConfigs.remove(config.id) },
                    )
                }
            }

            HorizontalDivider(color = Accent.copy(alpha = 0.2f))

            // ── New Configuration ───────────────────────────────────────────────
            if (!showNewConfig) {
                OutlinedButton(
                    onClick = { showNewConfig = true },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        "+ New Configuration",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        color = Accent,
                    )
                }
            } else {
                NewConfigForm(
                    onSave = { config ->
                        app.routerConfigs.add(config)
                        showNewConfig = false
                    },
                    onCancel = { showNewConfig = false },
                )
            }

            Spacer(Modifier.height(24.dp))
        }
        }
    }
}

@Composable
private fun MealCard(
    config: RouterConfig,
    onRun: () -> Unit,
    onSleep: (() -> Unit)?,
    onArchive: () -> Unit,
    onDelete: () -> Unit,
) {
    val isRunning = config.status == ConfigStatus.RUNNING
    val isSleeping = config.status == ConfigStatus.SLEEPING
    val borderColor = when {
        isRunning -> ReadyGreen
        isSleeping -> SleepAmber
        config.isReady -> Accent
        else -> Accent.copy(alpha = 0.3f)
    }

    Surface(
        color = if (isRunning) ReadyGreen.copy(alpha = 0.08f)
                else if (isSleeping) SleepAmber.copy(alpha = 0.06f)
                else HorizonsColors.Surface,
        shape = MaterialTheme.shapes.medium,
        border = BorderStroke(1.dp, borderColor.copy(alpha = 0.4f)),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    config.name,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = if (isRunning) ReadyGreen else Accent,
                )
                StatusPill(
                    text = when {
                        isRunning -> "RUNNING"
                        isSleeping -> "ON DECK"
                        config.isReady -> "READY"
                        else -> "INCOMPLETE"
                    },
                    active = isRunning || config.isReady,
                )
            }

            // Slot summary
            config.allSlots.filter { it.filled }.forEach { slot ->
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        slot.slotName,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                    )
                    Text(
                        slot.value.takeLast(40),
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp,
                        color = ReadyGreen.copy(alpha = 0.8f),
                    )
                }
            }

            // Action buttons
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (!isRunning) {
                    Button(
                        onClick = onRun,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ReadyGreen.copy(alpha = 0.2f),
                            contentColor = ReadyGreen,
                        ),
                    ) {
                        Text("START", fontFamily = FontFamily.Monospace, fontSize = 11.sp)
                    }
                }
                if (onSleep != null && !isSleeping) {
                    OutlinedButton(onClick = onSleep) {
                        Text("Sleep", fontFamily = FontFamily.Monospace, fontSize = 10.sp, color = SleepAmber)
                    }
                }
                if (isSleeping) {
                    Button(
                        onClick = onRun,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Accent.copy(alpha = 0.15f),
                            contentColor = Accent,
                        ),
                    ) {
                        Text("Wake", fontFamily = FontFamily.Monospace, fontSize = 10.sp)
                    }
                }
                OutlinedButton(onClick = onArchive) {
                    Text("Archive", fontFamily = FontFamily.Monospace, fontSize = 10.sp, color = Accent.copy(alpha = 0.6f))
                }
                TextButton(onClick = onDelete) {
                    Text("Delete", fontFamily = FontFamily.Monospace, fontSize = 10.sp, color = Color(0xFFFF6B6B))
                }
            }
        }
    }
}

@Composable
private fun IncompleteCard(
    config: RouterConfig,
    onDelete: () -> Unit,
) {
    Surface(
        color = HorizonsColors.Surface,
        shape = MaterialTheme.shapes.medium,
        border = BorderStroke(1.dp, Color(0xFFFF6B6B).copy(alpha = 0.3f)),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    config.name,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = Accent,
                )
                StatusPill("INCOMPLETE", active = false)
            }

            // Show what's filled
            config.allSlots.filter { it.filled }.forEach { slot ->
                Text(
                    "${slot.slotName} = ${slot.value.takeLast(30)}",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.sp,
                    color = ReadyGreen.copy(alpha = 0.6f),
                )
            }

            // Show what's missing
            if (config.missingSlots.isNotEmpty()) {
                Text(
                    "Missing: ${config.missingSlots.joinToString(", ")}",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFFF6B6B),
                )
            }

            TextButton(onClick = onDelete) {
                Text("Remove", fontFamily = FontFamily.Monospace, fontSize = 10.sp, color = Color(0xFFFF6B6B))
            }
        }
    }
}

@Composable
private fun NewConfigForm(
    onSave: (RouterConfig) -> Unit,
    onCancel: () -> Unit,
) {
    var name by remember { mutableStateOf("") }
    var runtime by remember { mutableStateOf("") }
    var backend by remember { mutableStateOf("") }
    var model by remember { mutableStateOf("") }
    var endpoint by remember { mutableStateOf("") }
    var apiKeyRef by remember { mutableStateOf("") }

    val runtimeOptions = listOf("geniex", "ort_engine", "cloud", "terminal", "cli")

    Surface(
        color = Accent.copy(alpha = 0.05f),
        shape = MaterialTheme.shapes.medium,
        border = BorderStroke(1.dp, Accent.copy(alpha = 0.3f)),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                "NEW CONFIGURATION",
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                color = Accent,
            )

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Config name") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                textStyle = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
            )

            Text(
                "Runtime",
                fontFamily = FontFamily.Monospace,
                fontSize = 10.sp,
                color = Accent.copy(alpha = 0.7f),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                runtimeOptions.forEach { opt ->
                    val selected = runtime == opt
                    Surface(
                        color = if (selected) Accent.copy(alpha = 0.15f) else HorizonsColors.Surface,
                        shape = MaterialTheme.shapes.small,
                        border = if (selected) BorderStroke(1.dp, Accent.copy(alpha = 0.5f)) else null,
                        modifier = Modifier.clickable { runtime = opt },
                    ) {
                        Text(
                            opt,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.sp,
                            color = if (selected) Accent else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        )
                    }
                }
            }

            OutlinedTextField(
                value = backend,
                onValueChange = { backend = it },
                label = { Text("Backend (e.g. qnn_sdk_ggml, HTP_v79, llama.cpp)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                textStyle = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
            )

            OutlinedTextField(
                value = model,
                onValueChange = { model = it },
                label = { Text("Model path or ID") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                textStyle = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
            )

            OutlinedTextField(
                value = endpoint,
                onValueChange = { endpoint = it },
                label = { Text("Endpoint (cloud configs only)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                textStyle = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
            )

            OutlinedTextField(
                value = apiKeyRef,
                onValueChange = { apiKeyRef = it },
                label = { Text("API key name from vault (e.g. openrouter)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                textStyle = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = {
                        if (name.isNotBlank()) {
                            onSave(
                                RouterConfig(
                                    name = name.trim(),
                                    runtime = runtime,
                                    backend = backend.trim(),
                                    model = model.trim(),
                                    endpoint = endpoint.trim(),
                                    apiKeyRef = apiKeyRef.trim(),
                                    status = if (runtime.isNotBlank()) ConfigStatus.READY else ConfigStatus.INCOMPLETE,
                                )
                            )
                        }
                    },
                    enabled = name.isNotBlank(),
                ) {
                    Text("Create", fontFamily = FontFamily.Monospace, fontSize = 12.sp)
                }
                OutlinedButton(onClick = onCancel) {
                    Text("Cancel", fontFamily = FontFamily.Monospace, fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
private fun RouterSection(title: String) {
    Text(
        title,
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Bold,
        fontSize = 14.sp,
        color = Accent,
    )
}

@Composable
private fun StatusPill(text: String, active: Boolean) {
    Surface(
        shape = MaterialTheme.shapes.small,
        color = if (active) HorizonsColors.StatusAsr.copy(alpha = 0.15f)
                else HorizonsColors.Surface,
    ) {
        Text(
            text,
            fontFamily = FontFamily.Monospace,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            color = if (active) HorizonsColors.StatusAsr else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
        )
    }
}
