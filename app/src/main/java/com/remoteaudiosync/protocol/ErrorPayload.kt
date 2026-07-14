package com.remoteaudiosync.protocol

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@SerialName("Error")
data class ErrorPayload(val code: String, val message: String) : Payload()
