param(
    [Parameter(Mandatory)][string]$JavaHome,
    [Parameter(Mandatory)][string]$ProfileRoot,
    [Parameter(Mandatory)][string]$BaseWorld,
    [Parameter(Mandatory)][ValidateSet('creative', 'guide', 'hud', 'placement', 'animation')][string]$Action,
    [ValidateSet(2, 3)][int]$GuiScale = 2,
    [Parameter(Mandatory)][ValidatePattern('^[a-zA-Z0-9_-]+$')][string]$Label
)

$ErrorActionPreference = 'Stop'
$repo = [IO.Path]::GetFullPath((Join-Path $PSScriptRoot '../../..'))
$profile = (Resolve-Path -LiteralPath $ProfileRoot).Path
$baseWorld = (Resolve-Path -LiteralPath $BaseWorld).Path
$world = Join-Path $profile "saves/$Label"
$output = Join-Path $repo "build/qa-audit/$Label"
if ((Test-Path -LiteralPath $world) -or (Test-Path -LiteralPath $output)) { throw 'Use a fresh UI evidence label.' }
Copy-Item -LiteralPath $baseWorld -Destination $world -Recurse
$launchOutput = Join-Path $profile '.qa/neoforge-21.1.252'
try {
    & (Join-Path $PSScriptRoot 'launch-packaged-neoforge.ps1') -Profile client -JavaHome $JavaHome `
        -OutputPath $profile -QuickPlaySingleplayer $Label -ObserverDirectory $output `
        -QaJvmArguments @("-Daura.qa.ui.action=$Action", "-Daura.qa.ui.dir=$output", "-Daura.qa.ui.guiScale=$GuiScale") `
        -TimeoutSeconds 240 -Execute
} finally {
    if (Test-Path -LiteralPath $output) {
        foreach ($name in @('launch-manifest.json', 'client.java.args', 'stdout.log', 'stderr.log', 'memory.csv')) {
            $path = Join-Path $launchOutput $name
            if (Test-Path -LiteralPath $path) { Copy-Item -LiteralPath $path -Destination $output }
        }
        $summary = Join-Path $launchOutput 'summary.json'
        if (Test-Path -LiteralPath $summary) { Copy-Item -LiteralPath $summary -Destination (Join-Path $output 'launch-summary.json') }
    }
}
$launch = Get-Content -LiteralPath (Join-Path $output 'launch-summary.json') -Raw | ConvertFrom-Json
$result = Get-Content -LiteralPath (Join-Path $output 'manifest.json') -Raw | ConvertFrom-Json
if ($launch.exitCode -ne 0 -or $launch.timedOut -or $launch.stoppedForMemory -or
    $result.status -notin @('passed', 'evidence_captured')) {
    throw "UI driver did not complete: action=$Action status=$($result.status) failure=$($result.failure)"
}
Write-Output "UI evidence ready for independent visual review: $output"
