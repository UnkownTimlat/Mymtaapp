package com.example.data.preferences

import android.content.Context
import android.content.SharedPreferences
import com.example.data.model.MapCalibrationConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class ServerSettings(
    val restBaseUrl: String,
    val wsUrl: String,
    val authToken: String,
    val calibration: MapCalibrationConfig
)

class SettingsRepository(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("mta_connection_settings", Context.MODE_PRIVATE)

    companion object {
        const val KEY_REST_URL = "rest_base_url"
        const val KEY_WS_URL = "ws_url"
        const val KEY_AUTH_TOKEN = "auth_token"
        const val KEY_MIN_X = "map_min_x"
        const val KEY_MAX_X = "map_max_x"
        const val KEY_MIN_Y = "map_min_y"
        const val KEY_MAX_Y = "map_max_y"

        const val DEFAULT_REST_URL = "https://YOUR_SERVER_API/"
        const val DEFAULT_WS_URL = "wss://YOUR_SERVER_API/ws"
    }

    private val _settings = MutableStateFlow(loadSettings())
    val settings: StateFlow<ServerSettings> = _settings.asStateFlow()

    private fun loadSettings(): ServerSettings {
        val restUrl = prefs.getString(KEY_REST_URL, DEFAULT_REST_URL) ?: DEFAULT_REST_URL
        val wsUrl = prefs.getString(KEY_WS_URL, DEFAULT_WS_URL) ?: DEFAULT_WS_URL
        val token = prefs.getString(KEY_AUTH_TOKEN, "") ?: ""
        val minX = prefs.getFloat(KEY_MIN_X, -3000f)
        val maxX = prefs.getFloat(KEY_MAX_X, 3000f)
        val minY = prefs.getFloat(KEY_MIN_Y, -3000f)
        val maxY = prefs.getFloat(KEY_MAX_Y, 3000f)

        return ServerSettings(
            restBaseUrl = restUrl,
            wsUrl = wsUrl,
            authToken = token,
            calibration = MapCalibrationConfig(minX, maxX, minY, maxY)
        )
    }

    fun saveConnection(restUrl: String, wsUrl: String, authToken: String) {
        val cleanRest = restUrl.trim()
        val cleanWs = wsUrl.trim()
        val cleanToken = authToken.trim()

        prefs.edit()
            .putString(KEY_REST_URL, cleanRest)
            .putString(KEY_WS_URL, cleanWs)
            .putString(KEY_AUTH_TOKEN, cleanToken)
            .apply()

        _settings.value = _settings.value.copy(
            restBaseUrl = cleanRest,
            wsUrl = cleanWs,
            authToken = cleanToken
        )
    }

    fun saveCalibration(minX: Float, maxX: Float, minY: Float, maxY: Float) {
        val cal = MapCalibrationConfig(minX, maxX, minY, maxY)
        prefs.edit()
            .putFloat(KEY_MIN_X, minX)
            .putFloat(KEY_MAX_X, maxX)
            .putFloat(KEY_MIN_Y, minY)
            .putFloat(KEY_MAX_Y, maxY)
            .apply()

        _settings.value = _settings.value.copy(calibration = cal)
    }

    fun resetToDefaults() {
        prefs.edit().clear().apply()
        _settings.value = loadSettings()
    }
}
