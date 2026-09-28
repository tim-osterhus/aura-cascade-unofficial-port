param(
    [Parameter(Mandatory = $true)][string]$JavaHome,
    [string]$Label = 'baseline',
    [string[]]$Tasks = @('test', 'build'),
    [int]$StopAtMB = 3800,
    [string]$ToolJvmOptions = '-Xmx512m -XX:ActiveProcessorCount=2'
)

$ErrorActionPreference = 'Stop'
$repo = (Resolve-Path (Join-Path $PSScriptRoot '../..')).Path
$outputDir = Join-Path $repo "build/qa-audit/$Label"
New-Item -ItemType Directory -Path $outputDir -Force | Out-Null
$env:JAVA_HOME = $JavaHome
$env:PATH = "$JavaHome\bin;$env:PATH"
$previousToolOptions = $env:JAVA_TOOL_OPTIONS
$env:JAVA_TOOL_OPTIONS = "$previousToolOptions $ToolJvmOptions".Trim()
$arguments = @('--console=plain', '--no-daemon', '--no-parallel', '--max-workers=1',
    '-Dorg.gradle.jvmargs=-Xmx1536m', '-I', 'scripts/qa/audit.init.gradle') + $Tasks
$started = [DateTime]::UtcNow
$launcher = Start-Process -FilePath (Join-Path $repo 'gradlew.bat') -ArgumentList $arguments `
    -WorkingDirectory $repo -WindowStyle Hidden -PassThru `
    -RedirectStandardOutput (Join-Path $outputDir 'stdout.log') `
    -RedirectStandardError (Join-Path $outputDir 'stderr.log')
$tracked = [System.Collections.Generic.HashSet[int]]::new()
$null = $tracked.Add($launcher.Id)
$samples = [System.Collections.Generic.List[object]]::new()
$stoppedForMemory = $false
try {
    do {
        $processes = Get-CimInstance Win32_Process
        # Follow descendants across the batch launcher, Gradle daemon and test JVM.
        do {
            $added = $false
            foreach ($process in $processes) {
                if ($tracked.Contains([int]$process.ParentProcessId)) {
                    if ($tracked.Add([int]$process.ProcessId)) { $added = $true }
                }
            }
        } while ($added)
        $live = @(Get-Process -Id @($tracked) -ErrorAction SilentlyContinue)
        $monitor = Get-Process -Id $PID
        $workingMB = (($live | Measure-Object WorkingSet64 -Sum).Sum + $monitor.WorkingSet64) / 1MB
        $privateMB = (($live | Measure-Object PrivateMemorySize64 -Sum).Sum + $monitor.PrivateMemorySize64) / 1MB
        $samples.Add([pscustomobject]@{
            utc = [DateTime]::UtcNow.ToString('o')
            working_set_mb = [math]::Round($workingMB, 1)
            private_bytes_mb = [math]::Round($privateMB, 1)
            processes = ($live.Id -join ',')
        })
        if ([math]::Max($workingMB, $privateMB) -gt $StopAtMB) {
            $stoppedForMemory = $true
            $live | Stop-Process -Force -ErrorAction SilentlyContinue
            break
        }
        Start-Sleep -Milliseconds 1000
        $launcher.Refresh()
    } while (-not $launcher.HasExited -or @($live | Where-Object { $_.Id -ne $launcher.Id }).Count -gt 0)
} finally {
    $samples | Export-Csv -LiteralPath (Join-Path $outputDir 'memory.csv') -NoTypeInformation
    $launcher.Refresh()
    $summary = [pscustomobject]@{
        started_utc = $started.ToString('o')
        finished_utc = [DateTime]::UtcNow.ToString('o')
        tasks = $Tasks
        java_home = $JavaHome
        gradle_heap_mb = 1536
        test_heap_mb = 512
        child_jvm_defaults = $ToolJvmOptions
        stop_at_mb = $StopAtMB
        peak_working_set_mb = ($samples | Measure-Object working_set_mb -Maximum).Maximum
        peak_private_bytes_mb = ($samples | Measure-Object private_bytes_mb -Maximum).Maximum
        stopped_for_memory = $stoppedForMemory
        exit_code = $launcher.ExitCode
        scope = 'Gradle launcher, descendants, and this monitor; excludes Codex/agent hosts, OS and unrelated apps. One-second samples are not an OS-enforced hard cap.'
    }
    $summary | ConvertTo-Json | Set-Content -LiteralPath (Join-Path $outputDir 'summary.json')
    $summary | ConvertTo-Json
    $env:JAVA_TOOL_OPTIONS = $previousToolOptions
}
if ($stoppedForMemory) { exit 124 }
exit $launcher.ExitCode
