package com.remoteaudiosync.protocol

import com.remoteaudiosync.crypto.CryptoManager
import kotlinx.serialization.SerializationException
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.Base64

object PacketCodec {
    val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = false
        classDiscriminator = "type"
    }

    fun serialize(packet: Packet): ProtocolResult<String> {
        return try {
            val jsonString = json.encodeToString(packet)
            ProtocolResult.Success(jsonString)
        } catch (e: SerializationException) {
            ProtocolResult.Failure(ProtocolError.SerializationFailure("Failed to serialize packet"))
        } catch (e: IllegalArgumentException) {
            ProtocolResult.Failure(ProtocolError.SerializationFailure("Failed to serialize packet due to invalid arguments"))
        }
    }

    fun deserialize(jsonString: String): ProtocolResult<Packet> {
        return try {
            val packet = json.decodeFromString<Packet>(jsonString)
            ProtocolResult.Success(packet)
        } catch (e: SerializationException) {
            ProtocolResult.Failure(ProtocolError.MalformedPacket("Failed to deserialize packet"))
        } catch (e: IllegalArgumentException) {
            ProtocolResult.Failure(ProtocolError.MalformedPacket("Failed to deserialize packet"))
        }
    }

    fun encryptPayload(packet: Packet, cryptoManager: CryptoManager): ProtocolResult<Packet> {
        val payload = packet.payload ?: return ProtocolResult.Success(packet)
        return try {
            val payloadJson = json.encodeToString(Payload.serializer(), payload)
            val aad = "${packet.packetType.name}|${packet.senderId}|${packet.receiverId}".toByteArray(Charsets.UTF_8)
            val (ciphertext, nonce) = cryptoManager.encrypt(payloadJson.toByteArray(Charsets.UTF_8), aad)
            val newPacket = packet.copy(
                payload = null,
                ciphertext = Base64.getEncoder().encodeToString(ciphertext),
                nonce = Base64.getEncoder().encodeToString(nonce)
            )
            ProtocolResult.Success(newPacket)
        } catch (e: Exception) {
            ProtocolResult.Failure(ProtocolError.SerializationFailure("Failed to encrypt payload"))
        }
    }

    fun decryptPayload(packet: Packet, cryptoManager: CryptoManager): ProtocolResult<Packet> {
        val allowedUnencrypted = setOf(PacketType.PAIR_REQUEST, PacketType.PAIR_RESPONSE, PacketType.AUTH_REQUEST, PacketType.AUTH_SUCCESS)

        if (packet.ciphertext == null) {
            if (cryptoManager.sessionKey != null && packet.packetType !in allowedUnencrypted) {
                return ProtocolResult.Failure(ProtocolError.MalformedPacket("Plaintext packet rejected after auth"))
            }
            return ProtocolResult.Success(packet)
        }

        val ciphertextBase64 = packet.ciphertext
        val nonceBase64 = packet.nonce ?: return ProtocolResult.Failure(ProtocolError.MalformedPacket("Missing nonce"))

        return try {
            val ciphertext = Base64.getDecoder().decode(ciphertextBase64)
            val nonce = Base64.getDecoder().decode(nonceBase64)
            val aad = "${packet.packetType.name}|${packet.senderId}|${packet.receiverId}".toByteArray(Charsets.UTF_8)
            val plaintextBytes = cryptoManager.decrypt(ciphertext, nonce, aad)
            val payloadJson = String(plaintextBytes, Charsets.UTF_8)
            val payload = json.decodeFromString(Payload.serializer(), payloadJson)
            val newPacket = packet.copy(
                payload = payload,
                ciphertext = null,
                nonce = null
            )
            ProtocolResult.Success(newPacket)
        } catch (e: IllegalArgumentException) {
            ProtocolResult.Failure(ProtocolError.MalformedPacket("Decryption failed"))
        } catch (e: Exception) {
            ProtocolResult.Failure(ProtocolError.MalformedPacket("Failed to decrypt payload"))
        }
    }
}
