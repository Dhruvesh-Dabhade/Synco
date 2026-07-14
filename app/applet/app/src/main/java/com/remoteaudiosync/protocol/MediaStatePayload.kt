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
    val volume: Float = 1.0f,
    val isMuted: Boolean = false
) : Payload()
