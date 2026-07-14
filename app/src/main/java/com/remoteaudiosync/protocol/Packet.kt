package com.remoteaudiosync.protocol

import kotlinx.serialization.Serializable

@Serializable
data class Packet(
    val version: Int,
    val id: String,
    val timestamp: Long,
    val senderId: String,
    val receiverId: String,
    val packetType: PacketType,
    val payload: Payload? = null,
    val signature: String? = null,
    val ciphertext: String? = null,
    val nonce: String? = null
)
