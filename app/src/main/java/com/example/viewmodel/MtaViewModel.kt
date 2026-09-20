package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.ConnectionState
import com.example.data.model.LogEntry
import com.example.data.model.MapCalibrationConfig
import com.example.data.model.MtaPlayer
import com.example.data.model.ServerBlip
import com.example.data.model.ServerStatus
import com.example.data.network.IncomingEvent
import com.example.data.network.NetworkManager
import com.example.data.preferences.ServerSettings
import com.example.data.preferences.SettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MtaViewModel(application: Application) : AndroidViewModel(application) {

    private val settingsRepository = SettingsRepository(application)
    private val networkManager = NetworkManager()

    val settings: StateFlow<ServerSettings> = settingsRepository.settings

    val connectionState: StateFlow<ConnectionState> = networkManager.webSocketClient.connectionState

    private val _serverStatus = MutableStateFlow<ServerStatus?>(null)
    val serverStatus: StateFlow<ServerStatus?> = _serverStatus.asStateFlow()

    private val _players = MutableStateFlow<Map<Int, MtaPlayer>>(emptyMap())
    val players: StateFlow<Map<Int, MtaPlayer>> = _players.asStateFlow()

    private val _blips = MutableStateFlow<List<ServerBlip>>(emptyList())
    val blips: StateFlow<List<ServerBlip>> = _blips.asStateFlow()

    private val _selectedPlayer = MutableStateFlow<MtaPlayer?>(null)
    val selectedPlayer: StateFlow<MtaPlayer?> = _selectedPlayer.asStateFlow()

    private val _focusedPlayerId = MutableStateFlow<Int?>(null)
    val focusedPlayerId: StateFlow<Int?> = _focusedPlayerId.asStateFlow()

    private val _isFollowing = MutableStateFlow<Boolean>(false)
    val isFollowing: StateFlow<Boolean> = _isFollowing.asStateFlow()

    private val _connectionLogs = MutableStateFlow<List<LogEntry>>(emptyList())
    val connectionLogs: StateFlow<List<LogEntry>> = _connectionLogs.asStateFlow()

    private val _isRestTesting = MutableStateFlow(false)
    val isRestTesting: StateFlow<Boolean> = _isRestTesting.asStateFlow()

    private val _restTestResult = MutableStateFlow<String?>(null)
    val restTestResult: StateFlow<String?> = _restTestResult.asStateFlow()

    init {
        // Collect incoming WebSocket events
        viewModelScope.launch {
            networkManager.webSocketClient.events.collect { event ->
                handleIncomingEvent(event)
            }
        }

        // Collect connection state changes for logging
        viewModelScope.launch {
            connectionState.collect { state ->
                when (state) {
                    is ConnectionState.Connected -> {
                        addLog("NET", "WebSocket connected to ${state.endpoint}")
                        fetchRestSnapshotIfAvailable()
                    }
                    is ConnectionState.Connecting -> {
                        addLog("NET", "Connecting to ${state.endpoint}...")
                    }
                    is ConnectionState.Authenticating -> {
                        addLog("AUTH", "Sending authentication token...")
                    }
                    is ConnectionState.Authenticated -> {
                        addLog("AUTH", "Authentication approved by server")
                    }
                    is ConnectionState.Disconnected -> {
                        addLog("NET", "Disconnected: ${state.reason ?: "Idle"}")
                    }
                    is ConnectionState.Error -> {
                        addLog("ERROR", state.message, isError = true)
                    }
                }
            }
        }

        // Auto-connect if a non-placeholder URL has been configured
        val initialSettings = settings.value
        if (isValidServerConfig(initialSettings.wsUrl)) {
            connect()
        } else {
            addLog("SYSTEM", "Awaiting server configuration. Configure backend URL in Settings.")
        }
    }

    private fun handleIncomingEvent(event: IncomingEvent) {
        when (event) {
            is IncomingEvent.Status -> {
                _serverStatus.value = event.status
                addLog("STATUS", "Server status received: ${event.status.serverName} (${event.status.playerCount} players)")
            }
            is IncomingEvent.PlayerJoined -> {
                val p = event.player
                _players.value = _players.value + (p.id to p)
                addLog("JOIN", "${p.name} (ID ${p.id}) joined")
            }
            is IncomingEvent.PlayerQuit -> {
                val removed = _players.value[event.id]
                _players.value = _players.value - event.id
                val name = event.name ?: removed?.name ?: "Player #${event.id}"
                addLog("QUIT", "$name left: ${event.reason ?: "Quit"}")

                if (_focusedPlayerId.value == event.id) {
                    _focusedPlayerId.value = null
                    _isFollowing.value = false
                }
                if (_selectedPlayer.value?.id == event.id) {
                    _selectedPlayer.value = null
                }
            }
            is IncomingEvent.PlayerUpdated -> {
                val updated = event.player
                val currentMap = _players.value
                val existing = currentMap[updated.id]

                // Merge or update player record
                val merged = if (existing != null) {
                    existing.copy(
                        name = updated.name.ifBlank { existing.name },
                        x = updated.x,
                        y = updated.y,
                        z = updated.z,
                        interior = updated.interior,
                        dimension = updated.dimension,
                        money = updated.money,
                        health = updated.health,
                        armor = updated.armor,
                        vehicle = updated.vehicle ?: existing.vehicle,
                        vehicleHealth = updated.vehicleHealth ?: existing.vehicleHealth,
                        rank = updated.rank ?: existing.rank,
                        playtime = updated.playtime ?: existing.playtime,
                        ping = updated.ping ?: existing.ping,
                        skin = updated.skin ?: existing.skin
                    )
                } else {
                    updated
                }

                _players.value = currentMap + (merged.id to merged)

                if (_selectedPlayer.value?.id == merged.id) {
                    _selectedPlayer.value = merged
                }
            }
            is IncomingEvent.PlayerVehicleUpdated -> {
                val current = _players.value[event.id]
                if (current != null) {
                    val updated = current.copy(
                        vehicle = event.vehicle,
                        vehicleHealth = event.vehicleHealth
                    )
                    _players.value = _players.value + (event.id to updated)
                    if (_selectedPlayer.value?.id == event.id) {
                        _selectedPlayer.value = updated
                    }
                }
            }
            is IncomingEvent.ServerBlipsReceived -> {
                _blips.value = event.blips
                addLog("BLIPS", "Received ${event.blips.size} server blips")
            }
            is IncomingEvent.AuthResult -> {
                if (event.success) {
                    addLog("AUTH", "Auth success: ${event.message ?: "Token verified"}")
                } else {
                    addLog("AUTH", "Auth failed: ${event.message ?: "Denied"}", isError = true)
                }
            }
            is IncomingEvent.ErrorEvent -> {
                addLog("SERVER", event.message, isError = true)
            }
        }
    }

    private fun fetchRestSnapshotIfAvailable() {
        viewModelScope.launch {
            val s = settings.value
            val apiService = networkManager.createApiService(s.restBaseUrl) ?: return@launch
            try {
                val tokenHeader = if (s.authToken.isNotBlank()) "Bearer ${s.authToken}" else null

                val statusResp = apiService.getServerStatus(tokenHeader)
                if (statusResp.isSuccessful && statusResp.body() != null) {
                    _serverStatus.value = statusResp.body()
                }

                val playersResp = apiService.getPlayers(tokenHeader)
                if (playersResp.isSuccessful && playersResp.body() != null) {
                    val playerList = playersResp.body()!!
                    val newMap = playerList.associateBy { it.id }
                    _players.value = _players.value + newMap
                    addLog("REST", "Synced ${playerList.size} players via REST")
                }

                val blipsResp = apiService.getServerBlips(tokenHeader)
                if (blipsResp.isSuccessful && blipsResp.body() != null) {
                    _blips.value = blipsResp.body()!!
                }
            } catch (e: Exception) {
                addLog("REST", "Initial REST sync: ${e.message ?: "Skipped"}")
            }
        }
    }

    fun connect() {
        val s = settings.value
        if (!isValidServerConfig(s.wsUrl)) {
            addLog("CONFIG", "Cannot connect: Placeholder or empty backend URL configured.", isError = true)
            return
        }
        networkManager.webSocketClient.connect(s.wsUrl, s.authToken)
    }

    fun disconnect() {
        networkManager.webSocketClient.disconnect()
    }

    fun saveConnectionSettings(restUrl: String, wsUrl: String, token: String) {
        settingsRepository.saveConnection(restUrl, wsUrl, token)
        addLog("CONFIG", "Updated configuration: WS=$wsUrl REST=$restUrl")
        disconnect()
        connect()
    }

    fun testRestConnection() {
        viewModelScope.launch {
            _isRestTesting.value = true
            _restTestResult.value = null
            val currentSettings = settings.value
            val api = networkManager.createApiService(currentSettings.restBaseUrl)

            if (api == null) {
                _restTestResult.value = "Invalid REST Base URL or placeholder still present."
                _isRestTesting.value = false
                return@launch
            }

            try {
                val tokenHeader = if (currentSettings.authToken.isNotBlank()) "Bearer ${currentSettings.authToken}" else null
                val response = api.getServerStatus(tokenHeader)
                if (response.isSuccessful && response.body() != null) {
                    val status = response.body()!!
                    _serverStatus.value = status
                    _restTestResult.value = "Success! Connected to '${status.serverName ?: "Server"}' (${status.playerCount} players online)"
                    addLog("TEST", "REST ping success: ${status.serverName}")
                } else {
                    _restTestResult.value = "HTTP ${response.code()}: ${response.message()}"
                    addLog("TEST", "REST ping HTTP error ${response.code()}", isError = true)
                }
            } catch (e: Exception) {
                _restTestResult.value = "Connection failed: ${e.localizedMessage ?: e.javaClass.simpleName}"
                addLog("TEST", "REST error: ${e.localizedMessage}", isError = true)
            } finally {
                _isRestTesting.value = false
            }
        }
    }

    fun selectPlayer(player: MtaPlayer?) {
        _selectedPlayer.value = player
    }

    fun selectPlayerById(id: Int) {
        _selectedPlayer.value = _players.value[id]
    }

    fun focusPlayer(playerId: Int, follow: Boolean = false) {
        _focusedPlayerId.value = playerId
        _isFollowing.value = follow
        val p = _players.value[playerId]
        if (p != null) {
            _selectedPlayer.value = p
            addLog("MAP", "Focused on ${p.name} (ID $playerId) [Follow=$follow]")
        }
    }

    fun setFollowing(follow: Boolean) {
        _isFollowing.value = follow
    }

    fun clearFocus() {
        _focusedPlayerId.value = null
        _isFollowing.value = false
    }

    fun updateCalibration(minX: Float, maxX: Float, minY: Float, maxY: Float) {
        settingsRepository.saveCalibration(minX, maxX, minY, maxY)
        addLog("MAP", "Map bounds updated: X[$minX, $maxX] Y[$minY, $maxY]")
    }

    fun resetCalibration() {
        settingsRepository.saveCalibration(-3000f, 3000f, -3000f, 3000f)
        addLog("MAP", "Map bounds reset to standard GTA:SA (-3000, 3000)")
    }

    fun clearLogs() {
        _connectionLogs.value = emptyList()
    }

    private fun addLog(tag: String, message: String, isError: Boolean = false) {
        val entry = LogEntry(
            tag = tag,
            message = message,
            isError = isError
        )
        // Keep last 150 entries
        _connectionLogs.value = (_connectionLogs.value + entry).takeLast(150)
    }

    private fun isValidServerConfig(url: String): Boolean {
        val trimmed = url.trim()
        return trimmed.isNotBlank() && !trimmed.contains("YOUR_SERVER_API", ignoreCase = true)
    }
}
