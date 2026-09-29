param(
    [Parameter(Mandatory)][string]$JavaHome,
    [Parameter(Mandatory)][string]$ClientLaunchManifest,
    [Parameter(Mandatory)][string]$AuraJar,
    [Parameter(Mandatory)][string]$ProbeJar,
    [Parameter(Mandatory)][string]$ServerTemplate,
    [ValidateRange(20000, 60000)][int]$Port = 25576,
    [switch]$Execute
)

$ErrorActionPreference = 'Stop'
$repo = [IO.Path]::GetFullPath((Join-Path $PSScriptRoot '../../..'))
if (-not $Execute) {
    Write-Output 'Preview only. Requires a native NeoForge probe JAR, installed server and packaged client launch manifest + sibling client.java.args.'
    Write-Output "Execution will create fresh game directories under build/qa-audit/multiplayer, bind 127.0.0.1:$Port, and run AuraOwnerQA + AuraWitnessQA offline."
    Write-Output 'Reserve three Minecraft slots with Mod Playtesting first; at most one other game JVM may run. Aggregate owned soft guard 3800 MiB; forced guard 4000 MiB.'
    Write-Output 'Memory-tuned profile: 512 MiB heap per JVM, 2 visible processors, client render distance 2; target measured aggregate below 3500 MiB (not guaranteed).'
    return
}
if ($PSVersionTable.PSVersion.Major -lt 7) { throw 'Use PowerShell 7.' }
function Game-Processes {
    @(Get-CimInstance Win32_Process | Where-Object {
        $_.Name -match '^javaw?\.exe$' -and $_.CommandLine -notmatch 'org\.gradle\.|GradleDaemon|GradleWorkerMain|JUnitStarter'
    })
}
if (@(Game-Processes).Count -gt 1) { throw 'Three reserved slots are unavailable: more than one other potential game JVM is active.' }
Add-Type -AssemblyName System.IO.Compression.FileSystem
$java = (Resolve-Path -LiteralPath (Join-Path $JavaHome 'bin/java.exe')).Path
$manifest = Get-Content -LiteralPath $ClientLaunchManifest -Raw | ConvertFrom-Json
if ($manifest.minecraft -ne '1.21.1' -or $manifest.neoForge -ne '21.1.252' -or $manifest.offlineIdentity.externalCredentials -ne $false) {
    throw 'Template must be an existing offline packaged 1.21.1 launch manifest.'
}
$templateArgs = @(Get-Content -LiteralPath (Join-Path (Split-Path -Parent $ClientLaunchManifest) 'client.java.args') | ForEach-Object { ConvertFrom-Json -InputObject $_ })
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
foreach ($path in @($classpath.Split(';')) + @($assets, $natives, $AuraJar, $ProbeJar, $manifest.workingDirectory)) { Assert-LocalArtifact $path }
if ($classpath -match '(?i)(build[\\/]classes|build[\\/]resources|devlaunchinjector|minecraft-merged|named-minecraft)' -or
    $manifest.mainClass -ne 'cpw.mods.bootstraplauncher.BootstrapLauncher') { throw 'Not an official packaged NeoForge client classpath.' }
function Mod-Id([string]$Path) {
    $archive = [IO.Compression.ZipFile]::OpenRead([IO.Path]::GetFullPath($Path))
    try {
        $entry = $archive.GetEntry('META-INF/neoforge.mods.toml')
        if (-not $entry) { throw "Missing mod metadata: $Path" }
        $reader = [IO.StreamReader]::new($entry.Open())
        try {
            $metadata = $reader.ReadToEnd()
            if ($metadata -notmatch '(?m)^\s*modId\s*=\s*"([^"]+)"') { throw 'Missing NeoForge mod ID' }
            $Matches[1]
        } finally { $reader.Dispose() }
    } finally { $archive.Dispose() }
}
if ((Mod-Id $AuraJar) -ne 'aura' -or (Mod-Id $ProbeJar) -ne 'aura_qa_multiplayer') { throw 'Wrong candidate/probe mod IDs.' }
if ($AuraJar -match '(?i)(-dev|-sources|-javadoc)\.jar$' -or $ProbeJar -match '(?i)(-dev|-sources|-javadoc)\.jar$') { throw 'Use remapped release JARs, never dev JARs.' }
$hash = (Get-FileHash -Algorithm SHA256 -LiteralPath $AuraJar).Hash.ToLowerInvariant()
$probeHash = (Get-FileHash -Algorithm SHA256 -LiteralPath $ProbeJar).Hash.ToLowerInvariant()
$dependencies = @(Get-ChildItem -LiteralPath (Join-Path $manifest.workingDirectory 'mods') -Filter '*.jar' -File | Where-Object {
    (Mod-Id $_.FullName) -eq 'patchouli'
})
$dependencyIds = @($dependencies | ForEach-Object { Mod-Id $_.FullName })
if ($dependencyIds -notcontains 'patchouli') { throw 'Template mods must contain NeoForge Patchouli.' }
$serverTemplate = (Resolve-Path -LiteralPath $ServerTemplate).Path
if ((Get-Content -LiteralPath (Join-Path $serverTemplate 'eula.txt') -Raw) -notmatch '(?m)^eula=true\s*$') {
    throw 'Requires the already accepted isolated server EULA; this runner does not accept a new EULA.'
}
$listener = [Net.Sockets.TcpListener]::new([Net.IPAddress]::Loopback, $Port)
try { $listener.Start() } finally { $listener.Stop() }
$root = Join-Path $repo ('build/qa-audit/neoforge-multiplayer/' + (Get-Date -Format 'yyyyMMdd-HHmmss-fff'))
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
foreach ($part in @('libraries', 'eula.txt')) {
    $source = Join-Path $serverTemplate $part
    if (Test-Path -LiteralPath $source) { Copy-Item -LiteralPath $source -Destination (Join-Path $serverDir $part) -Recurse }
}
$serverArgs = Join-Path $serverDir 'libraries/net/neoforged/neoforge/21.1.252/win_args.txt'
if (-not (Test-Path -LiteralPath $serverArgs)) { throw 'Isolated NeoForge server installation is incomplete.' }
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
generator-settings={"layers":[{"block":"minecraft:bedrock","height":1},{"block":"minecraft:dirt","height":2},{"block":"minecraft:grass_block","height":1}],"biome":"minecraft:plains"}
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
    $activeGames = @(Game-Processes).Count
    if ($activeGames + 1 -gt 4) { throw "Global four-Minecraft-session cap reached ($activeGames active); no launch performed." }
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
    if ($Role -eq 'server') {
        # Java does not expand an @argument-file nested inside another one.
        foreach ($argument in $all) { $info.ArgumentList.Add($argument) }
    } else {
        $info.ArgumentList.Add("@$argsFile")
    }
    $p = [Diagnostics.Process]::Start($info)
    $entry = [pscustomobject]@{ role=$Role; game=$Game; output=$out; process=$p; stdout=$p.StandardOutput.ReadToEndAsync(); stderr=$p.StandardError.ReadToEndAsync() }
    $processes.Add($entry)
    return $entry
}
function Launch-Client([string]$Role, [string]$Name, [string]$Game) {
    $arguments = [Collections.Generic.List[string]]::new()
    $arguments.Add('-Xmx512m')
    $replace = @{ '--username'=$Name; '--uuid'=(Offline-UUID $Name); '--gameDir'=$Game; '--width'='854'; '--height'='480' }
    for ($i=0; $i -lt $templateArgs.Count; $i++) {
        $arg = [string]$templateArgs[$i]
        if ($arg -match '^-Xm[sx]|^-XX:|^-Daura\.qa\.') { continue }
        if ($arg -in @('--quickPlaySingleplayer','--quickPlayMultiplayer','--quickPlayPath')) { $i++; continue }
        $arguments.Add($arg)
        if ($replace.ContainsKey($arg)) { $arguments.Add([string]$replace[$arg]); $i++ }
    }
    foreach($arg in @('--quickPlayMultiplayer',"127.0.0.1:$Port",'--quickPlayPath',(Join-Path $root "$Role/quickplay.json"))) { $arguments.Add($arg) }
    Launch $Role $Game $arguments.ToArray()
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
function Read-Probe($Entry) {
    if (Test-Path -LiteralPath (Join-Path $Entry.output 'failure.json')) {
        throw (Get-Content -LiteralPath (Join-Path $Entry.output 'failure.json') -Raw)
    }
    $path = Join-Path $Entry.output 'latest.json'
    if (-not (Test-Path -LiteralPath $path)) { return $null }
    $stream = $null
    $reader = $null
    try {
        $stream = [IO.FileStream]::new($path, [IO.FileMode]::Open, [IO.FileAccess]::Read, [IO.FileShare]::ReadWrite -bor [IO.FileShare]::Delete)
        $reader = [IO.StreamReader]::new($stream)
        $report = $reader.ReadToEnd() | ConvertFrom-Json
    } catch { return $null } finally {
        if ($reader) { $reader.Dispose() } elseif ($stream) { $stream.Dispose() }
    }
    if ($report.runtimeNamespace -ne 'neoforge' -or $report.auraSha256 -ne $hash -or
        $report.auraJarProof -ne 'verified') { throw 'Probe did not load the actual packaged native candidate.' }
    if ($Entry.role -eq 'server' -and $null -eq $report.ringsDropped) { throw 'Server probe lacks dropped-ring conservation evidence; rebuild the helper JAR.' }
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
function Command($Entry, [string]$Action, [hashtable]$Fields = @{}) {
    $id = 1 + [int]$sequence[$Entry.role]
    $sequence[$Entry.role] = $id
    $temp = Join-Path $Entry.output 'command.tmp'
    $payload = @{sequence=$id; action=$Action}
    foreach ($key in $Fields.Keys) {
        if ($payload.ContainsKey($key)) { throw "Reserved command field $key" }
        $payload[$key] = $Fields[$key]
    }
    $payload | ConvertTo-Json | Set-Content -LiteralPath $temp
    Move-Item -LiteralPath $temp -Destination (Join-Path $Entry.output 'command.json') -Force
}
function Fixture([string]$Label, [string]$Line) {
    [ordered]@{utc=[DateTime]::UtcNow.ToString('o'); label=$Label; command=$Line} |
        ConvertTo-Json -Compress | Add-Content -LiteralPath (Join-Path $root 'stdin-fixture.jsonl')
    $server.process.StandardInput.WriteLine($Line)
    $server.process.StandardInput.Flush()
}
function Player-Report($Report, [string]$Name) { @($Report.players | Where-Object { $_.name -eq $Name }) | Select-Object -First 1 }
function Inventory-Rings($Player, [int]$Bound) {
    @($Player.inventory | Where-Object {
        $_.item -eq 'aura:ring_of_binding' -and $_.count -eq 1 -and $_.bound -eq $Bound
    }).Count -eq 1
}
function Conserved-Ring($ServerReport, [int]$PlayerRings, [int]$DroppedRings) {
    $ownerState = Player-Report $ServerReport 'AuraOwnerQA'
    $ownerState -and $ownerState.ringsInInventoryAndEquipment -eq $PlayerRings -and
        $ServerReport.ringsDropped -eq $DroppedRings -and ($PlayerRings + $DroppedRings) -eq 1
}
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
        $report = $null
        for ($attempt = 0; $attempt -lt 20 -and -not $report; $attempt++) {
            $report = Read-Probe $entry
            if (-not $report) { Start-Sleep -Milliseconds 100 }
        }
        if (-not $report) { throw "Could not snapshot completed phase $Name from $($entry.role)." }
        $report | ConvertTo-Json -Depth 30 | Set-Content -LiteralPath (Join-Path $root "$Name-$($entry.role).json")
    }
}
try {
    $server = Launch 'server' $serverDir @('-Xmx512m', "@$serverArgs", 'nogui')
    Wait-Phase 'dedicated-server-ready' { $null -ne (Read-Probe $server) }
    $owner = Launch-Client 'owner-initial' 'AuraOwnerQA' $ownerDir
    Wait-Phase 'owner-joined' { (Read-Probe $owner).worldLoaded }
    $witness = Launch-Client 'witness' 'AuraWitnessQA' $witnessDir
    Wait-Phase 'two-distinct-players-joined' { @((Read-Probe $server).players).Count -eq 2 -and (Read-Probe $witness).worldLoaded }
    # Explicit stdin fixtures only; probe code never grants items or edits equipment.
    $fixture = @(
        @{label='quiet-world'; command='gamerule doMobSpawning false'},
        @{label='fixed-day'; command='gamerule doDaylightCycle false'},
        @{label='daylight'; command='time set day'},
        @{label='clear-weather'; command='weather clear'},
        @{label='survival-authority'; command='gamemode survival @a'},
        @{label='small-floor'; command='fill -8 63 -8 8 63 8 minecraft:smooth_stone'},
        @{label='owner-position'; command='tp AuraOwnerQA 0.5 64 0.5 0 10'},
        @{label='owner-respawn-position'; command='spawnpoint AuraOwnerQA 0 64 0'},
        @{label='witness-position'; command='tp AuraWitnessQA 0.5 64 4.5 180 10'},
        @{label='one-real-ring'; command='item replace entity AuraOwnerQA hotbar.0 with aura:ring_of_binding 1'},
        @{label='one-real-charm'; command='item replace entity AuraOwnerQA hotbar.1 with aura:fairy_charm 1'}
    )
    $fixture | ConvertTo-Json | Set-Content -LiteralPath (Join-Path $root 'stdin-fixture.json')
    foreach ($line in $fixture) { Fixture $line.label $line.command }
    Wait-Phase 'seed-items-arrived-before-equip' {
        $s = Read-Probe $server
        $o = Read-Probe $owner
        $w = Read-Probe $witness
        $sp = Player-Report $s 'AuraOwnerQA'
        $op = Player-Report $o 'AuraOwnerQA'
        $remote = Player-Report $w 'AuraOwnerQA'
        if ($remote -and $remote.accessoryAttached) { throw 'Private owner accessory attachment was synced to the witness before equip' }
        $sp -and $op -and $remote -and (Conserved-Ring $s 1 0) -and
            $sp.charmsInInventory -eq 1 -and $op.charmsInInventory -eq 1 -and
            $sp.slots[1].count -eq 0 -and $op.slots[1].count -eq 0
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

    Command $rejoined 'open'
    Wait-Phase 'reequipment-menu-opened' { (Read-Probe $rejoined).accessoryMenu }
    Command $rejoined 'unequip'
    Wait-Phase 'real-unequip-preserves-bound-ring-and-removes-fairy' {
        $s = Read-Probe $server
        $o = Read-Probe $rejoined
        $w = Read-Probe $witness
        $sp = Player-Report $s 'AuraOwnerQA'
        $op = Player-Report $o 'AuraOwnerQA'
        $remote = Player-Report $w 'AuraOwnerQA'
        if ($remote -and $remote.accessoryAttached) { throw 'Private owner attachment appeared on witness after unequip' }
        $sp -and $op -and $remote -and (Conserved-Ring $s 1 0) -and
            $sp.slots[1].count -eq 0 -and $op.slots[1].count -eq 0 -and
            (Inventory-Rings $sp 1) -and (Inventory-Rings $op 1) -and
            @($s.fairies).Count -eq 0 -and @($o.fairies).Count -eq 0 -and @($w.fairies).Count -eq 0
    } 12
    Save-Phase 'unequipped' $rejoined
    Command $rejoined 'reequip'
    Wait-Phase 'real-reequip-restores-one-bound-fairy' { (Conserved-Ring (Read-Probe $server) 1 0) -and (Bound-State $rejoined) } 24
    Save-Phase 'reequipped' $rejoined

    $beforeNether = (Read-Probe $server).tick
    Fixture 'nether-forceload-precondition' 'execute in minecraft:the_nether run forceload add 0 0'
    Fixture 'nether-clear-precondition' 'execute in minecraft:the_nether run fill -3 64 -3 3 67 3 minecraft:air'
    Fixture 'nether-floor-precondition' 'execute in minecraft:the_nether run fill -3 63 -3 3 63 3 minecraft:smooth_stone'
    Wait-Phase 'nether-stage-ticked' { (Read-Probe $server).tick -ge ($beforeNether + 20) }
    Fixture 'owner-native-dimension-outbound' 'execute as AuraOwnerQA in minecraft:the_nether run tp @s 0.5 64 0.5'
    Wait-Phase 'dimension-outbound-retains-ring-and-one-local-fairy' {
        $s = Read-Probe $server
        $o = Read-Probe $rejoined
        $w = Read-Probe $witness
        $sp = Player-Report $s 'AuraOwnerQA'
        $op = Player-Report $o 'AuraOwnerQA'
        $remote = Player-Report $w 'AuraOwnerQA'
        $sp -and $op -and $sp.dimension -eq 'minecraft:the_nether' -and
            $op.dimension -eq 'minecraft:the_nether' -and
            (Equipped $sp 1) -and (Equipped $op 1) -and
            (Conserved-Ring $s 1 0) -and -not $remote -and
            @($s.fairies).Count -eq 1 -and @($o.fairies).Count -eq 1 -and @($w.fairies).Count -eq 0 -and
            $s.fairies[0].owner -eq $sp.uuid -and $s.fairies[0].uuid -eq $o.fairies[0].uuid
    } 12
    Save-Phase 'nether' $rejoined
    Fixture 'owner-native-dimension-return' 'execute as AuraOwnerQA in minecraft:overworld run tp @s 0.5 64 0.5'
    Wait-Phase 'dimension-return-restores-private-bound-state' {
        $s = Read-Probe $server
        $o = Read-Probe $rejoined
        (Player-Report $s 'AuraOwnerQA').dimension -eq 'minecraft:overworld' -and
            (Player-Report $o 'AuraOwnerQA').dimension -eq 'minecraft:overworld' -and
            (Bound-State $rejoined)
    } 24
    Save-Phase 'dimension-return' $rejoined

    Fixture 'keep-inventory-true-control' 'gamerule keepInventory true'
    Wait-Phase 'keep-inventory-true-observed' { (Player-Report (Read-Probe $server) 'AuraOwnerQA').keepInventory -eq $true }
    Fixture 'owner-death-keep-inventory-true' 'kill AuraOwnerQA'
    Wait-Phase 'keep-inventory-true-death-conserves-equipped-ring' {
        $s = Read-Probe $server
        $o = Read-Probe $rejoined
        $sp = Player-Report $s 'AuraOwnerQA'
        $op = Player-Report $o 'AuraOwnerQA'
        $sp -and $op -and -not $sp.alive -and -not $op.alive -and
            $sp.keepInventory -eq $true -and (Conserved-Ring $s 1 0) -and
            $sp.slots[1].bound -eq 1 -and @($s.fairies).Count -eq 0
    }
    Command $rejoined 'respawn'
    Wait-Phase 'keep-inventory-true-respawn-restores-private-bound-state' {
        $s = Read-Probe $server
        (Player-Report $s 'AuraOwnerQA').keepInventory -eq $true -and
            (Conserved-Ring $s 1 0) -and (Bound-State $rejoined)
    } 24
    Save-Phase 'respawn-keep-inventory-true' $rejoined

    Fixture 'keep-inventory-false-control' 'gamerule keepInventory false'
    Wait-Phase 'keep-inventory-false-observed' { (Player-Report (Read-Probe $server) 'AuraOwnerQA').keepInventory -eq $false }
    Fixture 'owner-death-keep-inventory-false' 'kill AuraOwnerQA'
    Wait-Phase 'keep-inventory-false-death-drops-exact-bound-ring' {
        $s = Read-Probe $server
        $o = Read-Probe $rejoined
        $sp = Player-Report $s 'AuraOwnerQA'
        $op = Player-Report $o 'AuraOwnerQA'
        $drops = @($s.droppedItems | Where-Object { $_.item -eq 'aura:ring_of_binding' })
        $sp -and $op -and -not $sp.alive -and -not $op.alive -and
            $sp.keepInventory -eq $false -and (Conserved-Ring $s 0 1) -and
            $sp.slots[1].count -eq 0 -and $drops.Count -eq 1 -and
            $drops[0].count -eq 1 -and $drops[0].bound -eq 1 -and
            @($s.fairies).Count -eq 0
    }
    Save-Phase 'death-keep-inventory-false' $rejoined
    Command $rejoined 'respawn'
    Wait-Phase 'keep-inventory-false-respawn-clears-equipment-and-fairy' {
        $s = Read-Probe $server
        $o = Read-Probe $rejoined
        $w = Read-Probe $witness
        $sp = Player-Report $s 'AuraOwnerQA'
        $op = Player-Report $o 'AuraOwnerQA'
        $remote = Player-Report $w 'AuraOwnerQA'
        if ($remote -and $remote.accessoryAttached) { throw 'Private owner attachment appeared on witness after respawn' }
        $sp -and $op -and $sp.alive -and $op.alive -and
            $remote -and
            $sp.slots[1].count -eq 0 -and $op.slots[1].count -eq 0 -and
            ($sp.ringsInInventoryAndEquipment + $s.ringsDropped) -eq 1 -and
            @($s.fairies).Count -eq 0 -and @($o.fairies).Count -eq 0
    }
    Fixture 'owner-pickup-position-only' 'tp AuraOwnerQA 0.5 64 0.5'
    Wait-Phase 'natural-pickup-conserves-bound-ring-without-drop' {
        $s = Read-Probe $server
        $o = Read-Probe $rejoined
        $sp = Player-Report $s 'AuraOwnerQA'
        $op = Player-Report $o 'AuraOwnerQA'
        $sp -and $op -and (Conserved-Ring $s 1 0) -and
            $sp.slots[1].count -eq 0 -and $op.slots[1].count -eq 0 -and
            (Inventory-Rings $sp 1) -and (Inventory-Rings $op 1)
    }
    Save-Phase 'recovered-drop' $rejoined
    Command $rejoined 'open'
    Wait-Phase 'post-death-accessory-menu-opened' { (Read-Probe $rejoined).accessoryMenu }
    Command $rejoined 'reequip'
    Wait-Phase 'post-death-real-reequip-restores-private-fairy' { (Bound-State $rejoined) -and (Conserved-Ring (Read-Probe $server) 1 0) } 24
    Save-Phase 'post-death-reequipped' $rejoined

    $beforeBookshelf = (Read-Probe $server).tick
    Fixture 'unpowered-bookshelf-menu-precondition' 'setblock 2 64 0 aura:bookshelf_coordinator'
    Wait-Phase 'bookshelf-placement-ticked' { (Read-Probe $server).tick -ge ($beforeBookshelf + 20) }
    Command $rejoined 'bookshelfOpen' @{x=2; y=64; z=0}
    Wait-Phase 'real-bookshelf-menu-opened-unpowered' {
        $s = Read-Probe $server
        $o = Read-Probe $rejoined
        $sp = Player-Report $s 'AuraOwnerQA'
        $op = Player-Report $o 'AuraOwnerQA'
        $o.bookshelfMenu -and $sp.bookshelf -and $op.bookshelf -and
            $sp.bookshelf.containerId -eq $op.bookshelf.containerId -and
            $sp.bookshelf.revision -gt 0 -and $op.bookshelf.revision -gt 0 -and
            $sp.bookshelf.connectedShelves -eq 0 -and $op.bookshelf.connectedShelves -eq 0 -and
            $sp.bookshelf.storageShelves -eq $op.bookshelf.storageShelves -and
            $sp.bookshelf.requiredPower -eq $op.bookshelf.requiredPower -and
            $sp.bookshelf.availablePower -eq $op.bookshelf.availablePower -and
            $sp.bookshelf.networkComplete -eq $op.bookshelf.networkComplete -and
            @($sp.bookshelf.entries).Count -eq 0 -and @($op.bookshelf.entries).Count -eq 0
    }
    Save-Phase 'bookshelf-menu-unpowered' $rejoined
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
        probeSha256=$probeHash
        endpoint="127.0.0.1:$Port"; template=$ClientLaunchManifest
        startedUtc=$started.ToString('o'); finishedUtc=[DateTime]::UtcNow.ToString('o')
        peakWorkingMiB=($samples | Measure-Object workingMiB -Maximum).Maximum
        peakPrivateMiB=($samples | Measure-Object privateMiB -Maximum).Maximum
        processes=@($processes | ForEach-Object { @{role=$_.role; pid=$_.process.Id; exit=$_.process.ExitCode} })
        bookshelfPacket='not_sent_unpowered_network_has_no_valid_entry'
        scope='Source-defined private fixture; result covers observed state/UUIDs, not human screenshot review. 250ms memory sampling is not an OS hard cap.'
    } | ConvertTo-Json -Depth 6 | Set-Content -LiteralPath (Join-Path $root 'summary.json')
}
Write-Output "Multiplayer fixture artifacts: $root"
if (-not $success) { throw "Multiplayer fixture FAILED: $failure" }
Write-Output 'State/identity checks passed. Review both clients PNGs before claiming visual acceptance.'
