package com.remoteaudiosync.protocol

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@SerialName("WakeRequest")
data class WakeRequestPayload(val reason: String) : Payload()
