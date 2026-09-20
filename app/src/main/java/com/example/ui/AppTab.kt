package com.example.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.vector.ImageVector

enum class AppTab(val title: String, val icon: ImageVector) {
    MAP("Live Map", Icons.Default.Map),
    PLAYERS("Players", Icons.Default.People),
    STATUS("Server", Icons.Default.Dns),
    SETTINGS("Settings", Icons.Default.Settings)
}
