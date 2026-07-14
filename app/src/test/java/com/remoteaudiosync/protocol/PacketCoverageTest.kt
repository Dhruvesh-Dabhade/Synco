package com.remoteaudiosync.protocol

import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.UUID

class PacketCoverageTest {
    @Test
    fun `test all payload types validate correctly`() {
        val pairs = listOf(
            PacketType.PAIR_REQUEST to PairRequestPayload("dev", "key"),
            PacketType.PAIR_RESPONSE to PairResponsePayload("ok", "key"),
            PacketType.AUTH_REQUEST to AuthRequestPayload("token", "sig"),
            PacketType.AUTH_SUCCESS to AuthSuccessPayload("session", "sig", "sess_id"),
            PacketType.ACK to AckPayload("id"),
            PacketType.ERROR to ErrorPayload("400", "bad"),
            PacketType.DEVICE_INFO to DeviceInfoPayload("mod", "os", "app"),
            PacketType.ROLE_STATE to RoleStatePayload(true),
            PacketType.ROLE_CHANGE_REQUEST to RoleChangeRequestPayload(false),
            PacketType.MEDIA_COMMAND to MediaCommandPayload("play"),
            PacketType.MEDIA_STATE to MediaStatePayload("title", "art", true, 0, 100),
            PacketType.CALL_STATE to CallStatePayload("idle", null),
            PacketType.CALL_COMMAND to CallCommandPayload("answer"),
            PacketType.BATTERY_STATE to BatteryStatePayload(100, false),
            PacketType.PERMISSION_STATE to PermissionStatePayload(emptyList(), emptyList()),
            PacketType.NOTIFICATION_STATE to NotificationStatePayload("id", "t", "txt"),
            PacketType.ARTWORK_REQUEST to ArtworkRequestPayload("med"),
            PacketType.ARTWORK_RESPONSE to ArtworkResponsePayload("med", "b64"),
            PacketType.HEARTBEAT to HeartbeatPayload(1000L),
            PacketType.WAKE_REQUEST to WakeRequestPayload("rsn"),
            PacketType.LIMITED_MODE_STATE to LimitedModeStatePayload(true, "rsn")
        )

        for ((type, payload) in pairs) {
            val packet = Packet(
                version = 1,
                id = UUID.randomUUID().toString(),
                timestamp = System.currentTimeMillis(),
                senderId = "s",
                receiverId = "r",
                packetType = type,
                payload = payload
            )
            val result = PacketValidator.validate(packet)
            assertTrue("Validation failed for $type: ${if (result is ProtocolResult.Failure) result.error else ""}", result is ProtocolResult.Success)
            
            // Also test serialization coverage
            val ser = PacketCodec.serialize(packet)
            assertTrue(ser is ProtocolResult.Success)
            val des = PacketCodec.deserialize((ser as ProtocolResult.Success).data)
            assertTrue(des is ProtocolResult.Success)
        }
    }
}
