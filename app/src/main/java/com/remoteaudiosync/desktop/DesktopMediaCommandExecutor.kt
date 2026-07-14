package com.remoteaudiosync.desktop

import com.remoteaudiosync.protocol.MediaCommandPayload
import javax.sound.sampled.AudioSystem
import javax.sound.sampled.FloatControl
import javax.sound.sampled.BooleanControl
import javax.sound.sampled.Port

interface DesktopMediaCommandExecutor {
    fun executeCommand(command: MediaCommandPayload): Boolean
}

class DefaultDesktopMediaCommandExecutor : DesktopMediaCommandExecutor {
    private val osName = System.getProperty("os.name")?.lowercase() ?: ""

    override fun executeCommand(command: MediaCommandPayload): Boolean {
        try {
            when (command.command) {
                "PLAY" -> {
                    if (osName.contains("linux")) {
                        executeShell(listOf("playerctl", "play"))
                    } else if (osName.contains("mac") || osName.contains("darwin")) {
                        executeShell(listOf("osascript", "-e", "tell application \"Music\" to play"))
                    } else if (osName.contains("win")) {
                        executeShell(listOf("powershell", "-Command", "(New-Object -ComObject Wscript.Shell).SendKeys([char]179)"))
                    }
                }
                "PAUSE" -> {
                    if (osName.contains("linux")) {
                        executeShell(listOf("playerctl", "pause"))
                    } else if (osName.contains("mac") || osName.contains("darwin")) {
                        executeShell(listOf("osascript", "-e", "tell application \"Music\" to pause"))
                    } else if (osName.contains("win")) {
                        executeShell(listOf("powershell", "-Command", "(New-Object -ComObject Wscript.Shell).SendKeys([char]179)"))
                    }
                }
                "NEXT" -> {
                    if (osName.contains("linux")) {
                        executeShell(listOf("playerctl", "next"))
                    } else if (osName.contains("mac") || osName.contains("darwin")) {
                        executeShell(listOf("osascript", "-e", "tell application \"Music\" to next track"))
                    } else if (osName.contains("win")) {
                        executeShell(listOf("powershell", "-Command", "(New-Object -ComObject Wscript.Shell).SendKeys([char]176)"))
                    }
                }
                "PREVIOUS" -> {
                    if (osName.contains("linux")) {
                        executeShell(listOf("playerctl", "previous"))
                    } else if (osName.contains("mac") || osName.contains("darwin")) {
                        executeShell(listOf("osascript", "-e", "tell application \"Music\" to previous track"))
                    } else if (osName.contains("win")) {
                        executeShell(listOf("powershell", "-Command", "(New-Object -ComObject Wscript.Shell).SendKeys([char]177)"))
                    }
                }
                "SEEK" -> {
                    val posMs = command.seekPosition ?: return false
                    if (osName.contains("linux")) {
                        val posSec = posMs / 1000.0
                        executeShell(listOf("playerctl", "position", posSec.toString()))
                    } else if (osName.contains("mac") || osName.contains("darwin")) {
                        val posSec = posMs / 1000.0
                        executeShell(listOf("osascript", "-e", "tell application \"Music\" to set player position to $posSec"))
                    }
                }
                "SET_VOLUME" -> {
                    val volPercent = command.volume ?: command.seekPosition?.toInt() ?: return false
                    setLocalVolume(volPercent)
                }
                "MUTE" -> {
                    toggleMute()
                }
                else -> return false
            }
            return true
        } catch (e: Exception) {
            return false
        }
    }

    private fun setLocalVolume(volPercent: Int) {
        val clampedVol = volPercent.coerceIn(0, 100)
        if (osName.contains("linux")) {
            executeShell(listOf("amixer", "set", "Master", "$clampedVol%"))
            val volDouble = clampedVol / 100.0
            executeShell(listOf("playerctl", "volume", volDouble.toString()))
        } else if (osName.contains("mac") || osName.contains("darwin")) {
            executeShell(listOf("osascript", "-e", "set volume output volume $clampedVol"))
        } else {
            try {
                val line = AudioSystem.getLine(Port.Info.SPEAKER)
                if (line != null) {
                    line.open()
                    val volumeControl = line.getControl(FloatControl.Type.MASTER_GAIN) as? FloatControl
                    if (volumeControl != null) {
                        val min = volumeControl.minimum
                        val max = volumeControl.maximum
                        val target = min + (max - min) * (clampedVol / 100.0f)
                        volumeControl.value = target
                    }
                }
            } catch (e: Exception) {}
        }
    }

    private fun toggleMute() {
        if (osName.contains("linux")) {
            executeShell(listOf("amixer", "set", "Master", "toggle"))
        } else if (osName.contains("mac") || osName.contains("darwin")) {
            executeShell(listOf("osascript", "-e", "set volume with output muted"))
        } else {
            try {
                val line = AudioSystem.getLine(Port.Info.SPEAKER)
                if (line != null) {
                    line.open()
                    val muteControl = line.getControl(BooleanControl.Type.MUTE) as? BooleanControl
                    if (muteControl != null) {
                        muteControl.value = !muteControl.value
                    }
                }
            } catch (e: Exception) {}
        }
    }

    private fun executeShell(cmd: List<String>) {
        try {
            val process = ProcessBuilder(cmd).start()
            process.waitFor()
        } catch (e: Exception) {}
    }
}
