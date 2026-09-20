package com.example.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class PlayerUpdateEvent(
    @Json(name = "type") val type: String = "player_update",
    @Json(name = "player") val player: MtaPlayer
)

@JsonClass(generateAdapter = true)
data class PlayerJoinEvent(
    @Json(name = "type") val type: String = "player_join",
    @Json(name = "player") val player: MtaPlayer
)

@JsonClass(generateAdapter = true)
data class PlayerQuitEvent(
    @Json(name = "type") val type: String = "player_quit",
    @Json(name = "id") val id: Int,
    @Json(name = "name") val name: String? = null,
    @Json(name = "reason") val reason: String? = null
)

@JsonClass(generateAdapter = true)
data class PlayerVehicleUpdateEvent(
    @Json(name = "type") val type: String = "player_vehicle_update",
    @Json(name = "id") val id: Int,
    @Json(name = "vehicle") val vehicle: String? = null,
    @Json(name = "vehicleHealth") val vehicleHealth: Float? = null,
    @Json(name = "speed") val speed: Float? = null
)

@JsonClass(generateAdapter = true)
data class ServerBlipsEvent(
    @Json(name = "type") val type: String = "server_blips",
    @Json(name = "blips") val blips: List<ServerBlip> = emptyList()
)

@JsonClass(generateAdapter = true)
data class ServerStatusEvent(
    @Json(name = "type") val type: String = "server_status",
    @Json(name = "server") val server: ServerStatus
)

@JsonClass(generateAdapter = true)
data class AuthRequest(
    @Json(name = "type") val type: String = "authenticate",
    @Json(name = "token") val token: String
)

@JsonClass(generateAdapter = true)
data class SubscribeRequest(
    @Json(name = "type") val type: String = "subscribe",
    @Json(name = "channels") val channels: List<String> = listOf("players", "blips", "status")
)
