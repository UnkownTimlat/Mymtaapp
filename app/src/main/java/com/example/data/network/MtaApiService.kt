package com.example.data.network

import com.example.data.model.MtaPlayer
import com.example.data.model.ServerBlip
import com.example.data.model.ServerStatus
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path

interface MtaApiService {

    @GET("api/status")
    suspend fun getServerStatus(
        @Header("Authorization") authHeader: String? = null
    ): Response<ServerStatus>

    @GET("api/players")
    suspend fun getPlayers(
        @Header("Authorization") authHeader: String? = null
    ): Response<List<MtaPlayer>>

    @GET("api/players/{id}")
    suspend fun getPlayerDetails(
        @Path("id") id: Int,
        @Header("Authorization") authHeader: String? = null
    ): Response<MtaPlayer>

    @GET("api/blips")
    suspend fun getServerBlips(
        @Header("Authorization") authHeader: String? = null
    ): Response<List<ServerBlip>>

    @POST("api/auth")
    suspend fun authenticate(
        @Body payload: Map<String, String>
    ): Response<Map<String, Any>>
}
