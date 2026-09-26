param(
    [Parameter(Mandatory = $true)][ValidateSet('client', 'server')][string]$Mode,
    [Parameter(Mandatory = $true)][string]$JavaHome,
    [switch]$Execute
)

$ErrorActionPreference = 'Stop'
$repo = (Resolve-Path (Join-Path $PSScriptRoot '../..')).Path
$installDir = Join-Path $repo "build/qa-audit/packaged-$Mode"
$installer = Join-Path $repo 'build/qa-audit/tools/fabric-installer-1.1.2.jar'
$expectedSha256 = '61e035bf7bf70153e127440ce34de47c9036f0a2d0c65d1529454bd35ceefe4f'

if (-not (Test-Path -LiteralPath $installDir -PathType Container)) {
    throw "Isolated $Mode directory is missing: $installDir"
}
if (-not (Test-Path -LiteralPath $installer -PathType Leaf)) {
    throw "Fabric Installer is missing: $installer"
}
$actualSha256 = (Get-FileHash -Algorithm SHA256 -LiteralPath $installer).Hash.ToLowerInvariant()
if ($actualSha256 -ne $expectedSha256) {
    throw "Fabric Installer SHA-256 mismatch: expected $expectedSha256, got $actualSha256"
}

$java = Join-Path $JavaHome 'bin/java.exe'
if (-not (Test-Path -LiteralPath $java -PathType Leaf)) {
    throw "Java executable is missing: $java"
}

$installerArgs = if ($Mode -eq 'client') {
    @('client', '-dir', $installDir, '-mcversion', '1.21.1', '-loader', '0.19.1', '-noprofile')
} else {
    @('server', '-dir', $installDir, '-mcversion', '1.21.1', '-loader', '0.19.1', '-downloadMinecraft')
}

Write-Output "Isolated Fabric $Mode install: $installDir"
Write-Output "Verified Fabric Installer SHA-256: $actualSha256"
Write-Output ('Command: "{0}" -Xms32m -Xmx128m -jar "{1}" {2}' -f $java, $installer, ($installerArgs -join ' '))
if (-not $Execute) {
    Write-Output 'Preview only. Pass -Execute to run the installer.'
    exit 0
}

Push-Location $installDir
try {
    & $java '-Xms32m' '-Xmx128m' -jar $installer @installerArgs
    if ($LASTEXITCODE -ne 0) {
        throw "Fabric Installer exited with code $LASTEXITCODE"
    }
} finally {
    Pop-Location
}
