package com.remoteaudiosync.manager

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

interface BluetoothDeviceMonitor {
    val isConnected: StateFlow<Boolean>
    val batteryStatus: StateFlow<String> // "BATTERY_UNAVAILABLE" or level percentage
    val profileState: StateFlow<String> // "CONNECTED" or "DISCONNECTED"

    fun startMonitoring()
    fun stopMonitoring()
}

class DefaultBluetoothDeviceMonitor(private val context: Context) : BluetoothDeviceMonitor {
    private val _isConnected = MutableStateFlow(false)
    override val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

    private val _batteryStatus = MutableStateFlow("BATTERY_UNAVAILABLE")
    override val batteryStatus: StateFlow<String> = _batteryStatus.asStateFlow()

    private val _profileState = MutableStateFlow("DISCONNECTED")
    override val profileState: StateFlow<String> = _profileState.asStateFlow()

    private var isMonitoring = false

    private val bluetoothReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val action = intent.action ?: return
            when (action) {
                BluetoothDevice.ACTION_ACL_CONNECTED -> {
                    _isConnected.value = true
                    _profileState.value = "CONNECTED"
                }
                BluetoothDevice.ACTION_ACL_DISCONNECTED -> {
                    _isConnected.value = false
                    _profileState.value = "DISCONNECTED"
                    _batteryStatus.value = "BATTERY_UNAVAILABLE"
                }
                "android.bluetooth.device.action.BATTERY_LEVEL_CHANGED" -> {
                    val level = intent.getIntExtra("android.bluetooth.device.extra.BATTERY_LEVEL", -1)
                    if (level != -1) {
                        _batteryStatus.value = level.toString()
                    } else {
                        _batteryStatus.value = "BATTERY_UNAVAILABLE"
                    }
                }
            }
        }
    }

    override fun startMonitoring() {
        if (isMonitoring) return
        isMonitoring = true
        
        try {
            val filter = IntentFilter().apply {
                addAction(BluetoothDevice.ACTION_ACL_CONNECTED)
                addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED)
                addAction("android.bluetooth.device.action.BATTERY_LEVEL_CHANGED")
            }
            context.registerReceiver(bluetoothReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } catch (e: Exception) {
            // Fallback if failed to register
        }

        try {
            val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
            val adapter = bluetoothManager?.adapter
            if (adapter == null || !adapter.isEnabled) {
                _isConnected.value = false
                _batteryStatus.value = "BATTERY_UNAVAILABLE"
                _profileState.value = "DISCONNECTED"
            }
        } catch (e: SecurityException) {
            _isConnected.value = false
            _batteryStatus.value = "BATTERY_UNAVAILABLE"
            _profileState.value = "DISCONNECTED"
        } catch (e: Exception) {
            _isConnected.value = false
            _batteryStatus.value = "BATTERY_UNAVAILABLE"
            _profileState.value = "DISCONNECTED"
        }
    }

    override fun stopMonitoring() {
        if (!isMonitoring) return
        isMonitoring = false
        try {
            context.unregisterReceiver(bluetoothReceiver)
        } catch (e: Exception) {}
    }
}
