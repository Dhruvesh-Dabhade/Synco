package com.remoteaudiosync.artwork

interface ArtworkProvider {
    fun getArtwork(mediaId: String): ByteArray?
}
