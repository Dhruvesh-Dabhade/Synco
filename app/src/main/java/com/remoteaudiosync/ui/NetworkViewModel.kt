package com.remoteaudiosync.ui

import android.app.Application
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioDeviceInfo
import android.media.AudioManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.remoteaudiosync.manager.PairingManager
import com.remoteaudiosync.manager.PairingResult
import com.remoteaudiosync.manager.TrustedDeviceManager
import com.remoteaudiosync.network.ConnectionState
import com.remoteaudiosync.network.WebSocketClient
import com.remoteaudiosync.service.SyncoForegroundService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID
import com.remoteaudiosync.protocol.Packet
import com.remoteaudiosync.protocol.PacketType
import com.remoteaudiosync.protocol.RoleStatePayload
import com.remoteaudiosync.protocol.RoleChangeRequestPayload

fun getActiveAudioDeviceName(context: Context): String {
    try {
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager ?: return "Built-in Speaker"
        val devices = audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
        var hasBluetooth = false
        var btName = ""
        var wiredName = ""
        var speakerName = ""
        for (device in devices) {
            val name = device.productName?.toString() ?: ""
            if (device.type == AudioDeviceInfo.TYPE_BLUETOOTH_A2DP || 
                device.type == AudioDeviceInfo.TYPE_BLUETOOTH_SCO) {
                if (name.isNotEmpty()) return name
                hasBluetooth = true
            } else if (device.type == AudioDeviceInfo.TYPE_WIRED_HEADSET || 
                       device.type == AudioDeviceInfo.TYPE_WIRED_HEADPHONES) {
                wiredName = if (name.isNotEmpty()) name else "Wired Headphones"
            } else if (device.type == AudioDeviceInfo.TYPE_BUILTIN_SPEAKER) {
                speakerName = if (name.isNotEmpty()) name else "Built-in Speaker"
            }
        }
        if (hasBluetooth) return if (btName.isNotEmpty()) btName else "Bluetooth Audio Device"
        if (wiredName.isNotEmpty()) return wiredName
        if (speakerName.isNotEmpty()) return speakerName
    } catch (e: Exception) {
        // Fallback
    }
    return "Built-in Speaker"
}

class NetworkViewModel(application: Application) : AndroidViewModel(application) {
    private val webSocketClient = WebSocketClient()
    private val cryptoManager = com.remoteaudiosync.crypto.CryptoManager()
    private val reliableChannel = com.remoteaudiosync.network.ReliableChannel(webSocketClient, cryptoManager, viewModelScope)
    val connectionState: StateFlow<ConnectionState> = reliableChannel.connectionState
    val isAuthenticated: StateFlow<Boolean> = reliableChannel.isAuthenticated

    private val trustedDeviceManager = TrustedDeviceManager(application)
    private val identityKeyStore = com.remoteaudiosync.crypto.IdentityKeyStore(application)
    private val pairingManager = PairingManager(reliableChannel, trustedDeviceManager, identityKeyStore, cryptoManager)

    // User Profile Settings
    private val settingsPrefs = application.getSharedPreferences("synco_settings", Context.MODE_PRIVATE)
    private val _profileName = MutableStateFlow(settingsPrefs.getString("profile_name", "") ?: "")
    val profileName: StateFlow<String> = _profileName.asStateFlow()

    fun updateProfileName(name: String) {
        _profileName.value = name
        settingsPrefs.edit().putString("profile_name", name).apply()
    }

    private val _logs = MutableStateFlow<List<String>>(emptyList())
    val logs: StateFlow<List<String>> = _logs.asStateFlow()

    private val _pairingStatus = MutableStateFlow<String>("")
    val pairingStatus: StateFlow<String> = _pairingStatus.asStateFlow()

    val artworkCache = com.remoteaudiosync.artwork.ArtworkCache(application)
    val artworkManager = com.remoteaudiosync.artwork.ArtworkManager(reliableChannel, artworkCache, null, viewModelScope)
    val mediaManager = com.remoteaudiosync.manager.MediaManager(application, reliableChannel, viewModelScope, artworkCache)

    val audioOwnerStateManager = com.remoteaudiosync.manager.DefaultAudioOwnerStateManager("android-client")
    val bluetoothDeviceMonitor = com.remoteaudiosync.manager.DefaultBluetoothDeviceMonitor(application)
    val bluetoothOwnershipManager = com.remoteaudiosync.manager.DefaultBluetoothOwnershipManager(
        deviceMonitor = bluetoothDeviceMonitor,
        stateManager = audioOwnerStateManager,
        reliableChannel = reliableChannel,
        coroutineScope = viewModelScope,
        onRoleChanged = { role ->
            val isOwner = role == com.remoteaudiosync.manager.AudioRole.ACTIVE_AUDIO_OWNER
            _isAudioOwner.value = isOwner
            mediaManager.setRole(isOwner)
        }
    )

    val callManager = com.remoteaudiosync.manager.DefaultAndroidCallManager(
        context = application,
        reliableChannel = reliableChannel,
        stateManager = audioOwnerStateManager,
        coroutineScope = viewModelScope
    )
    val notificationManager = com.remoteaudiosync.manager.DefaultAndroidNotificationManager(
        context = application,
        reliableChannel = reliableChannel,
        stateManager = audioOwnerStateManager,
        coroutineScope = viewModelScope
    )

    private val _isAudioOwner = MutableStateFlow(false)
    val isAudioOwner: StateFlow<Boolean> = _isAudioOwner.asStateFlow()

    private val _desktopDeviceInfo = MutableStateFlow<com.remoteaudiosync.protocol.DeviceInfoPayload?>(null)
    val desktopDeviceInfo: StateFlow<com.remoteaudiosync.protocol.DeviceInfoPayload?> = _desktopDeviceInfo.asStateFlow()

    private val _activeAudioDevice = MutableStateFlow("Built-in Speaker")
    val activeAudioDevice: StateFlow<String> = _activeAudioDevice.asStateFlow()

    private val _hasPhonePermission = MutableStateFlow(false)
    val hasPhonePermission: StateFlow<Boolean> = _hasPhonePermission.asStateFlow()

    val hasNotificationPermission: StateFlow<Boolean> = mediaManager.hasNotificationPermission

    fun updatePermissionStates() {
        val phoneGranted = getApplication<Application>().checkSelfPermission(android.Manifest.permission.READ_PHONE_STATE) == android.content.pm.PackageManager.PERMISSION_GRANTED
        val answerGranted = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            getApplication<Application>().checkSelfPermission(android.Manifest.permission.ANSWER_PHONE_CALLS) == android.content.pm.PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
        _hasPhonePermission.value = phoneGranted && answerGranted
        mediaManager.checkPermission()
    }

    init {
        updatePermissionStates()
        bluetoothDeviceMonitor.startMonitoring()
        viewModelScope.launch {
            launch {
                connectionState.collect { state ->
                    val isConnectingOrConnected = state is ConnectionState.Connected ||
                            state is ConnectionState.Connecting ||
                            state is ConnectionState.Reconnecting ||
                            state is ConnectionState.WaitingForAck
                    
                    try {
                        val context = getApplication<Application>()
                        if (isConnectingOrConnected) {
                            val intent = Intent(context, SyncoForegroundService::class.java).apply {
                                action = SyncoForegroundService.ACTION_START
                            }
                            context.startForegroundService(intent)
                        } else {
                            val intent = Intent(context, SyncoForegroundService::class.java).apply {
                                action = SyncoForegroundService.ACTION_STOP
                            }
                            context.startService(intent)
                        }
                    } catch (e: Exception) {
                        // Suppress background start exceptions
                    }
                }
            }
            launch {
                webSocketClient.logs.collect { log ->
                    _logs.update { currentLogs ->
                        (currentLogs + "WS: $log").takeLast(100)
                    }
                }
            }
            launch {
                reliableChannel.logs.collect { log ->
                    _logs.update { currentLogs ->
                        (currentLogs + "RC: $log").takeLast(100)
                    }
                }
            }
            launch {
                reliableChannel.isAuthenticated.collect { authenticated ->
                    if (authenticated) {
                        bluetoothOwnershipManager.start()
                        callManager.start()
                        notificationManager.start()
                    } else {
                        bluetoothOwnershipManager.stop()
                        callManager.stop()
                        notificationManager.stop()
                        _desktopDeviceInfo.value = null
                    }
                }
            }
            launch {
                reliableChannel.incomingPackets.collect { packet ->
                    if (packet.packetType == PacketType.DEVICE_INFO) {
                        val payload = packet.payload as? com.remoteaudiosync.protocol.DeviceInfoPayload
                        if (payload != null) {
                            _desktopDeviceInfo.value = payload
                        }
                    }
                }
            }
            launch {
                while (true) {
                    _activeAudioDevice.value = getActiveAudioDeviceName(getApplication())
                    updatePermissionStates()
                    kotlinx.coroutines.delay(2000)
                }
            }
        }
    }

    fun connect(ip: String, port: Int) {
        if (ip.isNotBlank()) {
            reliableChannel.connect(ip, port)
        }
    }

    fun disconnect() {
        reliableChannel.disconnect()
        _pairingStatus.value = ""
    }
    
    fun initiatePairing(pin: String) {
        viewModelScope.launch {
            _pairingStatus.value = "Pairing..."
            when (val result = pairingManager.initiatePairing(pin)) {
                is PairingResult.Success -> {
                    reliableChannel.setAuthenticated(true)
                    _pairingStatus.value = "Paired"
                }
                is PairingResult.Failed -> {
                    reliableChannel.setAuthenticated(false)
                    _pairingStatus.value = "Failed: ${result.reason}"
                }
            }
        }
    }

    fun clearError() {
        webSocketClient.clearError()
        _pairingStatus.value = ""
    }

    fun clearLogs() {
        _logs.value = emptyList()
    }
    
    fun requestRole(isAudioOwner: Boolean) {
        viewModelScope.launch {
            val targetRole = if (isAudioOwner) {
                com.remoteaudiosync.manager.AudioRole.ACTIVE_AUDIO_OWNER
            } else {
                com.remoteaudiosync.manager.AudioRole.REMOTE_CONTROLLER
            }
            bluetoothOwnershipManager.switchManager.initiateSwitch(targetRole)
        }
    }

    override fun onCleared() {
        super.onCleared()
        artworkManager.cleanup()
        bluetoothOwnershipManager.stop()
        callManager.stop()
        notificationManager.stop()
    }
}
