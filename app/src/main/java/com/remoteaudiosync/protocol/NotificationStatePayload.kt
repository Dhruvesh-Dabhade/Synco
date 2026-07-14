package com.remoteaudiosync.protocol

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@SerialName("NotificationState")
data class NotificationStatePayload(
    val id: String,
    val title: String,
    val text: String,
    val packageName: String = "",
    val appName: String = "",
    val timestamp: Long = 0L,
    val isOngoing: Boolean = false,
    val action: String = "ADDED" // "ADDED", "UPDATED", "REMOVED"
) : Payload()
