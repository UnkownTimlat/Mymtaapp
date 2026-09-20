package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FilterCenterFocus
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.model.ConnectionState
import com.example.data.model.MapCalibrationConfig
import com.example.data.model.MtaPlayer
import com.example.data.model.ServerBlip
import com.example.ui.components.ConnectionBanner
import com.example.ui.theme.AmberGta
import com.example.ui.theme.CyanNeon
import com.example.ui.theme.EmeraldLive
import com.example.ui.theme.RoseError
import com.example.ui.theme.Slate400
import com.example.ui.theme.Slate800
import com.example.ui.theme.Slate900
import com.example.ui.theme.Slate950
import java.util.Locale
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min

@Composable
fun MapScreen(
    players: List<MtaPlayer>,
    blips: List<ServerBlip>,
    focusedPlayerId: Int?,
    isFollowing: Boolean,
    calibration: MapCalibrationConfig,
    connectionState: ConnectionState,
    onSelectPlayer: (MtaPlayer) -> Unit,
    onStopFollowing: () -> Unit,
    onOpenCalibration: () -> Unit,
    onConfigureServer: () -> Unit,
    onReconnect: () -> Unit,
    modifier: Modifier = Modifier
) {
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }

    // When follow mode is active, center viewport on tracked player in real time
    val trackedPlayer = players.firstOrNull { it.id == focusedPlayerId }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(Slate950)
            .clipToBounds()
    ) {
        val containerWidth = constraints.maxWidth.toFloat()
        val containerHeight = constraints.maxHeight.toFloat()
        val mapSize = min(containerWidth, containerHeight)
        val density = LocalDensity.current
        val mapSizeDp = with(density) { mapSize.toDp() }
        val tagShiftXDp = with(density) { 40.dp.toPx() }
        val tagShiftYDp = with(density) { 32.dp.toPx() }

        // Center calculation
        LaunchedEffect(trackedPlayer?.x, trackedPlayer?.y, isFollowing, focusedPlayerId) {
            if (trackedPlayer != null && (isFollowing || focusedPlayerId != null)) {
                val (u, v) = calibration.worldToNormalized(trackedPlayer.x, trackedPlayer.y)
                val targetZoom = if (scale < 2.5f) 2.5f else scale
                scale = targetZoom

                // Target pixel position inside the map area
                val targetMapX = u * mapSize
                val targetMapY = v * mapSize

                // Compute offset so that targetMapX, targetMapY appears in screen center
                val newOffsetX = (containerWidth / 2f) - (targetMapX * scale)
                val newOffsetY = (containerHeight / 2f) - (targetMapY * scale)
                offset = Offset(newOffsetX, newOffsetY)
            }
        }

        // Interactive Map Canvas Area
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTransformGestures { _, pan, zoom, _ ->
                        scale = (scale * zoom).coerceIn(0.8f, 10f)
                        offset += pan
                    }
                }
                .pointerInput(players, scale, offset, mapSize) {
                    detectTapGestures { tapOffset ->
                        // Check if tap hit any player marker
                        val touchRadius = 24.dp.toPx()
                        var clickedPlayer: MtaPlayer? = null

                        for (player in players) {
                            val (u, v) = calibration.worldToNormalized(player.x, player.y)
                            val px = (u * mapSize * scale) + offset.x
                            val py = (v * mapSize * scale) + offset.y
                            val dist = hypot((tapOffset.x - px).toDouble(), (tapOffset.y - py).toDouble())
                            if (dist <= touchRadius) {
                                clickedPlayer = player
                                break
                            }
                        }

                        if (clickedPlayer != null) {
                            onSelectPlayer(clickedPlayer)
                        }
                    }
                }
        ) {
            // Map Image layer transformed
            Image(
                painter = painterResource(id = R.drawable.saworld),
                contentDescription = "GTA San Andreas Live World Map",
                modifier = Modifier
                    .size(mapSizeDp)
                    .graphicsLayer(
                        scaleX = scale,
                        scaleY = scale,
                        translationX = offset.x,
                        translationY = offset.y,
                        transformOrigin = androidx.compose.ui.graphics.TransformOrigin(0f, 0f)
                    ),
                contentScale = ContentScale.FillBounds
            )

            // Dynamic Overlay Canvas for Markers, Names, and Range Rings
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("mta_map_canvas")
            ) {
                // Draw server blips first (if any)
                for (blip in blips) {
                    val (bu, bv) = calibration.worldToNormalized(blip.x, blip.y)
                    val bx = (bu * mapSize * scale) + offset.x
                    val by = (bv * mapSize * scale) + offset.y

                    if (bx in -50f..(size.width + 50f) && by in -50f..(size.height + 50f)) {
                        drawCircle(
                            color = AmberGta.copy(alpha = 0.8f),
                            radius = 4.dp.toPx(),
                            center = Offset(bx, by)
                        )
                        drawCircle(
                            color = Color.White,
                            radius = 2.dp.toPx(),
                            center = Offset(bx, by)
                        )
                    }
                }

                // Draw Players
                for (player in players) {
                    val (u, v) = calibration.worldToNormalized(player.x, player.y)
                    val px = (u * mapSize * scale) + offset.x
                    val py = (v * mapSize * scale) + offset.y

                    // Only draw if within visible viewport bounds
                    if (px in -80f..(size.width + 80f) && py in -80f..(size.height + 80f)) {
                        val isTracked = player.id == focusedPlayerId

                        // Highlight ring for tracked player
                        if (isTracked) {
                            drawCircle(
                                color = AmberGta.copy(alpha = 0.35f),
                                radius = 22.dp.toPx(),
                                center = Offset(px, py)
                            )
                            drawCircle(
                                color = AmberGta,
                                radius = 16.dp.toPx(),
                                center = Offset(px, py),
                                style = Stroke(width = 2.dp.toPx())
                            )
                        }

                        // Outer pulsing halo
                        drawCircle(
                            color = if (isTracked) AmberGta.copy(alpha = 0.5f) else CyanNeon.copy(alpha = 0.35f),
                            radius = 10.dp.toPx(),
                            center = Offset(px, py)
                        )

                        // Core player radar dot
                        drawCircle(
                            color = if (isTracked) AmberGta else CyanNeon,
                            radius = 5.dp.toPx(),
                            center = Offset(px, py)
                        )
                        drawCircle(
                            color = Color.White,
                            radius = 2.5.dp.toPx(),
                            center = Offset(px, py)
                        )
                    }
                }
            }

            // Interactive player tag overlays positioned over the canvas
            for (player in players) {
                val (u, v) = calibration.worldToNormalized(player.x, player.y)
                val px = (u * mapSize * scale) + offset.x
                val py = (v * mapSize * scale) + offset.y

                if (px in 20f..(containerWidth - 20f) && py in 30f..(containerHeight - 30f)) {
                    val isTracked = player.id == focusedPlayerId
                    Box(
                        modifier = Modifier
                            .graphicsLayer(
                                translationX = px - tagShiftXDp,
                                translationY = py - tagShiftYDp
                            )
                            .clickable { onSelectPlayer(player) }
                            .clip(RoundedCornerShape(4.dp))
                            .background(if (isTracked) AmberGta.copy(alpha = 0.9f) else Slate900.copy(alpha = 0.85f))
                            .border(
                                width = 1.dp,
                                color = if (isTracked) Color.White else CyanNeon.copy(alpha = 0.6f),
                                shape = RoundedCornerShape(4.dp)
                            )
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "${player.name} [${player.id}]",
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isTracked) Slate950 else Color.White,
                            maxLines = 1
                        )
                    }
                }
            }
        }

        // Top Overlay: Status Banner & Online Players Count
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
        ) {
            ConnectionBanner(
                connectionState = connectionState,
                onConfigureClick = onConfigureServer,
                onReconnectClick = onReconnect
            )

            // Top Status Pill
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = Slate900.copy(alpha = 0.85f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Slate800)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(if (players.isNotEmpty()) EmeraldLive else Slate400)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "${players.size} PLAYERS LIVE",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = Slate900.copy(alpha = 0.85f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Slate800)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "ZOOM: ${String.format(Locale.US, "%.1f", scale)}x",
                            style = MaterialTheme.typography.labelSmall,
                            color = Slate400,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        }

        // Bottom Follow Status Pill (when tracking a real player)
        AnimatedVisibility(
            visible = trackedPlayer != null && isFollowing,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 80.dp)
        ) {
            if (trackedPlayer != null) {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Slate900.copy(alpha = 0.95f)),
                    border = androidx.compose.foundation.BorderStroke(1.dp, AmberGta),
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                        .testTag("tracking_player_pill")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Visibility,
                            contentDescription = null,
                            tint = AmberGta,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "TRACKING: ${trackedPlayer.name} [${trackedPlayer.id}]",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = AmberGta
                            )
                            Text(
                                text = "X: ${trackedPlayer.x.toInt()}  Y: ${trackedPlayer.y.toInt()}  Z: ${trackedPlayer.z.toInt()} • ${trackedPlayer.vehicle ?: "On Foot"}",
                                style = MaterialTheme.typography.bodySmall,
                                fontSize = 10.sp,
                                color = Slate400,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        IconButton(
                            onClick = onStopFollowing,
                            modifier = Modifier.size(28.dp).testTag("stop_following_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Stop following",
                                tint = Slate400,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }

        // Floating Action Controls: Recenter, Zoom+, Zoom-, Calibrate
        Column(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Recenter Map
            FilledIconButton(
                onClick = {
                    scale = 1f
                    offset = Offset(
                        (containerWidth - mapSize) / 2f,
                        (containerHeight - mapSize) / 2f
                    )
                },
                modifier = Modifier.size(44.dp).testTag("recenter_map_button"),
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = Slate900.copy(alpha = 0.9f),
                    contentColor = Color.White
                )
            ) {
                Icon(
                    imageVector = Icons.Default.FilterCenterFocus,
                    contentDescription = "Recenter Map",
                    modifier = Modifier.size(20.dp)
                )
            }

            // Zoom In
            FilledIconButton(
                onClick = {
                    val newScale = (scale * 1.4f).coerceAtMost(10f)
                    // Zoom towards center
                    val zoomFactor = newScale / scale
                    val cx = containerWidth / 2f
                    val cy = containerHeight / 2f
                    offset = Offset(
                        cx - (cx - offset.x) * zoomFactor,
                        cy - (cy - offset.y) * zoomFactor
                    )
                    scale = newScale
                },
                modifier = Modifier.size(44.dp).testTag("zoom_in_button"),
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = Slate900.copy(alpha = 0.9f),
                    contentColor = Color.White
                )
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Zoom In",
                    modifier = Modifier.size(20.dp)
                )
            }

            // Zoom Out
            FilledIconButton(
                onClick = {
                    val newScale = (scale / 1.4f).coerceAtLeast(0.8f)
                    val zoomFactor = newScale / scale
                    val cx = containerWidth / 2f
                    val cy = containerHeight / 2f
                    offset = Offset(
                        cx - (cx - offset.x) * zoomFactor,
                        cy - (cy - offset.y) * zoomFactor
                    )
                    scale = newScale
                },
                modifier = Modifier.size(44.dp).testTag("zoom_out_button"),
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = Slate900.copy(alpha = 0.9f),
                    contentColor = Color.White
                )
            ) {
                Icon(
                    imageVector = Icons.Default.Remove,
                    contentDescription = "Zoom Out",
                    modifier = Modifier.size(20.dp)
                )
            }

            // Coordinate Calibration button
            FilledIconButton(
                onClick = onOpenCalibration,
                modifier = Modifier.size(44.dp).testTag("calibrate_map_button"),
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = Slate900.copy(alpha = 0.9f),
                    contentColor = CyanNeon
                )
            ) {
                Icon(
                    imageVector = Icons.Default.Tune,
                    contentDescription = "Map Calibration",
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}
