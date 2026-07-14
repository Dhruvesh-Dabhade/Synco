package com.remoteaudiosync.manager

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class AudioRole {
    ACTIVE_AUDIO_OWNER,
    REMOTE_CONTROLLER
}

interface AudioOwnerStateManager {
    val currentRole: StateFlow<AudioRole>
    val sourceEpoch: StateFlow<Long>
    val deviceId: String

    fun setRole(role: AudioRole, epoch: Long)
    fun incrementEpoch(): Long
}

class DefaultAudioOwnerStateManager(
    override val deviceId: String,
    initialRole: AudioRole = AudioRole.REMOTE_CONTROLLER
) : AudioOwnerStateManager {
    
    private val _currentRole = MutableStateFlow(initialRole)
    override val currentRole: StateFlow<AudioRole> = _currentRole.asStateFlow()

    private val _sourceEpoch = MutableStateFlow(0L)
    override val sourceEpoch: StateFlow<Long> = _sourceEpoch.asStateFlow()

    override fun setRole(role: AudioRole, epoch: Long) {
        _sourceEpoch.value = epoch
        _currentRole.value = role
    }

    override fun incrementEpoch(): Long {
        _sourceEpoch.value += 1
        return _sourceEpoch.value
    }
}
