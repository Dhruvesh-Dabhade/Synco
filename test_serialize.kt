package com.remoteaudiosync.protocol

fun main() {
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
    println("Serialized: $res")
    if (res is ProtocolResult.Success) {
        val dec = PacketCodec.deserialize(res.data)
        println("Deserialized: $dec")
    }
}
