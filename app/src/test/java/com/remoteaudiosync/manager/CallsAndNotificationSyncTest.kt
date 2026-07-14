package com.remoteaudiosync.manager

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.remoteaudiosync.desktop.DesktopCallManager
import com.remoteaudiosync.desktop.DesktopNotificationManager
import com.remoteaudiosync.network.ConnectionState
import com.remoteaudiosync.network.ReliableChannel
import com.remoteaudiosync.network.WebSocketClient
import com.remoteaudiosync.crypto.CryptoManager
import com.remoteaudiosync.protocol.CallStatePayload
import com.remoteaudiosync.protocol.NotificationStatePayload
import com.remoteaudiosync.protocol.Packet
import com.remoteaudiosync.protocol.PacketType
import com.remoteaudiosync.service.MediaNotificationListenerService
import com.remoteaudiosync.service.MyInCallService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.UUID

class TestFakeReliableChannel(scope: CoroutineScope) : ReliableChannel(WebSocketClient(), CryptoManager(), scope) {
    override val incomingPackets = MutableSharedFlow<Packet>()
    private val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)
    override val connectionState: StateFlow<ConnectionState> = _connectionState
    val sentPackets = mutableListOf<Packet>()
    init { setAuthenticated(true) }
    var mockSendWithAckResult = true

    fun setConnectionState(state: ConnectionState) {
        _connectionState.value = state
    }

    override fun send(packet: Packet) {
        sentPackets.add(packet)
        // Also feed into incomingPackets to simulate loopback where needed
        incomingPackets.tryEmit(packet)
    }

    override suspend fun sendWithAck(packet: Packet): Boolean {
        sentPackets.add(packet)
        incomingPackets.emit(packet)
        return mockSendWithAckResult
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class CallsAndNotificationSyncTest {

    private lateinit var context: Context
    private lateinit var channel: TestFakeReliableChannel
    private lateinit var stateManager: DefaultAudioOwnerStateManager

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
    }

    @Test
    fun `test incoming call`() = runTest {
        channel = TestFakeReliableChannel(backgroundScope)
        stateManager = DefaultAudioOwnerStateManager("android-client", AudioRole.ACTIVE_AUDIO_OWNER)

        val callManager = DefaultAndroidCallManager(
            context,
            channel,
            stateManager,
            backgroundScope,
            checkPermissions = { true }
        )

        callManager.start()
        runCurrent()

        // Bypass final class mock restrictions by calling helper handlers directly
        callManager.handleCallAdded(android.telecom.Call.STATE_RINGING, "123456789")
        runCurrent()

        assertEquals("ringing", callManager.callState.value)
        assertEquals("123456789", callManager.callerId.value)

        // Verify sent packet
        val sentCallPacket = channel.sentPackets.lastOrNull { it.packetType == PacketType.CALL_STATE }
        assertNotNull(sentCallPacket)
        val payload = sentCallPacket!!.payload as CallStatePayload
        assertEquals("ringing", payload.state)
        assertEquals("123456789", payload.callerId)

        callManager.stop()
    }

    @Test
    fun `test answer`() = runTest {
        channel = TestFakeReliableChannel(backgroundScope)
        stateManager = DefaultAudioOwnerStateManager("android-client", AudioRole.ACTIVE_AUDIO_OWNER)

        val callManager = DefaultAndroidCallManager(
            context,
            channel,
            stateManager,
            backgroundScope,
            checkPermissions = { true }
        )

        callManager.start()
        callManager.executeCommand("ANSWER")
        runCurrent()

        assertEquals("answered", callManager.callState.value)

        val lastPacket = channel.sentPackets.lastOrNull { it.packetType == PacketType.CALL_STATE }
        assertNotNull(lastPacket)
        assertEquals("answered", (lastPacket!!.payload as CallStatePayload).state)

        callManager.stop()
    }

    @Test
    fun `test reject`() = runTest {
        channel = TestFakeReliableChannel(backgroundScope)
        stateManager = DefaultAudioOwnerStateManager("android-client", AudioRole.ACTIVE_AUDIO_OWNER)

        val callManager = DefaultAndroidCallManager(
            context,
            channel,
            stateManager,
            backgroundScope,
            checkPermissions = { true }
        )
        callManager.start()
        callManager.executeCommand("REJECT")
        runCurrent()

        assertEquals("rejected", callManager.callState.value)
        val lastPacket = channel.sentPackets.lastOrNull { it.packetType == PacketType.CALL_STATE }
        assertNotNull(lastPacket)
        assertEquals("rejected", (lastPacket!!.payload as CallStatePayload).state)

        callManager.stop()
    }

    @Test
    fun `test end`() = runTest {
        channel = TestFakeReliableChannel(backgroundScope)
        stateManager = DefaultAudioOwnerStateManager("android-client", AudioRole.ACTIVE_AUDIO_OWNER)

        val callManager = DefaultAndroidCallManager(
            context,
            channel,
            stateManager,
            backgroundScope,
            checkPermissions = { true }
        )
        callManager.start()
        callManager.executeCommand("END")
        runCurrent()

        assertEquals("ended", callManager.callState.value)
        assertNull(callManager.callerId.value)

        val lastPacket = channel.sentPackets.lastOrNull { it.packetType == PacketType.CALL_STATE }
        assertNotNull(lastPacket)
        assertEquals("ended", (lastPacket!!.payload as CallStatePayload).state)

        callManager.stop()
    }

    @Test
    fun `test missed call`() = runTest {
        channel = TestFakeReliableChannel(backgroundScope)
        stateManager = DefaultAudioOwnerStateManager("android-client", AudioRole.ACTIVE_AUDIO_OWNER)

        val callManager = DefaultAndroidCallManager(
            context,
            channel,
            stateManager,
            backgroundScope,
            checkPermissions = { true }
        )

        callManager.start()
        
        callManager.handleCallAdded(android.telecom.Call.STATE_RINGING, "987654321")
        runCurrent()
        assertEquals("ringing", callManager.callState.value)

        // Transition directly to disconnected/removed (indicating missed)
        callManager.handleCallRemoved()
        runCurrent()

        assertEquals("ended", callManager.callState.value)
        val lastPacket = channel.sentPackets.last { it.packetType == PacketType.CALL_STATE }
        assertEquals("ended", (lastPacket.payload as CallStatePayload).state)

        callManager.stop()
    }

    @Test
    fun `test notification add`() = runTest {
        channel = TestFakeReliableChannel(backgroundScope)
        stateManager = DefaultAudioOwnerStateManager("android-client", AudioRole.ACTIVE_AUDIO_OWNER)

        val notificationManager = DefaultAndroidNotificationManager(
            context,
            channel,
            stateManager,
            backgroundScope,
            checkPermissions = { true }
        )

        notificationManager.start()
        runCurrent()

        // Simulate notification posted
        MediaNotificationListenerService.listener?.onNotificationPosted(
            id = "notif-1",
            title = "Hello",
            text = "World",
            packageName = "com.test",
            appName = "TestApp",
            timestamp = 1000L,
            isOngoing = false
        )
        runCurrent()

        assertEquals(1, notificationManager.activeNotifications.value.size)
        val notif = notificationManager.activeNotifications.value.first()
        assertEquals("Hello", notif.title)
        assertEquals("World", notif.text)
        assertEquals("ADDED", notif.action)

        val sentPacket = channel.sentPackets.lastOrNull { it.packetType == PacketType.NOTIFICATION_STATE }
        assertNotNull(sentPacket)
        val payload = sentPacket!!.payload as NotificationStatePayload
        assertEquals("ADDED", payload.action)
        assertEquals("Hello", payload.title)

        notificationManager.stop()
    }

    @Test
    fun `test notification remove`() = runTest {
        channel = TestFakeReliableChannel(backgroundScope)
        stateManager = DefaultAudioOwnerStateManager("android-client", AudioRole.ACTIVE_AUDIO_OWNER)

        val notificationManager = DefaultAndroidNotificationManager(
            context,
            channel,
            stateManager,
            backgroundScope,
            checkPermissions = { true }
        )

        notificationManager.start()
        runCurrent()

        // Add
        MediaNotificationListenerService.listener?.onNotificationPosted(
            id = "notif-1",
            title = "Hello",
            text = "World",
            packageName = "com.test",
            appName = "TestApp",
            timestamp = 1000L,
            isOngoing = false
        )
        runCurrent()

        // Remove
        MediaNotificationListenerService.listener?.onNotificationRemoved("notif-1", "com.test")
        runCurrent()

        assertTrue(notificationManager.activeNotifications.value.isEmpty())

        val lastPacket = channel.sentPackets.last()
        assertEquals(PacketType.NOTIFICATION_STATE, lastPacket.packetType)
        val payload = lastPacket.payload as NotificationStatePayload
        assertEquals("REMOVED", payload.action)
        assertEquals("notif-1", payload.id)

        notificationManager.stop()
    }

    @Test
    fun `test notification update`() = runTest {
        channel = TestFakeReliableChannel(backgroundScope)
        stateManager = DefaultAudioOwnerStateManager("android-client", AudioRole.ACTIVE_AUDIO_OWNER)

        val notificationManager = DefaultAndroidNotificationManager(
            context,
            channel,
            stateManager,
            backgroundScope,
            checkPermissions = { true }
        )

        notificationManager.start()
        runCurrent()

        // Posted 1
        MediaNotificationListenerService.listener?.onNotificationPosted(
            id = "notif-1",
            title = "Hello",
            text = "World",
            packageName = "com.test",
            appName = "TestApp",
            timestamp = 1000L,
            isOngoing = false
        )
        runCurrent()

        // Posted 2 (Update)
        MediaNotificationListenerService.listener?.onNotificationPosted(
            id = "notif-1",
            title = "Hello",
            text = "New World",
            packageName = "com.test",
            appName = "TestApp",
            timestamp = 2000L,
            isOngoing = false
        )
        runCurrent()

        assertEquals(1, notificationManager.activeNotifications.value.size)
        val notif = notificationManager.activeNotifications.value.first()
        assertEquals("New World", notif.text)
        assertEquals("UPDATED", notif.action)

        val lastPacket = channel.sentPackets.last()
        assertEquals("UPDATED", (lastPacket.payload as NotificationStatePayload).action)
        assertEquals("New World", (lastPacket.payload as NotificationStatePayload).text)

        notificationManager.stop()
    }

    @Test
    fun `test permission denied`() = runTest {
        channel = TestFakeReliableChannel(backgroundScope)
        stateManager = DefaultAudioOwnerStateManager("android-client", AudioRole.ACTIVE_AUDIO_OWNER)

        // For Call
        val callManager = DefaultAndroidCallManager(
            context,
            channel,
            stateManager,
            backgroundScope,
            checkPermissions = { false }
        )
        callManager.start()
        runCurrent()

        assertEquals("MISSING_PERMISSION", callManager.callState.value)
        assertEquals("MISSING_PERMISSION", callManager.callerId.value)
        val callPacket = channel.sentPackets.last { it.packetType == PacketType.CALL_STATE }
        assertEquals("MISSING_PERMISSION", (callPacket.payload as CallStatePayload).state)

        // For Notification
        val notificationManager = DefaultAndroidNotificationManager(
            context,
            channel,
            stateManager,
            backgroundScope,
            checkPermissions = { false }
        )
        notificationManager.start()
        runCurrent()

        assertFalse(notificationManager.permissionGranted.value)
        val notifPacket = channel.sentPackets.last { it.packetType == PacketType.NOTIFICATION_STATE }
        assertEquals("MISSING_PERMISSION", (notifPacket.payload as NotificationStatePayload).action)
    }

    @Test
    fun `test reconnect restoration`() = runTest {
        channel = TestFakeReliableChannel(backgroundScope)
        stateManager = DefaultAudioOwnerStateManager("android-client", AudioRole.ACTIVE_AUDIO_OWNER)

        val notificationManager = DefaultAndroidNotificationManager(
            context,
            channel,
            stateManager,
            backgroundScope,
            checkPermissions = { true }
        )

        notificationManager.start()
        runCurrent()

        // Add a notification while connected
        channel.setConnectionState(ConnectionState.Connected)
        runCurrent()

        MediaNotificationListenerService.listener?.onNotificationPosted(
            id = "notif-reconnect",
            title = "Reconnect",
            text = "Test",
            packageName = "com.test",
            appName = "TestApp",
            timestamp = 1000L,
            isOngoing = false
        )
        runCurrent()

        val initialCount = channel.sentPackets.count { it.packetType == PacketType.NOTIFICATION_STATE }

        // Disconnect
        channel.setConnectionState(ConnectionState.Disconnected)
        runCurrent()

        // Reconnect
        channel.setConnectionState(ConnectionState.Connected)
        runCurrent()

        // Verify notification is sent again
        val finalCount = channel.sentPackets.count { it.packetType == PacketType.NOTIFICATION_STATE }
        assertEquals(initialCount + 1, finalCount)

        notificationManager.stop()
    }

    @Test
    fun `test duplicate notification suppression`() = runTest {
        channel = TestFakeReliableChannel(backgroundScope)
        stateManager = DefaultAudioOwnerStateManager("android-client", AudioRole.ACTIVE_AUDIO_OWNER)

        // 1. Android side suppression
        val notificationManager = DefaultAndroidNotificationManager(
            context,
            channel,
            stateManager,
            backgroundScope,
            checkPermissions = { true }
        )

        notificationManager.start()
        runCurrent()

        // Post notification 1st time
        MediaNotificationListenerService.listener?.onNotificationPosted(
            id = "notif-dup",
            title = "Same",
            text = "Same",
            packageName = "com.test",
            appName = "TestApp",
            timestamp = 1000L,
            isOngoing = false
        )
        runCurrent()

        val notifPackets = channel.sentPackets.filter { it.packetType == PacketType.NOTIFICATION_STATE }
        println("DEBUG: after 1st post, notifPackets size = ${notifPackets.size}, packets = $notifPackets")
        val countAfterFirst = notifPackets.size

        // Post exact same notification 2nd time
        MediaNotificationListenerService.listener?.onNotificationPosted(
            id = "notif-dup",
            title = "Same",
            text = "Same",
            packageName = "com.test",
            appName = "TestApp",
            timestamp = 1000L,
            isOngoing = false
        )
        runCurrent()

        val notifPacketsSecond = channel.sentPackets.filter { it.packetType == PacketType.NOTIFICATION_STATE }
        println("DEBUG: after 2nd post, notifPackets size = ${notifPacketsSecond.size}, packets = $notifPacketsSecond")
        val countAfterSecond = notifPacketsSecond.size
        
        assertEquals(countAfterFirst, countAfterSecond) // Suppressed!

        // 2. Desktop side suppression
        val desktopChannel = TestFakeReliableChannel(backgroundScope)
        val desktopNotifManager = DesktopNotificationManager(desktopChannel, backgroundScope)
        runCurrent()
        
        // Post first
        val payload = NotificationStatePayload(
            id = "notif-dup",
            title = "Same",
            text = "Same",
            packageName = "com.test",
            appName = "TestApp",
            timestamp = 1000L,
            isOngoing = false,
            action = "ADDED"
        )
        
        val packet1 = Packet(
            version = 1,
            id = UUID.randomUUID().toString(),
            timestamp = System.currentTimeMillis(),
            senderId = "android-client",
            receiverId = "desktop-server",
            packetType = PacketType.NOTIFICATION_STATE,
            payload = payload
        )
        
        desktopChannel.incomingPackets.emit(packet1)
        runCurrent()
        assertEquals(1, desktopNotifManager.notifications.value.size)

        // Emit identical again
        desktopChannel.incomingPackets.emit(packet1)
        runCurrent()
        assertEquals(1, desktopNotifManager.notifications.value.size)

        notificationManager.stop()
        desktopNotifManager.stop()
    }
}
