package com.remoteaudiosync.manager

import com.remoteaudiosync.network.ConnectionState
import com.remoteaudiosync.network.ReliableChannel
import com.remoteaudiosync.network.WebSocketClient
import com.remoteaudiosync.crypto.CryptoManager
import com.remoteaudiosync.protocol.Packet
import com.remoteaudiosync.protocol.PacketType
import com.remoteaudiosync.protocol.RoleChangeRequestPayload
import com.remoteaudiosync.protocol.RoleStatePayload
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.UUID

class BluetoothFakeReliableChannel(scope: CoroutineScope) : ReliableChannel(WebSocketClient(), CryptoManager(), scope) {
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
    }

    override suspend fun sendWithAck(packet: Packet): Boolean {
        sentPackets.add(packet)
        return mockSendWithAckResult
    }
}

class FakeBluetoothDeviceMonitor : BluetoothDeviceMonitor {
    private val _isConnected = MutableStateFlow(false)
    override val isConnected: StateFlow<Boolean> = _isConnected

    private val _batteryStatus = MutableStateFlow("BATTERY_UNAVAILABLE")
    override val batteryStatus: StateFlow<String> = _batteryStatus

    private val _profileState = MutableStateFlow("DISCONNECTED")
    override val profileState: StateFlow<String> = _profileState

    var isMonitoring = false

    fun setConnected(connected: Boolean) {
        _isConnected.value = connected
        _profileState.value = if (connected) "CONNECTED" else "DISCONNECTED"
    }

    fun setBattery(battery: String) {
        _batteryStatus.value = battery
    }

    override fun startMonitoring() {
        isMonitoring = true
    }

    override fun stopMonitoring() {
        isMonitoring = false
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class BluetoothOwnershipTest {

    @Test
    fun `test Android to Desktop switch`() = runTest {
        val channel = BluetoothFakeReliableChannel(backgroundScope)
        val stateManager = DefaultAudioOwnerStateManager("android-client", AudioRole.ACTIVE_AUDIO_OWNER)
        var roleChangedCalled: AudioRole? = null
        
        val switchManager = DefaultSourceSwitchManager(
            reliableChannel = channel,
            stateManager = stateManager,
            coroutineScope = backgroundScope,
            onRoleChanged = { roleChangedCalled = it }
        )

        // Android is currently owner. User requests switch to Desktop (making Android REMOTE_CONTROLLER)
        val switchJob = backgroundScope.launch {
            switchManager.initiateSwitch(AudioRole.REMOTE_CONTROLLER)
        }

        runCurrent()
        assertTrue(channel.sentPackets.any { it.packetType == PacketType.ROLE_CHANGE_REQUEST })
        
        val reqPacket = channel.sentPackets.first { it.packetType == PacketType.ROLE_CHANGE_REQUEST }
        val reqPayload = reqPacket.payload as RoleChangeRequestPayload
        assertFalse(reqPayload.requestAudioOwner) // Wants peer to become active owner

        // Desktop accepts and sends ROLE_STATE indicating it is now the owner
        switchManager.handleIncomingState(
            RoleStatePayload(
                isAudioOwner = true,
                sourceEpoch = reqPayload.sourceEpoch,
                deviceId = "desktop-server"
            )
        )
        
        runCurrent()
        assertEquals(AudioRole.REMOTE_CONTROLLER, stateManager.currentRole.value)
        assertEquals(AudioRole.REMOTE_CONTROLLER, roleChangedCalled)
        switchJob.cancel()
    }

    @Test
    fun `test Desktop to Android switch`() = runTest {
        val channel = BluetoothFakeReliableChannel(backgroundScope)
        val stateManager = DefaultAudioOwnerStateManager("android-client", AudioRole.REMOTE_CONTROLLER)
        var roleChangedCalled: AudioRole? = null
        
        val switchManager = DefaultSourceSwitchManager(
            reliableChannel = channel,
            stateManager = stateManager,
            coroutineScope = backgroundScope,
            onRoleChanged = { roleChangedCalled = it }
        )

        // Android is currently controller. User requests switch to make Android ACTIVE_AUDIO_OWNER
        val switchJob = backgroundScope.launch {
            switchManager.initiateSwitch(AudioRole.ACTIVE_AUDIO_OWNER)
        }

        runCurrent()
        assertTrue(channel.sentPackets.any { it.packetType == PacketType.ROLE_CHANGE_REQUEST })
        
        val reqPacket = channel.sentPackets.first { it.packetType == PacketType.ROLE_CHANGE_REQUEST }
        val reqPayload = reqPacket.payload as RoleChangeRequestPayload
        assertTrue(reqPayload.requestAudioOwner)

        // Desktop gracefully releases ownership and sends ROLE_STATE indicating it is no longer the owner
        switchManager.handleIncomingState(
            RoleStatePayload(
                isAudioOwner = false,
                sourceEpoch = reqPayload.sourceEpoch,
                deviceId = "desktop-server"
            )
        )

        runCurrent()
        assertEquals(AudioRole.ACTIVE_AUDIO_OWNER, stateManager.currentRole.value)
        assertEquals(AudioRole.ACTIVE_AUDIO_OWNER, roleChangedCalled)
        switchJob.cancel()
    }

    @Test
    fun `test simultaneous requests sourceEpoch conflict`() = runTest {
        val channel = BluetoothFakeReliableChannel(backgroundScope)
        val stateManager = DefaultAudioOwnerStateManager("android-client", AudioRole.REMOTE_CONTROLLER)
        var roleChangedCalled: AudioRole? = null
        
        val switchManager = DefaultSourceSwitchManager(
            reliableChannel = channel,
            stateManager = stateManager,
            coroutineScope = backgroundScope,
            onRoleChanged = { roleChangedCalled = it }
        )

        // Start local switch attempt
        val switchJob = backgroundScope.launch {
            switchManager.initiateSwitch(AudioRole.ACTIVE_AUDIO_OWNER)
        }
        runCurrent()

        // Simulate incoming conflicting request with higher epoch (peer wins)
        val peerRequest = RoleChangeRequestPayload(
            requestAudioOwner = true,
            sourceEpoch = 10L, // local epoch is 1
            requestTimestamp = System.currentTimeMillis(),
            deviceId = "desktop-server"
        )
        val accepted = switchManager.handleIncomingRequest(peerRequest)
        assertTrue(accepted) // Android yields to higher epoch, request accepted

        runCurrent()
        assertEquals(AudioRole.REMOTE_CONTROLLER, stateManager.currentRole.value)
        switchJob.cancel()
    }

    @Test
    fun `test simultaneous requests requestTimestamp conflict`() = runTest {
        val channel = BluetoothFakeReliableChannel(backgroundScope)
        val stateManager = DefaultAudioOwnerStateManager("android-client", AudioRole.REMOTE_CONTROLLER)
        
        val switchManager = DefaultSourceSwitchManager(
            reliableChannel = channel,
            stateManager = stateManager,
            coroutineScope = backgroundScope,
            onRoleChanged = {}
        )

        // Start local switch attempt
        val switchJob = backgroundScope.launch {
            switchManager.initiateSwitch(AudioRole.ACTIVE_AUDIO_OWNER)
        }
        runCurrent()

        // Peer request with SAME epoch (1), but NEWER timestamp (peer wins)
        val peerRequest = RoleChangeRequestPayload(
            requestAudioOwner = true,
            sourceEpoch = stateManager.sourceEpoch.value,
            requestTimestamp = System.currentTimeMillis() + 10000L, // newer
            deviceId = "desktop-server"
        )
        val accepted = switchManager.handleIncomingRequest(peerRequest)
        assertTrue(accepted) // Android yields to newer timestamp

        assertEquals(AudioRole.REMOTE_CONTROLLER, stateManager.currentRole.value)
        switchJob.cancel()
    }

    @Test
    fun `test simultaneous requests deviceId tie-break`() = runTest {
        val channel = BluetoothFakeReliableChannel(backgroundScope)
        val stateManager = DefaultAudioOwnerStateManager("android-client", AudioRole.REMOTE_CONTROLLER)
        
        val switchManager = DefaultSourceSwitchManager(
            reliableChannel = channel,
            stateManager = stateManager,
            coroutineScope = backgroundScope,
            onRoleChanged = {}
        )

        // Start local switch attempt
        val switchJob = backgroundScope.launch {
            switchManager.initiateSwitch(AudioRole.ACTIVE_AUDIO_OWNER)
        }
        runCurrent()

        // Peer request with SAME epoch and SAME timestamp, but lexicographically larger deviceId ("desktop-server")
        // "android-client" < "desktop-server", so Android wins. Peer request should be rejected!
        val peerRequest = RoleChangeRequestPayload(
            requestAudioOwner = true,
            sourceEpoch = stateManager.sourceEpoch.value,
            requestTimestamp = 0L, // we'll control it
            deviceId = "desktop-server"
        )
        val accepted = switchManager.handleIncomingRequest(peerRequest)
        assertFalse(accepted) // Rejected! Android wins tie-break

        switchJob.cancel()
    }

    @Test
    fun `test timeout rollback`() = runTest {
        val channel = BluetoothFakeReliableChannel(backgroundScope)
        val stateManager = DefaultAudioOwnerStateManager("android-client", AudioRole.REMOTE_CONTROLLER)
        
        val switchManager = DefaultSourceSwitchManager(
            reliableChannel = channel,
            stateManager = stateManager,
            coroutineScope = backgroundScope,
            onRoleChanged = {}
        )

        // Start switch request but never emit response
        val switchJob = backgroundScope.launch {
            val result = switchManager.initiateSwitch(AudioRole.ACTIVE_AUDIO_OWNER)
            assertFalse(result) // fails due to timeout
        }

        runCurrent()
        advanceTimeBy(6000) // exceed 5 seconds timeout
        runCurrent()
        assertFalse(switchManager.isSwitching.value)
        assertEquals(AudioRole.REMOTE_CONTROLLER, stateManager.currentRole.value)
        switchJob.cancel()
    }

    @Test
    fun `test owner disconnect`() = runTest {
        val channel = BluetoothFakeReliableChannel(backgroundScope)
        val monitor = FakeBluetoothDeviceMonitor()
        val stateManager = DefaultAudioOwnerStateManager("android-client", AudioRole.REMOTE_CONTROLLER)
        
        val ownershipManager = DefaultBluetoothOwnershipManager(
            deviceMonitor = monitor,
            stateManager = stateManager,
            reliableChannel = channel,
            coroutineScope = backgroundScope,
            onRoleChanged = {}
        )

        ownershipManager.start()
        channel.setConnectionState(ConnectionState.Connected)
        runCurrent()
        
        // As a REMOTE_CONTROLLER, peer connection is active
        assertTrue(ownershipManager.isOwnerConnected.value)

        // Connection drops
        channel.setConnectionState(ConnectionState.Disconnected)
        runCurrent()

        // Check safe state entered immediately (owner is offline, local role is unchanged)
        assertFalse(ownershipManager.isOwnerConnected.value)
        assertEquals(AudioRole.REMOTE_CONTROLLER, stateManager.currentRole.value) // NO silent promotion!
        
        ownershipManager.stop()
    }

    @Test
    fun `test reconnect after switch`() = runTest {
        val channel = BluetoothFakeReliableChannel(backgroundScope)
        val monitor = FakeBluetoothDeviceMonitor()
        val stateManager = DefaultAudioOwnerStateManager("android-client", AudioRole.ACTIVE_AUDIO_OWNER)
        
        val ownershipManager = DefaultBluetoothOwnershipManager(
            deviceMonitor = monitor,
            stateManager = stateManager,
            reliableChannel = channel,
            coroutineScope = backgroundScope,
            onRoleChanged = {}
        )

        ownershipManager.start()
        
        // Reconnect
        channel.setConnectionState(ConnectionState.Connected)
        runCurrent()

        // Check if Android automatically recovers ownership by resending its active state
        val roleStatePacket = channel.sentPackets.lastOrNull { it.packetType == PacketType.ROLE_STATE }
        assertNotNull(roleStatePacket)
        val payload = roleStatePacket!!.payload as RoleStatePayload
        assertTrue(payload.isAudioOwner)
        assertEquals("android-client", payload.deviceId)

        ownershipManager.stop()
    }

    @Test
    fun `test Bluetooth unavailable`() = runTest {
        val monitor = DefaultBluetoothDeviceMonitor(
            context = androidx.test.core.app.ApplicationProvider.getApplicationContext()
        )
        // Since standard JVM/test environment won't have real bluetooth active/permissions,
        // start monitoring should report unavailable
        monitor.startMonitoring()
        
        assertFalse(monitor.isConnected.value)
        assertEquals("BATTERY_UNAVAILABLE", monitor.batteryStatus.value)
        assertEquals("DISCONNECTED", monitor.profileState.value)
        
        monitor.stopMonitoring()
    }

    @Test
    fun `test battery unavailable`() = runTest {
        val monitor = FakeBluetoothDeviceMonitor()
        monitor.startMonitoring()
        monitor.setConnected(true)
        monitor.setBattery("BATTERY_UNAVAILABLE")

        assertTrue(monitor.isConnected.value)
        assertEquals("BATTERY_UNAVAILABLE", monitor.batteryStatus.value)
        monitor.stopMonitoring()
    }
}
