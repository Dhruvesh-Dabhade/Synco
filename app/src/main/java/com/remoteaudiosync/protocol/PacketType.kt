package com.remoteaudiosync.protocol

import kotlinx.serialization.Serializable

@Serializable
enum class PacketType {
    PAIR_REQUEST,
    PAIR_RESPONSE,
    AUTH_REQUEST,
    AUTH_SUCCESS,
    ACK,
    HEARTBEAT,
    ERROR,
    PING,
    PONG,
    DEVICE_INFO,
    ROLE_STATE,
    ROLE_CHANGE_REQUEST,
    MEDIA_COMMAND,
    MEDIA_STATE,
    CALL_STATE,
    CALL_COMMAND,
    BATTERY_STATE,
    PERMISSION_STATE,
    NOTIFICATION_STATE,
    ARTWORK_REQUEST,
    ARTWORK_RESPONSE,
    LIMITED_MODE_STATE,
    WAKE_REQUEST
}
