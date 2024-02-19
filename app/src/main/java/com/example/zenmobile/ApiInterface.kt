package com.example.zenmobile

import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface ApiInterface {

    @GET("posts")
    fun getdata() : Call<List<dataModel>>

    @POST("posts")
    fun sendData(
        @Body userData : dataModel
    ) : Call<dataModel>

}