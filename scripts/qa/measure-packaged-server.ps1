param(
    [Parameter(Mandatory = $true)][string]$JavaHome,
    [ValidatePattern('^[a-zA-Z0-9_-]+$')][string]$Label = 'packaged-server-smoke',
    [int]$StopAtMB = 3200,
    [switch]$TargetPersistence,
    [switch]$ExternalEnergy,
    [switch]$TargetProgression,
    [switch]$TargetHooks,
    [switch]$RemainingHooks,
    [switch]$TargetVortex,
    [switch]$TargetStorage,
    [ValidateSet('', 'write', 'verify')][string]$TargetDiskPhase = '',
    [ValidateRange(60, 1800)][int]$TimeoutSeconds = 240
)

$ErrorActionPreference = 'Stop'
$repo = (Resolve-Path (Join-Path $PSScriptRoot '../..')).Path
$gameDir = Join-Path $repo 'build/qa-audit/packaged-server-1.21.11'
$outputDir = Join-Path $repo "build/qa-audit/$Label"
$launcher = Join-Path $gameDir 'fabric-server-launch.jar'
if (-not (Test-Path -LiteralPath $launcher -PathType Leaf)) { throw 'Run the isolated Fabric installer first.' }
if ((Get-Content -LiteralPath (Join-Path $gameDir 'eula.txt') -Raw) -notmatch '(?m)^eula=true\s*$') {
    throw 'The isolated server must have the existing accepted EULA configuration.'
}
$serverProperties = Join-Path $gameDir 'server.properties'
if (-not (Test-Path -LiteralPath $serverProperties -PathType Leaf)) {
    throw 'The isolated server must bind to 127.0.0.1 before startup.'
}
$bindSettings = @(Get-Content -LiteralPath $serverProperties | Where-Object { $_ -match '^\s*server-ip\s*=' })
if ($bindSettings.Count -ne 1 -or $bindSettings[0] -notmatch '^\s*server-ip\s*=\s*127\.0\.0\.1\s*$') {
    throw 'The isolated server must bind to 127.0.0.1 before startup.'
}
$java = Join-Path $JavaHome 'bin/java.exe'
if (-not (Test-Path -LiteralPath $java -PathType Leaf)) { throw "Java executable is missing: $java" }
if (Test-Path -LiteralPath $outputDir) { throw "Refusing to overwrite existing QA output: $outputDir" }

$probePaths = [ordered]@{}
$auraSha256 = $null
if ($TargetPersistence -or $ExternalEnergy -or $TargetProgression -or $TargetHooks -or $RemainingHooks -or $TargetVortex -or $TargetStorage -or $TargetDiskPhase) {
    $auraJars = @(Get-ChildItem -LiteralPath (Join-Path $gameDir 'mods') -Filter 'aura-cascade-*.jar' -File)
    if ($auraJars.Count -ne 1 -or $auraJars[0].Name -notmatch '^aura-cascade-.+\+1\.21\.11\.jar$') {
        throw 'Probes require exactly one staged Aura 1.21.11 production JAR.'
    }
    $auraSha256 = (Get-FileHash -Algorithm SHA256 -LiteralPath $auraJars[0].FullName).Hash.ToLowerInvariant()
    if ($TargetPersistence) { $probePaths['targetPersistence'] = Join-Path $outputDir 'target-persistence.json' }
    if ($ExternalEnergy) { $probePaths['externalEnergy'] = Join-Path $outputDir 'external-energy.json' }
    if ($TargetProgression) { $probePaths['targetProgression'] = Join-Path $outputDir 'target-progression.json' }
    if ($TargetHooks) { $probePaths['targetHooks'] = Join-Path $outputDir 'target-hooks.json' }
    if ($RemainingHooks) { $probePaths['remainingHooks'] = Join-Path $outputDir 'remaining-hooks.json' }
    if ($TargetVortex) { $probePaths['targetVortex'] = Join-Path $outputDir 'target-vortex.json' }
    if ($TargetStorage) { $probePaths['targetStorage'] = Join-Path $outputDir 'target-storage.json' }
    if ($TargetDiskPhase) { $probePaths['targetDisk'] = Join-Path $outputDir 'target-disk.json' }
}

New-Item -ItemType Directory -Path $outputDir | Out-Null
$info = [Diagnostics.ProcessStartInfo]::new()
$info.FileName = $java
$info.WorkingDirectory = $gameDir
$info.UseShellExecute = $false
$info.CreateNoWindow = $true
$info.RedirectStandardInput = $true
$info.RedirectStandardOutput = $true
$info.RedirectStandardError = $true
if ($TargetPersistence) {
    $info.ArgumentList.Add('-Daura.qa.targetPersistence=true')
    $info.ArgumentList.Add("-Daura.qa.targetPersistence.output=$($probePaths['targetPersistence'])")
    $info.ArgumentList.Add("-Daura.qa.targetPersistence.sha256=$auraSha256")
}
if ($ExternalEnergy) {
    $info.ArgumentList.Add('-Daura.qa.externalEnergy=true')
    $info.ArgumentList.Add("-Daura.qa.externalEnergy.output=$($probePaths['externalEnergy'])")
}
foreach ($key in @('targetProgression', 'targetHooks', 'remainingHooks', 'targetVortex', 'targetStorage', 'targetDisk')) {
    if ($probePaths.Contains($key)) {
        $info.ArgumentList.Add("-Daura.qa.$key=true")
        $info.ArgumentList.Add("-Daura.qa.$key.output=$($probePaths[$key])")
        $info.ArgumentList.Add("-Daura.qa.$key.sha256=$auraSha256")
    }
}
if ($TargetDiskPhase) { $info.ArgumentList.Add("-Daura.qa.targetDisk.phase=$TargetDiskPhase") }
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
$probeResults = [ordered]@{}
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
                $pending = @($probePaths.Values | Where-Object {
                    if (-not (Test-Path -LiteralPath $_ -PathType Leaf)) { return $true }
                    try {
                        $currentReport = Get-Content -LiteralPath $_ -Raw | ConvertFrom-Json
                        return ($currentReport.PSObject.Properties.Name -contains 'complete' -and -not $currentReport.complete)
                    } catch { return $true }
                })
                if ($pending.Count -eq 0) {
                    $process.StandardInput.WriteLine('list')
                    $process.StandardInput.WriteLine('save-all flush')
                    $process.StandardInput.WriteLine('stop')
                    $process.StandardInput.Flush()
                    $stopSent = $true
                }
            }
        }
        if (([DateTime]::UtcNow - $started).TotalSeconds -gt $TimeoutSeconds) {
            $failure = 'startup_or_shutdown_timeout'
            $process.Kill($true)
            break
        }
        Start-Sleep -Milliseconds 1000
    }
    $process.WaitForExit()
    foreach ($probe in $probePaths.GetEnumerator()) {
        try {
            if (-not (Test-Path -LiteralPath $probe.Value -PathType Leaf)) {
                throw "Missing $($probe.Key) probe report: $($probe.Value)"
            }
            $report = Get-Content -LiteralPath $probe.Value -Raw | ConvertFrom-Json
            if ($report.success -isnot [bool] -or -not $report.success) {
                throw "$($probe.Key) probe did not report success: $($probe.Value)"
            }
            if ($probe.Key -ne 'externalEnergy' -and $report.auraSha256 -ne $auraSha256) {
                throw "$($probe.Key) probe loaded a different Aura JAR: $($report.auraSha256)"
            }
            $probeResults[$probe.Key] = 'pass'
        } catch {
            $probeResults[$probe.Key] = $_.Exception.Message
            if (-not $failure) { $failure = "$($probe.Key)_probe_failed" }
        }
    }
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
        staged_aura_sha256=$auraSha256; probe_reports=$probePaths; probe_results=$probeResults
        peak_working_set_mb=($samples | Measure-Object working_set_mb -Maximum).Maximum
        peak_private_bytes_mb=($samples | Measure-Object private_bytes_mb -Maximum).Maximum
        scope='Isolated Minecraft dedicated server plus monitor; one-second samples, not an OS hard cap.'
    }
    $summary | ConvertTo-Json -Depth 4 | Set-Content -LiteralPath (Join-Path $outputDir 'summary.json')
    $summary | ConvertTo-Json -Depth 4
}
if ($failure -or -not $ready -or -not $stopSent -or $process.ExitCode -ne 0) { exit 1 }
