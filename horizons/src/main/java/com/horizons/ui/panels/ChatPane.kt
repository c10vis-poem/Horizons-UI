package com.horizons.ui.panels

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.projection.MediaProjectionManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.horizons.ChatMessage
import com.horizons.ChatMode
import com.horizons.HorizonsApplication
import com.horizons.ui.WaterDropletBackground
import com.horizons.ui.theme.HorizonsColors
import com.horizons.audio.AudioRecorder
import com.horizons.core.state.ChatSession
import com.horizons.fgs.LiveChatService
import com.horizons.fgs.ScreenShareService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.atomic.AtomicReference

private val ChatAccent = HorizonsColors.TileChat
private val CarbonBg = Color(0xFF1A1E22)
private val CarbonCard = Color(0xFF252A30)

@Composable
fun ChatPane(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val app = context.applicationContext as HorizonsApplication
    val messages by app.chatMessages.collectAsState()
    val busy by app.chatBusy.collectAsState()
    val pendingJpeg by app.pendingScreenJpeg.collectAsState()
    val currentMode by app.chatMode.collectAsState()
    val sessions by app.chatHistory.sessions.collectAsState()

    var input by remember { mutableStateOf("") }
    var isRecording by remember { mutableStateOf(false) }
    val recordJobRef = remember { AtomicReference<kotlinx.coroutines.Job?>(null) }
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    val snackbarHostState = remember { SnackbarHostState() }
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    var showPlusMenu by remember { mutableStateOf(false) }

    val lastText = messages.lastOrNull()?.text ?: ""
    LaunchedEffect(lastText) {
        if (messages.isNotEmpty()) listState.scrollToItem(messages.size - 1)
    }

    val backendStatus by app.llmRuntime.backendStatus.collectAsState()
    val modelReady = backendStatus != "idle" && backendStatus != "loading…"

    val screenShareLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            val serviceIntent = Intent(context, ScreenShareService::class.java).apply {
                putExtra(ScreenShareService.EXTRA_RESULT_CODE, result.resultCode)
                putExtra(ScreenShareService.EXTRA_RESULT_DATA, result.data)
            }
            ContextCompat.startForegroundService(context, serviceIntent)
        } else {
            scope.launch { snackbarHostState.showSnackbar("Screen share permission denied") }
        }
    }

    fun stopFgsIfRunning() {
        context.stopService(Intent(context, LiveChatService::class.java))
        context.stopService(Intent(context, ScreenShareService::class.java))
    }

    fun hasMicPermission(): Boolean =
        ContextCompat.checkSelfPermission(context, android.Manifest.permission.RECORD_AUDIO) ==
            android.content.pm.PackageManager.PERMISSION_GRANTED

    fun selectMode(mode: ChatMode) {
        if ((mode == ChatMode.A || mode == ChatMode.B) && !hasMicPermission()) {
            scope.launch { snackbarHostState.showSnackbar("Grant microphone permission first") }
            return
        }
        when (mode) {
            ChatMode.A -> {
                stopFgsIfRunning()
                app.setSessionMode("live")
                val projectionManager =
                    context.getSystemService(android.content.Context.MEDIA_PROJECTION_SERVICE)
                            as MediaProjectionManager
                screenShareLauncher.launch(projectionManager.createScreenCaptureIntent())
            }
            ChatMode.B -> {
                stopFgsIfRunning()
                app.setSessionMode("voice")
                ContextCompat.startForegroundService(
                    context, Intent(context, LiveChatService::class.java),
                )
            }
            ChatMode.C -> {
                stopFgsIfRunning()
                app.setSessionMode("standard")
            }
        }
    }

    val attachLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        scope.launch {
            val bytes = withContext(Dispatchers.IO) {
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    val bmp = BitmapFactory.decodeStream(stream) ?: return@use null
                    val longest = maxOf(bmp.width, bmp.height)
                    val scaled = if (longest > 1024) {
                        val s = 1024f / longest
                        Bitmap.createScaledBitmap(
                            bmp,
                            (bmp.width * s).toInt().coerceAtLeast(1),
                            (bmp.height * s).toInt().coerceAtLeast(1),
                            true
                        ).also { bmp.recycle() }
                    } else bmp
                    val out = ByteArrayOutputStream()
                    scaled.compress(Bitmap.CompressFormat.JPEG, 85, out)
                    scaled.recycle()
                    out.toByteArray()
                }
            }
            if (bytes != null) app.pendingScreenJpeg.value = bytes
        }
    }

    fun onMicClick() {
        if (isRecording) {
            recordJobRef.getAndSet(null)?.cancel()
            isRecording = false
        } else if (!busy) {
            isRecording = true
            val job = scope.launch {
                val pcm = app.voiceLoop.recordOnce()
                isRecording = false
                if (pcm != null && pcm.isNotEmpty()) {
                    val text = app.transcribeAudio(pcm, AudioRecorder.SAMPLE_RATE)
                    if (text.isNotBlank() && !text.startsWith("[")) input = text
                }
            }
            recordJobRef.set(job)
        }
    }

    fun submit() {
        val text = input.trim()
        val jpeg = app.pendingScreenJpeg.value
        if (jpeg != null) {
            app.screenAsk(jpeg, text.ifBlank { "What is in this image?" })
            app.pendingScreenJpeg.value = null
            input = ""
        } else if (text.isNotBlank() && !busy) {
            app.sendChat(text)
            input = ""
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ChatSidePanel(
                sessions = sessions,
                onNewSession = {
                    app.newChatSession()
                    scope.launch { drawerState.close() }
                },
                onLoadSession = { id ->
                    app.loadSession(id)
                    scope.launch { drawerState.close() }
                },
                onDeleteSession = { id ->
                    scope.launch { app.chatHistory.delete(id) }
                },
                activeSessionId = app.activeSessionId,
            )
        },
        modifier = modifier,
    ) {
        Box(Modifier.fillMaxSize()) {
        WaterDropletBackground()
        Column(Modifier.fillMaxSize()) {

            // ── Top bar: mode toggle + history button ─────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(CarbonBg)
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = { scope.launch { drawerState.open() } }) {
                    Text("☰", fontSize = 18.sp, color = ChatAccent)
                }
                FilterChip(
                    selected = currentMode == ChatMode.C,
                    onClick = { selectMode(ChatMode.C) },
                    label = { Text("Standard", fontSize = 13.sp) },
                )
                FilterChip(
                    selected = currentMode == ChatMode.A,
                    onClick = { selectMode(ChatMode.A) },
                    label = { Text("Live", fontSize = 13.sp) },
                )
                FilterChip(
                    selected = currentMode == ChatMode.B,
                    onClick = { selectMode(ChatMode.B) },
                    label = { Text("Voice", fontSize = 13.sp) },
                )
            }

            // ── Backend status banner ─────────────────────────────────────────
            val isCloud = backendStatus.contains("Cloud")
            val isNoBackend = backendStatus.contains("no backend")
            val bannerColor = when {
                !modelReady -> MaterialTheme.colorScheme.errorContainer
                isNoBackend -> MaterialTheme.colorScheme.errorContainer
                isCloud -> MaterialTheme.colorScheme.tertiaryContainer
                else -> MaterialTheme.colorScheme.primaryContainer
            }
            val bannerText = when {
                !modelReady -> "Loading…"
                isNoBackend -> "No backend — add API key in Settings or start daemon"
                isCloud -> "Cloud · $backendStatus"
                else -> backendStatus
            }
            Surface(color = bannerColor, modifier = Modifier.fillMaxWidth()) {
                Text(
                    bannerText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                )
            }

            // ── Message list (carbon tile bubbles) ────────────────────────────
            LazyColumn(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                state = listState,
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                items(messages) { msg -> CarbonBubble(msg) }
            }

            // ── Attachment thumbnail strip ────────────────────────────────────
            if (pendingJpeg != null) {
                val bmp: Bitmap? = remember(pendingJpeg!!.size) {
                    BitmapFactory.decodeByteArray(pendingJpeg, 0, pendingJpeg!!.size)
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(CarbonBg)
                        .padding(horizontal = 12.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    if (bmp != null) {
                        Image(
                            bitmap = bmp.asImageBitmap(),
                            contentDescription = "Attached image",
                            modifier = Modifier.size(48.dp),
                            contentScale = ContentScale.Crop,
                        )
                    }
                    Text(
                        "Image attached — ask a question",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.7f),
                        modifier = Modifier.weight(1f),
                    )
                    IconButton(onClick = { app.pendingScreenJpeg.value = null }) {
                        Text("×", fontSize = 18.sp, color = ChatAccent)
                    }
                }
            }

            SnackbarHost(hostState = snackbarHostState)

            // ── Input bar ─────────────────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(CarbonBg)
                    .padding(horizontal = 6.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // + button (attach files / load skills)
                if (!busy) {
                    Box {
                        IconButton(onClick = { showPlusMenu = true }) {
                            Text("+", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = ChatAccent)
                        }
                        DropdownMenu(
                            expanded = showPlusMenu,
                            onDismissRequest = { showPlusMenu = false },
                        ) {
                            DropdownMenuItem(
                                text = { Text("Attach Image") },
                                onClick = {
                                    showPlusMenu = false
                                    attachLauncher.launch("image/*")
                                },
                            )
                            DropdownMenuItem(
                                text = { Text("Attach File") },
                                onClick = {
                                    showPlusMenu = false
                                    attachLauncher.launch("*/*")
                                },
                            )
                            HorizontalDivider()
                            DropdownMenuItem(
                                text = { Text("Load System Prompt") },
                                onClick = {
                                    showPlusMenu = false
                                    input = "/system "
                                },
                            )
                            DropdownMenuItem(
                                text = { Text("Agent Mode") },
                                onClick = {
                                    showPlusMenu = false
                                    input = "/agent "
                                },
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = input,
                    onValueChange = { input = it },
                    modifier = Modifier.weight(1f),
                    placeholder = {
                        Text(
                            if (pendingJpeg != null) "Ask about the image…" else "Message",
                            color = Color.White.copy(alpha = 0.3f),
                        )
                    },
                    singleLine = true,
                    enabled = !busy,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(onSend = { submit() }),
                )

                if (busy) {
                    Button(
                        onClick = { app.stopAll() },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error,
                        ),
                    ) { Text("Stop") }
                } else {
                    IconButton(onClick = { onMicClick() }) {
                        Text(
                            if (isRecording) "●" else "🎙",
                            fontSize = 18.sp,
                            color = if (isRecording) Color.Red else ChatAccent,
                        )
                    }
                    Button(
                        onClick = { submit() },
                        enabled = (input.isNotBlank() || pendingJpeg != null),
                        colors = ButtonDefaults.buttonColors(containerColor = ChatAccent.copy(alpha = 0.2f)),
                    ) {
                        Text(
                            if (pendingJpeg != null) "Ask" else "Send",
                            color = ChatAccent,
                        )
                    }
                }
            }
        }
        }
    }
}

@Composable
private fun CarbonBubble(msg: ChatMessage) {
    val isUser = msg.role == "user"
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start,
    ) {
        Surface(
            shape = MaterialTheme.shapes.medium,
            color = if (isUser) CarbonCard else HorizonsColors.Surface,
            shadowElevation = 2.dp,
            modifier = Modifier.widthIn(max = 300.dp),
        ) {
            SelectionContainer {
                Text(
                    text = msg.text,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontFamily = FontFamily.Default,
                    ),
                    color = if (isUser) ChatAccent else Color.White.copy(alpha = 0.9f),
                )
            }
        }
    }
}

@Composable
private fun ChatSidePanel(
    sessions: List<ChatSession>,
    onNewSession: () -> Unit,
    onLoadSession: (String) -> Unit,
    onDeleteSession: (String) -> Unit,
    activeSessionId: String,
) {
    val dateFmt = remember { SimpleDateFormat("MMM d, HH:mm", Locale.US) }

    ModalDrawerSheet(
        drawerContainerColor = CarbonBg,
        modifier = Modifier.width(280.dp),
    ) {
        Column(
            Modifier
                .fillMaxHeight()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                "CHAT",
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = ChatAccent,
            )
            Text(
                "/ interface",
                fontFamily = FontFamily.Monospace,
                fontSize = 13.sp,
                color = ChatAccent.copy(alpha = 0.5f),
            )

            Spacer(Modifier.height(4.dp))

            // New session button
            Button(
                onClick = onNewSession,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = ChatAccent.copy(alpha = 0.15f),
                ),
            ) {
                Text("+ New Session", fontFamily = FontFamily.Monospace, color = ChatAccent)
            }

            HorizontalDivider(color = ChatAccent.copy(alpha = 0.2f))

            Text(
                "History",
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                color = ChatAccent.copy(alpha = 0.7f),
            )

            // Session list
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                items(sessions) { session ->
                    val isActive = session.id == activeSessionId
                    Surface(
                        color = if (isActive) ChatAccent.copy(alpha = 0.1f) else CarbonCard,
                        shape = MaterialTheme.shapes.small,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onLoadSession(session.id) },
                    ) {
                        Row(
                            Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(
                                    session.title.take(30),
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 13.sp,
                                    fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isActive) ChatAccent else Color.White.copy(alpha = 0.8f),
                                    maxLines = 1,
                                )
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text(
                                        dateFmt.format(Date(session.createdAt)),
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 12.sp,
                                        color = Color.White.copy(alpha = 0.3f),
                                    )
                                    if (session.mode != "standard") {
                                        Text(
                                            session.mode.uppercase(),
                                            fontFamily = FontFamily.Monospace,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = ChatAccent.copy(alpha = 0.5f),
                                        )
                                    }
                                    Text(
                                        "${session.messages.size} msgs",
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 12.sp,
                                        color = Color.White.copy(alpha = 0.3f),
                                    )
                                }
                            }
                            if (!isActive) {
                                TextButton(onClick = { onDeleteSession(session.id) }) {
                                    Text("×", fontSize = 14.sp, color = Color.White.copy(alpha = 0.3f))
                                }
                            }
                        }
                    }
                }
            }

            HorizontalDivider(color = ChatAccent.copy(alpha = 0.2f))

            // Bottom settings hint
            Text(
                "Swipe right to close",
                fontFamily = FontFamily.Monospace,
                fontSize = 12.sp,
                color = Color.White.copy(alpha = 0.2f),
                modifier = Modifier.padding(top = 4.dp),
            )
        }
    }
}
