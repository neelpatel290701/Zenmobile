@file:Suppress("DEPRECATION")

package pro.zentrades.android

import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import android.webkit.ValueCallback
import android.webkit.WebView
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat.startActivityForResult
import com.google.android.gms.tasks.OnCompleteListener
import com.google.firebase.messaging.FirebaseMessaging
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.ByteArrayOutputStream

object Helperfunction {


    private lateinit var activity: AppCompatActivity


    fun chooseFileFromMedia(activity: AppCompatActivity) {
        this.activity = activity
        // Implement your file choosing logic here
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



       fun chooseFileFromMediaWithoutCameraPermission(activity: AppCompatActivity){

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
    private fun apiRequestToServer(webView: WebView) {


        Log.d("apiRequestToServer","OKK" )


        webView.evaluateJavascript(
            "(function() { return localStorage.getItem('access-token'); })();"
        ) { accessTokenValue ->
            DataHolder.accessToken = accessTokenValue.substring(1, accessTokenValue.length - 1)
            Log.d("neel", "access-token : ${DataHolder.accessToken}")

            try{

            val userData = dataModelItem(DataHolder.registrationToken!!, "FCM")
            RetrofitInstance.apiInterface.sendToken(
                DataHolder.userId,
                DataHolder.companyId,
                DataHolder.accessToken!!,
                userData
            )
                .enqueue(object : Callback<dataModelItem?> {
                    override fun onResponse(
                        call: Call<dataModelItem?>, response: Response<dataModelItem?>
                    ) {
                        try {
                            if (response.isSuccessful) {
                                val responseData = response.body()
                                // Process responseData according to your application's logic
                                Log.d("apiRequestToServer", "Success! Response Data: $responseData")
                                Log.d("apiRequestToServer", "Success! Response Code: ${response}")
                            } else {
                                // Handle unsuccessful response (e.g., non-200 status code)
                                Log.e(
                                    "apiRequestToServer",
                                    "Unsuccessful response: ${response.code()}"
                                )
                            }


                        } catch (e: Exception) {
                            Log.e("apiRequestToServer", "Error: ${e.message}", e)
                        }

                    }

                    override fun onFailure(call: Call<dataModelItem?>, t: Throwable) {

                        Log.d("apiRequestToServer", "onFailure")
                        //                if (t is HttpException) {
                        Log.d("apiRequestToServer", "HTTP Status Code: $t")
                        //                }
                    }


                })

        }catch (e : Exception){

                Log.d("neel" , "${e.message}")
        }


        }

    }


    @RequiresApi(Build.VERSION_CODES.O)
     fun getTokenFromFCM(webView : WebView) {

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
            Log.d("Firebase Notification", "Registration-token received :  ${DataHolder.registrationToken}")
            Log.d("Neel", "get token")

//            val firebaseMessagingService = MyFirebaseMessagingService()
//            firebaseMessagingService.onNewToken(token)

            apiRequestToServer(webView)
//
//            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
//                checkLocationPermission()
//            }

        })

    }





}