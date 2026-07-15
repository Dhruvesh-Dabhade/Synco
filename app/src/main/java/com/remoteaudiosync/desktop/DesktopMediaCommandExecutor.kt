package com.remoteaudiosync.desktop

import com.remoteaudiosync.protocol.MediaCommandPayload
import java.io.File

interface DesktopMediaCommandExecutor {
    fun executeCommand(command: MediaCommandPayload): Boolean
}

class DefaultDesktopMediaCommandExecutor : DesktopMediaCommandExecutor {
    private val osName = System.getProperty("os.name")?.lowercase() ?: ""
    private var knownVolume = 50

    override fun executeCommand(command: MediaCommandPayload): Boolean {
        return try {
            when (command.command) {
                "PLAY" -> sendMediaKey(0xB3)
                "PAUSE" -> sendMediaKey(0xB3)
                "NEXT" -> sendMediaKey(0xB0)
                "PREVIOUS" -> sendMediaKey(0xB1)
                "SEEK" -> {
                    val posMs = command.seekPosition ?: return false
                    if (osName.contains("linux")) {
                        exec(listOf("playerctl", "position", (posMs / 1000.0).toString()))
                    } else if (osName.contains("mac") || osName.contains("darwin")) {
                        exec(listOf("osascript", "-e",
                            "tell application \"Music\" to set player position to ${posMs / 1000.0}"))
                    } else false
                }
                "SET_VOLUME" -> {
                    val target = (command.volume ?: return false).coerceIn(0, 100)
                    setVolume(target)
                }
                "MUTE" -> sendMediaKey(0xAD)
                "VOLUME_UP" -> sendMediaKey(0xAF)
                "VOLUME_DOWN" -> sendMediaKey(0xAE)
                else -> false
            }
        } catch (e: Exception) {
            System.err.println("[EXECUTOR] ${command.command} error: ${e.message}")
            false
        }
    }

    private fun sendMediaKey(vkCode: Int): Boolean {
        return when {
            osName.contains("win") -> pressKey(vkCode)
            osName.contains("linux") -> {
                val action = when (vkCode) {
                    0xB3 -> "play-pause"
                    0xB0 -> "next"
                    0xB1 -> "previous"
                    0xAF -> "volume 1+"
                    0xAE -> "volume 1-"
                    0xAD -> "volume 0"
                    else -> "play-pause"
                }
                exec(listOf("playerctl", action))
            }
            osName.contains("mac") || osName.contains("darwin") -> {
                val action = when (vkCode) {
                    0xB3 -> "playpause"
                    0xB0 -> "next track"
                    0xB1 -> "previous track"
                    0xAF -> "volume up"
                    0xAE -> "volume down"
                    0xAD -> "mute"
                    else -> "playpause"
                }
                exec(listOf("osascript", "-e",
                    "tell application \"Music\" to $action"))
            }
            else -> false
        }
    }

    private val tmpDir = System.getProperty("java.io.tmpdir")

    private fun pressKey(vkCode: Int): Boolean {
        val script = """
Add-Type -ErrorAction SilentlyContinue @"
using System;
using System.Runtime.InteropServices;
public static class WKey {
    [DllImport("user32.dll")]
    public static extern void keybd_event(byte vk,byte s,uint f,System.IntPtr x);
    public static void Send(byte c) {
        keybd_event(c,0,0,System.IntPtr.Zero);
        System.Threading.Thread.Sleep(50);
        keybd_event(c,0,2,System.IntPtr.Zero);
    }
}
"@
[WKey]::Send($vkCode)
""".trimIndent()
        val psFile = File(tmpDir, "synco_key_${vkCode}_${System.nanoTime()}.ps1")
        try {
            psFile.writeText(script)
            val ok = exec(listOf("powershell", "-NoProfile", "-ExecutionPolicy", "Bypass", "-File", psFile.absolutePath))
            if (!ok) System.err.println("[EXECUTOR] pressKey($vkCode) failed")
            return ok
        } finally {
            psFile.delete()
        }
    }

    private fun setVolume(target: Int): Boolean {
        if (osName.contains("linux")) {
            exec(listOf("amixer", "set", "Master", "$target%"))
            return exec(listOf("playerctl", "volume", (target / 100.0).toString()))
        }
        if (osName.contains("mac") || osName.contains("darwin")) {
            return exec(listOf("osascript", "-e", "set volume output volume $target"))
        }
        if (osName.contains("win")) {
            val diff = target - knownVolume
            if (diff == 0) return true
            val steps = (kotlin.math.abs(diff) + 1) / 2
            val key = if (diff > 0) 0xAF else 0xAE
            var ok = true
            for (i in 1..steps) {
                ok = pressKey(key) && ok
            }
            knownVolume = target
            return ok
        }
        return false
    }

    private fun exec(cmd: List<String>): Boolean {
        return try {
            val pb = ProcessBuilder(cmd)
            pb.redirectErrorStream(true)
            val p = pb.start()
            val exit = p.waitFor()
            if (exit != 0) {
                val err = p.inputStream.bufferedReader().readText().take(200)
                System.err.println("[EXECUTOR] exit=$exit ${cmd.joinToString(" ")} -- $err")
            }
            exit == 0
        } catch (e: Exception) {
            System.err.println("[EXECUTOR] exec fail: ${e.message}")
            false
        }
    }
}
