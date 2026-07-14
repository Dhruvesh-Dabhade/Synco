package com.remoteaudiosync.artwork

import com.remoteaudiosync.network.ReliableChannel
import com.remoteaudiosync.protocol.ArtworkRequestPayload
import com.remoteaudiosync.protocol.Packet
import com.remoteaudiosync.protocol.PacketType
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class ArtworkDownloader(
    private val reliableChannel: ReliableChannel,
    private val cache: ArtworkCache,
    private val coroutineScope: CoroutineScope
) {
    private val activeDownloads = ConcurrentHashMap<String, CompletableDeferred<ByteArray?>>()

    suspend fun downloadArtwork(artworkId: String): ByteArray? {
        cache.get(artworkId)?.let { return it }

        var deferred = activeDownloads[artworkId]
        var isNewDownload = false
        if (deferred == null) {
            deferred = CompletableDeferred()
            val existing = activeDownloads.putIfAbsent(artworkId, deferred)
            if (existing != null) {
                deferred = existing
            } else {
                isNewDownload = true
            }
        }

        if (isNewDownload) {
            coroutineScope.launch {
                try {
                    val packet = Packet(
                        version = 1,
                        id = UUID.randomUUID().toString(),
                        timestamp = System.currentTimeMillis(),
                        senderId = "android-client",
                        receiverId = "desktop-server",
                        packetType = PacketType.ARTWORK_REQUEST,
                        payload = ArtworkRequestPayload(mediaId = artworkId)
                    )
                    
                    val sentSuccessfully = reliableChannel.sendWithAck(packet)
                    if (!sentSuccessfully) {
                        deferred.complete(null)
                        activeDownloads.remove(artworkId)
                        return@launch
                    }

                    val result = withTimeoutOrNull(5000L) {
                        deferred.await()
                    }
                    if (result == null) {
                        deferred.complete(null)
                        activeDownloads.remove(artworkId)
                    }
                } catch (e: Exception) {
                    deferred.complete(null)
                    activeDownloads.remove(artworkId)
                }
            }
        }

        val bytes = deferred.await()
        if (bytes != null) {
            cache.put(artworkId, bytes)
        }
        return bytes
    }

    fun handleArtworkResponse(artworkId: String, bytes: ByteArray?) {
        val deferred = activeDownloads.remove(artworkId)
        deferred?.complete(bytes)
    }

    fun isDownloading(artworkId: String): Boolean {
        return activeDownloads.containsKey(artworkId)
    }
}
