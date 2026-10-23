#!/usr/bin/env sh
set -e
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"
git config core.hooksPath .githooks
chmod +x .githooks/pre-commit 2>/dev/null || true
echo "Installed git hooks: core.hooksPath = .githooks"
echo "Pre-commit runs: :app:compileDebugKotlin :app:compileDebugAndroidTestKotlin (when Kotlin/Gradle is staged)"
echo "Skip once: git commit --no-verify"
