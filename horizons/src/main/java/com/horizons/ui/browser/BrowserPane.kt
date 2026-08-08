package com.horizons.ui.browser

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.horizons.HorizonsApplication
import com.horizons.core.shell.DaemonLauncher
import kotlinx.coroutines.launch

// An in-app Chromium (Android System WebView) browser with multiple pages, so
// the UI runs its own browser instead of shelling out to a cloud/desktop one.
//
// This is the app's ONE browser implementation. It lives here (not inside a
// panel) so the main browser can sit in the Monitor/console tile while the
// Terminal keeps a shortcut to the same thing — same code, different accent.
//
// The "socket" the front end can attach to: every page gets a JavaScript bridge
// injected as `window.OmniClaw` — web content can call OmniClaw.postMessage(json)
// to talk back to the app, and OmniClaw.daemonPort() to discover the local
// ort_engine port. This is the seam for wiring the browser to the agent/daemon
// later (e.g. an in-page control surface that drives 127.0.0.1:8080).

const val BROWSER_HOME = "https://duckduckgo.com/"

private class BrowserPage(val id: Int, startUrl: String) {
    val url = mutableStateOf(startUrl)
    val title = mutableStateOf("New Tab")
    val canGoBack = mutableStateOf(false)
    val loadError = mutableStateOf<String?>(null)
    var webView: android.webkit.WebView? = null
    var loaded = false
    /** Set when the page was spawned by a target=_blank / window.open call —
     *  its WebView is handed to us already built, so don't load a URL into it. */
    var adopted = false
}

/** Turn free text into a navigable URL: bare domains → https://, anything else → DuckDuckGo search. */
private fun normalizeToUrl(input: String): String {
    val s = input.trim()
    if (s.isEmpty()) return BROWSER_HOME
    if (s.startsWith("http://") || s.startsWith("https://") || s.startsWith("about:")) return s
    val looksLikeHost = s.contains('.') && !s.contains(' ')
    return if (looksLikeHost) "https://$s"
           else "https://duckduckgo.com/?q=" + android.net.Uri.encode(s)
}

private class OmniClawBridge(private val onMessage: (String) -> Unit) {
    @android.webkit.JavascriptInterface
    fun postMessage(msg: String) { onMessage(msg) }

    @android.webkit.JavascriptInterface
    fun daemonPort(): Int = DaemonLauncher.ENGINE_PORT
}

@android.annotation.SuppressLint("SetJavaScriptEnabled")
private fun createBrowserWebView(
    context: android.content.Context,
    page: BrowserPage,
    onMessage: (String) -> Unit,
    onOpenInNewTab: (android.webkit.WebView) -> Unit,
): android.webkit.WebView {
  val web = android.webkit.WebView(context)
  return web.apply {
    layoutParams = android.view.ViewGroup.LayoutParams(
        android.view.ViewGroup.LayoutParams.MATCH_PARENT,
        android.view.ViewGroup.LayoutParams.MATCH_PARENT,
    )
    with(settings) {
        javaScriptEnabled = true
        domStorageEnabled = true
        databaseEnabled = true
        loadWithOverviewMode = true
        useWideViewPort = true
        builtInZoomControls = true
        displayZoomControls = false
        // A regular browser opens target=_blank / window.open in ANOTHER TAB.
        // This used to be false, which silently swallowed those links — and it
        // also broke OAuth flows, which almost always use a popup window.
        setSupportMultipleWindows(true)
        javaScriptCanOpenWindowsAutomatically = true
        mediaPlaybackRequiresUserGesture = true
    }
    // Persist cookies across restarts so logins survive — the device-native
    // sign-in path depends on this.
    android.webkit.CookieManager.getInstance().let { cm ->
        cm.setAcceptCookie(true)
        cm.setAcceptThirdPartyCookies(web, true)
    }
    addJavascriptInterface(OmniClawBridge(onMessage), "OmniClaw")
    webViewClient = object : android.webkit.WebViewClient() {
        override fun onPageStarted(view: android.webkit.WebView, url: String?, favicon: android.graphics.Bitmap?) {
            if (url != null) page.url.value = url
            page.loadError.value = null
        }
        override fun onPageFinished(view: android.webkit.WebView, url: String?) {
            page.url.value = view.url ?: url ?: page.url.value
            page.title.value = view.title?.takeIf { it.isNotBlank() } ?: page.url.value
            page.canGoBack.value = view.canGoBack()
            android.webkit.CookieManager.getInstance().flush()
        }
        override fun onReceivedError(
            view: android.webkit.WebView,
            request: android.webkit.WebResourceRequest,
            error: android.webkit.WebResourceError,
        ) {
            // Only main-frame failures mean the connection is actually gone —
            // a blocked ad or dead favicon shouldn't summon the cat.
            if (request.isForMainFrame) {
                page.loadError.value = error.description?.toString() ?: "connection lost"
            }
        }
    }
    webChromeClient = object : android.webkit.WebChromeClient() {
        override fun onReceivedTitle(view: android.webkit.WebView, title: String?) {
            if (!title.isNullOrBlank()) page.title.value = title
        }
        override fun onCreateWindow(
            view: android.webkit.WebView,
            isDialog: Boolean,
            isUserGesture: Boolean,
            resultMsg: android.os.Message?,
        ): Boolean {
            val transport = resultMsg?.obj as? android.webkit.WebView.WebViewTransport
                ?: return false
            // Hand the new tab's WebView back to the caller; the page strip adopts it.
            val child = android.webkit.WebView(view.context)
            onOpenInNewTab(child)
            transport.webView = child
            resultMsg.sendToTarget()
            return true
        }
    }
    // Save-as: route downloads to the system download manager.
    setDownloadListener { url, userAgent, contentDisposition, mimeType, _ ->
        runCatching {
            val req = android.app.DownloadManager.Request(android.net.Uri.parse(url)).apply {
                setMimeType(mimeType)
                addRequestHeader("User-Agent", userAgent)
                addRequestHeader("Cookie", android.webkit.CookieManager.getInstance().getCookie(url) ?: "")
                setNotificationVisibility(
                    android.app.DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED
                )
                setDestinationInExternalPublicDir(
                    android.os.Environment.DIRECTORY_DOWNLOADS,
                    android.webkit.URLUtil.guessFileName(url, contentDisposition, mimeType),
                )
            }
            (context.getSystemService(android.content.Context.DOWNLOAD_SERVICE)
                as android.app.DownloadManager).enqueue(req)
        }
    }
  }
}

/**
 * The browser surface. [accent] themes it to whichever tile is hosting it.
 */
@Composable
fun BrowserPane(
    app: HorizonsApplication,
    accent: Color,
    modifier: Modifier = Modifier,
) {
    val scope = rememberCoroutineScope()
    val pages = remember { mutableStateListOf(BrowserPage(0, BROWSER_HOME)) }
    var activeIdx by remember { mutableIntStateOf(0) }
    var nextId by remember { mutableIntStateOf(1) }
    var address by remember { mutableStateOf(BROWSER_HOME) }

    val active = pages.getOrNull(activeIdx)

    // Keep the address bar in sync with whichever page is showing.
    LaunchedEffect(activeIdx, active?.url?.value) {
        address = active?.url?.value ?: ""
    }

    // Destroy all WebViews when leaving the browser so we never leak them.
    DisposableEffect(Unit) {
        onDispose { pages.forEach { it.webView?.destroy() } }
    }

    // In-browser back takes priority over leaving the panel.
    BackHandler(enabled = active?.canGoBack?.value == true) {
        active?.webView?.goBack()
    }

    fun go() { active?.webView?.loadUrl(normalizeToUrl(address)) }

    Column(modifier.fillMaxSize().padding(horizontal = 8.dp, vertical = 6.dp)) {
        // ── Page (tab) strip ────────────────────────────────────────────────
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            pages.forEachIndexed { idx, p ->
                val selected = idx == activeIdx
                Surface(
                    color = if (selected) accent.copy(alpha = 0.15f) else Color.Black.copy(alpha = 0.4f),
                    shape = MaterialTheme.shapes.small,
                    modifier = Modifier.padding(end = 6.dp).clickable { activeIdx = idx },
                ) {
                    Row(
                        Modifier.padding(start = 10.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            p.title.value.take(14),
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp,
                            color = if (selected) accent else accent.copy(alpha = 0.5f),
                        )
                        if (pages.size > 1) {
                            Spacer(Modifier.width(4.dp))
                            Text(
                                "✕",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 13.sp,
                                color = accent.copy(alpha = 0.5f),
                                modifier = Modifier
                                    .size(16.dp)
                                    .clickable {
                                        val closing = pages[idx]
                                        closing.webView?.destroy()
                                        pages.removeAt(idx)
                                        if (activeIdx >= pages.size) activeIdx = pages.size - 1
                                    },
                            )
                        }
                    }
                }
            }
            Text(
                "＋",
                fontFamily = FontFamily.Monospace,
                fontSize = 16.sp,
                color = accent,
                modifier = Modifier
                    .padding(horizontal = 4.dp)
                    .clickable {
                        pages.add(BrowserPage(nextId++, BROWSER_HOME))
                        activeIdx = pages.size - 1
                    },
            )
        }

        Spacer(Modifier.height(6.dp))

        // ── Nav + address bar ───────────────────────────────────────────────
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "‹",
                fontFamily = FontFamily.Monospace,
                fontSize = 22.sp,
                color = if (active?.canGoBack?.value == true) accent else accent.copy(alpha = 0.25f),
                modifier = Modifier
                    .size(28.dp)
                    .clickable { if (active?.canGoBack?.value == true) active.webView?.goBack() },
            )
            Text(
                "⟳",
                fontFamily = FontFamily.Monospace,
                fontSize = 16.sp,
                color = accent,
                modifier = Modifier.size(28.dp).clickable { active?.webView?.reload() },
            )
            Spacer(Modifier.width(4.dp))
            OutlinedTextField(
                value = address,
                onValueChange = { address = it },
                modifier = Modifier.weight(1f),
                singleLine = true,
                label = { Text("url / search", color = accent.copy(alpha = 0.4f)) },
                textStyle = TextStyle(fontFamily = FontFamily.Monospace, color = accent, fontSize = 12.sp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = accent.copy(alpha = 0.6f),
                    unfocusedBorderColor = accent.copy(alpha = 0.2f),
                    cursorColor = accent,
                ),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
                keyboardActions = KeyboardActions(onGo = { go() }),
            )
            Spacer(Modifier.width(4.dp))
            Text(
                "→",
                fontFamily = FontFamily.Monospace,
                fontSize = 18.sp,
                color = accent,
                modifier = Modifier.size(28.dp).clickable { go() },
            )
        }

        Spacer(Modifier.height(6.dp))

        // ── The live page — a single host that re-parents the active WebView ─
        Box(Modifier.weight(1f).fillMaxWidth()) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { c -> android.widget.FrameLayout(c) },
                update = updater@ { host ->
                    val page = pages.getOrNull(activeIdx) ?: return@updater
                    val wv = page.webView ?: createBrowserWebView(
                        context = host.context,
                        page = page,
                        onMessage = { msg ->
                            scope.launch {
                                com.horizons.core.diag.Breadcrumb.drop("browser_msg: ${msg.take(120)}")
                            }
                        },
                        onOpenInNewTab = { child ->
                            // A link asked for a new window — give it a real tab.
                            val newPage = BrowserPage(nextId++, "about:blank").also {
                                it.adopted = true
                                it.loaded = true
                                it.webView = child
                            }
                            pages.add(newPage)
                            activeIdx = pages.size - 1
                        },
                    ).also { page.webView = it }
                    if (wv.parent !== host) {
                        (wv.parent as? android.view.ViewGroup)?.removeView(wv)
                        host.removeAllViews()
                        host.addView(wv)
                    }
                    if (!page.loaded) {
                        page.loaded = true
                        wv.loadUrl(page.url.value)
                    }
                },
            )
            val err = active?.loadError?.value
            if (err != null) {
                ConnectionLostCat(
                    accent = accent,
                    reason = err,
                    onRetry = {
                        active?.loadError?.value = null
                        active?.webView?.reload()
                    },
                )
            }
        }
    }
}

// ── Connection-lost cat — 404 for the browser ───────────────────────────────

@Composable
private fun ConnectionLostCat(accent: Color, reason: String, onRetry: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.96f))
            .clickable(onClick = onRetry),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            "404",
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            fontSize = 48.sp,
            color = accent,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            buildString {
                appendLine("  /\\_/\\  ")
                appendLine(" ( o.o ) ")
                appendLine("  > ^ <  ")
                appendLine(" /|   |\\ ")
                appendLine("(_|   |_)")
            },
            fontFamily = FontFamily.Monospace,
            fontSize = 14.sp,
            color = accent,
            lineHeight = 16.sp,
        )
        Spacer(Modifier.height(12.dp))
        Text(
            "CONNECTION_NOT_FOUND",
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            color = accent.copy(alpha = 0.8f),
        )
        Spacer(Modifier.height(4.dp))
        Text(
            reason,
            fontFamily = FontFamily.Monospace,
            fontSize = 12.sp,
            color = accent.copy(alpha = 0.45f),
        )
        Spacer(Modifier.height(16.dp))
        Text(
            "[ tap to retry ]",
            fontFamily = FontFamily.Monospace,
            fontSize = 12.sp,
            color = accent.copy(alpha = 0.35f),
        )
    }
}
