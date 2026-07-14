package com.remoteaudiosync.manager

import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioManager
import android.os.Build
import android.telecom.VideoProfile
import com.remoteaudiosync.network.ConnectionState
import com.remoteaudiosync.network.ReliableChannel
import com.remoteaudiosync.protocol.CallCommandPayload
import com.remoteaudiosync.protocol.CallStatePayload
import com.remoteaudiosync.protocol.Packet
import com.remoteaudiosync.protocol.PacketType
import com.remoteaudiosync.service.MyInCallService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

interface AndroidCallManager {
    val callState: StateFlow<String>
    val callerId: StateFlow<String?>
    fun start()
    fun stop()
    fun executeCommand(command: String)
}

class DefaultAndroidCallManager(
    private val context: Context,
    private val reliableChannel: ReliableChannel,
    private val stateManager: AudioOwnerStateManager,
    private val coroutineScope: CoroutineScope,
    private val checkPermissions: () -> Boolean = {
        val phoneStateGranted = context.checkSelfPermission(android.Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED
        val answerCallsGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.checkSelfPermission(android.Manifest.permission.ANSWER_PHONE_CALLS) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
        phoneStateGranted && answerCallsGranted
    }
) : AndroidCallManager {

    private val _callState = MutableStateFlow("ended")
    override val callState: StateFlow<String> = _callState.asStateFlow()

    private val _callerId = MutableStateFlow<String?>(null)
    override val callerId: StateFlow<String?> = _callerId.asStateFlow()

    private var incomingPacketsJob: Job? = null
    private var connectionStateJob: Job? = null

    private val callServiceListener = object : MyInCallService.CallListener {
        override fun onCallAdded(call: android.telecom.Call) {
            val caller = call.details?.handle?.schemeSpecificPart ?: "Unknown"
            handleCallAdded(call.state, caller)
        }

        override fun onCallRemoved(call: android.telecom.Call) {
            handleCallRemoved()
        }

        override fun onCallStateChanged(call: android.telecom.Call, state: Int) {
            handleCallStateChanged(state)
        }
    }

    fun handleCallAdded(state: Int, caller: String?) {
        val stateStr = mapTelecomState(state)
        _callerId.value = caller
        _callState.value = stateStr
        sendCallState(stateStr, caller)
    }

    fun handleCallRemoved() {
        _callState.value = "ended"
        _callerId.value = null
        sendCallState("ended", null)
    }

    fun handleCallStateChanged(state: Int) {
        val stateStr = mapTelecomState(state)
        _callState.value = stateStr
        sendCallState(stateStr, _callerId.value)
    }

    private fun mapTelecomState(state: Int): String {
        return when (state) {
            android.telecom.Call.STATE_RINGING -> "ringing"
            android.telecom.Call.STATE_DIALING -> "ringing"
            android.telecom.Call.STATE_ACTIVE -> "answered"
            android.telecom.Call.STATE_DISCONNECTED -> "ended"
            android.telecom.Call.STATE_DISCONNECTING -> "ended"
            else -> "ringing"
        }
    }

    override fun start() {
        if (!checkPermissions()) {
            _callState.value = "MISSING_PERMISSION"
            _callerId.value = "MISSING_PERMISSION"
            sendCallState("MISSING_PERMISSION", "MISSING_PERMISSION")
            return
        }

        MyInCallService.listener = callServiceListener

        incomingPacketsJob = coroutineScope.launch {
            reliableChannel.incomingPackets.collect { packet ->
                if (packet.packetType == PacketType.CALL_COMMAND) {
                    val payload = packet.payload as? CallCommandPayload
                    if (payload != null) {
                        // Remote commanded us to execute
                        if (stateManager.currentRole.value == AudioRole.ACTIVE_AUDIO_OWNER) {
                            executeLocally(payload.command)
                        }
                    }
                }
            }
        }

        connectionStateJob = coroutineScope.launch {
            reliableChannel.connectionState.collect { connState ->
                if (connState is ConnectionState.Connected) {
                    // Restore call state after reconnect
                    sendCallState(_callState.value, _callerId.value)
                }
            }
        }
    }

    override fun stop() {
        MyInCallService.listener = null
        incomingPacketsJob?.cancel()
        incomingPacketsJob = null
        connectionStateJob?.cancel()
        connectionStateJob = null
    }

    override fun executeCommand(command: String) {
        if (stateManager.currentRole.value == AudioRole.ACTIVE_AUDIO_OWNER) {
            executeLocally(command)
        } else {
            // Forward through ReliableChannel using sendWithAck
            coroutineScope.launch {
                val packet = Packet(
                    version = 1,
                    id = UUID.randomUUID().toString(),
                    timestamp = System.currentTimeMillis(),
                    senderId = stateManager.deviceId,
                    receiverId = if (stateManager.deviceId == "android-client") "desktop-server" else "android-client",
                    packetType = PacketType.CALL_COMMAND,
                    payload = CallCommandPayload(command)
                )
                reliableChannel.sendWithAck(packet)
            }
        }
    }

    private fun executeLocally(command: String) {
        val activeCall = MyInCallService.activeCall
        when (command.uppercase()) {
            "ANSWER" -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    activeCall?.answer(VideoProfile.STATE_AUDIO_ONLY)
                }
                _callState.value = "answered"
                sendCallState("answered", _callerId.value)
            }
            "REJECT" -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    activeCall?.reject(false, null)
                } else {
                    activeCall?.disconnect()
                }
                _callState.value = "rejected"
                sendCallState("rejected", _callerId.value)
            }
            "END" -> {
                activeCall?.disconnect()
                _callState.value = "ended"
                _callerId.value = null
                sendCallState("ended", null)
            }
            "MUTE" -> {
                val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
                if (audioManager != null) {
                    audioManager.isMicrophoneMute = !audioManager.isMicrophoneMute
                }
            }
        }
    }

    private fun sendCallState(state: String, callerId: String?) {
        coroutineScope.launch {
            val packet = Packet(
                version = 1,
                id = UUID.randomUUID().toString(),
                timestamp = System.currentTimeMillis(),
                senderId = stateManager.deviceId,
                receiverId = if (stateManager.deviceId == "android-client") "desktop-server" else "android-client",
                packetType = PacketType.CALL_STATE,
                payload = CallStatePayload(state, callerId)
            )
            reliableChannel.send(packet)
        }
    }
}
