package com.remoteaudiosync.manager

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

class TrustedDeviceManager(context: Context) {
    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val prefs = EncryptedSharedPreferences.create(
        context,
        "trusted_devices_encrypted",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

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
