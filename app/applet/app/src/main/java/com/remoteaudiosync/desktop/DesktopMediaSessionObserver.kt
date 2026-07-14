package com.remoteaudiosync.desktop

import com.remoteaudiosync.protocol.MediaStatePayload

interface DesktopMediaSessionObserver {
    fun startObserving(callback: (MediaStatePayload?) -> Unit)
    fun stopObserving()
    fun isSupported(): Boolean
    fun getCurrentState(): MediaStatePayload?
}

class DefaultDesktopMediaSessionObserver : DesktopMediaSessionObserver {
    private var callback: ((MediaStatePayload?) -> Unit)? = null
    private var isObserving = false

    override fun startObserving(callback: (MediaStatePayload?) -> Unit) {
        this.callback = callback
        this.isObserving = true
        // Honestly report no active session as standard JVM / headless environment has no active session
        callback(null)
    }

    override fun stopObserving() {
        this.callback = null
        this.isObserving = false
    }

    override fun isSupported(): Boolean = false

    override fun getCurrentState(): MediaStatePayload? = null
}
