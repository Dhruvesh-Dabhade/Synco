package com.remoteaudiosync.protocol

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@SerialName("ArtworkResponse")
data class ArtworkResponsePayload(val mediaId: String, val artworkBase64: String) : Payload()
