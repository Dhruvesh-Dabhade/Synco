package com.remoteaudiosync.protocol

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@SerialName("Ack")
data class AckPayload(val originalPacketId: String) : Payload()
