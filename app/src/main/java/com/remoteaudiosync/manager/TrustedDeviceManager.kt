package com.remoteaudiosync.manager

import android.content.Context
import android.content.SharedPreferences

class TrustedDeviceManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("trusted_devices", Context.MODE_PRIVATE)
    
    fun saveTrustedDevice(deviceId: String, publicKey: String) {
        prefs.edit().putString(deviceId, publicKey).apply()
    }
    
    fun getTrustedDevicePublicKey(deviceId: String): String? {
        return prefs.getString(deviceId, null)
    }
    
    fun isTrusted(deviceId: String): Boolean {
        return prefs.contains(deviceId)
    }
}
