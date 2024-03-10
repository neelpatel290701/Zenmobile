package pro.zentrades.android

import com.google.firebase.Firebase
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ServerValue
import com.google.firebase.database.database

class DatabaseClass() {

    val database : DatabaseReference = Firebase.database("https://zendatabase-dfa16-default-rtdb.firebaseio.com/").getReference("technicians")

    fun sendLocation(Total_dist: String , userId : String , companyId : String , deviceId : String , latitide : String , longitude : String){

        val userId_companyId : String = userId + "_" + companyId

        val technicianObject  = TechnicianObject(deviceId , latitide ,longitude, ServerValue.TIMESTAMP)

        val key = database.child(userId_companyId).push().key
        if (key != null) {
            database.child(userId_companyId).child(key).setValue(technicianObject)
        }
    }

}