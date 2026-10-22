package com.mmr.feature.stats.model

enum class DateRangeType {
    THIS_MONTH,
    LAST_MONTH,
    THIS_YEAR,
    ALL_TIME,
    CUSTOM
}

data class CategoryBreakdownUi(
    val categoryId: Long?,
    val categoryName: String,
    val colorHex: String,
    val amount: Double,
    val amountFormatted: String,
    val percentage: Float,
    val percentageFormatted: String,
    val transactionCount: Int
)

data class StatsSummaryUi(
    val totalIncome: Double = 0.0,
    val totalExpense: Double = 0.0,
    val netBalance: Double = 0.0,
    val totalIncomeFormatted: String = "₹0.00",
    val totalExpenseFormatted: String = "₹0.00",
    val netBalanceFormatted: String = "₹0.00"
)

data class StatsUiState(
    val isLoading: Boolean = true,
    val selectedRangeType: DateRangeType = DateRangeType.THIS_MONTH,
    val customStartDate: Long = 0L,
    val customEndDate: Long = 0L,
    val dateRangeLabel: String = "",
    val summary: StatsSummaryUi = StatsSummaryUi(),
    val categoryBreakdowns: List<CategoryBreakdownUi> = emptyList(),
    val errorMessage: String? = null,
    val isEmpty: Boolean = false
)

sealed interface StatsUiEvent {
    data class OnDateRangeTypeSelected(val rangeType: DateRangeType) : StatsUiEvent
    data class OnCustomDateRangeSet(val startDate: Long, val endDate: Long) : StatsUiEvent
    object OnRetryClick : StatsUiEvent
}
