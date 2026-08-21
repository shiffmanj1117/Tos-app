package com.tommi.os

import android.graphics.Bitmap
import android.net.http.SslError
import android.util.Log
import android.webkit.SslErrorHandler
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient

class TommiWebViewClient : WebViewClient() {

    override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
        // Keep navigation inside the WebView for all HTTP/HTTPS/asset URLs
        val url = request?.url?.toString() ?: return false
        if (url.startsWith("http://") || url.startsWith("https://") || url.startsWith("file:///")) {
            return false
        }
        return false
    }

    override fun onReceivedSslError(view: WebView?, handler: SslErrorHandler?, error: SslError?) {
        // Proceed on local network SSL errors (e.g. self-signed certificates on local dev server)
        Log.w("TommiOS_SSL", "SSL Error received: ${error?.primaryError}. Proceeding for local network connectivity.")
        handler?.proceed()
    }

    override fun onReceivedError(
        view: WebView?,
        request: WebResourceRequest?,
        error: WebResourceError?
    ) {
        super.onReceivedError(view, request, error)
        Log.e("TommiOS_Error", "WebResourceError: ${error?.description} for URL: ${request?.url}")
    }
}
