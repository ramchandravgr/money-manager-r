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
- **UI Toolkit**: Jetpack Compose with Material 3, Material Icons Extended, Google Fonts (Inter via `ui-text-google-fonts`), Compose Navigation
- **Architecture**: MVI / MVVM with Clean Architecture principles (Presentation → Domain → Data → Database)
- **Dependency Injection**: Dagger Hilt 2.60.1
- **Local Persistence**: Room SQLite 2.7.1 with KSP (Kotlin Symbol Processing); **schema version 3** (category/subcategory persistence)
- **Asynchronous Execution**: Kotlin Coroutines & Reactive Flows
- **Build System**: Gradle with Android Gradle Plugin (AGP) 9.4.1; JDK via `gradle.properties` `org.gradle.java.home` when needed

---

## 4. Architecture & Package Structure

```
com.mmr/
├── app/
│   ├── MmrApplication.kt
│   ├── MainActivity.kt               # enableEdgeToEdge(); MoneyManagerRTheme → MmrNavHost
│   ├── di/                           # AppModule, DatabaseModule, RepositoryModule
│   ├── ui/theme/                     # Inter (MmrFontFamily), Material 3 typography
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
│   ├── database/                     # MmrDatabase v3, CategoryEntity, SubcategoryEntity, TransactionEntity (relatedAccountId, loanId)
│   ├── parser/scanner/
│   │   └── LocalSmsScannerService.kt # scanInbox(monthsBack) — date >= now−N months, no message count cap
│   ├── data/preferences/
│   │   └── SmsScanSettingsStore.kt   # SharedPreferences: SMS scan months (default 3, range 1–36)
│   └── domain/ + data/repository/    # Transaction, Category, Approval, Account, Loan, …
└── feature/
    ├── home/                         # Month picker, view tabs (Daily/Calendar/Monthly/Total/Note), summary strip, grouping utils
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
| **Phase 9: Hardening & Testing** | **In progress** | Guards, MmrUiMessages, repository tests, ViewModel fakes. |
| **Category & subcategory CRUD** | **Completed** | Room v3, `CategoryRepository`, Settings → Manage categories, hierarchy UI. |
| **Home shell (reference layout)** | **Completed** | Center month title + picker dialog; tabs; Income/Expense/Total strip; grouped dates as `day, weekday, month, year` (not Today/Yesterday). |
| **Navigation shell** | **Completed** | Bottom bar with icons + Inter; Approvals via **mail icon + badge** on Home top bar (not a tab). |
| **Transaction search** | **Completed** | Full-screen Search from Home 🔍; notes-only text filter; date range; account; read-only category/subcategory; amount min/max. |
| **Add transaction UX** | **Completed** | Category row + two-column picker (parent/sub, long-press rename, + add cat/sub); Account as form row + sheet; Transfer **From** / **To** (accounts + active loans). |
| **Launcher icon** | **Completed** | Adaptive icon vectors (deep blue + M mark); legacy webp may need Image Asset refresh on API 24–25. |

---

## 6. Key Implementation Decisions (Recent)

### Navigation & Home
- **Main tabs**: Transactions (home), Stats, Loans, Recurring, Settings — `MmrMainDestinations`, `NavigationBar` with `WindowInsets` for system nav bar.
- **Approvals**: Route `approval_center`; opened from Home top-right mail icon; `getPendingCount()` badge.
- **Home top bar**: `CenterAlignedTopAppBar` — brand icon left, centered month ‹ ›, Search + Mail actions (equal-size icon buttons).
- **Home month picker**: `HomeMonthPickerDialog` — year arrows, 3×4 months, THIS MONTH shortcut.
- **Home data**: Month-scoped totals via `getTotalIncomeBetween` / `getTotalExpenseBetween`; Total tab uses all-time totals.

### Transactions & categories
- **Transfer persistence**: `accountId` = source; `relatedAccountId` = destination account OR `loanId` = destination loan (mutually exclusive on To).
- **Category picker in Add/Edit**: Creates/updates via `CategoryRepository` (syncs with Settings categories); search picker is **select-only** (no inline create).
- **Account UI**: `TransactionFormRow` + `AccountPickerSheet` / `TransferToPickerSheet` (not chip row).

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

### Approval & backup (unchanged core)
- **Approval promotion is atomic** via `ApprovalLedgerDao` `@Transaction` blocks.
- **Backup restore order**: FK-safe table order documented in prior phases.

### Testing
- JVM: `HomeViewModelTest`, fakes include `FakeApprovalRepository`, `FakeCategoryRepository`, etc.
- Prefer `assembleDebug` + `testDebugUnitTest` after feature batches.

---

## 7. Current State Verification

- **Build**: `:app:assembleDebug` and unit tests were passing after latest home/search/transfer work (re-verify after local changes).
- **Room version**: 3 (categories/subcategories); destructive migration on bump if not migrated.
- **SMS**: Scan depth controlled by Settings months preference (not message count).
- **No app-lock**: MainActivity → MmrNavHost directly.

---

## 8. Maintenance Note

**This file is not auto-updated by the IDE.** When significant features land (navigation, schema, SMS limits, UX contracts), append or revise §5–§6 in the same session or ask the agent to refresh `PROJECT_MEMORY.md`.
