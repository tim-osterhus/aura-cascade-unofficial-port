param(
    [Parameter(Mandatory = $true)][string]$JavaHome,
    [ValidatePattern('^[a-zA-Z0-9_-]+$')][string]$Label = 'client',
    [ValidatePattern('^[a-zA-Z0-9_-]+$')][string]$GameDirectoryName = 'client-world',
    [ValidatePattern('^[a-zA-Z0-9_]{1,16}$')][string]$Username = 'AuraLab',
    [int]$StopAtMB = 3800,
    [int]$DesktopBudgetMB = 4000,
    [switch]$EnforceDesktopBudget
)

$ErrorActionPreference = 'Stop'
$repo = (Resolve-Path (Join-Path $PSScriptRoot '../..')).Path
$outputDir = Join-Path $repo "build/qa-audit/$Label"
$gameDir = Join-Path $repo "build/qa-audit/$GameDirectoryName"
New-Item -ItemType Directory -Path $outputDir, $gameDir -Force | Out-Null
$optionsFile = Join-Path $gameDir 'options.txt'
if (-not (Test-Path -LiteralPath $optionsFile)) {
    [IO.File]::WriteAllLines($optionsFile, @(
        'version:3955', 'fullscreen:false', 'guiScale:2', 'renderDistance:6',
        'simulationDistance:5', 'maxFps:60', 'graphicsMode:1', 'particles:0',
        'entityDistanceScaling:0.75', 'enableVsync:true', 'tutorialStep:none',
        'narrator:0', 'lang:en_us', 'onboardAccessibility:false', 'soundCategory_master:0.0'
    ))
}
$launch = Get-Content -LiteralPath (Join-Path $repo 'build/qa-audit/client-launch.json') -Raw | ConvertFrom-Json
$clientJvmArgs = @($launch.jvmArgs | Where-Object { $_ -notlike '-Xmx*' -and $_ -notlike '-Xms*' }) +
    @('-Xms128m', '-Xmx1280m', '-XX:+UseSerialGC', '-XX:ReservedCodeCacheSize=96m', '-XX:MaxDirectMemorySize=256m')
$arguments = $clientJvmArgs + @($launch.mainClass) +
    @($launch.args) + @('--gameDir', $gameDir, '--username', $Username, '--width', '1280', '--height', '720')
# Java argument files avoid Windows command-line length limits on Loom classpaths.
$argumentFile = Join-Path $outputDir 'java.args'
$escaped = $arguments | ForEach-Object {
    if (([string]$_).StartsWith('@')) {
        Get-Content -LiteralPath ([string]$_).Substring(1) -Raw
    } else {
        '"' + ([string]$_).Replace('\', '\\').Replace('"', '\"') + '"'
    }
}
[IO.File]::WriteAllLines($argumentFile, $escaped)
$started = [DateTime]::UtcNow
$client = Start-Process -FilePath (Join-Path $JavaHome 'bin/java.exe') `
    -ArgumentList "@`"$argumentFile`"" -WorkingDirectory $gameDir -WindowStyle Hidden -PassThru `
    -RedirectStandardOutput (Join-Path $outputDir 'stdout.log') `
    -RedirectStandardError (Join-Path $outputDir 'stderr.log')
$client.Id | Set-Content -LiteralPath (Join-Path $outputDir 'pid.txt')
$samples = [System.Collections.Generic.List[object]]::new()
$stoppedForMemory = $false
try {
    do {
        $client.Refresh()
        if ($client.HasExited) { break }
        $monitor = Get-Process -Id $PID
        $workingMB = ($client.WorkingSet64 + $monitor.WorkingSet64) / 1MB
        $privateMB = ($client.PrivateMemorySize64 + $monitor.PrivateMemorySize64) / 1MB
        $combined = @(Get-Process -Name ChatGPT,codex,node -ErrorAction SilentlyContinue) + @($client, $monitor)
        $combined = @($combined | Sort-Object Id -Unique)
        $combinedWorkingMB = ($combined | Measure-Object WorkingSet64 -Sum).Sum / 1MB
        $combinedPrivateMB = ($combined | Measure-Object PrivateMemorySize64 -Sum).Sum / 1MB
        $samples.Add([pscustomobject]@{
            utc = [DateTime]::UtcNow.ToString('o')
            working_set_mb = [math]::Round($workingMB, 1)
            private_bytes_mb = [math]::Round($privateMB, 1)
            combined_working_set_mb = [math]::Round($combinedWorkingMB, 1)
            combined_private_bytes_mb = [math]::Round($combinedPrivateMB, 1)
        })
        if ([math]::Max($workingMB, $privateMB) -gt $StopAtMB -or
            ($EnforceDesktopBudget -and [math]::Max($combinedWorkingMB, $combinedPrivateMB) -gt $DesktopBudgetMB)) {
            $stoppedForMemory = $true
            $client | Stop-Process -Force
            break
        }
        Start-Sleep -Milliseconds 1000
    } while (-not $client.HasExited)
} finally {
    $samples | Export-Csv -LiteralPath (Join-Path $outputDir 'memory.csv') -NoTypeInformation
    $client.Refresh()
    $summary = [pscustomobject]@{
        started_utc = $started.ToString('o')
        finished_utc = [DateTime]::UtcNow.ToString('o')
        java_home = $JavaHome
        stop_at_mb = $StopAtMB
        peak_working_set_mb = ($samples | Measure-Object working_set_mb -Maximum).Maximum
        peak_private_bytes_mb = ($samples | Measure-Object private_bytes_mb -Maximum).Maximum
        combined_budget_mb = $DesktopBudgetMB
        combined_budget_enforced = [bool]$EnforceDesktopBudget
        combined_peak_working_set_mb = ($samples | Measure-Object combined_working_set_mb -Maximum).Maximum
        combined_peak_private_bytes_mb = ($samples | Measure-Object combined_private_bytes_mb -Maximum).Maximum
        stopped_for_memory = $stoppedForMemory
        exit_code = $client.ExitCode
        scope = 'Minecraft client including integrated server, and monitor; excludes agent/desktop hosts, OS and unrelated apps. One-second sampling, not a hard OS cap.'
        combined_scope = 'Client, monitor, and all ChatGPT/codex/node processes (including unrelated sessions conservatively); excludes OS and other apps. MiB units; default 4000 MiB leaves headroom below 4.5 decimal GB. One-second sampling cannot guarantee a strict cap.'
    }
    $summary | ConvertTo-Json | Set-Content -LiteralPath (Join-Path $outputDir 'summary.json')
    $summary | ConvertTo-Json
}
if ($stoppedForMemory) { exit 124 }
exit $client.ExitCode
