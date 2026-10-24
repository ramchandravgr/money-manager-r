# Transaction entry UI — where Income / Expense / Transfer is used

## One shared form implementation

| Screen | Composable | ViewModel | Routes |
|--------|------------|-----------|--------|
| Add / edit ledger transaction | `AddEditTransactionRoute` → `AddEditTransactionScreen.kt` | `AddEditTransactionViewModel` | `add_transaction` (optional approval prefill, date prefill, duplicate-from id), `edit_transaction/{id}` |
| Add / edit recurring rule | `AddEditRecurringRuleScreen.kt` | `AddEditRecurringRuleViewModel` | `add_recurring`, `edit_recurring/{ruleId}` |
| Add / edit loan | `AddEditLoanScreen.kt` | `AddEditLoanViewModel` | `add_loan`, `edit_loan/{loanId}` |

Shared field widgets: `TransactionFormFields.kt` (`TransactionAmountField`, `TransactionNoteField`, `TransactionDateTimeField`, `TransactionFormPickerField`), loan-specific `LoanFormFields.kt`, pickers in `CategorySubcategoryPickerSheet.kt`, `AccountPickerSheet.kt`, receipts in `TransactionReceiptSection.kt`.

**Visual chrome (2026-10):** `MmrTransactionFormChrome.kt` — screen background, white cards, type segment, picker rows (icon + value + chevron), text rows, choice chips, attach button. **Shared field API:** `TransactionFormFields.kt` (`TransactionNameField`, `TransactionAmountField`, `TransactionNoteField` + camera, `TransactionCategoryPickerField`, `TransactionAccountPickerField`, transfer/reminder pickers, `TransactionRecurrenceIntervalChips`). Receipt thumbnails: `TransactionReceiptSection.kt` (below note when photos exist).

## Navigation entry points (same `AddEditTransactionRoute`)

- **Home** FAB / calendar add → `Screen.AddTransaction`
- **Transaction detail** → Edit → `Screen.EditTransaction`
- **Search** row → edit transaction
- **Approval center** → edit pending → `Screen.AddTransaction` with `approvalId`
- **Duplicate** from detail → `Screen.AddTransaction` with duplicate source id + date
- **Loan detail** → linked transaction edit (nav graph `onNavigateToEditTransaction`)

## Recurring entry points

- **Recurring list** FAB → `Screen.AddRecurringRule`
- **Recurring detail** → Edit → `Screen.EditRecurringRule`

## Loan entry points

- **Loans list** FAB → `Screen.AddLoan`
- **Loan detail** → Edit → `Screen.EditLoan`

## Not the same UI (read-only or different flows)

- `TransactionDetailScreen.kt` — view / duplicate / delete (navigates to edit route for changes)
- `TransactionSearchScreen.kt` — list only
- Approval quick-approve / SMS flows — no full transaction form until “edit pending” opens add route with approval id
- Settings account/category forms — iOS inset grouped (`MmrIosSettings*`), not transaction entry cards
- Loan repayment / top-up dialogs — compact `MmrContentDialog` flows on loan detail (not full-screen entry)

## Tests touching these screens

`AddTransactionFlowUiTest`, `TransferTransactionFlowUiTest`, `TransactionDetailFlowUiTest`, `RecurringAddFlowUiTest`, `RecurringSaveFlowUiTest`, approval flows that open pending edit.
