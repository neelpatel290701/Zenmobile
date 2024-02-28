package pro.zentrades.android

import android.annotation.SuppressLint
import android.app.Activity
import android.content.ContentValues.TAG
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.os.Environment
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
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import android.Manifest
import android.content.IntentSender
import android.location.LocationManager
import android.os.Build
import java.util.regex.Pattern
import android.provider.Settings
import android.view.View
import android.widget.ProgressBar
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.LocationSettingsRequest
import com.google.android.gms.location.LocationSettingsResponse
import com.google.android.gms.location.SettingsClient
import com.google.android.gms.tasks.OnCompleteListener
import com.google.android.gms.tasks.Task
import com.google.firebase.messaging.FirebaseMessaging
import retrofit2.Call
import retrofit2.HttpException
import retrofit2.Response


@Suppress("DEPRECATION")
class MainActivity : AppCompatActivity() {

    private lateinit var userid : String
    private lateinit var companyid : String
    private lateinit var accesstoken : String
    private lateinit var registrationToken : String

    private var service : Intent ?= null

    private lateinit var webView : WebView
    private var uploadCallback: ValueCallback<Array<Uri>>? = null
//    lateinit var progressBar : ProgressBar


    var redirect = false
    var completely_loaded = true


    companion object {
        private const val LOCATION_GPS_ENABLE_CODE = 1001
    }

    // Declare the launcher at the top of your Activity/Fragment:
    @RequiresApi(Build.VERSION_CODES.O)
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { isGranted: Boolean ->
        if (isGranted) {
            // FCM SDK (and your app) can post notifications.
            Log.d("Neel", "notification-granted")
            getTokenFromFCM()

        } else {
            // TODO: Inform user that that your app will not show notifications.
            d(TAG, "Notification : Not Granted")
            askNotificationPermission()
        }
    }


    @RequiresApi(Build.VERSION_CODES.O)
    private fun getTokenFromFCM(){

        FirebaseMessaging.getInstance().token.addOnCompleteListener(OnCompleteListener { task ->
            if (!task.isSuccessful) {
                Log.w("Firebase Notification", "Fetching FCM registration token failed", task.exception)
                return@OnCompleteListener
            }

            // Get new FCM registration token
            val token = task.result
            registrationToken = token
            d("Firebase Notification", "Token received :  $token")
            Log.d("Neel", "get token")

            val firebaseMessagingService = MyFirebaseMessagingService()
            firebaseMessagingService.onNewToken(token)
            apiRequestToServer()
//
        })
    }

    private fun askNotificationPermission() {
        // This is only necessary for API level >= 33 (TIRAMISU)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) ==
                PackageManager.PERMISSION_GRANTED
            ) {
                // FCM SDK (and your app) can post notifications.
                Log.d("neel", "notification permission ok")

                if( !(::accesstoken.isInitialized)) {    // if permission granted but token is not initialized
                    getTokenFromFCM()
                    Log.d("neel", "token not initialized - called getTokenFromFCM() ")
                }

            } else if (shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS)) {
                // TODO: display an educational UI explaining to the user the features that will be enabled
                //       by them granting the POST_NOTIFICATION permission. This UI should provide the user
                //       "OK" and "No thanks" buttons. If the user selects "OK," directly request the permission.
                //       If the user selects "No thanks," allow the user to continue without notifications.
                d(TAG, "askNotification Permission :  shouldShowRequestPermission rational")
                Log.d("Neel", "asknotification")

                val builder = AlertDialog.Builder(this)
                builder.setTitle("Enable Notifications")
                    .setMessage("Please enable notifications for this app to receive important updates.")
                    .setPositiveButton("Enable") { _, _ ->
                        requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)

                    }
                    .setNegativeButton("Cancel") { dialog, _ ->
                        dialog.dismiss()
                    }
                    .show()

            } else {
                // Directly ask for the permission
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                d(TAG, "askNotification Permission :  launch requsetpermissionlauncher")
            }
        }
    }


    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == 101) {
                if (resultCode == Activity.RESULT_OK) {
                    // Get the selected file URI(s)
                    val result = WebChromeClient.FileChooserParams.parseResult(resultCode, data)
                    uploadFile(result)

                } else {
                    // Handle canceled file selection
                    uploadCallback?.onReceiveValue(null)
                    uploadCallback = null
                }
        }else if(requestCode == LOCATION_GPS_ENABLE_CODE){  // handle result of GPS LOCATION
                if (resultCode == Activity.RESULT_OK){
                        Log.d("neel OnActivityResult of GPS Result" , "After clicking Ok - Location is Enable")
                }
        }
    }

    //background permission result
    private val backgroundLocation = registerForActivityResult(ActivityResultContracts.RequestPermission()){
            if(it){
                Log.d("neel" , "backgroundlocation")
                service?.let { ContextCompat.startForegroundService(this, service!!) }
            }
            // after user give permission for location check for GPS is active or not
            requestDeviceLocationSettings()
    }

    //location permission result
    @RequiresApi(Build.VERSION_CODES.Q)
    val locationPermissionRequest = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        when {

            permissions.getOrDefault(Manifest.permission.ACCESS_FINE_LOCATION, false) -> {
                // Precise location access granted.
                Log.d("neel locationpermission" , "Fine")
                    if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q){

                            if(ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_BACKGROUND_LOCATION)
                                != PackageManager.PERMISSION_GRANTED)  {
                                backgroundLocation.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION)

                            }
                    }

            }

            permissions.getOrDefault(Manifest.permission.ACCESS_COARSE_LOCATION, false) -> {
                // Only approximate location access granted.

            } else -> {
            // No location access granted.
            Log.d("neel locationPermissionRequest", "called checkLocationPermission()")
            checkLocationPermission()

        }

        }
    }
    private fun checkLocationEnabled(context: Context) {
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val isLocationEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)

        if (!isLocationEnabled) {
            // Location services are not enabled, request the user to enable them
            Log.d("neel" ,"Location Service is not Enable")
            requestDeviceLocationSettings()
        } else {
            // Location services are already enabled
            Log.d("neel" ,"Location Service Enable")
        }
    }

    private fun requestDeviceLocationSettings(){
        val locationRequest = LocationRequest.create().apply {
            interval = 10000
            fastestInterval = 5000
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        }
        val builder = LocationSettingsRequest.Builder()
            .addLocationRequest(locationRequest)

        val client: SettingsClient = LocationServices.getSettingsClient(this)
        val task: Task<LocationSettingsResponse> = client.checkLocationSettings(builder.build())

        Log.d("neel" , "requestDeviceLocationSettings")

        task.addOnSuccessListener { locationSettingsResponse ->
            // All location settings are satisfied. The client can initialize
            // location requests here.
            Log.d("neel" , "GPS Enable")

            val state = locationSettingsResponse.locationSettingsStates

            val label = "GPS >> (Present: ${state?.isGpsPresent}  | Usable: ${state?.isGpsUsable} ) \n" +
                        "Network >> ( Present: ${state?.isNetworkLocationPresent} | Usable: ${state?.isNetworkLocationUsable} ) \n" +
                        "Location >> ( Present: ${state?.isLocationPresent} | Usable: ${state?.isLocationUsable} )"

            Log.d("neel" , label)

            Toast.makeText(this@MainActivity,"LOCATION IS ACTIVE" , Toast.LENGTH_LONG).show()
        }

        task.addOnFailureListener { exception ->
            if (exception is ResolvableApiException) {
                // Location settings are not satisfied, but this can be fixed
                // by showing the user a dialog.
                Log.d("neel" , "GPS Not Enable at time of opening the application")
                try {
                    // Show the dialog by calling startResolutionForResult(),
                    // and check the result in onActivityResult().
                    exception.startResolutionForResult(
                        this@MainActivity,
                        LOCATION_GPS_ENABLE_CODE
                    )
                } catch (sendEx: IntentSender.SendIntentException) {
                    // Ignore the error.

                }
            }
        }
    }


    @RequiresApi(Build.VERSION_CODES.Q)
    private fun checkLocationPermission() {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
            ) {

                    Log.d("neel ", "CheckLocationPermission : Ok")

                    //after taking permission check the GPS is unable or not
//                    checkLocationEnabled(this)   --OR--
                    requestDeviceLocationSettings()

                    if(ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_BACKGROUND_LOCATION)
                        == PackageManager.PERMISSION_GRANTED)  {

                        service?.let { ContextCompat.startForegroundService(this, service!!) }
                    }
                    if(ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                        locationPermissionRequest.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION))
                    }


            } else if (shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION)
                 && shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_COARSE_LOCATION)) {

                 Log.d("nee", "CheckLocationPermission  : shouldShowRequestPermissionRationale")

                    val builder: AlertDialog.Builder = AlertDialog.Builder(this)
                    builder.setTitle("Location Settings")
                    builder.setMessage("Location services are disabled. Do you want to enable them?")
                    builder.setPositiveButton("Yes") { _, _ ->

                            locationPermissionRequest.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION,
                                    Manifest.permission.ACCESS_COARSE_LOCATION))
                    }
                    builder.setNegativeButton("No")  { dialog, _ ->
                        dialog.dismiss()
                    }
                    builder.show()

            } else {
                Log.d("neel", "CheckLocationPermission  : create launcher")
                locationPermissionRequest.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION))

            }

        }
    }


    @RequiresApi(Build.VERSION_CODES.Q)
    override fun onResume() {
        super.onResume()
        Log.d(TAG, "onResume :  OK")

    }


    private fun uploadFile(fileUris: Array<Uri>?) {
        // Perform the file upload logic here
        // You can use the file URIs to handle the selected file(s)

        // For example, you might want to display the file name(s)

        fileUris?.forEach { uri ->
            val fileName = uri.lastPathSegment
//            Log.d("Value of fileUris is", fileName ?: "null")

            // Process the file name as needed
        }

        // Once the upload is complete, provide the result to the WebView
        if (uploadCallback != null) {
            uploadCallback?.onReceiveValue(fileUris)
            uploadCallback = null
        }
        else{
            Log.d("Value check for uploadCallBack","No value")
        }

    }

    // access local storage values : user-id , company-id , access-token
    @RequiresApi(Build.VERSION_CODES.O)
    private fun accessLocalStorage(webView: WebView) {

        webView.evaluateJavascript(
            "(function() { return localStorage.getItem('user-id'); })();"
        ) { userIdValue ->
            userid = userIdValue.substring(1, userIdValue.length - 1)
            Log.d("LocalStorage values", "user-id : $userid")

            webView.evaluateJavascript(
                "(function() { return localStorage.getItem('company-id'); })();"
            ) { companyIdValue ->
                companyid = companyIdValue.substring(1,companyIdValue.length-1)
                Log.d("LocalStorage values", "company-id : $companyid")

                webView.evaluateJavascript(
                    "(function() { return localStorage.getItem('access-token'); })();"
                ) { accessTokenValue ->
                    accesstoken = accessTokenValue.substring(1,accessTokenValue.length-1)
                    Log.d("LocalStorage values", "access-token : $accesstoken")

//                    // After all variables are initialized, call apiRequestToServer()
//                    Handler().postDelayed({
//                        apiRequestToServer()
//                    }, 20000)
//                    apiRequestToServer()
                    Log.d("Neel", "accesslocalstorage")
                    Log.d("Neel userid", "it $companyid")


                    // before login the variable value is NULL so after login it is called and if all set then askNotification Permission
                    if(userid != "ul") {
                        Log.d("Neel ----", "ok")
                         askNotificationPermission()
                    }



                }
            }
        }
    }



    @RequiresApi(Build.VERSION_CODES.O)
    private fun apiRequestToServer(){


        Log.d("apiRequestToServer",userid)
        Log.d("Neel", "api-request $userid")

        val userData = dataModelItem(registrationToken,"FCM")
        RetrofitInstance.apiInterface.sendToken(userid , companyid, accesstoken, userData ).enqueue(object :
            retrofit2.Callback<dataModelItem?>{
            override fun onResponse(
                call: Call<dataModelItem?>,
                response: Response<dataModelItem?>
            ) {
                try {
                    if (response.isSuccessful) {
                        val responseData = response.body()
                        // Process responseData according to your application's logic
                        Log.d("MainActivity POST", "Success! Response Data: $responseData")
                        Log.d("MainActivity POST", "Success! Response Code: ${response}")
                    } else {
                        // Handle unsuccessful response (e.g., non-200 status code)
                        Log.e("MainActivity POST", "Unsuccessful response: ${response.code()}")
                    }

                } catch (e: Exception) {
                    Log.e("MainActivity POST", "Error: ${e.message}", e)
                }

            }

            override fun onFailure(call: Call<dataModelItem?>, t: Throwable) {

                Log.d("MainActivity POST", "onFailure")
//                if (t is HttpException) {
                    Log.d("MainActivity POST", "HTTP Status Code: $t")
//                }
            }


        })

    }


    @RequiresApi(Build.VERSION_CODES.O)
    @SuppressLint("SetJavaScriptEnabled", "MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        service = Intent(this , LocationService::class.java)


        // Check if the activity was started by tapping on a notification
        if (intent != null && intent.extras != null) {
            val dataPayload = intent.extras?.getString("click_action")
            if (dataPayload != null) {
                // Handle the data payload here
                Log.d("MainActivity", "Data payload: $dataPayload")

            }
        }


//        this.window.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,WindowManager.LayoutParams.FLAG_FULLSCREEN)    // this line for remove the status bar

        window.statusBarColor = resources.getColor(android.R.color.black, theme)   // change the status bar color

        setContentView(R.layout.activity_main)


//        val overlayLayout = findViewById<FrameLayout>(R.id.overlayLayout)
        webView= findViewById(R.id.webView)
        webView.loadUrl("https://mobile.zentrades.pro/")


//        progressBar = findViewById(R.id.progressBar)


        webView.settings.javaScriptEnabled = true
        webView.settings.mixedContentMode = MIXED_CONTENT_COMPATIBILITY_MODE;
        webView.settings.domStorageEnabled = true
        webView.settings.databaseEnabled = true
//        webView.settings.userAgentString = "YourUserAgentString"



        // Check and request location permission if needed
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            checkLocationPermission()
        }


        webView.webViewClient = object : WebViewClient(){


            @SuppressLint("QueryPermissionsNeeded")
            override fun shouldOverrideUrlLoading(
                view: WebView?,
                request: WebResourceRequest?
            ): Boolean {

                val newUrl = request?.url.toString()
                Log.d("Override URL : ","override URL is $newUrl")


                if(isGoogleMapsUrl(newUrl)) {
                    Log.d("Override URL : ","google map url")
                    // Handle Google Maps URL
                    val mapIntent = Intent(Intent.ACTION_VIEW, Uri.parse(newUrl))
                    mapIntent.setPackage("com.google.android.apps.maps") // Specify the package to ensure it opens in Google Maps
                    startActivity(mapIntent)
                    return true // Return true to prevent WebView from loading the URL
                }



                if(newUrl.contains("mailto:") || newUrl.contains("sms:") || newUrl.contains("tel:")){

                    val mailIntent = Intent(Intent.ACTION_VIEW , Uri.parse(newUrl))
                    startActivity((mailIntent))
                    return true ;
                }



                if(newUrl.contains(".pdf")) {
                    Log.d(TAG, "Override Downloaded file URL is $newUrl")
//                  val downloadIntent = Intent(Intent.ACTION_VIEW , Uri.parse(newUrl))          //Using intent
//                  startActivity((downloadIntent))
                    view?.loadUrl(newUrl)           //load the url and auto call to setDownloadListener  for downloading file
                    return true
                }



                val desiredPattern = "^https://mobile\\.zentrades\\.pro/.*$"                // check URL at the time of logout and then redirect to login page
                val urlToCheck = newUrl

                if (isMatchingUrl(urlToCheck, desiredPattern)){

//                    view?.loadUrl(newUrl)
                    return false  // return false to load the url
                }



                if(newUrl.contains(".png")) {
                    Log.d(TAG, "Override Image  URL is $newUrl")
                    view?.loadUrl(newUrl)
                    return true
                }


                if(!completely_loaded) redirect = true
                completely_loaded = false
                return true
            }

            fun isMatchingUrl(url: String, pattern: String): Boolean {
                val regex = Pattern.compile(pattern)
                val matcher = regex.matcher(url)
                return matcher.matches()
            }

            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                super.onPageStarted(view, url, favicon)

                Log.d("Neel", "page-started $url")

                completely_loaded = false
                d(TAG, "completely_loaded : $completely_loaded")
                Log.d("onPageStarted : ", "Value of url(onPage-started) is $url")

//                progressBar.visibility = View.VISIBLE

            }

            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)


                webView.loadUrl("javascript:(function() { " +
                        "console.log('NEEL PATEL Here'); " +
                        "var data = window.localStorage.getItem('user-id');" +
                        "console.log('user-id is : '+data); })()")


                Log.d("onPageFinished", "ok")


                Log.d("Neel", "page-finished $url")
                accessLocalStorage(webView)




                if(!redirect) completely_loaded = true

                if(completely_loaded && !redirect){

                    d(TAG, "completely_loaded : $completely_loaded")

                }else redirect = false

            }

            override fun onPageCommitVisible(view: WebView?, url: String?) {
                super.onPageCommitVisible(view, url)
                // Your code here
                Log.d("onPageCommitVisible", "$url")
//                progressBar.visibility = View.GONE

            }



        }




        webView.webChromeClient = object : WebChromeClient() {
            // Override methods if needed

            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                super.onProgressChanged(view, newProgress)

                Log.d("onProgressChange", "ok")
                Log.d("onProgressChange in", "$newProgress")

//                progressBar.progress = newProgress
                if (newProgress >= 100) {
                    Log.d("webChromeClient:: onProgressChange in 100 %", "$newProgress")
//                    progressBar.visibility = View.GONE
                } else {
                    Log.d("webChromeClient:: onProgressChange in", "$newProgress")
//                    progressBar.visibility = View.VISIBLE
                }
            }



//            override fun onProgressChanged(view: WebView?, newProgress: Int) {
//                super.onProgressChanged(view, newProgress)
//
//                d(TAG, "onProgressChanged : $newProgress")
//                Log.d("onProgressChanged", "$newProgress")
//
////                if (newProgress < 100) {
////                    // Show overlay layout if data is still loading
////                    overlayLayout.visibility = View.VISIBLE
////                    webView.visibility = View.GONE
////                } else {
////                    // Hide overlay layout when loading is complete
////                    overlayLayout.visibility = View.GONE
////                    webView.visibility = View.VISIBLE
////                }
//            }

            // For Android 5.0+
            override fun onShowFileChooser(
                webView: WebView?,
                filePathCallback: ValueCallback<Array<Uri>>?,
                fileChooserParams: FileChooserParams?
            ): Boolean {
                // Open file chooser or camera here
                val intent = Intent(Intent.ACTION_GET_CONTENT)
                intent.addCategory(Intent.CATEGORY_OPENABLE)
                intent.type = "*/*"

                Log.d("Value of result is", "neel")
                uploadCallback = filePathCallback
                Log.d("Value of upload-callback is",uploadCallback.toString())

                val chooserIntent = Intent.createChooser(intent, "Choose File")
                startActivityForResult(chooserIntent, 101)

                return true
            }


        }


        webView.setDownloadListener(object : DownloadListener {
            override fun onDownloadStart(url: String?, userAgent: String?, contentDisposition: String?, mimeType: String?, contentLength: Long) {
                // Your implementation here
                Log.d("Download File","Downloaded file URL is $url")
//                val downloadIntent = Intent(Intent.ACTION_VIEW , Uri.parse(url))          //Using intent
//                startActivity((downloadIntent))
//                return


                // Create a DownloadManager request
                val request = android.app.DownloadManager.Request(Uri.parse(url))

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
                request.setNotificationVisibility(android.app.DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)

                // Get the DownloadManager service and enqueue the request
                val context: Context = webView.context
                val manager = context.getSystemService(Context.DOWNLOAD_SERVICE) as android.app.DownloadManager
                manager.enqueue(request)

                // Inform the user that the download has started
                Toast.makeText(context, "Downloading $fileName", Toast.LENGTH_SHORT).show()


            }
        })


    }


//    @Deprecated("Deprecated in Java")
//    override fun onBackPressed() {
//        // Check if there's a page to go back to in the WebView
//        if (webView.canGoBack()) {
//            // Go back in WebView history
//            webView.goBack()
//        } else {
////            // If no page to go back, let the system handle the back press
//            val builder = AlertDialog.Builder(this)
//            builder.setTitle("Close Application")
//            builder.setMessage("Do you want to close the application?")
//            builder.setPositiveButton("Yes") { _, _ ->
//                // Perform close actions here
//                super.onBackPressed()
//            }
//            builder.setNegativeButton("No") { _, _ ->
//                // Continue with the application
//            }
//            builder.show()
//
//
//        }
//    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {

        // Check whether the key event is the Back button and if there's history.
        if (keyCode == KeyEvent.KEYCODE_BACK && webView.canGoBack()) {
            webView.goBack()
            return true
        }else {
            // If it isn't the Back button or there isn't web page history, bubble up to
            // the default system behavior. Probably exit the activity.
//           return super.onKeyDown(keyCode, event)

            // If no page to go back, let the system handle the back press
            val builder = AlertDialog.Builder(this)
            builder.setTitle("Close Application")
            builder.setMessage("Do you want to close the application?")
            builder.setPositiveButton("Yes") { _, _ ->
                // Perform close actions here
                super.onBackPressed()
//                 super.onKeyDown(keyCode, event)
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

    override fun onDestroy() {
        super.onDestroy()
//        stopService(service)
    }

}

