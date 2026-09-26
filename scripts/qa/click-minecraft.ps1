param(
    [Parameter(Mandatory = $true)][int]$ProcessId,
    [Parameter(Mandatory = $true)][int]$X,
    [Parameter(Mandatory = $true)][int]$Y,
    [switch]$Right
)

$ErrorActionPreference = 'Stop'
Add-Type -TypeDefinition @'
using System;
using System.Runtime.InteropServices;
public static class MinecraftInput {
    [StructLayout(LayoutKind.Sequential)] public struct Point { public int X; public int Y; }
    [StructLayout(LayoutKind.Sequential)] public struct Rect { public int Left; public int Top; public int Right; public int Bottom; }
    [DllImport("user32.dll")] public static extern bool SetProcessDpiAwarenessContext(IntPtr value);
    [DllImport("user32.dll")] public static extern bool GetClientRect(IntPtr window, out Rect rect);
    [DllImport("user32.dll")] public static extern bool ClientToScreen(IntPtr window, ref Point point);
    [DllImport("user32.dll")] public static extern bool SetForegroundWindow(IntPtr window);
    [DllImport("user32.dll")] public static extern bool SetCursorPos(int x, int y);
    [DllImport("user32.dll")] public static extern void mouse_event(uint flags, uint x, uint y, uint data, UIntPtr extra);
}
'@
[void][MinecraftInput]::SetProcessDpiAwarenessContext([IntPtr](-4))
$game = Get-Process -Id $ProcessId
if ($game.ProcessName -notin @('java', 'javaw') -or $game.MainWindowTitle -notlike 'Minecraft*') {
    throw 'Target must be the running Minecraft Java window.'
}
$rect = New-Object MinecraftInput+Rect
if (-not [MinecraftInput]::GetClientRect($game.MainWindowHandle, [ref]$rect)) { throw 'No client rectangle' }
if ($X -lt 0 -or $Y -lt 0 -or $X -ge $rect.Right -or $Y -ge $rect.Bottom) { throw 'Point outside client' }
$point = New-Object MinecraftInput+Point
$point.X = $X
$point.Y = $Y
if (-not [MinecraftInput]::ClientToScreen($game.MainWindowHandle, [ref]$point)) { throw 'No screen coordinates' }
if (-not [MinecraftInput]::SetForegroundWindow($game.MainWindowHandle)) { throw 'Could not focus Minecraft' }
Start-Sleep -Milliseconds 200
[void][MinecraftInput]::SetCursorPos($point.X, $point.Y)
Start-Sleep -Milliseconds 100
$down = if ($Right) { 0x0008 } else { 0x0002 }
$up = if ($Right) { 0x0010 } else { 0x0004 }
[MinecraftInput]::mouse_event($down, 0, 0, 0, [UIntPtr]::Zero)
Start-Sleep -Milliseconds 100
[MinecraftInput]::mouse_event($up, 0, 0, 0, [UIntPtr]::Zero)
[pscustomobject]@{ process_id=$ProcessId; client_x=$X; client_y=$Y; width=$rect.Right; height=$rect.Bottom }
