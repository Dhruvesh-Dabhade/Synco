package com.remoteaudiosync.artwork

import com.remoteaudiosync.network.ReliableChannel
import com.remoteaudiosync.protocol.ArtworkResponsePayload
import com.remoteaudiosync.protocol.ErrorPayload
import com.remoteaudiosync.protocol.Packet
import com.remoteaudiosync.protocol.PacketType
import java.util.UUID

class ArtworkRequestHandler(
    private val reliableChannel: ReliableChannel,
    private val cache: ArtworkCache,
    private val provider: ArtworkProvider?
) {
    suspend fun handleArtworkRequest(artworkId: String, senderId: String, receiverId: String) {
        var bytes = cache.get(artworkId)
        if (bytes == null && provider != null) {
            bytes = provider.getArtwork(artworkId)
        }

        if (bytes == null) {
            sendError(
                code = "ARTWORK_NOT_FOUND",
                message = "Artwork not found for ID: $artworkId",
                senderId = receiverId,
                receiverId = senderId
            )
            return
        }

        if (bytes.isEmpty()) {
            sendError(
                code = "ARTWORK_CORRUPT",
                message = "Artwork bytes are empty for ID: $artworkId",
                senderId = receiverId,
                receiverId = senderId
            )
            return
        }

        if (bytes.size > ArtworkCache.MAX_SIZE_BYTES) {
            bytes = ArtworkCache.downscaleIfNeeded(bytes)
            if (bytes.size > ArtworkCache.MAX_SIZE_BYTES) {
                sendError(
                    code = "ARTWORK_OVERSIZED",
                    message = "Artwork exceeds 256 KB limit even after downscaling",
                    senderId = receiverId,
                    receiverId = senderId
                )
                return
            }
        }

        val base64Data = java.util.Base64.getEncoder().encodeToString(bytes)
        val responsePacket = Packet(
            version = 1,
            id = UUID.randomUUID().toString(),
            timestamp = System.currentTimeMillis(),
            senderId = receiverId,
            receiverId = senderId,
            packetType = PacketType.ARTWORK_RESPONSE,
            payload = ArtworkResponsePayload(mediaId = artworkId, artworkBase64 = base64Data)
        )
        reliableChannel.sendWithAck(responsePacket)
    }

    private fun sendError(code: String, message: String, senderId: String, receiverId: String) {
        val errorPacket = Packet(
            version = 1,
            id = UUID.randomUUID().toString(),
            timestamp = System.currentTimeMillis(),
            senderId = senderId,
            receiverId = receiverId,
            packetType = PacketType.ERROR,
            payload = ErrorPayload(code, message)
        )
        reliableChannel.send(errorPacket)
    }
}
