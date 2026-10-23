# MMR test coverage matrix

Legend: **Done** = automated test exists · **Partial** = logic only or narrow path · **Todo** = no meaningful test yet

Status keys used in tables: `Done` | `Partial` | `Todo`

---

## 1. Instrumentation & CI

| Item | Layer | Status |
|------|--------|--------|
| Hilt test runner + in-memory DB | androidTest harness | Done |
| `MmrUiTestFixtures` / repo graph | androidTest helpers | Done |
| Pre-commit compile hook | `.githooks` | Done (manual) |
| GitHub Actions `connectedDebugAndroidTest` | CI | Done |

---

## 2. UI flows (device / emulator)

| Feature | Test class | Status |
|---------|------------|--------|
| App launch + bottom nav (Home, Stats) | `MainActivityLaunchUiTest` | Done |
| Bottom nav (Loans, Recurring, Settings) | `MainNavigationUiTest` | Done |
| Add manual expense | `AddTransactionFlowUiTest` | Done |
| Add transfer (account → account) | `TransferTransactionFlowUiTest` | Done |
| Search by note | `TransactionSearchFlowUiTest` | Done |
| Home income/expense/total strip | `HomeSummaryStripUiTest` | Done |
| Approval quick approve → Home | `ApprovalCenterFlowUiTest` | Done |
| Approval quick reject | `ApprovalRejectFlowUiTest` | Done |
| Stats screen + ledger tabs | `StatsScreenUiTest` | Done |
| Home Calendar / Monthly / Total tabs | `HomeTabsUiTest` | Done |
| Home multi-select merge | `HomeMergeFlowUiTest` | Done |
| Home multi-select delete | `HomeDeleteFlowUiTest` | Done |
| Home Excel export period dialog | `HomeTabsUiTest` (Total tab) | Partial (no share intent assert) |
| Transaction detail edit / delete / duplicate | `TransactionDetailFlowUiTest` | Done (duplicate via Today) |
| Category add (Settings) | `CategoryAddFlowUiTest` | Partial |
| Settings → Accounts / Categories / Backup | `SettingsNavigationUiTest` | Done |
| Account CRUD | — | Partial (list nav only) |
| Loan create (flexible) | `LoanCreateFlowUiTest` | Done |
| Loan repayment dialog | `LoanRepaymentFlowUiTest` | Done |
| Recurring add rule screen | `RecurringAddFlowUiTest`, `RecurringSaveFlowUiTest` | Done |
| Backup export / import UI | `BackupScreenUiTest`, `BackupImportFlowUiTest`, `BackupImportHostUiTest` | Done (repository + `BackupScreenHostActivity` dialog replace/merge; no SAF Intents) |
| SMS inbox scan + permission | `ApprovalInboxScanUiTest` (physical device inbox) | Done (requires real bank SMS in scan window; excluded from CI emulator) |
| Approval merge dialog | `ApprovalMergeFlowUiTest` | Done |
| Manual SMS entry dialog | `ApprovalManualSmsFlowUiTest` | Done |

---

## 3. Data layer (instrumented, no UI)

| Feature | Test class | Status |
|---------|------------|--------|
| Transaction insert guards | `TransactionRepositoryInstrumentedTest` | Done |
| Approval insert / approve / reject | `ApprovalRepositoryInstrumentedTest` | Done |
| Loan payment + close | `LoanRepositoryInstrumentedTest` | Done |
| Backup export / restore / pending omit | `BackupRestoreInstrumentedTest` | Done |
| Backup JSON codec | `BackupJsonCodecInstrumentedTest` | Done |
| Merge transactions coordinator | `TransactionMergeCoordinatorInstrumentedTest` | Done |
| Approval merge into ledger row | `ApprovalMergeInstrumentedTest` | Done |
| EMI auto-post coordinator | `EmiAutoPostCoordinatorInstrumentedTest` | Done |
| Loan top-up / flexible EMI schedule | — | Partial (JVM loan tests) |
| Backup import merge mode (repository) | — | Partial (`BackupImportPlannerTest` JVM) |

---

## 4. Unit tests (JVM)

| Area | Test(s) | Status |
|------|---------|--------|
| SMS parser phases 1–3 | `SmsParserPhase*Test` | Done |
| SMS templates / normalizer / dedup | various | Done |
| Loan amortization / flexible interest / schedule | `Loan*Test` | Done |
| Categorization rules | `CategorizationRuleEngineTest` | Done |
| Transaction cash flow | `TransactionCashFlowTest` | Done |
| Backup import planner | `BackupImportPlannerTest` | Done |
| Excel export writer / periods | `PeriodExcelWriterTest`, `HomeExcelExportPeriodsTest` | Done |
| Home total utils | `HomeTotalUtilsTest` | Done |
| Search period presets | `SearchPeriodPresetsTest` | Done |
| Repository guards | `RepositoryGuardsTest` | Done |
| Home / Stats ViewModels | `HomeViewModelTest`, `StatsViewModelTest` | Partial |
| Stats period utils | `StatsPeriodUtilsTest` | Done |
| Loan list ViewModel | `LoanListViewModelTest` | Partial |
| Transaction merge coordinator | — | Partial (instrumented) |
| Emi auto-post coordinator | — | Partial (instrumented; no JVM) |

---

## 5. Tags (`MmrTestTags`) vs tests

| Tag | UI test |
|-----|---------|
| `nav.tab.*` | `MainActivityLaunchUiTest`, `MainNavigationUiTest` |
| `home.fab.add`, `transaction.field.*`, `app_bar.save` | Add + Transfer tests |
| `home.top_bar.search`, `search.field.note_query` | Search test |
| `home.summary.*` | Home summary test |
| `home.top_bar.approvals`, `approval.*` | Approve + Reject tests |
| `stats.tab.*` | Stats screen test |
| `transaction.type.*` | Transfer test |
| `home.tab.*`, `home.calendar.*`, `home.total.export_excel` | `HomeTabsUiTest` |
| `home.selection.merge`, `transaction.row.*` | `HomeMergeFlowUiTest` |
| `settings.nav.*` | `SettingsNavigationUiTest`, `CategoryAddFlowUiTest` |
| `loan.fab.add`, `loan.field.*` | `LoanCreateFlowUiTest` |
| `recurring.fab.add` | `RecurringAddFlowUiTest` |
| `approval.fab.manual_sms` | `ApprovalManualSmsFlowUiTest` |
| `app_bar.save` | Add / Transfer / Loan / Category tests |

---

## 6. Maintenance

- After each feature PR: add a row here and at least one **Done** test (JVM or UI).
- Full device suite (USB phone): `.\gradlew :app:connectedDebugAndroidTest`
- **Real SMS on device**: before `ApprovalInboxScanUiTest`, ensure READ_SMS is allowed and at least one parseable transaction SMS exists within the app’s SMS scan month setting. CI emulator job skips this class (see workflow).
- Full emulator CI suite: `connectedDebugAndroidTest` with `notClass` for `ApprovalInboxScanUiTest`
- JVM suite: `.\gradlew :app:testDebugUnitTest`
- UI tests call `MmrUiTestFixtures.resetUiTestDatabase()` in `@Before` to reduce cross-test pollution.
