package com.mmr.feature.stats.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mmr.app.ui.state.MmrUiMessages
import com.mmr.core.common.validation.MmrValidation
import com.mmr.core.domain.repository.TransactionRepository
import com.mmr.feature.stats.model.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class StatsViewModel @Inject constructor(
    private val transactionRepository: TransactionRepository
) : ViewModel() {

    private val _selectedRangeType = MutableStateFlow(DateRangeType.THIS_MONTH)
    private val _customDateRange = MutableStateFlow<Pair<Long, Long>?>(null)
    private val _errorMessage = MutableStateFlow<String?>(null)

    private val dateRangeFilter: StateFlow<DateRangeBounds> = combine(
        _selectedRangeType,
        _customDateRange
    ) { rangeType, customRange ->
        calculateDateRange(rangeType, customRange)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = calculateDateRange(DateRangeType.THIS_MONTH, null)
    )

    val uiState: StateFlow<StatsUiState> = dateRangeFilter.flatMapLatest { bounds ->
        combine(
            transactionRepository.getTotalIncomeBetween(bounds.startDate, bounds.endDate)
                .catch { e ->
                    _errorMessage.value = MmrUiMessages.friendly(e.message, MmrUiMessages.GENERIC_LOAD)
                    emit(null)
                },
            transactionRepository.getTotalExpenseBetween(bounds.startDate, bounds.endDate)
                .catch { e ->
                    _errorMessage.value = MmrUiMessages.friendly(e.message, MmrUiMessages.GENERIC_LOAD)
                    emit(null)
                },
            transactionRepository.getCategorySpendingBetween(bounds.startDate, bounds.endDate)
                .catch { e ->
                    _errorMessage.value = MmrUiMessages.friendly(e.message, MmrUiMessages.GENERIC_LOAD)
                    emit(emptyList())
                },
            _errorMessage
        ) { incomeVal, expenseVal, spendings, error ->
            val income = incomeVal ?: 0.0
            val expense = expenseVal ?: 0.0
            val net = income - expense

            val breakdowns = spendings.map { spending ->
                val percentage = if (expense > 0) (spending.totalAmount / expense).toFloat() else 0f
                CategoryBreakdownUi(
                    categoryId = spending.categoryId,
                    categoryName = spending.categoryName ?: "Uncategorized",
                    colorHex = spending.colorHex ?: "#6B7280",
                    amount = spending.totalAmount,
                    amountFormatted = "₹${"%.2f".format(spending.totalAmount)}",
                    percentage = percentage,
                    percentageFormatted = "${"%.1f".format(percentage * 100)}%",
                    transactionCount = spending.transactionCount
                )
            }

            val netPrefix = if (net >= 0) "+₹" else "-₹"
            val summary = StatsSummaryUi(
                totalIncome = income,
                totalExpense = expense,
                netBalance = net,
                totalIncomeFormatted = "+₹${"%.2f".format(income)}",
                totalExpenseFormatted = "-₹${"%.2f".format(expense)}",
                netBalanceFormatted = "$netPrefix${"%.2f".format(Math.abs(net))}"
            )

            StatsUiState(
                isLoading = false,
                selectedRangeType = bounds.type,
                customStartDate = bounds.startDate,
                customEndDate = bounds.endDate,
                dateRangeLabel = bounds.label,
                summary = summary,
                categoryBreakdowns = breakdowns,
                errorMessage = error,
                isEmpty = error == null && income == 0.0 && expense == 0.0 && breakdowns.isEmpty()
            )
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = StatsUiState(isLoading = true)
    )

    fun onEvent(event: StatsUiEvent) {
        when (event) {
            is StatsUiEvent.OnDateRangeTypeSelected -> {
                _errorMessage.value = null
                _selectedRangeType.value = event.rangeType
            }
            is StatsUiEvent.OnCustomDateRangeSet -> {
                val (start, end) = MmrValidation.normalizeDateRange(event.startDate, event.endDate)
                if (!MmrValidation.isValidEpochMillis(start) || !MmrValidation.isValidEpochMillis(end)) {
                    _errorMessage.value = "Please choose a valid date range."
                    return
                }
                _errorMessage.value = null
                _customDateRange.value = Pair(start, end)
                _selectedRangeType.value = DateRangeType.CUSTOM
            }
            StatsUiEvent.OnRetryClick -> {
                _errorMessage.value = null
            }
        }
    }

    private data class DateRangeBounds(
        val type: DateRangeType,
        val startDate: Long,
        val endDate: Long,
        val label: String
    )

    companion object {
        private fun calculateDateRange(type: DateRangeType, customRange: Pair<Long, Long>?): DateRangeBounds {
            val cal = Calendar.getInstance()
            val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
            val monthFormat = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
            val yearFormat = SimpleDateFormat("yyyy", Locale.getDefault())

            return when (type) {
                DateRangeType.THIS_MONTH -> {
                    cal.set(Calendar.DAY_OF_MONTH, 1)
                    cal.set(Calendar.HOUR_OF_DAY, 0)
                    cal.set(Calendar.MINUTE, 0)
                    cal.set(Calendar.SECOND, 0)
                    cal.set(Calendar.MILLISECOND, 0)
                    val start = cal.timeInMillis

                    cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH))
                    cal.set(Calendar.HOUR_OF_DAY, 23)
                    cal.set(Calendar.MINUTE, 59)
                    cal.set(Calendar.SECOND, 59)
                    cal.set(Calendar.MILLISECOND, 999)
                    val end = cal.timeInMillis

                    DateRangeBounds(type, start, end, monthFormat.format(Date(start)))
                }

                DateRangeType.LAST_MONTH -> {
                    cal.add(Calendar.MONTH, -1)
                    cal.set(Calendar.DAY_OF_MONTH, 1)
                    cal.set(Calendar.HOUR_OF_DAY, 0)
                    cal.set(Calendar.MINUTE, 0)
                    cal.set(Calendar.SECOND, 0)
                    cal.set(Calendar.MILLISECOND, 0)
                    val start = cal.timeInMillis

                    cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH))
                    cal.set(Calendar.HOUR_OF_DAY, 23)
                    cal.set(Calendar.MINUTE, 59)
                    cal.set(Calendar.SECOND, 59)
                    cal.set(Calendar.MILLISECOND, 999)
                    val end = cal.timeInMillis

                    DateRangeBounds(type, start, end, monthFormat.format(Date(start)))
                }

                DateRangeType.THIS_YEAR -> {
                    cal.set(Calendar.DAY_OF_YEAR, 1)
                    cal.set(Calendar.HOUR_OF_DAY, 0)
                    cal.set(Calendar.MINUTE, 0)
                    cal.set(Calendar.SECOND, 0)
                    cal.set(Calendar.MILLISECOND, 0)
                    val start = cal.timeInMillis

                    cal.set(Calendar.DAY_OF_YEAR, cal.getActualMaximum(Calendar.DAY_OF_YEAR))
                    cal.set(Calendar.HOUR_OF_DAY, 23)
                    cal.set(Calendar.MINUTE, 59)
                    cal.set(Calendar.SECOND, 59)
                    cal.set(Calendar.MILLISECOND, 999)
                    val end = cal.timeInMillis

                    DateRangeBounds(type, start, end, "Year ${yearFormat.format(Date(start))}")
                }

                DateRangeType.ALL_TIME -> {
                    DateRangeBounds(type, 0L, Long.MAX_VALUE, "All Time")
                }

                DateRangeType.CUSTOM -> {
                    val range = if (customRange != null) {
                        MmrValidation.normalizeDateRange(customRange.first, customRange.second)
                    } else {
                        0L to System.currentTimeMillis()
                    }
                    val start = range.first
                    val end = range.second
                    val label = if (customRange != null) {
                        "${dateFormat.format(Date(start))} - ${dateFormat.format(Date(end))}"
                    } else {
                        "Custom Range"
                    }
                    DateRangeBounds(type, start, end, label)
                }
            }
        }
    }
}
