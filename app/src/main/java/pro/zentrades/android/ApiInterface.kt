package pro.zentrades.android

import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

interface ApiInterface {

//    @GET("posts")
//    fun getdata() : Call<List<dataModel>>

    @POST("token")
    fun sendToken(
        @Header("user-id") userId : String  ,
        @Header("company-id") companyId : String ,
        @Header("access-token") accessToken : String ,
        @Body userToken : dataModelItem
    ) : Call<dataModelItem>

}