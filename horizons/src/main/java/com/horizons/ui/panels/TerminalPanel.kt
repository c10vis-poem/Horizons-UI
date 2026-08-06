package com.horizons.ui.panels

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Send
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.TextButton
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.viewinterop.AndroidView
import com.horizons.HorizonsApplication
import com.horizons.ui.browser.BrowserPane
import com.horizons.Panel
import com.horizons.core.shell.DaemonLauncher
import com.horizons.core.shell.ManualStore
import com.horizons.core.shell.TaskerBridge
import com.horizons.core.state.ConfigStatus
import com.horizons.core.state.RouterConfig
import com.horizons.core.state.RuntimeDef
import com.horizons.core.state.SavedCommand
import com.horizons.ui.theme.HorizonsColors
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

private val MatrixGreen = Color(0xFF00FF41)
private val MatrixDimGreen = Color(0xFF003B0F)

private data class RainColumn(
    val col: Int,
    val speed: Float,
    val chars: List<Char>,
    val startOffset: Float,
)

private data class ShellEntry(val cmd: String, val stdout: String, val stderr: String, val exitCode: Int)
private data class TermEntry(val input: String, val result: String, val ok: Boolean)

@Composable
fun TerminalPanel(
    onBack: () -> Unit,
    onNavigate: (Panel) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val ctx = LocalContext.current
    val app = ctx.applicationContext as HorizonsApplication
    val scope = rememberCoroutineScope()

    var selectedTab by remember { mutableIntStateOf(0) }
    val shellCmd = remember { mutableStateOf("") }

    val rainColumns = remember { generateRainColumns(40) }
    val transition = rememberInfiniteTransition(label = "matrix")
    val animProgress by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(8000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "rain",
    )

    Box(modifier = modifier.fillMaxSize().background(Color.Black)) {
        // Matrix waterfall background
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawMatrixRain(rainColumns, animProgress)
        }

        Column(Modifier.fillMaxSize()) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onBack) {
                    Text("←", fontSize = 20.sp, color = MatrixGreen)
                }
                Text(
                    "TERMINAL",
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    color = MatrixGreen,
                )
                Text(
                    "  / garage",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp,
                    color = MatrixGreen.copy(alpha = 0.5f),
                )
            }

            // Tab row
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = Color.Black.copy(alpha = 0.7f),
                contentColor = MatrixGreen,
                indicator = {},
                divider = { HorizontalDivider(color = MatrixGreen.copy(alpha = 0.2f)) },
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = {
                        Text(
                            "Shell",
                            fontFamily = FontFamily.Monospace,
                            color = if (selectedTab == 0) MatrixGreen else MatrixGreen.copy(alpha = 0.4f),
                        )
                    },
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = {
                        Text(
                            "Tasker",
                            fontFamily = FontFamily.Monospace,
                            color = if (selectedTab == 1) MatrixGreen else MatrixGreen.copy(alpha = 0.4f),
                        )
                    },
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    text = {
                        Text(
                            "Prompts",
                            fontFamily = FontFamily.Monospace,
                            color = if (selectedTab == 2) MatrixGreen else MatrixGreen.copy(alpha = 0.4f),
                        )
                    },
                )
                Tab(
                    selected = selectedTab == 3,
                    onClick = { selectedTab = 3 },
                    text = {
                        Text(
                            "Browser",
                            fontFamily = FontFamily.Monospace,
                            color = if (selectedTab == 3) MatrixGreen else MatrixGreen.copy(alpha = 0.4f),
                        )
                    },
                )
                Tab(
                    selected = selectedTab == 4,
                    onClick = { selectedTab = 4 },
                    text = {
                        Text(
                            "Runtime",
                            fontFamily = FontFamily.Monospace,
                            color = if (selectedTab == 4) MatrixGreen else MatrixGreen.copy(alpha = 0.4f),
                        )
                    },
                )
            }

            when (selectedTab) {
                0 -> ShellTab(app = app, scope = scope, cmdState = shellCmd)
                1 -> TaskerTab(app = app, scope = scope)
                2 -> PromptsTab(
                    app = app,
                    scope = scope,
                    onCommandSelected = { command ->
                        shellCmd.value = command
                        selectedTab = 0
                    },
                )
                3 -> BrowserPane(app = app, accent = MatrixGreen)
                4 -> RuntimeTab(app = app)
            }
        }
    }
}

private fun generateRainColumns(count: Int): List<RainColumn> {
    val rng = java.util.Random(7)
    val caretChar = '^'
    return List(count) { i ->
        RainColumn(
            col = i,
            speed = 0.3f + rng.nextFloat() * 0.7f,
            chars = List(8 + rng.nextInt(12)) { caretChar },
            startOffset = rng.nextFloat(),
        )
    }
}

private fun DrawScope.drawMatrixRain(columns: List<RainColumn>, progress: Float) {
    val colWidth = size.width / columns.size
    val charHeight = 18f

    columns.forEach { col ->
        val x = col.col * colWidth + colWidth / 2f
        val totalHeight = col.chars.size * charHeight
        val cyclePos = ((progress * col.speed + col.startOffset) % 1f) * (size.height + totalHeight)

        col.chars.forEachIndexed { i, _ ->
            val y = cyclePos - i * charHeight
            if (y in -charHeight..size.height + charHeight) {
                val fade = when {
                    i == 0 -> 0.9f
                    i < 3 -> 0.5f - i * 0.1f
                    else -> 0.15f - (i - 3) * 0.015f
                }.coerceIn(0.02f, 0.9f)

                drawCircle(
                    color = Color(0xFF00FF41).copy(alpha = fade * 0.3f),
                    radius = 4f,
                    center = Offset(x, y),
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ShellTab(
    app: HorizonsApplication,
    scope: kotlinx.coroutines.CoroutineScope,
    cmdState: MutableState<String>,
) {
    val listState = rememberLazyListState()
    val clipboard = LocalClipboardManager.current
    var cmd by cmdState
    var running by remember { mutableStateOf(false) }
    val history = remember { mutableStateListOf<ShellEntry>() }
    var archiveTarget by remember { mutableStateOf<ShellEntry?>(null) }

    // "Archive…" name dialog — the file lands in Archives under /terminal
    archiveTarget?.let { entry ->
        var fileName by remember(entry) {
            mutableStateOf("cmd-${entry.cmd.take(16).replace(Regex("[^A-Za-z0-9_-]"), "_")}.sh")
        }
        AlertDialog(
            onDismissRequest = { archiveTarget = null },
            containerColor = Color(0xFF0A140A),
            title = {
                Text("Archive to artifacts", fontFamily = FontFamily.Monospace, fontSize = 14.sp, color = MatrixGreen)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Saved into Archives / terminal /",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp,
                        color = MatrixGreen.copy(alpha = 0.5f),
                    )
                    OutlinedTextField(
                        value = fileName,
                        onValueChange = { fileName = it },
                        singleLine = true,
                        label = { Text("File name", color = MatrixGreen.copy(alpha = 0.4f)) },
                        textStyle = TextStyle(fontFamily = FontFamily.Monospace, color = MatrixGreen, fontSize = 12.sp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MatrixGreen.copy(alpha = 0.6f),
                            unfocusedBorderColor = MatrixGreen.copy(alpha = 0.2f),
                            cursorColor = MatrixGreen,
                        ),
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val body = buildString {
                        appendLine("# archived from Horizons terminal")
                        appendLine(entry.cmd)
                        if (entry.stdout.isNotEmpty() || entry.stderr.isNotEmpty()) {
                            appendLine()
                            appendLine("# --- output (exit ${entry.exitCode}) ---")
                            if (entry.stdout.isNotEmpty()) entry.stdout.lines().forEach { appendLine("# $it") }
                            if (entry.stderr.isNotEmpty()) entry.stderr.lines().forEach { appendLine("# [stderr] $it") }
                        }
                    }
                    app.archive.writeText("terminal", fileName, body)
                    archiveTarget = null
                }) {
                    Text("Archive", fontFamily = FontFamily.Monospace, color = MatrixGreen)
                }
            },
            dismissButton = {
                TextButton(onClick = { archiveTarget = null }) {
                    Text("Cancel", fontFamily = FontFamily.Monospace, color = MatrixGreen.copy(alpha = 0.5f))
                }
            },
        )
    }

    fun runCmd() {
        val command = cmd.trim().ifEmpty { return }
        running = true
        scope.launch {
            // `manual` is handled in-process rather than shelled out. The guide
            // lives in filesDir so `cat` works too, but resolving it here means
            // chapter lookup and search work without the user knowing the path.
            val result = if (command == "manual" || command.startsWith("manual ")) {
                val arg = command.removePrefix("manual").trim()
                TaskerBridge.ShellResult(
                    exitCode = 0,
                    stdout = ManualStore.query(app, arg.ifEmpty { null }),
                    stderr = "",
                )
            } else {
                app.tasker.runShellCommand(command)
            }
            history.add(ShellEntry(command, result.stdout, result.stderr, result.exitCode))
            if (result.exitCode == 0) cmd = ""
            running = false
        }
    }

    Column(
        Modifier.fillMaxSize().padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (!app.tasker.isTermuxInstalled()) {
            Surface(
                color = Color(0xFF1A0000),
                shape = MaterialTheme.shapes.small,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    "Termux not installed — install from F-Droid and grant RUN_COMMAND permission.",
                    color = Color(0xFFFF6666),
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.sp,
                    modifier = Modifier.padding(8.dp),
                )
            }
        }

        LazyColumn(
            state = listState,
            modifier = Modifier.weight(1f).fillMaxWidth(),
            reverseLayout = true,
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            items(history.asReversed()) { entry ->
                val isError = entry.exitCode != 0
                val color = if (isError) Color(0xFFFF4444) else MatrixGreen
                val output = when {
                    entry.stdout.isNotEmpty() && entry.stderr.isNotEmpty() ->
                        "${entry.stdout}\n[stderr] ${entry.stderr}"
                    entry.stdout.isNotEmpty() -> entry.stdout
                    entry.stderr.isNotEmpty() -> entry.stderr
                    else -> "(no output, exit ${entry.exitCode})"
                }
                // Tap to recall the command into the input; long-press for
                // the action menu (copy / export / save / archive).
                var menuOpen by remember { mutableStateOf(false) }
                Box(Modifier.fillMaxWidth()) {
                    Text(
                        "$ ${entry.cmd}\n$output",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        color = color,
                        modifier = Modifier
                            .fillMaxWidth()
                            .combinedClickable(
                                onClick = { cmd = entry.cmd },
                                onLongClick = { menuOpen = true },
                            ),
                    )
                    DropdownMenu(
                        expanded = menuOpen,
                        onDismissRequest = { menuOpen = false },
                    ) {
                        DropdownMenuItem(
                            text = { Text("Copy command", fontFamily = FontFamily.Monospace, fontSize = 12.sp) },
                            onClick = {
                                clipboard.setText(AnnotatedString(entry.cmd))
                                menuOpen = false
                            },
                        )
                        DropdownMenuItem(
                            text = { Text("Copy output", fontFamily = FontFamily.Monospace, fontSize = 12.sp) },
                            onClick = {
                                clipboard.setText(AnnotatedString(output))
                                menuOpen = false
                            },
                        )
                        DropdownMenuItem(
                            text = { Text("Export to Router", fontFamily = FontFamily.Monospace, fontSize = 12.sp) },
                            onClick = {
                                app.routerConfigs.add(
                                    RouterConfig(
                                        name = "Terminal: ${entry.cmd.take(30)}",
                                        runtime = "terminal",
                                        backend = "bash",
                                        model = entry.cmd,
                                    ),
                                )
                                menuOpen = false
                            },
                        )
                        DropdownMenuItem(
                            text = { Text("Save to Commands", fontFamily = FontFamily.Monospace, fontSize = 12.sp) },
                            onClick = {
                                scope.launch {
                                    app.savedCommands.add(
                                        SavedCommand(
                                            label = entry.cmd.take(24),
                                            command = entry.cmd,
                                            category = "terminal",
                                        ),
                                    )
                                }
                                menuOpen = false
                            },
                        )
                        DropdownMenuItem(
                            text = { Text("Archive…", fontFamily = FontFamily.Monospace, fontSize = 12.sp) },
                            onClick = {
                                archiveTarget = entry
                                menuOpen = false
                            },
                        )
                    }
                }
            }
        }

        HorizontalDivider(color = MatrixGreen.copy(alpha = 0.2f))

        OutlinedTextField(
            value = cmd,
            onValueChange = { cmd = it },
            label = { Text("Shell command", color = MatrixGreen.copy(alpha = 0.4f)) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            enabled = !running,
            textStyle = TextStyle(fontFamily = FontFamily.Monospace, color = MatrixGreen, fontSize = 13.sp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MatrixGreen.copy(alpha = 0.6f),
                unfocusedBorderColor = MatrixGreen.copy(alpha = 0.2f),
                cursorColor = MatrixGreen,
            ),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { runCmd() }),
        )

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = ::runCmd,
                enabled = !running && cmd.isNotBlank(),
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MatrixGreen.copy(alpha = 0.15f),
                    contentColor = MatrixGreen,
                ),
            ) {
                if (running) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = MatrixGreen,
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("Running…", fontFamily = FontFamily.Monospace)
                } else {
                    Icon(Icons.Filled.Send, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Run", fontFamily = FontFamily.Monospace)
                }
            }
            OutlinedButton(
                onClick = { history.clear() },
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MatrixGreen),
            ) {
                Icon(Icons.Filled.Clear, contentDescription = "Clear")
            }
        }

        if (cmd.isNotBlank()) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = {
                        val config = RouterConfig(
                            name = "Terminal: ${cmd.take(30)}",
                            runtime = "terminal",
                            backend = "bash",
                            model = cmd,
                        )
                        app.routerConfigs.add(config)
                    },
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFAA77FF)),
                ) {
                    Text("Export to Router", fontFamily = FontFamily.Monospace, fontSize = 10.sp)
                }
                OutlinedButton(
                    onClick = {
                        app.appState.put("script.${cmd.hashCode()}", cmd)
                    },
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFFF5577)),
                ) {
                    Text("Save to Vault", fontFamily = FontFamily.Monospace, fontSize = 10.sp)
                }
            }
        }
    }
}

@Composable
private fun TaskerTab(
    app: HorizonsApplication,
    scope: kotlinx.coroutines.CoroutineScope,
) {
    val listState = rememberLazyListState()

    var taskName by remember { mutableStateOf("") }
    var param1 by remember { mutableStateOf("") }
    val history = remember { mutableStateListOf<TermEntry>() }

    fun dispatch() {
        val name = taskName.trim().ifEmpty { return }
        val p = param1.trim()
        val params = if (p.isNotEmpty()) arrayOf("p1" to p) else emptyArray()
        val res = app.tasker.runTask(name, *params)
        val ok = res.isSuccess
        val msg = if (ok) "fired: $name${if (p.isNotEmpty()) " [$p]" else ""}"
                  else "error: ${res.exceptionOrNull()?.message ?: "unknown"}"
        history.add(TermEntry(name, msg, ok))
        if (ok) { taskName = ""; param1 = "" }
        scope.launch { listState.animateScrollToItem(history.size - 1) }
    }

    Column(
        Modifier.fillMaxSize().padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            "Tasker Terminal",
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            color = MatrixGreen,
        )

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(
                onClick = {
                    val res = app.tasker.runTask("HorizonsTest")
                    val ok = res.isSuccess
                    history.add(TermEntry("HorizonsTest", if (ok) "fired: HorizonsTest" else "error: ${res.exceptionOrNull()?.message}", ok))
                    scope.launch { if (history.isNotEmpty()) listState.animateScrollToItem(history.size - 1) }
                },
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MatrixGreen),
            ) { Text("Fire Test Task", fontFamily = FontFamily.Monospace) }
            OutlinedButton(
                onClick = { history.clear() },
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MatrixGreen),
            ) {
                Icon(Icons.Filled.Clear, contentDescription = "Clear", tint = MatrixGreen)
            }
        }

        if (!app.tasker.isTaskerInstalled()) {
            Text(
                "Tasker not installed — install from Play Store and enable External Access.",
                color = Color(0xFFFF6666),
                fontFamily = FontFamily.Monospace,
                fontSize = 10.sp,
            )
        }

        LazyColumn(
            state = listState,
            modifier = Modifier.weight(1f).fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            items(history) { entry ->
                val color = if (entry.ok) MatrixGreen else Color(0xFFFF4444)
                // Tap to recall the task name into the input; long-press to copy.
                Box(Modifier.fillMaxWidth().clickable { taskName = entry.input }) {
                    SelectionContainer {
                        Text(
                            "> ${entry.input}\n  ${entry.result}",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            color = color,
                        )
                    }
                }
            }
        }

        HorizontalDivider(color = MatrixGreen.copy(alpha = 0.2f))

        OutlinedTextField(
            value = taskName,
            onValueChange = { taskName = it },
            label = { Text("Tasker task name", color = MatrixGreen.copy(alpha = 0.4f)) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            textStyle = TextStyle(fontFamily = FontFamily.Monospace, color = MatrixGreen, fontSize = 13.sp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MatrixGreen.copy(alpha = 0.6f),
                unfocusedBorderColor = MatrixGreen.copy(alpha = 0.2f),
                cursorColor = MatrixGreen,
            ),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { dispatch() }),
        )
        OutlinedTextField(
            value = param1,
            onValueChange = { param1 = it },
            label = { Text("param1 (optional)", color = MatrixGreen.copy(alpha = 0.4f)) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            textStyle = TextStyle(fontFamily = FontFamily.Monospace, color = MatrixGreen, fontSize = 13.sp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MatrixGreen.copy(alpha = 0.6f),
                unfocusedBorderColor = MatrixGreen.copy(alpha = 0.2f),
                cursorColor = MatrixGreen,
            ),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { dispatch() }),
        )
        Button(
            onClick = ::dispatch,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = MatrixGreen.copy(alpha = 0.15f),
                contentColor = MatrixGreen,
            ),
        ) {
            Icon(Icons.Filled.Send, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Dispatch task", fontFamily = FontFamily.Monospace)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PromptsTab(
    app: HorizonsApplication,
    scope: kotlinx.coroutines.CoroutineScope,
    onCommandSelected: (String) -> Unit,
) {
    val commands by app.savedCommands.commands.collectAsState()
    val grouped = commands.groupBy { it.category }

    var newLabel by remember { mutableStateOf("") }
    var newCommand by remember { mutableStateOf("") }
    var newCategory by remember { mutableStateOf("") }

    Column(
        Modifier.fillMaxSize().padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            "Saved Commands",
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            color = MatrixGreen,
        )

        LazyColumn(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            grouped.forEach { (category, cmds) ->
                item(key = "header_$category") {
                    Text(
                        category.uppercase(),
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        color = MatrixGreen.copy(alpha = 0.6f),
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
                item(key = "grid_$category") {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        cmds.forEach { cmd ->
                            PromptCard(
                                cmd = cmd,
                                onTap = { onCommandSelected(cmd.command) },
                                onDelete = { scope.launch { app.savedCommands.remove(cmd.label) } },
                            )
                        }
                    }
                }
            }
        }

        HorizontalDivider(color = MatrixGreen.copy(alpha = 0.2f))

        Text(
            "ADD COMMAND",
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            fontSize = 11.sp,
            color = MatrixGreen.copy(alpha = 0.6f),
        )

        OutlinedTextField(
            value = newLabel,
            onValueChange = { newLabel = it },
            label = { Text("Label", color = MatrixGreen.copy(alpha = 0.4f)) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            textStyle = TextStyle(fontFamily = FontFamily.Monospace, color = MatrixGreen, fontSize = 13.sp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MatrixGreen.copy(alpha = 0.6f),
                unfocusedBorderColor = MatrixGreen.copy(alpha = 0.2f),
                cursorColor = MatrixGreen,
            ),
        )
        OutlinedTextField(
            value = newCommand,
            onValueChange = { newCommand = it },
            label = { Text("Command", color = MatrixGreen.copy(alpha = 0.4f)) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            textStyle = TextStyle(fontFamily = FontFamily.Monospace, color = MatrixGreen, fontSize = 13.sp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MatrixGreen.copy(alpha = 0.6f),
                unfocusedBorderColor = MatrixGreen.copy(alpha = 0.2f),
                cursorColor = MatrixGreen,
            ),
        )
        OutlinedTextField(
            value = newCategory,
            onValueChange = { newCategory = it },
            label = { Text("Category (optional)", color = MatrixGreen.copy(alpha = 0.4f)) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            textStyle = TextStyle(fontFamily = FontFamily.Monospace, color = MatrixGreen, fontSize = 13.sp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MatrixGreen.copy(alpha = 0.6f),
                unfocusedBorderColor = MatrixGreen.copy(alpha = 0.2f),
                cursorColor = MatrixGreen,
            ),
        )
        Button(
            onClick = {
                val label = newLabel.trim()
                val command = newCommand.trim()
                if (label.isNotEmpty() && command.isNotEmpty()) {
                    val cat = newCategory.trim().ifEmpty { "general" }
                    scope.launch {
                        app.savedCommands.add(SavedCommand(label, command, cat))
                    }
                    newLabel = ""
                    newCommand = ""
                    newCategory = ""
                }
            },
            enabled = newLabel.isNotBlank() && newCommand.isNotBlank(),
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = MatrixGreen.copy(alpha = 0.15f),
                contentColor = MatrixGreen,
            ),
        ) {
            Icon(Icons.Filled.Add, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Save Command", fontFamily = FontFamily.Monospace)
        }

        if (commands.isNotEmpty()) {
            OutlinedButton(
                onClick = {
                    commands.forEach { cmd ->
                        val config = RouterConfig(
                            name = "Script: ${cmd.label}",
                            runtime = "terminal",
                            backend = "bash",
                            model = cmd.command,
                        )
                        app.routerConfigs.add(config)
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFAA77FF)),
            ) {
                Text("Export All to Router", fontFamily = FontFamily.Monospace, fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun PromptCard(
    cmd: SavedCommand,
    onTap: () -> Unit,
    onDelete: () -> Unit,
) {
    Card(
        modifier = Modifier.clickable(onClick = onTap),
        colors = CardDefaults.cardColors(
            containerColor = MatrixGreen.copy(alpha = 0.08f),
        ),
        shape = MaterialTheme.shapes.small,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    cmd.label,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = MatrixGreen,
                )
                Text(
                    cmd.command,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.sp,
                    color = MatrixGreen.copy(alpha = 0.5f),
                    maxLines = 1,
                )
            }
            Spacer(Modifier.width(4.dp))
            Surface(
                modifier = Modifier.size(20.dp).clickable(onClick = onDelete),
                shape = CircleShape,
                color = Color.Transparent,
            ) {
                Icon(
                    Icons.Filled.Close,
                    contentDescription = "Delete",
                    tint = MatrixGreen.copy(alpha = 0.4f),
                    modifier = Modifier.size(14.dp).padding(2.dp),
                )
            }
        }
    }
}

// ── Browser tab ──────────────────────────────────────────────────────────────
// ── Runtime tab — define runtimes here, ship them to the Monitor ────────────
// A definition is parameters + handshake only: binary name, port, health
// endpoint, args template, required assets. Defining launches NOTHING —
// the Monitor acknowledges the definition and green-lights the assets;
// running stays a separate, user-paced step. That's what keeps a bad
// runtime from blowing the system: it can't start until everything checks.

@Composable
private fun RuntimeTab(app: HorizonsApplication) {
    val defs by app.runtimeDefs.defs.collectAsState()

    var name by remember { mutableStateOf("") }
    var binary by remember { mutableStateOf("") }
    var port by remember { mutableStateOf("") }
    var healthPath by remember { mutableStateOf("/health") }
    var args by remember { mutableStateOf("") }
    var assets by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var shipped by remember { mutableStateOf<String?>(null) }

    fun field(
        value: String,
        onChange: (String) -> Unit,
        label: String,
    ): @Composable () -> Unit = {
        OutlinedTextField(
            value = value,
            onValueChange = onChange,
            label = { Text(label, color = MatrixGreen.copy(alpha = 0.4f), fontSize = 11.sp) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            textStyle = TextStyle(fontFamily = FontFamily.Monospace, color = MatrixGreen, fontSize = 12.sp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MatrixGreen.copy(alpha = 0.6f),
                unfocusedBorderColor = MatrixGreen.copy(alpha = 0.2f),
                cursorColor = MatrixGreen,
            ),
        )
    }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            "DEFINED RUNTIMES",
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp,
            color = MatrixGreen,
        )
        defs.forEach { def ->
            Surface(
                color = Color.Black.copy(alpha = 0.5f),
                shape = MaterialTheme.shapes.small,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    Modifier.padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            "${def.name}  :${def.port}${def.healthPath}",
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = MatrixGreen,
                        )
                        Text(
                            "bin: ${def.binaryName}" +
                                if (def.requiredAssets.isNotEmpty())
                                    " · assets: ${def.requiredAssets.size}" else "",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.sp,
                            color = MatrixGreen.copy(alpha = 0.5f),
                        )
                        if (def.builtIn) {
                            Text(
                                "built-in",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 9.sp,
                                color = MatrixGreen.copy(alpha = 0.3f),
                            )
                        }
                    }
                    if (!def.builtIn) {
                        Text(
                            "✕",
                            fontSize = 12.sp,
                            color = Color(0xFFFF4444).copy(alpha = 0.7f),
                            modifier = Modifier
                                .clickable { app.runtimeDefs.remove(def.id) }
                                .padding(6.dp),
                        )
                    }
                }
            }
        }

        HorizontalDivider(color = MatrixGreen.copy(alpha = 0.2f))

        Text(
            "DEFINE NEW RUNTIME",
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp,
            color = MatrixGreen,
        )
        field(name, { name = it }, "Name (e.g. geniex-local)")()
        field(binary, { binary = it }, "Binary file name")()
        field(port, { port = it.filter { c -> c.isDigit() } }, "Port")()
        field(healthPath, { healthPath = it }, "Health endpoint path (handshake)")()
        field(args, { args = it }, "Args template ({model} {port} available)")()
        field(assets, { assets = it }, "Required assets, comma-separated")()
        field(notes, { notes = it }, "Notes")()

        Button(
            onClick = {
                val p = port.toIntOrNull() ?: return@Button
                app.runtimeDefs.add(
                    RuntimeDef(
                        name = name.trim(),
                        binaryName = binary.trim(),
                        port = p,
                        healthPath = healthPath.trim().ifBlank { "/health" },
                        argsTemplate = args.trim(),
                        requiredAssets = assets.split(',')
                            .map { it.trim() }.filter { it.isNotBlank() },
                        notes = notes.trim(),
                    ),
                )
                shipped = name.trim()
                name = ""; binary = ""; port = ""; args = ""; assets = ""; notes = ""
                healthPath = "/health"
            },
            enabled = name.isNotBlank() && binary.isNotBlank() && port.toIntOrNull() != null,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = MatrixGreen.copy(alpha = 0.15f),
                contentColor = MatrixGreen,
            ),
        ) {
            Text("Define & ship to Monitor", fontFamily = FontFamily.Monospace)
        }
        shipped?.let {
            Text(
                "✓ '$it' shipped — check its green lights in Monitor / console",
                fontFamily = FontFamily.Monospace,
                fontSize = 10.sp,
                color = MatrixGreen.copy(alpha = 0.7f),
            )
        }
        Spacer(Modifier.height(24.dp))
    }
}
