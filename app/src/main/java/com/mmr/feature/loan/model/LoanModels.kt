package com.mmr.feature.loan.model

import com.mmr.core.model.enums.LoanPaymentType
import com.mmr.core.model.enums.LoanStatus
import com.mmr.core.model.enums.LoanType

enum class LoanFilterType {
    ALL,
    BORROWED,
    LENT
}

enum class LoanStatusFilter {
    ALL,
    ACTIVE,
    CLOSED,
    OVERDUE,
    PAUSED
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
    val progressPercentage: Float,
    val progressFormatted: String
)

data class LoanSummaryUi(
    val totalBorrowedPrincipal: Double = 0.0,
    val totalBorrowedOutstanding: Double = 0.0,
    val totalLentPrincipal: Double = 0.0,
    val totalLentOutstanding: Double = 0.0,
    val activeLoansCount: Int = 0,
    val totalBorrowedOutstandingFormatted: String = "₹0.00",
    val totalLentOutstandingFormatted: String = "₹0.00"
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
    val notes: String?
)

data class LoanListUiState(
    val isLoading: Boolean = true,
    val loans: List<LoanItemUi> = emptyList(),
    val summary: LoanSummaryUi = LoanSummaryUi(),
    val selectedFilterType: LoanFilterType = LoanFilterType.ALL,
    val selectedStatusFilter: LoanStatusFilter = LoanStatusFilter.ALL,
    val isEmpty: Boolean = false,
    val errorMessage: String? = null,
    val statusMessage: String? = null
)

sealed interface LoanListUiEvent {
    data class OnFilterTypeSelected(val filterType: LoanFilterType) : LoanListUiEvent
    data class OnStatusFilterSelected(val statusFilter: LoanStatusFilter) : LoanListUiEvent
    data class OnDeleteLoan(val loanId: Long) : LoanListUiEvent
    data class OnUpdateLoanStatus(val loanId: Long, val status: LoanStatus) : LoanListUiEvent
    object OnRetryClick : LoanListUiEvent
    object OnDismissStatus : LoanListUiEvent
}

data class LoanDetailUiState(
    val isLoading: Boolean = true,
    val loan: LoanItemUi? = null,
    val payments: List<LoanPaymentItemUi> = emptyList(),
    val isRepaymentSubmitting: Boolean = false,
    val isRepaymentSuccess: Boolean = false,
    val repaymentAmountInput: String = "",
    val repaymentNotesInput: String = "",
    val repaymentType: LoanPaymentType = LoanPaymentType.PART_PAYMENT,
    val errorMessage: String? = null,
    val statusMessage: String? = null
)

sealed interface LoanDetailUiEvent {
    data class OnRepaymentAmountChanged(val amount: String) : LoanDetailUiEvent
    data class OnRepaymentNotesChanged(val notes: String) : LoanDetailUiEvent
    data class OnRepaymentTypeSelected(val type: LoanPaymentType) : LoanDetailUiEvent
    object OnSubmitRepayment : LoanDetailUiEvent
    data class OnStatusChanged(val status: LoanStatus) : LoanDetailUiEvent
    object OnClearRepaymentState : LoanDetailUiEvent
    object OnDeleteLoan : LoanDetailUiEvent
    object OnRetryClick : LoanDetailUiEvent
    object OnDismissStatus : LoanDetailUiEvent
}

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
    val linkedDebitAccountId: Long? = null,
    val notes: String = "",
    val errorMessage: String? = null,
    val canSave: Boolean = false
)

sealed interface AddEditLoanUiEvent {
    data class OnNameChanged(val name: String) : AddEditLoanUiEvent
    data class OnLenderNameChanged(val lenderName: String) : AddEditLoanUiEvent
    data class OnPrincipalChanged(val principal: String) : AddEditLoanUiEvent
    data class OnInterestRateChanged(val rate: String) : AddEditLoanUiEvent
    data class OnLoanTypeSelected(val type: LoanType) : AddEditLoanUiEvent
    data class OnDueDateChanged(val dueDate: Long?) : AddEditLoanUiEvent
    data class OnLinkedDebitAccountSelected(val accountId: Long?) : AddEditLoanUiEvent
    data class OnNotesChanged(val notes: String) : AddEditLoanUiEvent
    object OnSaveClick : AddEditLoanUiEvent
}
