# Money Inbox — app versioning

User-facing releases use [Semantic Versioning](https://semver.org/) (`MAJOR.MINOR.PATCH`). Internal wire formats (backup JSON, LAN payload, signed archive, Room schema) use **separate integer versions** — bump those when import/sync/migration behavior changes, not necessarily with every app semver bump.

## Source of truth

| Field | Location |
|-------|----------|
| `versionName` / `versionCode` | `app/build.gradle.kts` `defaultConfig` |
| Release notes copy | `app/src/main/java/com/rrparallelstudio/miapp/release/MmrReleaseNotesCatalog.kt` |
| “What’s new” ack | `ReleaseNotesStore` (per `versionName`) |

### `versionCode` formula

```
versionCode = major * 10000 + minor * 100 + patch
```

Examples: `0.9.0` → **900**, `0.9.1` → **901**, `1.0.0` → **10000**.

**Every Play upload** (internal, closed, open, production) must use a **strictly greater** `versionCode` than any previous upload for the same `applicationId`.

## When to bump `versionName`

While pre–1.0 (`0.y.z`), treat **MINOR** and **PATCH** as below; reserve **1.0.0** for the first production Play release when backup/LAN contracts are stable enough for real users.

| Bump | When | Example |
|------|------|---------|
| **PATCH** (`0.9.0` → `0.9.1`) | Bug fixes, copy/UI tweaks, security hardening **without** new backup/LAN import requirements for existing archives | Lint fixes, session TTL tweak |
| **MINOR** (`0.9.x` → `0.10.0`) | New user-facing features; backward-compatible backup import (same backup JSON / archive format, or app still reads old exports) | New screen, approval flow improvement |
| **MINOR** (also) | **Breaking** backup or LAN wire format for shipped users — prefer bumping **`BackupJsonCodec` / `LanSyncJsonCodec` / archive format** and documenting in release notes; bump app **MINOR** in the same release | Backup v4 → v5, LAN v6 → v7 |
| **MAJOR** (`0.x` → `1.0.0`) | First **stable** Play production milestone; slow intentional breaking changes after 1.0 | First public “stable” launch |
| **MAJOR** (`1.x` → `2.0.0`) | Post–1.0 breaking user contract (rare; plan migrations) | Package rename after shipped users |

### Build / channel (not separate semver)

Use the **same** `versionName` for debug and release builds of a given release. Distinguish channels by **how** you ship, not by suffix:

| Channel | Version bump | Notes |
|---------|--------------|--------|
| Local debug / `installDebug` | Optional PATCH only if you’re tracking builds; often skip until a test APK | No Play `versionCode` requirement |
| Play **internal** / **closed** test | **PATCH** or **MINOR** per table above | Must increment `versionCode` each upload |
| Play **production** | **PATCH** / **MINOR** / **1.0.0** per table | Update `MmrReleaseNotesCatalog` before upload |

Do **not** use fourth-number schemes (`0.9.0.1`) in `versionName`; Play uses `versionCode` for ordering.

## Release checklist

1. Decide **PATCH vs MINOR** (and wire-format bumps if any).
2. Update `app/build.gradle.kts`: `versionName`, `versionCode`.
3. Add/update **`MmrReleaseNotesCatalog`** (newest bullets first for current version; move prior entry to history if MINOR+).
4. Run `:app:compileDebugKotlin` and targeted tests.
5. Tag git optional: `v0.9.1`.
6. Upload AAB to Play or install locally (`./gradlew :app:installDebug`).
7. If older builds must not run after a breaking release: publish **`min_version_code`** in Firebase Remote Config (see [`docs/ads-admob-firebase-setup.md`](ads-admob-firebase-setup.md)); set to the new release’s `versionCode` after Play serves that build.

## Related (not app semver)

- Signed backup file extension: **`.mibxbac`** (`BackupSignedZipCodec.SUGGESTED_FILE_EXTENSION`); legacy **`.mmrbackup`** still imports.
- See `PROJECT_MEMORY.md` § Release builds for R8/minify and security audit pointers.
