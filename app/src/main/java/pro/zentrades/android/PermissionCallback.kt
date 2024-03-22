package pro.zentrades.android

interface PermissionCallback {

    fun onCameraPermissionGranted()
    fun onCameraPermissionDenied()
    fun onNotificationPermissionGranted()
}