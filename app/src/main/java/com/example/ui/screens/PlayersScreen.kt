package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationSearching
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ConnectionState
import com.example.data.model.MtaPlayer
import com.example.ui.components.ConnectionBanner
import com.example.ui.theme.AmberGta
import com.example.ui.theme.CyanNeon
import com.example.ui.theme.EmeraldLive
import com.example.ui.theme.RoseError
import com.example.ui.theme.Slate400
import com.example.ui.theme.Slate700
import com.example.ui.theme.Slate800
import com.example.ui.theme.Slate900
import com.example.ui.theme.Slate950
import com.example.ui.theme.VioletAdmin
import java.text.NumberFormat
import java.util.Locale

@Composable
fun PlayersScreen(
    players: List<MtaPlayer>,
    focusedPlayerId: Int?,
    isFollowing: Boolean,
    connectionState: ConnectionState,
    onSelectPlayer: (MtaPlayer) -> Unit,
    onShowOnMap: (MtaPlayer) -> Unit,
    onToggleFollow: (MtaPlayer) -> Unit,
    onConfigureServer: () -> Unit,
    onReconnect: () -> Unit,
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }

    val filteredPlayers = remember(players, searchQuery) {
        if (searchQuery.isBlank()) {
            players
        } else {
            val q = searchQuery.trim().lowercase(Locale.getDefault())
            players.filter {
                it.name.lowercase(Locale.getDefault()).contains(q) ||
                it.id.toString() == q ||
                it.vehicle?.lowercase(Locale.getDefault())?.contains(q) == true ||
                it.rank?.lowercase(Locale.getDefault())?.contains(q) == true
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Slate950)
    ) {
        // Connection Banner
        ConnectionBanner(
            connectionState = connectionState,
            onConfigureClick = onConfigureServer,
            onReconnectClick = onReconnect
        )

        // Header & Search Bar
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "ONLINE PLAYERS",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Slate800
                ) {
                    Text(
                        text = "${filteredPlayers.size} ACTIVE",
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = CyanNeon,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Search input field
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("player_search_input"),
                placeholder = { Text("Search by name, ID, rank, or vehicle...", color = Slate400, fontSize = 13.sp) },
                leadingIcon = {
                    Icon(imageVector = Icons.Default.Search, contentDescription = null, tint = Slate400)
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(imageVector = Icons.Default.Clear, contentDescription = "Clear", tint = Slate400)
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = CyanNeon,
                    unfocusedBorderColor = Slate800,
                    focusedContainerColor = Slate900,
                    unfocusedContainerColor = Slate900,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                )
            )
        }

        // Empty States or Online Players List
        if (players.isEmpty()) {
            EmptyPlayersState(
                connectionState = connectionState,
                onConfigureServer = onConfigureServer
            )
        } else if (filteredPlayers.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No players matched '$searchQuery'",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Slate400
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(filteredPlayers, key = { it.id }) { player ->
                    PlayerCard(
                        player = player,
                        isTracked = player.id == focusedPlayerId && isFollowing,
                        onCardClick = { onSelectPlayer(player) },
                        onShowOnMap = { onShowOnMap(player) },
                        onToggleFollow = { onToggleFollow(player) }
                    )
                }
                item {
                    Spacer(modifier = Modifier.height(80.dp))
                }
            }
        }
    }
}

@Composable
private fun PlayerCard(
    player: MtaPlayer,
    isTracked: Boolean,
    onCardClick: () -> Unit,
    onShowOnMap: () -> Unit,
    onToggleFollow: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCardClick() }
            .testTag("player_card_${player.id}"),
        colors = CardDefaults.cardColors(containerColor = Slate900),
        shape = RoundedCornerShape(14.dp),
        border = androidx.compose.foundation.BorderStroke(
            width = if (isTracked) 1.5.dp else 1.dp,
            color = if (isTracked) AmberGta else Slate800
        )
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Top Row: Name, ID, Rank, Ping
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(if (isTracked) AmberGta else CyanNeon)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = player.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = Slate800
                    ) {
                        Text(
                            text = "#${player.id}",
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = CyanNeon,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }

                val rankColor = when {
                    player.rank?.contains("Admin", ignoreCase = true) == true -> VioletAdmin
                    player.rank?.contains("VIP", ignoreCase = true) == true -> AmberGta
                    else -> Slate400
                }
                Text(
                    text = player.rank ?: "Player",
                    style = MaterialTheme.typography.labelSmall,
                    color = rankColor,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Vital bars: Health & Armor
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Health Bar
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("HP", style = MaterialTheme.typography.labelSmall, color = Slate400, fontSize = 10.sp)
                        Text("${player.health.toInt()}%", style = MaterialTheme.typography.labelSmall, color = RoseError, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(3.dp))
                    LinearProgressIndicator(
                        progress = { (player.health / 100f).coerceIn(0f, 1f) },
                        modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)),
                        color = RoseError,
                        trackColor = Slate800
                    )
                }

                // Armor Bar
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("ARMOR", style = MaterialTheme.typography.labelSmall, color = Slate400, fontSize = 10.sp)
                        Text("${player.armor.toInt()}%", style = MaterialTheme.typography.labelSmall, color = CyanNeon, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(3.dp))
                    LinearProgressIndicator(
                        progress = { (player.armor / 100f).coerceIn(0f, 1f) },
                        modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)),
                        color = CyanNeon,
                        trackColor = Slate800
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Vehicle & Money Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.DirectionsCar,
                        contentDescription = null,
                        tint = AmberGta,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = player.vehicle ?: "On Foot",
                        style = MaterialTheme.typography.bodySmall,
                        color = Slate400,
                        fontSize = 12.sp
                    )
                }

                val formattedMoney = NumberFormat.getCurrencyInstance(Locale.US).format(player.money)
                Text(
                    text = formattedMoney,
                    style = MaterialTheme.typography.bodySmall,
                    color = EmeraldLive,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilledTonalButton(
                    onClick = onShowOnMap,
                    modifier = Modifier.weight(1f).height(38.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = Slate800,
                        contentColor = CyanNeon
                    )
                ) {
                    Icon(imageVector = Icons.Default.Explore, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("MAP", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = onToggleFollow,
                    modifier = Modifier.weight(1f).height(38.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isTracked) AmberGta else Slate800,
                        contentColor = if (isTracked) Slate950 else Color.White
                    )
                ) {
                    Icon(
                        imageVector = if (isTracked) Icons.Default.Visibility else Icons.Default.LocationSearching,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(if (isTracked) "TRACKING" else "FOLLOW", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }

                OutlinedButton(
                    onClick = onCardClick,
                    modifier = Modifier.height(38.dp),
                    shape = RoundedCornerShape(8.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Slate700)
                ) {
                    Icon(imageVector = Icons.Default.Info, contentDescription = "View Profile", tint = Slate400, modifier = Modifier.size(14.dp))
                }
            }
        }
    }
}

@Composable
private fun EmptyPlayersState(
    connectionState: ConnectionState,
    onConfigureServer: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            Surface(
                modifier = Modifier.size(72.dp),
                shape = CircleShape,
                color = Slate900,
                border = androidx.compose.foundation.BorderStroke(1.dp, Slate800)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = if (connectionState is ConnectionState.Connected || connectionState is ConnectionState.Authenticated) {
                            Icons.Default.Person
                        } else {
                            Icons.Default.WifiOff
                        },
                        contentDescription = null,
                        tint = Slate400,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            val title = if (connectionState is ConnectionState.Connected || connectionState is ConnectionState.Authenticated) {
                "0 Players Currently Online"
            } else {
                "Backend Not Connected"
            }

            val desc = if (connectionState is ConnectionState.Connected || connectionState is ConnectionState.Authenticated) {
                "Connected to MTA server, but no players are currently in-game. Live player sync will automatically display players as they join."
            } else {
                "The app is awaiting a connection to your MTA:SA server backend. Configure the backend URL to sync live players and map coordinates."
            }

            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = desc,
                style = MaterialTheme.typography.bodySmall,
                color = Slate400,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            if (connectionState !is ConnectionState.Connected && connectionState !is ConnectionState.Authenticated) {
                Spacer(modifier = Modifier.height(20.dp))
                Button(
                    onClick = onConfigureServer,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = CyanNeon, contentColor = Slate950),
                    modifier = Modifier.testTag("empty_state_configure_button")
                ) {
                    Icon(imageVector = Icons.Default.Settings, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Configure Backend URL", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
