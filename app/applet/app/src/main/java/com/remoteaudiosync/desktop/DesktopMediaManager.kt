package com.remoteaudiosync.desktop

import com.remoteaudiosync.network.ConnectionState
import com.remoteaudiosync.network.ReliableChannel
import com.remoteaudiosync.protocol.MediaCommandPayload
import com.remoteaudiosync.protocol.MediaStatePayload
import com.remoteaudiosync.protocol.Packet
import com.remoteaudiosync.protocol.PacketType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

class DesktopMediaManager(
    private val reliableChannel: ReliableChannel,
    private val sessionObserver: DesktopMediaSessionObserver,
    private val commandExecutor: DesktopMediaCommandExecutor,
    private val statePublisher: DesktopMediaStatePublisher,
    private val coroutineScope: CoroutineScope
) {
    private var isAudioOwner = false
    private var connectionJob: Job? = null
    private var packetJob: Job? = null
    private var isObserving = false

    private val _mediaState = MutableStateFlow<MediaStatePayload?>(null)
    val mediaState: StateFlow<MediaStatePayload?> = _mediaState.asStateFlow()

    init {
        startConnectionMonitoring()
        startListeningToChannel()
    }

    fun setRole(isAudioOwner: Boolean) {
        this.isAudioOwner = isAudioOwner
        if (isAudioOwner) {
            startObservingLocalMedia()
        } else {
            stopObservingLocalMedia()
            _mediaState.value = null
        }
    }

    private fun startObservingLocalMedia() {
        if (!isObserving) {
            sessionObserver.startObserving { state ->
                _mediaState.value = state
                statePublisher.publishState(state)
            }
            isObserving = true
            // Publish initial state upon becoming owner
            val initialState = sessionObserver.getCurrentState()
            _mediaState.value = initialState
            statePublisher.publishState(initialState)
        }
    }

    private fun stopObservingLocalMedia() {
        if (isObserving) {
            sessionObserver.stopObserving()
            isObserving = false
        }
    }

    private fun startConnectionMonitoring() {
        connectionJob?.cancel()
        connectionJob = coroutineScope.launch {
            reliableChannel.connectionState.collect { state ->
                if (state is ConnectionState.Connected) {
                    if (isAudioOwner) {
                        // On reconnect, resend latest state and recover
                        statePublisher.publishState(sessionObserver.getCurrentState())
                    }
                }
            }
        }
    }

    private fun startListeningToChannel() {
        packetJob?.cancel()
        packetJob = coroutineScope.launch {
            reliableChannel.incomingPackets.collect { packet ->
                when (packet.packetType) {
                    PacketType.MEDIA_COMMAND -> {
                        if (isAudioOwner) {
                            val payload = packet.payload as? MediaCommandPayload
                            if (payload != null) {
                                commandExecutor.executeCommand(payload)
                            }
                        }
                    }
                    PacketType.MEDIA_STATE -> {
                        if (!isAudioOwner) {
                            val payload = packet.payload as? MediaStatePayload
                            if (payload != null) {
                                if (payload.title == "NO_ACTIVE_MEDIA_SESSION") {
                                    _mediaState.value = null
                                } else {
                                    _mediaState.value = payload
                                }
                            }
                        }
                    }
                    else -> {}
                }
            }
        }
    }

    fun sendCommand(command: String, seekPosition: Long? = null, volume: Float? = null) {
        if (isAudioOwner) {
            commandExecutor.executeCommand(MediaCommandPayload(command, seekPosition, volume))
        } else {
            val packet = Packet(
                version = 1,
                id = UUID.randomUUID().toString(),
                timestamp = System.currentTimeMillis(),
                senderId = "desktop-server",
                receiverId = "android-client",
                packetType = PacketType.MEDIA_COMMAND,
                payload = MediaCommandPayload(command, seekPosition, volume)
            )
            coroutineScope.launch {
                reliableChannel.sendWithAck(packet)
            }
        }
    }

    fun cleanup() {
        connectionJob?.cancel()
        packetJob?.cancel()
        stopObservingLocalMedia()
    }
}
