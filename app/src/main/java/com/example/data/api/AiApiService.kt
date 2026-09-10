package com.example.data.api

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.ResponseBody
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.POST
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

interface AiApiService {
    @Headers("Content-Type: application/json")
    @POST("chat")
    suspend fun sendChatMessage(@Body body: RequestBody): Response<ResponseBody>

    @Headers("Content-Type: application/json")
    @POST("clear")
    suspend fun clearSession(@Body body: RequestBody): Response<ResponseBody>

    @GET("ask/text")
    suspend fun askText(
        @Query("question") question: String,
        @Query("session_id") sessionId: String? = null
    ): Response<ResponseBody>

    @GET("ask")
    suspend fun ask(
        @Query("q") question: String,
        @Query("session_id") sessionId: String? = null
    ): Response<ResponseBody>

    @GET("health")
    suspend fun getHealth(): Response<ResponseBody>

    @GET("sessions")
    suspend fun getSessions(): Response<ResponseBody>
}

object AiApiClient {
    private const val BASE_URL = "http://51.75.118.171:20085/"

    private val moshi: Moshi by lazy {
        Moshi.Builder()
            .addLast(KotlinJsonAdapterFactory())
            .build()
    }

    private val okHttpClient: OkHttpClient by lazy {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        }
        OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .addInterceptor(logging)
            .retryOnConnectionFailure(true)
            .build()
    }

    val service: AiApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(AiApiService::class.java)
    }

    fun createJsonRequestBody(jsonString: String): RequestBody {
        return jsonString.toRequestBody("application/json; charset=utf-8".toMediaType())
    }
}
