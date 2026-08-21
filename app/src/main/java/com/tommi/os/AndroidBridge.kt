package com.tommi.os

import android.app.Activity
import android.content.Context
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.widget.Toast
import org.json.JSONObject

class AndroidBridge(
    private val activity: Activity,
    private val webView: WebView
) {
    private val mainHandler = Handler(Looper.getMainLooper())

    @JavascriptInterface
    fun postMessage(message: String) {
        // Broadcast or handle message from web app
        mainHandler.post {
            try {
                // Log or relay event to native components if needed
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    @JavascriptInterface
    fun showToast(message: String) {
        mainHandler.post {
            Toast.makeText(activity, message, Toast.LENGTH_SHORT).show()
        }
    }

    @JavascriptInterface
    fun requestPermission(permission: String): Boolean {
        // Native permission request hook
        return true
    }

    @JavascriptInterface
    fun getDeviceInfo(): String {
        val info = JSONObject()
        info.put("manufacturer", Build.MANUFACTURER)
        info.put("model", Build.MODEL)
        info.put("osVersion", Build.VERSION.RELEASE)
        info.put("sdkInt", Build.VERSION.SDK_INT)
        info.put("platform", "android")
        info.put("isNativeApp", true)
        return info.toString()
    }
}
