package com.remoteaudiosync.manager

import com.remoteaudiosync.network.ReliableChannel
import com.remoteaudiosync.protocol.Packet
import com.remoteaudiosync.protocol.PacketType
import com.remoteaudiosync.protocol.RoleChangeRequestPayload
import com.remoteaudiosync.protocol.RoleStatePayload
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

interface SourceSwitchManager {
    val isSwitching: StateFlow<Boolean>
    suspend fun initiateSwitch(targetRole: AudioRole): Boolean
    fun handleIncomingRequest(payload: RoleChangeRequestPayload): Boolean
    fun handleIncomingState(payload: RoleStatePayload)
    fun reset()
}

class DefaultSourceSwitchManager(
    private val reliableChannel: ReliableChannel,
    private val stateManager: AudioOwnerStateManager,
    private val coroutineScope: CoroutineScope,
    private val onRoleChanged: (AudioRole) -> Unit
) : SourceSwitchManager {

    private val _isSwitching = MutableStateFlow(false)
    override val isSwitching: StateFlow<Boolean> = _isSwitching.asStateFlow()

    private var pendingSwitchDeferred: CompletableDeferred<Boolean>? = null
    private var timeoutJob: Job? = null
    private var pendingTargetRole: AudioRole? = null
    private var pendingTimestamp: Long = 0L

    override suspend fun initiateSwitch(targetRole: AudioRole): Boolean {
        if (stateManager.currentRole.value == targetRole) {
            return true
        }
        if (_isSwitching.value) {
            return false
        }

        _isSwitching.value = true
        pendingTargetRole = targetRole

        val isRequestingOwner = targetRole == AudioRole.ACTIVE_AUDIO_OWNER
        val newEpoch = stateManager.incrementEpoch()
        pendingTimestamp = System.currentTimeMillis()

        val deferred = CompletableDeferred<Boolean>()
        pendingSwitchDeferred = deferred

        // Send ROLE_CHANGE_REQUEST
        val packet = Packet(
            version = 1,
            id = UUID.randomUUID().toString(),
            timestamp = pendingTimestamp,
            senderId = stateManager.deviceId,
            receiverId = if (stateManager.deviceId == "android-client") "desktop-server" else "android-client",
            packetType = PacketType.ROLE_CHANGE_REQUEST,
            payload = RoleChangeRequestPayload(
                requestAudioOwner = isRequestingOwner,
                sourceEpoch = newEpoch,
                requestTimestamp = pendingTimestamp,
                deviceId = stateManager.deviceId
            )
        )

        val sentSuccessfully = reliableChannel.sendWithAck(packet)
        if (!sentSuccessfully) {
            rollback()
            return false
        }

        // Start timeout job
        timeoutJob = coroutineScope.launch {
            delay(5000) // 5 seconds timeout
            deferred.complete(false)
        }

        val success = deferred.await()
        timeoutJob?.cancel()
        timeoutJob = null
        pendingSwitchDeferred = null

        if (success) {
            _isSwitching.value = false
            pendingTargetRole = null
            return true
        } else {
            rollback()
            return false
        }
    }

    private fun rollback() {
        _isSwitching.value = false
        pendingTargetRole = null
        pendingSwitchDeferred = null
        timeoutJob?.cancel()
        timeoutJob = null
    }

    override fun handleIncomingRequest(payload: RoleChangeRequestPayload): Boolean {
        val peerEpoch = payload.sourceEpoch
        val peerTimestamp = payload.requestTimestamp
        val peerDeviceId = payload.deviceId
        val wantsOwner = payload.requestAudioOwner

        val myEpoch = stateManager.sourceEpoch.value
        val myRole = stateManager.currentRole.value

        // Check if there is a conflict
        val isConflict = (myRole == AudioRole.ACTIVE_AUDIO_OWNER && wantsOwner) ||
                (_isSwitching.value && pendingTargetRole == AudioRole.ACTIVE_AUDIO_OWNER && wantsOwner)

        if (isConflict) {
            val peerWins = shouldYieldToPeer(
                myEpoch = myEpoch,
                myTimestamp = pendingTimestamp,
                myDeviceId = stateManager.deviceId,
                peerEpoch = peerEpoch,
                peerTimestamp = peerTimestamp,
                peerDeviceId = peerDeviceId
            )

            if (peerWins) {
                // Yield to peer!
                if (_isSwitching.value) {
                    pendingSwitchDeferred?.complete(false)
                    rollback()
                }
                
                if (myRole == AudioRole.ACTIVE_AUDIO_OWNER) {
                    stateManager.setRole(AudioRole.REMOTE_CONTROLLER, peerEpoch)
                    onRoleChanged(AudioRole.REMOTE_CONTROLLER)
                } else {
                    stateManager.setRole(AudioRole.REMOTE_CONTROLLER, peerEpoch)
                }

                // Send ROLE_STATE indicating we are REMOTE_CONTROLLER
                sendRoleState(AudioRole.REMOTE_CONTROLLER, peerEpoch)
                return true
            } else {
                // We win! Reject peer's request by sending our current state
                sendRoleState(myRole, myEpoch)
                return false
            }
        } else {
            // No conflict
            if (myRole == AudioRole.ACTIVE_AUDIO_OWNER && wantsOwner) {
                // Peer wants owner, and we are fine giving it
                stateManager.setRole(AudioRole.REMOTE_CONTROLLER, peerEpoch)
                onRoleChanged(AudioRole.REMOTE_CONTROLLER)
                sendRoleState(AudioRole.REMOTE_CONTROLLER, peerEpoch)
                return true
            } else if (myRole == AudioRole.REMOTE_CONTROLLER && !wantsOwner) {
                // Peer wants us to become owner
                stateManager.setRole(AudioRole.ACTIVE_AUDIO_OWNER, peerEpoch)
                onRoleChanged(AudioRole.ACTIVE_AUDIO_OWNER)
                sendRoleState(AudioRole.ACTIVE_AUDIO_OWNER, peerEpoch)
                return true
            }
            sendRoleState(myRole, myEpoch)
            return true
        }
    }

    override fun handleIncomingState(payload: RoleStatePayload) {
        val peerIsOwner = payload.isAudioOwner
        val peerEpoch = payload.sourceEpoch

        val myRole = stateManager.currentRole.value
        val target = pendingTargetRole

        if (_isSwitching.value && target != null) {
            if (target == AudioRole.ACTIVE_AUDIO_OWNER && !peerIsOwner) {
                // Peer released ownership, now we can become ACTIVE_AUDIO_OWNER
                stateManager.setRole(AudioRole.ACTIVE_AUDIO_OWNER, peerEpoch)
                onRoleChanged(AudioRole.ACTIVE_AUDIO_OWNER)
                sendRoleState(AudioRole.ACTIVE_AUDIO_OWNER, peerEpoch)
                pendingSwitchDeferred?.complete(true)
            } else if (target == AudioRole.REMOTE_CONTROLLER && peerIsOwner) {
                // Peer claimed ownership, we are now REMOTE_CONTROLLER
                stateManager.setRole(AudioRole.REMOTE_CONTROLLER, peerEpoch)
                onRoleChanged(AudioRole.REMOTE_CONTROLLER)
                sendRoleState(AudioRole.REMOTE_CONTROLLER, peerEpoch)
                pendingSwitchDeferred?.complete(true)
            } else {
                // We received a state that conflicts with our switch target.
                // This means our request was rejected or peer changed state concurrently.
                pendingSwitchDeferred?.complete(false)
            }
        } else {
            // Steady state role sync: make sure both agree
            if (peerIsOwner && myRole == AudioRole.ACTIVE_AUDIO_OWNER) {
                // Split brain detected! Resolve conflict
                val peerWins = shouldYieldToPeer(
                    myEpoch = stateManager.sourceEpoch.value,
                    myTimestamp = 0L,
                    myDeviceId = stateManager.deviceId,
                    peerEpoch = peerEpoch,
                    peerTimestamp = 0L,
                    peerDeviceId = payload.deviceId
                )
                if (peerWins) {
                    stateManager.setRole(AudioRole.REMOTE_CONTROLLER, peerEpoch)
                    onRoleChanged(AudioRole.REMOTE_CONTROLLER)
                    sendRoleState(AudioRole.REMOTE_CONTROLLER, peerEpoch)
                } else {
                    // We win, send our role state to peer to force sync
                    sendRoleState(AudioRole.ACTIVE_AUDIO_OWNER, stateManager.sourceEpoch.value)
                }
            } else if (!peerIsOwner && myRole == AudioRole.REMOTE_CONTROLLER) {
                // Both are remote controllers. We don't interfere, but stay in safe state.
            }
        }
    }

    override fun reset() {
        rollback()
    }

    private fun sendRoleState(role: AudioRole, epoch: Long) {
        val packet = Packet(
            version = 1,
            id = UUID.randomUUID().toString(),
            timestamp = System.currentTimeMillis(),
            senderId = stateManager.deviceId,
            receiverId = if (stateManager.deviceId == "android-client") "desktop-server" else "android-client",
            packetType = PacketType.ROLE_STATE,
            payload = RoleStatePayload(
                isAudioOwner = role == AudioRole.ACTIVE_AUDIO_OWNER,
                sourceEpoch = epoch,
                deviceId = stateManager.deviceId
            )
        )
        coroutineScope.launch {
            reliableChannel.send(packet)
        }
    }

    private fun shouldYieldToPeer(
        myEpoch: Long,
        myTimestamp: Long,
        myDeviceId: String,
        peerEpoch: Long,
        peerTimestamp: Long,
        peerDeviceId: String
    ): Boolean {
        if (peerEpoch != myEpoch) {
            return peerEpoch > myEpoch
        }
        if (peerTimestamp != myTimestamp) {
            return peerTimestamp > myTimestamp
        }
        return peerDeviceId < myDeviceId
    }
}
