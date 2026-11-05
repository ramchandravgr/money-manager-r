# MMR security audit — 2026-11-04

Canonical record of vulnerability / static security work on the **Money Inbox (MMR)** Android app (`com.mmr.app`).  
Repository: `E:/STM/MMR`. Branch reviewed: **`main`** at `53f8030` plus local uncommitted hardening.

---

## 1. Executive summary

| Category | Status |
|----------|--------|
| **Manual / agent static review (full app)** | Done — ~463 Kotlin sources + manifest + `res/xml` + `assets/web` |
| **First review (diff-only)** | Done — partial; missed committed code on `main` |
| **SCA (dependencies)** | OWASP Dependency-Check on `app/` tree; OSV spot-checks |
| **SAST (Android Lint)** | Full `:app:lintDebug` completed; **507 errors, 141 warnings** (see section 7) |
| **SAST (Semgrep)** | **Not completed** (pip blocked; npx package issues) |
| **Secrets scan (gitleaks)** | **Not run** |
| **Remediation in working tree** | Attachments, Web Manage, nav, backup rules; **LAN inbound pair** (PIN + pending approval); **backup import guard** (signed zip for untrusted sources). Open: APK HMAC, etc. |

---

## 2. What was scanned (scope)

### 2.1 First pass (2026-11-04, morning) — **change-biased**

| Tool | Scope | Result |
|------|--------|--------|
| Security Review subagent | **Uncommitted git diff only** (~35/122 changed files) | 2–3 medium findings in diff |
| Android Lint | Full `:app` module | **Failed** (AGP/Kotlin UAST internal error) |
| OWASP Dependency-Check CLI 12.1.3 | Directory `app/` (sources, assets, build outputs, APKs) | Exit 14 (Sonatype OSS Index unreachable); report written |
| OSV.dev API | `okhttp` 4.12.0, Room 2.7.1, fastexcel 0.18.4, kotlin-stdlib 2.2.10 | No known CVEs |
| Pattern grep (keys/certs) | Tracked source patterns | No private keys / common API key formats in repo source |
| Semgrep | — | Not run (App Control blocked `pip`) |

### 2.2 Second pass (2026-11-04, afternoon) — **full application code**

| Tool | Scope | Result |
|------|--------|--------|
| Security Review subagent | **Entire** `app/src/main/java`, manifest, `res/xml`, `assets/web` at HEAD | Open HIGH/MEDIUM list in section 5 |
| Android Lint `:app:lintDebug` | Full `:app` module | Completed; report path in section 7 |
| Semgrep via `npx` | `app/src/main` | Not completed (package/version 404) |

**Important:** Committed features on `main` (assets, reminders, calculator, sync expansion at `53f8030`) were **not** in the first diff-only review; they **are** covered by the second full-app review and Lint.

---

## 3. Software composition analysis (SCA)

### 3.1 OWASP Dependency-Check

- **Tool:** OWASP Dependency-Check **12.1.3** (CLI under `build/tools/dependency-check-extracted/`).
- **Data cache:** `build/tools/dc-data/`.
- **Reports:**
  - `build/reports/dependency-check/dependency-check-report.html`
  - `build/reports/dependency-check/dependency-check-report.json`
- **Notes:** First run downloaded full NVD (slow without NVD API key). Exit code **14** due to Sonatype OSS Index `component-reports` failures; NVD/Central analyzers still ran.
- **Finding (before fix):** Bundled **`highcharts.js` 4.2.7** → **CVE-2018-20801** (HIGH).

### 3.2 OSV.dev (spot-check)

No known CVEs reported for: OkHttp 4.12.0, Room 2.7.1, fastexcel 0.18.4, kotlin-stdlib 2.2.10.

---

## 4. Remediation already in working tree (not necessarily committed)

These address findings from the **first** scan and general hygiene. Verify on disk before release.

| Area | Change |
|------|--------|
| **Repo secrets** | `local.properties` in `.gitignore`; removed from git index (`git rm --cached`). Rotate Google OAuth web client if ever pushed. |
| **Attachment path traversal** | `MmrAttachmentSyncId` — validation + canonical paths in `TransactionAttachmentStore`, `AssetAttachmentStore`, LAN attachment GET, backup restore, `BackupSignedZipCodec`. |
| **Backup import DoS** | `MmrBackupAttachmentBytes` — per-file and total restore size caps; safe base64 decode. |
| **Web Manage** | Ephemeral port (`server.start()`); **PBKDF2-SHA256** (210k iter); `WebManageLoginRateLimiter`; client IP via `x-mmr-client-address` on HTTP server. |
| **Backup export / restore (M1)** | No Web Manage or LAN tokens in export; no `trustedLanDevices` in backup; import rotates LAN identity, clears trust, disables Web Manage (`BackupPayloadSanitizer`, `AppSettingsBackupCoordinator`). |
| **Deep links** | `MmrExternalNavRouteAllowlist` + `MmrNavRouteIntentTrust` (same-UID / internal delivery); strips `approvalId`/`action` query from untrusted intents. |
| **Android backup** | `backup_rules.xml` / `data_extraction_rules.xml` exclude database, shared prefs, attachment dirs. |
| **Highcharts** | Upgraded to **7.2.2** in `app/src/main/assets/chart/` (jsDelivr). |
| **LAN inbound pair** | `LanSyncPairingGate`, pending `pending_lan_pair_requests`, UI approve/decline, `pairingCode` on `POST /mmr/v1/pair`. |
| **Backup import (H2)** | `BackupImportGuard`; `importBackup` zip-only; unsigned JSON blocked when payload has `appSettings` or trusted LAN devices. |
| **Zip / HTTP DoS (M2, M3)** | `BackupArchiveLimits` + streaming capped `BackupSignedZipCodec.decode`; manifest payload SHA before attachments; `MmrHttpBodyLimits` + **413** on oversized bodies (lower cap on Web login / LAN pair). |
| **LAN / Web session (M4, partial)** | In-app trusted-network + HTTP warnings; `WebManageSessionManager` **2 h TTL** + session bound to `x-mmr-client-address`; login rate limit unchanged. |
| **Web Manage XSS (M5, partial)** | `app.js`: `escapeAttr`, `el()` helper, approval tiles + merge picker use DOM for user strings. |
| **Tests** | `MmrAttachmentSyncIdTest`, `MmrExternalNavRouteAllowlistTest`, `WebManagePasswordHasherTest`, `LanSyncPairingGateTest`, `BackupImportGuardTest`, `WebManageSessionManagerTest`. |

---

## 5. Full-app static review — open findings

Source: Security Review subagent, full-tree pass (2026-11-04). Items already fixed in section 4 are omitted here.

### 5.1 High

| ID | Location | Finding | Remediation (summary) |
|----|----------|---------|------------------------|
| H1 | `LanSyncRepositoryImpl.kt` (`handlePairRequest`) | **Unauthenticated LAN peer registration** — was: `POST /mmr/v1/pair` auto-trusted peers. | **Mitigated (2026-11-04, uncommitted):** require JSON `pairingCode` matching host PIN (15 min TTL); **pending** request + user approve (`LanSyncDao.approvePairRequest`) with device credential. |
| H2 | `BackupRepositoryImpl.kt` (`importBackup`) | **Unsigned JSON backup import** — no HMAC; malicious file can alter ledger + restore LAN tokens/settings. | **Mitigated (2026-11-04, uncommitted):** `importBackup` is the only import; signed `.mibxbac` zip only. `BackupPayloadSanitizer` drops Web Manage and LAN tokens (keeps non-secret LAN prefs). Restore rotates device credentials and clears LAN trust. |
| H3 | `BackupIntegrity.kt` ~14–17 | **HMAC key embedded in APK** — forgeable `.mibxbac` after decompile (`isMinifyEnabled = false`). | User-held export password or Keystore-derived key; treat current HMAC as checksum only in UI. |

### 5.2 Medium

| ID | Location | Finding | Remediation (summary) |
|----|----------|---------|------------------------|
| M1 | `AppSettingsBackupCoordinator.kt` capture; Drive upload | Backup exports **LAN tokens**, Web Manage hash/salt, SMS, full ledger — high value at rest. | **Mitigated (2026-11-04, uncommitted):** omit Web Manage + LAN identity from export; omit trusted peers; restore rotates device credentials and clears LAN trust (re-pair required). |
| M2 | `BackupSignedZipCodec.kt` | **Zip bomb** — unbounded `readBytes()` before signature verify. | **Mitigated (2026-11-04, uncommitted):** `BackupArchiveLimits`, streaming reads, manifest payload SHA before attachment blobs; manifest declares uncompressed sizes. |
| M3 | `LanSyncHttpServer.kt` | **HTTP body DoS** — unbounded `Content-Length` / chunked bodies. | **Mitigated (2026-11-04, uncommitted):** `MmrHttpBodyLimits` (1 MiB default, 16 KiB login/pair); **413** without reading oversized bodies. |
| M4 | `LanSyncHttpServer.kt`; `WebManageSessionManager.kt` | Cleartext HTTP on `0.0.0.0`; session token sniffable/reusable on LAN. | **Partially mitigated (2026-11-04, uncommitted):** trusted-network copy in Web Manage / LAN settings; **2 h** session TTL; token bound to client IP; login rate limit. **Future:** HTTPS + cert fingerprint (see §12). |
| M5 | `assets/web/app.js` | **Attribute injection / stored XSS** — dynamic values in HTML attributes. | **Mitigated (2026-11-04, uncommitted):** `escapeAttr` + approval tiles via DOM `textContent` / input `.value`; `el()` helper; merge picker uses DOM. **Future:** migrate remaining txn list templates (§12 M5-DOM). |
| M6 | `MainActivity`; `MmrExternalNavRouteAllowlist` | External apps could open **`approval_center?approvalId=&action=`** (allowlisted). | **Mitigated (2026-11-04, uncommitted):** `MmrNavRouteIntentTrust` (`launchedFromUid` + internal flag); untrusted intents drop sensitive query params (no auto widget actions). |

### 5.3 Lower priority / design notes

- **Release minify off (accepted v1):** eases reversing of backup/LAN logic; enable R8 on a later release — see §12 **RELEASE-R8** and **H3**.
- **Room**: parameterized queries — no SQL injection found in review.
- **FileProvider** (`export_file_paths.xml`): cache-scoped paths — OK.
- **SMS receiver**: `BROADCAST_SMS` permission — system senders only.
- **Room DB**: not encrypted at rest (expected unless SQLCipher added).
- **LAN / Web Manage**: HTTP by design on local network.

---

## 6. First diff-only review findings (historical)

These drove section 4 fixes; kept for traceability.

| Severity | Topic | Status |
|----------|--------|--------|
| Medium | `syncId` path traversal on backup restore | **Fixed** (working tree) |
| Medium | Web Manage fixed port 19681, SHA-256, no rate limit | **Fixed** (working tree) |
| Medium | Unbounded attachment base64 on import | **Fixed** (working tree) |
| High | `local.properties` tracked in git | **Fixed** (working tree) |
| Medium | Empty Android backup rules | **Fixed** (working tree) |
| Low | Unrestricted `EXTRA_NAV_ROUTE` | **Mitigated** (allowlist + same-UID trust) |
| Info | APK HMAC | **Open** (see H3) |

---

## 7. Android Lint (full-module SAST)

- **Command:** `./gradlew :app:lintDebug` (included in `./gradlew codeQuality`)
- **Date:** 2026-11-04 (updated after desugaring + notifier fixes)
- **Outcome:** Passes with **`app/lint-baseline.xml`** (no new issues above baseline)
- **Fixes:** `MmrAppNotificationPoster` centralizes POST_NOTIFICATIONS + channel checks; **core library desugaring** (`desugar_jdk_libs`) removes bulk `java.time` **NewApi** noise on minSdk 24
- **Baseline:** Regenerate with `./gradlew :app:updateLintBaseline` — tracks Glance widget **Instantiatable** manifest lint, misc warnings
- **Report:** `app/build/reports/lint-results-debug.html`

### 7.1 Security-relevant Lint errors (sample)

| Rule | Files (examples) | Notes |
|------|------------------|--------|
| `MissingPermission` | `BudgetAlertNotifier.kt:36`, `GoogleDriveBackupNotifier.kt:36`, `ScheduledReminderNotifier.kt:79`, `SmsApprovalSummaryNotifier.kt:72` | POST_NOTIFICATIONS — add runtime check or `@Suppress` with documented guarantee. |
| `Instantiatable` | `AndroidManifest.xml` widget action callbacks | Glance/action receivers vs manifest declaration — verify widget architecture. |
| `PermissionImpliesUnsupportedChromeOsHardware` | `AndroidManifest.xml` READ_SMS / RECEIVE_SMS | Add optional telephony `uses-feature` for ChromeOS. |

### 7.2 Lint noise (not security)

Most of the **507 errors** are **`NewApi` / `java.time`** on `minSdk 24`. The project may rely on **core library desugaring** at runtime; Lint still flags unless `lintOptions` / baseline / `@RequiresApi` patterns are applied. Treat as **quality/compatibility**, not exploitable vulnerabilities, unless confirmed missing desugaring in release builds.

---

## 8. Tools not run (gaps)

| Tool | Reason |
|------|--------|
| **Semgrep** | `pip` blocked by App Control; GitHub release zip 404; `npx semgrep` version not found |
| **Gitleaks** | Not installed |
| **MobSF** | Not run |
| **First Lint attempt** | AGP 9.4 Kotlin UAST crash on `ApprovalRepositoryImpl.kt` |

**Recommendation:** Install [Semgrep standalone binary](https://semgrep.dev/docs/getting-started/) or run Semgrep in CI, then:

```bash
semgrep scan --config p/kotlin --config p/secrets app/src/main
```

Register an [NVD API key](https://nvd.nist.gov/developers/request-an-api-key) for faster Dependency-Check updates.

---

## 9. Recommended priority order

1. ~~**H1** — Authenticate / confirm LAN pairing before trust.~~ **Done (uncommitted)** — see section 4.
2. **H2 / H3** — Signed or passphrase-protected backups only for untrusted imports.
3. **M3** — HTTP body size caps on embedded server.
4. **M5** — Escape dynamic content in Web Manage `app.js`.
5. **M2** — Zip entry size limits on backup import.
6. **M1** — Reduce secrets in backup export payload.
7. **Lint** — Fix `MissingPermission` notifiers; baseline or fix `NewApi` noise separately.

---

## 10. Related project memory

Shorter summary also lives in `PROJECT_MEMORY.md` (Current State → vulnerability scan bullet). **This file is the detailed audit record.**

---

## 11. Document history

| Date | Change |
|------|--------|
| 2026-11-04 | M6: nav route same-UID trust + strip untrusted query params |
| 2026-11-04 | M5 partial: `escapeAttr`, DOM approval tiles, `el()` helper; §12 M5-DOM TODO |
| 2026-11-04 | M4 partial: session IP bind + 2 h TTL; UI warnings; §12 TLS TODO |
| 2026-11-04 | RELEASE-R8: minify off for initial Play; deferred enable + H3 in §12 |
| 2026-11-04 | Initial consolidated audit document |

---

## 12. Future remediation (TODO)

| ID | Topic | Planned work |
|----|--------|----------------|
| **M4-TLS** | LAN HTTP confidentiality | Optional **HTTPS** on `LanSyncHttpServer` with **ephemeral self-signed cert**; show **SHA-256 fingerprint** in Web Manage / LAN settings for one-time browser trust. Cleartext HTTP remains fallback with warning until enabled. |
| **M5-DOM** | Web Manage XSS hardening (follow-up) | Migrate remaining **`app.js`** transaction list/card/table builders from `innerHTML` strings to **`el()`** + `textContent` (same pattern as approvals + merge picker). |
| **RELEASE-R8** | Release **`isMinifyEnabled`** | **Deferred (initial Play):** keep **`false`** on `release`; debug unchanged. **Later release:** `true` + `proguard-rules.pro` smoke test; optional with **H3**. |
| **H3** | Backup HMAC in APK | User export password or Keystore-derived key; honest UI if deferred. |
