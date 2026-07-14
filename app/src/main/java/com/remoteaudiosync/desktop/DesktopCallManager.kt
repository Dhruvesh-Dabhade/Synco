package com.remoteaudiosync.desktop

import com.remoteaudiosync.network.ReliableChannel
import com.remoteaudiosync.protocol.CallCommandPayload
import com.remoteaudiosync.protocol.CallStatePayload
import com.remoteaudiosync.protocol.Packet
import com.remoteaudiosync.protocol.PacketType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

class DesktopCallManager(
    private val reliableChannel: ReliableChannel,
    private val coroutineScope: CoroutineScope
) {
    private val _callState = MutableStateFlow("ended")
    val callState: StateFlow<String> = _callState.asStateFlow()

    private val _callerId = MutableStateFlow<String?>(null)
    val callerId: StateFlow<String?> = _callerId.asStateFlow()

    private var packetCollectorJob: Job? = null

    init {
        startListening()
    }

    fun hasLocalCallControlCapability(): Boolean {
        // Report capability honestly: desktop has no local telephony APIs
        return false
    }

    private fun startListening() {
        packetCollectorJob = coroutineScope.launch {
            reliableChannel.incomingPackets.collect { packet ->
                if (packet.packetType == PacketType.CALL_STATE) {
                    val payload = packet.payload as? CallStatePayload
                    if (payload != null) {
                        _callState.value = payload.state
                        _callerId.value = payload.callerId
                    }
                }
            }
        }
    }

    fun sendCommand(command: String) {
        // Forward through ReliableChannel using sendWithAck
        coroutineScope.launch {
            val packet = Packet(
                version = 1,
                id = UUID.randomUUID().toString(),
                timestamp = System.currentTimeMillis(),
                senderId = "desktop-server",
                receiverId = "android-client",
                packetType = PacketType.CALL_COMMAND,
                payload = CallCommandPayload(command)
            )
            reliableChannel.sendWithAck(packet)
        }
    }

    fun stop() {
        packetCollectorJob?.cancel()
    }
}
