package com.remoteaudiosync.ui.screens

import android.content.Intent
import android.provider.Settings
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.remoteaudiosync.ui.NetworkViewModel

// Premium Obsidian Theme Glass Container Card
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    border: BorderStroke = BorderStroke(1.dp, Color(0x11FFFFFF)),
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    if (onClick != null) {
        Card(
            modifier = modifier
                .clip(RoundedCornerShape(24.dp))
                .clickable { onClick() },
            colors = CardDefaults.cardColors(containerColor = Color(0x11201F20)),
            shape = RoundedCornerShape(24.dp),
            border = border,
            content = content
        )
    } else {
        Card(
            modifier = modifier,
            colors = CardDefaults.cardColors(containerColor = Color(0x11201F20)),
            shape = RoundedCornerShape(24.dp),
            border = border,
            content = content
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: NetworkViewModel,
    onNavigateToPairing: () -> Unit = {},
    onNavigateToMedia: () -> Unit = {},
    onNavigateToCalls: () -> Unit = {},
    onNavigateToSettings: () -> Unit = {},
    onNavigateToDiagnostics: () -> Unit = {},
    onNavigateToPermissions: () -> Unit = {}
) {
    val connectionState by viewModel.connectionState.collectAsState()
    val isAuthenticated by viewModel.isAuthenticated.collectAsState()
    val isAudioOwner by viewModel.isAudioOwner.collectAsState()
    val desktopInfo by viewModel.desktopDeviceInfo.collectAsState()
    val activeAudioDevice by viewModel.activeAudioDevice.collectAsState()
    val hasPhonePermission by viewModel.hasPhonePermission.collectAsState()
    val hasNotificationPermission by viewModel.hasNotificationPermission.collectAsState()
    val mediaState by viewModel.mediaManager.mediaState.collectAsState()
    val artworkBytes by viewModel.artworkManager.currentArtwork.collectAsState()
    val profileName by viewModel.profileName.collectAsState()

    val avatarText = remember(profileName) {
        if (profileName.isBlank()) {
            ""
        } else {
            val parts = profileName.trim().split("\\s+".toRegex())
            if (parts.size >= 2) {
                (parts[0].take(1) + parts[1].take(1)).uppercase()
            } else if (parts.isNotEmpty()) {
                parts[0].take(2).uppercase()
            } else {
                ""
            }
        }
    }

    val imageBitmap = remember(artworkBytes) {
        artworkBytes?.let {
            try {
                val bitmap = android.graphics.BitmapFactory.decodeByteArray(it, 0, it.size)
                bitmap?.asImageBitmap()
            } catch (e: Exception) {
                null
            }
        }
    }

    Scaffold(
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Branding with purple neon sync icon
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable { /* Refresh or details */ }
                ) {
                    Icon(
                        imageVector = Icons.Default.Sync,
                        contentDescription = "Sync",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Synco",
                        fontWeight = FontWeight.Bold,
                        fontSize = 24.sp,
                        letterSpacing = (-0.5).sp,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = ".",
                        fontWeight = FontWeight.Bold,
                        fontSize = 24.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                // Header actions: Connection Status Chip and Profile Avatar
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    StatusChip(
                        text = if (isAuthenticated) "Connected" else if (connectionState is com.remoteaudiosync.network.ConnectionState.Connecting) "Connecting..." else "Disconnected",
                        isConnected = isAuthenticated
                    )
                    
                    // Executive profile avatar with cinematic soft-glowing violet ring (Perfect Circle)
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF0E0E0F))
                            .border(
                                width = 1.5.dp,
                                brush = Brush.linearGradient(
                                    colors = listOf(
                                        MaterialTheme.colorScheme.primary,
                                        MaterialTheme.colorScheme.secondary
                                    )
                                ),
                                shape = CircleShape
                            )
                            .clickable { onNavigateToSettings() },
                        contentAlignment = Alignment.Center
                    ) {
                        if (avatarText.isNotEmpty()) {
                            Text(
                                text = avatarText,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = "Profile Settings",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        },
        containerColor = Color(0xFF0A0A0B) // Luxurious Pitch Obsidian Background
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Hero Section: Synco Link
            item {
                TopHeroCard(
                    androidModel = android.os.Build.MODEL ?: "Android Phone",
                    desktopModel = desktopInfo?.model,
                    desktopOs = desktopInfo?.osVersion,
                    isConnected = isAuthenticated,
                    isConnecting = connectionState is com.remoteaudiosync.network.ConnectionState.Connecting
                )
            }

            // Audio Routing Mode Swapper Card
            item {
                AudioRoutingCard(
                    activeAudioDevice = activeAudioDevice,
                    isAudioOwner = isAudioOwner,
                    onSwitchRole = { viewModel.requestRole(it) }
                )
            }

            // Bento Grid: Bento Quick Actions (Left) and Now Playing Card (Right)
            item {
                Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
                    // Quick Bento Navigation Grid
                    QuickActions(
                        onPair = onNavigateToPairing,
                        onMedia = onNavigateToMedia,
                        onCalls = onNavigateToCalls,
                        onDiagnostics = onNavigateToDiagnostics,
                        onPermissions = onNavigateToPermissions
                    )

                    // Now Syncing Playback Card
                    CurrentMediaCard(
                        mediaState = mediaState,
                        imageBitmap = imageBitmap,
                        onMediaCommand = { viewModel.mediaManager.sendCommand(it) }
                    )
                }
            }

            // Connectivity Diagnostics Chart / Stability analytics
            item {
                DiagnosticsCard(
                    connectionState = connectionState,
                    isAuthenticated = isAuthenticated,
                    hasPhonePermission = hasPhonePermission,
                    hasNotificationPermission = hasNotificationPermission
                )
            }
            
            // Spacer to keep spacing clean at bottom
            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
fun StatusChip(text: String, isConnected: Boolean) {
    Row(
        modifier = Modifier
            .clip(CircleShape)
            .background(Color(0x22201F20))
            .border(1.dp, Color(0x11FFFFFF), CircleShape)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(if (isConnected) Color(0xFF81C784) else Color(0xFFEF5350))
                .drawBehind {
                    drawCircle(
                        color = if (isConnected) Color(0xFF81C784).copy(alpha = 0.4f) else Color(0xFFEF5350).copy(alpha = 0.4f),
                        radius = size.minDimension * 1.5f
                    )
                }
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = text,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = if (isConnected) Color(0xFF81C784) else Color(0xFFE5E2E3)
        )
    }
}

@Composable
fun TopHeroCard(
    androidModel: String,
    desktopModel: String?,
    desktopOs: String?,
    isConnected: Boolean,
    isConnecting: Boolean
) {
    val infiniteTransition = rememberInfiniteTransition()
    
    // Floating animations for host and output pods
    val floatOffsetLeft by infiniteTransition.animateFloat(
        initialValue = -6f,
        targetValue = 6f,
        animationSpec = infiniteRepeatable(
            animation = tween(2500, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        )
    )
    val floatOffsetRight by infiniteTransition.animateFloat(
        initialValue = 6f,
        targetValue = -6f,
        animationSpec = infiniteRepeatable(
            animation = tween(2500, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        )
    )

    // Glowing connection wave particle flow animation
    val connectionFlow by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        )
    )

    GlassCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "LIVE SESSION",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        letterSpacing = 2.sp
                    )
                    Text(
                        text = "Synco Link",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
                
                Surface(
                    color = if (isConnected) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else Color(0x11FFFFFF),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = if (isConnected) "Secure Connection" else if (isConnecting) "Awaiting handshakes" else "Offline Bridge",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isConnected) MaterialTheme.colorScheme.primary else Color.Gray,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(36.dp))

            // Dual Node visualization with glowing connecting stream
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Host Pod (Phone)
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.offset(y = floatOffsetLeft.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(90.dp)
                            .clip(CircleShape)
                            .background(Color(0x33201F20))
                            .border(
                                1.5.dp,
                                Brush.radialGradient(
                                    colors = listOf(
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                                        Color.Transparent
                                    )
                                ),
                                CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF131314)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Smartphone,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = androidModel,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "Host Device",
                        fontSize = 11.sp,
                        color = Color.Gray
                    )
                }

                // Streaming Connection Indicator
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(30.dp)
                        .padding(horizontal = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    // Dashed connecting line
                    Canvas(modifier = Modifier.fillMaxWidth().height(2.dp)) {
                        val width = size.width
                        drawLine(
                            color = Color(0x33FFFFFF),
                            start = androidx.compose.ui.geometry.Offset(0f, size.height / 2),
                            end = androidx.compose.ui.geometry.Offset(width, size.height / 2),
                            strokeWidth = 2.dp.toPx(),
                            pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(
                                floatArrayOf(15f, 15f),
                                0f
                            )
                        )
                    }

                    // Floating glowing particle traveling along the stream
                    if (isConnected) {
                        Canvas(modifier = Modifier.fillMaxWidth().height(16.dp)) {
                            val width = size.width
                            val positionX = width * connectionFlow
                            drawCircle(
                                color = Color(0xFFD2BBFF),
                                radius = 5.dp.toPx(),
                                center = androidx.compose.ui.geometry.Offset(positionX, size.height / 2)
                            )
                        }
                    }

                    Surface(
                        modifier = Modifier.offset(y = (-14).dp),
                        color = Color(0xFF201F20),
                        border = BorderStroke(1.dp, Color(0x22FFFFFF)),
                        shape = CircleShape
                    ) {
                        Text(
                            text = if (isConnected) "Active Link" else "Disconnected",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isConnected) MaterialTheme.colorScheme.primary else Color.Gray,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                }

                // Output Pod (Desktop Connection)
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.offset(y = floatOffsetRight.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(90.dp)
                            .clip(CircleShape)
                            .background(Color(0x33201F20))
                            .border(
                                1.5.dp,
                                Brush.radialGradient(
                                    colors = listOf(
                                        if (isConnected) MaterialTheme.colorScheme.secondary.copy(alpha = 0.8f) else Color.Transparent,
                                        Color.Transparent
                                    )
                                ),
                                CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF131314)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Computer,
                                contentDescription = null,
                                tint = if (isConnected) MaterialTheme.colorScheme.secondary else Color.Gray,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = desktopModel ?: "Awaiting Hub",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = if (desktopOs != null) "OS: $desktopOs" else "Target Server",
                        fontSize = 11.sp,
                        color = Color.Gray
                    )
                }
            }
        }
    }
}

@Composable
fun AudioRoutingCard(
    activeAudioDevice: String,
    isAudioOwner: Boolean,
    onSwitchRole: (Boolean) -> Unit
) {
    GlassCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(54.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Headset,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(
                        text = "AUDIO OUTPUT DEV",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.Gray,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = activeAudioDevice.ifBlank { "System Default Output" },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = if (isAudioOwner) "Broadcasting Phone Audio" else "Remote Controlling Server Output",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
                    )
                }
            }

            Switch(
                checked = isAudioOwner,
                onCheckedChange = onSwitchRole,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color(0xFF0E0E0F),
                    checkedTrackColor = MaterialTheme.colorScheme.primary,
                    uncheckedThumbColor = Color.Gray,
                    uncheckedTrackColor = Color(0xFF201F20)
                ),
                modifier = Modifier.testTag("audio_owner_switch")
            )
        }
    }
}

@Composable
fun CurrentMediaCard(
    mediaState: com.remoteaudiosync.protocol.MediaStatePayload?,
    imageBitmap: androidx.compose.ui.graphics.ImageBitmap?,
    onMediaCommand: (String) -> Unit
) {
    GlassCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "NOW SYNCING",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.secondary,
                    letterSpacing = 1.5.sp
                )
                
                Surface(
                    color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "Hi-Fi Lossless",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            if (mediaState != null) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Album Artwork Card
                    Card(
                        modifier = Modifier
                            .size(80.dp)
                            .clip(RoundedCornerShape(16.dp)),
                        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color(0xFF131314)),
                            contentAlignment = Alignment.Center
                        ) {
                            if (imageBitmap != null) {
                                androidx.compose.foundation.Image(
                                    bitmap = imageBitmap,
                                    contentDescription = "Artwork",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.MusicNote,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.secondary,
                                    modifier = Modifier.size(36.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.width(20.dp))

                    Column(
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = mediaState.title.ifBlank { "Stellaris Ambient" },
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            maxLines = 1
                        )
                        Text(
                            text = mediaState.artist.ifBlank { "Deep Space Harmony" },
                            fontSize = 14.sp,
                            color = Color.Gray,
                            maxLines = 1
                        )
                        
                        if (!mediaState.appName.isNullOrBlank()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Surface(
                                color = Color(0x33FFFFFF),
                                shape = CircleShape
                            ) {
                                Text(
                                    text = mediaState.appName,
                                    fontSize = 10.sp,
                                    color = Color.White,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Interactive Progress Bar
                if (mediaState.duration > 0) {
                    val progress = mediaState.position.toFloat() / mediaState.duration.toFloat()
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        LinearProgressIndicator(
                            progress = { progress.coerceIn(0f, 1f) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(CircleShape),
                            color = MaterialTheme.colorScheme.secondary,
                            trackColor = Color(0xFF201F20)
                        )
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = formatDuration(mediaState.position),
                                fontSize = 11.sp,
                                color = Color.Gray
                            )
                            Text(
                                text = formatDuration(mediaState.duration),
                                fontSize = 11.sp,
                                color = Color.Gray
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // High End Playback Actions Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { onMediaCommand("PREVIOUS") },
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.SkipPrevious,
                            contentDescription = "Prev",
                            tint = Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(32.dp))

                    val isPlaying = mediaState.isPlaying
                    IconButton(
                        onClick = { onMediaCommand(if (isPlaying) "PAUSE" else "PLAY") },
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.secondary)
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = "PlayPause",
                            tint = Color.Black,
                            modifier = Modifier.size(32.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(32.dp))

                    IconButton(
                        onClick = { onMediaCommand("NEXT") },
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.SkipNext,
                            contentDescription = "Next",
                            tint = Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
            } else {
                // Empty state for Media Sync
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.MusicOff,
                        contentDescription = null,
                        tint = Color.Gray,
                        modifier = Modifier.size(42.dp)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(
                            "Ready to stream",
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            fontSize = 16.sp
                        )
                        Text(
                            "Handshake active. Start audio playback to sync controls.",
                            color = Color.Gray,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }
    }
}

private fun formatDuration(ms: Long): String {
    val sec = (ms / 1000) % 60
    val min = (ms / (1000 * 60)) % 60
    return String.format("%02d:%02d", min, sec)
}

@Composable
fun DiagnosticsCard(
    connectionState: com.remoteaudiosync.network.ConnectionState,
    isAuthenticated: Boolean,
    hasPhonePermission: Boolean,
    hasNotificationPermission: Boolean
) {
    val infiniteTransition = rememberInfiniteTransition()

    GlassCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
        ) {
            Text(
                text = "SYSTEM METRIC DIAGNOSTICS",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                letterSpacing = 1.5.sp
            )
            Spacer(modifier = Modifier.height(20.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(24.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Real-time animated futuristic equalizer/stability indicator
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = "Connection Stability",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    
                    Row(
                        modifier = Modifier.height(36.dp).fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.Bottom
                    ) {
                        val bars = listOf(0.4f, 0.6f, 0.5f, 0.8f, 0.6f, 0.9f, 0.5f, 0.7f, 0.4f, 0.8f, 0.3f, 0.5f)
                        bars.forEachIndexed { index, baseHeight ->
                            val heightMultiplier by infiniteTransition.animateFloat(
                                initialValue = 0.7f,
                                targetValue = 1.2f,
                                animationSpec = infiniteRepeatable(
                                    animation = tween((1200 + index * 150), easing = EaseInOutSine),
                                    repeatMode = RepeatMode.Reverse
                                )
                            )
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight((baseHeight * heightMultiplier).coerceIn(0.15f, 1f))
                                    .clip(RoundedCornerShape(topStart = 2.dp, topEnd = 2.dp))
                                    .background(
                                        Brush.verticalGradient(
                                            colors = listOf(
                                                MaterialTheme.colorScheme.primary,
                                                MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                                            )
                                        )
                                    )
                            )
                        }
                    }
                }

                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(64.dp)
                        .background(Color(0x22FFFFFF))
                )

                // Stats Section
                Row(
                    horizontalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = "LATENCY", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                        Text(
                            text = if (isAuthenticated) "12ms" else "--",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = "BITRATE", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                        Text(
                            text = if (isAuthenticated) "2.4 mbps" else "--",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
            HorizontalDivider(color = Color(0x11FFFFFF))
            Spacer(modifier = Modifier.height(16.dp))

            // Diagnostic Status Rows
            val channelStatus = if (isAuthenticated) "OK" else if (connectionState is com.remoteaudiosync.network.ConnectionState.Connecting) "Connecting" else "Disconnected"
            DiagnosticsRow(label = "WebSocket Channel", state = channelStatus, isWarning = !isAuthenticated)
            Spacer(modifier = Modifier.height(10.dp))
            DiagnosticsRow(label = "Notification Reader", state = if (hasNotificationPermission) "Granted" else "Missing", isWarning = !hasNotificationPermission)
            Spacer(modifier = Modifier.height(10.dp))
            DiagnosticsRow(label = "Phone State Access", state = if (hasPhonePermission) "Granted" else "Missing", isWarning = !hasPhonePermission)
        }
    }
}

@Composable
fun DiagnosticsRow(label: String, state: String, isWarning: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = if (isWarning) Icons.Default.Warning else Icons.Default.CheckCircle,
                contentDescription = null,
                tint = if (isWarning) Color(0xFFEF5350) else Color(0xFF81C784),
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = label, fontSize = 13.sp, color = Color.LightGray)
        }
        Text(
            text = state,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = if (isWarning) Color(0xFFEF5350) else Color(0xFF81C784)
        )
    }
}

@Composable
fun QuickActions(
    onPair: () -> Unit,
    onMedia: () -> Unit,
    onCalls: () -> Unit,
    onDiagnostics: () -> Unit,
    onPermissions: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = "QUICK ACTIONS BENTO",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            letterSpacing = 1.5.sp,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        // Bento grid of actions
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            BentoButton(
                label = "Pair Device",
                icon = Icons.Default.AddLink,
                backgroundColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f),
                tintColor = MaterialTheme.colorScheme.primary,
                onClick = onPair,
                modifier = Modifier.weight(1f).testTag("bento_pair_button")
            )
            BentoButton(
                label = "Media Sync",
                icon = Icons.Default.Tune,
                backgroundColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.2f),
                tintColor = MaterialTheme.colorScheme.secondary,
                onClick = onMedia,
                modifier = Modifier.weight(1f).testTag("bento_media_button")
            )
        }
        
        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            BentoButton(
                label = "Permissions",
                icon = Icons.Default.Security,
                backgroundColor = Color(0x1AFFFFFF),
                tintColor = Color.White,
                onClick = onPermissions,
                modifier = Modifier.weight(1f).testTag("bento_permissions_button")
            )
            BentoButton(
                label = "Diagnostics",
                icon = Icons.Default.Analytics,
                backgroundColor = Color(0x1AFFFFFF),
                tintColor = Color.White,
                onClick = onDiagnostics,
                modifier = Modifier.weight(1f).testTag("bento_diagnostics_button")
            )
        }
    }
}

@Composable
fun BentoButton(
    label: String,
    icon: ImageVector,
    backgroundColor: Color,
    tintColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    GlassCard(
        modifier = modifier.height(100.dp),
        onClick = onClick
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(backgroundColor),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = tintColor,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = label,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White
            )
        }
    }
}

@Composable
fun DetailScreenTemplate(
    title: String,
    onNavigateBack: () -> Unit,
    content: @Composable (PaddingValues) -> Unit
) {
    Scaffold(
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onNavigateBack,
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    color = Color.White
                )
            }
        },
        containerColor = Color(0xFF0A0A0B)
    ) { padding ->
        content(padding)
    }
}

@Composable
fun PairingScreen(onNavigateBack: () -> Unit = {}) {
    DetailScreenTemplate(title = "Pairing", onNavigateBack = onNavigateBack) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
            Text(
                text = "Awaiting Bluetooth & Local Discoverability",
                style = MaterialTheme.typography.bodyLarge,
                color = Color.LightGray
            )
        }
    }
}

@Composable
fun MediaScreen(
    onNavigateBack: () -> Unit = {},
    viewModel: NetworkViewModel
) {
    val mediaState by viewModel.mediaManager.mediaState.collectAsState()
    val isAudioOwner by viewModel.isAudioOwner.collectAsState()
    val artworkBytes by viewModel.artworkManager.currentArtwork.collectAsState()

    val imageBitmap = remember(artworkBytes) {
        artworkBytes?.let {
            try {
                val bitmap = android.graphics.BitmapFactory.decodeByteArray(it, 0, it.size)
                bitmap?.asImageBitmap()
            } catch (e: Exception) {
                null
            }
        }
    }

    DetailScreenTemplate(title = "Media Controller", onNavigateBack = onNavigateBack) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Audio broadcast ownership swapper
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Audio Broadcast Ownership",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = if (isAudioOwner) "Broadcasting Phone audio to Desktop PC" else "Controlling Desktop media playback",
                            fontSize = 12.sp,
                            color = Color.Gray
                        )
                    }
                    Switch(
                        checked = isAudioOwner,
                        onCheckedChange = { viewModel.requestRole(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.Black,
                            checkedTrackColor = MaterialTheme.colorScheme.primary,
                            uncheckedThumbColor = Color.Gray,
                            uncheckedTrackColor = Color(0xFF201F20)
                        )
                    )
                }
            }

            // Big Artwork Container with high end visual styling
            Box(
                modifier = Modifier
                    .size(260.dp)
                    .clip(RoundedCornerShape(32.dp))
                    .background(Color(0xFF131314))
                    .border(1.dp, Color(0x22FFFFFF), RoundedCornerShape(32.dp)),
                contentAlignment = Alignment.Center
            ) {
                if (imageBitmap != null) {
                    androidx.compose.foundation.Image(
                        bitmap = imageBitmap,
                        contentDescription = "Artwork",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.MusicNote,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(80.dp)
                    )
                }
            }

            // Meta Details
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = mediaState?.title ?: "Ready to Sync",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = mediaState?.artist ?: "Awaiting playback session",
                    fontSize = 16.sp,
                    color = Color.Gray,
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Large controls container
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Progress Indicator if playing
                    if (mediaState != null && mediaState!!.duration > 0) {
                        val progress = mediaState!!.position.toFloat() / mediaState!!.duration.toFloat()
                        LinearProgressIndicator(
                            progress = { progress.coerceIn(0f, 1f) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(CircleShape),
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = Color(0x11FFFFFF)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Volume Down
                        IconButton(onClick = { viewModel.mediaManager.sendCommand("VOLUME_DOWN") }) {
                            Icon(Icons.Default.VolumeDown, contentDescription = "Volume Down", tint = Color.White, modifier = Modifier.size(28.dp))
                        }

                        // Previous
                        IconButton(onClick = { viewModel.mediaManager.sendCommand("PREVIOUS") }) {
                            Icon(Icons.Default.SkipPrevious, contentDescription = "Prev", tint = Color.White, modifier = Modifier.size(28.dp))
                        }

                        // Play/Pause
                        val isPlaying = mediaState?.isPlaying == true
                        IconButton(
                            onClick = { viewModel.mediaManager.sendCommand(if (isPlaying) "PAUSE" else "PLAY") },
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary)
                        ) {
                            Icon(
                                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = "PlayPause",
                                tint = Color.Black,
                                modifier = Modifier.size(36.dp)
                            )
                        }

                        // Next
                        IconButton(onClick = { viewModel.mediaManager.sendCommand("NEXT") }) {
                            Icon(Icons.Default.SkipNext, contentDescription = "Next", tint = Color.White, modifier = Modifier.size(28.dp))
                        }

                        // Volume Up
                        IconButton(onClick = { viewModel.mediaManager.sendCommand("VOLUME_UP") }) {
                            Icon(Icons.Default.VolumeUp, contentDescription = "Volume Up", tint = Color.White, modifier = Modifier.size(28.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CallsScreen(onNavigateBack: () -> Unit = {}) {
    DetailScreenTemplate(title = "Phone Call Synchronization", onNavigateBack = onNavigateBack) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .clip(CircleShape)
                    .background(Color(0x11FFFFFF)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Call, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(48.dp))
            }
            
            Text(
                text = "Inbound Call Broadcasts",
                fontWeight = FontWeight.Bold,
                fontSize = 22.sp,
                color = Color.White
            )

            Text(
                text = "When active, Synco automatically intercept incoming call statuses on your phone, then forwards call alerts and contact names to your PC terminal screen instantly.",
                color = Color.Gray,
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
                lineHeight = 22.sp
            )

            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("SYNCO TELEMETRY STATES", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                    HorizontalDivider(color = Color(0x11FFFFFF))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Active Caller Sync Mode", color = Color.LightGray, fontSize = 13.sp)
                        Text("Fully Armed", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Inbound Router Stream", color = Color.LightGray, fontSize = 13.sp)
                        Text("Active", color = Color(0xFF81C784), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun BluetoothDeviceScreen(onNavigateBack: () -> Unit = {}) {
    DetailScreenTemplate(title = "Bluetooth Scan", onNavigateBack = onNavigateBack) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
            Text(
                text = "Searching for nearby bluetooth devices...",
                color = Color.LightGray
            )
        }
    }
}

@Composable
fun PermissionsScreen(
    onNavigateBack: () -> Unit = {},
    viewModel: NetworkViewModel
) {
    val context = LocalContext.current
    val hasNotificationPermission by viewModel.hasNotificationPermission.collectAsState()
    val hasPhonePermission by viewModel.hasPhonePermission.collectAsState()

    DetailScreenTemplate(title = "Access Authorizations", onNavigateBack = onNavigateBack) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Text(
                text = "Synco utilizes advanced local system bindings to route media state and call triggers to your desktop. Authorize permissions to arm full capabilities.",
                color = Color.Gray,
                fontSize = 14.sp,
                lineHeight = 22.sp
            )

            // Permission block: Notification Listener
            PermissionRowCard(
                title = "Notification Listener",
                desc = "Required to capture details of active media track, artist, artwork on your phone, then synchronize streaming commands from desktop PC.",
                isGranted = hasNotificationPermission,
                onGrantClick = {
                    val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
                    context.startActivity(intent)
                }
            )

            // Permission block: Phone State
            PermissionRowCard(
                title = "Phone Call Synchronization",
                desc = "Required to capture phone call events, Caller ID statuses, and route live desktop prompts on call alerts.",
                isGranted = hasPhonePermission,
                onGrantClick = {
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = android.net.Uri.fromParts("package", context.packageName, null)
                    }
                    context.startActivity(intent)
                }
            )
        }
    }
}

@Composable
fun PermissionRowCard(
    title: String,
    desc: String,
    isGranted: Boolean,
    onGrantClick: () -> Unit
) {
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = title, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                
                Surface(
                    color = if (isGranted) Color(0x2281C784) else Color(0x22EF5350),
                    shape = CircleShape
                ) {
                    Text(
                        text = if (isGranted) "Granted" else "Required",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isGranted) Color(0xFF81C784) else Color(0xFFEF5350),
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(text = desc, fontSize = 13.sp, color = Color.Gray, lineHeight = 18.sp)
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = onGrantClick,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isGranted) Color(0xFF201F20) else MaterialTheme.colorScheme.primary,
                    contentColor = if (isGranted) Color.White else Color.Black
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth().height(44.dp)
            ) {
                Text(
                    text = if (isGranted) "Configure in Settings" else "Grant Access Permission",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
            }
        }
    }
}

@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit = {},
    viewModel: NetworkViewModel
) {
    val profileName by viewModel.profileName.collectAsState()

    DetailScreenTemplate(title = "Settings Center", onNavigateBack = onNavigateBack) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Text("USER PROFILE", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
            
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(20.dp)) {
                    OutlinedTextField(
                        value = profileName,
                        onValueChange = { viewModel.updateProfileName(it) },
                        label = { Text("YOUR NAME", fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp) },
                        placeholder = { Text("Enter your name") },
                        leadingIcon = { Icon(Icons.Outlined.Person, contentDescription = null, tint = Color.Gray) },
                        modifier = Modifier.fillMaxWidth().testTag("profile_name_input_field"),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedContainerColor = Color(0x0AFFFFFF),
                            unfocusedContainerColor = Color.Transparent,
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = Color(0x22FFFFFF)
                        ),
                        shape = RoundedCornerShape(14.dp)
                    )
                }
            }

            Text("DEVICE PREFERENCES", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
            
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    SettingsItemRow("Auto-connect to saved hub", true)
                    SettingsItemRow("High Fidelity Audio Engine", true)
                    SettingsItemRow("Minimize latency overhead", false)
                    SettingsItemRow("Persistent Background Service", true)
                }
            }

            Text("SYSTEM INFO", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Synco Engine Version", color = Color.LightGray, fontSize = 13.sp)
                        Text("v1.4.2-Pro", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Handshake Protocol", color = Color.LightGray, fontSize = 13.sp)
                        Text("ECDH-384-ZSTD", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun SettingsItemRow(label: String, initialChecked: Boolean) {
    var checked by remember { mutableStateOf(initialChecked) }
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, color = Color.LightGray, fontSize = 14.sp)
        Switch(
            checked = checked,
            onCheckedChange = { checked = it },
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.Black,
                checkedTrackColor = MaterialTheme.colorScheme.primary,
                uncheckedThumbColor = Color.Gray,
                uncheckedTrackColor = Color(0xFF201F20)
            )
        )
    }
}

@Composable
fun DiagnosticsScreen(
    onNavigateBack: () -> Unit = {},
    viewModel: NetworkViewModel
) {
    val hasNotificationPermission by viewModel.hasNotificationPermission.collectAsState()
    val hasPhonePermission by viewModel.hasPhonePermission.collectAsState()
    val connectionState by viewModel.connectionState.collectAsState()
    val isAuthenticated by viewModel.isAuthenticated.collectAsState()

    DetailScreenTemplate(title = "Diagnostics Control", onNavigateBack = onNavigateBack) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Text(
                text = "Verify system integrity, evaluate real-time health modules, and manage environment permissions for the obsidian synchronization layer.",
                color = Color.Gray,
                fontSize = 14.sp,
                lineHeight = 22.sp
            )

            Text("CORE HEALTH MODULES", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Gray)

            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    ModuleHealthRow(
                        name = "WebSocket Connection",
                        desc = "Securing synchronized primary channel",
                        status = if (isAuthenticated) "ACTIVE" else if (connectionState is com.remoteaudiosync.network.ConnectionState.Connecting) "CONNECTING" else "INACTIVE",
                        isActive = isAuthenticated
                    )
                    HorizontalDivider(color = Color(0x11FFFFFF))
                    ModuleHealthRow(
                        name = "ReliableChannel",
                        desc = "Encrypted statebound transmission protocol",
                        status = if (isAuthenticated) "SECURED" else "STANDBY",
                        isActive = isAuthenticated
                    )
                    HorizontalDivider(color = Color(0x11FFFFFF))
                    ModuleHealthRow(
                        name = "Bluetooth Ownership",
                        desc = "LTE/Basic pairing broadcast node",
                        status = "STANDBY",
                        isActive = false
                    )
                }
            }

            // Stats blocks side-by-side
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                GlassCard(modifier = Modifier.weight(1f)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("MEMORY USED", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("42.6 MB", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    }
                }
                GlassCard(modifier = Modifier.weight(1f)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("UPTIME", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("14d 02h", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary)
                    }
                }
            }

            Text("ENVIRONMENT PERMISSIONS", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
            
            PermissionDiagnosticRow(
                name = "Notification Access",
                desc = "Forward system alerts and media track metadata details.",
                isArmed = hasNotificationPermission
            )

            PermissionDiagnosticRow(
                name = "Phone State Access",
                desc = "Forward callers identification and sync remote desktop widgets.",
                isArmed = hasPhonePermission
            )
        }
    }
}

@Composable
fun ModuleHealthRow(
    name: String,
    desc: String,
    status: String,
    isActive: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = name, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color.White)
            Text(text = desc, fontSize = 12.sp, color = Color.Gray)
        }
        
        Surface(
            color = if (isActive) Color(0x2281C784) else Color(0x22FFFFFF),
            shape = RoundedCornerShape(8.dp)
        ) {
            Text(
                text = status,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = if (isActive) Color(0xFF81C784) else Color.Gray,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
            )
        }
    }
}

@Composable
fun PermissionDiagnosticRow(
    name: String,
    desc: String,
    isArmed: Boolean
) {
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = name, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color.White)
                Text(text = desc, fontSize = 12.sp, color = Color.Gray)
            }
            Spacer(modifier = Modifier.width(16.dp))
            Surface(
                color = if (isArmed) Color(0x2281C784) else Color(0x22EF5350),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = if (isArmed) "Armed" else "Missing",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isArmed) Color(0xFF81C784) else Color(0xFFEF5350),
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                )
            }
        }
    }
}

@Composable
fun PreloaderScreen(onComplete: () -> Unit) {
    var progress by remember { mutableStateOf(0f) }
    var bootLog by remember { mutableStateOf("BOOTING ENGINE SEQUENCE...") }
    
    // Animate progress smoothly from 0 to 1 over 2200ms
    LaunchedEffect(Unit) {
        val duration = 2200L
        val steps = 100
        val stepTime = duration / steps
        for (i in 0..steps) {
            progress = i / 100f
            bootLog = when {
                i < 15 -> "INITIALIZING HIGH-FIDELITY SYNCO ENGINE..."
                i < 35 -> "SECURING ECC IDENTITY CHANNELS..."
                i < 55 -> "BUFFERING LOW-LATENCY AUDIO PIPELINE..."
                i < 75 -> "SYNCHRONIZING DESKTOP CLOCKS..."
                i < 95 -> "CALIBRATING DEVIATION OVERHEADS..."
                else -> "ENGINE SEQUENCE READY."
            }
            kotlinx.coroutines.delay(stepTime)
        }
        onComplete()
    }

    val infiniteTransition = rememberInfiniteTransition(label = "preloader")
    
    // Pulsing alpha for the glow effect
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.9f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow"
    )

    // Continuous rotation for outer rings
    val rotationAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF070708)), // Pitch dark luxury background
        contentAlignment = Alignment.Center
    ) {
        // Soft background ambient glow
        Box(
            modifier = Modifier
                .size(350.dp)
                .drawBehind {
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                Color(0x1E7B2CFF), // Soft neon purple
                                Color.Transparent
                            )
                        ),
                        alpha = glowAlpha
                    )
                }
        )

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(24.dp)
        ) {
            // Elegant Visual Loader Engine Core
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(150.dp)
            ) {
                // Spinning outer dashed ring
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .drawBehind {
                            drawArc(
                                color = Color(0x33ADC6FF),
                                startAngle = rotationAngle,
                                sweepAngle = 270f,
                                useCenter = false,
                                style = androidx.compose.ui.graphics.drawscope.Stroke(
                                    width = 3.dp.toPx(),
                                    pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(
                                        intervals = floatArrayOf(20f, 10f)
                                    )
                                )
                            )
                        }
                )

                // Counter-spinning inner accent arc
                Box(
                    modifier = Modifier
                        .size(110.dp)
                        .drawBehind {
                            drawArc(
                                color = Color(0xFF4EDEA3), // Emerald accent
                                startAngle = -rotationAngle * 1.5f,
                                sweepAngle = 120f,
                                useCenter = false,
                                style = androidx.compose.ui.graphics.drawscope.Stroke(
                                    width = 2.dp.toPx()
                                )
                            )
                        }
                )

                // Centered glowing Synco icon
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(Color(0x11FFFFFF))
                        .border(1.dp, Color(0x22FFFFFF), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Sync,
                        contentDescription = "Syncing",
                        tint = Color(0xFFADC6FF),
                        modifier = Modifier.size(32.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(48.dp))

            // Text Typography
            Text(
                text = "Synco",
                fontWeight = FontWeight.ExtraBold,
                fontSize = 28.sp,
                letterSpacing = (-0.5).sp,
                color = Color.White
            )
            
            Text(
                text = "PROFESSIONAL AUDIO ENGINE",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp,
                color = Color.Gray,
                modifier = Modifier.padding(top = 4.dp)
            )

            Spacer(modifier = Modifier.height(40.dp))

            // Premium Linear Glowing Progress Bar
            Box(
                modifier = Modifier
                    .width(220.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color(0x1AFFFFFF))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(progress)
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(
                                    Color(0xFF7B2CFF), // Violet
                                    Color(0xFFADC6FF)  // Ice blue
                                )
                            )
                        )
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Cyber-punk Diagnostic Log Feed
            Text(
                text = bootLog,
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF4EDEA3), // Cyber green
                textAlign = TextAlign.Center,
                modifier = Modifier.height(32.dp)
            )

            Text(
                text = "${(progress * 100).toInt()}% READY",
                fontFamily = FontFamily.Monospace,
                fontSize = 10.sp,
                color = Color.Gray
            )
        }
    }
}

