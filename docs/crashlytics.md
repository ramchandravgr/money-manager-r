# Firebase Crashlytics (Money Inbox)

## What is wired

| Piece | Location |
| --- | --- |
| Gradle SDK + plugin | `app/build.gradle.kts` (plugins apply only when `app/google-services.json` exists) |
| Startup policy | `MmrCrashlytics` + `MmrCrashlyticsPolicy` |
| App entry | `MmrFirebaseBootstrap.start()` from `MmrApplication.onCreate` |
| Custom keys | `version_name`, `version_code`, `build_type` on each session |
| Unit tests | `MmrCrashlyticsPolicyTest` |
| Debug verification | `MmrCrashlyticsDebugReceiver` (debug manifest only) |

## Collection policy

- **Release** (with `google-services.json` in the build): Crashlytics **on**.
- **Debug**: **off** unless `CRASHLYTICS_DEBUG=true` in `local.properties` (avoids polluting production crash dashboards during daily dev).
- **No** `google-services.json`: Firebase not configured → **no** collection (SDK may still be on classpath).

## One-time console setup

1. Firebase project **Money Inbox** (`money-inbox-156b5`) → Android app `com.rrparallelstudio.miapp`.
2. Download `google-services.json` → `app/google-services.json` (gitignored).
3. Firebase console → **Crashlytics** → enable for the project (if prompted).
4. Console: [Crashlytics issues](https://console.firebase.google.com/project/money-inbox-156b5/crashlytics/app/android:com.rrparallelstudio.miapp/issues).

## Verify on a device (recommended before Play)

**Status (2026-11-10):** Verification crash confirmed in Firebase console for `com.rrparallelstudio.miapp` (SM-S938B, debug build with `CRASHLYTICS_DEBUG=true`). VPN/firewall must allow Crashlytics endpoints.

### Option A — release-style build (collection on by default)

```powershell
# Requires signing / assembleRelease as you normally do for Play
.\gradlew :app:assembleRelease
adb install -r app\build\outputs\apk\release\app-release.apk
# Use the app briefly, then check the console (may take a few minutes)
```

### Option B — debug build with explicit opt-in

1. In `local.properties`: `CRASHLYTICS_DEBUG=true`
2. `.\gradlew :app:installDebug`
3. Force a test crash:

```powershell
adb shell am broadcast -a com.rrparallelstudio.miapp.action.CRASHLYTICS_TEST_CRASH -n com.rrparallelstudio.miapp/com.rrparallelstudio.miapp.debug.MmrCrashlyticsDebugReceiver
```

(Debug receiver is `exported=true` only in the debug manifest so shell adb can trigger it.)

4. Relaunch the app once (unsent reports flush on next start).
5. Open the Crashlytics console → issue titled **Money Inbox Crashlytics verification crash**.

### Automated smoke (local)

```powershell
.\scripts\verify-crashlytics-integration.ps1
```

Runs unit tests + checks `google-services.json` + confirms release manifest contains the Crashlytics build ID when Firebase is configured.

## Play / CI pipeline

1. Store **`google-services.json`** as a CI secret (or secure file), written to `app/google-services.json` before `bundleRelease`.
2. Without that file, Play AABs **do not** apply the Crashlytics Gradle plugin — reports will not ship.
3. After the first production or internal build with Firebase configured, confirm the console shows **at least one session** before relying on Vitals.

## Privacy / Play Data safety

When Crashlytics is enabled in release, declare **crash/diagnostic data** shared with **Google** in Data safety (align with privacy policy updates).

## Related

- Ads + Remote Config: [`docs/ads-admob-firebase-setup.md`](ads-admob-firebase-setup.md)
- `PROJECT_MEMORY.md` → Firebase console URLs
