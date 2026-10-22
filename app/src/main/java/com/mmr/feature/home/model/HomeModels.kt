package com.mmr.feature.home.model

import com.mmr.core.database.model.CategorySpending

enum class HomeViewTab {
    DAILY,
    CALENDAR,
    MONTHLY,
    TOTAL,
    NOTE
}

data class HomeUiState(
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val selectedViewTab: HomeViewTab = HomeViewTab.DAILY,
    val monthTitle: String = "",
    val showMonthPicker: Boolean = false,
    val monthPickerYear: Int = 0,
    val selectedYear: Int = 0,
    val selectedMonth: Int = 0,
    val selectedCategoryFilter: String? = null,
    val selectedTypeFilter: HomeTypeFilter = HomeTypeFilter.ALL,
    val categories: List<String> = emptyList(),
    val summary: HomeSummaryUi = HomeSummaryUi(),
    val groupedTransactions: List<TransactionGroupUi> = emptyList(),
    val calendarDays: List<CalendarDayUi> = emptyList(),
    val monthlyCategoryRows: List<CategorySpending> = emptyList(),
    val noteTransactions: List<TransactionRowUi> = emptyList(),
    val pendingApprovalCount: Int = 0,
    val errorMessage: String? = null,
    val isEmpty: Boolean = false
)

data class CalendarDayUi(
    val dayOfMonth: Int,
    val label: String,
    val netTotal: Double,
    val transactionCount: Int
)

data class HomeSummaryUi(
    val totalIncome: Double = 0.0,
    val totalExpense: Double = 0.0,
    val netBalance: Double = 0.0
)

data class TransactionGroupUi(
    val dateLabel: String,
    val dayTotal: Double,
    val items: List<TransactionRowUi>
)

data class TransactionRowUi(
    val id: Long,
    val title: String,
    val subtitle: String,
    val amountFormatted: String,
    val isIncome: Boolean,
    val timeLabel: String
)

enum class HomeTypeFilter {
    ALL, EXPENSE, INCOME, TRANSFER
}

sealed interface HomeUiEvent {
    data class OnCategoryFilterSelected(val category: String?) : HomeUiEvent
    data class OnTypeFilterSelected(val filter: HomeTypeFilter) : HomeUiEvent
    data class OnViewTabSelected(val tab: HomeViewTab) : HomeUiEvent
    data class OnTransactionClick(val id: Long) : HomeUiEvent
    object OnPreviousMonth : HomeUiEvent
    object OnNextMonth : HomeUiEvent
    object OnOpenMonthPicker : HomeUiEvent
    object OnDismissMonthPicker : HomeUiEvent
    object OnMonthPickerPreviousYear : HomeUiEvent
    object OnMonthPickerNextYear : HomeUiEvent
    data class OnMonthPickerMonthSelected(val month: Int) : HomeUiEvent
    object OnMonthPickerThisMonth : HomeUiEvent
    object OnAddTransactionClick : HomeUiEvent
    object OnRefresh : HomeUiEvent
    object OnRetryClick : HomeUiEvent
}
