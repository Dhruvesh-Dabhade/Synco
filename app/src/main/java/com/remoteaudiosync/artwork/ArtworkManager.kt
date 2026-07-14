package com.remoteaudiosync.artwork

import com.remoteaudiosync.network.ReliableChannel
import com.remoteaudiosync.protocol.ArtworkRequestPayload
import com.remoteaudiosync.protocol.ArtworkResponsePayload
import com.remoteaudiosync.protocol.ErrorPayload
import com.remoteaudiosync.protocol.MediaStatePayload
import com.remoteaudiosync.protocol.Packet
import com.remoteaudiosync.protocol.PacketType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

class ArtworkManager(
    private val reliableChannel: ReliableChannel,
    val cache: ArtworkCache,
    private val provider: ArtworkProvider?,
    private val coroutineScope: CoroutineScope
) {
    val downloader = ArtworkDownloader(reliableChannel, cache, coroutineScope)
    val requestHandler = ArtworkRequestHandler(reliableChannel, cache, provider)

    private val _currentArtwork = MutableStateFlow<ByteArray?>(null)
    val currentArtwork: StateFlow<ByteArray?> = _currentArtwork.asStateFlow()

    private var packetJob: Job? = null

    init {
        startListening()
    }

    private fun startListening() {
        packetJob?.cancel()
        packetJob = coroutineScope.launch {
            reliableChannel.incomingPackets.collect { packet ->
                when (packet.packetType) {
                    PacketType.ARTWORK_REQUEST -> {
                        val payload = packet.payload as? ArtworkRequestPayload
                        if (payload != null) {
                            requestHandler.handleArtworkRequest(
                                artworkId = payload.mediaId,
                                senderId = packet.senderId,
                                receiverId = packet.receiverId
                            )
                        }
                    }
                    PacketType.ARTWORK_RESPONSE -> {
                        val payload = packet.payload as? ArtworkResponsePayload
                        if (payload != null) {
                            handleArtworkResponse(payload, packet.senderId, packet.receiverId)
                        }
                    }
                    PacketType.MEDIA_STATE -> {
                        val payload = packet.payload as? MediaStatePayload
                        if (payload != null) {
                            handleMediaStateReceived(payload)
                        }
                    }
                    else -> {}
                }
            }
        }
    }

    private fun handleArtworkResponse(payload: ArtworkResponsePayload, senderId: String, receiverId: String) {
        val artworkId = payload.mediaId
        val base64Data = payload.artworkBase64
        
        if (base64Data.isEmpty()) {
            sendError("ARTWORK_CORRUPT", "Artwork base64 data is empty", receiverId, senderId)
            downloader.handleArtworkResponse(artworkId, null)
            return
        }

        val isBase64 = base64Data.all { it.isLetterOrDigit() || it == '+' || it == '/' || it == '=' || it.isWhitespace() }
        if (!isBase64) {
            sendError("ARTWORK_CORRUPT", "Artwork base64 contains invalid characters", receiverId, senderId)
            downloader.handleArtworkResponse(artworkId, null)
            return
        }

        val bytes = try {
            java.util.Base64.getDecoder().decode(base64Data)
        } catch (e: Exception) {
            sendError("ARTWORK_CORRUPT", "Failed to decode artwork base64: ${e.message}", receiverId, senderId)
            downloader.handleArtworkResponse(artworkId, null)
            return
        }

        if (bytes == null || bytes.isEmpty()) {
            sendError("ARTWORK_CORRUPT", "Decoded artwork bytes are empty or corrupt", receiverId, senderId)
            downloader.handleArtworkResponse(artworkId, null)
            return
        }

        val receivedHash = ArtworkCache.generateSha256(bytes)
        if (receivedHash != artworkId) {
            sendError("ARTWORK_CHECKSUM_MISMATCH", "Hash mismatch: expected $artworkId, got $receivedHash", receiverId, senderId)
            downloader.handleArtworkResponse(artworkId, null)
            return
        }

        downloader.handleArtworkResponse(artworkId, bytes)
        _currentArtwork.value = bytes
    }

    private fun handleMediaStateReceived(payload: MediaStatePayload) {
        val artworkId = payload.artworkId
        val available = payload.artworkAvailable

        if (artworkId != null && available) {
            coroutineScope.launch {
                val bytes = downloader.downloadArtwork(artworkId)
                if (bytes != null) {
                    _currentArtwork.value = bytes
                }
            }
        } else {
            _currentArtwork.value = null
        }
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

    fun cleanup() {
        packetJob?.cancel()
    }
}
