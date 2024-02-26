package pro.zentrades.android

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.location.Location
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.getSystemService
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationAvailability
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority


@Suppress("DEPRECATION")
class LocationService : Service() {

    companion object{
        const val CHANNEL_ID = "1234"
        const val NOTIFICATION_ID = 1234
    }

    private var fusedLocationProviderClient: FusedLocationProviderClient? = null
    private var locationCallback: LocationCallback? = null
    private var locationRequest: LocationRequest? = null
    private var location: Location? = null


    private var notificationManager : NotificationManager?=null

    override fun onCreate() {
        super.onCreate()

        Log.d("neel","Location onCreate()")
//
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)
        locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 1000).setIntervalMillis(500).build()

        locationCallback= object : LocationCallback(){

            override fun onLocationResult(locationResult: LocationResult) { 
                super.onLocationResult(locationResult)
                location = locationResult.lastLocation
                val longitude = location?.longitude
                val latitude = location?.latitude

                Log.d("neel LocationService", "longitude : $longitude")
                Log.d("neel LocationService", "latitude : $latitude")

                onNewLocation(locationResult)
            }

        }

        notificationManager = this.getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
            val notificationChannel = NotificationChannel(CHANNEL_ID , "Location",NotificationManager.IMPORTANCE_HIGH)
            notificationManager?.createNotificationChannel(notificationChannel)
        }

    }

    @SuppressLint("ForegroundServiceType")
    private fun onNewLocation(locationResult: LocationResult) {

                location = locationResult.lastLocation
                startForeground(NOTIFICATION_ID,getNotification())

    }

    private fun getNotification():Notification {

        val notification = NotificationCompat.Builder ( this , CHANNEL_ID)
            .setContentTitle("Location Update ")
            .setContentText("Latitude-->${location?.latitude}\nLongitude-->${location?.longitude}")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setOngoing(true)
            .setChannelId(CHANNEL_ID)

            return notification.build()
    }




    @Suppress("MissingPermission")
    private  fun createLocationrequset(){
        try {
            fusedLocationProviderClient?.requestLocationUpdates(locationRequest!!,locationCallback!!,null)
        }catch (e:Exception){
            e.printStackTrace()
            Log.d("neel","createlocationrequest exception")
        }


    }

    private fun removeLocationUpdates(){

            locationCallback?.let {
                fusedLocationProviderClient?.removeLocationUpdates(it)
            }

        stopForeground(true)
        stopSelf()

    }



    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        Log.d("neel LocationService", "onStartCommand")
        createLocationrequset()
        return START_STICKY
    }

    override fun onBind(intent: Intent): IBinder ?= null

    override fun onDestroy() {
        super.onDestroy()
        removeLocationUpdates()
    }

}
