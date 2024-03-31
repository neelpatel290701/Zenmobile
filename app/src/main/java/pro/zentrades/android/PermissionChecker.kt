import android.Manifest
import android.annotation.SuppressLint
import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.IntentSender
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.LocationSettingsRequest
import com.google.android.gms.location.LocationSettingsResponse
import com.google.android.gms.location.SettingsClient
import com.google.android.gms.tasks.Task
import pro.zentrades.android.DataHolder
import pro.zentrades.android.LocationService
import pro.zentrades.android.MainActivity
import pro.zentrades.android.PermissionCallback


@Suppress("DEPRECATION")
class PermissionChecker(
    private val activity: AppCompatActivity, private val permissionCallback: PermissionCallback
) {

    private val cameraPermission =
        activity.registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                permissionCallback.onCameraPermissionGranted()
            } else {
                permissionCallback.onCameraPermissionDenied()
            }
        }

    fun checkCameraPermission() {
        if (ContextCompat.checkSelfPermission(
                activity, Manifest.permission.CAMERA
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            cameraPermission.launch(Manifest.permission.CAMERA)
        } else {
            permissionCallback.onCameraPermissionGranted()
        }
    }

    // Check the Notification Result after creating launcher
    @RequiresApi(Build.VERSION_CODES.O)
    private val notificationPermissionLauncher = activity.registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { isGranted: Boolean ->
        if (isGranted) {
            permissionCallback.onNotificationPermissionGranted()
        } else {
            Log.d("neel", "Notification : Not Granted")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                checkLocationPermission()
            }
        }
    }

    fun askNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    activity, Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                Log.d("neel", "notification permission ok")
            } else if (activity.shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS)) {
                Log.d("neel", "askNotification Permission :  shouldShowRequestPermission rational")
                val builder = AlertDialog.Builder(activity)
                builder.setTitle("Enable Notifications")
                    .setMessage("Please enable notifications for this app to receive important updates.")
                    .setPositiveButton("Enable") { _, _ ->
                        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)

                    }.setNegativeButton("Cancel") { dialog, _ ->
                        dialog.dismiss()
                    }.show()

            } else {
                // Directly ask for the permission
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                Log.d("neel", "askNotification Permission :  launch requsetpermissionlauncher")
            }
        }
    }

    fun askNotificationPermissionOnNewAccessToken() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    activity, Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                permissionCallback.onNotificationPermissionGranted()
                Log.d(
                    "neel", "askNotificationPermissionOnNewAccessToken :  Permission Already Okkk"
                )
            } else if (activity.shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS)) {
                Log.d(
                    "neel",
                    "askNotificationPermissionOnNewAccessToken :  shouldShowRequestPermission rational"
                )
                val builder = AlertDialog.Builder(activity)
                builder.setTitle("Enable Notifications")
                    .setMessage("Please enable notifications for this app to receive important updates.")
                    .setPositiveButton("Enable") { _, _ ->
                        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)

                    }.setNegativeButton("Cancel") { dialog, _ ->
                        dialog.dismiss()
                    }.show()
            } else {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                Log.d(
                    "neel",
                    "askNotificationPermissionOnNewAccessToken :  launch requsetpermissionlauncher"
                )
            }
        }
    }

    private fun requestDeviceLocationSettings() {
        val locationRequest = LocationRequest.create().apply {
            interval = 10000
            fastestInterval = 5000
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        }
        val builder = LocationSettingsRequest.Builder().addLocationRequest(locationRequest)

        val client: SettingsClient = LocationServices.getSettingsClient(activity)
        val task: Task<LocationSettingsResponse> = client.checkLocationSettings(builder.build())

        Log.d("neel", "requestDeviceLocationSettings")

        task.addOnSuccessListener { locationSettingsResponse ->
            // All location settings are satisfied
            Log.d("neel", "GPS Enable")

            val state = locationSettingsResponse.locationSettingsStates
            val label =
                "GPS >> (Present: ${state?.isGpsPresent}  | Usable: ${state?.isGpsUsable} ) \n" + "Network >> ( Present: ${state?.isNetworkLocationPresent} | Usable: ${state?.isNetworkLocationUsable} ) \n" + "Location >> ( Present: ${state?.isLocationPresent} | Usable: ${state?.isLocationUsable} )"

            Toast.makeText(activity, "LOCATION IS ACTIVE", Toast.LENGTH_LONG).show()
        }

        task.addOnFailureListener { exception ->
            if (exception is ResolvableApiException) {
                Log.d("neel", "GPS Not Enable")
                try {
                    // Show the dialog by calling startResolutionForResult(),
                    // and check the result in onActivityResult().
                    exception.startResolutionForResult(
                        activity, MainActivity.LOCATION_GPS_ENABLE_CODE
                    )
                } catch (sendEx: IntentSender.SendIntentException) {
                    // Ignore the error.
                }
            }
        }
    }

    //background permission result
    private val backgroundLocation =
        activity.registerForActivityResult(ActivityResultContracts.RequestPermission()) {
            if (it) {
                Log.d("neel", "backgroundlocation")
                // after user give permission for location check for GPS is active or not
                requestDeviceLocationSettings()
                //Start the Foreground Service
                DataHolder.locationService = Intent(activity, LocationService::class.java).apply {
                    putExtra("userId", DataHolder.userId)
                    putExtra("companyId", DataHolder.companyId)
                    putExtra("androidId", DataHolder.androidId)
                }
                DataHolder.locationService?.let {
                    ContextCompat.startForegroundService(
                        activity, DataHolder.locationService!!
                    )
                }
            } else {
                requestDeviceLocationSettings()
            }
        }

    @RequiresApi(Build.VERSION_CODES.Q)
    fun askForPreciseLocation() {
        if (ContextCompat.checkSelfPermission(
                activity, Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            locationPermissionRequest.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION))
        }
    }

    //location permission result
    @RequiresApi(Build.VERSION_CODES.Q)
    val locationPermissionRequest = activity.registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        when {
            permissions.getOrDefault(Manifest.permission.ACCESS_FINE_LOCATION, false) -> {
                // Precise location access granted.
                Log.d("location-permission", "Precise Location : Granted")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    if (ContextCompat.checkSelfPermission(
                            activity, Manifest.permission.ACCESS_BACKGROUND_LOCATION
                        ) != PackageManager.PERMISSION_GRANTED
                    ) {
                        backgroundLocation.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
                    }
                }
            }

            permissions.getOrDefault(Manifest.permission.ACCESS_COARSE_LOCATION, false) -> {
                Log.d("nee location-permission", "Approximate Location : Granted")
                askForPreciseLocation()   /// ask for Precise Location to user
            }

            else -> {
                Log.d("neel ", "location-permission  :  Not Granted")
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    fun locationPermissionAlreadyGranted() {

        Log.d("neel ", "CheckLocationPermission : AlreadyGranted")
        //after taking permission check the GPS is unable or not
        requestDeviceLocationSettings()

        if (ContextCompat.checkSelfPermission(
                activity, Manifest.permission.ACCESS_BACKGROUND_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            // if already background permission granted then start the service
            DataHolder.locationService = Intent(activity, LocationService::class.java).apply {
                putExtra("userId", DataHolder.userId)
                putExtra("companyId", DataHolder.companyId)
                putExtra("androidId", DataHolder.androidId)
            }
            Log.d("neel ", "CheckLocationPermission : Ok  - start service ")
            DataHolder.locationService?.let {
                ContextCompat.startForegroundService(
                    activity, DataHolder.locationService!!
                )
            }

        }

        if (ContextCompat.checkSelfPermission(
                activity, Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            locationPermissionRequest.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION))
        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    fun checkLocationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (ContextCompat.checkSelfPermission(
                    activity, Manifest.permission.ACCESS_FINE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED || ContextCompat.checkSelfPermission(
                    activity, Manifest.permission.ACCESS_COARSE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
            ) {

                locationPermissionAlreadyGranted()

            } else if (activity.shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION)
                && activity.shouldShowRequestPermissionRationale(
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            ) {
                Log.d("neel", "CheckLocationPermission  : shouldShowRequestPermissionRationale")
                val builder: AlertDialog.Builder = AlertDialog.Builder(activity)
                builder.setTitle("Location Settings")
                builder.setMessage("Location services are disabled. Do you want to enable them?")
                builder.setPositiveButton("Yes") { _, _ ->

                    locationPermissionRequest.launch(
                        arrayOf(
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                        )
                    )
                }
                builder.setNegativeButton("No") { dialog, _ ->
                    dialog.dismiss()
                }
                builder.show()
            } else {
                Log.d("neel", "CheckLocationPermission  : create launcher")
                locationPermissionRequest.launch(
                    arrayOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    )
                )
            }
        }
    }

    @SuppressLint("ServiceCast")
    fun isForegroundServiceRunning(context: Context, serviceClass: Class<*>): Boolean {
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val runningServices = activityManager.getRunningServices(Int.MAX_VALUE)
        for (service in runningServices) {
            if (serviceClass.name == service.service.className && service.foreground) {
                return true
            }
        }
        return false
    }

    fun isIgnoringBatteryOptimizations(): Boolean {
        val powerManager =
            activity.getSystemService(AppCompatActivity.POWER_SERVICE) as PowerManager
        val packageName = activity.applicationContext.packageName
        return powerManager.isIgnoringBatteryOptimizations(packageName)
    }

    @SuppressLint("BatteryLife")
    fun requestBatteryOptimizations() {
        Log.d("hello", "ask for requestBatteryOptimization")
        val packageName = activity.applicationContext.packageName
        val intent = Intent()
        intent.setAction(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)
        intent.setData(Uri.parse("package:$packageName"))
        activity.startActivityForResult(intent, MainActivity.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)

    }
}
