package pro.zentrades.android

import android.annotation.SuppressLint
import android.webkit.WebSettings
import android.webkit.WebView

object WebViewConfig {

    @SuppressLint("SetJavaScriptEnabled")
    fun configureWebView(webView: WebView, mainActivity: MainActivity) {

        webView.loadUrl("https://mobile.zentrades.pro/")

        webView.settings.javaScriptEnabled = true
        webView.settings.mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
        webView.settings.domStorageEnabled = true
        webView.settings.databaseEnabled = true

        webView.webViewClient = MyWebViewClient(mainActivity)            // Set custom WebViewClient
        webView.webChromeClient = MyWebChromeClient(mainActivity)        // Set custom WebChromeClient
        webView.setDownloadListener(MyDownloadListener(mainActivity ,webView))
    }
}