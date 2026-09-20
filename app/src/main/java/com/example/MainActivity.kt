package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.model.ConnectionState
import com.example.ui.AppTab
import com.example.ui.components.PlayerProfileSheet
import com.example.ui.screens.MapScreen
import com.example.ui.screens.PlayersScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.StatusScreen
import com.example.ui.theme.CyanNeon
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.Slate400
import com.example.ui.theme.Slate800
import com.example.ui.theme.Slate900
import com.example.ui.theme.Slate950
import com.example.viewmodel.MtaViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                MtaApp()
            }
        }
    }
}

@Composable
fun MtaApp(viewModel: MtaViewModel = viewModel()) {
    var currentTab by rememberSaveable { mutableStateOf(AppTab.MAP) }

    val connectionState by viewModel.connectionState.collectAsStateWithLifecycle()
    val serverStatus by viewModel.serverStatus.collectAsStateWithLifecycle()
    val playersMap by viewModel.players.collectAsStateWithLifecycle()
    val blips by viewModel.blips.collectAsStateWithLifecycle()
    val selectedPlayer by viewModel.selectedPlayer.collectAsStateWithLifecycle()
    val focusedPlayerId by viewModel.focusedPlayerId.collectAsStateWithLifecycle()
    val isFollowing by viewModel.isFollowing.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val logs by viewModel.connectionLogs.collectAsStateWithLifecycle()
    val isRestTesting by viewModel.isRestTesting.collectAsStateWithLifecycle()
    val restTestResult by viewModel.restTestResult.collectAsStateWithLifecycle()

    val playersList = playersMap.values.toList()

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            NavigationBar(
                containerColor = Slate900,
                tonalElevation = 8.dp
            ) {
                AppTab.values().forEach { tab ->
                    val selected = currentTab == tab
                    NavigationBarItem(
                        selected = selected,
                        onClick = { currentTab = tab },
                        icon = {
                            if (tab == AppTab.PLAYERS && playersList.isNotEmpty()) {
                                BadgedBox(badge = {
                                    Badge(
                                        containerColor = CyanNeon,
                                        contentColor = Slate950
                                    ) {
                                        Text("${playersList.size}", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    }
                                }) {
                                    Icon(imageVector = tab.icon, contentDescription = tab.title)
                                }
                            } else {
                                Icon(imageVector = tab.icon, contentDescription = tab.title)
                            }
                        },
                        label = {
                            Text(
                                text = tab.title,
                                fontSize = 11.sp,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Slate950,
                            unselectedIconColor = Slate400,
                            selectedTextColor = CyanNeon,
                            unselectedTextColor = Slate400,
                            indicatorColor = CyanNeon
                        ),
                        modifier = Modifier.testTag("nav_tab_${tab.name.lowercase()}")
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(Slate950)
        ) {
            when (currentTab) {
                AppTab.MAP -> {
                    MapScreen(
                        players = playersList,
                        blips = blips,
                        focusedPlayerId = focusedPlayerId,
                        isFollowing = isFollowing,
                        calibration = settings.calibration,
                        connectionState = connectionState,
                        onSelectPlayer = { player ->
                            viewModel.selectPlayer(player)
                        },
                        onStopFollowing = {
                            viewModel.clearFocus()
                        },
                        onOpenCalibration = {
                            currentTab = AppTab.SETTINGS
                        },
                        onConfigureServer = {
                            currentTab = AppTab.SETTINGS
                        },
                        onReconnect = {
                            viewModel.connect()
                        }
                    )
                }

                AppTab.PLAYERS -> {
                    PlayersScreen(
                        players = playersList,
                        focusedPlayerId = focusedPlayerId,
                        isFollowing = isFollowing,
                        connectionState = connectionState,
                        onSelectPlayer = { player ->
                            viewModel.selectPlayer(player)
                        },
                        onShowOnMap = { player ->
                            viewModel.focusPlayer(player.id, follow = false)
                            currentTab = AppTab.MAP
                        },
                        onToggleFollow = { player ->
                            val willFollow = !(focusedPlayerId == player.id && isFollowing)
                            viewModel.focusPlayer(player.id, follow = willFollow)
                            currentTab = AppTab.MAP
                        },
                        onConfigureServer = {
                            currentTab = AppTab.SETTINGS
                        },
                        onReconnect = {
                            viewModel.connect()
                        }
                    )
                }

                AppTab.STATUS -> {
                    StatusScreen(
                        serverStatus = serverStatus,
                        connectionState = connectionState,
                        logs = logs,
                        onlinePlayerCount = playersList.size,
                        onConfigureServer = {
                            currentTab = AppTab.SETTINGS
                        },
                        onReconnect = {
                            viewModel.connect()
                        },
                        onClearLogs = {
                            viewModel.clearLogs()
                        }
                    )
                }

                AppTab.SETTINGS -> {
                    SettingsScreen(
                        currentSettings = settings,
                        isTesting = isRestTesting,
                        testResult = restTestResult,
                        onSaveConnection = { restUrl, wsUrl, token ->
                            viewModel.saveConnectionSettings(restUrl, wsUrl, token)
                        },
                        onTestConnection = {
                            viewModel.testRestConnection()
                        },
                        onSaveCalibration = { minX, maxX, minY, maxY ->
                            viewModel.updateCalibration(minX, maxX, minY, maxY)
                        },
                        onResetCalibration = {
                            viewModel.resetCalibration()
                        }
                    )
                }
            }

            // Player Profile Bottom Sheet (opens on tapping marker or player profile)
            if (selectedPlayer != null) {
                PlayerProfileSheet(
                    player = selectedPlayer,
                    isFollowing = focusedPlayerId == selectedPlayer?.id && isFollowing,
                    onDismiss = {
                        viewModel.selectPlayer(null)
                    },
                    onShowOnMap = { player ->
                        viewModel.focusPlayer(player.id, follow = false)
                        currentTab = AppTab.MAP
                    },
                    onToggleFollow = { player ->
                        val willFollow = !(focusedPlayerId == player.id && isFollowing)
                        viewModel.focusPlayer(player.id, follow = willFollow)
                        currentTab = AppTab.MAP
                    }
                )
            }
        }
    }
}
