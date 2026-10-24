# UI alignment review from screenshot traces

**Trace run reviewed:** `1792809808419` on SM-S938B — 40 UI flow methods with PNGs under `app/build/ui-test-trace/1792809808419/` (pulled mid-run).

**Latest full suite (2026-10-24):** `connectedDebugAndroidTest` — **79 tests, 2 failures** (`LoanRepaymentFlowUiTest` duplicate LazyColumn key; `SettingsNavigationUiTest` LAN sync row text not found). Traces on device are cleared when the test APK is reinstalled; use mid-run `adb pull` or copy to `Download/` if you need post-suite PNGs.

Review method: read each test’s `trace.log` and inspect **post** frames for layout against the settings alignment checklist and main chrome (header, tabs, bottom nav, grouped settings).

## Summary

| Area | Verdict | Notes |
|------|---------|--------|
| Bottom navigation | OK | Outline/filled icons, “Ledger” label, header-tint selected state |
| Home header + summary strip | OK | Income/expenses/total columns align; chips and list rows consistent |
| Home view tabs (Daily/Calendar/Monthly/Total) | OK | Full-width tab indicators under each column; Monthly/Total lists readable |
| Stats screen | OK | Income/expense toggle and empty state centered |
| Settings hub | OK | Grouped sections (GENERAL, BACKUP & SYNC, SMS); chevrons and toggles aligned |
| Accounts / Categories lists | OK | Inset cards, section headers, app-bar add (+) |
| Account / category **inline name** rows | **Fix applied** | Long test input drew over the “Name” label (clip on value column in `MmrIosSettingsInlineFieldRow`) |
| Backup & Google Drive settings | OK | Summary card, action buttons, nav row to Drive screen |
| LAN sync settings | Minor | Grouped layout OK; pairing code row shows long token text (truncation/wrap acceptable) |
| Approval center cards | OK | Amount, actions, metadata columns; quick actions fit on card |
| Loan add (Material form) | OK | Outlined fields; no overlap with labels |
| Add transaction / home ledger | OK | Row amount right-aligned; FAB and bottom nav clear |

## Per-test trace notes

Screens with **successful** post captures through the flow (no `FAILED` in `trace.log`):

- `BottomNavUiTest` (all 3)
- `MainActivityLaunchUiTest`, `MainNavigationUiTest` (nav smoke)
- `AccountAddFlowUiTest`, `CategoryAddFlowUiTest`
- `AddTransactionFlowUiTest`, `HomeSummaryStripUiTest`, `HomeTabsUiTest` (calendar/monthly; total tab pre-export)
- `SettingsNavigationUiTest` (accounts → categories → backup → LAN sync)
- `BackupScreenUiTest` (through backup screen; see gaps below)
- `GoogleDriveBackupNavigationUiTest`, `BackupImportFlowUiTest` (repository paths)
- `ApprovalCenterFlowUiTest`, `ApprovalRejectFlowUiTest`, `ApprovalInboxScanUiTest`
- `LoanCreateFlowUiTest`, `RecurringAddFlowUiTest`, `RecurringSaveFlowUiTest`
- `StatsScreenUiTest`
- `TransactionDetailFlowUiTest#editNote_fromDetail_updatesHomeList` (only method with full captures)

**Capture gaps** (`FAILED` in `trace.log` — not necessarily UI bugs):

| Trigger | Tests affected | Cause |
|---------|----------------|--------|
| System share sheet / no Compose root | `BackupScreenUiTest` (export) | Android shares backup file; activity leaves Compose |
| Modal / dialog / IME | Merge, delete, link, SMS manual, transfer pickers, loan repayment, search period sheet, Excel export, backup import host | `captureToImage` on root failed mid-overlay |
| Post-dialog steps | Several approval/link/merge/detail tests | Same as above |

**Mitigation (2026-10-24):** `MmrUiScreenshotTrace` falls back to `UiAutomation.takeScreenshot()` when Compose root capture fails, so later runs should retain frames over dialogs and share UI.

## Issues to track

1. **Inline settings name field** — fixed: clip value `Box` so end-aligned text cannot paint over the label column.
2. **LAN sync pairing code display** — optional UX: show shortened code + copy action instead of full JSON string in the value column.
3. **Home “Daily” tab indicator** — indicator is full tab width; if design wants underline only under label text, narrow the indicator `Box` to text width.
4. **Trace pull after full suite** — app external dir may be cleared on reinstall; pull during run or copy traces to `Download/` if post-run `pullUiTestScreenshots` is empty.

## How to re-run review

```powershell
$env:ANDROID_SERIAL="<device>"
.\gradlew :app:connectedDebugAndroidTest
.\gradlew :app:pullUiTestScreenshots
```

Open `app/build/ui-test-trace/<runId>/<Class>/<method>/`, follow `trace.log`, compare `*_post_*.png` step by step.
