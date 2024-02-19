package com.example.zenmobile

import android.annotation.SuppressLint
import android.app.Service
import android.content.ContentValues.TAG
import android.content.Intent
import android.os.IBinder
import android.util.Log
import android.widget.Toast
import androidx.core.app.NotificationCompat
import com.google.android.gms.tasks.OnCompleteListener
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

@SuppressLint("MissingFirebaseInstanceTokenRefresh")
class MyFirebaseMessagingService : FirebaseMessagingService() {

    lateinit var userid  : String
    lateinit var companyid : String
    lateinit var accesstoken : String

    fun processLocalStorageValues() {
        Log.d("Firebase","user-id : $userid")
    }

    override fun onCreate() {
        super.onCreate()

        Log.d("Firebase", "MyFirebaseMessagingService")

//        FirebaseMessaging.getInstance().token.addOnCompleteListener(OnCompleteListener { task ->
//            if (!task.isSuccessful) {
//                Log.w(TAG, "Fetching FCM registration token failed", task.exception)
//                return@OnCompleteListener
//            }
//
//            // Get new FCM registration token
//            val token = task.result
//            Log.d("Firebase", "Token received :  $token")
//
//            onNewToken(token)
////
//        })

    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        // Handle FCM messages here.
        Log.d("Firebase onMessageReceived", "MyFirebaseMessagingService")
    }


    override fun onNewToken(token: String) {
        Log.d("Firebase", "Refreshed token from onNewToken(): $token")

        // If you want to send messages to this application instance or
        // manage this apps subscriptions on the server side, send the
        // FCM registration token to your app server.
//        sendRegistrationToServer(token)
    }


}
