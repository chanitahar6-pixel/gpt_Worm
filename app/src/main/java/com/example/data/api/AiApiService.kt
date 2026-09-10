package com.example.data.api

import okhttp3.OkHttpClient
import okhttp3.ResponseBody
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

data class ChatApiRequest(
    val user_id: String,
    val message: String
)

data class UserIdRequest(
    val user_id: String
)

interface AiApiService {
    @Headers("Content-Type: application/json")
    @POST("chat")
    suspend fun sendChatMessage(@Body request: ChatApiRequest): Response<ResponseBody>

    @FormUrlEncoded
    @POST("chat/form")
    suspend fun sendChatForm(
        @Field("user_id") userId: String,
        @Field("message") message: String
    ): Response<ResponseBody>

    @GET("ask")
    suspend fun askQuestion(@Query("question") question: String): Response<ResponseBody>

    @GET("ask/text")
    suspend fun askQuestionText(@Query("question") question: String): Response<ResponseBody>

    @Headers("Content-Type: application/json")
    @POST("session")
    suspend fun createSession(@Body request: UserIdRequest): Response<ResponseBody>

    @Headers("Content-Type: application/json")
    @POST("reset")
    suspend fun resetMemory(@Body request: UserIdRequest): Response<ResponseBody>

    @GET("session/{user_id}")
    suspend fun getSession(@Path("user_id") userId: String): Response<ResponseBody>

    @DELETE("session/{user_id}")
    suspend fun deleteSession(@Path("user_id") userId: String): Response<ResponseBody>

    @GET("health")
    suspend fun getHealth(): Response<ResponseBody>

    @GET("version")
    suspend fun getVersion(): Response<ResponseBody>

    @GET("api")
    suspend fun getApiInfo(): Response<ResponseBody>
}

object AiApiClient {
    private const val BASE_URL = "http://51.75.118.171:20085/"

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
            .build()
            .create(AiApiService::class.java)
    }
}
