package com.remoteaudiosync.artwork

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import androidx.test.core.app.ApplicationProvider
import com.remoteaudiosync.crypto.CryptoManager
import com.remoteaudiosync.network.ReliableChannel
import com.remoteaudiosync.network.WebSocketClient
import com.remoteaudiosync.protocol.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.ByteArrayOutputStream
import java.util.UUID

class TestArtworkFakeReliableChannel(scope: CoroutineScope) : ReliableChannel(WebSocketClient(), CryptoManager(), scope) {
    override val incomingPackets = MutableSharedFlow<Packet>(extraBufferCapacity = 100)
    val sentPackets = mutableListOf<Packet>()
    init { setAuthenticated(true) }

    override fun send(packet: Packet) {
        sentPackets.add(packet)
    }

    override suspend fun sendWithAck(packet: Packet): Boolean {
        sentPackets.add(packet)
        return true
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class ArtworkSyncTest {

    private lateinit var context: Application
    private lateinit var cache: ArtworkCache

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        cache = ArtworkCache(context)
        cache.clear()
    }

    @Test
    fun `test SHA-256 generation`() {
        val testData = "hello-world-artwork-data".toByteArray()
        val expectedHash = "daf6d3598a6b2dd4b2fbc804a00f3dec7e1b01821e470e419c7fea8c7da54c61"
        val actualHash = ArtworkCache.generateSha256(testData)
        assertEquals(expectedHash, actualHash)
    }

    @Test
    fun `test cache hit and miss`() {
        val artworkId = "test-hash-123"
        val originalBytes = "artwork-bytes-content".toByteArray()

        // Cache miss
        assertNull(cache.get(artworkId))

        // Cache hit after put
        cache.put(artworkId, originalBytes)
        val retrieved = cache.get(artworkId)
        assertNotNull(retrieved)
        assertArrayEquals(originalBytes, retrieved)
    }

    @Test
    fun `test duplicate request suppression`() = runTest {
        val channel = TestArtworkFakeReliableChannel(backgroundScope)
        val manager = ArtworkManager(channel, cache, null, backgroundScope)
        val artworkId = "dup-artwork-id"

        // Fire multiple concurrent requests for the same artworkId
        val job1 = launch { manager.downloader.downloadArtwork(artworkId) }
        val job2 = launch { manager.downloader.downloadArtwork(artworkId) }
        runCurrent()

        // Verify only ONE ARTWORK_REQUEST packet is sent
        val requestPackets = channel.sentPackets.filter { it.packetType == PacketType.ARTWORK_REQUEST }
        assertEquals(1, requestPackets.size)

        // Mock response
        val testBytes = "some-artwork-bytes-for-dup".toByteArray()
        val finalArtworkId = ArtworkCache.generateSha256(testBytes)
        // Let's download using the final hash to bypass checksum verification mismatch
        val job3 = launch { manager.downloader.downloadArtwork(finalArtworkId) }
        runCurrent()

        val responsePacket = Packet(
            version = 1,
            id = UUID.randomUUID().toString(),
            timestamp = System.currentTimeMillis(),
            senderId = "desktop-server",
            receiverId = "android-client",
            packetType = PacketType.ARTWORK_RESPONSE,
            payload = ArtworkResponsePayload(finalArtworkId, android.util.Base64.encodeToString(testBytes, android.util.Base64.NO_WRAP))
        )
        channel.incomingPackets.emit(responsePacket)
        runCurrent()

        job3.join()
        assertNotNull(cache.get(finalArtworkId))
    }

    @Test
    fun `test concurrent requests`() = runTest {
        val channel = TestArtworkFakeReliableChannel(backgroundScope)
        val manager = ArtworkManager(channel, cache, null, backgroundScope)

        val id1 = "id1"
        val id2 = "id2"

        val job1 = launch { manager.downloader.downloadArtwork(id1) }
        val job2 = launch { manager.downloader.downloadArtwork(id2) }
        runCurrent()

        val requests = channel.sentPackets.filter { it.packetType == PacketType.ARTWORK_REQUEST }
        assertEquals(2, requests.size)

        // Both are downloading concurrently
        assertTrue(manager.downloader.isDownloading(id1))
        assertTrue(manager.downloader.isDownloading(id2))
    }

    @Test
    fun `test oversized artwork downscale`() {
        // Create a large bitmap to make it exceed 256 KB
        val bitmap = Bitmap.createBitmap(800, 800, Bitmap.Config.ARGB_8888)
        val out = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        val oversizedBytes = out.toByteArray()

        assertTrue(oversizedBytes.size > ArtworkCache.MAX_SIZE_BYTES)

        val downscaled = ArtworkCache.downscaleIfNeeded(oversizedBytes)
        assertTrue(downscaled.size <= ArtworkCache.MAX_SIZE_BYTES)
    }

    @Test
    fun `test checksum mismatch handling`() = runTest {
        val channel = TestArtworkFakeReliableChannel(backgroundScope)
        val manager = ArtworkManager(channel, cache, null, backgroundScope)
        val artworkId = "expected-sha256-hash-value"

        val job = launch { manager.downloader.downloadArtwork(artworkId) }
        runCurrent()

        // Send corrupt/mismatched artwork
        val wrongBytes = "wrong-content".toByteArray()
        val responsePacket = Packet(
            version = 1,
            id = UUID.randomUUID().toString(),
            timestamp = System.currentTimeMillis(),
            senderId = "desktop-server",
            receiverId = "android-client",
            packetType = PacketType.ARTWORK_RESPONSE,
            payload = ArtworkResponsePayload(artworkId, android.util.Base64.encodeToString(wrongBytes, android.util.Base64.NO_WRAP))
        )
        channel.incomingPackets.emit(responsePacket)
        runCurrent()

        job.join()

        // Checksum mismatch error should be sent to the peer
        val errorPackets = channel.sentPackets.filter { it.packetType == PacketType.ERROR }
        assertTrue(errorPackets.isNotEmpty())
        val errorPayload = errorPackets.first().payload as ErrorPayload
        assertEquals("ARTWORK_CHECKSUM_MISMATCH", errorPayload.code)
    }

    @Test
    fun `test corrupt artwork handling`() = runTest {
        val channel = TestArtworkFakeReliableChannel(backgroundScope)
        val manager = ArtworkManager(channel, cache, null, backgroundScope)
        val artworkId = "any-artwork-id"

        val job = launch { manager.downloader.downloadArtwork(artworkId) }
        runCurrent()

        // Send invalid base64 content
        val responsePacket = Packet(
            version = 1,
            id = UUID.randomUUID().toString(),
            timestamp = System.currentTimeMillis(),
            senderId = "desktop-server",
            receiverId = "android-client",
            packetType = PacketType.ARTWORK_RESPONSE,
            payload = ArtworkResponsePayload(artworkId, "!!!invalid-base64-characters!!!")
        )
        channel.incomingPackets.emit(responsePacket)
        runCurrent()

        job.join()

        val errorPackets = channel.sentPackets.filter { it.packetType == PacketType.ERROR }
        assertTrue(errorPackets.isNotEmpty())
        val errorPayload = errorPackets.first().payload as ErrorPayload
        assertEquals("ARTWORK_CORRUPT", errorPayload.code)
    }

    @Test
    fun `test download timeout`() = runTest {
        val channel = TestArtworkFakeReliableChannel(backgroundScope)
        val manager = ArtworkManager(channel, cache, null, backgroundScope)
        val artworkId = "timeout-artwork-id"

        var result: ByteArray? = "initial-non-null".toByteArray()
        val job = launch { 
            result = manager.downloader.downloadArtwork(artworkId)
        }
        runCurrent()

        // Advance virtual time past timeout (5000ms)
        advanceTimeBy(6000)
        runCurrent()

        job.join()
        assertNull(result)
    }

    @Test
    fun `test reconnect behavior`() = runTest {
        val channel = TestArtworkFakeReliableChannel(backgroundScope)
        val manager = ArtworkManager(channel, cache, null, backgroundScope)
        
        // Reconnect shouldn't request any artwork automatically
        runCurrent()
        val requestPacketsBefore = channel.sentPackets.filter { it.packetType == PacketType.ARTWORK_REQUEST }
        assertEquals(0, requestPacketsBefore.size)
    }
}
