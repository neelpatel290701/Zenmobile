package pro.zentrades.android

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.util.Log
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient

class MyWebViewClient(private val mainActivity: MainActivity) : WebViewClient() {

    @SuppressLint("QueryPermissionsNeeded")
    override fun shouldOverrideUrlLoading(
        view: WebView?, request: WebResourceRequest?
    ): Boolean {

        val newUrl = request?.url.toString()
        Log.d("ZenTrades", "override URL : $newUrl")

        if (Helperfunction.isGoogleMapsUrl(newUrl)) {
            Log.d("Override URL : ", "google map url")
            val mapIntent = Intent(Intent.ACTION_VIEW, Uri.parse(newUrl))
            // Specify the package to ensure it opens in Google Maps
            mapIntent.setPackage("com.google.android.apps.maps")
            mainActivity.startActivity(mapIntent)
            return true // Return true to prevent WebView from loading the URL
        }

        if (newUrl.contains("mailto:") || newUrl.contains("sms:") || newUrl.contains("tel:")) {
            val mailIntent = Intent(Intent.ACTION_VIEW, Uri.parse(newUrl))
            mainActivity.startActivity((mailIntent))
            return true
        }

        if (newUrl.contains(".pdf")) {
            Log.d("Override", "Override Downloaded file URL is $newUrl")
            view?.loadUrl(newUrl)    //load the url and auto call to setDownloadListener for downloading file
            return true
        }

        // check URL at the time of logout and then redirect to login page
        val desiredPattern = "^https://mobile\\.zentrades\\.pro/.*$"
        val urlToCheck = newUrl

        if (Helperfunction.isMatchingUrl(urlToCheck, desiredPattern)) {
            Log.d("override", "URL Loaded : https://mobile.zentrades.pro/")
            return false  // return false to load the url
        }

        if (newUrl.contains(".png")) {
            Log.d("override", "Override Image  URL is $newUrl")
            view?.loadUrl(newUrl)
            return true
        }

        return false  // load the URL
    }

    override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
        super.onPageStarted(view, url, favicon)
        Log.d("ZenTrades", "page-started $url")
        Log.d("onPageStarted : ", "Value of url(onPage-started) is $url")
    }

    override fun onPageFinished(view: WebView?, url: String?) {
        super.onPageFinished(view, url)
        Log.d("onPageFinished", "ok")
        Log.d("ZenTrades", "page-Finished $url")

        // take the values from the local storage after page is loaded
        mainActivity.accessPWALocalStorage(view!!)
    }


}