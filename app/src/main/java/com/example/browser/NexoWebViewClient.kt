package com.example.browser

import android.graphics.Bitmap
import android.net.Uri
import android.net.http.SslError
import android.webkit.SslErrorHandler
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import com.example.domain.model.SitePolicy
import com.example.protection.NavigationDecision
import com.example.protection.StrictProtectionEngine
import java.io.ByteArrayInputStream

class NexoWebViewClient(
    private val protectionEngine: StrictProtectionEngine,
    private val getSitePolicy: () -> SitePolicy?,
    private val getCurrentOriginHost: () -> String?,
    private val onNavigationBlocked: (reason: String, destinationHost: String) -> Unit,
    private val onPageTitleChanged: (String) -> Unit,
    private val onLoadingChanged: (Boolean) -> Unit,
    private val onRenderProcessCrashed: () -> Unit
) : WebViewClient() {

    override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
        val uri = request?.url ?: return false
        val originHost = getCurrentOriginHost()
        val policy = getSitePolicy()

        return when (val decision = protectionEngine.evaluateNavigation(uri, originHost, policy)) {
            is NavigationDecision.Allow -> {
                false // Allow standard WebView loading
            }
            is NavigationDecision.Block -> {
                onNavigationBlocked(decision.reason, decision.destinationHost)
                true // Block navigation request
            }
        }
    }

    override fun shouldInterceptRequest(view: WebView?, request: WebResourceRequest?): WebResourceResponse? {
        val uri = request?.url ?: return null
        val originHost = getCurrentOriginHost()

        val filterDecision = protectionEngine.evaluateResource(uri, originHost)
        if (filterDecision.shouldBlock) {
            // Return 403 Forbidden so image and script decoders properly treat resource as blocked
            return WebResourceResponse(
                "text/plain",
                "UTF-8",
                403,
                "Forbidden",
                mapOf("Access-Control-Allow-Origin" to "*"),
                ByteArrayInputStream(ByteArray(0))
            )
        }
        return super.shouldInterceptRequest(view, request)
    }

    override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
        super.onPageStarted(view, url, favicon)
        onLoadingChanged(true)
    }

    override fun onPageFinished(view: WebView?, url: String?) {
        super.onPageFinished(view, url)
        onLoadingChanged(false)
        view?.title?.let { onPageTitleChanged(it) }
    }

    override fun onReceivedSslError(view: WebView?, handler: SslErrorHandler?, error: SslError?) {
        // Strict Security: TLS errors are cancelled immediately without exception
        handler?.cancel()
    }

    override fun onRenderProcessGone(view: WebView?, detail: android.webkit.RenderProcessGoneDetail?): Boolean {
        onRenderProcessCrashed()
        return true // Handled gracefully to prevent application termination
    }
}
