package com.remoteaudiosync.crypto

import org.bouncycastle.crypto.agreement.X25519Agreement
import org.bouncycastle.crypto.digests.SHA256Digest
import org.bouncycastle.crypto.engines.AESEngine
import org.bouncycastle.crypto.generators.HKDFBytesGenerator
import org.bouncycastle.crypto.generators.X25519KeyPairGenerator
import org.bouncycastle.crypto.modes.GCMBlockCipher
import org.bouncycastle.crypto.params.AEADParameters
import org.bouncycastle.crypto.params.Ed25519PrivateKeyParameters
import org.bouncycastle.crypto.params.Ed25519PublicKeyParameters
import org.bouncycastle.crypto.params.HKDFParameters
import org.bouncycastle.crypto.params.KeyParameter
import org.bouncycastle.crypto.params.X25519KeyGenerationParameters
import org.bouncycastle.crypto.params.X25519PrivateKeyParameters
import org.bouncycastle.crypto.params.X25519PublicKeyParameters
import org.bouncycastle.crypto.signers.Ed25519Signer
import java.security.SecureRandom
import java.util.Base64

class CryptoManager {

    private val secureRandom = SecureRandom()
    
    // Identity Key (Static)
    private var identityKeyPair: Pair<ByteArray, ByteArray>? = null
    
    // Ephemeral Key Pair for Session (X25519)
    private var ephemeralPrivateKey: X25519PrivateKeyParameters? = null
    var ephemeralPublicKeyBytes: ByteArray? = null
        private set
        
    // Derived Session Key (AES-256)
    var sessionKey: ByteArray? = null
        private set
        
    fun setSessionKeyDirectly(key: ByteArray) {
        sessionKey = key
    }
        
    private val usedNonces = mutableSetOf<String>()

    fun clearSessionKey() {
        sessionKey = null
        ephemeralPrivateKey = null
        ephemeralPublicKeyBytes = null
        usedNonces.clear()
    }

    fun initIdentity(publicKey: ByteArray, privateKey: ByteArray) {
        identityKeyPair = Pair(publicKey, privateKey)
    }

    fun getIdentityPublicKey(): ByteArray? = identityKeyPair?.first

    fun generateEphemeralKeyPair() {
        val generator = X25519KeyPairGenerator()
        generator.init(X25519KeyGenerationParameters(secureRandom))
        val keyPair = generator.generateKeyPair()
        ephemeralPrivateKey = keyPair.private as X25519PrivateKeyParameters
        val pubParams = keyPair.public as X25519PublicKeyParameters
        ephemeralPublicKeyBytes = pubParams.encoded
    }

    fun sign(data: ByteArray): ByteArray {
        val privBytes = identityKeyPair?.second ?: throw IllegalStateException("Identity key not initialized")
        val privParams = Ed25519PrivateKeyParameters(privBytes, 0)
        val signer = Ed25519Signer()
        signer.init(true, privParams)
        signer.update(data, 0, data.size)
        return signer.generateSignature()
    }

    fun verifySignature(publicKey: ByteArray, data: ByteArray, signature: ByteArray): Boolean {
        return try {
            val pubParams = Ed25519PublicKeyParameters(publicKey, 0)
            val signer = Ed25519Signer()
            signer.init(false, pubParams)
            signer.update(data, 0, data.size)
            signer.verifySignature(signature)
        } catch (e: Exception) {
            false
        }
    }

    fun deriveSessionKey(remoteEphemeralPublicKey: ByteArray) {
        val priv = ephemeralPrivateKey
        if (priv == null) {
            println("[ERROR] Ephemeral key not generated! Returning early instead of crashing!")
            val ex = IllegalStateException("Ephemeral key not generated")
            ex.printStackTrace()
            return
        }
        val pub = X25519PublicKeyParameters(remoteEphemeralPublicKey, 0)
        
        val agreement = X25519Agreement()
        agreement.init(priv)
        val secret = ByteArray(agreement.agreementSize)
        agreement.calculateAgreement(pub, secret, 0)
        
        val hkdf = HKDFBytesGenerator(SHA256Digest())
        hkdf.init(HKDFParameters(secret, ByteArray(0), "RemoteAudioSync".toByteArray()))
        
        val derivedKey = ByteArray(32) // AES-256
        hkdf.generateBytes(derivedKey, 0, 32)
        
        sessionKey = derivedKey
    }

    fun encrypt(plaintext: ByteArray): Pair<ByteArray, ByteArray> { // Returns Pair<Ciphertext, Nonce>
        val key = sessionKey ?: throw IllegalStateException("Session key not derived")
        
        val nonce = ByteArray(12) // 96-bit nonce
        secureRandom.nextBytes(nonce)
        
        val cipher = GCMBlockCipher.newInstance(AESEngine.newInstance())
        val params = AEADParameters(KeyParameter(key), 128, nonce)
        cipher.init(true, params)
        
        val ciphertext = ByteArray(cipher.getOutputSize(plaintext.size))
        val len1 = cipher.processBytes(plaintext, 0, plaintext.size, ciphertext, 0)
        cipher.doFinal(ciphertext, len1)
        
        return Pair(ciphertext, nonce)
    }

    fun decrypt(ciphertext: ByteArray, nonce: ByteArray): ByteArray {
        val nonceBase64 = Base64.getEncoder().encodeToString(nonce)
        if (!usedNonces.add(nonceBase64)) {
            throw IllegalArgumentException("Nonce reuse detected")
        }

        val key = sessionKey ?: throw IllegalStateException("Session key not derived")
        
        val cipher = GCMBlockCipher.newInstance(AESEngine.newInstance())
        val params = AEADParameters(KeyParameter(key), 128, nonce)
        cipher.init(false, params)
        
        val plaintext = ByteArray(cipher.getOutputSize(ciphertext.size))
        val len1 = cipher.processBytes(ciphertext, 0, ciphertext.size, plaintext, 0)
        cipher.doFinal(plaintext, len1)
        
        return plaintext
    }
}
