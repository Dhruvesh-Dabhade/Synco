package com.remoteaudiosync.protocol

import java.util.UUID

object PacketValidator {
    const val CURRENT_VERSION = 1

    fun validate(packet: Packet): ProtocolResult<Packet> {
        if (packet.version <= 0) {
            return ProtocolResult.Failure(ProtocolError.InvalidVersion("Invalid protocol version: ${packet.version}"))
        }

        try {
            UUID.fromString(packet.id)
        } catch (e: Exception) {
            return ProtocolResult.Failure(ProtocolError.InvalidPacketId("Invalid packet ID format: ${packet.id}"))
        }

        if (packet.id.length > 100) {
            return ProtocolResult.Failure(ProtocolError.InvalidPacketId("Packet ID exceeds max safe length"))
        }

        if (packet.timestamp <= 0) {
            return ProtocolResult.Failure(ProtocolError.InvalidTimestamp("Timestamp must be greater than 0"))
        }

        val ageMs = Math.abs(System.currentTimeMillis() - packet.timestamp)
        if (ageMs > 300000L) { // 5 minutes max clock skew/drift to protect against replay attacks
            return ProtocolResult.Failure(ProtocolError.InvalidTimestamp("Packet timestamp out of acceptable window (skew: ${ageMs}ms)"))
        }

        if (packet.senderId.isBlank() || packet.senderId.length > 100) {
            return ProtocolResult.Failure(ProtocolError.InvalidSender("Sender ID must be valid and <= 100 characters"))
        }

        if (packet.receiverId.isBlank() || packet.receiverId.length > 100) {
            return ProtocolResult.Failure(ProtocolError.InvalidReceiver("Receiver ID must be valid and <= 100 characters"))
        }

        val payloadError = validatePayload(packet.packetType, packet.payload)
        if (payloadError != null) {
            return ProtocolResult.Failure(payloadError)
        }

        return ProtocolResult.Success(packet)
    }

    private fun validatePayload(type: PacketType, payload: Payload?): ProtocolError? {
        val expectedClass = when (type) {
            PacketType.PAIR_REQUEST -> PairRequestPayload::class
            PacketType.PAIR_RESPONSE -> PairResponsePayload::class
            PacketType.ACK -> AckPayload::class
            PacketType.ERROR -> ErrorPayload::class
            PacketType.DEVICE_INFO -> DeviceInfoPayload::class
            PacketType.ROLE_STATE -> RoleStatePayload::class
            PacketType.ROLE_CHANGE_REQUEST -> RoleChangeRequestPayload::class
            PacketType.MEDIA_COMMAND -> MediaCommandPayload::class
            PacketType.MEDIA_STATE -> MediaStatePayload::class
            PacketType.CALL_STATE -> CallStatePayload::class
            PacketType.CALL_COMMAND -> CallCommandPayload::class
            PacketType.BATTERY_STATE -> BatteryStatePayload::class
            PacketType.PERMISSION_STATE -> PermissionStatePayload::class
            PacketType.ARTWORK_REQUEST -> ArtworkRequestPayload::class
            PacketType.ARTWORK_RESPONSE -> ArtworkResponsePayload::class
            PacketType.HEARTBEAT -> HeartbeatPayload::class
            PacketType.AUTH_REQUEST -> AuthRequestPayload::class
            PacketType.AUTH_SUCCESS -> AuthSuccessPayload::class
            PacketType.NOTIFICATION_STATE -> NotificationStatePayload::class
            PacketType.LIMITED_MODE_STATE -> LimitedModeStatePayload::class
            PacketType.WAKE_REQUEST -> WakeRequestPayload::class

            // Types that don't strictly require a payload
            PacketType.PING,
            PacketType.PONG -> return null 
        }

        if (payload == null) {
            return ProtocolError.MissingPayload("Payload is required for packet type $type")
        }

        if (!expectedClass.isInstance(payload)) {
            return ProtocolError.PayloadTypeMismatch("Expected payload of type ${expectedClass.simpleName} but got ${payload::class.simpleName}")
        }

        return null
    }
}
