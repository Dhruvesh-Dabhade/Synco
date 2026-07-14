package com.remoteaudiosync.protocol

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.UUID

class PacketCodecTest {

    @Test
    fun `serialize and deserialize packet with payload`() {
        val payload = PairRequestPayload(deviceName = "TestDevice", publicKey = "key123")
        val packet = Packet(
            version = 1,
            id = UUID.randomUUID().toString(),
            timestamp = System.currentTimeMillis(),
            senderId = "sender",
            receiverId = "receiver",
            packetType = PacketType.PAIR_REQUEST,
            payload = payload
        )

        val serializedResult = PacketCodec.serialize(packet)
        assertTrue(serializedResult is ProtocolResult.Success)
        val json = (serializedResult as ProtocolResult.Success).data

        val deserializedResult = PacketCodec.deserialize(json)
        assertTrue(deserializedResult is ProtocolResult.Success)
        val deserializedPacket = (deserializedResult as ProtocolResult.Success).data

        assertEquals(packet.id, deserializedPacket.id)
        assertEquals(packet.packetType, deserializedPacket.packetType)
        assertTrue(deserializedPacket.payload is PairRequestPayload)
        
        val deserializedPayload = deserializedPacket.payload as PairRequestPayload
        assertEquals("TestDevice", deserializedPayload.deviceName)
        assertEquals("key123", deserializedPayload.publicKey)
    }

    @Test
    fun `serialize and deserialize packet without payload`() {
        val packet = Packet(
            version = 1,
            id = UUID.randomUUID().toString(),
            timestamp = 1000L,
            senderId = "a",
            receiverId = "b",
            packetType = PacketType.PING,
            payload = null
        )

        val serializedResult = PacketCodec.serialize(packet)
        assertTrue(serializedResult is ProtocolResult.Success)
        val json = (serializedResult as ProtocolResult.Success).data

        val deserializedResult = PacketCodec.deserialize(json)
        assertTrue(deserializedResult is ProtocolResult.Success)
        val deserializedPacket = (deserializedResult as ProtocolResult.Success).data

        assertEquals(packet.id, deserializedPacket.id)
        assertEquals(null, deserializedPacket.payload)
    }

    @Test
    fun `deserialize fails with invalid JSON`() {
        val invalidJson = "{ \"version\": 1, \"id\": \"123\" " // Malformed
        val result = PacketCodec.deserialize(invalidJson)
        assertTrue(result is ProtocolResult.Failure)
        assertTrue((result as ProtocolResult.Failure).error is ProtocolError.MalformedPacket)
    }

    @Test
    fun `serialize generates unique IDs for different packets`() {
        val id1 = UUID.randomUUID().toString()
        val id2 = UUID.randomUUID().toString()
        assertTrue(id1 != id2)
    }
}
