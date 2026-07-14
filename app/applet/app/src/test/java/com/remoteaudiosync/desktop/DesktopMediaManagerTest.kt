package com.remoteaudiosync.desktop

import com.remoteaudiosync.network.ConnectionState
import com.remoteaudiosync.network.ReliableChannel
import com.remoteaudiosync.network.WebSocketClient
import com.remoteaudiosync.crypto.CryptoManager
import com.remoteaudiosync.protocol.MediaCommandPayload
import com.remoteaudiosync.protocol.MediaStatePayload
import com.remoteaudiosync.protocol.Packet
import com.remoteaudiosync.protocol.PacketType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test
import java.util.UUID

class FakeReliableChannel(scope: CoroutineScope) : ReliableChannel(WebSocketClient(), CryptoManager(), scope) {
    override val incomingPackets = MutableSharedFlow<Packet>()
    private val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)
    override val connectionState: StateFlow<ConnectionState> = _connectionState
    val sentPackets = mutableListOf<Packet>()

    fun setConnectionState(state: ConnectionState) {
        _connectionState.value = state
    }

    override fun send(packet: Packet) {
        sentPackets.add(packet)
    }

    override suspend fun sendWithAck(packet: Packet): Boolean {
        sentPackets.add(packet)
        return true
    }
}

class MockDesktopMediaSessionObserver : DesktopMediaSessionObserver {
    var isSupportedValue = true
    var currentSessionState: MediaStatePayload? = null
    var callback: ((MediaStatePayload?) -> Unit)? = null
    var isObserving = false

    override fun startObserving(callback: (MediaStatePayload?) -> Unit) {
        this.callback = callback
        this.isObserving = true
    }

    override fun stopObserving() {
        this.callback = null
        this.isObserving = false
    }

    override fun isSupported(): Boolean = isSupportedValue
    override fun getCurrentState(): MediaStatePayload? = currentSessionState

    fun triggerStateChange(state: MediaStatePayload?) {
        callback?.invoke(state)
    }
}

class MockDesktopMediaCommandExecutor : DesktopMediaCommandExecutor {
    val executedCommands = mutableListOf<MediaCommandPayload>()
    var executeResult = true
    override fun executeCommand(command: MediaCommandPayload): Boolean {
        executedCommands.add(command)
        return executeResult
    }
}

class MockDesktopMediaStatePublisher : DesktopMediaStatePublisher {
    val publishedStates = mutableListOf<MediaStatePayload?>()
    override fun publishState(state: MediaStatePayload?) {
        publishedStates.add(state)
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class DesktopMediaManagerTest {

    @Test
    fun `test media observer starts and stops`() = runTest {
        val observer = MockDesktopMediaSessionObserver()
        val channel = FakeReliableChannel(backgroundScope)
        val executor = MockDesktopMediaCommandExecutor()
        val publisher = MockDesktopMediaStatePublisher()
        val manager = DesktopMediaManager(channel, observer, executor, publisher, backgroundScope)

        assertFalse(observer.isObserving)
        manager.setRole(isAudioOwner = true)
        assertTrue(observer.isObserving)

        manager.setRole(isAudioOwner = false)
        assertFalse(observer.isObserving)
        manager.cleanup()
    }

    @Test
    fun `test local command execution as ACTIVE_AUDIO_OWNER`() = runTest {
        val observer = MockDesktopMediaSessionObserver()
        val channel = FakeReliableChannel(backgroundScope)
        val executor = MockDesktopMediaCommandExecutor()
        val publisher = MockDesktopMediaStatePublisher()
        val manager = DesktopMediaManager(channel, observer, executor, publisher, backgroundScope)

        manager.setRole(isAudioOwner = true)
        
        manager.sendCommand("PLAY")
        assertEquals(1, executor.executedCommands.size)
        assertEquals("PLAY", executor.executedCommands[0].command)
        manager.cleanup()
    }

    @Test
    fun `test remote command routing as REMOTE_CONTROLLER`() = runTest {
        val observer = MockDesktopMediaSessionObserver()
        val channel = FakeReliableChannel(backgroundScope)
        val executor = MockDesktopMediaCommandExecutor()
        val publisher = MockDesktopMediaStatePublisher()
        val manager = DesktopMediaManager(channel, observer, executor, publisher, backgroundScope)

        manager.setRole(isAudioOwner = false)
        
        manager.sendCommand("PLAY")
        assertEquals(0, executor.executedCommands.size)
        assertEquals(1, channel.sentPackets.size)
        assertEquals(PacketType.MEDIA_COMMAND, channel.sentPackets[0].packetType)
        
        val payload = channel.sentPackets[0].payload as MediaCommandPayload
        assertEquals("PLAY", payload.command)
        manager.cleanup()
    }

    @Test
    fun `test MEDIA_STATE publishing`() = runTest {
        val observer = MockDesktopMediaSessionObserver()
        val channel = FakeReliableChannel(backgroundScope)
        val executor = MockDesktopMediaCommandExecutor()
        val publisher = MockDesktopMediaStatePublisher()
        val manager = DesktopMediaManager(channel, observer, executor, publisher, backgroundScope)

        manager.setRole(isAudioOwner = true)
        val state = MediaStatePayload("Test Title", "Test Artist", true, 1000L, 5000L, "TestApp", 0.8f, false)
        observer.triggerStateChange(state)

        assertEquals(state, manager.mediaState.value)
        assertTrue(publisher.publishedStates.contains(state))
        manager.cleanup()
    }

    @Test
    fun `test reconnect synchronization`() = runTest {
        val observer = MockDesktopMediaSessionObserver()
        val channel = FakeReliableChannel(backgroundScope)
        val executor = MockDesktopMediaCommandExecutor()
        val publisher = MockDesktopMediaStatePublisher()
        val manager = DesktopMediaManager(channel, observer, executor, publisher, backgroundScope)

        manager.setRole(isAudioOwner = true)
        val state = MediaStatePayload("Initial Title", "Artist", true, 0L, 10000L, "App")
        observer.currentSessionState = state
        observer.triggerStateChange(state)

        publisher.publishedStates.clear()
        channel.setConnectionState(ConnectionState.Connected)
        kotlinx.coroutines.delay(10)

        assertTrue(publisher.publishedStates.contains(state))
        manager.cleanup()
    }

    @Test
    fun `test no active session`() = runTest {
        val observer = MockDesktopMediaSessionObserver()
        val channel = FakeReliableChannel(backgroundScope)
        val executor = MockDesktopMediaCommandExecutor()
        val publisher = DefaultDesktopMediaStatePublisher(channel)
        val manager = DesktopMediaManager(channel, observer, executor, publisher, backgroundScope)

        manager.setRole(isAudioOwner = true)
        
        observer.currentSessionState = null
        observer.triggerStateChange(null)
        kotlinx.coroutines.delay(10)

        val nullStatePackets = channel.sentPackets.filter { it.packetType == PacketType.MEDIA_STATE }
        assertTrue(nullStatePackets.isNotEmpty())
        val lastPayload = nullStatePackets.last().payload as MediaStatePayload
        assertEquals("NO_ACTIVE_MEDIA_SESSION", lastPayload.title)
        manager.cleanup()
    }

    @Test
    fun `test unsupported platform capability`() = runTest {
        val observer = MockDesktopMediaSessionObserver()
        observer.isSupportedValue = false
        
        val channel = FakeReliableChannel(backgroundScope)
        val executor = MockDesktopMediaCommandExecutor()
        val publisher = DefaultDesktopMediaStatePublisher(channel)
        val manager = DesktopMediaManager(channel, observer, executor, publisher, backgroundScope)

        manager.setRole(isAudioOwner = true)
        
        val state = observer.getCurrentState()
        assertNull(state)
        
        publisher.publishState(state)
        val lastPacket = channel.sentPackets.last { it.packetType == PacketType.MEDIA_STATE }
        val lastPayload = lastPacket.payload as MediaStatePayload
        assertEquals("NO_ACTIVE_MEDIA_SESSION", lastPayload.title)
        manager.cleanup()
    }
}
