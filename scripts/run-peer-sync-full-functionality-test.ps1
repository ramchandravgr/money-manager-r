# Single-phone Peer Device Sync: JVM merge tests + LAN instrumented sweep + UI flow tests.
# Dual-phone E2E: scripts/run-peer-sync-dual-device-ui-test.ps1
param(
    [string]$DeviceSerial = $env:ANDROID_SERIAL,
    [string]$SdkRoot = "E:\SW\AndroidLocal\Android\Sdk"
)

$ErrorActionPreference = "Stop"
if (-not $DeviceSerial) {
    throw "Set ANDROID_SERIAL or pass -DeviceSerial."
}
$env:ANDROID_HOME = $SdkRoot
$env:ANDROID_SERIAL = $DeviceSerial
Push-Location (Split-Path $PSScriptRoot -Parent)

Write-Host "JVM: com.rrparallelstudio.miapp.core.lansync.*"
& .\gradlew.bat :app:testDebugUnitTest --tests "com.rrparallelstudio.miapp.core.lansync.*" --no-daemon
if ($LASTEXITCODE -ne 0) { throw "JVM LAN sync tests failed." }

$instrumented =
    @(
        "com.rrparallelstudio.miapp.core.lansync.LanSyncComprehensivePayloadInstrumentedTest",
        "com.rrparallelstudio.miapp.core.lansync.LanSyncMergerInstrumentedTest",
        "com.rrparallelstudio.miapp.core.lansync.LanSyncConflictDetectorInstrumentedTest",
        "com.rrparallelstudio.miapp.core.lansync.LanSyncAccountBalanceValidatorInstrumentedTest",
        "com.rrparallelstudio.miapp.core.lansync.transport.LanSyncHttpServerInstrumentedTest"
    ) -join ","

Write-Host "Instrumented LAN sweep on $DeviceSerial..."
& .\gradlew.bat :app:connectedDebugAndroidTest "-Pandroid.testInstrumentationRunnerArguments.class=$instrumented" --no-daemon
if ($LASTEXITCODE -ne 0) { throw "LAN instrumented sweep failed." }

Write-Host "UI: PeerDeviceSyncFlowUiTest (Wi‑Fi required)..."
& .\gradlew.bat :app:connectedDebugAndroidTest "-Pandroid.testInstrumentationRunnerArguments.class=com.rrparallelstudio.miapp.PeerDeviceSyncFlowUiTest" --no-daemon
if ($LASTEXITCODE -ne 0) { throw "Peer Device Sync UI flow failed." }

Write-Host "Peer Device Sync full functionality: OK"
Pop-Location
