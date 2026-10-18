package com.example.browser

import android.os.Message
import android.view.View
import android.webkit.WebChromeClient
import android.webkit.WebView

class NexoWebChromeClient(
    private val onProgressChanged: (Int) -> Unit,
    private val onShowCustomViewRequested: (view: View, callback: CustomViewCallback) -> Unit,
    private val onHideCustomViewRequested: () -> Unit
) : WebChromeClient() {

    override fun onProgressChanged(view: WebView?, newProgress: Int) {
        super.onProgressChanged(view, newProgress)
        onProgressChanged(newProgress)
    }

    override fun onShowCustomView(view: View?, callback: CustomViewCallback?) {
        if (view != null && callback != null) {
            onShowCustomViewRequested(view, callback)
        }
    }

    override fun onHideCustomView() {
        onHideCustomViewRequested()
    }

    override fun onCreateWindow(
        view: WebView?,
        isDialog: Boolean,
        isUserGesture: Boolean,
        resultMsg: Message?
    ): Boolean {
        // Strict Protection: Reject all popup windows (window.open, target="_blank")
        return false
    }
}
