package com.remoteaudiosync.protocol

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@SerialName("MediaCommand")
data class MediaCommandPayload(
    val command: String,
    val seekPosition: Long? = null,
    val volume: Int? = null
) : Payload()
