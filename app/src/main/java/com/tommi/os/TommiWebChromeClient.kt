package com.tommi.os

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.util.Log
import android.webkit.ConsoleMessage
import android.webkit.GeolocationPermissions
import android.webkit.PermissionRequest
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebView
import androidx.activity.result.ActivityResultLauncher

class TommiWebChromeClient(
    private val fileChooserLauncher: ((Intent) -> Unit)? = null
) : WebChromeClient() {

    var filePathCallback: ValueCallback<Array<Uri>>? = null

    override fun onPermissionRequest(request: PermissionRequest?) {
        // Automatically grant WebRTC camera and microphone permissions to WebView
        request?.let {
            val resources = it.resources
            it.grant(resources)
        }
    }

    override fun onGeolocationPermissionsShowPrompt(
        origin: String?,
        callback: GeolocationPermissions.Callback?
    ) {
        // Grant geolocation permission to web apps
        callback?.invoke(origin, true, false)
    }

    override fun onConsoleMessage(consoleMessage: ConsoleMessage?): Boolean {
        consoleMessage?.let {
            Log.d("TommiOS_Web", "${it.message()} -- From line ${it.lineNumber()} of ${it.sourceId()}")
        }
        return true
    }

    override fun onShowFileChooser(
        webView: WebView?,
        filePathCallback: ValueCallback<Array<Uri>>?,
        fileChooserParams: FileChooserParams?
    ): Boolean {
        this.filePathCallback?.onReceiveValue(null)
        this.filePathCallback = filePathCallback

        val intent = fileChooserParams?.createIntent() ?: Intent(Intent.ACTION_GET_CONTENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "*/*"
        }

        return try {
            fileChooserLauncher?.invoke(intent)
            true
        } catch (e: ActivityNotFoundException) {
            this.filePathCallback = null
            false
        }
    }
}
