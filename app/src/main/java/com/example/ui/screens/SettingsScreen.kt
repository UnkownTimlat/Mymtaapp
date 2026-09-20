package com.example.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CloudQueue
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.NetworkCheck
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.MapCalibrationConfig
import com.example.data.preferences.ServerSettings
import com.example.ui.theme.AmberGta
import com.example.ui.theme.CyanNeon
import com.example.ui.theme.EmeraldLive
import com.example.ui.theme.RoseError
import com.example.ui.theme.Slate400
import com.example.ui.theme.Slate700
import com.example.ui.theme.Slate800
import com.example.ui.theme.Slate900
import com.example.ui.theme.Slate950

@Composable
fun SettingsScreen(
    currentSettings: ServerSettings,
    isTesting: Boolean,
    testResult: String?,
    onSaveConnection: (restUrl: String, wsUrl: String, authToken: String) -> Unit,
    onTestConnection: () -> Unit,
    onSaveCalibration: (minX: Float, maxX: Float, minY: Float, maxY: Float) -> Unit,
    onResetCalibration: () -> Unit,
    modifier: Modifier = Modifier
) {
    var restUrl by remember(currentSettings.restBaseUrl) { mutableStateOf(currentSettings.restBaseUrl) }
    var wsUrl by remember(currentSettings.wsUrl) { mutableStateOf(currentSettings.wsUrl) }
    var authToken by remember(currentSettings.authToken) { mutableStateOf(currentSettings.authToken) }
    var showToken by remember { mutableStateOf(false) }

    var calMinX by remember(currentSettings.calibration.minX) { mutableStateOf(currentSettings.calibration.minX.toString()) }
    var calMaxX by remember(currentSettings.calibration.maxX) { mutableStateOf(currentSettings.calibration.maxX.toString()) }
    var calMinY by remember(currentSettings.calibration.minY) { mutableStateOf(currentSettings.calibration.minY.toString()) }
    var calMaxY by remember(currentSettings.calibration.maxY) { mutableStateOf(currentSettings.calibration.maxY.toString()) }

    var saveSuccessNotice by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Slate950)
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text(
            text = "SERVER & BACKEND CONFIGURATION",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
        Text(
            text = "Configure your MTA:SA backend gateway and real-time WebSocket protocol endpoints.",
            style = MaterialTheme.typography.bodySmall,
            color = Slate400
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Connection Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Slate900),
            shape = RoundedCornerShape(14.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, Slate800)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.Link, contentDescription = null, tint = CyanNeon, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Backend Gateway", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = Color.White)
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Quick Presets
                Text("QUICK PRESETS", style = MaterialTheme.typography.labelSmall, color = Slate400, fontSize = 10.sp)
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = restUrl.contains("22005"),
                        onClick = {
                            restUrl = "http://YOUR_OPTIKLINK_IP:22005/livemap/call/"
                            authToken = "SECURE_LIVE_MAP_TOKEN_2026"
                        },
                        label = { Text("Optiklink / MTA HTTP", fontSize = 11.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = EmeraldLive.copy(alpha = 0.2f),
                            selectedLabelColor = EmeraldLive
                        )
                    )
                    FilterChip(
                        selected = restUrl.contains("10.0.2.2"),
                        onClick = {
                            restUrl = "http://10.0.2.2:8080/"
                            wsUrl = "ws://10.0.2.2:8080/ws"
                        },
                        label = { Text("Emulator (10.0.2.2)", fontSize = 11.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = CyanNeon.copy(alpha = 0.2f),
                            selectedLabelColor = CyanNeon
                        )
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // REST Base URL input
                OutlinedTextField(
                    value = restUrl,
                    onValueChange = {
                        restUrl = it
                        saveSuccessNotice = false
                    },
                    label = { Text("REST Base URL (HTTPS / HTTP)") },
                    placeholder = { Text("https://YOUR_SERVER_API/") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("backend_rest_url_input"),
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = CyanNeon,
                        unfocusedBorderColor = Slate800,
                        focusedContainerColor = Slate950,
                        unfocusedContainerColor = Slate950,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    )
                )

                Spacer(modifier = Modifier.height(12.dp))

                // WebSocket URL input
                OutlinedTextField(
                    value = wsUrl,
                    onValueChange = {
                        wsUrl = it
                        saveSuccessNotice = false
                    },
                    label = { Text("WebSocket URL (WSS / WS)") },
                    placeholder = { Text("wss://YOUR_SERVER_API/ws") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("backend_ws_url_input"),
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = CyanNeon,
                        unfocusedBorderColor = Slate800,
                        focusedContainerColor = Slate950,
                        unfocusedContainerColor = Slate950,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    )
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Authentication Token
                OutlinedTextField(
                    value = authToken,
                    onValueChange = {
                        authToken = it
                        saveSuccessNotice = false
                    },
                    label = { Text("API Key / Bearer Token (Optional)") },
                    placeholder = { Text("Secret token for authorized access") },
                    singleLine = true,
                    visualTransformation = if (showToken) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { showToken = !showToken }) {
                            Icon(
                                imageVector = Icons.Default.Key,
                                contentDescription = "Toggle token visibility",
                                tint = if (showToken) CyanNeon else Slate400
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth().testTag("backend_auth_token_input"),
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = CyanNeon,
                        unfocusedBorderColor = Slate800,
                        focusedContainerColor = Slate950,
                        unfocusedContainerColor = Slate950,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    )
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Action Buttons: Test Connection & Save
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    FilledTonalButton(
                        onClick = onTestConnection,
                        enabled = !isTesting,
                        modifier = Modifier.weight(1f).height(44.dp).testTag("test_connection_button"),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        if (isTesting) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = CyanNeon)
                        } else {
                            Icon(imageVector = Icons.Default.NetworkCheck, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Test REST", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Button(
                        onClick = {
                            onSaveConnection(restUrl, wsUrl, authToken)
                            saveSuccessNotice = true
                        },
                        modifier = Modifier.weight(1f).height(44.dp).testTag("save_connection_button"),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = CyanNeon, contentColor = Slate950)
                    ) {
                        Icon(imageVector = Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Save & Connect", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }

                // Test Connection Result notice
                if (testResult != null) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        color = if (testResult.startsWith("Success", ignoreCase = true)) EmeraldLive.copy(alpha = 0.15f) else RoseError.copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = testResult,
                            modifier = Modifier.padding(10.dp),
                            style = MaterialTheme.typography.bodySmall,
                            fontFamily = FontFamily.Monospace,
                            color = if (testResult.startsWith("Success", ignoreCase = true)) EmeraldLive else RoseError
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Map Calibration Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Slate900),
            shape = RoundedCornerShape(14.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, Slate800)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.Tune, contentDescription = null, tint = AmberGta, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("GTA:SA Map Calibration", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = Color.White)
                    }

                    IconButton(
                        onClick = {
                            calMinX = "-3000.0"
                            calMaxX = "3000.0"
                            calMinY = "-3000.0"
                            calMaxY = "3000.0"
                            onResetCalibration()
                        },
                        modifier = Modifier.size(32.dp).testTag("reset_calibration_button")
                    ) {
                        Icon(imageVector = Icons.Default.RestartAlt, contentDescription = "Reset", tint = Slate400, modifier = Modifier.size(18.dp))
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Standard GTA San Andreas bounds span from -3000 to +3000. Adjust these values to calibrate map radar projection if your custom map has outer margins.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Slate400
                )

                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = calMinX,
                        onValueChange = { calMinX = it },
                        label = { Text("Min X (West)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = AmberGta,
                            unfocusedBorderColor = Slate800,
                            focusedContainerColor = Slate950,
                            unfocusedContainerColor = Slate950,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )
                    OutlinedTextField(
                        value = calMaxX,
                        onValueChange = { calMaxX = it },
                        label = { Text("Max X (East)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = AmberGta,
                            unfocusedBorderColor = Slate800,
                            focusedContainerColor = Slate950,
                            unfocusedContainerColor = Slate950,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = calMinY,
                        onValueChange = { calMinY = it },
                        label = { Text("Min Y (South)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = AmberGta,
                            unfocusedBorderColor = Slate800,
                            focusedContainerColor = Slate950,
                            unfocusedContainerColor = Slate950,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )
                    OutlinedTextField(
                        value = calMaxY,
                        onValueChange = { calMaxY = it },
                        label = { Text("Max Y (North)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = AmberGta,
                            unfocusedBorderColor = Slate800,
                            focusedContainerColor = Slate950,
                            unfocusedContainerColor = Slate950,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Button(
                    onClick = {
                        val minX = calMinX.toFloatOrNull() ?: -3000f
                        val maxX = calMaxX.toFloatOrNull() ?: 3000f
                        val minY = calMinY.toFloatOrNull() ?: -3000f
                        val maxY = calMaxY.toFloatOrNull() ?: 3000f
                        onSaveCalibration(minX, maxX, minY, maxY)
                    },
                    modifier = Modifier.fillMaxWidth().height(42.dp).testTag("save_calibration_button"),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Slate800, contentColor = AmberGta)
                ) {
                    Text("Apply Map Calibration", fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Architectural Pipeline Overview
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Slate900),
            shape = RoundedCornerShape(14.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, Slate800)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "MTA:SA INTEGRATION PIPELINE",
                    style = MaterialTheme.typography.labelSmall,
                    color = Slate400,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Android App (Kotlin / Jetpack Compose)\n    ↕  Direct REST (http://IP:22005/livemap/call/)\n    ↕  WSS Real-Time Telemetry & Blips\nMTA:SA Server Resource (livemap)\n    • Zero host OS dependencies (no VPS required)\n    • Compatible with Optiklink game hosting",
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = FontFamily.Monospace,
                    color = CyanNeon,
                    fontSize = 11.sp,
                    lineHeight = 18.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(80.dp))
    }
}
