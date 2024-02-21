package com.example.zenmobile

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
import android.webkit.GeolocationPermissions
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
import android.location.LocationManager
import android.os.Build
import android.os.Handler
import java.util.regex.Pattern
import android.provider.Settings
import android.view.View
import android.view.Window
import android.view.WindowInsetsAnimation.Callback
import android.view.WindowManager
import android.webkit.ConsoleMessage
import android.webkit.WebSettings
import android.widget.FrameLayout
import android.widget.ProgressBar
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContract
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.tasks.OnCompleteListener
import com.google.firebase.FirebaseApp
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.FirebaseMessagingService
import retrofit2.Call
import retrofit2.HttpException
import retrofit2.Response


class MainActivity : AppCompatActivity() {

    lateinit var userid : String
    lateinit var companyid : String
    lateinit var accesstoken : String
    lateinit var registrationToken : String

    // Declare the launcher at the top of your Activity/Fragment:
    @RequiresApi(Build.VERSION_CODES.O)
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { isGranted: Boolean ->
        if (isGranted) {
            // FCM SDK (and your app) can post notifications.
            d(TAG, "Notification :  Granted")
            getTokenFromFCM()

        } else {
            // TODO: Inform user that that your app will not show notifications.
            d(TAG, "Notification : Not Granted")
            askNotificationPermission()
        }
    }



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

            val firebaseMessagingService = MyFirebaseMessagingService()
            firebaseMessagingService.onNewToken(token)
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
                getTokenFromFCM()

            } else if (shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS)) {
                // TODO: display an educational UI explaining to the user the features that will be enabled
                //       by them granting the POST_NOTIFICATION permission. This UI should provide the user
                //       "OK" and "No thanks" buttons. If the user selects "OK," directly request the permission.
                //       If the user selects "No thanks," allow the user to continue without notifications.
                d(TAG, "askNotification Permission :  shouldShowRequestPermission rational")

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



    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted, reload the WebView
                Log.d(TAG, "onRequestPermissionResult-Granted :  OK")

            } else {
                // Permission denied, show a message or handle it accordingly
            }
        }

        if(requestCode == 100){
            d(TAG, "Notification :  100")
        }
    }


    private fun checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(                      //if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && --- ){}
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                LOCATION_PERMISSION_REQUEST_CODE
            )
        }
    }





    private lateinit var webView : WebView
    private var uploadCallback: ValueCallback<Array<Uri>>? = null
    lateinit var progressBar : ProgressBar


    var redirect = false
    var completely_loaded = true


    companion object {
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1001
    }

//    private lateinit var requestPermissionLauncherNotification : ActivityResultLauncher<Intent>

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == 101) {
            if (resultCode == Activity.RESULT_OK) {
                // Get the selected file URI(s)
                val result = WebChromeClient.FileChooserParams.parseResult(resultCode, data)
//                Log.d("Value of result is", result?.joinToString(", ") ?: "null")
//                Log.d("Value check","hello")
                uploadFile(result)
            } else {
                // Handle canceled file selection
                uploadCallback?.onReceiveValue(null)
                uploadCallback = null
            }
        }
    }



    override fun onResume() {
        super.onResume()
        Log.d(TAG, "onResume :  OK")
//        askNotificationPermission()
//        checkLocationSettings()
    }

    private fun checkLocationSettings() {
        val locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        Log.d(TAG, "checkLocationSetting :  OK")
        if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            Log.d(TAG, "checkLocationSetting - !location-manager:  OK")

            showLocationSettingsDialog()
        }
    }

    private fun showLocationSettingsDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Location Settings")
        builder.setMessage("Location services are disabled. Do you want to enable them?")
        builder.setPositiveButton("Yes") { _, _ ->
            val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
            startActivity(intent)
        }
        builder.setNegativeButton("No") { _, _ ->
            // Handle the case where the user chooses not to enable location services
        }
        builder.show()
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


//    // access local storage values : user-id , company-id , access-token
//    private fun accessLocalStorage(webView: WebView) {
//
////        val firebaseMessagingService = MyFirebaseMessagingService()
//
//        // Access localStorage using JavaScript
//        webView.evaluateJavascript(
//            "(function() { return  JSON.stringify(localStorage); })();"
//        ) { value ->
//            // Handle the value retrieved from localStorage here
//            Log.d("LocalStorage values", "Value from localStorage: $value")
//        }
//
//        webView.evaluateJavascript(
//            "(function() { return localStorage.getItem('user-id');  })();"
//        ) { value ->
//            userid = value
//            Log.d("LocalStorage values", "user-id : $userid")
////            firebaseMessagingService.processLocalStorageValues(userid)
////            firebaseMessagingService.userid = userid
////            val temp = firebaseMessagingService.userid
//            Log.d("LocalStorage values---", "company-id : $userid")
//        }
//
//        webView.evaluateJavascript(
//            "(function() { return localStorage.getItem('access-token');  })();"
//        ) { value ->
//            accesstoken = value
//            Log.d("LocalStorage values", "access-token : $accesstoken")
////            firebaseMessagingService.accesstoken = accesstoken
//        }
//
//        webView.evaluateJavascript(
//            "(function() { return localStorage.getItem('company-id'); })();"
//        ) { value ->
//            companyid = value
//            Log.d("LocalStorage values", "company-id : $companyid")
////            firebaseMessagingService.companyid = companyid
//        }
//
//
//    }

    // access local storage values : user-id , company-id , access-token
    @RequiresApi(Build.VERSION_CODES.O)
    private fun accessLocalStorage(webView: WebView) {
        var uid: String
        var cid: String
        var at: String
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
                    apiRequestToServer()

                }
            }
        }
    }



    @RequiresApi(Build.VERSION_CODES.O)
    private fun apiRequestToServer(){


        Log.d("apiRequestToServer",userid)

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
                if (t is HttpException) {
                    Log.d("MainActivity POST", "HTTP Status Code: $t")
                }
            }


        })

    }


    @RequiresApi(Build.VERSION_CODES.O)
    @SuppressLint("SetJavaScriptEnabled", "MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)


//        this.window.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,WindowManager.LayoutParams.FLAG_FULLSCREEN)    // this line for remove the status bar

        window.statusBarColor = resources.getColor(android.R.color.black, theme)   // change the status bar color


        setContentView(R.layout.activity_main)

//        FirebaseMessaging.getInstance().subscribeToTopic("Notification")
//        val overlayLayout = findViewById<FrameLayout>(R.id.overlayLayout)
        webView= findViewById(R.id.webView)
//        webView.loadUrl("https://www.ilovepdf.com/pdf_to_word")                       //upload the doc
        webView.loadUrl("https://mobile.zentrades.pro/")
//        webView.loadUrl("https://sample-videos.com/download-sample-doc-file.php")     //For Downloading file

        progressBar = findViewById(R.id.progressBar)


        webView.settings.javaScriptEnabled = true
        webView.settings.mixedContentMode = MIXED_CONTENT_COMPATIBILITY_MODE;
        webView.settings.domStorageEnabled = true
        webView.settings.databaseEnabled = true
//        webView.settings.userAgentString = "YourUserAgentString"
//        webView.settings.setSupportMultipleWindows(true) // Enable support for multiple windows
//        webView.settings.cacheMode = WebSettings.LOAD_NO_CACHE



        askNotificationPermission()
        // Check and request location permission if needed
        checkLocationPermission()



        webView.webViewClient = object : WebViewClient(){


            @SuppressLint("QueryPermissionsNeeded")
            override fun shouldOverrideUrlLoading(
                view: WebView?,
                request: WebResourceRequest?
            ): Boolean {

                val newUrl = request?.url.toString()
                Log.d(TAG,"override URL is $newUrl")

//                if(isGoogleMapsUrl(newUrl)) {
//                    Log.d(TAG,"override URL is a google map url")
//                    // Handle Google Maps URL
//                    val mapIntent = Intent(Intent.ACTION_VIEW, Uri.parse(newUrl))
//                    mapIntent.setPackage("com.google.android.apps.maps") // Specify the package to ensure it opens in Google Maps
//                    startActivity(mapIntent)
//                    return true // Return true to prevent WebView from loading the URL
//                }

                if (isGoogleMapsUrl(newUrl)) {
                    Log.d(TAG, "Override URL is a Google Maps URL")

                    // Create the intent to view the Google Maps URL
                    val mapIntent = Intent(Intent.ACTION_VIEW, Uri.parse(newUrl))

                    // Resolve the activity to ensure it's handled by Google Maps
                    val packageManager = packageManager
                    val activities = packageManager.queryIntentActivities(mapIntent, PackageManager.MATCH_DEFAULT_ONLY)

                    Log.d(TAG, "Override URL is a Google Maps URL :  activities $activities")

                    // Check if there's an activity that can handle the intent
                    if (activities.isNotEmpty()) {
                        // Iterate through resolved activities and find the one for Google Maps
                        for (activity in activities) {
                            if (activity.activityInfo.packageName == "com.google.android.apps.maps") {
                                // Found Google Maps activity, set its package name and start the activity
                                mapIntent.setPackage(activity.activityInfo.packageName)
                                startActivity(mapIntent)
                                return true // Return true to prevent WebView from loading the URL
                            }
                        }
                    }

                    Log.d(TAG, "Override URL is a Google Maps URL :  activity empty")
                    // If Google Maps is not found, fallback to opening the URL in any available browser
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

                completely_loaded = false
                d(TAG, "completely_loaded : $completely_loaded")
                Log.d(TAG, "Value of url(onPage-started) is $url")


            }

            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)


                webView.loadUrl("javascript:(function() { " +
                        "console.log('NEEL PATEL Here'); " +
                        "var data = window.localStorage.getItem('user-id');" +
                        "console.log('user-id is : '+data); })()")



//                accessLocalStorage(webView)



                if(!redirect) completely_loaded = true

                if(completely_loaded && !redirect){

                    d(TAG, "completely_loaded : $completely_loaded")

                }else redirect = false

            }



        }




        webView.webChromeClient = object : WebChromeClient() {
            // Override methods if needed

            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                super.onProgressChanged(view, newProgress)

                Log.d("onProgressChange", "ok")

                progressBar.progress = newProgress
                if (newProgress >= 100) {
                    Log.d("onProgressChange in 100 %", "ok")
                    progressBar.visibility = View.GONE
                } else {
                    Log.d("onProgressChange in", "ok")
                    progressBar.visibility = View.VISIBLE
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


//            override fun onGeolocationPermissionsShowPrompt(
//                origin: String?,
//                callback: GeolocationPermissions.Callback?
//            ) {
//                Log.d(TAG,"onGeolocationPermission origin :  $origin")
//                showLocationPermissionDialog(callback)
//            }

//            private fun showLocationPermissionDialog(callback: GeolocationPermissions.Callback?) {
//                val builder = AlertDialog.Builder(this@MainActivity)
//                builder.setTitle("Location Permission")
//                builder.setMessage("This app needs location access to provide relevant content. Do you want to enable it?")
//                builder.setPositiveButton("Yes") { _, _ ->
//                    requestLocationPermission(callback)
//                }
//                builder.setNegativeButton("No") { _, _ ->
//                    callback?.invoke(null, false, false)
//                }
//
//                builder.show()
//            }


//            private fun requestLocationPermission(callback: GeolocationPermissions.Callback?) {
//                Log.d(TAG, "RequestLocationPermission :  OK")
//                ActivityCompat.requestPermissions(
//                    this@MainActivity,
//                    arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
//                    LOCATION_PERMISSION_REQUEST_CODE
//                )
//                Log.d(TAG, "RequestLocationPermission After :  OK")
//            }



        }


        webView.setDownloadListener(object : DownloadListener {
            override fun onDownloadStart(url: String?, userAgent: String?, contentDisposition: String?, mimeType: String?, contentLength: Long) {
                // Your implementation here
                Log.d(TAG,"Downloaded file URL is $url")
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


    // Function to open the notification settings screen
//    @RequiresApi(Build.VERSION_CODES.O)
//    private fun openNotificationSettings(context: Context) {
//        val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
//        intent.putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
//        startActivity(intent)
////        requestPermissionLauncherNotification.launch(intent)
//    }


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


}

