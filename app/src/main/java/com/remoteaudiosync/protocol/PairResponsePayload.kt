package com.remoteaudiosync.protocol

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@SerialName("PairResponse")
data class PairResponsePayload(val status: String, val publicKey: String) : Payload()
