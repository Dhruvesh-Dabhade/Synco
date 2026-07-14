package com.remoteaudiosync.protocol

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@SerialName("RoleChangeRequest")
data class RoleChangeRequestPayload(
    val requestAudioOwner: Boolean,
    val sourceEpoch: Long = 0L,
    val requestTimestamp: Long = 0L,
    val deviceId: String = ""
) : Payload()
