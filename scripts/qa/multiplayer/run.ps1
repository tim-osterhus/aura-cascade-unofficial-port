param(
    [Parameter(Mandatory)][string]$JavaHome,
    [Parameter(Mandatory)][string]$ClientLaunchManifest,
    [Parameter(Mandatory)][string]$AuraJar,
    [Parameter(Mandatory)][string]$ProbeJar,
    [ValidateRange(20000, 60000)][int]$Port = 25576,
    [switch]$Execute
)

$ErrorActionPreference = 'Stop'
$repo = [IO.Path]::GetFullPath((Join-Path $PSScriptRoot '../../..'))
if (-not $Execute) {
    Write-Output 'Preview only. No files/processes created. Requires a remapped private probe JAR and an existing packaged launch manifest + sibling java.args.'
    Write-Output "Execution will create fresh game directories under build/qa-audit/multiplayer, bind 127.0.0.1:$Port, and run AuraOwnerQA + AuraWitnessQA offline."
    Write-Output 'Run no other Aura Java workload concurrently. This session aggregate soft guard is 3800 MiB; forced guard 4000 MiB; total deadline 600 seconds.'
    Write-Output 'Memory-tuned profile: 512 MiB heap per JVM, 2 visible processors, client render distance 2; target measured aggregate below 3500 MiB (not guaranteed).'
    return
}
if ($PSVersionTable.PSVersion.Major -lt 7) { throw 'Use PowerShell 7.' }
Add-Type -AssemblyName System.IO.Compression.FileSystem
$java = (Resolve-Path -LiteralPath (Join-Path $JavaHome 'bin/java.exe')).Path
$manifest = Get-Content -LiteralPath $ClientLaunchManifest -Raw | ConvertFrom-Json
if ($manifest.minecraft -ne '1.21.11' -or $manifest.runtimeNamespaceExpected -ne 'intermediary' -or $manifest.identity -notmatch '^offline;') {
    throw 'Template must be an existing offline packaged 1.21.11 launch manifest.'
}
$templateArgs = @(Get-Content -LiteralPath (Join-Path (Split-Path -Parent $ClientLaunchManifest) 'java.args') | ForEach-Object { ConvertFrom-Json -InputObject $_ })
function Template-Value([string]$Flag) {
    $index = [Array]::IndexOf($templateArgs, $Flag)
    if ($index -lt 0 -or $index + 1 -ge $templateArgs.Count) { throw "Template lacks $Flag" }
    [string]$templateArgs[$index + 1]
}
if ((Template-Value '--accessToken') -ne '0') { throw 'Only credential-free offline templates are accepted.' }
$classpath = Template-Value '-cp'
$assets = Template-Value '--assetsDir'
$assetIndex = Template-Value '--assetIndex'
$version = Template-Value '--version'
$nativeArgument = @($templateArgs | Where-Object { $_ -like '-Djava.library.path=*' })
if ($nativeArgument.Count -ne 1) { throw 'Expected one staged native path in template.' }
$natives = $nativeArgument[0].Substring('-Djava.library.path='.Length)
function Assert-LocalArtifact([string]$Path) {
    $full = [IO.Path]::GetFullPath($Path)
    if (-not $full.StartsWith($repo + [IO.Path]::DirectorySeparatorChar, [StringComparison]::OrdinalIgnoreCase)) {
        throw "Artifact is not staged in this repository: $full"
    }
    if (-not (Test-Path -LiteralPath $full)) { throw "Missing staged artifact $full" }
}
foreach ($path in @($classpath.Split(';')) + @($assets, $natives, $AuraJar, $ProbeJar, $manifest.gameDirectory)) { Assert-LocalArtifact $path }
if ($classpath -match '(?i)(build[\\/]classes|build[\\/]resources|devlaunchinjector|minecraft-merged|named-minecraft)' -or
    $manifest.mainClass -ne 'net.fabricmc.loader.impl.launch.knot.KnotClient') { throw 'Not an official packaged Fabric client classpath.' }
function Mod-Id([string]$Path) {
    $archive = [IO.Compression.ZipFile]::OpenRead([IO.Path]::GetFullPath($Path))
    try {
        $entry = $archive.GetEntry('fabric.mod.json')
        if (-not $entry) { throw "Missing mod metadata: $Path" }
        $reader = [IO.StreamReader]::new($entry.Open())
        try { ($reader.ReadToEnd() | ConvertFrom-Json).id } finally { $reader.Dispose() }
    } finally { $archive.Dispose() }
}
if ((Mod-Id $AuraJar) -ne 'aura' -or (Mod-Id $ProbeJar) -ne 'aura_qa_multiplayer') { throw 'Wrong candidate/probe mod IDs.' }
if ($AuraJar -match '(?i)(-dev|-sources|-javadoc)\.jar$' -or $ProbeJar -match '(?i)(-dev|-sources|-javadoc)\.jar$') { throw 'Use remapped release JARs, never dev JARs.' }
$hash = (Get-FileHash -Algorithm SHA256 -LiteralPath $AuraJar).Hash.ToLowerInvariant()
$dependencies = @(Get-ChildItem -LiteralPath (Join-Path $manifest.gameDirectory 'mods') -Filter '*.jar' -File | Where-Object {
    (Mod-Id $_.FullName) -in @('fabric-api', 'patchouli', 'team_reborn_energy')
})
$dependencyIds = @($dependencies | ForEach-Object { Mod-Id $_.FullName })
if ($dependencyIds -notcontains 'fabric-api' -or $dependencyIds -notcontains 'patchouli') { throw 'Template mods must contain staged Fabric API and Patchouli.' }
$serverTemplate = Join-Path $repo 'build/qa-audit/packaged-server-1.21.11'
if ((Get-Content -LiteralPath (Join-Path $serverTemplate 'eula.txt') -Raw) -notmatch '(?m)^eula=true\s*$') {
    throw 'Requires the already accepted isolated server EULA; this runner does not accept a new EULA.'
}
$listener = [Net.Sockets.TcpListener]::new([Net.IPAddress]::Loopback, $Port)
try { $listener.Start() } finally { $listener.Stop() }
$root = Join-Path $repo ('build/qa-audit/multiplayer/' + (Get-Date -Format 'yyyyMMdd-HHmmss-fff'))
if (Test-Path -LiteralPath $root) { throw 'Refusing to reuse an existing fixture directory.' }
New-Item -ItemType Directory -Path $root | Out-Null
$serverDir = Join-Path $root 'server-game'
$ownerDir = Join-Path $root 'owner-game'
$witnessDir = Join-Path $root 'witness-game'
foreach ($dir in @($serverDir, $ownerDir, $witnessDir)) {
    New-Item -ItemType Directory -Path (Join-Path $dir 'mods') -Force | Out-Null
    foreach ($mod in @($AuraJar, $ProbeJar) + @($dependencies.FullName)) {
        Copy-Item -LiteralPath $mod -Destination (Join-Path $dir 'mods')
    }
}
foreach ($part in @('fabric-server-launch.jar', 'server.jar', 'libraries', 'versions', 'eula.txt')) {
    $source = Join-Path $serverTemplate $part
    if (Test-Path -LiteralPath $source) { Copy-Item -LiteralPath $source -Destination (Join-Path $serverDir $part) -Recurse }
}
if (-not (Test-Path -LiteralPath (Join-Path $serverDir 'fabric-server-launch.jar')) -or
    -not (Test-Path -LiteralPath (Join-Path $serverDir 'server.jar'))) { throw 'Isolated server installation is incomplete.' }
'serverJar=server.jar' | Set-Content -LiteralPath (Join-Path $serverDir 'fabric-server-launcher.properties')
@"
server-ip=127.0.0.1
server-port=$Port
online-mode=false
enforce-secure-profile=false
enable-rcon=false
enable-query=false
enable-status=false
max-players=2
view-distance=2
simulation-distance=2
spawn-protection=0
level-name=qa-world
level-type=minecraft:flat
level-seed=42
gamemode=survival
difficulty=peaceful
max-tick-time=60000
"@ | Set-Content -LiteralPath (Join-Path $serverDir 'server.properties')
foreach ($dir in @($ownerDir, $witnessDir)) {
    # Client simulation distance has a minimum of 5; the dedicated server controls simulation here.
    @('renderDistance:2', 'simulationDistance:5', 'maxFps:20', 'enableVsync:false', 'pauseOnLostFocus:false',
        'graphicsMode:0', 'particles:2', 'mipmapLevels:0', 'soundCategory_master:0.0', 'onboardAccessibility:false') |
        Set-Content -LiteralPath (Join-Path $dir 'options.txt')
}
$processes = [Collections.Generic.List[object]]::new()
$samples = [Collections.Generic.List[object]]::new()
$started = [DateTime]::UtcNow
$sequence = @{}
$success = $false
$failure = $null
$checks = [Collections.Generic.List[string]]::new()

function Offline-UUID([string]$Name) {
    $bytes = [Security.Cryptography.MD5]::HashData([Text.Encoding]::UTF8.GetBytes("OfflinePlayer:$Name"))
    $bytes[6] = ($bytes[6] -band 15) -bor 48
    $bytes[8] = ($bytes[8] -band 63) -bor 128
    $hex = [Convert]::ToHexString($bytes).ToLowerInvariant()
    '{0}-{1}-{2}-{3}-{4}' -f $hex.Substring(0,8), $hex.Substring(8,4), $hex.Substring(12,4), $hex.Substring(16,4), $hex.Substring(20,12)
}
function Launch([string]$Role, [string]$Game, [string[]]$Arguments) {
    $out = Join-Path $root $Role
    New-Item -ItemType Directory -Path $out | Out-Null
    $argsFile = Join-Path $out 'java.args'
    $all = @('-Xms128m', '-XX:+UseSerialGC', '-XX:ActiveProcessorCount=2', '-XX:MaxDirectMemorySize=128m', '-XX:ReservedCodeCacheSize=96m',
        "-Daura.qa.multiplayer.output=$out", "-Daura.qa.multiplayer.sha256=$hash") + $Arguments
    $escaped = $all | ForEach-Object { '"' + $_.Replace('\','\\').Replace('"','\"') + '"' }
    [IO.File]::WriteAllLines($argsFile, $escaped)
    $info = [Diagnostics.ProcessStartInfo]::new()
    $info.FileName = $java
    $info.WorkingDirectory = $Game
    $info.UseShellExecute = $false
    $info.CreateNoWindow = $true
    $info.RedirectStandardInput = $true
    $info.RedirectStandardOutput = $true
    $info.RedirectStandardError = $true
    $info.ArgumentList.Add("@$argsFile")
    $p = [Diagnostics.Process]::Start($info)
    $entry = [pscustomobject]@{ role=$Role; game=$Game; output=$out; process=$p; stdout=$p.StandardOutput.ReadToEndAsync(); stderr=$p.StandardError.ReadToEndAsync() }
    $processes.Add($entry)
    return $entry
}
function Launch-Client([string]$Role, [string]$Name, [string]$Game) {
    Launch $Role $Game @('-Xmx512m', "-Djava.library.path=$natives", '-cp', $classpath, $manifest.mainClass,
        '--username', $Name, '--uuid', (Offline-UUID $Name), '--accessToken', '0', '--userType', 'legacy',
        '--version', $version, '--gameDir', $Game, '--assetsDir', $assets, '--assetIndex', $assetIndex,
        '--width', '854', '--height', '480', '--quickPlayMultiplayer', "127.0.0.1:$Port",
        '--quickPlayPath', (Join-Path $root "$Role/quickplay.json"))
}
function Memory-Sample {
    $monitor = Get-Process -Id $PID
    [long]$working = $monitor.WorkingSet64
    [long]$private = $monitor.PrivateMemorySize64
    foreach ($entry in $processes) {
        $entry.process.Refresh()
        if (-not $entry.process.HasExited) { $working += $entry.process.WorkingSet64; $private += $entry.process.PrivateMemorySize64 }
    }
    $mb = [Math]::Max($working, $private) / 1MB
    $samples.Add([pscustomobject]@{utc=[DateTime]::UtcNow.ToString('o'); workingMiB=[Math]::Round($working/1MB,1); privateMiB=[Math]::Round($private/1MB,1)})
    if ($mb -ge 4000) {
        foreach ($entry in $processes) { if (-not $entry.process.HasExited) { $entry.process.Kill($true) } }
        throw 'Forced memory guard (4000 MiB)'
    }
    return $mb
}
function Read-LiveSnapshot([string]$Path) {
    # Java replaces this file while the runner samples it; allow rename on Windows.
    $stream = [IO.File]::Open($Path, [IO.FileMode]::Open, [IO.FileAccess]::Read,
        [IO.FileShare]::ReadWrite -bor [IO.FileShare]::Delete)
    try {
        $reader = [IO.StreamReader]::new($stream)
        try { return $reader.ReadToEnd() } finally { $reader.Dispose() }
    } finally { $stream.Dispose() }
}
function Read-Probe($Entry) {
    if (Test-Path -LiteralPath (Join-Path $Entry.output 'failure.json')) {
        throw (Get-Content -LiteralPath (Join-Path $Entry.output 'failure.json') -Raw)
    }
    $path = Join-Path $Entry.output 'latest.json'
    if (-not (Test-Path -LiteralPath $path)) { return $null }
    try { $report = Read-LiveSnapshot $path | ConvertFrom-Json } catch { return $null }
    if ($report.runtimeNamespace -ne 'intermediary' -or $report.auraSha256 -ne $hash) { throw 'Probe did not load the actual remapped candidate.' }
    if ($report.maxFairiesObserved -gt 1) { throw "Duplicate fairy count observed by $($Entry.role)" }
    return $report
}
function Wait-Phase([string]$Name, [scriptblock]$Condition, [int]$Stable = 1) {
    $deadline = [DateTime]::UtcNow.AddSeconds(150)
    $matches = 0
    while ([DateTime]::UtcNow -lt $deadline) {
        if ((Memory-Sample) -gt 3800) { throw 'Soft aggregate memory guard (3800 MiB)' }
        if (([DateTime]::UtcNow - $started).TotalSeconds -gt 600) { throw 'Overall deadline exceeded' }
        foreach ($trackedProcess in $processes) {
            if (Test-Path -LiteralPath (Join-Path $trackedProcess.output 'failure.json')) { throw "Probe failure in $($trackedProcess.role)" }
            if ($trackedProcess.process.HasExited -and -not (Test-Path -LiteralPath (Join-Path $trackedProcess.output 'stop.request'))) {
                throw "Unexpected process exit $($trackedProcess.role) ($($trackedProcess.process.ExitCode))"
            }
        }
        if (& $Condition) { $matches++ } else { $matches = 0 }
        if ($matches -ge $Stable) { $checks.Add($Name); return }
        Start-Sleep -Milliseconds 250
    }
    throw "Phase timed out: $Name"
}
function Command($Entry, [string]$Action) {
    $id = 1 + [int]$sequence[$Entry.role]
    $sequence[$Entry.role] = $id
    $temp = Join-Path $Entry.output 'command.tmp'
    @{sequence=$id; action=$Action} | ConvertTo-Json | Set-Content -LiteralPath $temp
    Move-Item -LiteralPath $temp -Destination (Join-Path $Entry.output 'command.json') -Force
}
function Player-Report($Report, [string]$Name) { @($Report.players | Where-Object { $_.name -eq $Name }) | Select-Object -First 1 }
function Equipped($Player, [int]$Bound) {
    return $null -ne $Player -and $Player.accessoryAttached -and $Player.slots[1].item -eq 'aura:ring_of_binding' -and
        $Player.slots[1].count -eq 1 -and $Player.slots[1].bound -eq $Bound -and $Player.ringsInInventoryAndEquipment -eq 1
}
function Bound-State($OwnerEntry) {
    $s = Read-Probe $server
    $o = Read-Probe $OwnerEntry
    $w = Read-Probe $witness
    if (-not $s -or -not $o -or -not $w) { return $false }
    $sp = Player-Report $s 'AuraOwnerQA'
    $op = Player-Report $o 'AuraOwnerQA'
    $remote = Player-Report $w 'AuraOwnerQA'
    if ($remote.accessoryAttached) { throw 'Private owner accessory attachment was synced to the witness' }
    if (-not (Equipped $sp 1) -or -not (Equipped $op 1) -or -not $remote -or
        $sp.charmsInInventory -ne 0 -or $op.charmsInInventory -ne 0) { return $false }
    if ($sp.uuid -ne (Offline-UUID 'AuraOwnerQA') -or $op.uuid -ne $sp.uuid) { throw 'Owner identity mismatch' }
    foreach ($r in @($s, $o, $w)) {
        if (@($r.fairies).Count -ne 1 -or $r.fairies[0].owner -ne $sp.uuid -or $r.fairies[0].slot -ne 0) { return $false }
        if ($r.fairies[0].uuid -ne $s.fairies[0].uuid) { return $false }
    }
    return $o.worldLoaded -and $w.worldLoaded -and -not $o.singleplayer -and -not $w.singleplayer
}
function Capture($Entry, [string]$Label) {
    Command $Entry 'capture'
    Wait-Phase $Label {
        $r = Read-Probe $Entry
        $r -and $r.acknowledged -eq $sequence[$Entry.role] -and $r.screenshot -and
            (Test-Path -LiteralPath (Join-Path $Entry.output $r.screenshot))
    }
}
function Save-Phase([string]$Name, $OwnerEntry) {
    foreach ($entry in @($server, $OwnerEntry, $witness)) {
        Read-LiveSnapshot (Join-Path $entry.output 'latest.json') |
            Set-Content -LiteralPath (Join-Path $root "$Name-$($entry.role).json")
    }
}
try {
    $lifetimeReport = Join-Path $root 'consumer-lifetime-probe.json'
    $server = Launch 'server' $serverDir @('-Xmx512m',
        '-Daura.qa.consumerLifetimeProbe=true',
        "-Daura.qa.consumerLifetimeProbe.output=$lifetimeReport",
        "-Daura.qa.consumerLifetimeProbe.sha256=$hash",
        '-jar', (Join-Path $serverDir 'fabric-server-launch.jar'), 'nogui')
    Wait-Phase 'dedicated-server-ready' { $null -ne (Read-Probe $server) }
    if (-not (Test-Path -LiteralPath $lifetimeReport -PathType Leaf)) { throw 'Missing transformed item-lifetime probe report' }
    $lifetime = Get-Content -LiteralPath $lifetimeReport -Raw | ConvertFrom-Json
    if (-not $lifetime.success -or -not $lifetime.itemEntityTransformed -or
        $lifetime.runtimeNamespace -ne 'intermediary' -or $lifetime.auraSha256 -ne $hash -or
        $lifetime.ageAndPersistence -ne 'PASS' -or $lifetime.merge -ne 'PASS' -or
        $lifetime.oldMergeEligibility -ne 'PASS') { throw 'Transformed item-lifetime runtime checks failed' }
    $checks.Add('transformed-item-lifetime-persistence-merge-and-eligibility')
    $owner = Launch-Client 'owner-initial' 'AuraOwnerQA' $ownerDir
    Wait-Phase 'owner-joined' { (Read-Probe $owner).worldLoaded }
    $witness = Launch-Client 'witness' 'AuraWitnessQA' $witnessDir
    Wait-Phase 'two-distinct-players-joined' { @((Read-Probe $server).players).Count -eq 2 -and (Read-Probe $witness).worldLoaded }
    # Explicit stdin fixtures only; probe code never grants items or edits equipment.
    $fixture = @(
        @{label='quiet-world'; command='gamerule minecraft:spawn_mobs false'},
        @{label='fixed-day'; command='gamerule minecraft:advance_time false'},
        @{label='daylight'; command='time set day'},
        @{label='clear-weather'; command='weather clear'},
        @{label='survival-authority'; command='gamemode survival @a'},
        @{label='small-floor'; command='fill -8 63 -8 8 63 8 minecraft:smooth_stone'},
        @{label='owner-position'; command='tp AuraOwnerQA 0.5 64 0.5 0 10'},
        @{label='witness-position'; command='tp AuraWitnessQA 0.5 64 4.5 180 10'},
        @{label='one-real-ring'; command='item replace entity AuraOwnerQA hotbar.0 with aura:ring_of_binding 1'},
        @{label='one-real-charm'; command='item replace entity AuraOwnerQA hotbar.1 with aura:fairy_charm 1'}
    )
    $fixture | ConvertTo-Json | Set-Content -LiteralPath (Join-Path $root 'stdin-fixture.json')
    foreach ($line in $fixture) { $server.process.StandardInput.WriteLine($line.command) }
    $server.process.StandardInput.Flush()
    Wait-Phase 'seed-items-arrived-before-equip' {
        $p = Player-Report (Read-Probe $owner) 'AuraOwnerQA'
        $p -and -not $p.accessoryAttached -and $p.ringsInInventoryAndEquipment -eq 1 -and $p.charmsInInventory -eq 1
    }
    Command $owner 'open'
    Wait-Phase 'real-accessory-menu-opened' { (Read-Probe $owner).accessoryMenu }
    Command $owner 'equip'
    Wait-Phase 'real-physical-ring-slot-and-owner-attachment-synced' {
        (Equipped (Player-Report (Read-Probe $server) 'AuraOwnerQA') 0) -and
            (Equipped (Player-Report (Read-Probe $owner) 'AuraOwnerQA') 0)
    }
    Command $owner 'bind'
    Wait-Phase 'bound-fairy-same-uuid-server-and-both-clients-private-owner-attachment' { Bound-State $owner } 24
    Capture $owner 'initial-owner-screenshot'
    Capture $witness 'initial-witness-screenshot'
    Save-Phase 'initial-bound' $owner
    'normal owner logout' | Set-Content -LiteralPath (Join-Path $owner.output 'stop.request')
    Wait-Phase 'owner-exited-normally' { $owner.process.HasExited }
    if ($owner.process.ExitCode -ne 0) { throw 'Owner did not stop normally' }
    Wait-Phase 'owner-logout-removes-fairy-server-and-witness' {
        $s = Read-Probe $server
        $w = Read-Probe $witness
        $s -and $w -and -not (Player-Report $s 'AuraOwnerQA') -and -not (Player-Report $w 'AuraOwnerQA') -and
            @($s.fairies).Count -eq 0 -and @($w.fairies).Count -eq 0
    } 12
    Save-Phase 'owner-absent' $owner
    $rejoined = Launch-Client 'owner-rejoin' 'AuraOwnerQA' $ownerDir
    # No open/equip/use command on rejoin: persistence and target-only sync must work independently of a menu.
    Wait-Phase 'owner-rejoin-restores-one-bound-ring-and-one-fairy-without-menu' { Bound-State $rejoined } 24
    Capture $rejoined 'rejoined-owner-screenshot'
    Capture $witness 'rejoined-witness-screenshot'
    Save-Phase 'rejoined-bound' $rejoined
    $success = $true
} catch {
    $failure = $_.ToString()
} finally {
    foreach ($entry in $processes) { 'runner shutdown' | Set-Content -LiteralPath (Join-Path $entry.output 'stop.request') }
    if ($server -and -not $server.process.HasExited) {
        try { $server.process.StandardInput.WriteLine('stop'); $server.process.StandardInput.Flush() } catch { }
    }
    $stopDeadline = [DateTime]::UtcNow.AddSeconds(25)
    while (@($processes | Where-Object { -not $_.process.HasExited }).Count -gt 0 -and [DateTime]::UtcNow -lt $stopDeadline) {
        try { [void](Memory-Sample) } catch { $failure = $_.ToString(); $success = $false; break }
        Start-Sleep -Milliseconds 250
    }
    foreach ($entry in $processes) {
        if (-not $entry.process.HasExited) { $entry.process.Kill($true); $failure = 'Forced shutdown after normal-stop deadline'; $success = $false }
        $entry.process.WaitForExit()
        if ($entry.process.ExitCode -ne 0) { $success = $false; if (-not $failure) { $failure = "Nonzero exit: $($entry.role)" } }
        if (Test-Path -LiteralPath (Join-Path $entry.output 'failure.json')) {
            $success = $false
            if (-not $failure) { $failure = "Probe failure recorded by $($entry.role)" }
        }
        [IO.File]::WriteAllText((Join-Path $entry.output 'stdout.log'), $entry.stdout.GetAwaiter().GetResult())
        [IO.File]::WriteAllText((Join-Path $entry.output 'stderr.log'), $entry.stderr.GetAwaiter().GetResult())
    }
    $samples | Export-Csv -LiteralPath (Join-Path $root 'memory.csv') -NoTypeInformation
    [ordered]@{
        success=$success; failure=$failure; checks=$checks.ToArray(); auraSha256=$hash
        probeSha256=(Get-FileHash -Algorithm SHA256 -LiteralPath $ProbeJar).Hash.ToLowerInvariant()
        endpoint="127.0.0.1:$Port"; template=$ClientLaunchManifest
        startedUtc=$started.ToString('o'); finishedUtc=[DateTime]::UtcNow.ToString('o')
        peakWorkingMiB=($samples | Measure-Object workingMiB -Maximum).Maximum
        peakPrivateMiB=($samples | Measure-Object privateMiB -Maximum).Maximum
        processes=@($processes | ForEach-Object { @{role=$_.role; pid=$_.process.Id; exit=$_.process.ExitCode} })
        scope='Source-defined private fixture; result covers observed state/UUIDs, not human screenshot review. 250ms memory sampling is not an OS hard cap.'
    } | ConvertTo-Json -Depth 6 | Set-Content -LiteralPath (Join-Path $root 'summary.json')
}
Write-Output "Multiplayer fixture artifacts: $root"
if (-not $success) { throw "Multiplayer fixture FAILED: $failure" }
Write-Output 'State/identity checks passed. Review both clients PNGs before claiming visual acceptance.'
