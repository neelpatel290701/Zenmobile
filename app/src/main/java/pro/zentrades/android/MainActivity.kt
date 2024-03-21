package pro.zentrades.android

import PermissionChecker
import android.annotation.SuppressLint
import android.app.Activity
import android.app.ActivityManager
import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.PowerManager
import android.provider.ContactsContract.Data
import android.provider.MediaStore
import android.provider.Settings
import android.util.Log
import android.util.Log.d
import android.view.KeyEvent
import android.webkit.DownloadListener
import android.webkit.URLUtil
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebSettings.*
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import org.json.JSONException
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.regex.Pattern


@Suppress("DEPRECATION")
class MainActivity : AppCompatActivity() , PermissionCallback{

    lateinit var webView: WebView
    // Initialize PermissionChecker
    val permissionChecker = PermissionChecker(this, this)
    companion object {
                     const val LOCATION_GPS_ENABLE_CODE = 1001
                     const val REQUEST_IGNORE_BATTERY_OPTIMIZATIONS = 2010
                     const val FILE_CHOOSER_REQUEST_CODE = 101
                     const val ACCESS_LOCALSTORAGE_DATA_SCRIPT = """
                               (function() {
                                    var userId = localStorage.getItem('user-id');
                                    var companyId = localStorage.getItem('company-id');
                                    var accessToken = localStorage.getItem('access-token');
                                    return JSON.stringify({userId: userId, companyId: companyId, accessToken: accessToken});
                               })();
                               """
                     }
    @RequiresApi(Build.VERSION_CODES.O)
    @SuppressLint("SetJavaScriptEnabled", "MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {

        installSplashScreen()           // Handle the splash screen transition.

        super.onCreate(savedInstanceState)

//      Helperfunction.clearApplicationData(cacheDir)     //clean the app data- files , cache , database
//      apiResponseFromServerForCacheClear()              //get Response from server for CacheClear or not

        // Check if the app is not already exempted from battery optimizations
        if (!permissionChecker.isIgnoringBatteryOptimizations()) {
            permissionChecker.requestBatteryOptimizations()
        }

        DataHolder.androidId = Helperfunction.getAndroidId(applicationContext)
        d("neel", "Device Id : ${DataHolder.androidId}")

        // Check if the activity was started by tapping on a notification
        if (intent != null && intent.extras != null) {
            val dataPayload = intent.extras?.getString("click_action")
            if (dataPayload != null) {
                // Handle the data payload here
                d("neel", "Data payload: $dataPayload")

            }
        }

        setContentView(R.layout.activity_main)
        webView = findViewById(R.id.webView)
        webView.loadUrl("https://mobile.zentrades.pro/")

        window.statusBarColor = resources.getColor(android.R.color.black, theme)   // change the status bar color

        webView.settings.javaScriptEnabled = true
        webView.settings.mixedContentMode = MIXED_CONTENT_COMPATIBILITY_MODE;
        webView.settings.domStorageEnabled = true
        webView.settings.databaseEnabled = true
//        webView.settings.userAgentString = "YourUserAgentString"

        webView.webViewClient = MyWebViewClient(this) // Set custom WebViewClient
        webView.webChromeClient = MyWebChromeClient(this) // Set custom WebChromeClient
        webView.setDownloadListener(MyDownloadListener(this))


    }

    @RequiresApi(Build.VERSION_CODES.Q)
    override fun onResume() {
        super.onResume()
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == FILE_CHOOSER_REQUEST_CODE) {
            Helperfunction.handleFileChooserResultCode(resultCode , data)
        } else if (requestCode == LOCATION_GPS_ENABLE_CODE) {
            if (resultCode == Activity.RESULT_OK) {
                Log.d("OnActivityResult", "GPS Enable : Ok")
            }
        } else if (requestCode == REQUEST_IGNORE_BATTERY_OPTIMIZATIONS) {
            if (resultCode == Activity.RESULT_OK) {
                Log.d("onActivityResult", "Granted for ignore battery optimization : $resultCode")
            } else {
                Log.d("onActivityResult", "Not granted for ignore battery optimization : $resultCode")
            }
        }
    }

    fun accessPWALocalStorage(webView: WebView){
        webView.evaluateJavascript(ACCESS_LOCALSTORAGE_DATA_SCRIPT){
                jsonString ->
            try {
                val cleanedJsonString = jsonString.replace("\\", "")
                val finalJSON = cleanedJsonString.substring(1, cleanedJsonString.length - 1)
                val dataObject = JSONObject(finalJSON)

                takePWALocalStorageValue(dataObject)

            } catch (e: JSONException){
                Log.e("neel", "Error parsing JSON: ${e.message}")
            }
        }
    }


    private fun takePWALocalStorageValue(dataObject : JSONObject){

        DataHolder.userId  = dataObject.getString("userId")
        DataHolder.companyId = dataObject.getString("companyId")
        val currAccessToken = dataObject.getString("accessToken")

        Log.d("neel" , "curr-access-token : $currAccessToken")
        if(currAccessToken != "null"){
            if(DataHolder.accessToken == null || DataHolder.accessToken == currAccessToken){
                DataHolder.accessToken = currAccessToken
                Log.d("neel", "both access token same OR Null")
                permissionChecker.askNotificationPermission()
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    permissionChecker.checkLocationPermission()
                }
            }else if(DataHolder.accessToken != currAccessToken){
                Log.d("neel", "both access token Not -same : ${DataHolder.accessToken}")
                DataHolder.accessToken = currAccessToken
                permissionChecker.askNotificationPermissionOnNewAccessToken()
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    permissionChecker.checkLocationPermission()
                }
            }
        }else{
            val isForegroundServiceRunning = permissionChecker.isForegroundServiceRunning(this, LocationService::class.java)
            if (isForegroundServiceRunning) {
                Log.d("neel" , "Foreground service is running and then stop it")
                stopService(DataHolder.service)
            } else {
                Log.d("neel" , "Foreground service is not running")
            }
        }
    }

    override fun onCameraPermissionGranted() {
        Helperfunction.chooseFileFromMedia(this)
    }
    override fun onCameraPermissionDenied() {
        Helperfunction.chooseFileFromMediaWithoutCameraPermission(this)
    }
    @RequiresApi(Build.VERSION_CODES.O)
    override fun onNotificationPermissionGranted() {
         Helperfunction.getTokenFromFCM(webView)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                permissionChecker.checkLocationPermission()
        }
    }
    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK && webView.canGoBack()) {
            webView.goBack()
            return true
        } else {
            // If no page to go back, let the system handle the back press
            val builder = AlertDialog.Builder(this)
            builder.setTitle("Close Application")
            builder.setMessage("Do you want to close the application?")
            builder.setPositiveButton("Yes") { _, _ ->
                super.onBackPressed()
            }
            builder.setNegativeButton("No") { _, _ ->
                // Continue with the application
            }
            builder.show()
        }
        return true
    }
    override fun onDestroy() {
        super.onDestroy()
//        stopService(service)
    }

}

