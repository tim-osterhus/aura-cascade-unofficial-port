param(
    [string]$JavaHome,
    [ValidatePattern('^[A-Za-z0-9_]{1,16}$')][string]$Username = 'AuraBrowserQA',
    [ValidateRange(512, 7000)][int]$StopAtMB = 3800,
    [switch]$StageDependencies,
    [switch]$Execute,
    [switch]$Interactive,
    [switch]$ClientProbe,
    [switch]$FeedbackProbe,
    [switch]$RenderProbe
)

$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.IO.Compression.FileSystem
if ($StageDependencies -and $Execute) { throw 'Use -StageDependencies or -Execute, not both.' }
$PrepareDependencies = $StageDependencies -or $Execute
if ([Environment]::OSVersion.Platform -ne [PlatformID]::Win32NT) {
    throw 'This bounded launcher is for the Windows x64 QA host only.'
}
if ([Runtime.InteropServices.RuntimeInformation]::ProcessArchitecture -ne [Runtime.InteropServices.Architecture]::X64) {
    throw 'The staged Minecraft natives require a Windows x64 process.'
}

$repo = (Resolve-Path (Join-Path $PSScriptRoot '../..')).Path
$root = Join-Path $repo 'build/qa-audit/packaged-client-1.21.11'
$runId = Get-Date -Format 'yyyyMMdd-HHmmss-fff'
$runDir = Join-Path $repo "build/qa-audit/packaged-client-1.21.11-runs/$runId"
$observerOutputDir = Join-Path $repo "build/qa-audit/observer/runs/1.21.11/$runId"
$versionId = '1.21.11'
$baseJsonPath = Join-Path $root "versions/$versionId/$versionId.json"
$clientJar = Join-Path $root "versions/$versionId/$versionId.jar"
$modsDir = Join-Path $root 'mods'
$patchouliSha256 = '08f8834ea942a5eefcb49811dd753f5f169fe76d142858f71914245f3b2d4462'
$fixtureName = 'disposablefixturecopy'
$fixtureDir = Join-Path $root "saves/$fixtureName"
$projectProperties = @{}
foreach ($line in Get-Content -LiteralPath (Join-Path $repo 'gradle.properties')) {
    if ($line -match '^\s*([^#!\s][^=]*)=(.*)$') { $projectProperties[$matches[1].Trim()] = $matches[2].Trim() }
}
$projectVersion = [string]$projectProperties['mod_version']
$expectedProperties = @{
    minecraft_version = '1.21.11'
    loader_version = '0.19.5'
    fabric_api_version = '0.141.6+1.21.11'
    patchouli_file = '8713841'
    energy_api_version = '4.2.0'
}
foreach ($key in $expectedProperties.Keys) {
    if ([string]$projectProperties[$key] -ne $expectedProperties[$key]) {
        throw "gradle.properties $key must be $($expectedProperties[$key]) for this packaged run."
    }
}
if (-not $projectVersion.EndsWith('+1.21.11')) { throw "Unexpected target mod version: $projectVersion" }
$archiveName = if ($projectProperties['archives_base_name']) { [string]$projectProperties['archives_base_name'] } else { 'aura-cascade' }
$auraJarSource = Join-Path $repo "build/libs/$archiveName-$projectVersion.jar"
$observerJarSource = Join-Path $repo "build/qa-audit/observer/aura-qa-observer-$projectVersion.jar"
$auraJarTarget = Join-Path $modsDir (Split-Path -Leaf $auraJarSource)
$observerJarTarget = Join-Path $modsDir (Split-Path -Leaf $observerJarSource)
$officialHosts = @('libraries.minecraft.net', 'maven.fabricmc.net', 'piston-meta.mojang.com', 'resources.download.minecraft.net')
$missingArtifacts = [System.Collections.Generic.List[string]]::new()

function Get-Sha1([string]$Path) {
    (Get-FileHash -Algorithm SHA1 -LiteralPath $Path).Hash.ToLowerInvariant()
}

function Read-FabricModMetadata([string]$Path) {
    $archive = [IO.Compression.ZipFile]::OpenRead($Path)
    try {
        $entry = $archive.GetEntry('fabric.mod.json')
        if (-not $entry) { throw "Missing fabric.mod.json in $Path" }
        $reader = [IO.StreamReader]::new($entry.Open())
        try { return $reader.ReadToEnd() | ConvertFrom-Json } finally { $reader.Dispose() }
    } finally {
        $archive.Dispose()
    }
}

function Stage-RemappedMod([string]$Source, [string]$Destination, [string]$ExpectedId) {
    if (-not (Test-Path -LiteralPath $Source -PathType Leaf)) { throw "Required packaged mod JAR is missing: $Source" }
    if ([IO.Path]::GetFileName($Source) -match '(?i)(-dev|-sources|-javadoc)\.jar$') { throw "Refusing non-production mod JAR: $Source" }
    $metadata = Read-FabricModMetadata $Source
    if ($metadata.id -ne $ExpectedId) { throw "Expected mod id $ExpectedId in $Source; found $($metadata.id)" }
    if (Test-Path -LiteralPath $Destination -PathType Leaf) {
        $sourceHash = (Get-FileHash -Algorithm SHA256 -LiteralPath $Source).Hash
        $targetHash = (Get-FileHash -Algorithm SHA256 -LiteralPath $Destination).Hash
        if ($sourceHash -ne $targetHash) { throw "A different staged mod already exists at $Destination; refusing to overwrite it." }
    } else {
        Copy-Item -LiteralPath $Source -Destination $Destination | Out-Null
    }
    $metadata
}

function Test-ExpectedFile([string]$Path, $Metadata) {
    if (-not (Test-Path -LiteralPath $Path -PathType Leaf)) { return $false }
    if ($Metadata.sha1) { return (Get-Sha1 $Path) -eq ([string]$Metadata.sha1).ToLowerInvariant() }
    if ($Metadata.size) { return (Get-Item -LiteralPath $Path).Length -eq [long]$Metadata.size }
    return $true
}

function Assert-OfficialUrl([string]$Url) {
    $uri = [Uri]$Url
    if ($uri.Scheme -ne 'https' -or $officialHosts -notcontains $uri.Host.ToLowerInvariant()) {
        throw "Refusing non-Mojang/Fabric artifact URL: $Url"
    }
}

function Get-LibraryPath($Library, $Artifact) {
    if ($Artifact.path) { return ([string]$Artifact.path).Replace('/', '\') }
    $parts = ([string]$Library.name) -split '@', 2
    $coordinate = $parts[0].Split(':')
    if ($coordinate.Length -lt 3) { throw "Cannot derive Maven path for $($Library.name)" }
    $extension = if ($parts.Length -gt 1) { $parts[1] } else { 'jar' }
    $classifier = if ($coordinate.Length -gt 3) { '-' + $coordinate[3] } else { '' }
    $fileName = "$($coordinate[1])-$($coordinate[2])$classifier.$extension"
    $groupPath = $coordinate[0].Replace('.', '\')
    Join-Path (Join-Path (Join-Path $groupPath $coordinate[1]) $coordinate[2]) $fileName
}

function Find-GradleArtifact($Library, [string]$FileName) {
    $coordinate = (([string]$Library.name) -split '@', 2)[0].Split(':')
    if ($coordinate.Length -lt 3) { return $null }
    $versionPath = Join-Path (Join-Path (Join-Path $env:USERPROFILE '.gradle/caches/modules-2/files-2.1') $coordinate[0]) (Join-Path $coordinate[1] $coordinate[2])
    if (-not (Test-Path -LiteralPath $versionPath -PathType Container)) { return $null }
    Get-ChildItem -LiteralPath $versionPath -Filter $FileName -File -Recurse | Select-Object -First 1 -ExpandProperty FullName
}

function Resolve-Artifact($Library, $Artifact) {
    $relative = Get-LibraryPath $Library $Artifact
    $destination = Join-Path $root (Join-Path 'libraries' $relative)
    if (Test-Path -LiteralPath $destination -PathType Leaf) {
        if (-not (Test-ExpectedFile $destination $Artifact)) { throw "Cached isolated artifact failed metadata validation: $relative" }
        return $destination
    }

    $fileName = [IO.Path]::GetFileName($relative)
    $candidates = @(
        (Join-Path (Join-Path $env:APPDATA '.minecraft/libraries') $relative),
        (Find-GradleArtifact $Library $fileName)
    ) | Where-Object { $_ }
    foreach ($candidate in $candidates) {
        if (Test-ExpectedFile $candidate $Artifact) {
            if (-not $PrepareDependencies) { return $candidate }
            New-Item -ItemType Directory -Path (Split-Path -Parent $destination) -Force | Out-Null
            Copy-Item -LiteralPath $candidate -Destination $destination | Out-Null
            return $destination
        }
    }

    if (-not $PrepareDependencies) {
        $missingArtifacts.Add($relative)
        return $destination
    }
    if (-not $Artifact.sha1 -and -not $Artifact.size) {
        throw "Refusing to download artifact without a published checksum or size: $relative"
    }
    $url = [string]$Artifact.url
    if (-not $url -and $Library.url) { $url = ([string]$Library.url).TrimEnd('/') + '/' + $relative.Replace('\', '/') }
    if (-not $url) { throw "No official artifact URL is available for $relative" }
    Assert-OfficialUrl $url
    New-Item -ItemType Directory -Path (Split-Path -Parent $destination) -Force | Out-Null
    $partial = "$destination.partial"
    Invoke-WebRequest -Uri $url -OutFile $partial -UseBasicParsing -TimeoutSec 90
    if (-not (Test-ExpectedFile $partial $Artifact)) { throw "Downloaded artifact failed metadata validation: $relative" }
    Move-Item -LiteralPath $partial -Destination $destination -Force | Out-Null
    $destination
}

function Test-AllowedByRules($Entry, $Features) {
    if (-not $Entry.rules -or @($Entry.rules).Count -eq 0) { return $true }
    $allowed = $false
    foreach ($rule in $Entry.rules) {
        $isMatch = $true
        if ($rule.os.name -and $rule.os.name -ne 'windows') { $isMatch = $false }
        if ($rule.os.arch -and $rule.os.arch -notmatch '^(x86_64|amd64)$') { $isMatch = $false }
        if ($isMatch -and $rule.os.version -and [Environment]::OSVersion.Version.ToString() -notmatch $rule.os.version) { $isMatch = $false }
        if ($isMatch -and $rule.features) {
            foreach ($feature in $rule.features.PSObject.Properties) {
                if (-not $Features.ContainsKey($feature.Name) -or [bool]$Features[$feature.Name] -ne [bool]$feature.Value) { $isMatch = $false; break }
            }
        }
        if ($isMatch) { $allowed = $rule.action -eq 'allow' }
    }
    $allowed
}

function Expand-Arguments($RawArguments, $Features, $Substitutions) {
    if ($null -eq $RawArguments) { return @() }
    $expanded = [System.Collections.Generic.List[string]]::new()
    foreach ($entry in @($RawArguments)) {
        if ($entry -is [string]) {
            $tokens = @([string]$entry)
        } else {
            if (-not (Test-AllowedByRules $entry $Features)) { continue }
            $tokens = @($entry.value)
        }
        foreach ($value in $tokens) {
            $token = [string]$value
            foreach ($key in $Substitutions.Keys) { $token = $token.Replace('${' + $key + '}', [string]$Substitutions[$key]) }
            if ($token -match '\$\{[^}]+\}') { throw "Unsupported launcher placeholder in argument: $token" }
            $expanded.Add($token)
        }
    }
    @($expanded)
}

function Ensure-AssetIndex($Base, [string]$AssetDirectory) {
    $id = [string]$Base.assetIndex.id
    $staged = Join-Path $AssetDirectory "indexes/$id.json"
    $metadata = [pscustomobject]@{ sha1 = $Base.assetIndex.sha1; size = $Base.assetIndex.size }
    if (-not (Test-ExpectedFile $staged $metadata)) {
        if (-not $PrepareDependencies) { $missingArtifacts.Add("asset index $id (isolated copy invalid or absent)"); return $null }
        $url = [string]$Base.assetIndex.url
        Assert-OfficialUrl $url
        New-Item -ItemType Directory -Path (Split-Path -Parent $staged) -Force | Out-Null
        $partial = "$staged.partial"
        Invoke-WebRequest -Uri $url -OutFile $partial -UseBasicParsing -TimeoutSec 90
        if (-not (Test-ExpectedFile $partial $metadata)) { throw "Downloaded Mojang asset index $id failed validation" }
        Move-Item -LiteralPath $partial -Destination $staged -Force | Out-Null
    }
    Get-Content -LiteralPath $staged -Raw | ConvertFrom-Json
}

function Ensure-Assets($Index, [string]$AssetDirectory) {
    $sourceRoots = @(
        (Join-Path $env:USERPROFILE '.gradle/caches/fabric-loom/assets'),
        (Join-Path $env:APPDATA '.minecraft/assets')
    )
    $status = [ordered]@{ verifiedCacheObjects = 0; copiedFromCache = 0; downloaded = 0; unresolved = 0 }
    foreach ($property in $Index.objects.PSObject.Properties) {
        $metadata = $property.Value
        $hash = ([string]$metadata.hash).ToLowerInvariant()
        $relative = "objects/$($hash.Substring(0, 2))/$hash"
        $target = Join-Path $AssetDirectory $relative
        $expected = [pscustomobject]@{ sha1 = $hash; size = $metadata.size }
        if (Test-ExpectedFile $target $expected) { continue }
        $source = $null
        foreach ($sourceRoot in $sourceRoots) {
            $candidate = Join-Path $sourceRoot $relative
            if (Test-ExpectedFile $candidate $expected) { $source = $candidate; break }
        }
        if ($source) {
            $status.verifiedCacheObjects++
            if ($PrepareDependencies) {
                New-Item -ItemType Directory -Path (Split-Path -Parent $target) -Force | Out-Null
                Copy-Item -LiteralPath $source -Destination $target -Force | Out-Null
                $status.copiedFromCache++
            }
            continue
        }
        if (-not $PrepareDependencies) {
            $status.unresolved++
            continue
        }
        $url = "https://resources.download.minecraft.net/$($hash.Substring(0, 2))/$hash"
        Assert-OfficialUrl $url
        New-Item -ItemType Directory -Path (Split-Path -Parent $target) -Force | Out-Null
        $partial = "$target.partial"
        Invoke-WebRequest -Uri $url -OutFile $partial -UseBasicParsing -TimeoutSec 90
        if ((Get-Sha1 $partial) -ne $hash -or (Get-Item -LiteralPath $partial).Length -ne [long]$metadata.size) {
            throw "Downloaded Mojang asset failed validation: $relative"
        }
        Move-Item -LiteralPath $partial -Destination $target -Force | Out-Null
        $status.downloaded++
    }
    [pscustomobject]$status
}

if (-not (Test-Path -LiteralPath $baseJsonPath -PathType Leaf) -or -not (Test-Path -LiteralPath $clientJar -PathType Leaf)) {
    throw 'The staged vanilla 1.21.11 JSON/JAR is missing.'
}
$base = Get-Content -LiteralPath $baseJsonPath -Raw | ConvertFrom-Json
if ($base.id -ne $versionId -or (Get-Sha1 $clientJar) -ne $base.downloads.client.sha1) {
    throw 'The staged vanilla client JAR does not match the 1.21.11 version metadata.'
}

$profiles = [System.Collections.Generic.List[object]]::new()
foreach ($path in Get-ChildItem -LiteralPath (Join-Path $root 'versions') -Filter '*.json' -File -Recurse) {
    try { $candidate = Get-Content -LiteralPath $path.FullName -Raw | ConvertFrom-Json } catch { continue }
    if ($candidate.inheritsFrom -eq $versionId -and $candidate.mainClass -eq 'net.fabricmc.loader.impl.launch.knot.KnotClient') {
        $profiles.Add([pscustomobject]@{ Path = $path.FullName; Data = $candidate })
    }
}
if ($profiles.Count -ne 1) { throw "Expected one installed Fabric KnotClient profile inheriting 1.21.11; found $($profiles.Count). Run the isolated installer first." }
$profile = $profiles[0].Data
if ($profile.id -ne 'fabric-loader-0.19.5-1.21.11') { throw "Unexpected Fabric profile ID: $($profile.id)" }

$java = $null
if ($Execute) {
    if (-not $JavaHome) { throw 'Pass -JavaHome when using -Execute.' }
    $java = Join-Path $JavaHome 'bin/java.exe'
    if (-not (Test-Path -LiteralPath $java -PathType Leaf)) { throw "Java executable is missing: $java" }
}
$auraMetadata = $null
$observerMetadata = $null
if (Test-Path -LiteralPath $auraJarSource -PathType Leaf) {
    $auraMetadata = Read-FabricModMetadata $auraJarSource
    if ($auraMetadata.id -ne 'aura' -or $auraMetadata.version -ne $projectVersion -or
        $auraMetadata.depends.minecraft -ne $versionId -or $auraMetadata.depends.fabricloader -ne '>=0.19.5' -or
        $auraMetadata.depends.patchouli -ne '>=1.21.11-94.4-FABRIC') {
        throw "Packaged Aura JAR metadata does not match the 1.21.11 release: $auraJarSource"
    }
}
if (Test-Path -LiteralPath $observerJarSource -PathType Leaf) {
    $observerMetadata = Read-FabricModMetadata $observerJarSource
    if ($observerMetadata.id -ne 'aura_qa_observer' -or $observerMetadata.depends.minecraft -ne $versionId) {
        throw "Packaged observer JAR must target Minecraft $versionId`: $observerJarSource"
    }
}
if ($Execute) {
    if (-not $projectVersion -or -not $auraMetadata -or -not $observerMetadata) {
        throw "Expected versioned remapped JARs: $auraJarSource and $observerJarSource"
    }
    $auraZip = [IO.Compression.ZipFile]::OpenRead($auraJarSource)
    try {
        if (-not $auraZip.GetEntry('META-INF/jars/energy-4.2.0.jar')) {
            throw 'The packaged Aura JAR is missing nested energy-4.2.0.jar.'
        }
    } finally { $auraZip.Dispose() }
    if (Get-ChildItem -LiteralPath $modsDir -Filter 'energy-*.jar' -File -ErrorAction SilentlyContinue) {
        throw 'Remove the duplicate standalone Energy API from isolated mods; Aura already nests it.'
    }
    $auraMetadata = Stage-RemappedMod $auraJarSource $auraJarTarget 'aura'
    $observerMetadata = Stage-RemappedMod $observerJarSource $observerJarTarget 'aura_qa_observer'
}
$modJars = if (Test-Path -LiteralPath $modsDir -PathType Container) {
    @(Get-ChildItem -LiteralPath $modsDir -Filter '*.jar' -File | Sort-Object Name)
} else { @() }
if ($Execute) {
    $modCounts = @{ 'fabric-api' = 0; patchouli = 0; aura = 0; aura_qa_observer = 0 }
    foreach ($modJar in $modJars) {
        if ($modJar.Name -match '(?i)(bridge|lab6|-dev\.jar$|-sources\.jar$)') {
            throw "Non-production/bridge artifact is not allowed in packaged mods: $($modJar.Name)"
        }
        $metadata = Read-FabricModMetadata $modJar.FullName
        $modId = [string]$metadata.id
        if (-not $modCounts.ContainsKey($modId)) {
            throw "Unexpected mod in isolated production run: $($modJar.Name) (id=$modId)"
        }
        $modCounts[$modId]++
        if ($modId -eq 'fabric-api' -and $metadata.version -ne '0.141.6+1.21.11') {
            throw "Fabric API must be 0.141.6+1.21.11: $($modJar.Name)"
        }
        if ($modId -eq 'patchouli') {
            $actualHash = (Get-FileHash -Algorithm SHA256 -LiteralPath $modJar.FullName).Hash.ToLowerInvariant()
            if ($metadata.version -ne '1.21.11-94.4-FABRIC' -or $actualHash -ne $patchouliSha256) {
                throw "Patchouli must be CurseForge file 8713841 with SHA-256 ${patchouliSha256}: $($modJar.Name)"
            }
        }
    }
    if ($modCounts['fabric-api'] -ne 1 -or $modCounts['patchouli'] -ne 1) {
        throw 'The isolated mods folder must contain exactly one target Fabric API and one verified Patchouli JAR.'
    }
    if ($Execute -and ($modCounts['aura'] -ne 1 -or $modCounts['aura_qa_observer'] -ne 1)) {
        throw 'The packaged run must contain exactly one Aura JAR and one observer JAR.'
    }
}
if ($Execute -and -not (Test-Path -LiteralPath $fixtureDir -PathType Container)) {
    throw "Disposable Quick Play fixture is missing: $fixtureDir"
}

$features = @{
    is_demo_user = $false
    has_custom_resolution = $true
    has_quick_plays_support = $true
    is_quick_play_singleplayer = $true
    is_quick_play_multiplayer = $false
    is_quick_play_realms = $false
}
$assetDirectory = Join-Path $root 'assets'
$assetIndex = Ensure-AssetIndex $base $assetDirectory
$assetStatus = if ($assetIndex) { Ensure-Assets $assetIndex $assetDirectory } else { [pscustomobject]@{ verifiedCacheObjects = 0; copiedFromCache = 0; downloaded = 0; unresolved = 1 } }
$missingAssetCount = [int]$assetStatus.unresolved

$classpath = [System.Collections.Generic.List[string]]::new()
$classpath.Add($clientJar)
$nativeJars = [System.Collections.Generic.List[string]]::new()
foreach ($version in @($base, $profile)) {
    foreach ($library in @($version.libraries)) {
        if (-not (Test-AllowedByRules $library $features)) { continue }
        $coordinate = (([string]$library.name) -split '@', 2)[0].Split(':')
        $classifier = if ($coordinate.Length -gt 3) { [string]$coordinate[3] } else { '' }
        if ($classifier -match '^natives-windows-' -and $classifier -ne 'natives-windows') { continue }
        $isWindowsNative = $classifier -eq 'natives-windows'
        $artifact = $library.downloads.artifact
        if (-not $artifact -and $library.PSObject.Properties['sha1']) {
            $artifact = [pscustomobject]@{ path = $null; sha1 = $library.sha1; size = $library.size; url = $null }
        }
        if (-not $artifact -and $library.url) {
            # Fabric's loader and intermediary entries use Maven checksum sidecars.
            $relative = Get-LibraryPath $library $null
            $url = ([string]$library.url).TrimEnd('/') + '/' + $relative.Replace('\', '/')
            Assert-OfficialUrl $url
            $checksumPath = Join-Path $root (Join-Path 'libraries' "$relative.sha1")
            $checksum = if (Test-Path -LiteralPath $checksumPath -PathType Leaf) {
                (Get-Content -LiteralPath $checksumPath -Raw).Trim()
            } elseif ($PrepareDependencies) {
                ([string](Invoke-RestMethod -Uri "$url.sha1" -TimeoutSec 60)).Trim()
            } else { '' }
            if (-not $checksum) {
                $missingArtifacts.Add("Published checksum for $relative")
                continue
            }
            if ($checksum -notmatch '^[0-9a-fA-F]{40}$') { throw "Invalid official Maven checksum for $relative" }
            if ($PrepareDependencies -and -not (Test-Path -LiteralPath $checksumPath)) {
                New-Item -ItemType Directory -Path (Split-Path -Parent $checksumPath) -Force | Out-Null
                [IO.File]::WriteAllText($checksumPath, $checksum)
            }
            $artifact = [pscustomobject]@{ path = $relative; sha1 = $checksum; size = $null; url = $url }
        }
        if ($artifact) {
            $resolvedArtifact = Resolve-Artifact $library $artifact
            if ($isWindowsNative) { $nativeJars.Add($resolvedArtifact) }
            else { $classpath.Add($resolvedArtifact) }
        }
        if ($library.natives.windows -and $library.downloads.classifiers) {
            $classifierName = ([string]$library.natives.windows).Replace('${arch}', '64')
            $classifier = $library.downloads.classifiers.PSObject.Properties[$classifierName]
            if ($classifier) { $nativeJars.Add((Resolve-Artifact $library $classifier.Value)) }
        }
    }
}
$classpathItems = @($classpath | Sort-Object -Unique)
$badClasspath = @($classpathItems | Where-Object { $_ -match '(?i)(build[\\/]classes|build[\\/]resources|devlaunchinjector|minecraft-merged|named-minecraft)' })
if ($badClasspath.Count -gt 0) { throw "Development/named Minecraft path leaked into packaged classpath: $($badClasspath -join ', ')" }

if ($StageDependencies) {
    if ($missingArtifacts.Count -gt 0 -or $missingAssetCount -gt 0) {
        throw "Dependency staging left unresolved files: $($missingArtifacts -join ', '); asset objects=$missingAssetCount"
    }
    Write-Output "Staged packaged client dependencies under $root without starting Java."
    Write-Output "Verified cache assets copied: $($assetStatus.copiedFromCache); official Mojang asset downloads: $($assetStatus.downloaded)."
    Write-Output "Classpath entries resolved: $($classpathItems.Count); native archives staged: $($nativeJars.Count)."
    exit 0
}

$offlineBytes = [Text.Encoding]::UTF8.GetBytes("OfflinePlayer:$Username")
$offlineHash = [Security.Cryptography.MD5]::HashData($offlineBytes)
$offlineHash[6] = ($offlineHash[6] -band 0x0f) -bor 0x30
$offlineHash[8] = ($offlineHash[8] -band 0x3f) -bor 0x80
$offlineHex = [Convert]::ToHexString($offlineHash).ToLowerInvariant()
$offlineUuid = '{0}-{1}-{2}-{3}-{4}' -f $offlineHex.Substring(0, 8), $offlineHex.Substring(8, 4), $offlineHex.Substring(12, 4), $offlineHex.Substring(16, 4), $offlineHex.Substring(20, 12)
$nativesDir = Join-Path $root 'natives'
$values = @{
    auth_player_name = $Username
    version_name = [string]$profile.id
    game_directory = $root
    assets_root = $assetDirectory
    assets_index_name = [string]$base.assetIndex.id
    auth_uuid = $offlineUuid
    auth_access_token = '0'
    clientid = ''
    auth_xuid = ''
    user_type = 'legacy'
    version_type = 'release'
    resolution_width = '1280'
    resolution_height = '720'
    quickPlayPath = (Join-Path $observerOutputDir 'quickPlay.json')
    quickPlaySingleplayer = $fixtureName
    quickPlayMultiplayer = ''
    quickPlayRealms = ''
    natives_directory = $nativesDir
    library_directory = (Join-Path $root 'libraries')
    classpath = ($classpathItems -join ';')
    classpath_separator = ';'
    launcher_name = 'AuraCascadeQA'
    launcher_version = '1'
}
$baseGameArgs = @(Expand-Arguments $base.arguments.game $features $values)
$profileGameArgs = @(Expand-Arguments $profile.arguments.game $features $values)
$hasCoreGameArgs = $profileGameArgs -contains '--username' -and $profileGameArgs -contains '--gameDir'
$gameArgs = if ($hasCoreGameArgs) { @($profileGameArgs) } else { @($baseGameArgs) + @($profileGameArgs) }
$filteredGameArgs = [System.Collections.Generic.List[string]]::new()
for ($i = 0; $i -lt $gameArgs.Count; $i++) {
    if ([string]$gameArgs[$i] -in @('--quickPlayPath', '--quickPlaySingleplayer')) { $i++; continue }
    $filteredGameArgs.Add([string]$gameArgs[$i])
}
$gameArgs = @($filteredGameArgs)
$gameArgs += @('--quickPlayPath', (Join-Path $observerOutputDir 'quickPlay.json'), '--quickPlaySingleplayer', $fixtureName)

$baseJvmArgs = @(Expand-Arguments $base.arguments.jvm $features $values)
$profileJvmArgs = @(Expand-Arguments $profile.arguments.jvm $features $values)
$selectedJvmArgs = if ($profileJvmArgs -contains '-cp' -or $profileJvmArgs -contains '-classpath') { @($profileJvmArgs) } else { @($baseJvmArgs) + @($profileJvmArgs) }
$jvmArgs = [System.Collections.Generic.List[string]]::new()
for ($i = 0; $i -lt $selectedJvmArgs.Count; $i++) {
    $arg = [string]$selectedJvmArgs[$i]
    if ($arg -match '^-Xm[sx]') { continue }
    if ($arg -in @('-cp', '-classpath', '--class-path')) { $i++; continue }
    if ($arg.StartsWith('-Djava.library.path=')) { continue }
    $jvmArgs.Add($arg)
}
$jvmArgs.Insert(0, '-Xms128m')
$jvmArgs.Insert(1, '-Xmx1280m')
$jvmArgs.Add('-XX:+UseSerialGC')
$jvmArgs.Add('-XX:ReservedCodeCacheSize=96m')
$jvmArgs.Add('-XX:MaxDirectMemorySize=256m')
$jvmArgs.Add("-Djava.library.path=$nativesDir")
$jvmArgs.Add("-Daura.qa.observer.dir=$observerOutputDir")
if ($ClientProbe) {
    $jvmArgs.Add('-Daura.qa.clientProbe=true')
    $jvmArgs.Add("-Daura.qa.clientProbe.output=$(Join-Path $observerOutputDir 'client-probe')")
}
if ($FeedbackProbe) {
    $jvmArgs.Add('-Daura.qa.feedbackProbe=true')
    $jvmArgs.Add("-Daura.qa.feedbackProbe.output=$(Join-Path $observerOutputDir 'feedback-probe')")
}
if ($RenderProbe) {
    $jvmArgs.Add('-Daura.qa.renderProbe=true')
    $jvmArgs.Add("-Daura.qa.renderProbe.output=$(Join-Path $observerOutputDir 'render-probe')")
}
if ($Interactive) { $jvmArgs.Add('-Daura.qa.observer.keepOpen=true') }
$jvmArgs.Add('-cp')
$jvmArgs.Add($values.classpath)

Write-Output "Fabric profile: $($profile.id) / $($profile.mainClass)"
Write-Output "Offline QA identity: $Username ($offlineUuid); no account credentials are used."
Write-Output "Classpath entries: $($classpathItems.Count); unresolved metadata artifacts: $($missingArtifacts.Count)"
Write-Output "Windows native archives: $($nativeJars.Count); isolated asset root: $assetDirectory"
Write-Output "Cached asset objects requiring download: $missingAssetCount"
Write-Output "Fixture required: $fixtureDir"
Write-Output "Aura JAR: $auraJarSource"
Write-Output "Observer JAR: $observerJarSource"
Write-Output "Observer output: $observerOutputDir"
if ($Interactive) { Write-Output 'Interactive mode: the client remains open after the observer finishes; close it normally when done.' }
Write-Output "Screenshot: $(Join-Path $observerOutputDir 'screenshot.png')"
if ($missingArtifacts.Count -gt 0) { Write-Output "Unresolved paths: $($missingArtifacts -join '; ')" }
if (-not $Execute) {
    Write-Output 'Preview only. Pass -Execute after the installed profile, built JARs, and fixture are ready.'
    exit 0
}
if ($missingArtifacts.Count -gt 0) { throw "Unresolved packaged artifacts remain: $($missingArtifacts -join ', ')" }

if ((Test-Path -LiteralPath $runDir) -or (Test-Path -LiteralPath $observerOutputDir)) {
    throw "Refusing to overwrite packaged QA run output: $runDir or $observerOutputDir"
}
New-Item -ItemType Directory -Path $nativesDir,$runDir,$observerOutputDir -Force | Out-Null
$optionsPath = Join-Path $root 'options.txt'
if (-not (Test-Path -LiteralPath $optionsPath -PathType Leaf)) {
    [IO.File]::WriteAllLines($optionsPath, @(
        'onboardAccessibility:false', 'renderDistance:4', 'simulationDistance:5',
        'maxFps:30', 'enableVsync:false', 'pauseOnLostFocus:false', 'guiScale:3',
        'graphicsMode:0', 'mipmapLevels:0', 'soundCategory_master:0.0'
    ))
}
foreach ($nativeJar in @($nativeJars | Sort-Object -Unique)) {
    [IO.Compression.ZipFile]::ExtractToDirectory($nativeJar, $nativesDir, $true)
}

$argumentFile = Join-Path $runDir 'java.args'
$arguments = @($jvmArgs) + @($profile.mainClass) + @($gameArgs)
$escaped = $arguments | ForEach-Object { '"' + ([string]$_).Replace('\', '\\').Replace('"', '\"') + '"' }
[IO.File]::WriteAllLines($argumentFile, $escaped)
$manifest = [pscustomobject]@{
    profile = $profile.id
    mainClass = $profile.mainClass
    minecraft = $versionId
    username = $Username
    identity = 'offline; accessToken=0; no external account credentials'
    quickPlaySingleplayer = $fixtureName
    gameDirectory = $root
    assetsDirectory = $assetDirectory
    observerOutputDirectory = $observerOutputDir
    runtimeNamespaceExpected = 'intermediary'
    auraJarOrigin = $auraJarTarget
    auraJarSha256 = (Get-FileHash -Algorithm SHA256 -LiteralPath $auraJarTarget).Hash.ToLowerInvariant()
    observerJarOrigin = $observerJarTarget
    observerJarSha256 = (Get-FileHash -Algorithm SHA256 -LiteralPath $observerJarTarget).Hash.ToLowerInvariant()
    classpathEntryCount = $classpathItems.Count
    classpathSha256 = [Convert]::ToHexString([Security.Cryptography.SHA256]::HashData([Text.Encoding]::UTF8.GetBytes($values.classpath))).ToLowerInvariant()
    mods = @($modJars | ForEach-Object { [pscustomobject]@{ name = $_.Name; sha256 = (Get-FileHash -Algorithm SHA256 -LiteralPath $_.FullName).Hash.ToLowerInvariant() } })
    unresolvedArtifacts = @($missingArtifacts)
    memoryLimitMiB = $StopAtMB
    interactive = [bool]$Interactive
    startedUtc = [DateTime]::UtcNow.ToString('o')
}
$manifest | ConvertTo-Json -Depth 5 | Set-Content -LiteralPath (Join-Path $runDir 'launch-manifest.json')

$started = [DateTime]::UtcNow
$windowStyle = if ($Interactive) { 'Normal' } else { 'Hidden' }
$client = Start-Process -FilePath $java -ArgumentList "@`"$argumentFile`"" -WorkingDirectory $root -WindowStyle $windowStyle -PassThru `
    -RedirectStandardOutput (Join-Path $runDir 'stdout.log') -RedirectStandardError (Join-Path $runDir 'stderr.log')
$client.Id | Set-Content -LiteralPath (Join-Path $runDir 'pid.txt')
$samples = [System.Collections.Generic.List[object]]::new()
$stoppedForMemory = $false
try {
    do {
        $client.Refresh()
        if ($client.HasExited) { break }
        $monitor = Get-Process -Id $PID
        $workingMB = ($client.WorkingSet64 + $monitor.WorkingSet64) / 1MB
        $privateMB = ($client.PrivateMemorySize64 + $monitor.PrivateMemorySize64) / 1MB
        $samples.Add([pscustomobject]@{
            utc = [DateTime]::UtcNow.ToString('o')
            working_set_mb = [math]::Round($workingMB, 1)
            private_bytes_mb = [math]::Round($privateMB, 1)
        })
        if ([math]::Max($workingMB, $privateMB) -gt $StopAtMB) {
            $stoppedForMemory = $true
            $client | Stop-Process -Force
            break
        }
        Start-Sleep -Milliseconds 1000
    } while (-not $client.HasExited)
} finally {
    $samples | Export-Csv -LiteralPath (Join-Path $runDir 'memory.csv') -NoTypeInformation
    $client.Refresh()
    $summary = [pscustomobject]@{
        started_utc = $started.ToString('o')
        finished_utc = [DateTime]::UtcNow.ToString('o')
        java_home = $JavaHome
        stop_at_mb = $StopAtMB
        peak_working_set_mb = ($samples | Measure-Object working_set_mb -Maximum).Maximum
        peak_private_bytes_mb = ($samples | Measure-Object private_bytes_mb -Maximum).Maximum
        stopped_for_memory = $stoppedForMemory
        exit_code = $client.ExitCode
        screenshot = (Join-Path $observerOutputDir 'screenshot.png')
        observer_manifest = (Join-Path $observerOutputDir 'manifest.json')
        manifest = (Join-Path $runDir 'launch-manifest.json')
        scope = 'Packaged client, integrated server, and this PowerShell monitor; one-second samples, not a hard OS cap.'
    }
    $summary | ConvertTo-Json | Set-Content -LiteralPath (Join-Path $runDir 'summary.json')
    $summary | ConvertTo-Json
}
if ($stoppedForMemory) { exit 124 }
if ($client.ExitCode -ne 0) { throw "Packaged client exited abnormally with code $($client.ExitCode)" }
$observerManifestPath = Join-Path $observerOutputDir 'manifest.json'
$observerScreenshot = Join-Path $observerOutputDir 'screenshot.png'
if ($Interactive -and -not (Test-Path -LiteralPath $observerManifestPath -PathType Leaf) -and
    -not (Test-Path -LiteralPath $observerScreenshot -PathType Leaf)) {
    Write-Output 'Interactive client closed before the observer report completed; no packaged-run pass is claimed.'
    exit 0
}
if (-not (Test-Path -LiteralPath $observerManifestPath -PathType Leaf) -or -not (Test-Path -LiteralPath $observerScreenshot -PathType Leaf)) {
    throw "Observer did not write its manifest and screenshot under $observerOutputDir"
}
$observerReport = Get-Content -LiteralPath $observerManifestPath -Raw | ConvertFrom-Json
$expectedAuraHash = [string]$manifest.auraJarSha256
$expectedAuraOrigin = [IO.Path]::GetFullPath($auraJarTarget)
if (-not $observerReport.success -or $observerReport.runtimeNamespace -ne 'intermediary' -or
    -not $observerReport.singleplayer -or -not $observerReport.worldLoaded -or
    $observerReport.stableRenderedFrames -lt 40 -or
    $observerReport.auraSha256 -ne $expectedAuraHash -or
    [IO.Path]::GetFullPath([string]$observerReport.auraOrigin) -ne $expectedAuraOrigin -or
    $observerReport.screenshotWidth -lt 1 -or $observerReport.screenshotHeight -lt 1 -or
    $observerReport.screenshotBytes -le 0) {
    throw "Observer manifest did not prove the packaged singleplayer run: $observerManifestPath"
}
Write-Output "Observer PASS: runtime=intermediary, Aura SHA-256=$expectedAuraHash"
if ($ClientProbe) {
    $probePath = Join-Path $observerOutputDir 'client-probe/manifest.json'
    if (-not (Test-Path -LiteralPath $probePath -PathType Leaf)) { throw 'Missing client interaction probe report.' }
    $probe = Get-Content -LiteralPath $probePath -Raw | ConvertFrom-Json
    if (-not $probe.complete -or -not $probe.success) { throw "Client interaction probe failed: $probePath" }
    Write-Output "Client interaction probe PASS: $probePath"
}
Write-Output "Screenshot: $observerScreenshot"
Write-Output "Observer manifest: $observerManifestPath"
foreach ($probeName in @($(if ($FeedbackProbe) { 'feedback-probe' }), $(if ($RenderProbe) { 'render-probe' }))) {
    if (-not $probeName) { continue }
    $probePath = Join-Path $observerOutputDir "$probeName/manifest.json"
    if (-not (Test-Path -LiteralPath $probePath -PathType Leaf)) { throw "Missing $probeName report." }
    $probe = Get-Content -LiteralPath $probePath -Raw | ConvertFrom-Json
    if (-not $probe.complete -or -not $probe.success) { throw "$probeName failed: $probePath" }
    Write-Output "$probeName state checks PASS (independent visual review still required): $probePath"
}
exit $client.ExitCode
