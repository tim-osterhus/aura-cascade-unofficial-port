param(
    [Parameter(Mandatory = $true)][string]$JavaHome,
    [Parameter(Mandatory = $true)][string]$GameDir,
    [ValidatePattern('^[a-zA-Z0-9_-]+$')][string]$Label = 'neoforge-packaged-server',
    [string]$NeoVersion = '21.1.252',
    [string[]]$Commands = @('list'),
    [string]$ExpectedLogPattern = '',
    [string[]]$QaJvmArguments = @(),
    [ValidateRange(30, 600)][int]$TimeoutSeconds = 240,
    [int]$StopAtMB = 3200
)

$ErrorActionPreference = 'Stop'
. (Join-Path $PSScriptRoot 'session-limit.ps1')
$repo = (Resolve-Path (Join-Path $PSScriptRoot '../../..')).Path
$gameDir = (Resolve-Path -LiteralPath $GameDir).Path
$argumentFile = Join-Path $gameDir "libraries/net/neoforged/neoforge/$NeoVersion/win_args.txt"
if (-not (Test-Path -LiteralPath $argumentFile -PathType Leaf)) { throw 'Install the isolated NeoForge server first.' }
if ((Get-Content -LiteralPath (Join-Path $gameDir 'eula.txt') -Raw) -notmatch '(?m)^eula=true\s*$') {
    throw 'The isolated server must have the existing accepted EULA configuration.'
}
$outputDir = Join-Path $repo "build/qa-audit/$Label"
if (Test-Path -LiteralPath $outputDir) { throw "Use a fresh evidence label: $Label" }
New-Item -ItemType Directory -Path $outputDir | Out-Null
$info = [Diagnostics.ProcessStartInfo]::new()
$info.FileName = Join-Path $JavaHome 'bin/java.exe'
$info.WorkingDirectory = $gameDir
$info.UseShellExecute = $false
$info.CreateNoWindow = $true
$info.RedirectStandardInput = $true
$info.RedirectStandardOutput = $true
$info.RedirectStandardError = $true
foreach ($argument in $QaJvmArguments) {
    if ($argument -notmatch '^-Daura\.qa\.[a-zA-Z0-9_.]+=') { throw 'Only scoped QA JVM properties are accepted.' }
    $info.ArgumentList.Add($argument)
}
foreach ($argument in @('-Xms128m', '-Xmx768m', '-XX:ActiveProcessorCount=2', '-XX:+UseSerialGC', '-XX:MaxDirectMemorySize=128m', "-Daura.qa.observer.dir=$outputDir", "@$argumentFile", 'nogui')) {
    $info.ArgumentList.Add($argument)
}
$started = [DateTime]::UtcNow
$modHashes = @(Get-ChildItem -LiteralPath (Join-Path $gameDir 'mods') -Filter '*.jar' -File | ForEach-Object {
    [ordered]@{ name=$_.Name; sha256=(Get-FileHash -LiteralPath $_.FullName -Algorithm SHA256).Hash.ToLowerInvariant() }
})
$existingSessions = @(Assert-MinecraftSessionCapacity)
$process = [Diagnostics.Process]::Start($info)
$stdout = $process.StandardOutput.ReadToEndAsync()
$stderr = $process.StandardError.ReadToEndAsync()
$samples = [Collections.Generic.List[object]]::new()
$ready = $false
$stopSent = $false
$expectationMet = $false
$commandsSentAt = $null
$failure = $null
$logPath = Join-Path $gameDir 'logs/latest.log'
try {
    while (-not $process.HasExited) {
        $process.Refresh()
        $monitor = Get-Process -Id $PID
        $working = ($process.WorkingSet64 + $monitor.WorkingSet64) / 1MB
        $private = ($process.PrivateMemorySize64 + $monitor.PrivateMemorySize64) / 1MB
        $samples.Add([pscustomobject]@{ utc=[DateTime]::UtcNow.ToString('o'); working_set_mb=[math]::Round($working,1); private_bytes_mb=[math]::Round($private,1) })
        if ([math]::Max($working, $private) -gt $StopAtMB) {
            $failure = 'memory_guard'
            $process.Kill($true)
            break
        }
        if (-not $stopSent -and (Test-Path -LiteralPath $logPath)) {
            $file = Get-Item -LiteralPath $logPath
            if ($file.LastWriteTimeUtc -ge $started) {
                $log = Get-Content -LiteralPath $logPath -Raw
                if (-not $ready -and $log -match 'Done \([\d.,]+s\)!') {
                    $ready = $true
                    foreach ($command in $Commands) { $process.StandardInput.WriteLine($command) }
                    $process.StandardInput.Flush()
                    $commandsSentAt = [DateTime]::UtcNow
                }
                $expectationMet = $ready -and ([string]::IsNullOrEmpty($ExpectedLogPattern) -or $log -match $ExpectedLogPattern)
                if ($expectationMet -and ([DateTime]::UtcNow - $commandsSentAt).TotalSeconds -ge 3) {
                    $process.StandardInput.WriteLine('save-all flush')
                    $process.StandardInput.WriteLine('stop')
                    $process.StandardInput.Flush()
                    $stopSent = $true
                }
            }
        }
        if (([DateTime]::UtcNow - $started).TotalSeconds -gt $TimeoutSeconds) {
            $failure = 'startup_command_or_shutdown_timeout'
            $process.Kill($true)
            break
        }
        Start-Sleep -Milliseconds 1000
    }
    $process.WaitForExit()
} finally {
    if (-not $process.HasExited) { $process.Kill($true); $process.WaitForExit() }
    [IO.File]::WriteAllText((Join-Path $outputDir 'stdout.log'), $stdout.GetAwaiter().GetResult())
    [IO.File]::WriteAllText((Join-Path $outputDir 'stderr.log'), $stderr.GetAwaiter().GetResult())
    $samples | Export-Csv -LiteralPath (Join-Path $outputDir 'memory.csv') -NoTypeInformation
    $summary = [ordered]@{
        started_utc=$started.ToString('o'); finished_utc=[DateTime]::UtcNow.ToString('o')
        reached_ready=$ready; commands=$Commands; expectation_met=$expectationMet
        mods=$modHashes
        session_pids_before_launch=@($existingSessions.ProcessId)
        normal_stop_requested=$stopSent; exit_code=$process.ExitCode; failure=$failure
        peak_working_set_mb=($samples | Measure-Object working_set_mb -Maximum).Maximum
        peak_private_bytes_mb=($samples | Measure-Object private_bytes_mb -Maximum).Maximum
        scope='Isolated Minecraft dedicated server plus monitor; one-second samples, not an OS hard cap.'
    }
    $summary | ConvertTo-Json | Set-Content -LiteralPath (Join-Path $outputDir 'summary.json')
    $summary | ConvertTo-Json
}
if ($failure -or -not $ready -or -not $expectationMet -or $process.ExitCode -ne 0) { exit 1 }
