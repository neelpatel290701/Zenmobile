package pro.zentrades.android

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory


object RetrofitInstance {

    private const val BASE_URL = "https://services-stage.smartserv.io/api/notification/"
    private const val BASE_URL2 = "https://jsonplaceholder.typicode.com/"

    //create logger
    private val logger = HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.BODY)

    // create okHttp instance
    private val okHttp : OkHttpClient.Builder = OkHttpClient.Builder().addInterceptor(logger)

    private val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .client(okHttp.build())
            .build()
    }


    val apiInterface: ApiInterface by lazy {
        retrofit.create(ApiInterface::class.java)
    }



    //for get response - IsCacheCleared  ?
    private val retrofit2: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL2)
            .addConverterFactory(GsonConverterFactory.create())
            .client(okHttp.build())
            .build()
    }

    val apiInterface2: ApiInterface by lazy {
        retrofit2.create(ApiInterface::class.java)
    }


}


