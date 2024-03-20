import android.Manifest
import android.content.Intent
import android.content.IntentSender
import android.content.pm.PackageManager
import android.os.Build
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
    private val activity: AppCompatActivity ,
    private val permissionCallback: PermissionCallback
) {

//    private var service: Intent? = null


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
                activity,
                Manifest.permission.CAMERA
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // Request camera permission
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
            // isGranted-True : called the getTokenFromFCM
//            Log.d("Neel", "notification-granted")
//            getTokenFromFCM()
             permissionCallback.onNotificationPermissionGranted()


        } else {

            // TODO: Inform user that that your app will not show notifications.
            Log.d("neel", "Notification : Not Granted")

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                checkLocationPermission()
            }
        }
    }


    fun askNotificationPermission() {
        // This is only necessary for API level >= 33 (TIRAMISU)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {

            if (ContextCompat.checkSelfPermission(
                    activity,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
            ) {

                Log.d("neel", "notification permission ok")
//
//                if (DataHolder.registrationToken == null) {    // if permission granted but token is not initialized
//                permissionCallback.onNotificationPermissionGranted()
//                    Log.d("neel", "token not initialized - called getTokenFromFCM() : ${DataHolder.registrationToken}")
//                }

            } else if (activity.shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS)) {
                // TODO: display an educational UI explaining to the user the features that will be enabled
                //       by them granting the POST_NOTIFICATION permission. This UI should provide the user
                //       "OK" and "No thanks" buttons. If the user selects "OK," directly request the permission.
                //       If the user selects "No thanks," allow the user to continue without notifications.
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

    fun askNotificationPermissionOnNewAccessToken(){

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {

            if (ContextCompat.checkSelfPermission(
                    activity,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
            ){

                permissionCallback.onNotificationPermissionGranted()
                Log.d("neel", "askNotificationPermissionOnNewAccessToken :  Permission Already Okkk")

            }else if (activity.shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS)) {
                // TODO: display an educational UI explaining to the user the features that will be enabled
                //       by them granting the POST_NOTIFICATION permission. This UI should provide the user
                //       "OK" and "No thanks" buttons. If the user selects "OK," directly request the permission.
                //       If the user selects "No thanks," allow the user to continue without notifications.
                Log.d("neel", "askNotificationPermissionOnNewAccessToken :  shouldShowRequestPermission rational")

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
                Log.d("neel", "askNotificationPermissionOnNewAccessToken :  launch requsetpermissionlauncher")
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
            // All location settings are satisfied. The client can initialize
            // location requests here.
            Log.d("neel", "GPS Enable")

            val state = locationSettingsResponse.locationSettingsStates

            val label =
                "GPS >> (Present: ${state?.isGpsPresent}  | Usable: ${state?.isGpsUsable} ) \n" +
                        "Network >> ( Present: ${state?.isNetworkLocationPresent} | Usable: ${state?.isNetworkLocationUsable} ) \n" +
                        "Location >> ( Present: ${state?.isLocationPresent} | Usable: ${state?.isLocationUsable} )"

//            Log.d("neel" , label)

            Toast.makeText(activity, "LOCATION IS ACTIVE", Toast.LENGTH_LONG).show()
        }

        task.addOnFailureListener { exception ->
            if (exception is ResolvableApiException) {
                // Location settings are not satisfied, but this can be fixed
                // by showing the user a dialog.
                Log.d("neel", "GPS Not Enable at time of opening the application")
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

                DataHolder.service = Intent(activity, LocationService::class.java).apply {
                    putExtra("userId", DataHolder.userId)
                    putExtra("companyId", DataHolder.companyId)
                    putExtra("androidId", DataHolder.androidId)
                }
                DataHolder.service?.let { ContextCompat.startForegroundService(activity, DataHolder.service!!) }
            }else{

                requestDeviceLocationSettings()
            }
        }


    @RequiresApi(Build.VERSION_CODES.Q)
    fun askForPreciseLocation(){

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
                // Only approximate location access granted.
                Log.d("nee location-permission", "Approximate Location : Granted")

               askForPreciseLocation()   /// ask for Precise Location to user


            }

            else -> {
                // No location access granted.
                Log.d("neel ", "location-permission  :  Not Granted")


            }

        }
    }



     @RequiresApi(Build.VERSION_CODES.Q)
     fun checkLocationPermission() {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

            if (ContextCompat.checkSelfPermission(activity, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(activity, Manifest.permission.ACCESS_COARSE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {

                Log.d("neel ", "CheckLocationPermission : Ok")

                    //after taking permission check the GPS is unable or not
                    requestDeviceLocationSettings()

                if (ContextCompat.checkSelfPermission(
                        activity,
                        Manifest.permission.ACCESS_BACKGROUND_LOCATION
                    )==PackageManager.PERMISSION_GRANTED
                ){
                    // if already background permission granted then start the service
                    DataHolder.service = Intent(activity, LocationService::class.java).apply {
                        putExtra("userId", DataHolder.userId)
                        putExtra("companyId", DataHolder.companyId)
                        putExtra("androidId", DataHolder.androidId)
                    }
                    Log.d("neel ", "CheckLocationPermission : Ok  - start service ")
                    DataHolder.service?.let { ContextCompat.startForegroundService(activity, DataHolder.service!!) }

                }

                if (ContextCompat.checkSelfPermission(
                        activity, Manifest.permission.ACCESS_FINE_LOCATION
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    locationPermissionRequest.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION))
                }


            } else if (activity.shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION) && activity.shouldShowRequestPermissionRationale(
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


}
