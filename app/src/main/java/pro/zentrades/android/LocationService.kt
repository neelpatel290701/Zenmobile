package pro.zentrades.android

import android.Manifest
import android.annotation.SuppressLint
import android.app.ForegroundServiceStartNotAllowedException
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.location.Location
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority

import kotlin.math.*


@Suppress("DEPRECATION")
class LocationService : Service() {

    companion object {
        const val CHANNEL_ID = "1234"
        const val NOTIFICATION_ID = 1234
    }

    private var fusedLocationProviderClient: FusedLocationProviderClient? = null
    private var locationCallback: LocationCallback? = null
    private var locationRequest: LocationRequest? = null
    private var location: Location? = null

    val LocationSendToDatabase = DatabaseClass()  // create instance of Firebase database

    var userID: String? = null
    var companyID: String? = null
    var deviceId: String? = null

    private var latitude1: Double? = null
    private var longitude1: Double? = null
    private var latitude2: Double? = null
    private var longitude2: Double? = null
    private var Total_distance: Double = 0.0

    private var notificationManager: NotificationManager? = null


    fun distance(
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


    @SuppressLint("ForegroundServiceType")
    override fun onCreate() {
        super.onCreate()


        Log.d("neel", "Location onCreate() ")
//
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)
        locationRequest =
            LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 1000).setIntervalMillis(500)
                .build()

        locationCallback = object : LocationCallback() {

            override fun onLocationResult(locationResult: LocationResult) {
                super.onLocationResult(locationResult)
                location = locationResult.lastLocation

                val longitude_curr = location?.longitude
                val latitude_curr = location?.latitude

                Log.d("neel LocationService", "latitude : $latitude_curr")
                Log.d("neel LocationService", "longitude : $longitude_curr")

//                Location.sendLocation(longitude_curr.toString())

                if (latitude1 == null && longitude1 == null) {

                    latitude1 = latitude_curr
                    longitude1 = longitude_curr

                } else {

                    latitude2 = latitude_curr
                    longitude2 = longitude_curr

                    val distance = distance(latitude1!!, longitude1!!, latitude2!!, longitude2!!)
                    val distance_meter = distance * 1000

                    Log.d("neel", "userId : $userID and companyId : $companyID")
                    Log.d("neel", "Distance-meter diff  is : $distance_meter \n\n")

                    if (distance_meter > 3) {

                        Total_distance += (distance_meter)

                        LocationSendToDatabase.sendLocation(
                            "$Total_distance", "$userID", "$companyID",
                            "$deviceId", "$latitude2", "$longitude2"
                        )

                        latitude1 = latitude2
                        longitude1 = longitude2


                        Log.d("neel", "Distance is : $Total_distance")

                    }

                }

//
            }

        }


        notificationManager = this.getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationChannel =
                NotificationChannel(CHANNEL_ID, "Location", NotificationManager.IMPORTANCE_HIGH)
            notificationManager?.createNotificationChannel(notificationChannel)
        }

//        startForeground(NOTIFICATION_ID,getNotification())

        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_BACKGROUND_LOCATION
            ) == PackageManager.PERMISSION_DENIED
        ) {
            // Without background location permissions the service cannot run in the foreground
            // Consider informing user or updating your app UI if visible.
            Log.d("neel", "Location Service : No backgroundLocation Permission")
            stopSelf()
            return

        } else {

            try {

                Log.d("neel", "Location Service : startNotification")
                ServiceCompat.startForeground(
                    this, NOTIFICATION_ID, getNotification(),
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION
                    } else {
                        0
                    }
                )
            } catch (e: Exception) {

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
                    && e is ForegroundServiceStartNotAllowedException
                ) {
                    // App not in a valid state to start foreground service
                    // (e.g. started from bg)
                }
            }


        }

    }


    @SuppressLint("SuspiciousIndentation")
    private fun getNotification(): Notification {

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Location Update ")
//            .setContentText("Latitude-->${location?.latitude}\nLongitude-->${location?.longitude}")
            .setContentText("we are currently tracking your location...")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setOngoing(true)
            .setChannelId(CHANNEL_ID)

        return notification.build()
    }


    @Suppress("MissingPermission")
    private fun createLocationrequset() {
        try {
            fusedLocationProviderClient?.requestLocationUpdates(
                locationRequest!!,
                locationCallback!!,
                null
            )
        } catch (e: Exception) {
            e.printStackTrace()
            Log.d("neel", "createlocationrequest exception")
        }


    }

    private fun removeLocationUpdates() {

        locationCallback?.let {
            fusedLocationProviderClient?.removeLocationUpdates(it)
        }

        stopForeground(true)
        stopSelf()

    }


    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        Log.d("neel LocationService", "onStartCommand")
        userID = intent?.getStringExtra("userId")
        companyID = intent?.getStringExtra("companyId")
        deviceId = intent?.getStringExtra("androidId")
        createLocationrequset()
        return START_STICKY
    }

    override fun onBind(intent: Intent): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        removeLocationUpdates()
    }

}
