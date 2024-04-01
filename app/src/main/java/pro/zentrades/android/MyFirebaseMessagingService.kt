package pro.zentrades.android

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.ContentValues.TAG
import android.content.Context
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
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
        remoteMessage.notification?.let {
            val notificationTitle = it.title ?: "Notification Title"
            val notificationBody = it.body ?: "Notification Body"

            // Create and show notification
            createNotificationChannel()
            showNotification(notificationTitle, notificationBody)
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channelId = "FCM_Foreground_Notification"
            val channelName = "FCM_Foreground_Notification"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(channelId, channelName, importance)

            val notificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun showNotification(title: String, body: String) {
        val notificationBuilder =
            NotificationCompat.Builder(this, "FCM_Foreground_Notification").setContentTitle(title)
                .setContentText(body).setSmallIcon(R.mipmap.ic_launcher)
                .setPriority(NotificationCompat.PRIORITY_HIGH)

        val notificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(0, notificationBuilder.build())
    }

    override fun onNewToken(token: String) {
        Log.d("Firebase", "Refreshed token from onNewToken(): $token")
        // If you want to send messages to this application instance or
        // manage this apps subscriptions on the server side, send the
        // FCM registration token to your app server.
    }


}
