package pro.zentrades.android

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.util.Log
import android.webkit.DownloadListener
import android.webkit.URLUtil
import android.webkit.WebView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

@Suppress("DEPRECATION")
class MyDownloadListener(private val mainActivity: MainActivity , private val webView: WebView) : DownloadListener {

    override fun onDownloadStart(
        url: String?,
        userAgent: String?,
        contentDisposition: String?,
        mimeType: String?,
        contentLength: Long
    ) {

        Log.d("Download File", "Downloaded file URL")
        val request = DownloadManager.Request(Uri.parse(url))
        request.setMimeType(mimeType)
        request.setDescription("Downloading file")
        val fileName = URLUtil.guessFileName(url, contentDisposition, mimeType)
        request.setTitle(fileName)
        request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName)
        request.allowScanningByMediaScanner()

        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
        // Get the DownloadManager service and enqueue the request
        val context: Context = webView.context
        val manager = context.getSystemService(AppCompatActivity.DOWNLOAD_SERVICE) as DownloadManager
        manager.enqueue(request)
        // Inform the user that the download has started
        Toast.makeText(context, "Downloading $fileName", Toast.LENGTH_SHORT).show()
    }
}