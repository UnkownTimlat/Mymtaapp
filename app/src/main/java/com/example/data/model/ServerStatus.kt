package com.example.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class ServerStatus(
    @Json(name = "serverName") val serverName: String? = "MTA:SA Server",
    @Json(name = "serverIp") val serverIp: String? = null,
    @Json(name = "serverPort") val serverPort: Int? = 22003,
    @Json(name = "gameType") val gameType: String? = "Play",
    @Json(name = "mapName") val mapName: String? = "San Andreas",
    @Json(name = "playerCount") val playerCount: Int? = 0,
    @Json(name = "maxPlayers") val maxPlayers: Int? = 128,
    @Json(name = "version") val version: String? = "1.6.0",
    @Json(name = "uptime") val uptime: Long? = 0L,
    @Json(name = "passwordProtected") val passwordProtected: Boolean? = false
)
