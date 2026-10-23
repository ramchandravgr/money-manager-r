package com.mmr.feature.stats.model

import com.mmr.feature.home.model.TransactionGroupUi
import java.time.LocalDate

data class SubcategoryBreakdownUi(
    val subcategoryId: Long?,
    val name: String,
    val amount: Double,
    val amountFormatted: String,
    val percentage: Float,
    val percentageFormatted: String,
)

data class MonthlyChartPointUi(
    val label: String,
    val amount: Double,
)

data class StatsCategoryDetailUiState(
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val isSelectionMode: Boolean = false,
    val selectedTransactionIds: Set<Long> = emptySet(),
    val showDeleteConfirmDialog: Boolean = false,
    val siblingCategories: List<StatsSiblingCategoryUi> = emptyList(),
    val categoryId: Long = 0L,
    val transactionType: String = "",
    val searchSubcategoryId: Long = -1L,
    val categoryName: String = "",
    val categoryColorHex: String = "#6B7280",
    val periodMode: StatsPeriodMode = StatsPeriodMode.MONTHLY,
    val navigationLabel: String = "",
    val periodStartMillis: Long = 0L,
    val periodEndMillis: Long = 0L,
    val periodStartDate: LocalDate? = null,
    val periodEndDate: LocalDate? = null,
    val canNavigatePeriod: Boolean = true,
    val periodSwipeEnabled: Boolean = true,
    val totalFormatted: String = "₹ 0.00",
    val subcategoryBreakdowns: List<SubcategoryBreakdownUi> = emptyList(),
    val selectedSubcategoryId: Long? = null,
    val chartPoints: List<MonthlyChartPointUi> = emptyList(),
    val groupedTransactions: List<TransactionGroupUi> = emptyList(),
    val errorMessage: String? = null,
    val isEmpty: Boolean = false,
    val showPeriodStartPicker: Boolean = false,
    val showPeriodEndPicker: Boolean = false,
    val showMonthPicker: Boolean = false,
    val monthPickerYear: Int =
        java.time.YearMonth
            .now()
            .year,
)

sealed interface StatsCategoryDetailUiEvent {
    object OnPreviousPeriod : StatsCategoryDetailUiEvent

    object OnNextPeriod : StatsCategoryDetailUiEvent

    data class OnSubcategorySelected(
        val subcategoryId: Long?,
    ) : StatsCategoryDetailUiEvent

    object OnPeriodStartFieldClick : StatsCategoryDetailUiEvent

    object OnPeriodEndFieldClick : StatsCategoryDetailUiEvent

    object OnDismissPeriodStartPicker : StatsCategoryDetailUiEvent

    object OnDismissPeriodEndPicker : StatsCategoryDetailUiEvent

    data class OnCustomPeriodStartSelected(
        val dateMillis: Long,
    ) : StatsCategoryDetailUiEvent

    data class OnCustomPeriodEndSelected(
        val dateMillis: Long,
    ) : StatsCategoryDetailUiEvent

    data class OnPeriodModeSelected(
        val mode: StatsPeriodMode,
    ) : StatsCategoryDetailUiEvent

    object OnRetryClick : StatsCategoryDetailUiEvent

    object OnRefresh : StatsCategoryDetailUiEvent

    object OnEnterSelectionMode : StatsCategoryDetailUiEvent

    object OnExitSelectionMode : StatsCategoryDetailUiEvent

    data class OnToggleTransactionSelection(
        val transactionId: Long,
    ) : StatsCategoryDetailUiEvent

    object OnSelectAllTransactions : StatsCategoryDetailUiEvent

    object OnRequestDeleteSelected : StatsCategoryDetailUiEvent

    object OnDismissDeleteConfirm : StatsCategoryDetailUiEvent

    object OnConfirmDeleteSelected : StatsCategoryDetailUiEvent

    data class OnRecategorizeSelected(
        val targetCategoryId: Long,
    ) : StatsCategoryDetailUiEvent

    data class OnSiblingCategorySelected(
        val categoryId: Long,
    ) : StatsCategoryDetailUiEvent

    object OnOpenMonthPicker : StatsCategoryDetailUiEvent

    object OnDismissMonthPicker : StatsCategoryDetailUiEvent

    object OnMonthPickerPreviousYear : StatsCategoryDetailUiEvent

    object OnMonthPickerNextYear : StatsCategoryDetailUiEvent

    data class OnMonthPickerMonthSelected(
        val month: Int,
    ) : StatsCategoryDetailUiEvent

    object OnMonthPickerThisMonth : StatsCategoryDetailUiEvent
}
