package com.example.zenmobile

import android.app.Application
import com.google.firebase.FirebaseApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

import com.onesignal.OneSignal
import com.onesignal.debug.LogLevel

// NOTE: Replace the below with your own ONESIGNAL_APP_ID
const val ONESIGNAL_APP_ID = "e5bb540d-e0de-484c-bc3a-461edad2bc1d"

class ApplicationClass : Application() {
    override fun onCreate() {
        super.onCreate()

        // Verbose Logging set to help debug issues, remove before releasing your app.
//        OneSignal.Debug.logLevel = LogLevel.VERBOSE

        // OneSignal Initialization
//        OneSignal.initWithContext(this, ONESIGNAL_APP_ID)

        // requestPermission will show the native Android notification permission prompt.
        // NOTE: It's recommended to use a OneSignal In-App Message to prompt instead.

//        CoroutineScope(Dispatchers.IO).launch {
//            OneSignal.Notifications.requestPermission(true)
//        }

        FirebaseApp.initializeApp(this)
    }
}

