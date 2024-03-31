@file:Suppress("DEPRECATION")

package pro.zentrades.android

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.provider.Settings
import android.util.Log
import android.webkit.WebView
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.tasks.OnCompleteListener
import com.google.firebase.messaging.FirebaseMessaging
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.regex.Pattern
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

object Helperfunction {

    private lateinit var activity: AppCompatActivity

    fun chooseFileFromMedia(activity: AppCompatActivity) {
        this.activity = activity
        // Open file chooser, camera for image, and camera for video
        val fileIntent = Intent(Intent.ACTION_GET_CONTENT)
        fileIntent.addCategory(Intent.CATEGORY_OPENABLE)
        fileIntent.type = "*/*"
        fileIntent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)

        val cameraImageIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        val cameraVideoIntent = Intent(MediaStore.ACTION_VIDEO_CAPTURE)

        val chooserIntent = Intent.createChooser(fileIntent, "Choose File")
        // Add both camera intents to the chooser
        chooserIntent.putExtra(
            Intent.EXTRA_INITIAL_INTENTS, arrayOf(cameraImageIntent, cameraVideoIntent)
        )
        activity.startActivityForResult(
            chooserIntent,
            MainActivity.FILE_CHOOSER_REQUEST_CODE
        )
    }

    fun chooseFileFromMediaWithoutCameraPermission(activity: AppCompatActivity) {

        this.activity = activity

        // Open file chooser or gallery Here
        val fileIntent = Intent(Intent.ACTION_GET_CONTENT)
        fileIntent.addCategory(Intent.CATEGORY_OPENABLE)
        fileIntent.type = "*/*"
        fileIntent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)

        val galleryIntent = Intent(Intent.ACTION_OPEN_DOCUMENT)
        galleryIntent.addCategory(Intent.CATEGORY_OPENABLE)
        galleryIntent.type = "image/*"
        galleryIntent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)

        val chooserIntent = Intent.createChooser(fileIntent, "Choose File")
        // Add both gallery Intent and File Intent
        chooserIntent.putExtra(
            Intent.EXTRA_INITIAL_INTENTS, arrayOf(galleryIntent)
        )
        activity.startActivityForResult(chooserIntent, MainActivity.FILE_CHOOSER_REQUEST_CODE)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun apiRequestToServerForPushNotification(webView: WebView) {
        Log.d("apiRequestToServer", "OKK")

        webView.evaluateJavascript(
            "(function() { return localStorage.getItem('access-token'); })();"
        ) { accessTokenValue ->
            DataHolder.accessToken = accessTokenValue.substring(1, accessTokenValue.length - 1)
            Log.d("neel", "access-token : ${DataHolder.accessToken}")

            try {
                val userData =
                    DataModelItemForPushNotification(DataHolder.registrationToken!!, "FCM")
                RetrofitInstance.apiInterface.sendToken(
                    DataHolder.userId,
                    DataHolder.companyId,
                    DataHolder.accessToken!!,
                    userData
                )
                    .enqueue(object : Callback<DataModelItemForPushNotification?> {

                        override fun onResponse(
                            call: Call<DataModelItemForPushNotification?>,
                            response: Response<DataModelItemForPushNotification?>
                        ) {
                            try {
                                if (response.isSuccessful) {
                                    val responseData = response.body()
                                    Log.d(
                                        "apiRequestToServer",
                                        "Success! Response Data: $responseData"
                                    )
                                } else {
                                    // Handle unsuccessful response (e.g., non-200 status code)
                                    Log.d(
                                        "apiRequestToServer",
                                        "Unsuccessful response: ${response.code()}"
                                    )
                                }
                            } catch (e: Exception) {
                                Log.e("apiRequestToServer", "Error: ${e.message}", e)
                            }
                        }

                        override fun onFailure(
                            call: Call<DataModelItemForPushNotification?>,
                            t: Throwable
                        ) {
                            Log.d("apiRequestToServer", "onFailure")
                            Log.d("apiRequestToServer", "HTTP Status Code: $t")
                        }
                    })
            } catch (e: Exception) {
                Log.d("neel", "${e.message}")
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun getTokenFromFCM(webView: WebView) {
        FirebaseMessaging.getInstance().token.addOnCompleteListener(OnCompleteListener { task ->
            if (!task.isSuccessful) {
                Log.d(
                    "Firebase Notification",
                    "Fetching FCM registration token failed",
                    task.exception
                )
                return@OnCompleteListener
            }
            // Get new FCM registration token
            val token = task.result
            DataHolder.registrationToken = token
            Log.d(
                "Firebase Notification",
                "Registration-token received :  ${DataHolder.registrationToken}"
            )
            Log.d("Neel", "get token")

            apiRequestToServerForPushNotification(webView)
        })
    }

    fun apiResponseFromServerForCacheClear() {
        val requestCall = RetrofitInstance.apiInterface2.getIsCacheCleared()
        requestCall.enqueue(object : Callback<List<ResponseDataModelItemForCacheClear>> {
            override fun onResponse(
                call: Call<List<ResponseDataModelItemForCacheClear>>,
                response: Response<List<ResponseDataModelItemForCacheClear>>
            ) {
                try {
                    if (response.isSuccessful) {
                        val responseData = response.body()
                        Log.d("apiResponseFromServer", "Success! Response Data: $responseData")
                    } else {
                        Log.e("apiResponseFromServer", "Unsuccessful response: ${response.code()}")
                    }
                } catch (e: Exception) {
                    Log.e("apiResponseFromServer", "Error: ${e.message}", e)
                }
            }

            override fun onFailure(
                call: Call<List<ResponseDataModelItemForCacheClear>>,
                t: Throwable
            ) {
                Log.d("apiResponseFromServer", "onFailure")
                Log.d("apiResponseFromServer", "HTTP Status Code: $t")
            }
        })
    }

    fun isGoogleMapsUrl(url: String): Boolean {
        return url.startsWith("https://maps.google.com/maps") && url.contains("daddr=")
    }

    fun isMatchingUrl(url: String, pattern: String): Boolean {
        val regex = Pattern.compile(pattern)
        val matcher = regex.matcher(url)
        return matcher.matches()
    }

    @SuppressLint("HardwareIds")
    fun getAndroidId(context: Context): String {
        return Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
    }

    fun bitmapToUri(bitmap: Bitmap): Uri {
        val bytes = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, bytes)
        val path = MediaStore.Images.Media.insertImage(
            activity.applicationContext.contentResolver, bitmap, "Image", null
        )
        return Uri.parse(path)
    }

    fun handleFileChooserResultCode(resultCode: Int, data: Intent?) {
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
        DataHolder.uploadCallback?.onReceiveValue(null)
        DataHolder.uploadCallback = null
    }

    fun handleFileURIs(data: Intent?) {

        if (data != null && data.data != null) {
            // File picker selected - Single File Selected - Camera video
            val uri = data.data!!
            DataHolder.uploadCallback?.onReceiveValue(arrayOf(uri))
            DataHolder.uploadCallback = null
        } else if (data?.clipData != null) {
            // File picker selected - multiple files
            val uris = mutableListOf<Uri>()
            for (i in 0 until data.clipData!!.itemCount) {
                val uri = data.clipData!!.getItemAt(i).uri
                uris.add(uri)
            }
            if (uris.isNotEmpty()) {
                DataHolder.uploadCallback?.onReceiveValue(uris.toTypedArray())
                DataHolder.uploadCallback = null
            }
        } else if (data?.extras?.containsKey("data") == true) {
            // Camera selected
            val imageBitmap = data.extras?.get("data") as Bitmap?
            if (imageBitmap != null) {
                val uri = bitmapToUri(imageBitmap)
                // Pass the Uri to the uploadCallback
                DataHolder.uploadCallback?.onReceiveValue(arrayOf(uri))
                DataHolder.uploadCallback = null
            }
        } else {
            // Neither file picker nor camera selected
        }
    }

    fun clearApplicationData(cacheDir: File?) {
        val cache = cacheDir               // /data/user/0/pro.zentrades.android/cache
        val appDir = File(cache?.parent)   // /data/user/0/pro.zentrades.android
        if (appDir.exists()) {
            val children = appDir.list()
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

    fun distanceBetweenTwoLocationPoint(
        lat1: Double,
        lon1: Double,
        lat2: Double,
        lon2: Double
    ): Double {
        val R = 6371 // Radius of the earth in km
        val dLat = deg2rad(lat2 - lat1)  // deg2rad below
        val dLon = deg2rad(lon2 - lon1)
        val a =
            sin(dLat / 2) * sin(dLat / 2) +
                    cos(deg2rad(lat1)) * cos(deg2rad(lat2)) *
                    sin(dLon / 2) * sin(dLon / 2)

        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        val d = R * c // Distance in km
        return d
    }

    fun deg2rad(deg: Double): Double {
        return deg * (Math.PI / 180)
    }

}