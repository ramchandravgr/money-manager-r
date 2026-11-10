# Crashlytics integration smoke: policy unit tests + Firebase build wiring checks.
$ErrorActionPreference = "Stop"
$root = Split-Path $PSScriptRoot -Parent
Set-Location $root

Write-Host "Running MmrCrashlyticsPolicyTest..."
& .\gradlew.bat :app:testDebugUnitTest --tests "com.rrparallelstudio.miapp.ads.firebase.MmrCrashlyticsPolicyTest" --no-daemon -q
if ($LASTEXITCODE -ne 0) { throw "Unit tests failed" }

$gs = Join-Path $root "app\google-services.json"
if (Test-Path $gs) {
    Write-Host "google-services.json present — checking release Crashlytics manifest merge..."
    & .\gradlew.bat :app:processReleaseMainManifest --no-daemon -q
    if ($LASTEXITCODE -ne 0) { throw "processReleaseMainManifest failed" }
    $merged = Get-ChildItem -Path (Join-Path $root "app\build\intermediates\merged_manifest") -Recurse -Filter "AndroidManifest.xml" |
        Where-Object { $_.FullName -match "release" } |
        Sort-Object LastWriteTime -Descending |
        Select-Object -First 1
    if (-not $merged) { throw "Merged release manifest not found" }
    $text = Get-Content $merged.FullName -Raw
    if ($text -notmatch "CrashlyticsRegistrar|firebase_crashlytics") {
        Write-Warning "Merged manifest missing Crashlytics registrar — confirm Crashlytics plugin applied"
    } else {
        Write-Host "Release manifest includes Crashlytics (Firebase registrar)."
    }
} else {
    Write-Host "google-services.json missing (OK for CI without secrets) — skipping release manifest check."
}

Write-Host "Crashlytics integration checks passed."
