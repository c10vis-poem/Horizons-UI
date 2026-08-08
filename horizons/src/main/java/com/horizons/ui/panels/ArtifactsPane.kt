package com.horizons.ui.panels

import android.content.Context
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.horizons.HorizonsApplication
import com.horizons.core.state.ChatSession
import com.horizons.core.state.ConfigStatus
import com.horizons.core.state.SavedCommand
import com.horizons.ui.FilmGrainBackground
import com.horizons.ui.theme.HorizonsColors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val dateFormat = SimpleDateFormat("MMM d, yyyy HH:mm", Locale.US)

@Composable
fun ArtifactsPane(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val ctx = LocalContext.current
    val app = ctx.applicationContext as HorizonsApplication
    val scope = rememberCoroutineScope()

    val sessions by app.chatHistory.sessions.collectAsState()
    val commands by app.savedCommands.commands.collectAsState()

    Box(modifier = modifier.fillMaxSize()) {
    FilmGrainBackground()
    SelectionContainer {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // ── Header ───────────────────────────────────────────────────────
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Text("←", fontSize = 20.sp, color = HorizonsColors.TileArtifacts)
            }
            Text(
                "ARCHIVES",
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                color = HorizonsColors.TileArtifacts,
            )
            Text(
                "  / archive",
                fontFamily = FontFamily.Monospace,
                fontSize = 12.sp,
                color = HorizonsColors.TileArtifacts.copy(alpha = 0.5f),
            )
        }

        HorizontalDivider(color = HorizonsColors.TileArtifacts.copy(alpha = 0.2f))

        // ── File Archive — a real file manager over filesDir/archive ─────
        SectionHeader("File Archive", HorizonsColors.TileArtifacts)
        ArchiveFileManager(app)

        HorizontalDivider(color = HorizonsColors.TileArtifacts.copy(alpha = 0.2f))

        // ── Archived Configs ─────────────────────────────────────────────
        val configs by app.routerConfigs.configs.collectAsState()
        val archivedConfigs = configs.filter { it.status == ConfigStatus.ARCHIVED }

        SectionHeader("Archived Configs", HorizonsColors.TileArtifacts)

        if (archivedConfigs.isEmpty()) {
            PlaceholderCard("No archived router configurations. Archive configs from the Router.")
        } else {
            archivedConfigs.forEach { config ->
                Surface(
                    color = HorizonsColors.Surface,
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(
                                    config.name,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = HorizonsColors.TileArtifacts,
                                )
                                Text(
                                    "${config.runtime.ifBlank { "—" }} / ${config.backend.ifBlank { "—" }} / ${config.model.ifBlank { "—" }}",
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                                )
                                Text(
                                    dateFormat.format(Date(config.createdAt)),
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                                )
                            }
                            Text(
                                "Restore",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = HorizonsColors.TileArtifacts,
                                modifier = Modifier
                                    .clickable {
                                        app.routerConfigs.setStatus(config.id, ConfigStatus.INCOMPLETE)
                                    }
                                    .padding(8.dp),
                            )
                            Text(
                                "Delete",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.error,
                                modifier = Modifier
                                    .clickable { app.routerConfigs.remove(config.id) }
                                    .padding(8.dp),
                            )
                        }
                    }
                }
            }
        }

        HorizontalDivider(color = HorizonsColors.TileArtifacts.copy(alpha = 0.2f))

        // ── Chat Archives ────────────────────────────────────────────────
        SectionHeader("Chat Archives", HorizonsColors.TileArtifacts)

        if (sessions.isEmpty()) {
            PlaceholderCard("No archived conversations yet.")
        } else {
            sessions.forEach { session ->
                ChatSessionCard(
                    session = session,
                    onDelete = { scope.launch { app.chatHistory.delete(session.id) } },
                    onShare = { shareChatSession(ctx, session) },
                )
            }
        }

        HorizontalDivider(color = HorizonsColors.TileArtifacts.copy(alpha = 0.2f))

        // ── Scripts ──────────────────────────────────────────────────────
        SectionHeader("Scripts", HorizonsColors.TileArtifacts)

        if (commands.isEmpty()) {
            PlaceholderCard("No saved scripts or commands.")
        } else {
            commands.forEach { cmd ->
                SavedCommandCard(cmd)
            }
        }

        HorizontalDivider(color = HorizonsColors.TileArtifacts.copy(alpha = 0.2f))

        // ── Boot diagnostics ─────────────────────────────────────────────
        SectionHeader("Boot Diagnostics", HorizonsColors.TileArtifacts)

        var diagText by remember { mutableStateOf(com.horizons.core.diag.Breadcrumb.readAll()) }

        Surface(
            color = HorizonsColors.Surface,
            shape = MaterialTheme.shapes.medium,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(Modifier.padding(12.dp)) {
                Text(
                    diagText.takeLast(4000), // last 4 KB so we see most recent
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                )
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Refresh",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        color = HorizonsColors.TileArtifacts,
                        modifier = Modifier.clickable {
                            diagText = com.horizons.core.diag.Breadcrumb.readAll()
                        },
                    )
                    Text(
                        "Clear",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        color = HorizonsColors.TileSettings,
                        modifier = Modifier.clickable {
                            com.horizons.core.diag.Breadcrumb.clear()
                            diagText = com.horizons.core.diag.Breadcrumb.readAll()
                        },
                    )
                    Text(
                        "Failure Report",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        color = HorizonsColors.TileMonitor,
                        modifier = Modifier.clickable {
                            // Rebuilds externalFilesDir/failures/{report.json,REPORT.md}
                            // — adb-pullable, see FAILURES.md.
                            diagText = com.horizons.core.diag.FailureMonitor.snapshot()
                        },
                    )
                }
            }
        }

        // ── Logs ─────────────────────────────────────────────────────────
        SectionHeader("Logs", HorizonsColors.TileArtifacts)

        val logDir = remember { File(app.filesDir, "crash_logs") }
        val logFiles = remember(logDir.lastModified()) {
            if (logDir.exists() && logDir.isDirectory) {
                logDir.listFiles { f ->
                    f.extension == "txt" || f.extension == "log"
                }?.sortedByDescending { it.lastModified() } ?: emptyList()
            } else emptyList()
        }

        if (logFiles.isEmpty()) {
            PlaceholderCard("No logs recorded.")
        } else {
            logFiles.forEach { file ->
                LogFileCard(
                    file = file,
                    onShare = { shareLogFile(ctx, file) },
                )
            }
        }

        // ── Export ───────────────────────────────────────────────────────
        HorizontalDivider(color = HorizonsColors.TileArtifacts.copy(alpha = 0.2f))

        SectionHeader("Export", HorizonsColors.TileArtifacts)

        var exportStatus by remember { mutableStateOf<String?>(null) }

        val exportLauncher = rememberLauncherForActivityResult(
            ActivityResultContracts.CreateDocument("application/json")
        ) { uri ->
            if (uri == null) return@rememberLauncherForActivityResult
            exportStatus = "Exporting…"
            scope.launch {
                try {
                    val tempFile = File(app.cacheDir, "chat_history_export_${System.currentTimeMillis()}.json")
                    val result = app.chatHistory.exportAll(tempFile)
                    result.onSuccess { count ->
                        withContext(Dispatchers.IO) {
                            ctx.contentResolver.openOutputStream(uri)?.use { out ->
                                tempFile.inputStream().use { input -> input.copyTo(out) }
                            } ?: throw IllegalStateException("Cannot open output stream")
                            tempFile.delete()
                        }
                        exportStatus = "Exported $count session${if (count == 1) "" else "s"}."
                    }.onFailure { e ->
                        exportStatus = "Export failed: ${e.message}"
                    }
                } catch (e: Exception) {
                    exportStatus = "Export failed: ${e.message}"
                }
            }
        }

        Surface(
            color = HorizonsColors.Surface,
            shape = MaterialTheme.shapes.medium,
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    exportStatus = null
                    val name = "chat_history_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())}.json"
                    exportLauncher.launch(name)
                },
        ) {
            Column(Modifier.padding(12.dp)) {
                Text(
                    "Export chat history (JSON) →",
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = HorizonsColors.TileArtifacts,
                )
                Text(
                    "Saves every stored session as one JSON array file wherever you choose.",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                )
                exportStatus?.let { status ->
                    Spacer(Modifier.height(4.dp))
                    Text(
                        status,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        color = HorizonsColors.TileArtifacts.copy(alpha = 0.8f),
                    )
                }
            }
        }

        PlaceholderCard(
            "You can also use the share buttons on individual chat archives and logs above to export via the Android share sheet."
        )

        Spacer(Modifier.height(24.dp))
    }
    }
    }
}

// ── Archive File Manager ─────────────────────────────────────────────────────

@Composable
private fun ArchiveFileManager(app: HorizonsApplication) {
    var path by remember { mutableStateOf("") }
    var refresh by remember { mutableStateOf(0) }
    val entries = remember(path, refresh) { app.archive.list(path) }
    var viewingFile by remember { mutableStateOf<String?>(null) }
    var newFolderOpen by remember { mutableStateOf(false) }
    var newFileOpen by remember { mutableStateOf(false) }

    Surface(
        color = HorizonsColors.Surface,
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            // Breadcrumb — tap a segment to jump back
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "archive/",
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = HorizonsColors.TileArtifacts,
                    modifier = Modifier.clickable { path = "" },
                )
                path.split('/').filter { it.isNotBlank() }.forEachIndexed { i, seg ->
                    Text(
                        "$seg/",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 13.sp,
                        color = HorizonsColors.TileArtifacts.copy(alpha = 0.7f),
                        modifier = Modifier.clickable {
                            path = path.split('/').filter { it.isNotBlank() }
                                .take(i + 1).joinToString("/")
                        },
                    )
                }
                Spacer(Modifier.weight(1f))
                Text(
                    "+folder",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = HorizonsColors.TileArtifacts,
                    modifier = Modifier.clickable { newFolderOpen = true }.padding(4.dp),
                )
                Text(
                    "+file",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = HorizonsColors.TileArtifacts,
                    modifier = Modifier.clickable { newFileOpen = true }.padding(4.dp),
                )
            }

            HorizontalDivider(color = HorizonsColors.TileArtifacts.copy(alpha = 0.1f))

            if (entries.isEmpty()) {
                Text(
                    "empty — archive terminal commands, harnesses, or add files here",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f),
                )
            }

            entries.forEach { entry ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            if (entry.isFolder) {
                                path = if (path.isBlank()) entry.name else "$path/${entry.name}"
                            } else {
                                viewingFile = if (viewingFile == entry.name) null else entry.name
                            }
                        },
                ) {
                    Text(
                        if (entry.isFolder) "▸ ${entry.name}/" else "  ${entry.name}",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        fontWeight = if (entry.isFolder) FontWeight.Bold else FontWeight.Normal,
                        color = if (entry.isFolder) HorizonsColors.TileArtifacts
                        else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f),
                        modifier = Modifier.weight(1f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (!entry.isFolder) {
                        Text(
                            "${entry.sizeBytes}B",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f),
                        )
                        Spacer(Modifier.width(8.dp))
                    }
                    Text(
                        "✕",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.error.copy(alpha = 0.6f),
                        modifier = Modifier
                            .clickable {
                                app.archive.delete(path, entry.name)
                                if (viewingFile == entry.name) viewingFile = null
                                refresh++
                            }
                            .padding(horizontal = 6.dp, vertical = 2.dp),
                    )
                }
                // Inline file viewer — tap the file row again to collapse
                if (!entry.isFolder && viewingFile == entry.name) {
                    Surface(
                        color = MaterialTheme.colorScheme.background.copy(alpha = 0.5f),
                        shape = MaterialTheme.shapes.small,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(
                            app.archive.readText(path, entry.name)?.take(6000)
                                ?: "(could not read file)",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f),
                            modifier = Modifier.padding(8.dp),
                        )
                    }
                }
            }
        }
    }

    if (newFolderOpen) {
        ArchiveNameDialog(
            title = "New folder",
            initial = "",
            onConfirm = { name ->
                app.archive.mkdir(path, name)
                newFolderOpen = false
                refresh++
            },
            onDismiss = { newFolderOpen = false },
        )
    }
    if (newFileOpen) {
        ArchiveNameDialog(
            title = "New file",
            initial = "notes.md",
            onConfirm = { name ->
                app.archive.writeText(path, name, "")
                newFileOpen = false
                refresh++
            },
            onDismiss = { newFileOpen = false },
        )
    }
}

@Composable
private fun ArchiveNameDialog(
    title: String,
    initial: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var name by remember { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = HorizonsColors.Surface,
        title = {
            Text(title, fontFamily = FontFamily.Monospace, fontSize = 14.sp, color = HorizonsColors.TileArtifacts)
        },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                singleLine = true,
                label = { Text("Name", color = HorizonsColors.TileArtifacts.copy(alpha = 0.5f)) },
                textStyle = androidx.compose.ui.text.TextStyle(
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                ),
            )
        },
        confirmButton = {
            TextButton(onClick = { if (name.isNotBlank()) onConfirm(name) }) {
                Text("Create", fontFamily = FontFamily.Monospace, color = HorizonsColors.TileArtifacts)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
            }
        },
    )
}

// ── Chat Session Card ────────────────────────────────────────────────────────

@Composable
private fun ChatSessionCard(
    session: ChatSession,
    onDelete: () -> Unit,
    onShare: () -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }

    Surface(
        color = HorizonsColors.Surface,
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { expanded = !expanded },
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        session.title,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = HorizonsColors.TileArtifacts,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(Modifier.height(2.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            dateFormat.format(Date(session.createdAt)),
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "${session.messages.size} msgs",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                        )
                        Spacer(Modifier.width(8.dp))
                        Surface(
                            color = HorizonsColors.TileArtifacts.copy(alpha = 0.15f),
                            shape = MaterialTheme.shapes.small,
                        ) {
                            Text(
                                session.mode,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 12.sp,
                                color = HorizonsColors.TileArtifacts,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            )
                        }
                    }
                }
                IconButton(onClick = onShare, modifier = Modifier.size(32.dp)) {
                    Icon(
                        Icons.Default.Share,
                        contentDescription = "Share",
                        tint = HorizonsColors.TileArtifacts.copy(alpha = 0.7f),
                        modifier = Modifier.size(16.dp),
                    )
                }
                IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                        modifier = Modifier.size(16.dp),
                    )
                }
            }

            AnimatedVisibility(visible = expanded) {
                Column(modifier = Modifier.padding(top = 8.dp)) {
                    HorizontalDivider(
                        color = HorizonsColors.TileArtifacts.copy(alpha = 0.1f),
                        modifier = Modifier.padding(bottom = 6.dp),
                    )
                    session.messages.take(5).forEach { msg ->
                        Text(
                            "${msg.role}: ${msg.text.take(120)}",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(bottom = 4.dp),
                        )
                    }
                    if (session.messages.size > 5) {
                        Text(
                            "... ${session.messages.size - 5} more messages",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f),
                        )
                    }
                }
            }
        }
    }
}

// ── Saved Command Card ───────────────────────────────────────────────────────

@Composable
private fun SavedCommandCard(cmd: SavedCommand) {
    Surface(
        color = HorizonsColors.Surface,
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    cmd.label,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = HorizonsColors.TileArtifacts,
                    modifier = Modifier.weight(1f),
                )
                Surface(
                    color = HorizonsColors.TileArtifacts.copy(alpha = 0.15f),
                    shape = MaterialTheme.shapes.small,
                ) {
                    Text(
                        cmd.category,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        color = HorizonsColors.TileArtifacts,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                    )
                }
            }
            Spacer(Modifier.height(4.dp))
            Text(
                cmd.command,
                fontFamily = FontFamily.Monospace,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

// ── Log File Card ────────────────────────────────────────────────────────────

@Composable
private fun LogFileCard(file: File, onShare: () -> Unit) {
    val preview = remember(file.lastModified()) {
        try {
            file.bufferedReader().useLines { lines ->
                lines.take(3).joinToString("\n")
            }
        } catch (_: Exception) { "" }
    }

    Surface(
        color = HorizonsColors.Surface,
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        file.name,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = HorizonsColors.TileArtifacts,
                    )
                    Text(
                        dateFormat.format(Date(file.lastModified())),
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                    )
                }
                IconButton(onClick = onShare, modifier = Modifier.size(32.dp)) {
                    Icon(
                        Icons.Default.Share,
                        contentDescription = "Share log",
                        tint = HorizonsColors.TileArtifacts.copy(alpha = 0.7f),
                        modifier = Modifier.size(16.dp),
                    )
                }
            }
            if (preview.isNotBlank()) {
                Spacer(Modifier.height(4.dp))
                Text(
                    preview,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

// ── Placeholder Card ─────────────────────────────────────────────────────────

@Composable
private fun PlaceholderCard(message: String) {
    Surface(
        color = HorizonsColors.Surface,
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(
            message,
            fontFamily = FontFamily.Monospace,
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
            modifier = Modifier.padding(16.dp),
        )
    }
}

// ── Section Header ───────────────────────────────────────────────────────────

@Composable
private fun SectionHeader(title: String, color: androidx.compose.ui.graphics.Color) {
    Text(
        title,
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Bold,
        fontSize = 14.sp,
        color = color,
    )
}

// ── Share helpers ────────────────────────────────────────────────────────────

private fun shareChatSession(ctx: Context, session: ChatSession) {
    val text = buildString {
        appendLine("Chat: ${session.title}")
        appendLine("Date: ${dateFormat.format(Date(session.createdAt))}")
        appendLine("Mode: ${session.mode}")
        appendLine("---")
        session.messages.forEach { msg ->
            appendLine("[${msg.role}] ${msg.text}")
        }
    }
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_SUBJECT, "Chat: ${session.title}")
        putExtra(Intent.EXTRA_TEXT, text)
    }
    ctx.startActivity(Intent.createChooser(intent, "Share chat").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
}

private fun shareLogFile(ctx: Context, file: File) {
    val text = try {
        file.readText().take(10_000)
    } catch (_: Exception) { "Could not read log file." }
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_SUBJECT, "Log: ${file.name}")
        putExtra(Intent.EXTRA_TEXT, text)
    }
    ctx.startActivity(Intent.createChooser(intent, "Share log").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
}
