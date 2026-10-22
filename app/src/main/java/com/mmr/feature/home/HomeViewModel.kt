package com.mmr.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mmr.app.ui.state.MmrUiMessages
import com.mmr.core.common.validation.MmrValidation
import com.mmr.core.database.relation.TransactionWithDetails
import com.mmr.core.domain.repository.ApprovalRepository
import com.mmr.core.domain.repository.CategoryRepository
import com.mmr.core.domain.repository.TransactionRepository
import com.mmr.core.model.enums.TransactionType
import com.mmr.feature.home.model.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import java.text.SimpleDateFormat
import java.time.YearMonth
import java.time.ZoneId
import java.util.*
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val categoryRepository: CategoryRepository,
    private val approvalRepository: ApprovalRepository
) : ViewModel() {

    private val zoneId = ZoneId.systemDefault()

    private val _yearMonth = MutableStateFlow(YearMonth.now())
    private val _viewTab = MutableStateFlow(HomeViewTab.DAILY)
    private val _selectedCategoryFilter = MutableStateFlow<String?>(null)
    private val _selectedTypeFilter = MutableStateFlow(HomeTypeFilter.ALL)
    private val _isRefreshing = MutableStateFlow(false)
    private val _errorMessage = MutableStateFlow<String?>(null)
    private val _showMonthPicker = MutableStateFlow(false)
    private val _monthPickerYear = MutableStateFlow(YearMonth.now().year)

    private data class MonthAggregate(
        val income: Double,
        val expense: Double,
        val categorySpending: List<com.mmr.core.database.model.CategorySpending>
    )

    private val transactionsAndCategories = combine(
        transactionRepository.getApprovedTransactionsDetailed()
            .catch { e ->
                _errorMessage.value = MmrUiMessages.friendly(e.message, MmrUiMessages.GENERIC_LOAD)
                emit(emptyList())
            },
        categoryRepository.getActiveCategories()
            .catch { e ->
                _errorMessage.value = MmrUiMessages.friendly(e.message, MmrUiMessages.GENERIC_LOAD)
                emit(emptyList())
            }
    ) { transactions, categories -> transactions to categories }

    private val monthAggregate = _yearMonth.flatMapLatest { yearMonth ->
        val (start, end) = HomePeriodUtils.epochRangeMillis(yearMonth, zoneId)
        combine(
            transactionRepository.getTotalIncomeBetween(start, end).catch { emit(null) },
            transactionRepository.getTotalExpenseBetween(start, end).catch { emit(null) },
            transactionRepository.getCategorySpendingBetween(start, end).catch { emit(emptyList()) }
        ) { income, expense, spending ->
            MonthAggregate(
                income = income ?: 0.0,
                expense = expense ?: 0.0,
                categorySpending = spending
            )
        }
    }

    private val allTimeTotals = combine(
        transactionRepository.getTotalIncome().catch { emit(null) },
        transactionRepository.getTotalExpense().catch { emit(null) }
    ) { income, expense -> (income ?: 0.0) to (expense ?: 0.0) }

    private val filterState = combine(
        combine(
            _selectedCategoryFilter,
            _selectedTypeFilter,
            _isRefreshing
        ) { selectedCategory, selectedType, refreshing ->
            FilterSnapshotPartial(
                selectedCategory = selectedCategory,
                selectedType = selectedType,
                refreshing = refreshing
            )
        },
        combine(
            _errorMessage,
            _yearMonth,
            _viewTab,
            _showMonthPicker,
            _monthPickerYear
        ) { error, yearMonth, viewTab, showMonthPicker, monthPickerYear ->
            FilterSnapshotMeta(
                error = error,
                yearMonth = yearMonth,
                viewTab = viewTab,
                showMonthPicker = showMonthPicker,
                monthPickerYear = monthPickerYear
            )
        }
    ) { partial, meta ->
        FilterSnapshot(
            selectedCategory = partial.selectedCategory,
            selectedType = partial.selectedType,
            refreshing = partial.refreshing,
            error = meta.error,
            yearMonth = meta.yearMonth,
            viewTab = meta.viewTab,
            showMonthPicker = meta.showMonthPicker,
            monthPickerYear = meta.monthPickerYear
        )
    }

    private data class FilterSnapshotMeta(
        val error: String?,
        val yearMonth: YearMonth,
        val viewTab: HomeViewTab,
        val showMonthPicker: Boolean,
        val monthPickerYear: Int
    )

    private data class FilterSnapshotPartial(
        val selectedCategory: String?,
        val selectedType: HomeTypeFilter,
        val refreshing: Boolean
    )

    private data class FilterSnapshot(
        val selectedCategory: String?,
        val selectedType: HomeTypeFilter,
        val refreshing: Boolean,
        val error: String?,
        val yearMonth: YearMonth,
        val viewTab: HomeViewTab,
        val showMonthPicker: Boolean,
        val monthPickerYear: Int
    )

    private val pendingApprovalCount = approvalRepository.getPendingCount()
        .catch { emit(0) }

    val uiState: StateFlow<HomeUiState> = combine(
        transactionsAndCategories,
        monthAggregate,
        allTimeTotals,
        filterState,
        pendingApprovalCount
    ) { txAndCats, monthData, allTime, filters, pendingCount ->
        val (allTransactions, categories) = txAndCats
        val (allTimeIncome, allTimeExpense) = allTime

        val monthScoped = allTransactions.filter { item ->
            HomePeriodUtils.isInMonth(item.transaction.dateTime, filters.yearMonth, zoneId)
        }

        val baseList = when (filters.viewTab) {
            HomeViewTab.TOTAL -> allTransactions
            else -> monthScoped
        }

        val filtered = applyFilters(
            items = baseList,
            selectedCategory = filters.selectedCategory,
            selectedType = filters.selectedType,
            notesOnly = filters.viewTab == HomeViewTab.NOTE
        )

        val summary = when (filters.viewTab) {
            HomeViewTab.TOTAL -> HomeSummaryUi(
                totalIncome = allTimeIncome,
                totalExpense = allTimeExpense,
                netBalance = allTimeIncome - allTimeExpense
            )
            else -> HomeSummaryUi(
                totalIncome = monthData.income,
                totalExpense = monthData.expense,
                netBalance = monthData.income - monthData.expense
            )
        }

        val grouped = when (filters.viewTab) {
            HomeViewTab.DAILY, HomeViewTab.TOTAL -> TransactionGrouping.groupByDate(filtered, zoneId)
            else -> emptyList()
        }

        val calendarDays = when (filters.viewTab) {
            HomeViewTab.CALENDAR -> buildCalendarDays(monthScoped, filters.yearMonth)
            else -> emptyList()
        }

        val monthlyRows = when (filters.viewTab) {
            HomeViewTab.MONTHLY -> monthData.categorySpending
            else -> emptyList()
        }

        val noteRows = when (filters.viewTab) {
            HomeViewTab.NOTE -> filtered.map { toTransactionRow(it) }
            else -> emptyList()
        }

        val isEmpty = when (filters.viewTab) {
            HomeViewTab.DAILY, HomeViewTab.TOTAL -> filters.error == null && grouped.isEmpty()
            HomeViewTab.CALENDAR -> filters.error == null && calendarDays.none { it.transactionCount > 0 }
            HomeViewTab.MONTHLY -> filters.error == null && monthlyRows.isEmpty()
            HomeViewTab.NOTE -> filters.error == null && noteRows.isEmpty()
        }

        HomeUiState(
            isLoading = false,
            isRefreshing = filters.refreshing,
            selectedViewTab = filters.viewTab,
            monthTitle = HomePeriodUtils.monthTitle(filters.yearMonth),
            showMonthPicker = filters.showMonthPicker,
            monthPickerYear = filters.monthPickerYear,
            selectedYear = filters.yearMonth.year,
            selectedMonth = filters.yearMonth.monthValue,
            selectedCategoryFilter = filters.selectedCategory,
            selectedTypeFilter = filters.selectedType,
            categories = categories.map { it.name },
            summary = summary,
            groupedTransactions = grouped,
            calendarDays = calendarDays,
            monthlyCategoryRows = monthlyRows,
            noteTransactions = noteRows,
            pendingApprovalCount = pendingCount,
            errorMessage = filters.error,
            isEmpty = isEmpty
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = HomeUiState(isLoading = true)
    )

    fun onEvent(event: HomeUiEvent) {
        when (event) {
            is HomeUiEvent.OnCategoryFilterSelected -> {
                _selectedCategoryFilter.value =
                    if (_selectedCategoryFilter.value == event.category) null else event.category
            }

            is HomeUiEvent.OnTypeFilterSelected -> {
                _selectedTypeFilter.value = event.filter
            }

            is HomeUiEvent.OnViewTabSelected -> {
                _viewTab.value = event.tab
            }

            HomeUiEvent.OnPreviousMonth -> {
                _yearMonth.update { it.minusMonths(1) }
            }

            HomeUiEvent.OnNextMonth -> {
                _yearMonth.update { it.plusMonths(1) }
            }

            HomeUiEvent.OnOpenMonthPicker -> {
                _monthPickerYear.value = _yearMonth.value.year
                _showMonthPicker.value = true
            }

            HomeUiEvent.OnDismissMonthPicker -> {
                _showMonthPicker.value = false
            }

            HomeUiEvent.OnMonthPickerPreviousYear -> {
                _monthPickerYear.update { it - 1 }
            }

            HomeUiEvent.OnMonthPickerNextYear -> {
                _monthPickerYear.update { it + 1 }
            }

            is HomeUiEvent.OnMonthPickerMonthSelected -> {
                _yearMonth.value = YearMonth.of(_monthPickerYear.value, event.month)
                _showMonthPicker.value = false
            }

            HomeUiEvent.OnMonthPickerThisMonth -> {
                val now = YearMonth.now()
                _yearMonth.value = now
                _monthPickerYear.value = now.year
                _showMonthPicker.value = false
            }

            HomeUiEvent.OnRefresh, HomeUiEvent.OnRetryClick -> {
                _errorMessage.value = null
                _isRefreshing.value = true
                _isRefreshing.value = false
            }

            HomeUiEvent.OnAddTransactionClick,
            is HomeUiEvent.OnTransactionClick -> Unit
        }
    }

    private fun applyFilters(
        items: List<TransactionWithDetails>,
        selectedCategory: String?,
        selectedType: HomeTypeFilter,
        notesOnly: Boolean
    ): List<TransactionWithDetails> {
        return items.filter { item ->
            if (notesOnly) {
                val note = item.transaction.note?.trim()
                if (note.isNullOrEmpty()) return@filter false
            }

            val matchesCategory = selectedCategory == null || item.category?.name == selectedCategory

            val matchesType = when (selectedType) {
                HomeTypeFilter.ALL -> true
                HomeTypeFilter.EXPENSE -> item.transaction.type == TransactionType.EXPENSE
                HomeTypeFilter.INCOME -> item.transaction.type == TransactionType.INCOME
                HomeTypeFilter.TRANSFER -> item.transaction.type == TransactionType.TRANSFER
            }

            matchesCategory && matchesType
        }
    }

    private fun buildCalendarDays(
        monthTransactions: List<TransactionWithDetails>,
        yearMonth: YearMonth
    ): List<CalendarDayUi> {
        val daysInMonth = yearMonth.lengthOfMonth()
        val totals = DoubleArray(daysInMonth + 1)
        val counts = IntArray(daysInMonth + 1)

        val calendar = Calendar.getInstance()
        for (item in monthTransactions) {
            if (!MmrValidation.isValidEpochMillis(item.transaction.dateTime)) continue
            calendar.timeInMillis = item.transaction.dateTime
            val day = calendar.get(Calendar.DAY_OF_MONTH)
            if (day < 1 || day > daysInMonth) continue
            counts[day]++
            val delta = when (item.transaction.type) {
                TransactionType.INCOME -> item.transaction.amount
                TransactionType.EXPENSE -> item.transaction.amount.unaryMinus()
                TransactionType.TRANSFER,
                TransactionType.LOAN_PAYMENT,
                TransactionType.ADJUSTMENT -> 0.0
            }
            totals[day] += delta
        }

        return (1..daysInMonth).map { day ->
            CalendarDayUi(
                dayOfMonth = day,
                label = day.toString(),
                netTotal = totals[day],
                transactionCount = counts[day]
            )
        }
    }

    private fun toTransactionRow(item: TransactionWithDetails): TransactionRowUi {
        val timeFormat = SimpleDateFormat("dd MMM • hh:mm a", Locale.getDefault())
        val isIncome = item.transaction.type == TransactionType.INCOME
        val sign = if (isIncome) "+" else "-"
        val categoryName = item.category?.name ?: "Uncategorized"
        val accountName = item.account?.name ?: "Account"
        val title = item.transaction.note?.trim()?.takeIf { it.isNotEmpty() } ?: categoryName
        return TransactionRowUi(
            id = item.transaction.id,
            title = title,
            subtitle = "$categoryName • $accountName",
            amountFormatted = "$sign₹${"%.2f".format(item.transaction.amount)}",
            isIncome = isIncome,
            timeLabel = timeFormat.format(Date(item.transaction.dateTime))
        )
    }

}
