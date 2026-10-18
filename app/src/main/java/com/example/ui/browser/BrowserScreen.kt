package com.example.ui.browser

import android.app.Activity
import android.os.SystemClock
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.webkit.CookieManager
import android.webkit.WebSettings
import android.webkit.WebView
import android.widget.FrameLayout
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Mouse
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.filled.ZoomOut
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.browser.NexoWebChromeClient
import com.example.browser.NexoWebViewClient
import com.example.ui.ControlMode
import com.example.ui.NexoMainViewModel
import com.example.ui.common.tvFocusable
import com.example.ui.theme.NexoBlueLight
import com.example.ui.theme.NexoBluePrimary
import com.example.ui.theme.NexoFocusBorder
import com.example.ui.theme.NexoNavyDark
import com.example.ui.theme.NexoRed
import com.example.ui.theme.NexoSurfaceCard
import com.example.ui.theme.NexoSurfaceElevated
import kotlin.math.roundToInt

@Composable
fun BrowserScreen(
    viewModel: NexoMainViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val url by viewModel.browserUrl.collectAsState()
    val controlMode by viewModel.browserControlMode.collectAsState()
    val isToolbarVisible by viewModel.isToolbarVisible.collectAsState()
    val blockedNotice by viewModel.blockedNotice.collectAsState()
    val blockedCount by viewModel.protectionEngine.blockedCountFlow.collectAsState()
    val blockedLogs by viewModel.protectionEngine.logsFlow.collectAsState()
    val showBlockedDialog by viewModel.showBlockedDialog.collectAsState()

    var webViewRef by remember { mutableStateOf<WebView?>(null) }
    var customVideoView by remember { mutableStateOf<View?>(null) }
    var customViewCallback by remember { mutableStateOf<android.webkit.WebChromeClient.CustomViewCallback?>(null) }
    var loadProgress by remember { mutableIntStateOf(0) }
    var isLoading by remember { mutableStateOf(true) }

    // Virtual cursor position
    var cursorX by remember { mutableFloatStateOf(400f) }
    var cursorY by remember { mutableFloatStateOf(300f) }

    val toolbarFocusRequester = remember { FocusRequester() }

    // Screen-on keeper while playing fullscreen video
    DisposableEffect(customVideoView) {
        if (customVideoView != null) {
            activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
        onDispose {
            activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    // Lifecycle observer and WebView cleanup on exit
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            when (event) {
                androidx.lifecycle.Lifecycle.Event.ON_RESUME -> {
                    webViewRef?.onResume()
                    webViewRef?.resumeTimers()
                }
                androidx.lifecycle.Lifecycle.Event.ON_PAUSE -> {
                    webViewRef?.onPause()
                    webViewRef?.pauseTimers()
                }
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            webViewRef?.apply {
                stopLoading()
                loadUrl("about:blank")
                onPause()
                pauseTimers()
                removeAllViews()
                destroy()
            }
            webViewRef = null
        }
    }

    // Strict Back navigation hierarchy
    BackHandler {
        when {
            showBlockedDialog -> {
                viewModel.showBlockedDialog.value = false
            }
            customVideoView != null -> {
                customViewCallback?.onCustomViewHidden()
                customVideoView = null
                customViewCallback = null
            }
            !isToolbarVisible -> {
                viewModel.isToolbarVisible.value = true
                toolbarFocusRequester.requestFocus()
            }
            else -> {
                // From toolbar, exit back to the portal
                viewModel.closeBrowser()
            }
        }
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .onKeyEvent { keyEvent ->
                if (keyEvent.type == KeyEventType.KeyDown) {
                    when (controlMode) {
                        ControlMode.CURSOR -> {
                            val step = 32f
                            when (keyEvent.key) {
                                Key.DirectionLeft -> {
                                    cursorX = (cursorX - step).coerceAtLeast(0f)
                                    true
                                }
                                Key.DirectionRight -> {
                                    cursorX = (cursorX + step).coerceAtMost(1800f)
                                    true
                                }
                                Key.DirectionUp -> {
                                    cursorY = (cursorY - step).coerceAtLeast(0f)
                                    true
                                }
                                Key.DirectionDown -> {
                                    cursorY = (cursorY + step).coerceAtMost(1000f)
                                    true
                                }
                                Key.DirectionCenter, Key.Enter -> {
                                    webViewRef?.let { wv ->
                                        val now = SystemClock.uptimeMillis()
                                        val down = MotionEvent.obtain(now, now, MotionEvent.ACTION_DOWN, cursorX, cursorY, 0)
                                        val up = MotionEvent.obtain(now, now + 50, MotionEvent.ACTION_UP, cursorX, cursorY, 0)
                                        wv.dispatchTouchEvent(down)
                                        wv.dispatchTouchEvent(up)
                                        down.recycle()
                                        up.recycle()
                                    }
                                    true
                                }
                                else -> false
                            }
                        }
                        ControlMode.SCROLL -> {
                            when (keyEvent.key) {
                                Key.DirectionUp -> {
                                    webViewRef?.scrollBy(0, -180)
                                    true
                                }
                                Key.DirectionDown -> {
                                    webViewRef?.scrollBy(0, 180)
                                    true
                                }
                                Key.DirectionLeft -> {
                                    webViewRef?.scrollBy(-180, 0)
                                    true
                                }
                                Key.DirectionRight -> {
                                    webViewRef?.scrollBy(180, 0)
                                    true
                                }
                                else -> false
                            }
                        }
                        ControlMode.FOCUS -> false
                    }
                } else false
            }
    ) {
        val maxWidthPx = constraints.maxWidth.toFloat()
        val maxHeightPx = constraints.maxHeight.toFloat()

        // WebView Container
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                val webViewContext = activity ?: ctx
                WebView(webViewContext).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    settings.apply {
                        javaScriptEnabled = true
                        domStorageEnabled = true
                        databaseEnabled = true
                        cacheMode = WebSettings.LOAD_DEFAULT
                        mediaPlaybackRequiresUserGesture = false
                        mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
                        setSupportMultipleWindows(false)
                        allowFileAccess = false
                        allowContentAccess = false
                        useWideViewPort = true
                        loadWithOverviewMode = true
                        loadsImagesAutomatically = true
                    }
                    CookieManager.getInstance().setAcceptThirdPartyCookies(this, false)

                    webViewClient = NexoWebViewClient(
                        protectionEngine = viewModel.protectionEngine,
                        getSitePolicy = { null },
                        getCurrentOriginHost = { viewModel.browserOriginHost.value },
                        onNavigationBlocked = { reason, destHost ->
                            viewModel.notifyNavigationBlocked(reason, destHost)
                        },
                        onPageTitleChanged = { title ->
                            viewModel.browserTitle.value = title
                        },
                        onLoadingChanged = { loading ->
                            isLoading = loading
                        },
                        onRenderProcessCrashed = {
                            viewModel.notifyNavigationBlocked("Fallo del renderizador", "webView")
                        }
                    )

                    webChromeClient = NexoWebChromeClient(
                        onProgressChanged = { progress ->
                            loadProgress = progress
                            if (progress >= 100) isLoading = false
                        },
                        onShowCustomViewRequested = { view, callback ->
                            customVideoView = view
                            customViewCallback = callback
                        },
                        onHideCustomViewRequested = {
                            customViewCallback?.onCustomViewHidden()
                            customVideoView = null
                            customViewCallback = null
                        }
                    )

                    url?.let { loadUrl(it) }
                    webViewRef = this
                }
            },
            update = { wv ->
                webViewRef = wv
            }
        )

        // Custom Fullscreen Video Overlay (WebChromeClient)
        if (customVideoView != null) {
            AndroidView(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black),
                factory = {
                    FrameLayout(it).apply {
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                    }
                },
                update = { frame ->
                    frame.removeAllViews()
                    customVideoView?.let { v ->
                        (v.parent as? ViewGroup)?.removeView(v)
                        frame.addView(
                            v,
                            FrameLayout.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.MATCH_PARENT
                            )
                        )
                    }
                }
            )
        }

        // Virtual Cursor Indicator
        if (controlMode == ControlMode.CURSOR && customVideoView == null) {
            Box(
                modifier = Modifier
                    .offset {
                        IntOffset(
                            cursorX.coerceIn(0f, maxWidthPx - 28f).roundToInt(),
                            cursorY.coerceIn(0f, maxHeightPx - 28f).roundToInt()
                        )
                    }
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(Color(0xCC3B82F6))
                    .border(2.dp, Color.White, CircleShape)
            )
        }

        // Top Loading Progress Bar
        if (isLoading && loadProgress < 100) {
            LinearProgressIndicator(
                progress = { loadProgress / 100f },
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter),
                color = NexoBlueLight,
                trackColor = Color.Transparent
            )
        }

        // Native Remote Control Toolbar
        AnimatedVisibility(
            visible = isToolbarVisible,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.TopCenter)
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                shape = RoundedCornerShape(12.dp),
                color = NexoNavyDark.copy(alpha = 0.94f),
                border = androidx.compose.foundation.BorderStroke(1.dp, NexoSurfaceElevated)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Left Navigation & Actions
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Button(
                            onClick = { viewModel.closeBrowser() },
                            colors = ButtonDefaults.buttonColors(containerColor = NexoSurfaceElevated),
                            modifier = Modifier
                                .focusRequester(toolbarFocusRequester)
                                .tvFocusable("toolbar_home_button")
                        ) {
                            Icon(Icons.Default.Home, contentDescription = "Volver al portal", modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Portal", fontSize = 14.sp)
                        }

                        Spacer(Modifier.width(8.dp))

                        IconButton(
                            onClick = { if (webViewRef?.canGoBack() == true) webViewRef?.goBack() },
                            modifier = Modifier.tvFocusable("toolbar_back_button")
                        ) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Atrás historial", tint = Color.White)
                        }

                        IconButton(
                            onClick = { if (webViewRef?.canGoForward() == true) webViewRef?.goForward() },
                            modifier = Modifier.tvFocusable("toolbar_forward_button")
                        ) {
                            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Adelante historial", tint = Color.White)
                        }

                        IconButton(
                            onClick = { webViewRef?.reload() },
                            modifier = Modifier.tvFocusable("toolbar_reload_button")
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = "Recargar página", tint = Color.White)
                        }
                    }

                    // Center Control Mode
                    Button(
                        onClick = { viewModel.cycleControlMode() },
                        colors = ButtonDefaults.buttonColors(containerColor = NexoBluePrimary),
                        modifier = Modifier.tvFocusable("toolbar_mode_button")
                    ) {
                        Icon(Icons.Default.Mouse, contentDescription = "Modo de control", modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(controlMode.label, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }

                    // Right Tools (Zoom, Blocked Count, Fullscreen)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = { webViewRef?.zoomOut() },
                            modifier = Modifier.tvFocusable("toolbar_zoom_out_button")
                        ) {
                            Icon(Icons.Default.ZoomOut, contentDescription = "Reducir zoom", tint = Color.White)
                        }

                        IconButton(
                            onClick = { webViewRef?.zoomIn() },
                            modifier = Modifier.tvFocusable("toolbar_zoom_in_button")
                        ) {
                            Icon(Icons.Default.ZoomIn, contentDescription = "Aumentar zoom", tint = Color.White)
                        }

                        Button(
                            onClick = { viewModel.showBlockedDialog.value = true },
                            colors = ButtonDefaults.buttonColors(containerColor = NexoSurfaceCard),
                            modifier = Modifier.tvFocusable("toolbar_blocked_badge")
                        ) {
                            Icon(Icons.Default.Security, contentDescription = "Protección", tint = NexoBlueLight, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("$blockedCount bloqueados", fontSize = 12.sp, color = NexoBlueLight)
                        }

                        Spacer(Modifier.width(8.dp))

                        IconButton(
                            onClick = { viewModel.toggleImmersiveWeb() },
                            modifier = Modifier.tvFocusable("toolbar_fullscreen_button")
                        ) {
                            Icon(Icons.Default.Fullscreen, contentDescription = "Ocultar herramientas", tint = Color.White)
                        }
                    }
                }
            }
        }

        // Notice of Blocked Navigation / Escape
        blockedNotice?.let { msg ->
            Card(
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 24.dp),
                colors = CardDefaults.cardColors(containerColor = NexoSurfaceElevated),
                border = androidx.compose.foundation.BorderStroke(1.dp, NexoRed)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Protección de Navegación", color = NexoRed, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text(msg, color = Color.White, fontSize = 13.sp)
                    }
                    Button(
                        onClick = { viewModel.clearBlockedNotice() },
                        colors = ButtonDefaults.buttonColors(containerColor = NexoSurfaceCard),
                        modifier = Modifier.tvFocusable("dismiss_blocked_notice")
                    ) {
                        Text("Entendido")
                    }
                }
            }
        }

        // Blocked Log Inspection Dialog
        if (showBlockedDialog) {
            AlertDialog(
                onDismissRequest = { viewModel.showBlockedDialog.value = false },
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Security, contentDescription = null, tint = NexoBlueLight)
                        Spacer(Modifier.width(8.dp))
                        Text("Protección Estricta: Registro de Bloqueos")
                    }
                },
                text = {
                    Column {
                        Text(
                            "Total de recursos bloqueados en esta sesión: $blockedCount",
                            fontSize = 14.sp,
                            color = Color.White,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(Modifier.size(12.dp))
                        if (blockedLogs.isEmpty()) {
                            Text("No se han interceptado solicitudes sospechosas todavía.", color = Color.Gray)
                        } else {
                            LazyColumn(modifier = Modifier.size(width = 460.dp, height = 240.dp)) {
                                items(blockedLogs) { log ->
                                    Card(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 4.dp),
                                        colors = CardDefaults.cardColors(containerColor = NexoNavyDark)
                                    ) {
                                        Column(modifier = Modifier.padding(8.dp)) {
                                            Text(log.host, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = NexoBlueLight)
                                            Text(log.reason, fontSize = 12.sp, color = Color.LightGray)
                                        }
                                    }
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = { viewModel.showBlockedDialog.value = false },
                        modifier = Modifier.tvFocusable("close_blocked_dialog_btn")
                    ) {
                        Text("Cerrar")
                    }
                }
            )
        }
    }
}
