package com.mmr.feature.home.model

import com.mmr.core.model.settings.MmrAmountColorScheme
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth

enum class HomeViewTab {
    DAILY,
    CALENDAR,
    MONTHLY,
    TOTAL,
    ;

    val mmLabel: String
        get() =
            when (this) {
                DAILY -> "Daily"
                CALENDAR -> "Calendar"
                MONTHLY -> "Monthly"
                TOTAL -> "Total"
            }
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
    val calendarCells: List<CalendarDayUi> = emptyList(),
    val calendarSelectedDate: LocalDate? = null,
    val calendarSelectedDayGroup: TransactionGroupUi? = null,
    val calendarWeekStart: DayOfWeek = DayOfWeek.MONDAY,
    val monthlyPeriods: List<MonthlyPeriodUi> = emptyList(),
    val monthlyExpandedMonths: Set<YearMonth> = emptySet(),
    val totalAccountsSummary: HomeTotalAccountsSummaryUi? = null,
    val pendingApprovalCount: Int = 0,
    val errorMessage: String? = null,
    val isEmpty: Boolean = false,
    val isSelectionMode: Boolean = false,
    val selectedTransactionIds: Set<Long> = emptySet(),
    val showMergeDialog: Boolean = false,
    val mergeNameInput: String = "",
    val mergeErrorMessage: String? = null,
    val canMergeSelection: Boolean = false,
    val canDeleteSelection: Boolean = false,
    val allVisibleSelected: Boolean = false,
    val visibleSelectableCount: Int = 0,
    val showDeleteConfirmDialog: Boolean = false,
    val showExcelExportDialog: Boolean = false,
    val excelExportOptions: List<HomeExcelExportOptionUi> = emptyList(),
    val isExcelExporting: Boolean = false,
    val amountColorScheme: MmrAmountColorScheme = MmrAmountColorScheme.SET_B,
    val monthSwipeEnabled: Boolean = true,
    val showTransactionDescription: Boolean = true,
    val showTransactionTime: Boolean = false,
    val transactionTimeAscending: Boolean = true,
)

data class MonthlyPeriodUi(
    val yearMonth: YearMonth,
    val monthLabel: String,
    val rangeLabel: String,
    val income: Double,
    val expense: Double,
    val weeks: List<WeeklyPeriodUi>,
)

data class HomeTotalAccountsSummaryUi(
    val periodRangeLabel: String,
    val comparedExpensePercentLabel: String,
    val cashExpenseLabel: String,
    val cardExpenseLabel: String,
    val transferExpenseLabel: String,
    val showCardExpenseRow: Boolean = true,
    val accountExpense: Double,
    val cardExpense: Double,
    val transferAmount: Double,
)

data class WeeklyPeriodUi(
    val startDate: LocalDate,
    val endDate: LocalDate,
    val rangeLabel: String,
    val income: Double,
    val expense: Double,
)

data class CalendarDayUi(
    val date: LocalDate,
    val isCurrentMonth: Boolean,
    val expenseTotal: Double,
    val incomeTotal: Double,
    val transactionCount: Int,
) {
    val dayOfMonth: Int get() = date.dayOfMonth
}

data class HomeSummaryUi(
    val totalIncome: Double = 0.0,
    val totalExpense: Double = 0.0,
    val netBalance: Double = 0.0,
    /** Balance from before the current period when carry-over is enabled. */
    val carriedForward: Double = 0.0,
)

data class TransactionGroupUi(
    val dateKey: String,
    val dayOfMonth: Int,
    val weekdayShort: String,
    val monthYearLabel: String,
    val dayExpenseTotal: Double,
    val dayIncomeTotal: Double,
    val items: List<TransactionRowUi>,
)

data class TransactionRowUi(
    val id: Long,
    val title: String,
    val categoryLabel: String,
    val detailLine: String,
    val subtitle: String,
    /** e.g. "₹ 538.00" without sign prefix */
    val amountLabel: String,
    val amountFormatted: String,
    val isIncome: Boolean,
    val timeLabel: String,
)

enum class HomeTypeFilter {
    ALL,
    EXPENSE,
    INCOME,
    TRANSFER,
}

enum class HomeExcelExportPreset {
    CURRENT_MONTH,
    LAST_MONTH,
    CURRENT_YEAR,
    LAST_YEAR,
    ALL,
}

sealed interface HomeExcelExportRange {
    data class Bounded(
        val start: LocalDate,
        val end: LocalDate,
    ) : HomeExcelExportRange

    data object AllTime : HomeExcelExportRange
}

data class HomeExcelExportOptionUi(
    val preset: HomeExcelExportPreset,
    val label: String,
    val range: HomeExcelExportRange,
)

sealed interface HomeUiEvent {
    data class OnCategoryFilterSelected(
        val category: String?,
    ) : HomeUiEvent

    data class OnTypeFilterSelected(
        val filter: HomeTypeFilter,
    ) : HomeUiEvent

    data class OnViewTabSelected(
        val tab: HomeViewTab,
    ) : HomeUiEvent

    data class OnTransactionClick(
        val id: Long,
    ) : HomeUiEvent

    object OnPreviousMonth : HomeUiEvent

    object OnNextMonth : HomeUiEvent

    object OnOpenMonthPicker : HomeUiEvent

    object OnDismissMonthPicker : HomeUiEvent

    object OnMonthPickerPreviousYear : HomeUiEvent

    object OnMonthPickerNextYear : HomeUiEvent

    data class OnMonthPickerMonthSelected(
        val month: Int,
    ) : HomeUiEvent

    object OnMonthPickerThisMonth : HomeUiEvent

    object OnAddTransactionClick : HomeUiEvent

    object OnRefresh : HomeUiEvent

    object OnRetryClick : HomeUiEvent

    object OnEnterSelectionMode : HomeUiEvent

    object OnExitSelectionMode : HomeUiEvent

    data class OnToggleTransactionSelection(
        val id: Long,
    ) : HomeUiEvent

    object OnOpenMergeDialog : HomeUiEvent

    object OnDismissMergeDialog : HomeUiEvent

    data class OnMergeNameChanged(
        val name: String,
    ) : HomeUiEvent

    object OnConfirmMerge : HomeUiEvent

    object OnToggleSelectAllVisible : HomeUiEvent

    object OnOpenDeleteConfirmDialog : HomeUiEvent

    object OnDismissDeleteConfirmDialog : HomeUiEvent

    object OnConfirmDeleteSelected : HomeUiEvent

    data class OnCalendarDateSelected(
        val date: LocalDate,
    ) : HomeUiEvent

    object OnCalendarPreviousDay : HomeUiEvent

    object OnCalendarNextDay : HomeUiEvent

    data class OnMonthlyMonthToggle(
        val yearMonth: YearMonth,
    ) : HomeUiEvent

    object OnExportPeriodClick : HomeUiEvent

    object OnDismissExcelExportDialog : HomeUiEvent

    data class OnExcelExportPeriodSelected(
        val preset: HomeExcelExportPreset,
    ) : HomeUiEvent
}

data class HomePeriodExportRequest(
    val contentUri: android.net.Uri,
    val subject: String,
)
