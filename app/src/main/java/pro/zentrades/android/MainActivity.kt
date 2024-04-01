package pro.zentrades.android

import PermissionChecker
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.util.Log.d
import android.view.KeyEvent
import android.webkit.WebSettings.*
import android.webkit.WebView
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import org.json.JSONException
import org.json.JSONObject
import pro.zentrades.android.databinding.ActivityMainBinding


@Suppress("DEPRECATION")
class MainActivity : AppCompatActivity(), PermissionCallback {

    lateinit var binding: ActivityMainBinding

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

        //Handle the splash screen transition.
        installSplashScreen()

        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        DataHolder.androidId = Helperfunction.getAndroidId(applicationContext)
        d("ZenTrades", "Device Id : ${DataHolder.androidId}")

        window.statusBarColor = resources.getColor(android.R.color.black, theme)

        WebViewConfig.configureWebView(binding.webView, this)


        //Helperfunction.clearApplicationData(cacheDir)     //clean the app data- files , cache , database
        //apiResponseFromServerForCacheClear()              //get Response from server for CacheClear or not


    }

    @RequiresApi(Build.VERSION_CODES.Q)
    override fun onResume() {
        super.onResume()
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == FILE_CHOOSER_REQUEST_CODE) {
            Helperfunction.handleFileChooserResultCode(resultCode, data)
        } else if (requestCode == LOCATION_GPS_ENABLE_CODE) {
            if (resultCode == Activity.RESULT_OK) {
                Log.d("OnActivityResult", "GPS Enable : Ok")
            }
        } else if (requestCode == REQUEST_IGNORE_BATTERY_OPTIMIZATIONS) {
            if (resultCode == Activity.RESULT_OK) {
                Log.d("onActivityResult", "Granted for ignore battery optimization ")
            } else {
                Log.d(
                    "onActivityResult", "Not granted for ignore battery optimization"
                )
            }
        }
    }

    fun accessPWALocalStorage(webView: WebView) {
        webView.evaluateJavascript(ACCESS_LOCALSTORAGE_DATA_SCRIPT) { jsonString ->
            try {
                val cleanedJsonString = jsonString.replace("\\", "")
                val finalJSON = cleanedJsonString.substring(1, cleanedJsonString.length - 1)
                val dataObject = JSONObject(finalJSON)

                takePWALocalStorageValue(dataObject)

            } catch (e: JSONException) {
                Log.e("ZenTrades", "Error parsing JSON")
            }
        }
    }


    private fun takePWALocalStorageValue(dataObject: JSONObject) {

        val currUserId = dataObject.getString("userId")
        val currCompanyId = dataObject.getString("companyId")
        val currAccessToken = dataObject.getString("accessToken")

        if (currAccessToken != "null") {

            DataHolder.userID = currUserId
            DataHolder.companyID = currCompanyId

            if (DataHolder.accessToken == null || DataHolder.accessToken == currAccessToken) {
                DataHolder.accessToken = currAccessToken
                permissionChecker.askNotificationPermission()

            } else if (DataHolder.accessToken != currAccessToken) {
                DataHolder.accessToken = currAccessToken
                permissionChecker.askNotificationPermissionOnNewAccessToken()
            }

        } else {

            val isForegroundServiceRunning =
                permissionChecker.isForegroundServiceRunning(this, LocationService::class.java)
            if (isForegroundServiceRunning) {
                Log.d("ZenTrades", "Foreground service is running and then stop it")
                stopService(DataHolder.locationService)
            } else {
                Log.d("ZenTrades", "Foreground service is not running")
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
        Helperfunction.getTokenFromFCM(binding.webView)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            permissionChecker.checkLocationPermission()
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK && binding.webView.canGoBack()) {
            binding.webView.goBack()
            return true
        } else {
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

