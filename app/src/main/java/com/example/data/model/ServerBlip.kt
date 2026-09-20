package com.example.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class ServerBlip(
    @Json(name = "id") val id: Int,
    @Json(name = "icon") val icon: Int = 0,
    @Json(name = "x") val x: Float,
    @Json(name = "y") val y: Float,
    @Json(name = "z") val z: Float,
    @Json(name = "name") val name: String? = null,
    @Json(name = "color") val color: String? = null
)
