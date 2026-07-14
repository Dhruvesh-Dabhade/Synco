import java.util.Base64
val script = """
[Windows.Media.Control.GlobalSystemMediaTransportControlsSessionManager, Windows.Media, ContentType = WindowsRuntime] | Out-Null
try {
    ${'$'}manager = [Windows.Media.Control.GlobalSystemMediaTransportControlsSessionManager]::RequestAsync().GetAwaiter().GetResult()
    ${'$'}session = ${'$'}manager.GetCurrentSession()
    if (${'$'}session -ne ${'$'}null) {
        ${'$'}info = ${'$'}session.TryGetMediaPropertiesAsync().GetAwaiter().GetResult()
        Write-Output ("TITLE:" + ${'$'}info.Title)
        Write-Output ("ARTIST:" + ${'$'}info.Artist)
        ${'$'}playback = ${'$'}session.GetPlaybackInfo()
        Write-Output ("STATUS:" + ${'$'}playback.PlaybackStatus)
    }
} catch {}
""".trimIndent()
val b64 = Base64.getEncoder().encodeToString(script.toByteArray(Charsets.UTF_16LE))
println("powershell -EncodedCommand " + b64)
