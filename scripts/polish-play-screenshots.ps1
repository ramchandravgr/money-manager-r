# Crop status + nav bars and optionally wait/recapture. Run after capture-play-listing-screenshots.ps1
param(
    [Parameter(Mandatory = $true)]
    [string]$Serial,
    [string]$Adb = "E:\SW\AndroidLocal\Android\Sdk\platform-tools\adb.exe",
    [string]$Dir = "e:\STM\MMR\docs\device-screenshots\play-listing",
    [int]$CropTop = 110,
    [int]$CropBottom = 132,
    [switch]$RecaptureUploadSet  # long run; keep phone unlocked
)

Add-Type -AssemblyName System.Drawing

function Invoke-Adb([string[]]$Args) {
    & $Adb -s $Serial @Args
}

function Screenshot([string]$path) {
    cmd /c "`"$Adb`" -s $Serial exec-out screencap -p > `"$path`""
}

function Dump-Raw {
    Invoke-Adb shell, uiautomator, dump, /sdcard/uidump.xml | Out-Null
    $local = Join-Path $env:TEMP "mmr-polish-ui.xml"
    Invoke-Adb pull, /sdcard/uidump.xml, $local | Out-Null
    return Get-Content $local -Raw
}

function Tap-CenterFromMatch([System.Text.RegularExpressions.Match]$m) {
    $x = ([int]$m.Groups[1].Value + [int]$m.Groups[3].Value) / 2
    $y = ([int]$m.Groups[2].Value + [int]$m.Groups[4].Value) / 2
    Invoke-Adb shell, input, tap, [string]$x, [string]$y | Out-Null
}

function Tap-Desc([string]$d) {
    $raw = Dump-Raw
    $m = [regex]::Match($raw, "content-desc=`"$([regex]::Escape($d))`"[^>]*bounds=`"\[(\d+),(\d+)\]\[(\d+),(\d+)\]`"")
    if ($m.Success) { Tap-CenterFromMatch $m; Start-Sleep -Seconds 1 }
}

function Tap-Text([string]$t) {
    $raw = Dump-Raw
    $m = [regex]::Match($raw, "text=`"$([regex]::Escape($t))`"[^>]*bounds=`"\[(\d+),(\d+)\]\[(\d+),(\d+)\]`"")
    if (-not $m.Success) {
        $ea = $t -replace '&', '&amp;'
        $m = [regex]::Match($raw, "text=`"$([regex]::Escape($ea))`"[^>]*bounds=`"\[(\d+),(\d+)\]\[(\d+),(\d+)\]`"")
    }
    if ($m.Success) { Tap-CenterFromMatch $m; Start-Sleep -Seconds 1 }
}

function Wait-Until([scriptblock]$Predicate, [int]$Sec = 20) {
    $deadline = (Get-Date).AddSeconds($Sec)
    do {
        if (& $Predicate) { return $true }
        Start-Sleep -Milliseconds 800
    } while ((Get-Date) -lt $deadline)
    return $false
}

function Prep-Device {
    Invoke-Adb shell, settings, put, system, show_touches, 0 | Out-Null
    Invoke-Adb shell, settings, put, system, pointer_location, 0 | Out-Null
    Invoke-Adb shell, cmd, statusbar, collapse | Out-Null
}

function Crop-Png([string]$src, [string]$dest) {
    $img = [System.Drawing.Image]::FromFile($src)
    $w = $img.Width
    $h = $img.Height - $CropTop - $CropBottom
    if ($h -lt 320) { $img.Dispose(); Copy-Item $src $dest -Force; return }
    $bmp = New-Object System.Drawing.Bitmap $w, $h
    $g = [System.Drawing.Graphics]::FromImage($bmp)
    $g.DrawImage($img, (New-Object System.Drawing.Rectangle 0, 0, $w, $h), (New-Object System.Drawing.Rectangle 0, $CropTop, $w, $h), [System.Drawing.GraphicsUnit]::Pixel)
    $g.Dispose(); $img.Dispose()
    $bmp.Save($dest, [System.Drawing.Imaging.ImageFormat]::Png)
    $bmp.Dispose()
}

function Nav-Route([string]$route) {
    Invoke-Adb shell, am, start, "-n", "com.rrparallelstudio.miapp/.MainActivity", "--es", "nav_route", $route | Out-Null
    Start-Sleep -Seconds 3
}

if ($RecaptureUploadSet) {
    Prep-Device
    Invoke-Adb shell, monkey, -p, com.rrparallelstudio.miapp, -c, android.intent.category.LAUNCHER, 1 | Out-Null
    Start-Sleep -Seconds 3
  # Home loaded with data
    Nav-Route "home"
    Wait-Until { (Dump-Raw) -match 'text="₹ 12,500.00"' } | Out-Null
    Invoke-Adb shell, input, swipe, 540, 1500, 540, 900, 350 | Out-Null
    Prep-Device; Screenshot (Join-Path $Dir "01_home_daily.png")

    Nav-Route "home"
    Start-Sleep -Seconds 2
    Tap-Desc "Approvals"
    Wait-Until { $r = Dump-Raw; $r -match 'Approval Center' -and $r -notmatch 'No pending' -or $r -match '-₹499' } -Sec 25 | Out-Null
    Start-Sleep -Seconds 1
    Prep-Device; Screenshot (Join-Path $Dir "04_approval_center.png")

    Nav-Route "home"
    $raw = Dump-Raw
    $m = [regex]::Match($raw, 'text="₹ 850.00"[^>]*bounds="\[(\d+),(\d+)\]\[(\d+),(\d+)\]"')
    if ($m.Success) { Tap-CenterFromMatch $m; Start-Sleep -Seconds 2 }
    Wait-Until { (Dump-Raw) -match 'Transaction' } | Out-Null
    Prep-Device; Screenshot (Join-Path $Dir "06_transaction_detail.png")
    Invoke-Adb shell, input, keyevent, 4 | Out-Null

    Tap-Desc "Search"
    Wait-Until { (Dump-Raw) -match 'Search' } | Out-Null
    Start-Sleep -Seconds 1
    Prep-Device; Screenshot (Join-Path $Dir "05_transaction_search.png")
    Invoke-Adb shell, input, keyevent, 4 | Out-Null

    Invoke-Adb shell, input, tap, 270, 2158 | Out-Null
    Wait-Until { (Dump-Raw) -match 'Trends|Income' } -Sec 15 | Out-Null
    Start-Sleep -Seconds 2
    Prep-Device; Screenshot (Join-Path $Dir "07_trends.png")

    Nav-Route "budgets"
    Wait-Until { $r = Dump-Raw; $r -match 'Budgets' -and $r -notmatch 'CircularProgressIndicator' } -Sec 20 | Out-Null
    Start-Sleep -Seconds 3
    Prep-Device; Screenshot (Join-Path $Dir "08_budgets.png")

    Nav-Route "settings"
    Wait-Until { (Dump-Raw) -match 'Settings' } | Out-Null
    Prep-Device; Screenshot (Join-Path $Dir "11_settings.png")

    Tap-Text "Home & Menu"
    Start-Sleep -Seconds 1
    Invoke-Adb shell, input, swipe, 540, 2000, 540, 600, 450 | Out-Null
    Start-Sleep -Seconds 1
    Tap-Text "App Color Theme"
    Start-Sleep -Seconds 1
    Prep-Device; Screenshot (Join-Path $Dir "15_themes.png")
    Invoke-Adb shell, input, keyevent, 4 | Out-Null

    Invoke-Adb shell, input, tap, 810, 2158 | Out-Null
    Wait-Until { (Dump-Raw) -match 'Scheduled' } | Out-Null
    Start-Sleep -Seconds 2
    Prep-Device; Screenshot (Join-Path $Dir "10_scheduled.png")

    Nav-Route "settings"
    Tap-Text "Backup & Restore"
    Wait-Until { (Dump-Raw) -match 'Export Backup' } | Out-Null
    Start-Sleep -Seconds 1
    Prep-Device; Screenshot (Join-Path $Dir "14_backup_restore.png")
}

$polished = Join-Path $Dir "polished"
New-Item -ItemType Directory -Force -Path $polished | Out-Null

$files = @(
    "01_home_daily.png", "04_approval_center.png", "05_transaction_search.png", "06_transaction_detail.png",
    "07_trends.png", "08_budgets.png", "10_scheduled.png", "11_settings.png", "14_backup_restore.png", "15_themes.png"
)
foreach ($f in $files) {
    $src = Join-Path $Dir $f
    if (Test-Path $src) {
        Crop-Png $src (Join-Path $polished $f)
        Write-Host "polished $f"
    }
}

# Refresh upload-phone-8 from polished versions of the curated set
$upload = Join-Path $Dir "upload-phone-8"
New-Item -ItemType Directory -Force -Path $upload | Out-Null
$map = @{
    "01_home_daily.png" = "01_home_daily.png"
    "04_approval_center.png" = "02_approval_center.png"
    "06_transaction_detail.png" = "03_transaction_detail.png"
    "07_trends.png" = "04_trends.png"
    "08_budgets.png" = "05_budgets.png"
    "10_scheduled.png" = "06_scheduled.png"
    "15_themes.png" = "07_themes.png"
    "14_backup_restore.png" = "08_backup_restore.png"
}
foreach ($e in $map.GetEnumerator()) {
    $src = Join-Path $polished $e.Key
    if (Test-Path $src) { Copy-Item $src (Join-Path $upload $e.Value) -Force }
}
Write-Host "Polished -> $polished ; upload set -> $upload"
