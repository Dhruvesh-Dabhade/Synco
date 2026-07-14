package com.remoteaudiosync.artwork

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import java.io.ByteArrayOutputStream
import java.io.File
import java.security.MessageDigest
import java.util.concurrent.ConcurrentHashMap

class ArtworkCache(private val context: Context) {
    private val memCache = ConcurrentHashMap<String, ByteArray>()
    private val cacheDir = File(context.cacheDir, "artwork_cache").apply { mkdirs() }

    fun get(artworkId: String): ByteArray? {
        memCache[artworkId]?.let { return it }

        val file = File(cacheDir, artworkId)
        if (file.exists()) {
            try {
                val bytes = file.readBytes()
                memCache[artworkId] = bytes
                return bytes
            } catch (e: Exception) {
                file.delete()
            }
        }
        return null
    }

    fun put(artworkId: String, bytes: ByteArray) {
        memCache[artworkId] = bytes
        try {
            val file = File(cacheDir, artworkId)
            file.writeBytes(bytes)
        } catch (e: Exception) {}
    }

    fun contains(artworkId: String): Boolean {
        if (memCache.containsKey(artworkId)) return true
        return File(cacheDir, artworkId).exists()
    }

    fun clear() {
        memCache.clear()
        try {
            cacheDir.listFiles()?.forEach { it.delete() }
        } catch (e: Exception) {}
    }

    companion object {
        const val MAX_SIZE_BYTES = 256 * 1024 // 256 KB

        fun generateSha256(bytes: ByteArray): String {
            val md = MessageDigest.getInstance("SHA-256")
            val digest = md.digest(bytes)
            return digest.joinToString("") { "%02x".format(it) }
        }

        fun downscaleIfNeeded(bytes: ByteArray): ByteArray {
            if (bytes.size <= MAX_SIZE_BYTES) return bytes
            try {
                var bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size) ?: return bytes
                var quality = 90
                var scale = 0.9f
                var outputStream = ByteArrayOutputStream()
                bitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream)
                var compressedBytes = outputStream.toByteArray()

                while (compressedBytes.size > MAX_SIZE_BYTES && (bitmap.width > 50 || bitmap.height > 50)) {
                    val newWidth = (bitmap.width * scale).toInt()
                    val newHeight = (bitmap.height * scale).toInt()
                    if (newWidth <= 10 || newHeight <= 10) break

                    val scaledBitmap = Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
                    if (bitmap != scaledBitmap) {
                        bitmap.recycle()
                    }
                    bitmap = scaledBitmap

                    outputStream = ByteArrayOutputStream()
                    bitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream)
                    compressedBytes = outputStream.toByteArray()

                    if (quality > 30) {
                        quality -= 10
                    }
                }
                bitmap.recycle()
                return compressedBytes
            } catch (e: Throwable) {
                return bytes
            }
        }
    }
}
