package com.remoteaudiosync.protocol

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@SerialName("ArtworkRequest")
data class ArtworkRequestPayload(val mediaId: String) : Payload()
