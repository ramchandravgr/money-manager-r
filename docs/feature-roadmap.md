# Money Inbox — Feature roadmap

Sensible next features grouped by how they fit the current product (offline-first, SMS approvals, LAN sync).  
**Not** a generic finance-app wish list.

**Status legend**

| Mark | Meaning |
|------|---------|
| `[x]` | **Done** — shipped in app (may still need polish/tests) |
| `[~]` | **Partial** — foundation or subset exists |
| `[ ]` | **Not started** |
| `[-]` | **Declined / deferred** — explicit product decision not to build (or postponed) |

*Last reviewed: 2026-10-23 (Ramchandra / student project tracker).*

---

## Already on your roadmap (finish / deepen)

From peer-sync product notes and in-progress LAN work.

### Peer sync UX

| Status | Item | Notes |
|--------|------|--------|
| `[x]` | Pick **one device** vs **sync all trusted peers** | `LanSyncPreferencesStore`, peer picker, **Sync with: {name}**, disabled until peer selected in single-device mode |
| `[x]` | **Restore from device** on empty DB | Per-peer restore buttons, one-way pull, navigate Home + sync log |
| `[-]` | Suggest sync on trusted Wi‑Fi **banner** (tap to sync) | **Removed** — manual **Settings → Peer Device Sync → Sync now** only; no home nudges |
| `[x]` | Wi‑Fi trust on first sync on new SSID | `LanSyncWifiTrustStore` dialog |
| `[x]` | Post-sync status (**Synced with** / **Failed**) | `LanSyncStatusMessages`, `peerSuccesses` |
| `[~]` | Phase 9 hardening (tests, edge cases, Play declarations) | Ongoing; instrumented + unit coverage growing; Play SMS/notification copy still TBD |

### Sync conflicts & merge

| Status | Item | Notes |
|--------|------|--------|
| `[x]` | **SMS scan months** + **accounts/categories** on sync (no prompt) | Auto **merge-by-name** for duplicate seeds; same `syncId` → newer wins (categories always refresh); SMS window → **max** of local/remote months; Transaction Appearance stays device-local |
| `[~]` | **Recurring** per-rule mine / theirs | Only remaining **sync merge** dialog; bulk mine/theirs (not per-rule list yet) |
| `[x]` | **LAN delete tombstones** (auto-apply, no prompt) | Room `sync_tombstones`; payload v3 `tombstones[]`; txn/recurring/loan/payment/account/category/subcategory/attachment; delete vs edit via `deletedAt` vs `updatedAt` |
| `[x]` | **Duplicate account/category** without sync dialog | Default `MERGE_BY_NAME` in `LanSyncMerger` |
| `[x]` | Balance warnings after merge | `LanSyncAccountBalanceValidator` → sync log |

### Sync provenance & catalog

| Status | Item | Notes |
|--------|------|--------|
| `[x]` | Small **“From [device]”** on transaction detail | `syncedFromDeviceName` / id on `TransactionEntity` |
| `[~]` | **Categorization rules** — full UI to edit user rules | Engine + JSON seeder + LAN/backup sync; **no** dedicated Settings screen for CRUD |
| `[x]` | LAN payload v2 (loans, recurring, rules, notifications, attachments metadata) | `LanSyncExporter` / `LanSyncMerger` |

---

## SMS & approvals

| Status | Item | Notes |
|--------|------|--------|
| `[x]` | **Live inbound** notify toggle | `IncomingSmsReceiver` → `ProcessIncomingSmsWorker` → summary notification |
| `[x]` | Tie notify toggle to **READ_SMS** (optional, once) | Prompt when enabling live SMS; `read_sms_inbound_prompt_shown` |
| `[x]` | **POST_NOTIFICATIONS** when enabling (API 33+) | Settings toggle flow |
| `[ ]` | Notification **actions**: Approve / Open only | Today: tap opens Approval Center; no action buttons |
| `[~]` | **reconcilePendingWithCurrentParser()** | Manual from Approval Center; **no** daily WorkManager job yet |
| `[-]` | Backfill disbursal txn for old loans | One-time tool — deferred |
| `[~]` | **SMS template overlay** | Import + Settings screen; **no** export overlay / match-rate debug / AI drafting |
| `[x]` | Inbox scan + parser phases 1–3 | Templates, classifier, approval metadata |
| `[x]` | Account/card mapping on approve | `SmsAccountResolver` (credit last4, wallets, bank) |

---

## Web Manage & desktop

| Status | Item | Notes |
|--------|------|--------|
| `[x]` | LAN browser UI (approvals, transactions, spending charts, Excel export) | `assets/web/`, `WebManageRequestHandler` |
| `[x]` | Mutual exclusion with peer sync + FGS | `LocalLanServerCoordinator` |
| `[ ]` | **HTTPS** / pinned cert on LAN (or stronger “trusted network only” docs) | Plain HTTP today |
| `[ ]` | Web: **budgets** widget, approval quick-actions parity, **dark theme** for web assets |
| `[ ]` | **Scheduled Excel** to Downloads (WorkManager) | Manual export from Home / Web exists |

---

## Ledger & insights

| Status | Item | Notes |
|--------|------|--------|
| `[x]` | **Stats** periods + category pies / drill-down | `StatsViewModel`, Web spending tab |
| `[ ]` | **Budgets / envelopes** per category | |
| `[ ]` | **Tags** on transactions (multi-label search) | |
| `[ ]` | **Split transactions** (one SMS → multiple categories) | |
| `[x]` | **Attachments** (receipt photos) + optional **LAN sync** flag | Room v15, `Include attachments` in Peer Device Sync |
| `[ ]` | **Reconciliation mode** (cleared vs statement balance) | Concept only; not in app |
| `[~]` | **Dark mode** | Palettes reserved; **no** user toggle |

---

## Loans & recurring

| Status | Item | Notes |
|--------|------|--------|
| `[x]` | Loans (EMI, flexible, top-ups, schedules) | See `PROJECT_MEMORY.md` § Loans |
| `[x]` | Recurring rules + auto-post coordinator | |
| `[~]` | **Loan reminders** via `notification_rules` | DB + LAN sync; **no** feature UI / scheduler surfaced |
| `[ ]` | Recurring: edit **single occurrence** vs series; skip + notification | |
| `[~]` | Credit card: **CARD_PAYMENT** informational SMS | Parser path; **no** statement cycle / due-date product UI |

---

## Accounts & home

| Status | Item | Notes |
|--------|------|--------|
| `[x]` | **Home screen widgets** (Glance) | Pending approval count; month spend/income/net to date |
| `[x]` | Configurable **start screen tab** | Transaction Appearance → start tab; live switch on Home |
| `[~]` | Home **filters persist** | Month/type/category in session; not full “saved filter presets” |
| `[~]` | **Multi-currency** display | Main/sub currency in settings + LAN snapshot; **no** FX rates / conversion ledger |

---

## Safety & data

| Status | Item | Notes |
|--------|------|--------|
| `[x]` | **Biometric / device credential** for LAN trust & remove peer | `MmrDeviceCredentialGate` |
| `[ ]` | **App lock** (biometric) on launch | |
| `[ ]` | **Encrypted backup** (password on export file) | JSON backup unencrypted |
| `[ ]` | **Auto-backup** to user-chosen folder on schedule | |
| `[x]` | Manual backup & restore | `BackupScreen`, `BackupJsonCodec` |

---

## Bigger bets (only if product direction changes)

| Status | Item | Notes |
|--------|------|--------|
| `[x]` | **Cloud backup** (Google Drive, opt-in) | `MoneyInbox/Backups`, 6-file rotation, manual + auto (daily/weekly, Wi‑Fi); SIWG/Credential Manager polish later |
| `[ ]` | **Wear** / quick approve pending SMS | |
| `[ ]` | **Import** from other apps (CSV/OFX) | |

---

## High leverage for a student project (offline story stays clear)

Priority stack aligned with what’s already built:

1. `[~]` Finish **peer sync** conflict + **delete/tombstones** + restore polish  
2. `[~]` **Loan/recurring local notifications** (reuse SMS summary notification pattern)  
3. `[ ]` **Budgets** tied to Stats categories  
4. `[~]` **Categorization rules** UI  
5. `[x]` Polish **live SMS** (READ_SMS on enable); `[ ]` optional **approve-from-notification**

---

## Related docs

- Canonical architecture & current state: [`PROJECT_MEMORY.md`](../PROJECT_MEMORY.md) (§5 Development Roadmap, §6 decisions, §7 current state).  
- Test matrix: [`docs/test-coverage-matrix.md`](test-coverage-matrix.md).
