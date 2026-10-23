# Installs repo git hooks from .githooks/ (local core.hooksPath only).
$ErrorActionPreference = "Stop"
$Root = Split-Path -Parent (Split-Path -Parent $MyInvocation.MyCommand.Path)
Set-Location $Root

git config core.hooksPath .githooks
Write-Host "Installed git hooks: core.hooksPath = .githooks"
Write-Host "Pre-commit runs: :app:compileDebugKotlin :app:compileDebugAndroidTestKotlin (when Kotlin/Gradle is staged)"
Write-Host "Skip once: git commit --no-verify"
