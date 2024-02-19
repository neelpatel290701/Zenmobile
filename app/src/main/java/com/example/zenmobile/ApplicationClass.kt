package com.example.zenmobile

import android.app.Application
import com.google.firebase.FirebaseApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch


class ApplicationClass : Application() {
    override fun onCreate() {
        super.onCreate()


//        FirebaseApp.initializeApp(this)
    }
}

