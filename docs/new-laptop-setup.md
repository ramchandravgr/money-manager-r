# New laptop — MMR (MoneyInbox) setup

Use this when cloning or copying the repo onto a fresh machine. Tool versions below match the repo as of **2026-10-24**; prefer `gradle/libs.versions.toml` and `gradle/wrapper/gradle-wrapper.properties` if they drift.

---

## 1. What to copy from the old laptop

| Item | Typical path | Notes |
|------|----------------|-------|
| Whole repo | `D:\Development\MMR` (or your clone) | Git history + `.idea`, `.cursor`, `local.properties`, etc. if you committed or copied the folder |
| `local.properties` | repo root | `sdk.dir` + `GOOGLE_WEB_CLIENT_ID` — see [§4](#4-localproperties-and-google-sign-in) |
| `gradle.properties` | repo root | **Edit** `org.gradle.java.home` on the new PC (see [§3](#3-jdk-and-gradle)) |
| Git hooks config | not in repo | Re-run `scripts/install-git-hooks.ps1` after clone |
| Android SDK | `%LOCALAPPDATA%\Android\Sdk` | Optional full copy; faster to reinstall via Android Studio |
| Gradle caches | `%USERPROFILE%\.gradle` | Optional; wrapper will re-download Gradle 9.6.0 |
| Signing keys | your safe store | Only if you use release signing outside the repo (this project has no committed keystore) |
| OAuth JSON | `client_secret*.json` | Was historically gitignored; keep in a password manager or Cloud Console download — **never** publish publicly |

**Repo docs:** `PROJECT_MEMORY.md` (architecture), `docs/ui-test-screenshot-trace.md`, `docs/test-coverage-matrix.md`.

---

## 2. Install on the new machine

1. **Git** (for clone / pull).
2. **Android Studio** (stable channel) with:
   - Android SDK **API 35** (project `compileSdk` / `targetSdk`)
   - SDK Platform **API 30** if you run the same emulator as CI
   - **Android SDK Build-Tools** (Studio usually installs latest compatible)
   - **Platform-tools** (`adb`)
3. **Cursor** (optional) — project rules live in `.cursor/rules/`.
4. **Sh Git** (Git for Windows) — pre-commit hook is a `#!/bin/sh` script in `.githooks/pre-commit`.

No separate Node/Python stack is required for the Android app.

---

## 3. JDK and Gradle

| Setting | Value |
|---------|--------|
| **Gradle (wrapper)** | **9.6.0** (`gradle/wrapper/gradle-wrapper.properties`) |
| **Android Gradle Plugin** | **9.4.1** |
| **Kotlin** | **2.2.10** |
| **KSP** | **2.2.10-2.0.2** |
| **Java language level (app)** | **11** (`compileOptions` / `jvmTarget` in `app/build.gradle.kts`) |
| **JDK to run Gradle** | **17+** (CI uses Temurin 17; AGP needs a full JDK with `jlink`) |

**`gradle.properties` on the new laptop**

- Set **`org.gradle.java.home`** to this machine’s JDK, e.g. Android Studio’s embedded runtime:
  - Windows: `C\:\\Program Files\\Android\\Android Studio\\jbr`
  - Or comment out the line and set **Settings → Build, Execution, Deployment → Build Tools → Gradle → Gradle JDK** to **Embedded JDK** (17+).
- Do **not** point Gradle at a bare JRE (e.g. some VS Code Java extension runtimes) — builds will fail on `jlink`.

First build (downloads dependencies):

```powershell
cd D:\Development\MMR   # your path
.\gradlew :app:compileDebugKotlin --no-daemon
```

---

## 4. `local.properties` and Google Sign-In

Copy from the old laptop or create from **`local.properties.example`**:

```properties
sdk.dir=C\:\\Users\\<You>\\AppData\\Local\\Android\\Sdk
GOOGLE_WEB_CLIENT_ID=<Web OAuth client ID from Google Cloud Console>
```

- Drives **`BuildConfig.GOOGLE_WEB_CLIENT_ID`** for Drive backup / Credential Manager (`app/build.gradle.kts`).
- **Android** OAuth client in Cloud Console: package `com.mmr.app` + **debug keystore SHA-1** (new laptop = new debug SHA-1 unless you copy `~/.android/debug.keystore` — if you copy the keystore, SHA-1 stays the same).
- Details: `PROJECT_MEMORY.md` (Google OAuth / Drive backup).

---

## 5. App identity (from repo)

| Field | Value |
|-------|--------|
| Application ID | `com.mmr.app` |
| `versionCode` / `versionName` | `1` / `1.0` |
| `minSdk` | 24 |
| `compileSdk` / `targetSdk` | 35 |
| Room DB version | 17 (`MmrDatabase`) |
| Root project name | `MoneyInbox` (`settings.gradle.kts`) |

---

## 6. Key dependency pins (`gradle/libs.versions.toml`)

Use the TOML as source of truth. Highlights:

| Area | Version |
|------|---------|
| Compose BOM | 2026.02.01 |
| Navigation Compose | 2.8.8 |
| Hilt | 2.60.1 |
| Room | 2.7.1 |
| Coroutines | 1.9.0 |
| Espresso (androidTest) | 3.7.0 |
| AndroidX Test JUnit | 1.2.1 |
| WorkManager | 2.10.1 |
| Glance (widgets) | 1.1.1 |
| Detekt | 1.23.8 |
| ktlint (Gradle plugin) | 13.1.0 |

---

## 7. Git hooks (local only)

```powershell
.\scripts\install-git-hooks.ps1
```

Sets `core.hooksPath` to `.githooks`. Pre-commit runs **ktlint**, **detekt**, and **compileDebugKotlin** + **compileDebugAndroidTestKotlin** when Kotlin/Gradle files are staged. Skip once: `git commit --no-verify`.

---

## 8. Run and test

**Emulator or device**

```powershell
.\gradlew :app:installDebug
```

**Unit tests**

```powershell
.\gradlew :app:testDebugUnitTest
```

**Instrumented tests** (device/emulator; long on a physical phone)

```powershell
.\gradlew :app:connectedDebugAndroidTest
```

- Pull UI screenshot traces after a run: `.\gradlew :app:pullUiTestScreenshots` → `app/build/ui-test-trace/`.
- CI (`.github/workflows/android-instrumented.yml`): API **30** emulator, JDK **17**, excludes `ApprovalInboxScanUiTest`.

**Physical device tips**

- Enable USB debugging; for wireless adb, pair in Studio **Device Manager**.
- If multiple devices: `adb -s <serial> ...`

---

## 9. IDE / editor preferences

Tracked in repo (after you commit them):

- **`.idea/`** — Android Studio project settings (JDK, deployment target, run configs). Open the folder as the project root.
- **`.cursor/rules/`** — Cursor agent rules (`project-memory.mdc`, UI system rules).

Not in repo (reconfigure manually):

- Android Studio **keymap**, **plugins**, global SDK paths
- Cursor account, extensions, user settings
- `git config user.name` / `user.email`

---

## 10. Optional: copying debug keystore (same debug SHA-1)

Default debug keystore:

- Windows: `%USERPROFILE%\.android\debug.keystore`

Copying it to the new laptop keeps the same debug signing certificate so existing Google OAuth **debug** SHA-1 entries keep working without Cloud Console changes.

---

## 11. Large folders and git

`.gitignore` was cleared so machine-specific files can travel with the repo. Before pushing to a **public** remote:

- Avoid committing **`client_secret*.json`**, production keystores, or secrets.
- Consider excluding **`build/`**, **`.gradle/`**, and **`app/build/`** from commits (large, reproducible) unless you intentionally want a full disk mirror.

For a private backup branch or USB copy, committing `.idea` + `local.properties` + `gradle.properties` is usually enough for a smooth Studio reopen.

---

## 12. Quick health check

```powershell
.\gradlew :app:compileDebugKotlin :app:compileDebugAndroidTestKotlin --no-daemon
.\gradlew :app:ktlintCheck :app:detekt --no-daemon
```

Both should pass before you rely on the new machine for daily dev.
