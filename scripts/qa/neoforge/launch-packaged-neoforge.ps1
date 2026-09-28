[CmdletBinding()]
param(
    [Parameter(Mandatory = $true)][ValidateSet('client', 'server')][string]$Profile,
    [Parameter(Mandatory = $true)][string]$JavaHome,
    [Parameter(Mandatory = $true)][string]$OutputPath,
    [string]$QuickPlaySingleplayer,
    [string]$ObserverDirectory,
    [switch]$ObserverScene,
    [switch]$GenerateLaunchOnly,
    [switch]$Execute,
    [ValidateRange(1024, 4500)][int]$StopAtMiB = 3800,
    [ValidateRange(30, 1800)][int]$TimeoutSeconds = 240
)

$ErrorActionPreference = 'Stop'
$minecraftVersion = '1.21.1'
$neoForgeVersion = '21.1.252'
$patchouliVersion = '1.21.1-93-NEOFORGE'
$profileId = 'neoforge-21.1.252'
$username = 'AuraCascadeQA'

if ($GenerateLaunchOnly -eq $Execute) { throw 'Specify exactly one of -GenerateLaunchOnly or -Execute.' }
if (-not [Environment]::Is64BitOperatingSystem) { throw 'This packaged profile helper currently supports Windows x64 only.' }
if ($Profile -eq 'server' -and ($QuickPlaySingleplayer -or $ObserverDirectory)) { throw 'Quick-play and observer options are client-only.' }
if ($ObserverDirectory -and -not $QuickPlaySingleplayer) { throw '-ObserverDirectory requires -QuickPlaySingleplayer.' }
if ($ObserverScene -and -not $QuickPlaySingleplayer) { throw '-ObserverScene requires -QuickPlaySingleplayer.' }
if (($QuickPlaySingleplayer -and $QuickPlaySingleplayer -match '[\\/:*?"<>|]') -or $QuickPlaySingleplayer -in @('.', '..')) {
    throw 'QuickPlaySingleplayer must be a single world folder name.'
}

function Test-PathWithin([string]$Path, [string]$Root) {
    $fullPath = [IO.Path]::GetFullPath($Path).TrimEnd('\')
    $fullRoot = [IO.Path]::GetFullPath($Root).TrimEnd('\')
    return $fullPath.Equals($fullRoot, [StringComparison]::OrdinalIgnoreCase) -or
        $fullPath.StartsWith($fullRoot + '\', [StringComparison]::OrdinalIgnoreCase)
}

function Get-SafeProfileRoot([string]$Path) {
    if (-not [IO.Path]::IsPathRooted($Path)) { throw 'OutputPath must be an absolute path.' }
    $fullPath = [IO.Path]::GetFullPath($Path)
    if (-not (Test-Path -LiteralPath $fullPath -PathType Container)) { throw "Installed profile directory is missing: $fullPath" }
    $resolved = (Resolve-Path -LiteralPath $fullPath).Path
    $segments = @($resolved -split '[\\/]')
    if ($segments -contains '.minecraft' -or $segments -contains 'instances') {
        throw 'Refusing to use a Minecraft or launcher instances directory as the packaged profile.'
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
            throw "Refusing to use a launcher-managed location: $protectedRoot"
        }
    }
    return $resolved
}

function Assert-OfficialUrl([string]$Url) {
    $uri = [Uri]$Url
    $hosts = @(
        'piston-meta.mojang.com', 'piston-data.mojang.com', 'resources.download.minecraft.net',
        'libraries.minecraft.net', 'maven.neoforged.net', 'maven.minecraftforge.net'
    )
    if ($uri.Scheme -ne 'https' -or $uri.Host -notin $hosts) { throw "Unexpected dependency URL: $Url" }
}

function Get-Sha1([string]$Path) {
    (Get-FileHash -LiteralPath $Path -Algorithm SHA1).Hash.ToLowerInvariant()
}

function Test-ExpectedFile([string]$Path, $Expected) {
    if (-not (Test-Path -LiteralPath $Path -PathType Leaf)) { return $false }
    $file = Get-Item -LiteralPath $Path
    if ($null -ne $Expected.size -and $file.Length -ne [long]$Expected.size) { return $false }
    if ($Expected.sha1 -and (Get-Sha1 $Path) -ne ([string]$Expected.sha1).ToLowerInvariant()) { return $false }
    return $true
}

function Get-SafeObserverDirectory([string]$Path, [string]$ProfileRoot) {
    $candidate = if ([IO.Path]::IsPathRooted($Path)) { [IO.Path]::GetFullPath($Path) } else { [IO.Path]::GetFullPath((Join-Path $ProfileRoot $Path)) }
    $parent = Split-Path -Parent $candidate
    if (-not (Test-Path -LiteralPath $parent -PathType Container)) { throw "ObserverDirectory parent must already exist: $parent" }
    $resolvedParent = (Resolve-Path -LiteralPath $parent).Path
    $resolved = Join-Path $resolvedParent (Split-Path -Leaf $candidate)
    if (Test-Path -LiteralPath $resolved) { $resolved = (Resolve-Path -LiteralPath $resolved).Path }
    $segments = @($resolved -split '[\\/]')
    if ($segments -contains '.minecraft' -or $segments -contains 'instances') { throw 'ObserverDirectory must not be inside a launcher-managed profile.' }
    $protectedRoots = @(
        (Join-Path $env:APPDATA '.minecraft'),
        (Join-Path $env:USERPROFILE '.minecraft'),
        (Join-Path $env:APPDATA 'PrismLauncher'),
        (Join-Path $env:APPDATA 'MultiMC'),
        (Join-Path $env:APPDATA 'CurseForge\Minecraft\Instances'),
        (Join-Path $env:USERPROFILE 'curseforge\minecraft\Instances')
    ) | Where-Object { $_ }
    foreach ($protectedRoot in $protectedRoots) {
        if (Test-PathWithin $resolved $protectedRoot) { throw "ObserverDirectory is inside a launcher-managed location: $protectedRoot" }
    }
    if ((Test-Path -LiteralPath $resolved -PathType Container) -and @(Get-ChildItem -LiteralPath $resolved -Force).Count -gt 0) {
        throw "Refusing to overwrite existing observer output: $resolved"
    }
    New-Item -ItemType Directory -Path $resolved -Force | Out-Null
    return $resolved
}

function Get-SafeDestination([string]$RelativePath) {
    $destination = [IO.Path]::GetFullPath((Join-Path $root ($RelativePath.Replace('/', '\'))))
    if (-not (Test-PathWithin $destination $root)) { throw "Metadata path escapes the isolated profile: $RelativePath" }
    return $destination
}

function Ensure-Download([string]$Url, [string]$Destination, [string]$Sha1, $Size) {
    Assert-OfficialUrl $Url
    $expected = [pscustomobject]@{ sha1 = $Sha1; size = $Size }
    if (Test-ExpectedFile $Destination $expected) { return $Destination }
    if (-not $Sha1 -and $null -eq $Size) { throw "No checksum or size is available for $Url" }
    New-Item -ItemType Directory -Path (Split-Path -Parent $Destination) -Force | Out-Null
    $partial = "$Destination.$PID.partial"
    Remove-Item -LiteralPath $partial -Force -ErrorAction SilentlyContinue
    Invoke-WebRequest -Uri $Url -OutFile $partial -UseBasicParsing -TimeoutSec 180
    if (-not (Test-ExpectedFile $partial $expected)) {
        Remove-Item -LiteralPath $partial -Force -ErrorAction SilentlyContinue
        throw "Downloaded dependency failed SHA-1/size validation: $Url"
    }
    Move-Item -LiteralPath $partial -Destination $Destination -Force
    return $Destination
}

function Get-LibraryPath($Library, $Artifact) {
    if ($Artifact.path) { return ([string]$Artifact.path).Replace('\', '/') }
    $parts = (([string]$Library.name -split '@', 2)[0]).Split(':')
    if ($parts.Length -lt 3 -or $parts.Length -gt 4) { throw "Cannot resolve Maven path for $($Library.name)" }
    $extension = if (([string]$Library.name).Contains('@')) { ([string]$Library.name).Split('@', 2)[1] } else { 'jar' }
    $fileName = "$($parts[1])-$($parts[2])"
    if ($parts.Length -eq 4) { $fileName += "-$($parts[3])" }
    return "$($parts[0].Replace('.', '/'))/$($parts[1])/$($parts[2])/$fileName.$extension"
}

function Ensure-LibraryArtifact($Library, $Artifact) {
    if (-not $Artifact) { return $null }
    $relative = Get-LibraryPath $Library $Artifact
    $destination = Get-SafeDestination "libraries/$relative"
    $sha1 = [string]$Artifact.sha1
    $size = $Artifact.size
    $url = [string]$Artifact.url
    if (-not $url -and $Library.url) { $url = ([string]$Library.url).TrimEnd('/') + '/' + $relative }
    if (-not $url) { throw "No download URL is available for $($Library.name)" }
    if (-not $sha1 -and $null -eq $size) {
        Assert-OfficialUrl $url
        $sidecar = ([string](Invoke-RestMethod -Uri "$url.sha1" -TimeoutSec 90)) -match '([0-9a-fA-F]{40})'
        if (-not $sidecar) { throw "No checksum or size is available for $relative" }
        $sha1 = $Matches[1].ToLowerInvariant()
    }
    Ensure-Download $url $destination $sha1 $size
}

function Test-AllowedByRules($Entry, $Features) {
    if (-not $Entry.rules -or @($Entry.rules).Count -eq 0) { return $true }
    $allowed = $false
    foreach ($rule in $Entry.rules) {
        $matches = $true
        if ($rule.os.name -and $rule.os.name -ne 'windows') { $matches = $false }
        if ($rule.os.arch -and $rule.os.arch -notmatch '^(x86_64|amd64)$') { $matches = $false }
        if ($matches -and $rule.os.version -and [Environment]::OSVersion.Version.ToString() -notmatch $rule.os.version) { $matches = $false }
        if ($matches -and $rule.features) {
            foreach ($feature in $rule.features.PSObject.Properties) {
                if (-not $Features.ContainsKey($feature.Name) -or [bool]$Features[$feature.Name] -ne [bool]$feature.Value) {
                    $matches = $false
                    break
                }
            }
        }
        if ($matches) { $allowed = $rule.action -eq 'allow' }
    }
    return $allowed
}

function Expand-Arguments($RawArguments, $Features, $Substitutions) {
    $expanded = [Collections.Generic.List[string]]::new()
    foreach ($entry in @($RawArguments)) {
        if ($entry -is [string]) { $tokens = @([string]$entry) }
        else {
            if (-not (Test-AllowedByRules $entry $Features)) { continue }
            $tokens = @($entry.value)
        }
        foreach ($value in $tokens) {
            $token = [string]$value
            foreach ($key in $Substitutions.Keys) { $token = $token.Replace('${' + $key + '}', [string]$Substitutions[$key]) }
            if ($token -match '\$\{[^}]+\}') { throw "Unsupported launcher placeholder: $token" }
            $expanded.Add($token)
        }
    }
    return @($expanded)
}

function Ensure-MinecraftBase {
    $manifestUrl = 'https://piston-meta.mojang.com/mc/game/version_manifest_v2.json'
    Assert-OfficialUrl $manifestUrl
    $versionManifest = Invoke-RestMethod -Uri $manifestUrl -TimeoutSec 90
    $versionEntry = @($versionManifest.versions | Where-Object id -EQ $minecraftVersion | Select-Object -First 1)
    if ($versionEntry.Count -ne 1) { throw "Minecraft $minecraftVersion is missing from the official version manifest." }
    $versionJsonPath = Get-SafeDestination "versions/$minecraftVersion/$minecraftVersion.json"
    Ensure-Download ([string]$versionEntry[0].url) $versionJsonPath ([string]$versionEntry[0].sha1) $null | Out-Null
    $base = Get-Content -LiteralPath $versionJsonPath -Raw | ConvertFrom-Json
    if ($base.id -ne $minecraftVersion) { throw "Unexpected Minecraft base metadata id: $($base.id)" }
    $client = $base.downloads.client
    if (-not $client.url -or -not $client.sha1) { throw 'Minecraft base metadata has no verified client JAR download.' }
    $clientPath = Get-SafeDestination "versions/$minecraftVersion/$minecraftVersion.jar"
    Ensure-Download ([string]$client.url) $clientPath ([string]$client.sha1) $client.size | Out-Null
    return [pscustomobject]@{ Metadata = $base; JsonPath = $versionJsonPath; ClientJar = $clientPath }
}

function Ensure-Assets($Base, [string]$AssetDirectory) {
    $assetIndex = $Base.assetIndex
    if (-not $assetIndex.id -or -not $assetIndex.url -or -not $assetIndex.sha1) { throw 'Minecraft base metadata is missing its asset index.' }
    $indexPath = Get-SafeDestination "assets/indexes/$($assetIndex.id).json"
    Ensure-Download ([string]$assetIndex.url) $indexPath ([string]$assetIndex.sha1) $assetIndex.size | Out-Null
    $index = Get-Content -LiteralPath $indexPath -Raw | ConvertFrom-Json
    $handler = [Net.Http.HttpClientHandler]::new()
    $handler.MaxConnectionsPerServer = 8
    $http = [Net.Http.HttpClient]::new($handler)
    $batch = [Collections.Generic.List[object]]::new()
    $downloaded = 0
    $checked = 0
    try {
        foreach ($property in $index.objects.PSObject.Properties) {
            $asset = $property.Value
            $hash = ([string]$asset.hash).ToLowerInvariant()
            if ($hash -notmatch '^[0-9a-f]{40}$') { throw "Invalid Mojang asset hash for $($property.Name)" }
            $destination = Get-SafeDestination "assets/objects/$($hash.Substring(0, 2))/$hash"
            if ((Test-Path -LiteralPath $destination -PathType Leaf) -and
                (Get-Item -LiteralPath $destination).Length -eq [long]$asset.size -and
                (Get-Sha1 $destination) -eq $hash) {
                $checked++
                continue
            }
            New-Item -ItemType Directory -Path (Split-Path -Parent $destination) -Force | Out-Null
            $batch.Add([pscustomobject]@{
                Url = "https://resources.download.minecraft.net/$($hash.Substring(0, 2))/$hash"
                Path = $destination
                Hash = $hash
                Size = [long]$asset.size
            })
            if ($batch.Count -ge 16) {
                Complete-AssetBatch $http $batch
                $downloaded += $batch.Count
                $batch.Clear()
                if (($checked + $downloaded) % 512 -lt 16) {
                    Write-Progress -Activity 'Preparing isolated Minecraft assets' -Status "$($checked + $downloaded) / $($index.objects.PSObject.Properties.Count)" -PercentComplete (100 * ($checked + $downloaded) / $index.objects.PSObject.Properties.Count)
                }
            }
        }
        if ($batch.Count -gt 0) {
            Complete-AssetBatch $http $batch
            $downloaded += $batch.Count
        }
    } finally {
        Write-Progress -Activity 'Preparing isolated Minecraft assets' -Completed
        $http.Dispose()
        $handler.Dispose()
    }
    return [pscustomobject]@{ Index = $index; Id = [string]$assetIndex.id; Checked = $checked; Downloaded = $downloaded }
}

function Complete-AssetBatch($HttpClient, $Batch) {
    $tasks = [Collections.Generic.List[Threading.Tasks.Task]]::new()
    foreach ($asset in $Batch) {
        Assert-OfficialUrl $asset.Url
        $tasks.Add($HttpClient.GetByteArrayAsync($asset.Url))
    }
    [Threading.Tasks.Task]::WhenAll($tasks.ToArray()).GetAwaiter().GetResult()
    for ($i = 0; $i -lt $Batch.Count; $i++) {
        $asset = $Batch[$i]
        $bytes = $tasks[$i].GetAwaiter().GetResult()
        if ($bytes.LongLength -ne $asset.Size) { throw "Mojang asset size mismatch: $($asset.Path)" }
        $sha = [Convert]::ToHexString([Security.Cryptography.SHA1]::HashData($bytes)).ToLowerInvariant()
        if ($sha -ne $asset.Hash) { throw "Mojang asset SHA-1 mismatch: $($asset.Path)" }
        [IO.File]::WriteAllBytes($asset.Path, $bytes)
    }
}

function Ensure-LoggingConfig($Base) {
    if (-not $Base.logging.client.file) { return '' }
    $file = $Base.logging.client.file
    $relativeId = ([string]$file.id).Replace('\', '/')
    $destination = Get-SafeDestination "assets/log_configs/$relativeId"
    Ensure-Download ([string]$file.url) $destination ([string]$file.sha1) $file.size | Out-Null
    return $destination
}

function Get-OfflineUuid([string]$Name) {
    $md5 = [Security.Cryptography.MD5]::Create()
    try { $bytes = $md5.ComputeHash([Text.Encoding]::UTF8.GetBytes("OfflinePlayer:$Name")) } finally { $md5.Dispose() }
    $bytes[6] = ($bytes[6] -band 0x0f) -bor 0x30
    $bytes[8] = ($bytes[8] -band 0x3f) -bor 0x80
    $hex = [Convert]::ToHexString($bytes).ToLowerInvariant()
    return '{0}-{1}-{2}-{3}-{4}' -f $hex.Substring(0, 8), $hex.Substring(8, 4), $hex.Substring(12, 4), $hex.Substring(16, 4), $hex.Substring(20, 12)
}

function Read-ModId([string]$JarPath) {
    $zip = [IO.Compression.ZipFile]::OpenRead($JarPath)
    try {
        $entry = $zip.GetEntry('META-INF/neoforge.mods.toml')
        if (-not $entry) { throw "Missing NeoForge mod metadata in $JarPath" }
        $reader = [IO.StreamReader]::new($entry.Open())
        try { $metadata = $reader.ReadToEnd() } finally { $reader.Dispose() }
        if ($metadata -match '(?m)^\s*modId\s*=\s*["'']([^"'']+)["'']\s*$') { return $Matches[1] }
        throw "No NeoForge mod id found in $JarPath"
    } finally { $zip.Dispose() }
}

function ConvertTo-JavaArgLine([string]$Argument) {
    if ($Argument.Contains("`n") -or $Argument.Contains("`r")) { throw 'Java launch arguments cannot contain line breaks.' }
    return '"' + $Argument.Replace('\', '\\').Replace('"', '\"') + '"'
}

$root = Get-SafeProfileRoot $OutputPath
$javaHomeResolved = (Resolve-Path -LiteralPath $JavaHome).Path
$java = Join-Path $javaHomeResolved 'bin/java.exe'
if (-not (Test-Path -LiteralPath $java -PathType Leaf)) { throw "Java executable is missing: $java" }
$installManifestPath = Join-Path $root '.aura-cascade-neoforge-profile.json'
if (-not (Test-Path -LiteralPath $installManifestPath -PathType Leaf)) { throw 'Run install-packaged-neoforge.ps1 for this isolated profile first.' }
$installManifest = Get-Content -LiteralPath $installManifestPath -Raw | ConvertFrom-Json
if ($installManifest.profile -ne $Profile -or $installManifest.minecraft -ne $minecraftVersion -or $installManifest.neoForge -ne $neoForgeVersion) {
    throw 'The isolated profile marker does not match the requested NeoForge target/profile.'
}

$modsDirectory = Join-Path $root 'mods'
$modJars = @(Get-ChildItem -LiteralPath $modsDirectory -Filter '*.jar' -File)
$modIds = @{}
foreach ($modJar in $modJars) {
    $id = Read-ModId $modJar.FullName
    if ($id -notin @('aura', 'patchouli', 'aura_qa_observer')) { throw "Unexpected mod in isolated profile: $($modJar.Name) (id=$id)" }
    if ($modIds.ContainsKey($id)) { throw "Duplicate packaged mod id in isolated profile: $id" }
    $modIds[$id] = $modJar
}
if (-not $modIds.ContainsKey('aura') -or -not $modIds.ContainsKey('patchouli')) {
    throw 'The isolated mods directory must contain exactly one Aura production JAR and the required Patchouli JAR.'
}
if ([bool]$installManifest.observerJar -ne $modIds.ContainsKey('aura_qa_observer')) {
    throw 'Observer JAR presence does not match the isolated install manifest.'
}
if ((Get-FileHash -LiteralPath $modIds.aura.FullName -Algorithm SHA256).Hash.ToLowerInvariant() -ne [string]$installManifest.auraJar.sha256) {
    throw 'Packaged Aura JAR changed after the isolated installation was recorded.'
}
if ((Get-FileHash -LiteralPath $modIds.patchouli.FullName -Algorithm SHA256).Hash.ToLowerInvariant() -ne [string]$installManifest.patchouliJar.sha256) {
    throw 'Patchouli JAR changed after the isolated installation was recorded.'
}
if ($modIds.ContainsKey('aura_qa_observer') -and
    (Get-FileHash -LiteralPath $modIds.aura_qa_observer.FullName -Algorithm SHA256).Hash.ToLowerInvariant() -ne [string]$installManifest.observerJar.sha256) {
    throw 'Observer JAR changed after the isolated installation was recorded.'
}
if ($modIds.ContainsKey('aura_qa_observer') -and -not $QuickPlaySingleplayer) {
    throw 'The packaged observer mod requires -QuickPlaySingleplayer.'
}

$runDirectory = Join-Path $root '.qa/neoforge-21.1.252'
New-Item -ItemType Directory -Path $runDirectory -Force | Out-Null
$javaArguments = [Collections.Generic.List[string]]::new()
$launchDetails = [ordered]@{
    profile = $Profile
    minecraft = $minecraftVersion
    neoForge = $neoForgeVersion
    java = $java
    workingDirectory = $root
    memoryLimitMiB = if ($Profile -eq 'client') { 1280 } else { 768 }
    monitorStopAtMiB = $StopAtMiB
    timeoutSeconds = $TimeoutSeconds
    mods = @($modJars | ForEach-Object { [ordered]@{ name = $_.Name; id = (Read-ModId $_.FullName); sha256 = (Get-FileHash -LiteralPath $_.FullName -Algorithm SHA256).Hash.ToLowerInvariant() } })
    createdUtc = [DateTime]::UtcNow.ToString('o')
}

if ($Profile -eq 'client') {
    $profilePath = Join-Path $root "versions/$profileId/$profileId.json"
    if (-not (Test-Path -LiteralPath $profilePath -PathType Leaf)) { throw "Run the isolated NeoForge client installer first: $profilePath" }
    $clientMetadata = Get-Content -LiteralPath $profilePath -Raw | ConvertFrom-Json
    if ($clientMetadata.id -ne $profileId -or $clientMetadata.inheritsFrom -ne $minecraftVersion -or $clientMetadata.mainClass -ne 'cpw.mods.bootstraplauncher.BootstrapLauncher') {
        throw 'Installed NeoForge client profile metadata does not match NeoForge 21.1.252 / Minecraft 1.21.1.'
    }
    $baseInfo = Ensure-MinecraftBase
    $base = $baseInfo.Metadata
    $assetDirectory = Join-Path $root 'assets'
    $assetStatus = Ensure-Assets $base $assetDirectory
    $logConfigPath = Ensure-LoggingConfig $base
    $quickPlayEnabled = -not [string]::IsNullOrWhiteSpace($QuickPlaySingleplayer)
    $observerOutputDirectory = $null
    if ($quickPlayEnabled) {
        $observerPath = if ($ObserverDirectory) { $ObserverDirectory } else { Join-Path $runDirectory 'observer' }
        $observerOutputDirectory = Get-SafeObserverDirectory $observerPath $root
        $worldPath = Join-Path $root "saves/$QuickPlaySingleplayer"
        if ($Execute -and -not (Test-Path -LiteralPath $worldPath -PathType Container)) {
            throw "Quick-play singleplayer world is missing from the isolated profile: $worldPath"
        }
    }

    $features = @{
        is_demo_user = $false
        has_custom_resolution = $true
        has_quick_plays_support = $quickPlayEnabled
        is_quick_play_singleplayer = $quickPlayEnabled
        is_quick_play_multiplayer = $false
        is_quick_play_realms = $false
    }
    $classpath = [Collections.Generic.List[string]]::new()
    $classpath.Add($baseInfo.ClientJar)
    $nativeJars = [Collections.Generic.List[string]]::new()
    foreach ($version in @($base, $clientMetadata)) {
        foreach ($library in @($version.libraries)) {
            if (-not (Test-AllowedByRules $library $features)) { continue }
            if ($library.name -match ':natives-windows-(arm64|x86)$') { continue }
            if ($library.downloads.artifact) {
                $artifactPath = Ensure-LibraryArtifact $library $library.downloads.artifact
                if ($artifactPath) {
                    if ($library.downloads.artifact.path -match '(?i)natives-(windows|win32)') { $nativeJars.Add($artifactPath) }
                    else { $classpath.Add($artifactPath) }
                }
            } elseif (-not $library.natives.windows) {
                throw "Library has no Windows artifact: $($library.name)"
            }
            if ($library.natives.windows -and $library.downloads.classifiers) {
                $classifierName = ([string]$library.natives.windows).Replace('${arch}', '64')
                $classifierProperty = $library.downloads.classifiers.PSObject.Properties[$classifierName]
                if (-not $classifierProperty) { throw "Windows native classifier '$classifierName' is missing for $($library.name)" }
                $nativePath = Ensure-LibraryArtifact $library $classifierProperty.Value
                if ($nativePath) { $nativeJars.Add($nativePath) }
            }
        }
    }
    $classpathItems = @($classpath | Select-Object -Unique)
    if ($classpathItems.Count -eq 0) { throw 'Resolved packaged client classpath is empty.' }
    $classpathText = $classpathItems -join [IO.Path]::PathSeparator
    $nativesDirectory = Join-Path $root "natives/$profileId"
    New-Item -ItemType Directory -Path $nativesDirectory -Force | Out-Null
    foreach ($nativeJar in @($nativeJars | Select-Object -Unique)) {
        $archive = [IO.Compression.ZipFile]::OpenRead($nativeJar)
        try {
            foreach ($entry in $archive.Entries) {
                if ($entry.FullName.EndsWith('/') -or [IO.Path]::GetExtension($entry.FullName) -notmatch '(?i)^\.dll$') { continue }
                $nativePath = [IO.Path]::GetFullPath((Join-Path $nativesDirectory ([IO.Path]::GetFileName($entry.FullName))))
                if (-not (Test-PathWithin $nativePath $nativesDirectory)) { throw "Native archive entry escapes the isolated natives directory: $($entry.FullName)" }
                New-Item -ItemType Directory -Path (Split-Path -Parent $nativePath) -Force | Out-Null
                $inputStream = $entry.Open()
                $outputStream = [IO.File]::Create($nativePath)
                try { $inputStream.CopyTo($outputStream) } finally { $outputStream.Dispose(); $inputStream.Dispose() }
            }
        } finally { $archive.Dispose() }
    }

    $offlineUuid = Get-OfflineUuid $username
    $substitutions = @{
        auth_player_name = $username
        version_name = [string]$clientMetadata.id
        game_directory = $root
        assets_root = $assetDirectory
        assets_index_name = [string]$assetStatus.Id
        auth_uuid = $offlineUuid
        auth_access_token = '0'
        clientid = ''
        auth_xuid = ''
        user_type = 'legacy'
        version_type = 'release'
        resolution_width = '1280'
        resolution_height = '720'
        natives_directory = $nativesDirectory
        library_directory = (Join-Path $root 'libraries')
        classpath = $classpathText
        classpath_separator = [string][IO.Path]::PathSeparator
        launcher_name = 'AuraCascadeNeoForgeQA'
        launcher_version = '1'
        path = $logConfigPath
        quickPlayPath = if ($observerOutputDirectory) { Join-Path $observerOutputDirectory 'quickPlay.json' } else { '' }
        quickPlaySingleplayer = if ($quickPlayEnabled) { $QuickPlaySingleplayer } else { '' }
        quickPlayMultiplayer = ''
        quickPlayRealms = ''
    }
    $baseGameArgs = @(Expand-Arguments $base.arguments.game $features $substitutions)
    $profileGameArgs = @(Expand-Arguments $clientMetadata.arguments.game $features $substitutions)
    if ($profileGameArgs -contains '--username' -and $profileGameArgs -contains '--gameDir') { $gameArgs = @($profileGameArgs) }
    else { $gameArgs = @($baseGameArgs) + @($profileGameArgs) }
    $filteredGameArgs = [Collections.Generic.List[string]]::new()
    $quickPlaySwitches = @('--quickPlayPath', '--quickPlaySingleplayer', '--quickPlayMultiplayer', '--quickPlayRealms')
    for ($i = 0; $i -lt $gameArgs.Count; $i++) {
        if ([string]$gameArgs[$i] -in $quickPlaySwitches) { $i++; continue }
        $filteredGameArgs.Add([string]$gameArgs[$i])
    }
    $gameArgs = @($filteredGameArgs)
    if ($quickPlayEnabled) { $gameArgs += @('--quickPlayPath', (Join-Path $observerOutputDirectory 'quickPlay.json'), '--quickPlaySingleplayer', $QuickPlaySingleplayer) }

    $rawJvmArgs = @(Expand-Arguments $base.arguments.jvm $features $substitutions) + @(Expand-Arguments $clientMetadata.arguments.jvm $features $substitutions)
    for ($i = 0; $i -lt $rawJvmArgs.Count; $i++) {
        $argument = [string]$rawJvmArgs[$i]
        # The isolated profile keeps the vanilla JAR under its original version name.
        if ($argument.StartsWith('-DignoreList=')) { $argument += ",$(Split-Path -Leaf $baseInfo.ClientJar)" }
        if ($argument -match '^-Xm[sx]') { continue }
        if ($argument -in @('-cp', '-classpath', '--class-path')) { $i++; continue }
        if ($argument.StartsWith('-Djava.library.path=')) { continue }
        $javaArguments.Add($argument)
    }
    $javaArguments.Insert(0, '-Xms128m')
    $javaArguments.Insert(1, '-Xmx1280m')
    $javaArguments.Add('-XX:+UseSerialGC')
    $javaArguments.Add('-XX:ReservedCodeCacheSize=96m')
    $javaArguments.Add('-XX:MaxDirectMemorySize=256m')
    $javaArguments.Add("-Djava.library.path=$nativesDirectory")
    if ($observerOutputDirectory) { $javaArguments.Add("-Daura.qa.observer.dir=$observerOutputDirectory") }
    if ($ObserverScene) { $javaArguments.Add('-Daura.qa.observer.scene=true') }
    $javaArguments.Add('-cp')
    $javaArguments.Add($classpathText)
    $javaArguments.Add([string]$clientMetadata.mainClass)
    foreach ($argument in $gameArgs) { $javaArguments.Add([string]$argument) }

    $argumentFile = Join-Path $runDirectory 'client.java.args'
    $lines = @($javaArguments | ForEach-Object { ConvertTo-JavaArgLine ([string]$_) })
    [IO.File]::WriteAllLines($argumentFile, $lines)
    $launchDetails.mainClass = [string]$clientMetadata.mainClass
    $launchDetails.offlineIdentity = [ordered]@{ username = $username; uuid = $offlineUuid; externalCredentials = $false }
    $launchDetails.clientJar = $baseInfo.ClientJar
    $launchDetails.assetIndex = $assetStatus.Id
    $launchDetails.assetObjectsDownloaded = $assetStatus.Downloaded
    $launchDetails.assetObjectsVerifiedExisting = $assetStatus.Checked
    $launchDetails.classpathEntries = $classpathItems.Count
    $launchDetails.nativeArchives = @($nativeJars | Select-Object -Unique).Count
    $launchDetails.argumentsFile = $argumentFile
    $launchDetails.quickPlaySingleplayer = if ($quickPlayEnabled) { $QuickPlaySingleplayer } else { $null }
    $launchDetails.observerDirectory = $observerOutputDirectory
    $launchArguments = @("@$argumentFile")
} else {
    $serverArgumentsFile = Join-Path $root 'libraries/net/neoforged/neoforge/21.1.252/win_args.txt'
    if (-not (Test-Path -LiteralPath $serverArgumentsFile -PathType Leaf)) { throw 'Run the isolated NeoForge server installer first.' }
    $javaArguments.Add('-Xms128m')
    $javaArguments.Add('-Xmx768m')
    $javaArguments.Add('-XX:+UseSerialGC')
    $javaArguments.Add('-XX:MaxDirectMemorySize=128m')
    $javaArguments.Add("@$serverArgumentsFile")
    $javaArguments.Add('nogui')
    $launchDetails.argumentsFile = $serverArgumentsFile
    $launchDetails.eulaAccepted = $false
    $launchArguments = @($javaArguments)
    if ($Execute) {
        $eulaPath = Join-Path $root 'eula.txt'
        if (-not (Test-Path -LiteralPath $eulaPath -PathType Leaf) -or (Get-Content -LiteralPath $eulaPath -Raw) -notmatch '(?m)^eula=true\s*$') {
            throw 'The isolated server EULA has not been accepted. Set eula=true in this profile only after reviewing and accepting the Minecraft EULA.'
        }
        $launchDetails.eulaAccepted = $true
    }
}

$launchDetails.arguments = @($launchArguments)
$launchDetails.generatedUtc = [DateTime]::UtcNow.ToString('o')
$launchManifestPath = Join-Path $runDirectory 'launch-manifest.json'
$launchDetails | ConvertTo-Json -Depth 7 | Set-Content -LiteralPath $launchManifestPath -Encoding UTF8

if ($GenerateLaunchOnly) {
    [pscustomobject]@{
        generated = $true
        profile = $Profile
        java = $java
        outputPath = $root
        manifest = $launchManifestPath
        argumentFile = if ($Profile -eq 'client') { Join-Path $runDirectory 'client.java.args' } else { $serverArgumentsFile }
        action = 'launch not started; rerun with -Execute only when ready'
    } | ConvertTo-Json -Depth 4
    exit 0
}

$processInfo = [Diagnostics.ProcessStartInfo]::new()
$processInfo.FileName = $java
$processInfo.WorkingDirectory = $root
$processInfo.UseShellExecute = $false
$processInfo.CreateNoWindow = $true
$processInfo.RedirectStandardOutput = $true
$processInfo.RedirectStandardError = $true
if ($Profile -eq 'client') { $processInfo.ArgumentList.Add("@$(Join-Path $runDirectory 'client.java.args')") }
else { foreach ($argument in $javaArguments) { $processInfo.ArgumentList.Add([string]$argument) } }

$started = [DateTime]::UtcNow
$process = [Diagnostics.Process]::Start($processInfo)
$stdout = $process.StandardOutput.ReadToEndAsync()
$stderr = $process.StandardError.ReadToEndAsync()
$process.Id | Set-Content -LiteralPath (Join-Path $runDirectory 'pid.txt')
$samples = [Collections.Generic.List[object]]::new()
$stoppedForMemory = $false
$timedOut = $false
try {
    while ($true) {
        $process.Refresh()
        if ($process.HasExited) { break }
        $monitor = Get-Process -Id $PID
        $gameWorkingSet = $process.WorkingSet64
        $gamePrivateBytes = $process.PrivateMemorySize64
        $workingMiB = ($gameWorkingSet + $monitor.WorkingSet64) / 1MB
        $privateMiB = ($gamePrivateBytes + $monitor.PrivateMemorySize64) / 1MB
        $samples.Add([pscustomobject]@{
            utc = [DateTime]::UtcNow.ToString('o')
            game_working_set_mib = [math]::Round($gameWorkingSet / 1MB, 1)
            game_private_bytes_mib = [math]::Round($gamePrivateBytes / 1MB, 1)
            owned_working_set_mib = [math]::Round($workingMiB, 1)
            owned_private_bytes_mib = [math]::Round($privateMiB, 1)
        })
        if ([math]::Max($workingMiB, $privateMiB) -gt $StopAtMiB) {
            $stoppedForMemory = $true
            $process.Kill($true)
            break
        }
        if (([DateTime]::UtcNow - $started).TotalSeconds -ge $TimeoutSeconds) {
            $timedOut = $true
            $process.Kill($true)
            break
        }
        Start-Sleep -Milliseconds 1000
    }
    if (-not $process.HasExited) { $process.WaitForExit() }
} finally {
    if (-not $process.HasExited) { $process.Kill($true); $process.WaitForExit() }
    [IO.File]::WriteAllText((Join-Path $runDirectory 'stdout.log'), $stdout.GetAwaiter().GetResult())
    [IO.File]::WriteAllText((Join-Path $runDirectory 'stderr.log'), $stderr.GetAwaiter().GetResult())
    $samples | Export-Csv -LiteralPath (Join-Path $runDirectory 'memory.csv') -NoTypeInformation
    $process.Refresh()
    $summary = [ordered]@{
        startedUtc = $started.ToString('o')
        finishedUtc = [DateTime]::UtcNow.ToString('o')
        profile = $Profile
        java = $java
        stopAtMiB = $StopAtMiB
        timeoutSeconds = $TimeoutSeconds
        peakWorkingSetMiB = ($samples | Measure-Object owned_working_set_mib -Maximum).Maximum
        peakPrivateBytesMiB = ($samples | Measure-Object owned_private_bytes_mib -Maximum).Maximum
        gamePeakWorkingSetMiB = ($samples | Measure-Object game_working_set_mib -Maximum).Maximum
        gamePeakPrivateBytesMiB = ($samples | Measure-Object game_private_bytes_mib -Maximum).Maximum
        stoppedForMemory = $stoppedForMemory
        timedOut = $timedOut
        processId = $process.Id
        exitCode = if ($process.HasExited) { $process.ExitCode } else { $null }
        scope = 'Packaged Minecraft process plus this PowerShell monitor; one-second samples, not a hard OS cap. The configured Java heap is separately capped at 1280 MiB client or 768 MiB server.'
    }
    $summary | ConvertTo-Json -Depth 4 | Set-Content -LiteralPath (Join-Path $runDirectory 'summary.json') -Encoding UTF8
    $summary | ConvertTo-Json -Depth 4
}
if ($stoppedForMemory -or $timedOut) { exit 124 }
if ($process.ExitCode -ne 0) { throw "Packaged NeoForge $Profile exited with code $($process.ExitCode)." }
