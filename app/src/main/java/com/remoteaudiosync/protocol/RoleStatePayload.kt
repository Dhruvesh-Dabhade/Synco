package com.remoteaudiosync.protocol

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@SerialName("RoleState")
data class RoleStatePayload(
    val isAudioOwner: Boolean,
    val sourceEpoch: Long = 0L,
    val deviceId: String = ""
) : Payload()
