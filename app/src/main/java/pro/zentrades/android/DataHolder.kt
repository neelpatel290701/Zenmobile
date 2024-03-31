package pro.zentrades.android

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.webkit.ValueCallback

object DataHolder {

    var userId: String = ""
    var companyId: String = ""
    var registrationToken: String? = null
    var androidId: String = ""
    var locationService: Intent? = null

    var uploadCallback: ValueCallback<Array<Uri>>? = null  // Using for onShowFileChooser-CallBack

    private const val ACCESS_TOKEN_KEY = "access_token"

    /*
     Initialize SharedPreferences  ---- here used sharepreference for access-token so that store
     the value of it and second time app opening can be get it.
    */
    private val sharedPreferences: SharedPreferences by lazy {
        // Use applicationContext to prevent memory leaks
        ApplicationClass.context.getSharedPreferences("my_app_prefs", Context.MODE_PRIVATE)
    }

    fun initialize(context: Context) {
        ApplicationClass.context = context.applicationContext
    }

    // Getter and setter for accessToken
    var accessToken: String?
        get() = sharedPreferences.getString(ACCESS_TOKEN_KEY, null)
        set(value) {
            // Save the token to SharedPreferences
            sharedPreferences.edit().putString(ACCESS_TOKEN_KEY, value).apply()
        }

}