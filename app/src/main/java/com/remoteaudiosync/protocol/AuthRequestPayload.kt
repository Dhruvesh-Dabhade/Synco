package com.remoteaudiosync.protocol

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@SerialName("AuthRequest")
data class AuthRequestPayload(
    val ephemeralPublicKey: String,
    val signature: String
) : Payload()
