package com.remoteaudiosync.crypto

import com.remoteaudiosync.protocol.*
import org.bouncycastle.crypto.generators.Ed25519KeyPairGenerator
import org.bouncycastle.crypto.params.Ed25519KeyGenerationParameters
import org.bouncycastle.crypto.params.Ed25519PrivateKeyParameters
import org.bouncycastle.crypto.params.Ed25519PublicKeyParameters
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.security.SecureRandom
import java.util.UUID

@RunWith(RobolectricTestRunner::class)
class CryptoRoundtripTest {

    private fun generateIdentityKeys(): Pair<ByteArray, ByteArray> {
        val generator = Ed25519KeyPairGenerator()
        generator.init(Ed25519KeyGenerationParameters(SecureRandom()))
        val keyPair = generator.generateKeyPair()
        val pubParams = keyPair.public as Ed25519PublicKeyParameters
        val privParams = keyPair.private as Ed25519PrivateKeyParameters
        return Pair(pubParams.encoded, privParams.encoded)
    }

    @Test
    fun testFullHandshakeAndCryptoRoundtrip() {
        val aliceCrypto = CryptoManager()
        val bobCrypto = CryptoManager()
        
        // 1. Setup identities
        val aliceId = generateIdentityKeys()
        aliceCrypto.initIdentity(aliceId.first, aliceId.second)
        
        val bobId = generateIdentityKeys()
        bobCrypto.initIdentity(bobId.first, bobId.second)
        
        // 2. Ephemeral key exchange
        aliceCrypto.generateEphemeralKeyPair()
        bobCrypto.generateEphemeralKeyPair()
        
        val aliceEphemeralPub = aliceCrypto.ephemeralPublicKeyBytes!!
        val bobEphemeralPub = bobCrypto.ephemeralPublicKeyBytes!!
        
        // 3. Signatures
        val aliceSig = aliceCrypto.sign(aliceEphemeralPub)
        val bobSig = bobCrypto.sign(bobEphemeralPub)
        
        // 4. Verification
        assertTrue(bobCrypto.verifySignature(aliceId.first, aliceEphemeralPub, aliceSig))
        assertTrue(aliceCrypto.verifySignature(bobId.first, bobEphemeralPub, bobSig))
        assertFalse(bobCrypto.verifySignature(aliceId.first, bobEphemeralPub, bobSig)) // Wrong signature
        
        // 5. Session keys
        aliceCrypto.deriveSessionKey(bobEphemeralPub)
        bobCrypto.deriveSessionKey(aliceEphemeralPub)
        
        assertArrayEquals(aliceCrypto.sessionKey, bobCrypto.sessionKey)
        
        // 6. Packet encryption / decryption
        val mediaPayload = MediaCommandPayload("play")
        val packet = Packet(
            version = 1,
            id = UUID.randomUUID().toString(),
            timestamp = System.currentTimeMillis(),
            senderId = "alice",
            receiverId = "bob",
            packetType = PacketType.MEDIA_COMMAND,
            payload = mediaPayload
        )
        
        // Encrypt with alice
        val encryptedResult = PacketCodec.encryptPayload(packet, aliceCrypto)
        assertTrue("Encrypt failed: $encryptedResult", encryptedResult is ProtocolResult.Success)
        val encryptedPacket = (encryptedResult as ProtocolResult.Success).data
        
        assertNull(encryptedPacket.payload)
        assertNotNull(encryptedPacket.ciphertext)
        assertNotNull(encryptedPacket.nonce)
        
        // Decrypt with bob
        val decryptedResult = PacketCodec.decryptPayload(encryptedPacket, bobCrypto)
        assertTrue("Decrypt failed: $decryptedResult", decryptedResult is ProtocolResult.Success)
        val decryptedPacket = (decryptedResult as ProtocolResult.Success).data
        
        assertNull(decryptedPacket.ciphertext)
        assertNull(decryptedPacket.nonce)
        assertTrue(decryptedPacket.payload is MediaCommandPayload)
        assertEquals("play", (decryptedPacket.payload as MediaCommandPayload).command)
        
        // 7. Test Nonce Reuse rejection
        val replayResult = PacketCodec.decryptPayload(encryptedPacket, bobCrypto)
        assertTrue(replayResult is ProtocolResult.Failure)
        assertTrue((replayResult as ProtocolResult.Failure).error.message.contains("Nonce reuse") || replayResult.error.message.contains("Failed to decrypt"))
        
        // 8. Test plaintext rejection after auth
        val unencryptedPacket = Packet(
            version = 1,
            id = UUID.randomUUID().toString(),
            timestamp = System.currentTimeMillis(),
            senderId = "alice",
            receiverId = "bob",
            packetType = PacketType.MEDIA_COMMAND,
            payload = MediaCommandPayload("pause")
        )
        
        val rejectedResult = PacketCodec.decryptPayload(unencryptedPacket, bobCrypto)
        assertTrue(rejectedResult is ProtocolResult.Failure)
        assertEquals("Plaintext packet rejected after auth", (rejectedResult as ProtocolResult.Failure).error.message)
        
        // 9. Unencrypted allowed packets
        val authPacket = Packet(
            version = 1,
            id = UUID.randomUUID().toString(),
            timestamp = System.currentTimeMillis(),
            senderId = "alice",
            receiverId = "bob",
            packetType = PacketType.AUTH_REQUEST,
            payload = AuthRequestPayload("ephemeral", "sig")
        )
        val allowedResult = PacketCodec.decryptPayload(authPacket, bobCrypto)
        assertTrue("Allowed plaintext failed: $allowedResult", allowedResult is ProtocolResult.Success)
    }
}
