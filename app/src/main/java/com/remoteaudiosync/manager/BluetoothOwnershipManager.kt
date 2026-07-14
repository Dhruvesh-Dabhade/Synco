package com.remoteaudiosync.manager

import com.remoteaudiosync.network.ConnectionState
import com.remoteaudiosync.network.ReliableChannel
import com.remoteaudiosync.protocol.Packet
import com.remoteaudiosync.protocol.PacketType
import com.remoteaudiosync.protocol.RoleChangeRequestPayload
import com.remoteaudiosync.protocol.RoleStatePayload
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

interface BluetoothOwnershipManager {
    val deviceMonitor: BluetoothDeviceMonitor
    val stateManager: AudioOwnerStateManager
    val switchManager: SourceSwitchManager
    val isOwnerConnected: StateFlow<Boolean>

    fun start()
    fun stop()
}

class DefaultBluetoothOwnershipManager(
    override val deviceMonitor: BluetoothDeviceMonitor,
    override val stateManager: AudioOwnerStateManager,
    private val reliableChannel: ReliableChannel,
    private val coroutineScope: CoroutineScope,
    private val onRoleChanged: (AudioRole) -> Unit
) : BluetoothOwnershipManager {

    private val _isOwnerConnected = MutableStateFlow(false)
    override val isOwnerConnected: StateFlow<Boolean> = _isOwnerConnected.asStateFlow()

    override val switchManager: SourceSwitchManager = DefaultSourceSwitchManager(
        reliableChannel = reliableChannel,
        stateManager = stateManager,
        coroutineScope = coroutineScope,
        onRoleChanged = onRoleChanged
    )

    private var packetJob: Job? = null
    private var connectionJob: Job? = null

    override fun start() {
        deviceMonitor.startMonitoring()
        startListening()
        monitorConnection()
    }

    override fun stop() {
        deviceMonitor.stopMonitoring()
        packetJob?.cancel()
        packetJob = null
        connectionJob?.cancel()
        connectionJob = null
        switchManager.reset()
    }

    private fun startListening() {
        packetJob?.cancel()
        packetJob = coroutineScope.launch {
            reliableChannel.incomingPackets.collect { packet ->
                when (packet.packetType) {
                    PacketType.ROLE_CHANGE_REQUEST -> {
                        val payload = packet.payload as? RoleChangeRequestPayload
                        if (payload != null) {
                            switchManager.handleIncomingRequest(payload)
                        }
                    }
                    PacketType.ROLE_STATE -> {
                        val payload = packet.payload as? RoleStatePayload
                        if (payload != null) {
                            switchManager.handleIncomingState(payload)
                            
                            if (payload.isAudioOwner) {
                                _isOwnerConnected.value = true
                            } else if (stateManager.currentRole.value == AudioRole.ACTIVE_AUDIO_OWNER) {
                                _isOwnerConnected.value = false
                            }
                        }
                    }
                    else -> {}
                }
            }
        }
    }

    private fun monitorConnection() {
        connectionJob?.cancel()
        connectionJob = coroutineScope.launch {
            reliableChannel.connectionState.collect { state ->
                when (state) {
                    is ConnectionState.Connected -> {
                        if (stateManager.currentRole.value == AudioRole.ACTIVE_AUDIO_OWNER) {
                            _isOwnerConnected.value = false
                            val packet = Packet(
                                version = 1,
                                id = UUID.randomUUID().toString(),
                                timestamp = System.currentTimeMillis(),
                                senderId = stateManager.deviceId,
                                receiverId = if (stateManager.deviceId == "android-client") "desktop-server" else "android-client",
                                packetType = PacketType.ROLE_STATE,
                                payload = RoleStatePayload(
                                    isAudioOwner = true,
                                    sourceEpoch = stateManager.sourceEpoch.value,
                                    deviceId = stateManager.deviceId
                                )
                            )
                            reliableChannel.send(packet)
                        } else {
                            _isOwnerConnected.value = true
                        }
                    }
                    is ConnectionState.Disconnected, is ConnectionState.Failed, is ConnectionState.Lost -> {
                        _isOwnerConnected.value = false
                        switchManager.reset()
                    }
                    else -> {}
                }
            }
        }
    }
}
