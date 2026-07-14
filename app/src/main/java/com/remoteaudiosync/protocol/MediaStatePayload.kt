package com.remoteaudiosync.protocol

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@SerialName("MediaState")
data class MediaStatePayload(
    val title: String, 
    val artist: String, 
    val isPlaying: Boolean, 
    val position: Long, 
    val duration: Long,
    val appName: String = "",
    val volume: Int = 100,
    val isMuted: Boolean = false,
    val artworkId: String? = null,
    val artworkAvailable: Boolean = false
) : Payload()
