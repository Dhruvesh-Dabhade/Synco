package com.remoteaudiosync.crypto

import android.content.Context
import android.util.Base64
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import org.bouncycastle.crypto.AsymmetricCipherKeyPair
import org.bouncycastle.crypto.generators.Ed25519KeyPairGenerator
import org.bouncycastle.crypto.params.Ed25519KeyGenerationParameters
import org.bouncycastle.crypto.params.Ed25519PrivateKeyParameters
import org.bouncycastle.crypto.params.Ed25519PublicKeyParameters
import java.security.SecureRandom

class IdentityKeyStore(context: Context, private val customPrefs: android.content.SharedPreferences? = null) {

    private val masterKey by lazy {
        MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
    }

    private val prefs by lazy {
        customPrefs ?: EncryptedSharedPreferences.create(
            context,
            "identity_prefs",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    fun getOrGenerateIdentityKey(): Pair<ByteArray, ByteArray> {
        val pubB64 = prefs.getString("public_key", null)
        val privB64 = prefs.getString("private_key", null)

        if (pubB64 != null && privB64 != null) {
            return Pair(
                Base64.decode(pubB64, Base64.NO_WRAP),
                Base64.decode(privB64, Base64.NO_WRAP)
            )
        }

        val generator = Ed25519KeyPairGenerator()
        generator.init(Ed25519KeyGenerationParameters(SecureRandom()))
        val keyPair: AsymmetricCipherKeyPair = generator.generateKeyPair()

        val pubParams = keyPair.public as Ed25519PublicKeyParameters
        val privParams = keyPair.private as Ed25519PrivateKeyParameters

        val pubBytes = pubParams.encoded
        val privBytes = privParams.encoded

        prefs.edit()
            .putString("public_key", Base64.encodeToString(pubBytes, Base64.NO_WRAP))
            .putString("private_key", Base64.encodeToString(privBytes, Base64.NO_WRAP))
            .apply()

        return Pair(pubBytes, privBytes)
    }
}
