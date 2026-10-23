package com.mmr.feature.stats.model

import com.mmr.core.model.enums.TransactionType

enum class StatsPeriodMode {
    WEEKLY,
    MONTHLY,
    ANNUALLY,
    PERIOD,
}

enum class StatsLedgerTab {
    INCOME,
    EXPENSE,
}

data class CategoryBreakdownUi(
    val categoryId: Long?,
    val categoryName: String,
    val colorHex: String,
    val amount: Double,
    val amountFormatted: String,
    val percentage: Float,
    val percentageFormatted: String,
    val transactionCount: Int,
)

data class StatsSummaryUi(
    val totalIncome: Double = 0.0,
    val totalExpense: Double = 0.0,
    val netBalance: Double = 0.0,
    val totalIncomeFormatted: String = "₹0.00",
    val totalExpenseFormatted: String = "₹0.00",
    val netBalanceFormatted: String = "₹0.00",
)

data class StatsUiState(
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val periodMode: StatsPeriodMode = StatsPeriodMode.MONTHLY,
    val ledgerTab: StatsLedgerTab = StatsLedgerTab.EXPENSE,
    val navigationLabel: String = "",
    val periodStartMillis: Long = 0L,
    val periodEndMillis: Long = 0L,
    val periodStartDate: java.time.LocalDate? = null,
    val periodEndDate: java.time.LocalDate? = null,
    val summary: StatsSummaryUi = StatsSummaryUi(),
    val incomeBreakdowns: List<CategoryBreakdownUi> = emptyList(),
    val expenseBreakdowns: List<CategoryBreakdownUi> = emptyList(),
    val categoryBreakdowns: List<CategoryBreakdownUi> = emptyList(),
    val activeTabTotalFormatted: String = "₹ 0.00",
    val errorMessage: String? = null,
    val isEmpty: Boolean = false,
    val incomeTabEmpty: Boolean = false,
    val expenseTabEmpty: Boolean = false,
    val showPeriodStartPicker: Boolean = false,
    val showPeriodEndPicker: Boolean = false,
    val showMonthPicker: Boolean = false,
    val monthPickerYear: Int =
        java.time.YearMonth
            .now()
            .year,
    val canNavigatePeriod: Boolean = true,
    val periodSwipeEnabled: Boolean = true,
)

sealed interface StatsUiEvent {
    data class OnPeriodModeSelected(
        val mode: StatsPeriodMode,
    ) : StatsUiEvent

    data class OnLedgerTabSelected(
        val tab: StatsLedgerTab,
    ) : StatsUiEvent

    object OnPreviousPeriod : StatsUiEvent

    object OnNextPeriod : StatsUiEvent

    data class OnCustomPeriodStartSelected(
        val dateMillis: Long,
    ) : StatsUiEvent

    data class OnCustomPeriodEndSelected(
        val dateMillis: Long,
    ) : StatsUiEvent

    object OnDismissPeriodStartPicker : StatsUiEvent

    object OnDismissPeriodEndPicker : StatsUiEvent

    object OnPeriodStartFieldClick : StatsUiEvent

    object OnPeriodEndFieldClick : StatsUiEvent

    object OnRetryClick : StatsUiEvent

    object OnRefresh : StatsUiEvent

    object OnOpenMonthPicker : StatsUiEvent

    object OnDismissMonthPicker : StatsUiEvent

    object OnMonthPickerPreviousYear : StatsUiEvent

    object OnMonthPickerNextYear : StatsUiEvent

    data class OnMonthPickerMonthSelected(
        val month: Int,
    ) : StatsUiEvent

    object OnMonthPickerThisMonth : StatsUiEvent
}

data class StatsSiblingCategoryUi(
    val categoryId: Long,
    val name: String,
    val colorHex: String,
    val amount: Double,
)

data class StatsCategoryDrillDown(
    val categoryId: Long,
    val startMillis: Long,
    val endMillis: Long,
    val transactionType: TransactionType,
    val periodMode: StatsPeriodMode,
    val anchorDate: java.time.LocalDate,
    val customStartDate: java.time.LocalDate?,
    val customEndDate: java.time.LocalDate?,
)
