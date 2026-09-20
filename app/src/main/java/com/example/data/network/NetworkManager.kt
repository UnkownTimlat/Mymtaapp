package com.example.data.network

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit

class NetworkManager {
    private val moshi: Moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    val okHttpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    val webSocketClient: MtaWebSocketClient = MtaWebSocketClient(okHttpClient)

    fun createApiService(baseUrl: String): MtaApiService? {
        val cleanUrl = baseUrl.trim()
        if (cleanUrl.isBlank() || cleanUrl.contains("YOUR_SERVER_API", ignoreCase = true)) {
            return null
        }
        val validatedUrl = if (!cleanUrl.endsWith("/")) "$cleanUrl/" else cleanUrl
        return try {
            Retrofit.Builder()
                .baseUrl(validatedUrl)
                .client(okHttpClient)
                .addConverterFactory(MoshiConverterFactory.create(moshi))
                .build()
                .create(MtaApiService::class.java)
        } catch (e: Exception) {
            null
        }
    }
}
