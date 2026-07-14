[Windows.Media.Control.GlobalSystemMediaTransportControlsSessionManager, Windows.Media, ContentType = WindowsRuntime] | Out-Null
$manager = [Windows.Media.Control.GlobalSystemMediaTransportControlsSessionManager]::RequestAsync().GetAwaiter().GetResult()
$session = $manager.GetCurrentSession()
if ($session -ne $null) {
    $info = $session.TryGetMediaPropertiesAsync().GetAwaiter().GetResult()
    Write-Output "Title:$($info.Title)"
    Write-Output "Artist:$($info.Artist)"
    $playback = $session.GetPlaybackInfo()
    Write-Output "Status:$($playback.PlaybackStatus)"
}
