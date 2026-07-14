package com.remoteaudiosync.protocol

import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.UUID

class PacketValidatorTest {

    private fun createValidPacket(
        type: PacketType = PacketType.PING,
        payload: Payload? = null
    ): Packet {
        return Packet(
            version = PacketValidator.CURRENT_VERSION,
            id = UUID.randomUUID().toString(),
            timestamp = System.currentTimeMillis(),
            senderId = "desktop-1",
            receiverId = "phone-1",
            packetType = type,
            payload = payload
        )
    }

    @Test
    fun `validate returns success for valid packet`() {
        val packet = createValidPacket(
            type = PacketType.PAIR_REQUEST,
            payload = PairRequestPayload("Desktop", "pubKey")
        )
        val result = PacketValidator.validate(packet)
        assertTrue(result is ProtocolResult.Success)
    }

    @Test
    fun `validate fails for invalid version`() {
        val packet = createValidPacket().copy(version = 0)
        val result = PacketValidator.validate(packet)
        assertTrue(result is ProtocolResult.Failure)
        assertTrue((result as ProtocolResult.Failure).error is ProtocolError.InvalidVersion)
    }

    @Test
    fun `validate fails for invalid packet id format`() {
        val packet = createValidPacket().copy(id = "invalid-uuid")
        val result = PacketValidator.validate(packet)
        assertTrue(result is ProtocolResult.Failure)
        assertTrue((result as ProtocolResult.Failure).error is ProtocolError.InvalidPacketId)
    }

    @Test
    fun `validate fails for invalid timestamp`() {
        val packet = createValidPacket().copy(timestamp = 0L)
        val result = PacketValidator.validate(packet)
        assertTrue(result is ProtocolResult.Failure)
        assertTrue((result as ProtocolResult.Failure).error is ProtocolError.InvalidTimestamp)
    }

    @Test
    fun `validate fails for blank sender`() {
        val packet = createValidPacket().copy(senderId = "  ")
        val result = PacketValidator.validate(packet)
        assertTrue(result is ProtocolResult.Failure)
        assertTrue((result as ProtocolResult.Failure).error is ProtocolError.InvalidSender)
    }

    @Test
    fun `validate fails for blank receiver`() {
        val packet = createValidPacket().copy(receiverId = "")
        val result = PacketValidator.validate(packet)
        assertTrue(result is ProtocolResult.Failure)
        assertTrue((result as ProtocolResult.Failure).error is ProtocolError.InvalidReceiver)
    }

    @Test
    fun `validate fails for missing payload`() {
        val packet = createValidPacket(type = PacketType.PAIR_REQUEST, payload = null)
        val result = PacketValidator.validate(packet)
        assertTrue(result is ProtocolResult.Failure)
        assertTrue((result as ProtocolResult.Failure).error is ProtocolError.MissingPayload)
    }

    @Test
    fun `validate fails for payload type mismatch`() {
        val packet = createValidPacket(
            type = PacketType.PAIR_REQUEST,
            payload = AckPayload("some-id") // Wrong payload
        )
        val result = PacketValidator.validate(packet)
        assertTrue(result is ProtocolResult.Failure)
        assertTrue((result as ProtocolResult.Failure).error is ProtocolError.PayloadTypeMismatch)
    }

    @Test
    fun `validate allows null payload for PING`() {
        val packet = createValidPacket(type = PacketType.PING, payload = null)
        val result = PacketValidator.validate(packet)
        assertTrue(result is ProtocolResult.Success)
    }
}
