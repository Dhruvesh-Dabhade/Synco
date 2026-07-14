package com.remoteaudiosync.protocol

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@SerialName("Heartbeat")
data class HeartbeatPayload(val uptimeMillis: Long) : Payload()
