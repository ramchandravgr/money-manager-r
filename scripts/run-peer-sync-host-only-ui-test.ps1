# Host-only E2E: automated Samsung (or host serial), manual client on second phone.
# Prints pairingPin + seedNote from logcat; paste on the other device while this waits for pairing.
param(
    [string]$HostSerial = "adb-RZGL906ER5R-z2S0eC._adb-tls-connect._tcp",
    [string]$Adb = "E:\SW\AndroidLocal\Android\Sdk\platform-tools\adb.exe"
)

$ErrorActionPreference = "Stop"
Push-Location (Split-Path $PSScriptRoot -Parent)

$env:ANDROID_SERIAL = $HostSerial
Write-Host "Installing debug + androidTest on host $HostSerial..."
& .\gradlew.bat :app:installDebug :app:installDebugAndroidTest --no-daemon | Out-Null

$runner = "com.rrparallelstudio.miapp.test/com.rrparallelstudio.miapp.HiltTestRunner"
$hostClass = "com.rrparallelstudio.miapp.PeerDeviceSyncE2eUiTest#hostRole_pairApproveAndSyncNow"

& $Adb -s $HostSerial shell input keyevent KEYCODE_WAKEUP | Out-Null
& $Adb -s $HostSerial logcat -c
Write-Host "HOST test running — watch phone UI; paste PIN on second device when shown below."
Write-Host ""

& $Adb -s $HostSerial shell am instrument -w -e peer_sync_role host -e class $hostClass $runner

Pop-Location
