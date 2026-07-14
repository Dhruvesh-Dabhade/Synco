package com.remoteaudiosync.desktop

import com.remoteaudiosync.protocol.MediaCommandPayload

interface DesktopMediaCommandExecutor {
    fun executeCommand(command: MediaCommandPayload): Boolean
}

class DefaultDesktopMediaCommandExecutor(
    private val sessionObserver: DesktopMediaSessionObserver
) : DesktopMediaCommandExecutor {
    override fun executeCommand(command: MediaCommandPayload): Boolean {
        val cmd = command.command
        val volume = command.volume
        
        when (cmd) {
            "SET_VOLUME" -> {
                if (volume != null) {
                    setSystemVolume(volume)
                }
            }
            "VOLUME_UP" -> adjustSystemVolume(true)
            "VOLUME_DOWN" -> adjustSystemVolume(false)
            "MUTE" -> toggleSystemMute()
        }
        return true
    }

    private fun setSystemVolume(volume: Float) {
        val os = System.getProperty("os.name").lowercase()
        try {
            if (os.contains("linux")) {
                val percent = (volume * 100).toInt()
                Runtime.getRuntime().exec(arrayOf("amixer", "-q", "sset", "Master", "$percent%"))
            }
        } catch (e: Exception) {}
    }

    private fun adjustSystemVolume(up: Boolean) {
        val os = System.getProperty("os.name").lowercase()
        try {
            if (os.contains("linux")) {
                val action = if (up) "5%+" else "5%-"
                Runtime.getRuntime().exec(arrayOf("amixer", "-q", "sset", "Master", action))
            }
        } catch (e: Exception) {}
    }

    private fun toggleSystemMute() {
        val os = System.getProperty("os.name").lowercase()
        try {
            if (os.contains("linux")) {
                Runtime.getRuntime().exec(arrayOf("amixer", "-q", "sset", "Master", "toggle"))
            }
        } catch (e: Exception) {}
    }
}
