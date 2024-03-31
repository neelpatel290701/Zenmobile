package pro.zentrades.android

import android.annotation.SuppressLint
import android.content.Context
import android.net.Uri
import android.util.Log
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebView

class MyWebChromeClient(private val mainActivity: MainActivity) : WebChromeClient() {

    override fun onProgressChanged(view: WebView?, newProgress: Int) {
        super.onProgressChanged(view, newProgress)
        if (newProgress >= 100) {
            Log.d("webChromeClient:: onProgressChange in 100 %", "$newProgress")
        } else {
            Log.d("webChromeClient:: onProgressChange in", "$newProgress")
        }
    }

    // For Android 5.0+
    @SuppressLint("IntentReset")
    override fun onShowFileChooser(
        webView: WebView?,
        filePathCallback: ValueCallback<Array<Uri>>?,
        fileChooserParams: FileChooserParams?
    ): Boolean {
        Log.d("neel", "onShowFileChooser")
        fileChooserParams?.let { params ->

            val acceptTypes = params.acceptTypes
            Log.d("fileChooserParams", "accept file : $acceptTypes")

            if (acceptTypes != null) {
                val acceptTypesString = acceptTypes.joinToString(", ")
                Log.d("fileChooserParams", "Accepted MIME types: $acceptTypesString")
            } else {
                Log.d("fileChooserParams", "No accepted MIME types specified")
            }

        }

        mainActivity.permissionChecker.checkCameraPermission()
        DataHolder.uploadCallback = filePathCallback

        return true
    }

}