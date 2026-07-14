package com.remoteaudiosync.protocol

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@SerialName("PairRequest")
data class PairRequestPayload(val deviceName: String, val publicKey: String, val pin: String = "") : Payload()
