package pro.zentrades.android

import android.annotation.SuppressLint
import android.content.Context
import android.net.Uri
import android.util.Log
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebView

class MyWebChromeClient(private val mainActivity: MainActivity) : WebChromeClient() {

    // For Android 5.0+
    @SuppressLint("IntentReset")
    override fun onShowFileChooser(
        webView: WebView?,
        filePathCallback: ValueCallback<Array<Uri>>?,
        fileChooserParams: FileChooserParams?
    ): Boolean {
        Log.d("ZenTrades", "onShowFileChooser")

        mainActivity.permissionChecker.checkCameraPermission()
        DataHolder.uploadCallback = filePathCallback

        return true
    }

}