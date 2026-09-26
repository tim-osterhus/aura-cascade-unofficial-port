param(
    [Parameter(Mandatory = $true)][string]$JavaHome,
    [ValidatePattern('^[a-zA-Z0-9_-]+$')][string]$Label = 'packaged-server-smoke',
    [int]$StopAtMB = 3200
)

$ErrorActionPreference = 'Stop'
$repo = (Resolve-Path (Join-Path $PSScriptRoot '../..')).Path
$gameDir = Join-Path $repo 'build/qa-audit/packaged-server'
$outputDir = Join-Path $repo "build/qa-audit/$Label"
$launcher = Join-Path $gameDir 'fabric-server-launch.jar'
if (-not (Test-Path -LiteralPath $launcher -PathType Leaf)) { throw 'Run the isolated Fabric installer first.' }
if ((Get-Content -LiteralPath (Join-Path $gameDir 'eula.txt') -Raw) -notmatch '(?m)^eula=true\s*$') {
    throw 'The isolated server must have the existing accepted EULA configuration.'
}
New-Item -ItemType Directory -Path $outputDir -Force | Out-Null
$info = [Diagnostics.ProcessStartInfo]::new()
$info.FileName = Join-Path $JavaHome 'bin/java.exe'
$info.WorkingDirectory = $gameDir
$info.UseShellExecute = $false
$info.CreateNoWindow = $true
$info.RedirectStandardInput = $true
$info.RedirectStandardOutput = $true
$info.RedirectStandardError = $true
foreach ($argument in @('-Xms128m', '-Xmx1280m', '-XX:+UseSerialGC', '-XX:MaxDirectMemorySize=256m', '-jar', $launcher, 'nogui')) {
    $info.ArgumentList.Add($argument)
}
$started = [DateTime]::UtcNow
$process = [Diagnostics.Process]::Start($info)
$stdout = $process.StandardOutput.ReadToEndAsync()
$stderr = $process.StandardError.ReadToEndAsync()
$samples = [Collections.Generic.List[object]]::new()
$ready = $false
$stopSent = $false
$failure = $null
$logPath = Join-Path $gameDir 'logs/latest.log'
try {
    while (-not $process.HasExited) {
        $process.Refresh()
        $monitor = Get-Process -Id $PID
        $working = ($process.WorkingSet64 + $monitor.WorkingSet64) / 1MB
        $private = ($process.PrivateMemorySize64 + $monitor.PrivateMemorySize64) / 1MB
        $samples.Add([pscustomobject]@{utc=[DateTime]::UtcNow.ToString('o'); working_set_mb=[math]::Round($working,1); private_bytes_mb=[math]::Round($private,1)})
        if ([math]::Max($working, $private) -gt $StopAtMB) {
            $failure = 'memory_guard'
            $process.Kill($true)
            break
        }
        if (-not $stopSent -and (Test-Path -LiteralPath $logPath)) {
            $log = Get-Item -LiteralPath $logPath
            if ($log.LastWriteTimeUtc -ge $started -and (Get-Content -LiteralPath $logPath -Raw) -match 'Done \([\d.,]+s\)!') {
                $ready = $true
                $process.StandardInput.WriteLine('list')
                $process.StandardInput.WriteLine('save-all flush')
                $process.StandardInput.WriteLine('stop')
                $process.StandardInput.Flush()
                $stopSent = $true
            }
        }
        if (([DateTime]::UtcNow - $started).TotalSeconds -gt 240) {
            $failure = 'startup_or_shutdown_timeout'
            $process.Kill($true)
            break
        }
        Start-Sleep -Milliseconds 1000
    }
    $process.WaitForExit()
} finally {
    if (-not $process.HasExited) { $process.Kill($true); $process.WaitForExit() }
    $outText = $stdout.GetAwaiter().GetResult()
    $errText = $stderr.GetAwaiter().GetResult()
    [IO.File]::WriteAllText((Join-Path $outputDir 'stdout.log'), $outText)
    [IO.File]::WriteAllText((Join-Path $outputDir 'stderr.log'), $errText)
    $samples | Export-Csv -LiteralPath (Join-Path $outputDir 'memory.csv') -NoTypeInformation
    $summary = [ordered]@{
        started_utc=$started.ToString('o'); finished_utc=[DateTime]::UtcNow.ToString('o')
        reached_ready=$ready; normal_stop_requested=$stopSent; exit_code=$process.ExitCode; failure=$failure
        peak_working_set_mb=($samples | Measure-Object working_set_mb -Maximum).Maximum
        peak_private_bytes_mb=($samples | Measure-Object private_bytes_mb -Maximum).Maximum
        scope='Isolated Minecraft dedicated server plus monitor; one-second samples, not an OS hard cap.'
    }
    $summary | ConvertTo-Json | Set-Content -LiteralPath (Join-Path $outputDir 'summary.json')
    $summary | ConvertTo-Json
}
if ($failure -or -not $ready -or $process.ExitCode -ne 0) { exit 1 }
