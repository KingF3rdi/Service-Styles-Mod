# SkellyHole Styles: reads what plays from Windows' media controls (the same info the volume flyout shows)
# and prints one JSON line per update. Placeholders __PARENT__, __ANY__, __DIR__ are filled in by the mod.
$ErrorActionPreference = 'Stop'
[Console]::OutputEncoding = [System.Text.Encoding]::UTF8
Add-Type -AssemblyName System.Runtime.WindowsRuntime
$asTask = ([System.WindowsRuntimeSystemExtensions].GetMethods() | Where-Object {
    $_.Name -eq 'AsTask' -and $_.GetParameters().Count -eq 1 -and $_.GetParameters()[0].ParameterType.Name -eq 'IAsyncOperation`1' })[0]
function Await($op, $type) {
    $t = $asTask.MakeGenericMethod($type).Invoke($null, @($op))
    $t.Wait(-1) | Out-Null
    $t.Result
}
[Windows.Media.Control.GlobalSystemMediaTransportControlsSessionManager, Windows.Media.Control, ContentType = WindowsRuntime] | Out-Null
[Windows.Storage.Streams.DataReader, Windows.Storage.Streams, ContentType = WindowsRuntime] | Out-Null
$mgr = Await ([Windows.Media.Control.GlobalSystemMediaTransportControlsSessionManager]::RequestAsync()) ([Windows.Media.Control.GlobalSystemMediaTransportControlsSessionManager])

$parent = __PARENT__
$any = $__ANY__
$dir = '__DIR__'
$cmdFile = Join-Path $dir 'command.txt'
$lastKey = ''
$coverNo = 0
$last = ''

while ($true) {
    if ($parent -gt 0 -and -not (Get-Process -Id $parent -ErrorAction SilentlyContinue)) { exit }
    $session = $null
    foreach ($s in $mgr.GetSessions()) { if ($s.SourceAppUserModelId -match 'spotify') { $session = $s } }
    if (-not $session -and $any) { $session = $mgr.GetCurrentSession() }

    if ($session -and (Test-Path $cmdFile)) {
        $cmd = (Get-Content $cmdFile -Raw).Trim()
        Remove-Item $cmdFile -Force
        switch ($cmd) {
            'toggle' { Await ($session.TryTogglePlayPauseAsync()) ([bool]) | Out-Null }
            'next' { Await ($session.TrySkipNextAsync()) ([bool]) | Out-Null }
            'prev' { Await ($session.TrySkipPreviousAsync()) ([bool]) | Out-Null }
        }
    }

    $line = '{}'
    if ($session) {
        try {
            $p = Await ($session.TryGetMediaPropertiesAsync()) ([Windows.Media.Control.GlobalSystemMediaTransportControlsSessionMediaProperties])
            $tl = $session.GetTimelineProperties()
            $info = $session.GetPlaybackInfo()
            $key = "$($p.Title)|$($p.Artist)"
            $cover = ''
            if ($key -ne $lastKey -and $null -ne $p.Thumbnail) {
                try { # a missing or broken cover must never hide the song
                    $stream = Await ($p.Thumbnail.OpenReadAsync()) ([Windows.Storage.Streams.IRandomAccessStreamWithContentType])
                    $size = [uint32]$stream.Size
                    $reader = [Windows.Storage.Streams.DataReader]::new($stream.GetInputStreamAt(0))
                    Await ($reader.LoadAsync($size)) ([uint32]) | Out-Null
                    $bytes = New-Object byte[] $size
                    $reader.ReadBytes($bytes)
                    $coverNo++
                    $cover = Join-Path $dir "cover$($coverNo % 4).img"
                    [IO.File]::WriteAllBytes($cover, $bytes)
                    $reader.Dispose()
                    $stream.Dispose()
                } catch { $cover = '' }
            }
            if ($key -ne $lastKey) { $lastKey = $key }
            $line = [pscustomobject]@{
                app = $session.SourceAppUserModelId; title = $p.Title; artist = $p.Artist; album = $p.AlbumTitle
                pos = [long]$tl.Position.TotalMilliseconds; dur = [long]$tl.EndTime.TotalMilliseconds
                upd = $tl.LastUpdatedTime.ToUnixTimeMilliseconds(); now = [DateTimeOffset]::UtcNow.ToUnixTimeMilliseconds()
                playing = ($info.PlaybackStatus.ToString() -eq 'Playing'); cover = $cover
            } | ConvertTo-Json -Compress
        } catch { $line = '{}' }
    }
    if ($line -ne $last -or $line -ne '{}') { [Console]::Out.WriteLine($line); [Console]::Out.Flush(); $last = $line }
    Start-Sleep -Milliseconds 700
}
