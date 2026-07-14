package com.remoteaudiosync.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Computer
import androidx.compose.material.icons.outlined.Pin
import androidx.compose.material.icons.outlined.Terminal
import androidx.compose.material.icons.rounded.Cable
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.PowerSettingsNew
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.remoteaudiosync.network.ConnectionState
import com.remoteaudiosync.ui.NetworkViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NetworkScreen(
    onNavigateBack: () -> Unit,
    viewModel: NetworkViewModel = viewModel()
) {
    var ip by remember { mutableStateOf("") }
    var port by remember { mutableStateOf("8765") }
    var pin by remember { mutableStateOf("") }

    val connectionState by viewModel.connectionState.collectAsState()
    val logs by viewModel.logs.collectAsState()
    val pairingStatus by viewModel.pairingStatus.collectAsState()
    val isAuthenticated by viewModel.isAuthenticated.collectAsState()
    val isAudioOwner by viewModel.isAudioOwner.collectAsState()

    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    DetailScreenTemplate(title = "Local Pair Hub", onNavigateBack = onNavigateBack) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // High-End Header description
            item {
                Column(modifier = Modifier.padding(bottom = 8.dp)) {
                    Text(
                        text = "Local Pair",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Establish a secure, encrypted handshake connection between your handheld device and the primary Synco terminal client protocol.",
                        color = Color.Gray,
                        fontSize = 13.sp,
                        lineHeight = 18.sp
                    )
                }
            }

            // Status Header Badging
            item {
                ConnectionStatusBadge(state = connectionState, isAuthenticated = isAuthenticated)
            }

            // Step 1: Network Identity Config Card
            item {
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Rounded.Wifi,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(22.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = "Network Identity",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                            
                            // STEP 01 Tag
                            Surface(
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = "STEP 01",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Custom Styled IP Field
                        OutlinedTextField(
                            value = ip,
                            onValueChange = { ip = it },
                            label = { Text("DESKTOP IP ADDRESS", fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp) },
                            placeholder = { Text("e.g. 192.168.1.100") },
                            leadingIcon = { Icon(Icons.Outlined.Computer, contentDescription = null, tint = Color.Gray) },
                            modifier = Modifier.fillMaxWidth().testTag("ip_input_field"),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedContainerColor = Color(0x0AFFFFFF),
                                unfocusedContainerColor = Color.Transparent,
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = Color(0x22FFFFFF)
                            ),
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Uri,
                                imeAction = ImeAction.Next
                            ),
                            shape = RoundedCornerShape(14.dp)
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        // Custom Styled Port Field
                        OutlinedTextField(
                            value = port,
                            onValueChange = { port = it },
                            label = { Text("PORT", fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp) },
                            leadingIcon = { Icon(Icons.Default.Numbers, contentDescription = null, tint = Color.Gray) },
                            modifier = Modifier.fillMaxWidth().testTag("port_input_field"),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedContainerColor = Color(0x0AFFFFFF),
                                unfocusedContainerColor = Color.Transparent,
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = Color(0x22FFFFFF)
                            ),
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Number,
                                imeAction = ImeAction.Done
                            ),
                            keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                            shape = RoundedCornerShape(14.dp)
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        val isConnectedOrConnecting = connectionState is ConnectionState.Connected ||
                                connectionState is ConnectionState.Connecting ||
                                connectionState is ConnectionState.Reconnecting ||
                                connectionState is ConnectionState.WaitingForAck

                        if (isConnectedOrConnecting) {
                            Button(
                                onClick = {
                                    viewModel.disconnect()
                                    keyboardController?.hide()
                                    focusManager.clearFocus()
                                },
                                modifier = Modifier.fillMaxWidth().height(48.dp).testTag("disconnect_channel_button"),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0x33EF5350),
                                    contentColor = Color(0xFFEF5350)
                                ),
                                border = BorderStroke(1.dp, Color(0xFFEF5350)),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Rounded.PowerSettingsNew, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Disconnect Channel", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            }
                        } else {
                            Button(
                                onClick = {
                                    viewModel.connect(ip, port.toIntOrNull() ?: 8765)
                                    keyboardController?.hide()
                                    focusManager.clearFocus()
                                },
                                enabled = ip.isNotBlank(),
                                modifier = Modifier.fillMaxWidth().height(48.dp).testTag("connect_to_desktop_button"),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary,
                                    contentColor = Color.Black,
                                    disabledContainerColor = Color(0x11FFFFFF),
                                    disabledContentColor = Color.Gray
                                ),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Rounded.Link, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Connect to Desktop", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            }
                        }
                    }
                }
            }

            // Step 2: Handshake PIN Verification Card
            item {
                val isConnected = connectionState is ConnectionState.Connected
                val showHighlightedBorder = isConnected && !isAuthenticated
                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    border = BorderStroke(
                        width = if (showHighlightedBorder) 1.5.dp else 1.dp,
                        color = if (showHighlightedBorder) MaterialTheme.colorScheme.primary else Color(0x11FFFFFF)
                    )
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Security,
                                    contentDescription = null,
                                    tint = if (showHighlightedBorder) MaterialTheme.colorScheme.primary else Color.Gray,
                                    modifier = Modifier.size(22.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = "Security PIN",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                            
                            // STEP 02 Tag
                            Surface(
                                color = if (isConnected) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else Color(0x11FFFFFF),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = "STEP 02",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isConnected) MaterialTheme.colorScheme.primary else Color.Gray,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = "Enter the 6-digit access token displayed on your desktop console client window to establish safe session authentication.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray,
                            lineHeight = 18.sp
                        )

                        Spacer(modifier = Modifier.height(18.dp))

                        // Outlined PIN field
                        OutlinedTextField(
                            value = pin,
                            onValueChange = {
                                if (it.length <= 6) pin = it
                            },
                            enabled = isConnected && !isAuthenticated,
                            label = { Text("6-DIGIT ACCESS PIN", fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp) },
                            placeholder = { Text("e.g. 123456") },
                            leadingIcon = { Icon(Icons.Outlined.Pin, contentDescription = null, tint = Color.Gray) },
                            modifier = Modifier.fillMaxWidth().testTag("pin_input_field"),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedContainerColor = Color(0x0AFFFFFF),
                                unfocusedContainerColor = Color.Transparent,
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = Color(0x22FFFFFF)
                            ),
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Number,
                                imeAction = ImeAction.Done
                            ),
                            keyboardActions = KeyboardActions(onDone = {
                                focusManager.clearFocus()
                                if (pin.length == 6) {
                                    viewModel.initiatePairing(pin)
                                }
                            }),
                            shape = RoundedCornerShape(14.dp)
                        )

                        Spacer(modifier = Modifier.height(18.dp))

                        // Initialize Link Action button
                        Button(
                            onClick = {
                                viewModel.initiatePairing(pin)
                                keyboardController?.hide()
                                focusManager.clearFocus()
                            },
                            enabled = isConnected && !isAuthenticated && pin.length == 6,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF00A2E6), // Neon bright Cyan/Blue from bento
                                contentColor = Color.Black,
                                disabledContainerColor = Color(0x11FFFFFF),
                                disabledContentColor = Color.Gray
                            ),
                            modifier = Modifier.fillMaxWidth().height(48.dp).testTag("initiate_pairing_button"),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Initialize Link", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }

                        // Real pairing status indicator
                        if (pairingStatus.isNotBlank()) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Surface(
                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = pairingStatus,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Step 3: Protocol Metadata sheet (Matches premium mockup exactly)
            item {
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text(
                            text = "PROTOCOL METADATA",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color.Gray,
                            letterSpacing = 1.5.sp
                        )
                        Spacer(modifier = Modifier.height(16.dp))

                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            MetadataItemRow("Cipher Suite", "AES-GCM-256")
                            MetadataItemRow("Compression", "ZSTD-4")
                            MetadataItemRow("Latency Cap", "15ms")
                            MetadataItemRow("Auth Method", "ECDH384-HK")
                        }
                    }
                }
            }

            // Connection Terminal protocol logs Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0C0C0D)),
                    border = BorderStroke(1.dp, Color(0xFF1B1B1C))
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Outlined.Terminal,
                                    contentDescription = null,
                                    tint = Color(0xFF90A4AE),
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "HARDWARE PROTOCOL CONSOLE",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF90A4AE),
                                    letterSpacing = 1.sp
                                )
                            }
                            
                            TextButton(
                                onClick = { viewModel.clearLogs() },
                                colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFFEF5350)),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                modifier = Modifier.testTag("clear_logs_button")
                            ) {
                                Icon(Icons.Default.DeleteSweep, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Clear", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Terminal viewport
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFF040405))
                                .padding(12.dp)
                        ) {
                            if (logs.isEmpty()) {
                                Text(
                                    text = "Terminal idle. Establish connection links to inspect protocol handshakes...",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontFamily = FontFamily.Monospace,
                                    color = Color(0xFF455A64),
                                    lineHeight = 16.sp
                                )
                            } else {
                                LazyColumn(
                                    modifier = Modifier.fillMaxSize(),
                                    verticalArrangement = Arrangement.spacedBy(6.dp),
                                    reverseLayout = true
                                ) {
                                    items(logs.reversed()) { log ->
                                        Text(
                                            text = log,
                                            style = MaterialTheme.typography.bodySmall,
                                            fontFamily = FontFamily.Monospace,
                                            lineHeight = 16.sp,
                                            color = when {
                                                log.contains("Failed") || log.contains("Error") -> Color(0xFFEF5350)
                                                log.contains("RC: TX") || log.contains("RC: RX") -> Color(0xFF81C784)
                                                log.contains("WS: ") -> Color(0xFF64B5F6)
                                                else -> Color(0xFFECEFF1)
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MetadataItemRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, color = Color.Gray, fontSize = 13.sp)
        Text(text = value, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
    }
}

@Composable
fun ConnectionStatusBadge(state: ConnectionState, isAuthenticated: Boolean) {
    val bgColor: Color
    val textColor: Color
    val text: String
    val icon: ImageVector

    when {
        isAuthenticated -> {
            bgColor = Color(0x2281C784)
            textColor = Color(0xFF81C784)
            text = "Securely Paired & Authenticated"
            icon = Icons.Default.VerifiedUser
        }
        state is ConnectionState.Connected -> {
            bgColor = Color(0x2264B5F6)
            textColor = Color(0xFF64B5F6)
            text = "TCP Connected (Handshake Required)"
            icon = Icons.Default.CheckCircle
        }
        state is ConnectionState.Connecting || state is ConnectionState.Reconnecting || state is ConnectionState.WaitingForAck -> {
            bgColor = Color(0x22FFB300)
            textColor = Color(0xFFFFB300)
            text = if (state is ConnectionState.Connecting) "Connecting to Server..." else "Reconnecting..."
            icon = Icons.Default.Sync
        }
        state is ConnectionState.Failed -> {
            bgColor = Color(0x22EF5350)
            textColor = Color(0xFFEF5350)
            text = "Connection Failed: ${(state as? ConnectionState.Failed)?.error ?: "Check host IP / Port"}"
            icon = Icons.Default.Error
        }
        state is ConnectionState.Lost -> {
            bgColor = Color(0x22EF5350)
            textColor = Color(0xFFEF5350)
            text = "Connection Lost"
            icon = Icons.Default.SignalWifiOff
        }
        else -> {
            bgColor = Color(0x11FFFFFF)
            textColor = Color.LightGray
            text = "Disconnected"
            icon = Icons.Default.PowerOff
        }
    }

    Surface(
        color = bgColor,
        contentColor = textColor,
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, textColor.copy(alpha = 0.2f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = text,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp
            )
        }
    }
}
