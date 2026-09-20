package com.example.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class MtaPlayer(
    @Json(name = "id") val id: Int,
    @Json(name = "name") val name: String,
    @Json(name = "x") val x: Float,
    @Json(name = "y") val y: Float,
    @Json(name = "z") val z: Float,
    @Json(name = "interior") val interior: Int = 0,
    @Json(name = "dimension") val dimension: Int = 0,
    @Json(name = "money") val money: Long = 0L,
    @Json(name = "health") val health: Float = 100f,
    @Json(name = "armor") val armor: Float = 0f,
    @Json(name = "vehicle") val vehicle: String? = null,
    @Json(name = "vehicleHealth") val vehicleHealth: Float? = null,
    @Json(name = "rank") val rank: String? = "Player",
    @Json(name = "playtime") val playtime: Long? = 0L,
    @Json(name = "ping") val ping: Int? = null,
    @Json(name = "skin") val skin: Int? = null,
    @Json(name = "ip") val ip: String? = null
)
