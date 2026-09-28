[CmdletBinding()]
param(
    [Parameter(Mandatory = $true)][ValidateSet('client', 'server')][string]$Profile,
    [Parameter(Mandatory = $true)][string]$JavaHome,
    [Parameter(Mandatory = $true)][string]$OutputPath,
    [Parameter(Mandatory = $true)][string]$ModJar,
    [string]$InstallerJar,
    [string]$ObserverJar,
    [switch]$Execute
)

$ErrorActionPreference = 'Stop'
$minecraftVersion = '1.21.1'
$neoForgeVersion = '21.1.252'
$patchouliVersion = '1.21.1-93-NEOFORGE'
$installerUrl = "https://maven.neoforged.net/releases/net/neoforged/neoforge/$neoForgeVersion/neoforge-$neoForgeVersion-installer.jar"
$patchouliUrl = "https://maven.blamejared.com/vazkii/patchouli/Patchouli/$patchouliVersion/Patchouli-$patchouliVersion.jar"

function Test-PathWithin([string]$Path, [string]$Root) {
    $fullPath = [IO.Path]::GetFullPath($Path).TrimEnd('\')
    $fullRoot = [IO.Path]::GetFullPath($Root).TrimEnd('\')
    return $fullPath.Equals($fullRoot, [StringComparison]::OrdinalIgnoreCase) -or
        $fullPath.StartsWith($fullRoot + '\', [StringComparison]::OrdinalIgnoreCase)
}

function Get-SafeOutputPath([string]$Path) {
    if (-not [IO.Path]::IsPathRooted($Path)) { throw 'OutputPath must be an absolute path.' }
    $fullPath = [IO.Path]::GetFullPath($Path)
    $parent = Split-Path -Parent $fullPath
    if (-not (Test-Path -LiteralPath $parent -PathType Container)) {
        throw "OutputPath parent must already exist: $parent"
    }
    $resolvedParent = (Resolve-Path -LiteralPath $parent).Path
    $resolved = Join-Path $resolvedParent (Split-Path -Leaf $fullPath)
    if (Test-Path -LiteralPath $resolved) { $resolved = (Resolve-Path -LiteralPath $resolved).Path }

    $segments = @($resolved -split '[\\/]')
    if ($segments -contains '.minecraft' -or $segments -contains 'instances') {
        throw 'OutputPath must be a new isolated QA directory, not a Minecraft or launcher instances directory.'
    }
    $protectedRoots = @(
        (Join-Path $env:APPDATA '.minecraft'),
        (Join-Path $env:USERPROFILE '.minecraft'),
        (Join-Path $env:APPDATA 'PrismLauncher'),
        (Join-Path $env:APPDATA 'MultiMC'),
        (Join-Path $env:APPDATA 'CurseForge\Minecraft\Instances'),
        (Join-Path $env:USERPROFILE 'curseforge\minecraft\Instances')
    ) | Where-Object { $_ }
    foreach ($protectedRoot in $protectedRoots) {
        if (Test-PathWithin $resolved $protectedRoot) {
            throw "OutputPath is inside a launcher-managed location: $protectedRoot"
        }
    }
    if ([IO.Path]::GetPathRoot($resolved).TrimEnd('\') -eq $resolved.TrimEnd('\')) {
        throw 'OutputPath cannot be a drive root.'
    }
    return $resolved
}

function Get-Sha256([string]$Path) {
    (Get-FileHash -LiteralPath $Path -Algorithm SHA256).Hash.ToLowerInvariant()
}

function Get-OfficialSha256([string]$Uri) {
    $content = (Invoke-WebRequest -Uri "$Uri.sha256" -UseBasicParsing -TimeoutSec 90).Content
    $checksum = $(if ($content -is [byte[]]) { [Text.Encoding]::UTF8.GetString($content) } else { [string]$content }).Trim()
    if ($checksum -notmatch '^[0-9a-fA-F]{64}$') { throw "Official SHA-256 response was invalid for $Uri" }
    return $checksum.ToLowerInvariant()
}

function Get-VerifiedDownload([string]$Uri, [string]$Destination) {
    $checksum = Get-OfficialSha256 $Uri
    New-Item -ItemType Directory -Path (Split-Path -Parent $Destination) -Force | Out-Null
    $partial = "$Destination.partial"
    Invoke-WebRequest -Uri $Uri -OutFile $partial -UseBasicParsing -TimeoutSec 180
    if ((Get-Sha256 $partial) -ne $checksum.ToLowerInvariant()) {
        Remove-Item -LiteralPath $partial -Force -ErrorAction SilentlyContinue
        throw "Downloaded file did not match the official SHA-256: $Uri"
    }
    Move-Item -LiteralPath $partial -Destination $Destination -Force
    return [pscustomobject]@{ Path = $Destination; Sha256 = $checksum.ToLowerInvariant() }
}

function Assert-ModId([string]$Path, [string]$ExpectedId) {
    $zip = [IO.Compression.ZipFile]::OpenRead($Path)
    try {
        $entry = $zip.GetEntry('META-INF/neoforge.mods.toml')
        if (-not $entry) { throw "Missing META-INF/neoforge.mods.toml in $Path" }
        $reader = [IO.StreamReader]::new($entry.Open())
        try { $metadata = $reader.ReadToEnd() } finally { $reader.Dispose() }
        $pattern = '(?m)^\s*modId\s*=\s*["'']' + [regex]::Escape($ExpectedId) + '["'']\s*$'
        if ($metadata -notmatch $pattern) {
            throw "Expected NeoForge mod id '$ExpectedId' in $Path"
        }
    } finally { $zip.Dispose() }
}

$safeOutput = Get-SafeOutputPath $OutputPath
$java = Join-Path $JavaHome 'bin/java.exe'
if (-not (Test-Path -LiteralPath $java -PathType Leaf)) { throw "Java executable is missing: $java" }
if (-not (Test-Path -LiteralPath $ModJar -PathType Leaf)) { throw "Production mod JAR is missing: $ModJar" }
Assert-ModId $ModJar 'aura'
if ($ObserverJar) {
    if (-not (Test-Path -LiteralPath $ObserverJar -PathType Leaf)) { throw "Observer mod JAR is missing: $ObserverJar" }
    Assert-ModId $ObserverJar 'aura_qa_observer'
}

if (Test-Path -LiteralPath $safeOutput) {
    if (-not (Test-Path -LiteralPath $safeOutput -PathType Container)) { throw "OutputPath is not a directory: $safeOutput" }
    if (@(Get-ChildItem -LiteralPath $safeOutput -Force).Count -gt 0) {
        throw "Refusing to install into a non-empty directory. Choose a fresh per-profile OutputPath: $safeOutput"
    }
}

$installerPath = Join-Path $safeOutput '.qa-downloads/neoforge-21.1.252-installer.jar'
$patchouliPath = Join-Path $safeOutput '.qa-downloads/Patchouli-1.21.1-93-NEOFORGE.jar'
$installerArgs = @('-Xms128m', '-Xmx768m', '-jar', $installerPath)
if ($Profile -eq 'client') { $installerArgs += @('--installClient', $safeOutput) }
else { $installerArgs += '--installServer' }

if (-not $Execute) {
    [pscustomobject]@{
        profile = $Profile
        minecraft = $minecraftVersion
        neoForge = $neoForgeVersion
        patchouli = $patchouliVersion
        outputPath = $safeOutput
        java = $java
        installerUrl = $installerUrl
        installerJar = $InstallerJar
        patchouliUrl = $patchouliUrl
        observerJar = $ObserverJar
        installerArguments = $installerArgs
        action = 'preview only; pass -Execute to download, install into the isolated output path, and stage the production mods'
    } | ConvertTo-Json -Depth 4
    exit 0
}

New-Item -ItemType Directory -Path $safeOutput -Force | Out-Null
if ($Profile -eq 'client') {
    $launcherProfiles = Join-Path $safeOutput 'launcher_profiles.json'
    [ordered]@{ profiles = [ordered]@{} } | ConvertTo-Json -Depth 3 | Set-Content -LiteralPath $launcherProfiles -Encoding UTF8
}
if ($InstallerJar) {
    if (-not (Test-Path -LiteralPath $InstallerJar -PathType Leaf)) { throw "Provided installer JAR is missing: $InstallerJar" }
    $expectedInstallerHash = Get-OfficialSha256 $installerUrl
    if ((Get-Sha256 $InstallerJar) -ne $expectedInstallerHash) { throw 'Provided installer JAR does not match the official NeoForge Maven checksum.' }
    New-Item -ItemType Directory -Path (Split-Path -Parent $installerPath) -Force | Out-Null
    Copy-Item -LiteralPath $InstallerJar -Destination $installerPath
    $installer = [pscustomobject]@{ Path = $installerPath; Sha256 = $expectedInstallerHash }
} else {
    $installer = Get-VerifiedDownload $installerUrl $installerPath
}
$patchouli = Get-VerifiedDownload $patchouliUrl $patchouliPath

$startInfo = [Diagnostics.ProcessStartInfo]::new()
$startInfo.FileName = $java
$startInfo.WorkingDirectory = $safeOutput
$startInfo.UseShellExecute = $false
$startInfo.CreateNoWindow = $true
$startInfo.RedirectStandardOutput = $true
$startInfo.RedirectStandardError = $true
foreach ($argument in $installerArgs) { $startInfo.ArgumentList.Add([string]$argument) }
$process = [Diagnostics.Process]::Start($startInfo)
$stdout = $process.StandardOutput.ReadToEndAsync()
$stderr = $process.StandardError.ReadToEndAsync()
$process.WaitForExit()
[IO.File]::WriteAllText((Join-Path $safeOutput 'installer.stdout.log'), $stdout.GetAwaiter().GetResult())
[IO.File]::WriteAllText((Join-Path $safeOutput 'installer.stderr.log'), $stderr.GetAwaiter().GetResult())
if ($process.ExitCode -ne 0) { throw "NeoForge $Profile installation failed with exit code $($process.ExitCode). See installer logs in $safeOutput." }

if ($Profile -eq 'client') {
    $installedProfile = Join-Path $safeOutput 'versions/neoforge-21.1.252/neoforge-21.1.252.json'
    if (-not (Test-Path -LiteralPath $installedProfile -PathType Leaf)) {
        throw "NeoForge installer exited successfully but did not create the expected client profile: $installedProfile"
    }
    foreach ($vanillaFile in @(
        (Join-Path $safeOutput 'versions/1.21.1/1.21.1.json'),
        (Join-Path $safeOutput 'versions/1.21.1/1.21.1.jar')
    )) {
        if (-not (Test-Path -LiteralPath $vanillaFile -PathType Leaf)) {
            throw "NeoForge client installer did not stage the required vanilla 1.21.1 artifact: $vanillaFile"
        }
    }
} else {
    $serverArgs = Join-Path $safeOutput 'libraries/net/neoforged/neoforge/21.1.252/win_args.txt'
    if (-not (Test-Path -LiteralPath $serverArgs -PathType Leaf)) {
        throw "NeoForge installer exited successfully but did not create Windows server arguments: $serverArgs"
    }
}

$modsDirectory = Join-Path $safeOutput 'mods'
New-Item -ItemType Directory -Path $modsDirectory -Force | Out-Null
$auraTarget = Join-Path $modsDirectory (Split-Path -Leaf $ModJar)
$patchouliTarget = Join-Path $modsDirectory (Split-Path -Leaf $patchouli.Path)
Copy-Item -LiteralPath $ModJar -Destination $auraTarget
Copy-Item -LiteralPath $patchouli.Path -Destination $patchouliTarget
Assert-ModId $auraTarget 'aura'
Assert-ModId $patchouliTarget 'patchouli'
$observerTarget = $null
if ($ObserverJar) {
    $observerTarget = Join-Path $modsDirectory (Split-Path -Leaf $ObserverJar)
    Copy-Item -LiteralPath $ObserverJar -Destination $observerTarget
    Assert-ModId $observerTarget 'aura_qa_observer'
}

$manifest = [ordered]@{
    profile = $Profile
    minecraft = $minecraftVersion
    neoForge = $neoForgeVersion
    patchouli = $patchouliVersion
    installedUtc = [DateTime]::UtcNow.ToString('o')
    outputPath = $safeOutput
    installerUrl = $installerUrl
    installerSha256 = $installer.Sha256
    auraJar = [ordered]@{ name = (Split-Path -Leaf $ModJar); sha256 = (Get-Sha256 $auraTarget) }
    patchouliJar = [ordered]@{ name = (Split-Path -Leaf $patchouliTarget); sha256 = (Get-Sha256 $patchouliTarget) }
    observerJar = if ($observerTarget) { [ordered]@{ name = (Split-Path -Leaf $observerTarget); sha256 = (Get-Sha256 $observerTarget) } } else { $null }
}
$manifestPath = Join-Path $safeOutput '.aura-cascade-neoforge-profile.json'
$manifest | ConvertTo-Json -Depth 5 | Set-Content -LiteralPath $manifestPath -Encoding UTF8

[pscustomobject]@{
    installed = $true
    profile = $Profile
    minecraft = $minecraftVersion
    neoForge = $neoForgeVersion
    patchouli = $patchouliVersion
    outputPath = $safeOutput
    manifest = $manifestPath
    auraJar = $auraTarget
    patchouliJar = $patchouliTarget
    observerJar = $observerTarget
    launchCommand = ".\scripts\qa\neoforge\launch-packaged-neoforge.ps1 -Profile $Profile -JavaHome `"$JavaHome`" -OutputPath `"$safeOutput`" -GenerateLaunchOnly"
} | ConvertTo-Json -Depth 4
