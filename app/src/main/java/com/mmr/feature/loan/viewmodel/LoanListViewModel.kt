package com.mmr.feature.loan.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mmr.app.ui.state.MmrUiMessages
import com.mmr.core.common.display.MmrMoneyFormat
import com.mmr.core.database.entity.LoanEntity
import com.mmr.core.database.model.LoanSummaryAggregate
import com.mmr.core.domain.repository.LoanRepository
import com.mmr.core.loan.LoanDisplayFormat
import com.mmr.core.model.enums.LoanStatus
import com.mmr.core.model.enums.LoanType
import com.mmr.feature.loan.model.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

@HiltViewModel
class LoanListViewModel
    @Inject
    constructor(
        private val loanRepository: LoanRepository,
    ) : ViewModel() {
        private val _selectedFilterType = MutableStateFlow(LoanFilterType.ALL)
        private val _selectedStatusFilter = MutableStateFlow(LoanStatusFilter.ALL)
        private val _loadErrorMessage = MutableStateFlow<String?>(null)
        private val _statusMessage = MutableStateFlow<String?>(null)

        private val loanListData =
            combine(
                loanRepository
                    .getAllLoans()
                    .catch { e ->
                        _loadErrorMessage.value = MmrUiMessages.friendly(e.message, MmrUiMessages.GENERIC_LOAD)
                        emit(emptyList())
                    },
                loanRepository
                    .getLoanSummary()
                    .catch { emit(LoanSummaryAggregate(0.0, 0.0, 0.0, 0.0, 0)) },
                _selectedFilterType,
                _selectedStatusFilter,
            ) { allLoans, summaryAggregate, filterType, statusFilter ->
                LoanListData(allLoans, summaryAggregate, filterType, statusFilter)
            }

        val uiState: StateFlow<LoanListUiState> =
            combine(
                loanListData,
                _loadErrorMessage,
                _statusMessage,
            ) { data, loadError, status ->
                val allLoans = data.allLoans
                val summaryAggregate = data.summaryAggregate
                val filterType = data.filterType
                val statusFilter = data.statusFilter
                val filtered =
                    allLoans.filter { loan ->
                        val matchesType =
                            when (filterType) {
                                LoanFilterType.ALL -> true
                                LoanFilterType.BORROWED -> loan.loanType == LoanType.BORROWED
                                LoanFilterType.LENT -> loan.loanType == LoanType.LENT
                            }

                        val matchesStatus =
                            when (statusFilter) {
                                LoanStatusFilter.ALL -> true
                                LoanStatusFilter.ACTIVE -> loan.status == LoanStatus.ACTIVE
                                LoanStatusFilter.CLOSED -> loan.status == LoanStatus.CLOSED
                                LoanStatusFilter.OVERDUE -> loan.status == LoanStatus.OVERDUE
                                LoanStatusFilter.PAUSED -> loan.status == LoanStatus.PAUSED
                            }

                        matchesType && matchesStatus
                    }

                val items = filtered.map { it.toItemUi() }

                val hasAnyLoans = allLoans.isNotEmpty()
                LoanListUiState(
                    isLoading = false,
                    loans = items,
                    summary = summaryAggregate.toSummaryUi(),
                    selectedFilterType = filterType,
                    selectedStatusFilter = statusFilter,
                    isEmpty = loadError == null && !hasAnyLoans,
                    isFilteredEmpty = loadError == null && hasAnyLoans && items.isEmpty(),
                    errorMessage = loadError,
                    statusMessage = status,
                )
            }.stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = LoanListUiState(isLoading = true),
            )

        fun onEvent(event: LoanListUiEvent) {
            when (event) {
                is LoanListUiEvent.OnFilterTypeSelected -> {
                    _selectedFilterType.value = event.filterType
                }

                is LoanListUiEvent.OnStatusFilterSelected -> {
                    _selectedStatusFilter.value = event.statusFilter
                }

                is LoanListUiEvent.OnDeleteLoan -> {
                    viewModelScope.launch {
                        try {
                            val loan = loanRepository.getLoanById(event.loanId)
                            if (loan != null) {
                                loanRepository.deleteLoan(loan)
                            } else {
                                _statusMessage.value = "Loan not found."
                            }
                        } catch (e: Exception) {
                            _statusMessage.value = MmrUiMessages.friendly(e.message, MmrUiMessages.GENERIC_ACTION)
                        }
                    }
                }

                is LoanListUiEvent.OnUpdateLoanStatus -> {
                    viewModelScope.launch {
                        try {
                            loanRepository.updateLoanStatus(event.loanId, event.status)
                        } catch (e: Exception) {
                            _statusMessage.value = MmrUiMessages.friendly(e.message, MmrUiMessages.GENERIC_ACTION)
                        }
                    }
                }

                LoanListUiEvent.OnRetryClick -> {
                    _loadErrorMessage.value = null
                    _statusMessage.value = null
                }

                LoanListUiEvent.OnDismissStatus -> {
                    _statusMessage.value = null
                }

                LoanListUiEvent.OnResetFilters -> {
                    _selectedFilterType.value = LoanFilterType.ALL
                    _selectedStatusFilter.value = LoanStatusFilter.ALL
                }
            }
        }
    }

private data class LoanListData(
    val allLoans: List<LoanEntity>,
    val summaryAggregate: LoanSummaryAggregate,
    val filterType: LoanFilterType,
    val statusFilter: LoanStatusFilter,
)

internal fun LoanEntity.toItemUi(): LoanItemUi {
    val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
    val repaid = (principalAmount - outstandingAmount).coerceAtLeast(0.0)
    val progress = if (principalAmount > 0.0) (repaid / principalAmount).toFloat() else 0f

    val statusLabel =
        status.name
            .lowercase(Locale.getDefault())
            .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }

    return LoanItemUi(
        id = id,
        name = name,
        lenderName = lenderName,
        principalAmount = principalAmount,
        principalFormatted = MmrMoneyFormat.formatInr(principalAmount),
        outstandingAmount = outstandingAmount,
        outstandingFormatted = MmrMoneyFormat.formatInr(outstandingAmount),
        interestRate = interestRate,
        interestRateFormatted = LoanDisplayFormat.annualRatePercent(interestRate),
        loanType = loanType,
        status = status,
        statusLabel = statusLabel,
        dueDateFormatted = dueDate?.let { dateFormat.format(Date(it)) },
        disbursalDateFormatted = dateFormat.format(Date(openingDate)),
        progressPercentage = progress.coerceIn(0f, 1f),
        progressFormatted = "${"%.1f".format(progress * 100)}%",
    )
}

internal fun LoanSummaryAggregate.toSummaryUi(): LoanSummaryUi =
    LoanSummaryUi(
        totalBorrowedPrincipal = totalBorrowedPrincipal,
        totalBorrowedOutstanding = totalBorrowedOutstanding,
        totalLentPrincipal = totalLentPrincipal,
        totalLentOutstanding = totalLentOutstanding,
        activeLoansCount = activeLoansCount,
        totalBorrowedOutstandingFormatted = MmrMoneyFormat.formatInr(totalBorrowedOutstanding),
        totalLentOutstandingFormatted = MmrMoneyFormat.formatInr(totalLentOutstanding),
    )
