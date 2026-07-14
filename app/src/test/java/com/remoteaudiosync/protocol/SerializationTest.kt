package com.remoteaudiosync.protocol

import org.junit.Test
import org.junit.Assert.*

class SerializationTest {
    @Test
    fun testHeartbeatSerialization() {
        val packet = Packet(
            version = 1,
            id = "1",
            timestamp = 100,
            senderId = "A",
            receiverId = "B",
            packetType = PacketType.HEARTBEAT,
            payload = HeartbeatPayload(uptimeMillis = 100)
        )
        val res = PacketCodec.serialize(packet)
        assertTrue(res is ProtocolResult.Success)
        val jsonString = (res as ProtocolResult.Success).data
        val dec = PacketCodec.deserialize(jsonString)
        assertTrue(dec is ProtocolResult.Success)
    }
}
