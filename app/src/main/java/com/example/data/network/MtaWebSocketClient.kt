package com.example.data.network

import android.util.Log
import com.example.data.model.AuthRequest
import com.example.data.model.ConnectionState
import com.example.data.model.MtaPlayer
import com.example.data.model.PlayerJoinEvent
import com.example.data.model.PlayerQuitEvent
import com.example.data.model.PlayerUpdateEvent
import com.example.data.model.PlayerVehicleUpdateEvent
import com.example.data.model.ServerBlip
import com.example.data.model.ServerBlipsEvent
import com.example.data.model.ServerStatus
import com.example.data.model.ServerStatusEvent
import com.example.data.model.SubscribeRequest
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import java.util.concurrent.TimeUnit

sealed interface IncomingEvent {
    data class Status(val status: ServerStatus) : IncomingEvent
    data class PlayerJoined(val player: MtaPlayer) : IncomingEvent
    data class PlayerQuit(val id: Int, val name: String?, val reason: String?) : IncomingEvent
    data class PlayerUpdated(val player: MtaPlayer) : IncomingEvent
    data class PlayerVehicleUpdated(val id: Int, val vehicle: String?, val vehicleHealth: Float?, val speed: Float?) : IncomingEvent
    data class ServerBlipsReceived(val blips: List<ServerBlip>) : IncomingEvent
    data class AuthResult(val success: Boolean, val message: String?) : IncomingEvent
    data class ErrorEvent(val message: String) : IncomingEvent
}

class MtaWebSocketClient(
    private val okHttpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(0, TimeUnit.MILLISECONDS) // 0 for infinite WebSocket read
        .build()
) {
    private val tag = "MtaWebSocketClient"
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val moshi: Moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    private val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected())
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    private val _events = MutableSharedFlow<IncomingEvent>(extraBufferCapacity = 64)
    val events: SharedFlow<IncomingEvent> = _events.asSharedFlow()

    private var activeWebSocket: WebSocket? = null
    private var heartbeatJob: Job? = null
    private var reconnectJob: Job? = null

    private var currentUrl: String = ""
    private var currentToken: String = ""
    private var shouldAutoReconnect = false
    private var reconnectAttempts = 0

    fun connect(wsUrl: String, authToken: String = "") {
        val trimmedUrl = wsUrl.trim()
        if (trimmedUrl.isBlank() || trimmedUrl.contains("YOUR_SERVER_API", ignoreCase = true)) {
            _connectionState.value = ConnectionState.Error(
                "Please configure a valid backend WebSocket URL (currently contains placeholder)."
            )
            return
        }

        if (!trimmedUrl.startsWith("ws://") && !trimmedUrl.startsWith("wss://")) {
            _connectionState.value = ConnectionState.Error(
                "Invalid WebSocket protocol: URL must start with ws:// or wss://"
            )
            return
        }

        currentUrl = trimmedUrl
        currentToken = authToken.trim()
        shouldAutoReconnect = true
        reconnectAttempts = 0

        performConnect()
    }

    private fun performConnect() {
        disconnectInternal(clearTarget = false)
        _connectionState.value = ConnectionState.Connecting(currentUrl)

        val requestBuilder = Request.Builder().url(currentUrl)
        if (currentToken.isNotBlank()) {
            requestBuilder.addHeader("Authorization", "Bearer $currentToken")
        }

        val request = try {
            requestBuilder.build()
        } catch (e: Exception) {
            _connectionState.value = ConnectionState.Error("Malformed URL: ${e.localizedMessage}")
            return
        }

        activeWebSocket = okHttpClient.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                Log.d(tag, "WebSocket connection opened: $currentUrl")
                reconnectAttempts = 0
                _connectionState.value = ConnectionState.Connected(currentUrl)

                // Start periodic ping heartbeat
                startHeartbeat(webSocket)

                // If authentication token is supplied, send auth packet
                if (currentToken.isNotBlank()) {
                    _connectionState.value = ConnectionState.Authenticating(currentUrl)
                    val authPayload = moshi.adapter(AuthRequest::class.java).toJson(
                        AuthRequest(token = currentToken)
                    )
                    webSocket.send(authPayload)
                }

                // Send default subscription packet
                val subPayload = moshi.adapter(SubscribeRequest::class.java).toJson(
                    SubscribeRequest()
                )
                webSocket.send(subPayload)
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                scope.launch {
                    handleIncomingJson(text)
                }
            }

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                Log.d(tag, "WebSocket closing code=$code reason=$reason")
                webSocket.close(1000, null)
                _connectionState.value = ConnectionState.Disconnected("Server closed connection: $reason")
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                Log.d(tag, "WebSocket closed code=$code reason=$reason")
                stopHeartbeat()
                _connectionState.value = ConnectionState.Disconnected(reason.ifEmpty { "Connection closed" })
                scheduleReconnectIfAppropriate()
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                val errorMsg = t.localizedMessage ?: "Unknown network failure connecting to server"
                Log.e(tag, "WebSocket failure: $errorMsg", t)
                stopHeartbeat()
                _connectionState.value = ConnectionState.Error("Connection failed: $errorMsg")
                scheduleReconnectIfAppropriate()
            }
        })
    }

    private fun handleIncomingJson(text: String) {
        try {
            val mapAdapter = moshi.adapter(Map::class.java)
            @Suppress("UNCHECKED_CAST")
            val rootMap = mapAdapter.fromJson(text) as? Map<String, Any?> ?: return
            val type = rootMap["type"] as? String ?: return

            when (type) {
                "player_update" -> {
                    val event = moshi.adapter(PlayerUpdateEvent::class.java).fromJson(text)
                    if (event != null) {
                        _events.tryEmit(IncomingEvent.PlayerUpdated(event.player))
                    }
                }
                "player_join" -> {
                    val event = moshi.adapter(PlayerJoinEvent::class.java).fromJson(text)
                    if (event != null) {
                        _events.tryEmit(IncomingEvent.PlayerJoined(event.player))
                    }
                }
                "player_quit" -> {
                    val event = moshi.adapter(PlayerQuitEvent::class.java).fromJson(text)
                    if (event != null) {
                        _events.tryEmit(IncomingEvent.PlayerQuit(event.id, event.name, event.reason))
                    }
                }
                "player_vehicle_update" -> {
                    val event = moshi.adapter(PlayerVehicleUpdateEvent::class.java).fromJson(text)
                    if (event != null) {
                        _events.tryEmit(
                            IncomingEvent.PlayerVehicleUpdated(
                                id = event.id,
                                vehicle = event.vehicle,
                                vehicleHealth = event.vehicleHealth,
                                speed = event.speed
                            )
                        )
                    }
                }
                "server_blips" -> {
                    val event = moshi.adapter(ServerBlipsEvent::class.java).fromJson(text)
                    if (event != null) {
                        _events.tryEmit(IncomingEvent.ServerBlipsReceived(event.blips))
                    }
                }
                "server_status" -> {
                    val event = moshi.adapter(ServerStatusEvent::class.java).fromJson(text)
                    if (event != null) {
                        _events.tryEmit(IncomingEvent.Status(event.server))
                    }
                }
                "auth_response" -> {
                    val success = (rootMap["success"] as? Boolean) ?: true
                    val message = rootMap["message"] as? String
                    if (success) {
                        _connectionState.value = ConnectionState.Authenticated(currentUrl)
                    } else {
                        _connectionState.value = ConnectionState.Error("Auth failed: ${message ?: "Unauthorized"}")
                    }
                    _events.tryEmit(IncomingEvent.AuthResult(success, message))
                }
                "error" -> {
                    val message = rootMap["message"] as? String ?: "Server error"
                    _events.tryEmit(IncomingEvent.ErrorEvent(message))
                }
                else -> {
                    Log.d(tag, "Received unhandled event type: $type")
                }
            }
        } catch (e: Exception) {
            Log.e(tag, "Error parsing incoming WebSocket frame: ${e.message}", e)
        }
    }

    private fun startHeartbeat(webSocket: WebSocket) {
        stopHeartbeat()
        heartbeatJob = scope.launch {
            while (isActive) {
                delay(25_000L)
                try {
                    webSocket.send("{\"type\":\"ping\"}")
                } catch (e: Exception) {
                    Log.w(tag, "Failed to send ping heartbeat", e)
                    break
                }
            }
        }
    }

    private fun stopHeartbeat() {
        heartbeatJob?.cancel()
        heartbeatJob = null
    }

    private fun scheduleReconnectIfAppropriate() {
        if (!shouldAutoReconnect || currentUrl.isBlank()) return
        if (reconnectAttempts >= 5) {
            Log.d(tag, "Max reconnect attempts reached ($reconnectAttempts)")
            return
        }

        reconnectJob?.cancel()
        reconnectJob = scope.launch {
            reconnectAttempts++
            val delayMs = (2_000L * reconnectAttempts).coerceAtMost(10_000L)
            Log.d(tag, "Scheduling reconnect #$reconnectAttempts in ${delayMs}ms")
            delay(delayMs)
            if (shouldAutoReconnect && isActive) {
                performConnect()
            }
        }
    }

    fun disconnect() {
        shouldAutoReconnect = false
        disconnectInternal(clearTarget = true)
        _connectionState.value = ConnectionState.Disconnected("User disconnected")
    }

    private fun disconnectInternal(clearTarget: Boolean) {
        stopHeartbeat()
        reconnectJob?.cancel()
        reconnectJob = null

        try {
            activeWebSocket?.close(1000, "App closed")
        } catch (e: Exception) {
            Log.w(tag, "Error closing websocket", e)
        }
        activeWebSocket = null

        if (clearTarget) {
            currentUrl = ""
            currentToken = ""
        }
    }

    fun sendRaw(message: String): Boolean {
        return activeWebSocket?.send(message) ?: false
    }
}
