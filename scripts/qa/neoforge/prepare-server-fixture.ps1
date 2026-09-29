param(
    [Parameter(Mandatory)][string]$ServerTemplate,
    [Parameter(Mandatory)][string]$AuraJar,
    [string[]]$HelperJars = @(),
    [Parameter(Mandatory)][ValidatePattern('^[a-zA-Z0-9_-]+$')][string]$Label,
    [ValidateRange(20000, 60000)][int]$Port = 25642
)

$ErrorActionPreference = 'Stop'
$repo = [IO.Path]::GetFullPath((Join-Path $PSScriptRoot '../../..'))
$template = (Resolve-Path -LiteralPath $ServerTemplate).Path
if ((Get-Content -LiteralPath (Join-Path $template 'eula.txt') -Raw) -notmatch '(?m)^eula=true\s*$') {
    throw 'Requires an isolated template with the already accepted EULA.'
}
$root = Join-Path $repo "build/qa-audit/$Label"
if (Test-Path -LiteralPath $root) { throw 'Use a fresh fixture label; existing worlds are never overwritten.' }
$patchouli = @(Get-ChildItem -LiteralPath (Join-Path $template 'mods') -Filter 'Patchouli-*-NEOFORGE.jar' -File)
if ($patchouli.Count -ne 1) { throw 'Expected exactly one NeoForge Patchouli dependency.' }
$jars = @((Resolve-Path -LiteralPath $AuraJar).Path, $patchouli[0].FullName)
foreach ($helper in $HelperJars) { $jars += (Resolve-Path -LiteralPath $helper).Path }
New-Item -ItemType Directory -Path (Join-Path $root 'mods') | Out-Null
foreach ($part in @('libraries', 'eula.txt')) {
    Copy-Item -LiteralPath (Join-Path $template $part) -Destination (Join-Path $root $part) -Recurse
}
foreach ($jar in $jars) { Copy-Item -LiteralPath $jar -Destination (Join-Path $root 'mods') }
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
generate-structures=false
gamemode=creative
difficulty=peaceful
max-tick-time=60000
"@ | Set-Content -LiteralPath (Join-Path $root 'server.properties')
[ordered]@{
    fixtureDirectory=$root; createdUtc=[DateTime]::UtcNow.ToString('o'); port=$Port
    mods=@($jars | ForEach-Object {
        @{ name=(Split-Path -Leaf $_); sha256=(Get-FileHash -LiteralPath $_ -Algorithm SHA256).Hash.ToLowerInvariant() }
    })
} | ConvertTo-Json -Depth 5 | Set-Content -LiteralPath (Join-Path $root 'fixture-install.json')
Write-Output $root
