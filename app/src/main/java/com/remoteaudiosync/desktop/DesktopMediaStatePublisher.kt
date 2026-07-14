package com.remoteaudiosync.desktop

import com.remoteaudiosync.network.ReliableChannel
import com.remoteaudiosync.protocol.MediaStatePayload
import com.remoteaudiosync.protocol.Packet
import com.remoteaudiosync.protocol.PacketType
import java.util.UUID

interface DesktopMediaStatePublisher {
    fun publishState(state: MediaStatePayload?)
}

class DefaultDesktopMediaStatePublisher(
    private val reliableChannel: ReliableChannel
) : DesktopMediaStatePublisher {
    
    override fun publishState(state: MediaStatePayload?) {
        if (!reliableChannel.isAuthenticated.value) return
        val payload = state ?: MediaStatePayload("NO_ACTIVE_MEDIA_SESSION", "", false, 0, 0, "")
        val packet = Packet(
            version = 1,
            id = UUID.randomUUID().toString(),
            timestamp = System.currentTimeMillis(),
            senderId = "desktop-server",
            receiverId = "android-client",
            packetType = PacketType.MEDIA_STATE,
            payload = payload
        )
        reliableChannel.send(packet)
    }
}
