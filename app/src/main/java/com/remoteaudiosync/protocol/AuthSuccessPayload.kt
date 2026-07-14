package com.remoteaudiosync.protocol

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@SerialName("AuthSuccess")
data class AuthSuccessPayload(
    val ephemeralPublicKey: String,
    val signature: String,
    val sessionId: String
) : Payload()
