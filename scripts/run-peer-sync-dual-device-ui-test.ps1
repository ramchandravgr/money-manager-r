# Runs Peer Device Sync E2E: host on $HostSerial, client on $ClientSerial (same Wi‑Fi).
# Requires: adb, built debug + androidTest APKs, both devices connected.
param(
    [string]$HostSerial = "adb-RZGL906ER5R-z2S0eC._adb-tls-connect._tcp",
    [string]$ClientSerial = "9919cfdb",
    [string]$Adb = "E:\SW\AndroidLocal\Android\Sdk\platform-tools\adb.exe"
)

$ErrorActionPreference = "Stop"
Push-Location (Split-Path $PSScriptRoot -Parent)

Write-Host "Installing debug + androidTest on both devices..."
$env:ANDROID_SERIAL = $HostSerial
& .\gradlew.bat :app:installDebug :app:installDebugAndroidTest --no-daemon | Out-Null
$env:ANDROID_SERIAL = $ClientSerial
& .\gradlew.bat :app:installDebug :app:installDebugAndroidTest --no-daemon | Out-Null

$runner = "com.rrparallelstudio.miapp.test/com.rrparallelstudio.miapp.HiltTestRunner"
$hostClass = "com.rrparallelstudio.miapp.PeerDeviceSyncE2eUiTest#hostRole_pairApproveAndSyncNow"

Write-Host "Starting HOST on $HostSerial (background logcat)..."
& $Adb -s $HostSerial logcat -c
$hostJob =
    Start-Job -ScriptBlock {
        param($adb, $serial, $runner, $class)
        & $adb -s $serial shell am instrument -w -e peer_sync_role host -e class $class $runner 2>&1
    } -ArgumentList $Adb, $HostSerial, $runner, $hostClass

Start-Sleep -Seconds 25
$logLine =
    & $Adb -s $HostSerial logcat -d -s PEER_SYNC_E2E:I |
    Select-String "pairingPin" |
    Select-Object -Last 1
if (-not $logLine) {
    throw "Host did not emit PEER_SYNC_E2E log. Ensure Wi‑Fi and host test is running."
}
$jsonText = $logLine.Line.Substring($logLine.Line.IndexOf("{"))
$payload = $jsonText | ConvertFrom-Json
$pin = $payload.pairingPin
$seed = $payload.seedNote
Write-Host "Host pairingPin=$pin seedNote=$seed"

Write-Host "Running CLIENT on $ClientSerial..."
& $Adb -s $ClientSerial shell input keyevent KEYCODE_WAKEUP | Out-Null
& $Adb -s $ClientSerial shell am force-stop com.rrparallelstudio.miapp | Out-Null
& $Adb -s $ClientSerial shell am force-stop com.rrparallelstudio.miapp.test | Out-Null
if ($ClientSerial -eq "9919cfdb") {
    Write-Warning "OnePlus A5010 (API 28): Compose UI instrumentation often hangs with no on-screen action. Prefer Samsung as CLIENT or pair manually on OnePlus while host test runs on Samsung."
}
$clientOut =
    & $Adb -s $ClientSerial shell am instrument -w `
        -e peer_sync_role client `
        -e e2e_pairing_pin $pin `
        -e e2e_seed_note $seed `
        -e class com.rrparallelstudio.miapp.PeerDeviceSyncE2eUiTest#clientRole_trustsHostAndReceivesLedger `
        $runner 2>&1
Write-Host $clientOut

Wait-Job $hostJob -Timeout 300 | Out-Null
$hostOut = Receive-Job $hostJob
Write-Host "HOST output:"
Write-Host $hostOut

Pop-Location
