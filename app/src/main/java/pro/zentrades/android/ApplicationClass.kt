package pro.zentrades.android

import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import com.google.firebase.FirebaseApp


class ApplicationClass : Application() {

    companion object {
        @SuppressLint("StaticFieldLeak")
        lateinit var context: Context
    }

    override fun onCreate() {
        super.onCreate()


        FirebaseApp.initializeApp(this)

        // Initialize DataHolder with the application context
        DataHolder.initialize(applicationContext)

    }



}

