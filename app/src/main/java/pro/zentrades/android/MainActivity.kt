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

    private lateinit var webView: WebView
    private var uploadCallback: ValueCallback<Array<Uri>>? = null   // Using for onShowFileChooser

    // Initialize PermissionChecker
    val permissionChecker = PermissionChecker(this, this)

    companion object {
        const val LOCATION_GPS_ENABLE_CODE = 1001
        const val REQUEST_IGNORE_BATTERY_OPTIMIZATIONS = 2010
        const val FILE_CHOOSER_REQUEST_CODE = 101
//        const val SCRIPT_GET_LOCAL_DATA = "(function() { return localStorage.getItem('user-id'); })();"
    }

    private fun handleFileChooserResultCode(resultCode: Int ,data: Intent?){

        when (resultCode) {
            Activity.RESULT_OK -> {
                Log.d("onActivityResult", "File chooser Result : Ok")
                handleFileURIs(data)
            }
            Activity.RESULT_CANCELED -> {
                Log.d("onActivityResult", "File chooser canceled")
            }
            else -> {
                // Handle other cases where the result might not be ok
                Log.d("onActivityResult", "File chooser result: Not ok")
            }
        }
        // Reset filePathCallback here - so that second time onShowFileChooser can be called
        uploadCallback?.onReceiveValue(null)
        uploadCallback = null

    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == FILE_CHOOSER_REQUEST_CODE) {
                handleFileChooserResultCode(resultCode , data)
        } else if (requestCode == LOCATION_GPS_ENABLE_CODE) {
                if (resultCode == Activity.RESULT_OK) {
                    Log.d("OnActivityResult", "GPS Enable : Ok")
                }

        } else if (requestCode == REQUEST_IGNORE_BATTERY_OPTIMIZATIONS) {

                Log.d("onActivityResult", " Ignore Battery Optimization - resultcode  : $resultCode")

                if (resultCode == Activity.RESULT_OK) {
                    Log.d("onActivityResult", "get permission for ignore battery optimization")
                } else {
                    Log.d("onActivityResult", "permission not granted for ignore battery optimization $resultCode")
                }
        }
    }


    private fun bitmapToUri(bitmap: Bitmap): Uri {
        val bytes = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, bytes)
        val path = MediaStore.Images.Media.insertImage(
            applicationContext.contentResolver, bitmap, "Image", null
        )
        return Uri.parse(path)
    }


    private fun handleFileURIs(data: Intent?) {

        if (data != null && data.data != null) {

            Log.d("handleFileURIs", "Single File Selected - Camera video")
            // File picker selected
            val uri = data.data!!
            uploadCallback?.onReceiveValue(arrayOf(uri))
            uploadCallback = null

        } else if (data?.clipData != null) {

            Log.d("handleFileURIs", "Multiple Files Selected")
            // File picker selected (multiple files)
            val uris = mutableListOf<Uri>()
            for (i in 0 until data.clipData!!.itemCount) {
                val uri = data.clipData!!.getItemAt(i).uri
                uris.add(uri)
            }

            if (uris.isNotEmpty()) {
                uploadCallback?.onReceiveValue(uris.toTypedArray())
                uploadCallback = null
            }

        } else if (data?.extras?.containsKey("data") == true) {

            Log.d("handleFileURIs", "Camera Photo Selected")
            // Camera selected

            val imageBitmap = data.extras?.get("data") as Bitmap?
            if (imageBitmap != null) {
                // Convert Bitmap to Uri
                val uri = bitmapToUri(imageBitmap)
                // Pass the Uri to the uploadCallback
                uploadCallback?.onReceiveValue(arrayOf(uri))
                uploadCallback = null
            }

        } else {

            // Neither file picker nor camera selected
        }

    }


    @RequiresApi(Build.VERSION_CODES.Q)
    override fun onResume() {
        super.onResume()
        Log.d("onResume", "onResume :  OK")

    }

    @SuppressLint("ServiceCast")
    fun isForegroundServiceRunning(context: Context, serviceClass: Class<*>): Boolean {
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val runningServices = activityManager.getRunningServices(Int.MAX_VALUE)
        for (service in runningServices) {
            if (serviceClass.name == service.service.className && service.foreground) {
                return true
            }
        }
        return false
    }


//     access local storage values : user-id , company-id , access-token
     @SuppressLint("SuspiciousIndentation")
     @RequiresApi(Build.VERSION_CODES.O)
    private fun accessLocalStorage(webView: WebView){

        webView.evaluateJavascript(
            """
        (function() {
            var userId = localStorage.getItem('user-id');
            var companyId = localStorage.getItem('company-id');
            var accessToken = localStorage.getItem('access-token');
            return JSON.stringify({userId: userId, companyId: companyId, accessToken: accessToken});
        })();
        """
        ) { jsonString ->
                try {
                      val cleanedJsonString = jsonString.replace("\\", "")
                      val finalJSON = cleanedJsonString.substring(1, cleanedJsonString.length - 1)
                      val dataObject = JSONObject(finalJSON)
                      DataHolder.userId  = dataObject.getString("userId")
                      DataHolder.companyId = dataObject.getString("companyId")
                      val currAccessToken = dataObject.getString("accessToken")


                     Log.d("neel" , "curr-access-token : ${currAccessToken}")



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
//                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
//                                permissionChecker.checkLocationPermission()
//                            }

                        }
                    }else{

                        val isForegroundServiceRunning = isForegroundServiceRunning(this, LocationService::class.java)
                        if (isForegroundServiceRunning) {
                            // Foreground service is running
                            Log.d("neel" , "Service is running and then stop it")
                            stopService(DataHolder.service)
                        } else {
                            // Foreground service is not running
                            Log.d("neel" , "Service is not running")
                        }
                    }

                }catch (e: JSONException){
                    Log.e("neel", "Error parsing JSON: ${e.message}")
                }
        }
    }



////
//    @RequiresApi(Build.VERSION_CODES.O)
//    private fun accessLocalStorage(webView: WebView) {
//        webView.evaluateJavascript(
//            "(function() { return localStorage.getItem('user-id'); })();"
//        ) { userIdValue ->
//            DataHolder.userId = userIdValue.substring(1, userIdValue.length - 1)
//            Log.d("LocalStorage values", "user-id : ${DataHolder.userId}")
//
//            webView.evaluateJavascript(
//                "(function() { return localStorage.getItem('company-id'); })();"
//            ) { companyIdValue ->
//                DataHolder.companyId = companyIdValue.substring(1, companyIdValue.length - 1)
//                Log.d("LocalStorage values", "company-id : ${DataHolder.companyId}")
//
//                webView.evaluateJavascript(
//                    "(function() { return localStorage.getItem('access-token'); })();"
//                ) { TempAccessTokenValue ->
//
//                    if(TempAccessTokenValue != "null"){
//
//                            var tempAT : String = TempAccessTokenValue.substring(1, TempAccessTokenValue.length - 1)
//
//                            if(DataHolder.accessToken == null || DataHolder.accessToken == tempAT){
//
//                                DataHolder.accessToken = TempAccessTokenValue.substring(1, TempAccessTokenValue.length - 1)
//                                Log.d("neel", "access-token is null :  ${DataHolder.accessToken}")
//                                Log.d("neel", "both access token same")
//
//                                permissionChecker.askNotificationPermission()
//                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
//                                    permissionChecker.checkLocationPermission()
//                                }
//
//
//                            }else if(DataHolder.accessToken != tempAT){
//
//                                Log.d("neel", "both access token Not -same")
//                                DataHolder.accessToken = tempAT
//                                Log.d("neel" , "Access-Token : ${DataHolder.accessToken}")
//                                permissionChecker.askNotificationPermissionOnNewAccessToken()
//                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
//                                    permissionChecker.checkLocationPermission()
//                                }
//
//                            }
//                    }else{
//
//                        val isForegroundServiceRunning = isForegroundServiceRunning(this, LocationService::class.java)
//                        if (isForegroundServiceRunning) {
//                            // Foreground service is running
//                            Log.d("neel" , "Service is running and then stop it")
//                            stopService(DataHolder.service)
//                        } else {
//                            // Foreground service is not running
//                            Log.d("neel" , "Service is not running")
//                        }
//                    }
//
//
//                }
//            }
//        }
//    }


    private fun apiResponseFromServer() {

        val requestCall = RetrofitInstance.apiInterface2.getIsCacheCleared()

        requestCall.enqueue(object : Callback<List<responseDataModelItem>> {
            override fun onResponse(
                call: Call<List<responseDataModelItem>>,
                response: Response<List<responseDataModelItem>>
            ) {
                try {
                    if (response.isSuccessful) {
                        val responseData = response.body()
                        // Process responseData according to your application's logic
                        Log.d("apiResponseFromServer", "Success! Response Data: $responseData")
                        Log.d("apiResponseFromServer", "Success! Response Code: ${response}")
                    } else {
                        // Handle unsuccessful response (e.g., non-200 status code)
                        Log.e("apiResponseFromServer", "Unsuccessful response: ${response.code()}")
                    }


                } catch (e: Exception) {
                    Log.e("apiResponseFromServer", "Error: ${e.message}", e)
                }
            }

            override fun onFailure(call: Call<List<responseDataModelItem>>, t: Throwable) {
                Log.d("apiResponseFromServer", "onFailure")
                //                if (t is HttpException) {
                Log.d("apiResponseFromServer", "HTTP Status Code: $t")
                //                }
            }

        })

    }


    @SuppressLint("HardwareIds")
    private fun getAndroidId(context: Context): String {
        return Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
    }


    private fun isIgnoringBatteryOptimizations(): Boolean {
        val powerManager = getSystemService(POWER_SERVICE) as PowerManager
        val packageName = applicationContext.packageName
        return powerManager.isIgnoringBatteryOptimizations(packageName)
    }

    @SuppressLint("BatteryLife")
    private fun requestBatteryOptimizations() {

        Log.d("hello", "ask for requestBatteryOptimization")
        val packageName = applicationContext.packageName
        val intent = Intent()
        intent.setAction(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)
        intent.setData(Uri.parse("package:$packageName"))
        startActivityForResult(intent, REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)


    }

    fun clearApplicationData() {

        val cache = cacheDir               // /data/user/0/pro.zentrades.android/cache
        val appDir = File(cache.parent)   // /data/user/0/pro.zentrades.android

        if (appDir.exists()) {
            val children = appDir.list()
//            Log.d("neel","children : $children")
            for (s in children!!) {
                if (s != "lib") {
                    deleteDir(File(appDir, s))
                    Log.i("neel", "File /data/data/APP_PACKAGE/$s DELETED")
                }
            }
        }
    }

    fun deleteDir(dir: File?): Boolean {
        if (dir != null && dir.isDirectory) {
            val children = dir.list()
            for (i in children.indices) {
                val success = deleteDir(File(dir, children[i]))
                if (!success) {
                    return false
                }
            }
        }
        return dir!!.delete()
    }


    @RequiresApi(Build.VERSION_CODES.O)
    @SuppressLint("SetJavaScriptEnabled", "MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {

        // Handle the splash screen transition.
        installSplashScreen()

        super.onCreate(savedInstanceState)

//        clearApplicationData()     //clean the app data- files , cache , database

//        apiResponseFromServer()    get Response from server for CacheClear or not


        // Check if the app is not already exempted from battery optimizations
//        if (!isIgnoringBatteryOptimizations()) {
//            // Request exemption from battery optimizations
//            Log.d("hello","no permission for ignorebattery")
//            requestBatteryOptimizations()
//        }


        DataHolder.androidId = getAndroidId(applicationContext)
        d("neel", "Device Id : ${DataHolder.androidId}")

        // Check if the activity was started by tapping on a notification
        if (intent != null && intent.extras != null) {
            val dataPayload = intent.extras?.getString("click_action")
            if (dataPayload != null) {
                // Handle the data payload here
                d("neel", "Data payload: $dataPayload")

            }
        }

        window.statusBarColor = resources.getColor(android.R.color.black, theme)   // change the status bar color

        setContentView(R.layout.activity_main)

        webView = findViewById(R.id.webView)
        webView.loadUrl("https://mobile.zentrades.pro/")


        webView.settings.javaScriptEnabled = true
        webView.settings.mixedContentMode = MIXED_CONTENT_COMPATIBILITY_MODE;
        webView.settings.domStorageEnabled = true
        webView.settings.databaseEnabled = true
//        webView.settings.userAgentString = "YourUserAgentString"


        webView.webViewClient = object : WebViewClient() {


            @SuppressLint("QueryPermissionsNeeded")
            override fun shouldOverrideUrlLoading(
                view: WebView?, request: WebResourceRequest?
            ): Boolean {

                val newUrl = request?.url.toString()
                Log.d("neel" ,"override URL : $newUrl")

                if (isGoogleMapsUrl(newUrl)) {
                    d("Override URL : ", "google map url")
                    // Handle Google Maps URL
                    val mapIntent = Intent(Intent.ACTION_VIEW, Uri.parse(newUrl))
                    mapIntent.setPackage("com.google.android.apps.maps") // Specify the package to ensure it opens in Google Maps
                    startActivity(mapIntent)
                    return true // Return true to prevent WebView from loading the URL
                }



                if (newUrl.contains("mailto:") || newUrl.contains("sms:") || newUrl.contains("tel:")) {

                    val mailIntent = Intent(Intent.ACTION_VIEW, Uri.parse(newUrl))
                    startActivity((mailIntent))
                    return true
                }



                if (newUrl.contains(".pdf")) {
                    d("Override", "Override Downloaded file URL is $newUrl")
                    view?.loadUrl(newUrl)           //load the url and auto call to setDownloadListener for downloading file
                    return true
                }


                val desiredPattern = "^https://mobile\\.zentrades\\.pro/.*$"         // check URL at the time of logout and then redirect to login page
                val urlToCheck = newUrl

                if (isMatchingUrl(urlToCheck, desiredPattern)) {

//                    view?.loadUrl(newUrl)
                    Log.d("override" , "URL Loaded : https://mobile.zentrades.pro/")
                    return false  // return false to load the url
                }



                if (newUrl.contains(".png")) {
                    d("override", "Override Image  URL is $newUrl")
                    view?.loadUrl(newUrl)
                    return true
                }


                return false  // load the URL
            }


            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                super.onPageStarted(view, url, favicon)

                d("neel", "page-started $url")

                d("onPageStarted : ", "Value of url(onPage-started) is $url")

//                progressBar.visibility = View.VISIBLE

            }

            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)

                Log.d("onPageFinished", "ok")
                d("neel", "page-Finished $url")

                // take the values from the local storage after page is loaded
                accessLocalStorage(webView)

            }


        }



        webView.webChromeClient = object : WebChromeClient() {

            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                super.onProgressChanged(view, newProgress)

                d("onProgressChange", "ok")
                d("onProgressChange in", "$newProgress")

//                progressBar.progress = newProgress
                if (newProgress >= 100) {
                    d("webChromeClient:: onProgressChange in 100 %", "$newProgress")
//                    progressBar.visibility = View.GONE
                } else {
                    d("webChromeClient:: onProgressChange in", "$newProgress")
//                    progressBar.visibility = View.VISIBLE
                }
            }

            // For Android 5.0+
            @SuppressLint("IntentReset")
            override fun onShowFileChooser(
                webView: WebView?,
                filePathCallback: ValueCallback<Array<Uri>>?,
                fileChooserParams: FileChooserParams?
            ): Boolean {

                fileChooserParams?.let { params ->
                    // Retrieve accepted mime types
                    val acceptTypes = params.acceptTypes
                    Log.d("fileChooserParams", "accept file : $acceptTypes")

                    if (acceptTypes != null) {
                        // Convert the array of MIME types to a string representation
                        val acceptTypesString = acceptTypes.joinToString(", ")
                        // Now, you can use acceptTypesString as needed
                        Log.d("fileChooserParams", "Accepted MIME types: $acceptTypesString")
                    } else {
                        Log.d("fileChooserParams", "No accepted MIME types specified")
                    }

                }

                Log.d("neel", "onShowFileChooser")


                // take permission for camera access
                permissionChecker.checkCameraPermission()

                uploadCallback = filePathCallback
                d("Value of upload-callback is", uploadCallback.toString())

                return true
            }


        }


        webView.setDownloadListener(object : DownloadListener {
            override fun onDownloadStart(
                url: String?,
                userAgent: String?,
                contentDisposition: String?,
                mimeType: String?,
                contentLength: Long
            ) {
                // Your implementation here
                d("Download File", "Downloaded file URL is $url")


                // Create a DownloadManager request
                val request = DownloadManager.Request(Uri.parse(url))

                // Set the MIME type
                request.setMimeType(mimeType)

                // Set the content description
                request.setDescription("Downloading file")

                // Set the title of the download notification
                val fileName = URLUtil.guessFileName(url, contentDisposition, mimeType)
                request.setTitle(fileName)

                // Set the destination folder for the download
                request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName)

                // Allow MediaScanner to scan the downloaded file
                request.allowScanningByMediaScanner()

                // Show a notification when the download is complete
                request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)

                // Get the DownloadManager service and enqueue the request
                val context: Context = webView.context
                val manager = context.getSystemService(DOWNLOAD_SERVICE) as DownloadManager
                manager.enqueue(request)

                // Inform the user that the download has started
                Toast.makeText(context, "Downloading $fileName", Toast.LENGTH_SHORT).show()


            }
        })


    }

    override fun onCameraPermissionGranted() {
        // Handle camera permission granted
        Helperfunction.chooseFileFromMedia(this) // Pass MainActivity reference
    }
    override fun onCameraPermissionDenied() {
        // Handle camera permission denied
        // You can handle this as needed
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

        // Check whether the key event is the Back button and if there's history.
        if (keyCode == KeyEvent.KEYCODE_BACK && webView.canGoBack()) {
            webView.goBack()
            return true
        } else {
            // If it isn't the Back button or there isn't web page history, bubble up to
            // the default system behavior. Probably exit the activity.
            // return super.onKeyDown(keyCode, event)

            // If no page to go back, let the system handle the back press
            val builder = AlertDialog.Builder(this)
            builder.setTitle("Close Application")
            builder.setMessage("Do you want to close the application?")
            builder.setPositiveButton("Yes") { _, _ ->
                // Perform close actions here
                super.onBackPressed()
                //super.onKeyDown(keyCode, event)
            }
            builder.setNegativeButton("No") { _, _ ->
                // Continue with the application
            }

            builder.show()
        }

        return true
    }

    // check the URL is google map's or Not
    private fun isGoogleMapsUrl(url: String): Boolean {
        // Check if the URL starts with "https://maps.google.com/maps" and contains "daddr=" indicating destination coordinates
        return url.startsWith("https://maps.google.com/maps") && url.contains("daddr=")
    }

    fun isMatchingUrl(url: String, pattern: String): Boolean {
        val regex = Pattern.compile(pattern)
        val matcher = regex.matcher(url)
        return matcher.matches()
    }

    override fun onDestroy() {
        super.onDestroy()
//        stopService(service)
    }

}

