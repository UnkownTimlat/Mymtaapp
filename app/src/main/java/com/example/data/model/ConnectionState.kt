package com.example.data.model

sealed interface ConnectionState {
    data class Disconnected(val reason: String? = null) : ConnectionState
    data class Connecting(val endpoint: String) : ConnectionState
    data class Connected(val endpoint: String) : ConnectionState
    data class Authenticating(val endpoint: String) : ConnectionState
    data class Authenticated(val endpoint: String) : ConnectionState
    data class Error(val message: String, val timestamp: Long = System.currentTimeMillis()) : ConnectionState
}

data class LogEntry(
    val timestamp: Long = System.currentTimeMillis(),
    val tag: String,
    val message: String,
    val isError: Boolean = false
)
