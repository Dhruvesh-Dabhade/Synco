package com.remoteaudiosync.protocol

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@SerialName("CallState")
data class CallStatePayload(val state: String, val callerId: String? = null) : Payload()
