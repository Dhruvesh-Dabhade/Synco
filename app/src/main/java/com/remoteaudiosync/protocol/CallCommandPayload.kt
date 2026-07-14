package com.remoteaudiosync.protocol

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@SerialName("CallCommand")
data class CallCommandPayload(val command: String) : Payload()
