# Money Manager R (MMR) — Project Memory & Architecture Tracker

## 1. Project Overview & Vision

**Money Manager R (MMR)** is an **offline-first, privacy-centric personal finance tracking Android application** built entirely with modern Android technologies (Kotlin, Jetpack Compose, Hilt, Room, and Navigation).

### Core Principles
- **100% Offline & Local**: No external backend servers or third-party cloud finance APIs. All user financial data is kept securely on-device.
- **Rule-Based SMS Parser with Explicit Approval**: Scans bank and merchant transaction SMS messages locally, normalizes them, detects duplicates via cryptographic hashing, and routes them to a **Transaction Approval Center** where users can review, edit, approve, or reject entries before saving.
- **Robust Financial Ledger**: Supports standard expenses, income, inter-account transfers (from account → account or loan), and loan payment linkages.
- **Rich Analytics & Loan Tracking**: Local aggregation for category-wise breakdowns, spending trends, loan repayment schedules, and outstanding balances.

---

## 2. Key References & Documentation

All architectural decisions, schemas, and UI designs are grounded in specifications located in `D:\Development\Docs`:

| Document | Purpose |
| :--- | :--- |
| `Android_Finance_Tracking_App.md` | Core product scope (V1 feature set, offline constraints, approval pipeline). |
| `MMR_Step_by_Step_MVP_Implementation_Plan.doc` | 10-phase development roadmap (Phase 0 to Phase 9). |
| `MMR_SQLite_Room_Schema_Bundle.rtf` | Complete database blueprint: tables, Room entities, DAOs, relationships, foreign keys, and type converters. |
| `MMR_Folder_Structure_For_Each_Module.rtf` | Source code tree guidelines and layered package conventions. |
| `Full Compose file tree for all screens.txt` | Target module/file tree across all future screens and features. |
| `MMR UI Layout Specification` & `Wireframes.html` | Screen-by-screen UX specifications and component behaviors. |

---

## 3. Tech Stack & Build Toolchain

- **Language**: Kotlin 2.2.10
- **UI Toolkit**: Jetpack Compose with Material 3, Material Icons Extended, Compose Navigation
- **Architecture**: MVI / MVVM with Clean Architecture principles (Presentation → Domain → Data → Database)
- **Dependency Injection**: Dagger Hilt 2.60.1
- **Local Persistence**: Room SQLite 2.7.1 with KSP (Kotlin Symbol Processing); **schema version 10** (loans, categorization rules, app display settings, loan EMI input mode, etc.)
- **Asynchronous Execution**: Kotlin Coroutines & Reactive Flows
- **Build System**: Gradle with Android Gradle Plugin (AGP) 9.4.1; JDK via `gradle.properties` `org.gradle.java.home` when needed
- **Static analysis (2026-10)**: [ktlint](https://github.com/ktlint/ktlint) 1.5.0 + [detekt](https://github.com/detekt/detekt) 1.23.8 on `:app` (Compose rules `io.nlopez.compose.rules:detekt`). Config: `config/detekt/detekt.yml`, `.editorconfig`. Baselines: `config/detekt/baseline.xml`, `config/ktlint/baseline.xml` (legacy debt; shrink over time). Root task `./gradlew codeQuality` → `:app:ktlintCheck` + `:app:detekt`. **AGP note**: `android.disallowKotlinSourceSets=false` — ktlint-gradle only lints `.kts` unless `:app:ktlintCheckSources` (CLI on `app/src/`). Regenerate baselines: `:app:detektBaseline`, `:app:ktlintGenerateSrcBaseline`.

---

## 4. Architecture & Package Structure

```
com.mmr/
├── app/
│   ├── MmrApplication.kt
│   ├── MainActivity.kt               # enableEdgeToEdge(); MoneyManagerRTheme → MmrNavHost
│   ├── di/                           # AppModule, DatabaseModule, RepositoryModule
│   ├── ui/theme/                     # System sans-serif (MmrFontFamily), Material 3 typography
│   ├── ui/state/                     # MmrUiMessages, MmrStateViews
│   └── navigation/
│       ├── Screen.kt                 # home, stats, loans, recurring, settings, transaction_search, approval_center, categories, …
│       ├── MmrMainDestinations.kt    # Bottom tabs (no Approvals tab)
│       ├── MmrScrollableBottomBar.kt # NavigationBar + icons (Trans., Stats, Loans, Recurring, Settings)
│       └── MmrNavHost.kt             # Scaffold + bottom bar on main tabs; safeDrawing insets
├── core/
│   ├── common/validation/            # MmrValidation
│   ├── backup/
│   ├── model/enums/
│   ├── database/                     # MmrDatabase v10 — ledger, loans, schedules, payments, categorization rules, SMS records
│   ├── loan/                         # Amortization, schedules, flexible interest, top-ups, ledger coordination
│   ├── categorization/             # Rule engine + seeded rules from assets
│   ├── parser/scanner/
│   │   └── LocalSmsScannerService.kt # scanInbox(monthsBack) — date >= now−N months, no message count cap
│   ├── data/preferences/
│   │   └── SmsScanSettingsStore.kt   # SharedPreferences: SMS scan months (default 3, range 1–36)
│   └── domain/ + data/repository/    # Transaction, Category, Approval, Account, Loan, …
└── feature/
    ├── home/                         # Month picker, view tabs (Daily/Calendar/Monthly/Total), summary strip, grouping utils
    ├── search/                       # TransactionSearchScreen — note + period + account + category + amount filters
    ├── transaction/                  # Add/Edit; category picker with inline add/rename; transfer From/To; account picker sheets
    ├── category/                     # List + add/edit category & subcategory (Settings)
    ├── approval/                     # Approval Center; Scan SMS → scanInbox(monthsBack from settings)
    ├── stats/, loan/, recurring/, settings/, backup/
```

---

## 5. Development Roadmap & Status

| Milestone | Status | Notes |
| :--- | :---: | :--- |
| Phases 0–8 | **Completed** | Per original MVP plan (ledger, approval, stats, loans, recurring, backup). |
| **Recurring add/edit UI** | **Landed** | `AddEditRecurringRuleScreen` mirrors transaction form: MM header, expense/income segments, shared amount/date/note fields, picker sheets for category/account/reminder. |
| **Phase 9: Hardening & Testing** | **In progress** | Guards, MmrUiMessages, repository tests, ViewModel fakes. |
| **Category & subcategory CRUD** | **Completed** | Room v3, `CategoryRepository`, Settings → Manage categories, hierarchy UI. |
| **Home shell (reference layout)** | **Completed** | Center month title + picker dialog; tabs; Income/Expense/Total strip; grouped dates as `day, weekday, month, year` (not Today/Yesterday). |
| **Navigation shell** | **Completed** | Bottom bar with icons; Approvals via **mail icon + badge** on Home top bar (not a tab). |
| **Transaction search** | **Completed** | Full-screen Search from Home 🔍; notes-only text filter; date range; account; read-only category/subcategory; amount min/max. |
| **Add transaction UX** | **Completed** | Category row + two-column picker (parent/sub, long-press rename, + add cat/sub); Account as form row + sheet; Transfer **From** / **To** (accounts + active loans). |
| **Launcher icon** | **Completed** | Adaptive icon vectors (deep blue + M mark); legacy webp may need Image Asset refresh on API 24–25. |
| **Accounts (manual)** | **Completed** | LOAN liability accounts; linked on loan create; chips on add/edit loan. |
| **Loans — fixed EMI** | **Completed** | Calendar-month EMI dates, actual/365 interest in schedule, disbursal `openingDate`, tenure↔EMI entry mode (`fixedEmiInputMode`), part/full repayment dialog, schedule refresh from disbursal or last payment. |
| **Loans — top-ups** | **Completed** | `LoanTopUpProcessor`, stepped principal accrual after top-up, liability account balance increase. |
| **Loans — flexible repayment** | **Completed** | Actual/365 accrued interest from disbursal or last payment/top-up; payments clear interest then principal (`LoanFlexibleInterest`); pending interest on loan detail + repayment dialog hint. |
| **Categorization rules** | **In progress / landed** | DB + `categorization_rules.json` seeder, `CategorizationRuleEngine`. |
| **Backup import modes** | **Landed** | `BackupImportPlanner`, app settings in backup payload. |
| **Wi‑Fi LAN sync (v1)** | **Landed (initial)** | Settings → **Wi‑Fi sync**; opt-in; trusted devices + pending pair approval; manual **Sync now** on same Wi‑Fi; approved transactions + referenced cats/accounts/loans + SMS scan + display settings; no SMS/approval queue. Room **v11** `syncId` on ledger entities; `core/lansync/*` + NSD + local HTTP. |

---

## 6. Key Implementation Decisions (Recent)

### Navigation & Home
- **Main tabs**: Transactions (home), Stats, Loans, Recurring, Settings — `MmrMainDestinations`, `NavigationBar` with `WindowInsets` for system nav bar.
- **Approvals**: Route `approval_center`; opened from Home top-right mail icon; `getPendingCount()` badge.
- **Home top bar**: `CenterAlignedTopAppBar` — brand icon left, centered month ‹ ›, Search + Mail actions (equal-size icon buttons).
- **Home month picker**: `HomeMonthPickerDialog` — year arrows, 3×4 months, THIS MONTH shortcut.
- **Home data**: Month-scoped totals via `getTotalIncomeBetween` / `getTotalExpenseBetween` for the income/expense strip on all tabs (including **Total**).
- **Home Total Excel export**: `PeriodExcelWriter` / `PeriodExcelExporter` (fastexcel, MM `ExcelExportBuilder` columns); **period picker** `HomeExcelExportPeriodDialog` + `HomeExcelExportPeriods` (MM `ExcelExportDialog`: Monthly / Last month / Annually / Last year / Total) anchored to Home month + `monthStartDay`; writes `cache/exports/*.xlsx`; share via `MmrExportFileProvider` + `ACTION_SEND` (OOXML); loading overlay while building file.
- **Home Total tab**: `HomeTotalTabContent` + `HomeTotalUtils` — MM-style **Accounts** summary: **Compared Expenses (Last month)** % = `round((curr−prev)×100/prev)+100` per `BudgetService.m39078c` (`mmComparedExpensePercent`; when previous month spend is 0, delta term is 0 → **100%**); **Cash Expenses** / **Card Expenses** / **Transfer-Expense** (`main_summary_view_text3–5`) with contributing **account names** in parentheses (`Transfer-Expense (Bank → )`); card row hidden when ₹0; transfer-expense = outbound transfer from cash-like accounts to non-internal destinations or loan; `HomeTotalUtilsTest`.

### Transactions & categories
- **Transfer persistence**: `accountId` = source; `relatedAccountId` = destination account OR `loanId` = destination loan (mutually exclusive on To).
- **App display settings** (`AppDisplaySettingsStore`): month/week start, swipe, amount colors, show description, time show + within-day sort (asc/desc), start screen tab (live switch on Home), balance carry-over (prior net in period Total), note autocomplete on add/edit transaction. `inputOrder` persisted but not wired to form layout yet.
- **Category picker sheet**: `CategoryPickerComponents` — light header, left-aligned rows, accent bar on selected parent, inline “+ Add” links (transaction + search sheets).
- **Shared dialogs**: `MmrInputDialog`, `MmrContentDialog`, `MmrConfirmDialog` in `app/ui/components/MmrInputDialog.kt` — rounded surface, outlined fields; used for category name entry, confirmations, manual SMS, settings pickers, etc.
- **Home / stats cash flow**: `TransactionCashFlow` — loan repayments are `TRANSFER` in DB but count toward **Expenses** (borrowed) or **Income** (lent); top-ups excluded. DAO `getTotalIncome/Expense*` joins `loans` accordingly; day headers use the same rules.
- **Category picker in Add/Edit**: Creates/updates via `CategoryRepository` (syncs with Settings categories); search picker is **select-only** (no inline create).
- **Account UI**: `TransactionFormRow` + unified **`AccountPickerSheet`** — searchable, ~92% height bottom sheet, accounts grouped by `AccountType` (Cash, Bank, Debit, Credit, Wallet, Other; loan **liability** accounts excluded from spend/from pickers). **`AccountPickerMode.TransferTo`** adds a **Loans** section (repayment targets). Used on add/edit transaction, recurring, and search filters. `TransferToPickerSheet` removed.
- **Typography** (`Type.kt`, `AppFont.kt`): `FontFamily.SansSerif` (Roboto on stock Android), reduced scale, **normal** weights app-wide (minimal MM density).
- **Form app bars**: back **arrow** (no Cancel text); **Save** / **Delete** via `MmrAppBarSaveIcon` / `MmrAppBarDeleteIcon` on add/edit flows; indigo `MmrTopAppBar` uses compact title by default.
- **Action buttons**: `MmrShapes` (10dp corners, not pill) in theme; `MmrPrimaryActionButton` / `MmrSecondaryActionButton` for full-width Save / outlined actions (loan create, recurring save, backup, dialogs, export, etc.).
- **Manual add transaction**: `TransactionDateTimeField` is **date-only** (no time picker); new saves use selected date + **current local time** at save (`manualEntryDateTimeMillis`).
- **Transaction detail** (`TransactionDetailScreen`): MM header with **Edit**, **Duplicate**, **Delete** icon actions; delete uses confirm + loan unlink/reverse when applicable; duplicate asks **Same date** vs **Today** then opens `AddTransaction` with `duplicateFromId` + `dateMillis` (prefills add form, new row on save). Nav: `Screen.AddTransaction.createDuplicateRoute`.

### Wi‑Fi LAN sync (2026-03)
- **Entry**: Settings → **Wi‑Fi sync** (`Screen.lan_sync`, `LanSyncScreen` / `LanSyncViewModel`).
- **Opt-in**: `LanSyncIdentityStore`; when enabled, registers NSD `_mmr-sync._tcp` and local HTTP server (`LanSyncHttpServer`, port `19681` default).
- **Trust**: Share **pairing code** (`LanSyncPairingCodec`); paste on other device → **Add trusted device**; remote can POST `/mmr/v1/pair` → **Approve pairing** on this device. Sync requires row in `trusted_lan_devices` (`theirAuthToken` verified on `/mmr/v1/sync`).
- **Payload**: `MmrLanSyncPayload` / `LanSyncJsonCodec` — only **APPROVED** transactions (`LanSyncExporter`); merge by **`syncId`** + `updatedAt` (`LanSyncMerger`). SMS inbox and approval queue never exported.
- **Identity**: `syncId` (UUID) on `CategoryEntity`, `SubcategoryEntity`, `AccountEntity`, `LoanEntity`, `TransactionEntity`; included in file backup JSON; assigned on new inserts via `MmrSyncId`.
- **Caveats**: Plain HTTP on LAN (no TLS next milestone); guest Wi‑Fi / AP isolation may block peer discovery; both devices must trust each other for bidirectional sync (pair + approve on each side).
- **Product decisions (Q&A, Ramchandra)**: **Local Wi‑Fi only** for now (no internet P2P/cloud path yet). **Device display name**: editable **only** in Wi‑Fi sync settings (default phone model until changed). Pairing: **copy/paste code** primary; **paste = trust** — no separate **Approve pairing** step (two-way still needs each device to add the other via paste/code; incoming pair may auto-trust when code validates). Sync: **all trusted peers on LAN by default** + optional pick-one-device (to implement); **manual Sync now** primary; optional setting **suggest sync** on trusted Wi‑Fi (banner/button — still requires tap, no background auto-merge). Conflicts: **newer `updatedAt` wins** for transactions/loans/accounts (including **transfers**, no special case); **account `currentBalance`**: newer wins, but sync log **warns** if balance doesn’t match recomputed-from-transactions on this device; **recurring**: **prompt** on conflict — **one choice per rule** (use mine / use theirs for whole rule). Scope to add: **recurring rules** + **all loans** (full state); **full category tree + all accounts** on device (not minimal closure); **all categorization rules** (SMS auto-category DB, incl. user edits); **all notification rules** (loan/recurring reminders). Settings: **prompt when Configuration differs** on sync — resolve **per section** (e.g. periods vs **display & currency** vs SMS scan), not all-or-nothing; **main/sub currency** grouped with display in conflict UI. Deletes: **prompt on remote delete** (tombstones TBD); choices **delete here / keep** + optional **remember default** for future deletes. Trusted device **cap: none**; **one-sided revoke** blocks peer on this device. Multi-peer sync: **partial success** OK; peer **order arbitrary** (merge order doesn’t matter). Post-sync UX: **summary + sync log** (keep last **10** runs); **Clear history** in Wi‑Fi sync **Advanced**. **Sync provenance**: minimal text on **transaction detail** only (“from [device]”), very small type / unobtrusive placement — not in home lists. **Attachments**: optional **“Include attachments”** per sync (default **off**); ledger always syncs. **Transaction `dateTime`**: sync **raw epoch millis**; display uses local locale/TZ. **Pending approvals**: **Sync now** allowed; show **one-line reminder** that pending SMS/approvals are not synced. **Device credential** required for **pairing** and **removing** trusted device, not for each sync. **Wi‑Fi trust**: warn on first sync on a new SSID; remember per-network “I trust this Wi‑Fi”. Wiped/reinstalled device **restores from peer**, not global delete. **Duplicate seeds** (e.g. two “Cash” accounts, different `syncId`, same name): **ask once per entity type** during sync (“Merge duplicate …?”). **Empty device**: **Restore from device** (one-way pull, pick peer) **and** **Sync now** on main Wi‑Fi sync UI. **Non-empty device**: **Restore** only under **Advanced** (not main screen); normal use is **Sync now** + merge rules.

### SMS import
- **Scan window**: `LocalSmsScannerService.scanInbox(monthsBack)` filters inbox with `date >= since` (no SQL `LIMIT`). Months from **`SmsScanSettingsStore`** (Settings → **SMS approvals** slider, default **3**, range **1–36**).
- **Permission**: `READ_SMS` in manifest; Approval Center requests runtime permission before scan.
- Parsing + duplicate hash still filter what becomes pending approvals.
- **Parser phase 1**: role-based amount extraction (ignore avl bal / credit limit), expanded exclusions (mandate, telecom data, failed payments, SIP), VPA/BOB card merchants, broader sender list (`AmountRule`, `SmsClassifier`, `MerchantRule`, `TypeAndSenderRules`).
- **Parser phase 2**: `assets/sms_templates.json` + `SmsTemplateRegistry` (sender suffix + regex, priority); `SmsParserEngine` tries templates before generic rules; `ParsedSmsCandidate` adds `templateId`, `paymentChannel`, `instrument` (Hilt-injected into `LocalSmsScannerService`).
- **Parser phase 3**: `SmsParseResult` (Transaction / Informational / Ignored); mandates, statements, SIP notices counted in `SmsInboxScanSummary`; scan message reports skipped notices; Approval cards show **Parse source** + confidence; Room v4 stores `extractedTemplateId`, `extractedPaymentChannel`, `extractedInstrument` on approvals.
- **Product rules (SMS)**: `SmsAccountResolver` auto-creates/maps CC by `cardLastFour`, Pluxee + HP Pay wallets; `SmsCrossChannelDedup` merges HP Pay+card duplicate SMS (card wins); IPO block → informational, IPO debit + SIP success → transactions; cashback bank credit → low-confidence INCOME; coins ignored. Accounts Room v5 adds `cardLastFour`.

### Shared UI / data helpers
- `TransactionGrouping.groupByDate()` — shared by Home and Search results.
- `HomePeriodUtils` — month ranges, group date labels, short dates for search period display.
- **Home multi-select** (`HomeScreen`, `HomeViewModel`): long-press enters selection on Daily/Total lists; indigo top bar with **Select all** / **Delete** / **Merge** icons (no bottom Cancel bar); bulk delete with confirm dialog; merge when ≥2 selected; selection rows match compact list styling.
- **Stats pie chart**: `Other` slice covers expense/income counted in tab totals but not in category SQL (e.g. loan-linked transfers); pie normalizes if slices still &lt; 100%.
- **Stats period UI**: Period mode uses separate tappable **start** and **end** dates in `MmrMmHeaderBar`; category detail has the same period chip + range pickers (search removed from category drill-down).
- **Stats MM parity (2026-10)** — `MmrMmStatsTabRow` (43dp / 13sp / Income & Expenses totals). Main: `HorizontalPager` ledger tabs, dual breakdown preload, **WebView + Highcharts** pie **300dp** (`StatsWebPieChart` → `assets/chart/pie.html`, bundled `highcharts.js` + jQuery) in `LazyColumn` header (`StatsUiComponents`), MM list rows + ad placeholder, pull-to-refresh, title month picker, `MmrPeriodSwipeBox`. `StatsPeriodUtils.ANNUALLY` → `HomeExcelExportPeriods.calendarYearDateRange`. Detail: total block, MM subcategory rows, **WebView** `StatsWebLineChart` **180dp** (`assets/chart/line.html`), bottom category picker (nav + bulk recategorize), long-press multi-select delete. `StatsChartJsonBuilder` builds MM-shaped `drawChart(...)` JSON; `MmrHighchartsWebView` loads `file:///android_asset/…`, `JavascriptInterface androidActivity`, fixed Compose height (avoids pager 0×0). Compose fallbacks: `StatsPieChart`, `StatsCategoryLineChart`.
- **Nested scaffold insets** — `MmrNestedScaffoldInsets` on main-tab inner scaffolds; outer `MmrMainNavigationScaffold` uses **zero** `contentWindowInsets` (status bar handled once in MM chrome via `statusBarsPadding`, avoids double top gap on Home); compact bottom nav `52dp` + `navigationBars` padding (`MmrScrollableBottomBar`).
- **Home transaction list** (`HomeTransactionListComponents`, MM `item_day_adapter`): **8dp** day gap; header **34dp** min / **15×5dp** pad / **0.75dp** rule; rows **50dp** min / **15dp** horizontal / **80dp** category / **3dp** gutter; **0.5dp** `#D5D5D5` full-width row dividers; type **19/14/12/10sp** (day / title+amount / category / meta); dimens in `MmrDimens.transaction*`.
- **Home Calendar tab** (`HomeCalendarUtils`, `HomeCalendarTabContent`): MM-style month grid (weekday header, padded cells, expense totals, adjacent-month gray cells); tap date → day list with prev/next + **Add**; FAB/add flow passes `dateMillis` to `AddTransaction`.
- **Home Monthly tab** (`HomeMonthlyUtils`, `HomeMonthlyTabContent`): 12-month accordion with income/expense/net; expand month → weekly rows (current week highlighted); respects filters + custom month start / week start.

### Approval & backup
- **Approval promotion is atomic** via `ApprovalLedgerDao` `@Transaction` blocks.
- **Backup restore order**: FK-safe table order documented in prior phases.
- **Backup export** (`BackupDao.getSmsMessageRecordsForExport` / `getTransactionApprovalsForExport`): rows with `approvalState == PENDING` are omitted from export and from the on-screen backup summary; import format unchanged.

### Loans (`core/loan/`, `feature/loan/`)

- **Disbursal**: Stored as `LoanEntity.openingDate`; interest accrual and schedule anchors use `disbursalDateMillis()` extension.
- **Fixed EMI schedule**: `LoanAmortizationEngine.buildPlannedSchedule` — actual/365 days between disbursal (or anchor) and each due date; EMI due dates advance by calendar month (`advanceEmiDueOneMonth`). `LoanRepositoryImpl.refreshPlannedEmiSchedule` / `emiScheduleAnchorMillis` rebuild from disbursal or last payment.
- **EMI validation**: `computeTenureMonths` returns 0 if EMI ≤ monthly interest; minimum EMI ≈ monthly interest + ₹1 (`MIN_PRINCIPAL_ABOVE_INTEREST_PER_EMI`).
- **Add/edit UX**: Principal → rate → disbursal → tenure/EMI block; `FixedEmiEntryMode` persisted in DB v10.
- **Repayment**: `LoanPaymentProcessor` + `LoanRepaymentDialog` (Part/Full, payment date, fixed-EMI part-payment tenure/EMI adjustment).
- **Top-up**: `LoanPaymentType.TOP_UP`, `LoanTopUpProcessor`, UI via loan detail menu; `PrincipalStepAccrual` for stepped interest after top-up.
- **Flexible repayment**: No planned EMI schedule required. Accrual period starts at disbursal or `max(paymentDate)` over all payments (including top-ups). `LoanFlexibleInterest.pendingInterest` / `splitPayment` use `interestForActualDays`. UI: `LoanDetailUiState.flexibleInterestPendingFormatted`, `flexibleRepaymentAccruedInterestHint` in repayment dialog.
- **Repayment history cards**: `PaymentHistoryCard` shows principal vs interest split when `LoanPaymentEntity` stores `principalComponent` / `interestComponent` (all new repayments via `LoanPaymentProcessor`).
- **Loan delete**: Deactivates linked LOAN account when safe (no other loan link, no transactions).
- **Tests**: `LoanSchedulePlannerTest`, `LoanFlexibleInterestTest`; `:app:compileDebugKotlin` is the reliable compile check.

### Testing
- JVM: repository and loan planner tests; `HomeViewModelTest` / `StatsViewModelTest` may need `displaySettingsStore` in constructor (known breakage when running full `testDebugUnitTest`).
- Prefer `:app:compileDebugKotlin` after feature batches; run targeted `--tests` when full suite is red.
- **Device UI harness (2026-10)**: `HiltTestRunner` + `hilt-android-testing`; `TestDatabaseModule` (`@TestInstallIn` replaces `DatabaseModule` with in-memory Room; must mirror all DAO `@Provides` from `DatabaseModule`, e.g. `LanSyncDao`); `MainActivityLaunchUiTest` uses `createAndroidComposeRule<MainActivity>`; shared tags in `app/testing/MmrTestTags.kt` (bottom nav `nav.tab.<route>`, `app_bar.save`). Run: `./gradlew :app:connectedDebugAndroidTest` (device/emulator).
- **Repository instrumented tests**: `MmrInstrumentedRepositoryGraph` wires `TransactionRepositoryImpl`, `ApprovalRepositoryImpl`, `LoanRepositoryImpl` (+ coordinators) on in-memory Room; loan/approval setups seed a default **Cash** or **Bank** account for ledger/approve paths. `:app:compileDebugAndroidTestKotlin` succeeds.
- **UI E2E (2026-10)**: Each Compose UI test class extending `MmrHiltUiTest` must declare `@HiltAndroidTest` (not inherited). **androidTest** uses **Espresso 3.7.0** / **androidx.test.ext:junit 1.2.1** so Compose idle sync works on Android 16+ (API 36/37). Wide layouts use **navigation rail** with the same `nav.tab.*` tags as the bottom bar. `TestDatabaseModule` seeds default catalog + Cash/Bank accounts; `MmrUiTestFixtures` (`seedHomeSummarySample`, `seedSearchTarget`, `seedPendingApproval`). UI tests: `AddTransactionFlowUiTest`, `TransactionSearchFlowUiTest`, `HomeSummaryStripUiTest`, `ApprovalCenterFlowUiTest` (mail icon → quick approve → Home). Tags: `home.top_bar.approvals`, `approval.card.<id>`, `approval.<id>.approve`, plus search/summary/transaction tags. **CI**: `.github/workflows/android-instrumented.yml` runs `connectedDebugAndroidTest` on API 30 emulator (excludes `ApprovalInboxScanUiTest`). **Local phone**: run full suite including SMS scan with device attached.
- **Git hooks**: `.githooks/pre-commit` runs `:app:ktlintCheck`, `:app:detekt`, then compiles `:app:compileDebugKotlin` + `:app:compileDebugAndroidTestKotlin` when staged files touch Kotlin/Gradle (no device). Install: `scripts/install-git-hooks.ps1` or `.sh` → `git config core.hooksPath .githooks` (local). Skip: `git commit --no-verify` or `SKIP_HOOK=1`.
- **Test matrix**: `docs/test-coverage-matrix.md`. Stricter UI asserts via `MmrUiTestFormat` / `assertTransactionRowShowsExpense` on `transaction.row.<id>`; search uses `search.header.period_chip`, Year period, collapsed filters, then row + amount.
- **Approval inbox scan UI (physical device)**: `ApprovalInboxScanUiTest` scans the **real** inbox over USB (grants `READ_SMS` via `UiAutomation`; clears pending approvals before scan). Expects parseable bank SMS within the app scan-month setting — fails if banner is “No transaction SMS found”. **Not run on CI emulator** (`notClass=…ApprovalInboxScanUiTest` in `android-instrumented.yml`). Optional emulator seeding only: `SmsInboxTestHelper` (not used by the main scan test).
- **Backup import UI**: `BackupImportFlowUiTest` (repository → Home + backup summary); `BackupImportHostUiTest` on debug `BackupScreenHostActivity` + `BackupScreenHostTestBridge` (replace/merge dialog, no SAF). Tags: `backup.import.mode.replace|merge`, `backupImportConfirm`.

---

## 7. Current State Verification

- **Build**: `:app:compileDebugKotlin` and `./gradlew codeQuality` succeed; full unit test suite may fail on unrelated ViewModel test constructors.
- **Room version**: **10** (`fixedEmiInputMode`, categorization rules, loan schedule entities, etc.); destructive migration on bump if not migrated.
- **SMS**: Scan depth controlled by Settings months preference (not message count).
- **Agent memory**: Cursor rule `.cursor/rules/project-memory.mdc` — agents should update this file at the end of each substantive prompt.

---

## 8. Maintenance Note

**Agents must keep this file current.** See `.cursor/rules/project-memory.mdc` (`alwaysApply: true`): at the end of each user request with project changes, revise §5–§7 (especially **Loans** and **Current State**). Humans can edit anytime; do not commit unless the user asks.
