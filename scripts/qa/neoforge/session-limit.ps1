# Unknown Java processes are conservatively counted as game sessions.
function Get-MinecraftSession {
    @(Get-CimInstance Win32_Process | Where-Object {
        $_.Name -match '^javaw?\.exe$' -and
        $_.CommandLine -notmatch 'org\.gradle\.|GradleDaemon|GradleWorkerMain|JUnitStarter'
    } | Select-Object ProcessId, ParentProcessId)
}

function Assert-MinecraftSessionCapacity([int]$Additional = 1) {
    $sessions = @(Get-MinecraftSession)
    if ($sessions.Count + $Additional -gt 4) {
        throw "Four-session Minecraft limit: $($sessions.Count) active/unknown JVMs, $Additional requested. Coordinate with Mod Playtesting before launching."
    }
    return $sessions
}
