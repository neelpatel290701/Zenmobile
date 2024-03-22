package pro.zentrades.android

import com.google.firebase.database.IgnoreExtraProperties

@IgnoreExtraProperties
data class TechnicianObject(val deviceId: String? = null, val latitude: String? = null, val longitude: String? = null, val recordedAt: MutableMap<String, String>? = null){
    // Null default values create a no-argument default constructor, which is needed
    // for deserialization from a DataSnapshot
}
