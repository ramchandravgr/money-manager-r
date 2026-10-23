package com.mmr.feature.loan.model

import com.mmr.core.model.enums.LoanPartPaymentAdjustment
import com.mmr.core.model.enums.LoanPaymentType
import com.mmr.core.model.enums.LoanRepaymentMode
import com.mmr.core.model.enums.LoanStatus
import com.mmr.core.model.enums.LoanTopUpAdjustment
import com.mmr.core.model.enums.LoanType

enum class LoanFilterType {
    ALL,
    BORROWED,
    LENT,
}

enum class LoanStatusFilter {
    ALL,
    ACTIVE,
    CLOSED,
    OVERDUE,
    PAUSED,
}

/** Which value the user enters for fixed-EMI loans; the other is derived at save time. */
enum class FixedEmiEntryMode {
    TENURE,
    EMI_AMOUNT,
}

data class LoanItemUi(
    val id: Long,
    val name: String,
    val lenderName: String,
    val principalAmount: Double,
    val principalFormatted: String,
    val outstandingAmount: Double,
    val outstandingFormatted: String,
    val interestRate: Double,
    val interestRateFormatted: String,
    val loanType: LoanType,
    val status: LoanStatus,
    val statusLabel: String,
    val dueDateFormatted: String?,
    val disbursalDateFormatted: String,
    val progressPercentage: Float,
    val progressFormatted: String,
)

data class LoanSummaryUi(
    val totalBorrowedPrincipal: Double = 0.0,
    val totalBorrowedOutstanding: Double = 0.0,
    val totalLentPrincipal: Double = 0.0,
    val totalLentOutstanding: Double = 0.0,
    val activeLoansCount: Int = 0,
    val totalBorrowedOutstandingFormatted: String = "₹0.00",
    val totalLentOutstandingFormatted: String = "₹0.00",
)

data class LoanPaymentItemUi(
    val id: Long,
    val paymentAmount: Double,
    val paymentAmountFormatted: String,
    val paymentDateFormatted: String,
    val paymentType: LoanPaymentType,
    val paymentTypeLabel: String,
    val principalComponentFormatted: String?,
    val interestComponentFormatted: String?,
    val outstandingAfterFormatted: String,
    val notes: String?,
    val linkedTransactionId: Long? = null,
)

data class LoanListUiState(
    val isLoading: Boolean = true,
    val loans: List<LoanItemUi> = emptyList(),
    val summary: LoanSummaryUi = LoanSummaryUi(),
    val selectedFilterType: LoanFilterType = LoanFilterType.ALL,
    val selectedStatusFilter: LoanStatusFilter = LoanStatusFilter.ALL,
    val isEmpty: Boolean = false,
    /** Loans exist in the database but none match the current filters. */
    val isFilteredEmpty: Boolean = false,
    val errorMessage: String? = null,
    val statusMessage: String? = null,
)

sealed interface LoanListUiEvent {
    data class OnFilterTypeSelected(
        val filterType: LoanFilterType,
    ) : LoanListUiEvent

    data class OnStatusFilterSelected(
        val statusFilter: LoanStatusFilter,
    ) : LoanListUiEvent

    data class OnDeleteLoan(
        val loanId: Long,
    ) : LoanListUiEvent

    data class OnUpdateLoanStatus(
        val loanId: Long,
        val status: LoanStatus,
    ) : LoanListUiEvent

    object OnRetryClick : LoanListUiEvent

    object OnDismissStatus : LoanListUiEvent

    object OnResetFilters : LoanListUiEvent
}

data class LoanScheduleItemUi(
    val dueDateLabel: String,
    val emiFormatted: String,
    val principalFormatted: String,
    val interestFormatted: String,
)

data class LoanInterestRevisionItemUi(
    val id: Long,
    val effectiveDateLabel: String,
    val rateLabel: String,
    val note: String?,
)

data class LoanRevisionDetailUi(
    val beforeRateLabel: String,
    val afterRateLabel: String,
    val emiBeforeFormatted: String?,
    val emiAfterFormatted: String?,
    val note: String?,
)

data class LoanDetailUiState(
    val isLoading: Boolean = true,
    val loan: LoanItemUi? = null,
    val repaymentMode: LoanRepaymentMode = LoanRepaymentMode.FLEXIBLE,
    val fixedEmiFormatted: String? = null,
    val remainingTenureMonths: Int? = null,
    val upcomingSchedule: List<LoanScheduleItemUi> = emptyList(),
    val payments: List<LoanPaymentItemUi> = emptyList(),
    val isRepaymentSubmitting: Boolean = false,
    val isRepaymentSuccess: Boolean = false,
    val repaymentAmountInput: String = "",
    val repaymentNotesInput: String = "",
    val repaymentPaymentDateMillis: Long = System.currentTimeMillis(),
    val repaymentType: LoanPaymentType = LoanPaymentType.PART_PAYMENT,
    val partPaymentAdjustment: LoanPartPaymentAdjustment = LoanPartPaymentAdjustment.REDUCE_TENURE,
    val showRateChangeDialog: Boolean = false,
    val showEmiChangeDialog: Boolean = false,
    val rateChangeInput: String = "",
    val emiChangeInput: String = "",
    val errorMessage: String? = null,
    val statusMessage: String? = null,
    val interestRevisions: List<LoanInterestRevisionItemUi> = emptyList(),
    val revisionDetail: LoanRevisionDetailUi? = null,
    val isLoanDeleted: Boolean = false,
    val showTopUpDialog: Boolean = false,
    val topUpAmountInput: String = "",
    val topUpNotesInput: String = "",
    val topUpDateMillis: Long = System.currentTimeMillis(),
    val topUpAdjustment: LoanTopUpAdjustment = LoanTopUpAdjustment.EXTEND_TENURE,
    val isTopUpSubmitting: Boolean = false,
    val flexibleInterestPendingFormatted: String? = null,
    val flexibleInterestAccrualFromLabel: String? = null,
    /** Accrued interest as of repayment dialog payment date (flexible loans). */
    val flexibleRepaymentAccruedInterestHint: String? = null,
)

sealed interface LoanDetailUiEvent {
    data class OnRepaymentAmountChanged(
        val amount: String,
    ) : LoanDetailUiEvent

    data class OnRepaymentNotesChanged(
        val notes: String,
    ) : LoanDetailUiEvent

    data class OnRepaymentPaymentDateChanged(
        val paymentDateMillis: Long,
    ) : LoanDetailUiEvent

    data class OnRepaymentTypeSelected(
        val type: LoanPaymentType,
    ) : LoanDetailUiEvent

    object OnSubmitRepayment : LoanDetailUiEvent

    data class OnStatusChanged(
        val status: LoanStatus,
    ) : LoanDetailUiEvent

    object OnClearRepaymentState : LoanDetailUiEvent

    object OnDeleteLoan : LoanDetailUiEvent

    object OnRetryClick : LoanDetailUiEvent

    object OnDismissStatus : LoanDetailUiEvent

    data class OnPartPaymentAdjustmentSelected(
        val adjustment: LoanPartPaymentAdjustment,
    ) : LoanDetailUiEvent

    object OnOpenRateChangeDialog : LoanDetailUiEvent

    object OnDismissRateChangeDialog : LoanDetailUiEvent

    data class OnRateChangeInputChanged(
        val value: String,
    ) : LoanDetailUiEvent

    object OnSubmitRateChange : LoanDetailUiEvent

    object OnOpenEmiChangeDialog : LoanDetailUiEvent

    object OnDismissEmiChangeDialog : LoanDetailUiEvent

    data class OnEmiChangeInputChanged(
        val value: String,
    ) : LoanDetailUiEvent

    object OnSubmitEmiChange : LoanDetailUiEvent

    data class OnInterestRevisionClick(
        val revisionId: Long,
    ) : LoanDetailUiEvent

    object OnDismissRevisionDetail : LoanDetailUiEvent

    object OnOpenTopUpDialog : LoanDetailUiEvent

    object OnDismissTopUpDialog : LoanDetailUiEvent

    data class OnTopUpAmountChanged(
        val amount: String,
    ) : LoanDetailUiEvent

    data class OnTopUpNotesChanged(
        val notes: String,
    ) : LoanDetailUiEvent

    data class OnTopUpDateChanged(
        val dateMillis: Long,
    ) : LoanDetailUiEvent

    data class OnTopUpAdjustmentSelected(
        val adjustment: LoanTopUpAdjustment,
    ) : LoanDetailUiEvent

    object OnSubmitTopUp : LoanDetailUiEvent
}

data class LoanLiabilityAccountUi(
    val id: Long,
    val name: String,
)

data class AddEditLoanUiState(
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val isSaved: Boolean = false,
    val loanId: Long? = null,
    val name: String = "",
    val lenderName: String = "",
    val principalAmount: String = "",
    val interestRate: String = "0.0",
    val loanType: LoanType = LoanType.BORROWED,
    val dueDate: Long? = null,
    val disbursalDateMillis: Long = System.currentTimeMillis(),
    val canEditDisbursalDate: Boolean = true,
    val linkedDebitAccountId: Long? = null,
    val linkedLoanAccountId: Long? = null,
    val loanLiabilityAccounts: List<LoanLiabilityAccountUi> = emptyList(),
    val repaymentMode: LoanRepaymentMode = LoanRepaymentMode.FLEXIBLE,
    val fixedEmiEntryMode: FixedEmiEntryMode = FixedEmiEntryMode.TENURE,
    val tenureMonths: String = "",
    val fixedEmiOverride: String = "",
    val emiDayOfMonth: String = "1",
    val notes: String = "",
    val originalRepaymentMode: LoanRepaymentMode? = null,
    val errorMessage: String? = null,
    val canSave: Boolean = false,
)

sealed interface AddEditLoanUiEvent {
    data class OnNameChanged(
        val name: String,
    ) : AddEditLoanUiEvent

    data class OnLenderNameChanged(
        val lenderName: String,
    ) : AddEditLoanUiEvent

    data class OnPrincipalChanged(
        val principal: String,
    ) : AddEditLoanUiEvent

    data class OnInterestRateChanged(
        val rate: String,
    ) : AddEditLoanUiEvent

    data class OnLoanTypeSelected(
        val type: LoanType,
    ) : AddEditLoanUiEvent

    data class OnDueDateChanged(
        val dueDate: Long?,
    ) : AddEditLoanUiEvent

    data class OnDisbursalDateChanged(
        val disbursalDateMillis: Long,
    ) : AddEditLoanUiEvent

    data class OnLinkedDebitAccountSelected(
        val accountId: Long?,
    ) : AddEditLoanUiEvent

    data class OnLinkedLoanAccountSelected(
        val accountId: Long?,
    ) : AddEditLoanUiEvent

    data class OnNotesChanged(
        val notes: String,
    ) : AddEditLoanUiEvent

    data class OnRepaymentModeSelected(
        val mode: LoanRepaymentMode,
    ) : AddEditLoanUiEvent

    data class OnFixedEmiEntryModeSelected(
        val mode: FixedEmiEntryMode,
    ) : AddEditLoanUiEvent

    data class OnTenureMonthsChanged(
        val months: String,
    ) : AddEditLoanUiEvent

    data class OnEmiDayChanged(
        val day: String,
    ) : AddEditLoanUiEvent

    data class OnFixedEmiOverrideChanged(
        val emi: String,
    ) : AddEditLoanUiEvent

    object OnSaveClick : AddEditLoanUiEvent
}
