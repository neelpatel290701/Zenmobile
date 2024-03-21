package pro.zentrades.android

import android.annotation.SuppressLint
import android.content.ContentValues.TAG
import android.util.Log
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

@SuppressLint("MissingFirebaseInstanceTokenRefresh")
class MyFirebaseMessagingService : FirebaseMessagingService() {

    //it is called when firebase service is create
    override fun onCreate() {
        super.onCreate()
        Log.d("Firebase onCreate()", "MyFirebaseMessagingService")
    }
    // onMessageReceived when the application state is in Foreground and getting the notification
    //or in background state that times called only when data is pass
    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        // Handle FCM messages here.
        Log.d("Firebase onMessageReceived", "MyFirebaseMessagingService....")
        var data = remoteMessage.notification?.body
//        var data = remoteMessage.data
        Log.d("Firebase onMessageReceived", "Message is : $data")
        if (remoteMessage.data.isNotEmpty()) {
            Log.d(TAG, "Message data payload: ${remoteMessage.data}")
        }
    }
    override fun onNewToken(token: String) {
        Log.d("Firebase", "Refreshed token from onNewToken(): $token")
        // If you want to send messages to this application instance or
        // manage this apps subscriptions on the server side, send the
        // FCM registration token to your app server.
    }


}
