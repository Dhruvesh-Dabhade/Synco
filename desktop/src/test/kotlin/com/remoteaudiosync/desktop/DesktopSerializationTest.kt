package com.remoteaudiosync.desktop

import com.remoteaudiosync.protocol.*
import org.junit.Test
import org.junit.Assert.*

class DesktopSerializationTest {
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
        if (res is ProtocolResult.Failure) {
            println("Serialization failed: " + res.error)
        }
        assertTrue(res is ProtocolResult.Success)
    }
}
