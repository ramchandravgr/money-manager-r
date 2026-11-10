# Capture Money Inbox Play listing screenshots via adb (device must already have demo data).
param(
    [Parameter(Mandatory = $true)]
    [string]$Serial,
    [string]$Adb = "E:\SW\AndroidLocal\Android\Sdk\platform-tools\adb.exe",
    [string]$OutDir = "e:\STM\MMR\docs\device-screenshots\play-listing"
)

$ErrorActionPreference = "Stop"
New-Item -ItemType Directory -Force -Path $OutDir | Out-Null

function Invoke-Adb([string[]]$Args) {
    & $Adb -s $Serial @Args
    if ($LASTEXITCODE -ne 0) { throw "adb failed: $Args" }
}

function Dump-UiRaw {
    Invoke-Adb "shell", "uiautomator", "dump", "/sdcard/uidump.xml" | Out-Null
    $local = Join-Path $env:TEMP "mmr-uidump.xml"
    Invoke-Adb "pull", "/sdcard/uidump.xml", $local | Out-Null
    return Get-Content $local -Raw
}

function Center-Bounds([string]$Bounds) {
    if ($Bounds -notmatch '\[(\d+),(\d+)\]\[(\d+),(\d+)\]') { return $null }
    return @{
        X = ([int]$Matches[1] + [int]$Matches[3]) / 2
        Y = ([int]$Matches[2] + [int]$Matches[4]) / 2
    }
}

function Tap-Text([string]$Text) {
    $raw = Dump-UiRaw
    $m = [regex]::Match($raw, "text=`"$([regex]::Escape($Text))`"[^>]*bounds=`"(\[[^\]]+\]\[[^\]]+\])`"")
    if (-not $m.Success) {
        $escapedAmp = $Text -replace '&', '&amp;'
        $m = [regex]::Match($raw, "text=`"$([regex]::Escape($escapedAmp))`"[^>]*bounds=`"(\[[^\]]+\]\[[^\]]+\])`"")
    }
    if (-not $m.Success) { throw "Text not found: $Text" }
    $c = Center-Bounds $m.Groups[1].Value
    Invoke-Adb "shell", "input", "tap", [string]$c.X, [string]$c.Y | Out-Null
    Start-Sleep -Milliseconds 600
}

function Tap-ContentDesc([string]$Desc) {
    $raw = Dump-UiRaw
    $m = [regex]::Match($raw, "content-desc=`"$([regex]::Escape($Desc))`"[^>]*bounds=`"(\[[^\]]+\]\[[^\]]+\])`"")
    if (-not $m.Success) { throw "content-desc not found: $Desc" }
    $c = Center-Bounds $m.Groups[1].Value
    Invoke-Adb "shell", "input", "tap", [string]$c.X, [string]$c.Y | Out-Null
    Start-Sleep -Milliseconds 600
}

function Screenshot([string]$Name) {
    $path = Join-Path $OutDir "$Name.png"
    # PowerShell corrupts exec-out binary; use cmd redirect.
    cmd /c "`"$Adb`" -s $Serial exec-out screencap -p > `"$path`""
    if (-not (Test-Path $path)) { throw "Screenshot failed: $Name" }
    Write-Host "Saved $path ($((Get-Item $path).Length) bytes)"
}

function Nav-Route([string]$Route) {
    Invoke-Adb "shell", "am", "start", "-n", "com.rrparallelstudio.miapp/.MainActivity", `
        "--es", "nav_route", $Route | Out-Null
    Start-Sleep -Seconds 2
}

Write-Host "Serial=$Serial OutDir=$OutDir"
Write-Host "Assumes demo ledger + pending SMS already on device (seed manually first)."

Nav-Route "home"
Invoke-Adb "shell", "input", "swipe", "540", "1500", "540", "900", "350" | Out-Null
Screenshot "01_home_daily"
try { Tap-Text "Daily"; Tap-Text "Weekly"; Screenshot "02_home_weekly"; Tap-Text "Weekly"; Tap-Text "Monthly"; Screenshot "03_home_monthly" } catch { Write-Warning "Ledger view-mode taps failed." }

Nav-Route "approval_center"
Screenshot "04_approval_center"

Nav-Route "home"
try { Tap-ContentDesc "Search"; Screenshot "05_transaction_search" } catch { Write-Warning "Search screen capture skipped." }
Invoke-Adb "shell", "input", "keyevent", "4" | Out-Null
Start-Sleep -Seconds 1

Nav-Route "home"
# Open first visible expense row (Groceries ₹850).
$raw = Dump-UiRaw
$m = [regex]::Match($raw, 'text="₹ 850.00"[^>]*bounds="\[(\d+),(\d+)\]\[(\d+),(\d+)\]"')
if ($m.Success) {
    $cx = ([int]$m.Groups[1].Value + [int]$m.Groups[3].Value) / 2
    $cy = ([int]$m.Groups[2].Value + [int]$m.Groups[4].Value) / 2
    Invoke-Adb "shell", "input", "tap", [string]$cx, [string]$cy | Out-Null
    Start-Sleep -Seconds 2
}
Screenshot "06_transaction_detail"
Invoke-Adb "shell", "input", "keyevent", "4" | Out-Null

Invoke-Adb "shell", "input", "tap", "270", "2158" | Out-Null
Start-Sleep -Seconds 2
Screenshot "07_trends"

Nav-Route "budgets"
Screenshot "08_budgets"
Invoke-Adb "shell", "input", "tap", "630", "2158" | Out-Null
Start-Sleep -Seconds 2
Screenshot "09_assets"
Invoke-Adb "shell", "input", "tap", "810", "2158" | Out-Null
Start-Sleep -Seconds 2
Screenshot "10_scheduled"

Nav-Route "settings"
Screenshot "11_settings"
try {
    Tap-Text "Transaction Appearance"
    Invoke-Adb "shell", "input", "swipe", "540", "2000", "540", "600", "500" | Out-Null
    Start-Sleep -Seconds 1
    Tap-Text "App Color Theme"
    Start-Sleep -Seconds 1
    Screenshot "15_themes"
    Invoke-Adb "shell", "input", "keyevent", "4" | Out-Null
    Start-Sleep -Seconds 1
    Screenshot "12_configuration"
    Invoke-Adb "shell", "input", "keyevent", "4" | Out-Null
} catch { Write-Warning "Configuration / themes capture skipped." }
Nav-Route "settings"
try { Tap-Text "Categories"; Screenshot "13_categories" } catch { Write-Warning "Categories capture skipped." }
Nav-Route "settings"
try { Tap-Text "Backup & Restore"; Screenshot "14_backup_restore" } catch { Write-Warning "Backup capture skipped." }

Write-Host "Done (15 PNGs). Play default listing allows up to 8 phone screenshots — pick the best subset for upload."
