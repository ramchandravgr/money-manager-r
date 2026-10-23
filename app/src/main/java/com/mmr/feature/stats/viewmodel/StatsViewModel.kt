package com.mmr.feature.stats.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mmr.app.ui.state.MmrUiMessages
import com.mmr.core.common.validation.MmrValidation
import com.mmr.core.data.preferences.AppDisplaySettingsStore
import com.mmr.core.domain.repository.TransactionRepository
import com.mmr.core.model.settings.MmrSwipeBehavior
import com.mmr.feature.home.HomePeriodUtils
import com.mmr.feature.stats.StatsPeriodUtils
import com.mmr.feature.stats.model.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class StatsViewModel
    @Inject
    constructor(
        private val transactionRepository: TransactionRepository,
        displaySettingsStore: AppDisplaySettingsStore,
    ) : ViewModel() {
        private val zoneId = ZoneId.systemDefault()
        private val displaySettings =
            displaySettingsStore.settings
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), displaySettingsStore.snapshot())

        private val _periodMode = MutableStateFlow(StatsPeriodMode.MONTHLY)
        private val _anchorDate = MutableStateFlow(LocalDate.now())
        private val _customPeriodStart = MutableStateFlow<LocalDate?>(null)
        private val _customPeriodEnd = MutableStateFlow<LocalDate?>(null)
        private val _ledgerTab = MutableStateFlow(StatsLedgerTab.EXPENSE)
        private val _errorMessage = MutableStateFlow<String?>(null)
        private val _showPeriodStartPicker = MutableStateFlow(false)
        private val _showPeriodEndPicker = MutableStateFlow(false)
        private val _showMonthPicker = MutableStateFlow(false)
        private val _monthPickerYear = MutableStateFlow(YearMonth.now().year)
        private val _refreshTick = MutableStateFlow(0)
        private val _isRefreshing = MutableStateFlow(false)

        private val periodAnchors =
            combine(
                _periodMode,
                _anchorDate,
                _customPeriodStart,
                _customPeriodEnd,
            ) { mode, anchor, customStart, customEnd ->
                PeriodAnchors(mode, anchor, customStart, customEnd)
            }

        private val periodConfig =
            combine(periodAnchors, displaySettings, _refreshTick) { anchors, display, _ ->
                StatsPeriodUtils.resolveBounds(
                    mode = anchors.mode,
                    anchorDate = anchors.anchor,
                    customStart = anchors.customStart,
                    customEnd = anchors.customEnd,
                    zoneId = zoneId,
                    monthStartDay = display.monthStartDay,
                    weekStartDay = HomePeriodUtils.toDayOfWeek(display.weekStartDay),
                )
            }

        private val shellFlags =
            combine(
                _ledgerTab,
                _errorMessage,
                _showPeriodStartPicker,
                _showPeriodEndPicker,
                _showMonthPicker,
            ) { ledgerTab, error, showStartPicker, showEndPicker, showMonthPicker ->
                ShellFlags(ledgerTab, error, showStartPicker, showEndPicker, showMonthPicker)
            }

        val uiState: StateFlow<StatsUiState> =
            combine(
                periodConfig,
                shellFlags,
                _monthPickerYear,
                _isRefreshing,
            ) { bounds, flags, pickerYear, refreshing ->
                PeriodShell(
                    bounds = bounds,
                    ledgerTab = flags.ledgerTab,
                    error = flags.error,
                    showStartPicker = flags.showStartPicker,
                    showEndPicker = flags.showEndPicker,
                    showMonthPicker = flags.showMonthPicker,
                    pickerYear = pickerYear,
                    refreshing = refreshing,
                )
            }.flatMapLatest { shell ->
                val bounds = shell.bounds
                combine(
                    transactionRepository
                        .getTotalIncomeBetween(bounds.startMillis, bounds.endMillis)
                        .catch { emit(0.0) },
                    transactionRepository
                        .getTotalExpenseBetween(bounds.startMillis, bounds.endMillis)
                        .catch { emit(0.0) },
                    transactionRepository
                        .getCategorySpendingBetween(bounds.startMillis, bounds.endMillis)
                        .catch { e ->
                            _errorMessage.value = MmrUiMessages.friendly(e.message, MmrUiMessages.GENERIC_LOAD)
                            emit(emptyList())
                        },
                    transactionRepository
                        .getCategoryIncomeBetween(bounds.startMillis, bounds.endMillis)
                        .catch { e ->
                            _errorMessage.value = MmrUiMessages.friendly(e.message, MmrUiMessages.GENERIC_LOAD)
                            emit(emptyList())
                        },
                ) { incomeVal, expenseVal, expenseSpendings, incomeSpendings ->
                    val income = incomeVal ?: 0.0
                    val expense = expenseVal ?: 0.0
                    val net = income - expense
                    val expenseBreakdowns = buildCategoryBreakdowns(expenseSpendings, expense)
                    val incomeBreakdowns = buildCategoryBreakdowns(incomeSpendings, income)
                    val activeBreakdowns =
                        when (shell.ledgerTab) {
                            StatsLedgerTab.INCOME -> incomeBreakdowns
                            StatsLedgerTab.EXPENSE -> expenseBreakdowns
                        }
                    val tabTotal = if (shell.ledgerTab == StatsLedgerTab.INCOME) income else expense

                    val netPrefix = if (net >= 0) "+₹" else "-₹"
                    val summary =
                        StatsSummaryUi(
                            totalIncome = income,
                            totalExpense = expense,
                            netBalance = net,
                            totalIncomeFormatted = formatLedgerTabAmount(income, isIncome = true),
                            totalExpenseFormatted = formatLedgerTabAmount(expense, isIncome = false),
                            netBalanceFormatted = "$netPrefix${"%.2f".format(kotlin.math.abs(net))}",
                        )

                    val mode = _periodMode.value
                    StatsUiState(
                        isLoading = false,
                        isRefreshing = shell.refreshing,
                        periodMode = mode,
                        ledgerTab = shell.ledgerTab,
                        navigationLabel = bounds.navigationLabel,
                        periodStartMillis = bounds.startMillis,
                        periodEndMillis = bounds.endMillis,
                        periodStartDate = bounds.startDate,
                        periodEndDate = bounds.endDate,
                        summary = summary,
                        incomeBreakdowns = incomeBreakdowns,
                        expenseBreakdowns = expenseBreakdowns,
                        categoryBreakdowns = activeBreakdowns,
                        activeTabTotalFormatted = "₹ ${"%.2f".format(tabTotal)}",
                        errorMessage = shell.error,
                        isEmpty = shell.error == null && tabTotal == 0.0 && activeBreakdowns.isEmpty(),
                        incomeTabEmpty = shell.error == null && income == 0.0 && incomeBreakdowns.isEmpty(),
                        expenseTabEmpty = shell.error == null && expense == 0.0 && expenseBreakdowns.isEmpty(),
                        showPeriodStartPicker = shell.showStartPicker,
                        showPeriodEndPicker = shell.showEndPicker,
                        showMonthPicker = shell.showMonthPicker,
                        monthPickerYear = shell.pickerYear,
                        canNavigatePeriod = mode != StatsPeriodMode.PERIOD,
                        periodSwipeEnabled = displaySettings.value.swipeBehavior == MmrSwipeBehavior.CHANGE_DATE,
                    )
                }
            }.stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = StatsUiState(isLoading = true),
            )

        fun onEvent(event: StatsUiEvent) {
            when (event) {
                is StatsUiEvent.OnPeriodModeSelected -> {
                    _errorMessage.value = null
                    _periodMode.value = event.mode
                    if (event.mode == StatsPeriodMode.PERIOD &&
                        _customPeriodStart.value == null &&
                        _customPeriodEnd.value == null
                    ) {
                        val today = LocalDate.now()
                        _customPeriodStart.value = today.minusDays(6)
                        _customPeriodEnd.value = today
                    }
                }
                is StatsUiEvent.OnLedgerTabSelected -> {
                    _ledgerTab.value = event.tab
                }
                StatsUiEvent.OnPreviousPeriod -> shiftPeriod(forward = false)
                StatsUiEvent.OnNextPeriod -> shiftPeriod(forward = true)
                StatsUiEvent.OnPeriodStartFieldClick -> _showPeriodStartPicker.value = true
                StatsUiEvent.OnPeriodEndFieldClick -> _showPeriodEndPicker.value = true
                StatsUiEvent.OnDismissPeriodStartPicker -> _showPeriodStartPicker.value = false
                StatsUiEvent.OnDismissPeriodEndPicker -> _showPeriodEndPicker.value = false
                is StatsUiEvent.OnCustomPeriodStartSelected -> {
                    if (!MmrValidation.isValidEpochMillis(event.dateMillis)) {
                        _errorMessage.value = "Please choose a valid start date."
                        return
                    }
                    _errorMessage.value = null
                    _customPeriodStart.value = millisToLocalDate(event.dateMillis)
                    _showPeriodStartPicker.value = false
                    _periodMode.value = StatsPeriodMode.PERIOD
                }
                is StatsUiEvent.OnCustomPeriodEndSelected -> {
                    if (!MmrValidation.isValidEpochMillis(event.dateMillis)) {
                        _errorMessage.value = "Please choose a valid end date."
                        return
                    }
                    _errorMessage.value = null
                    _customPeriodEnd.value = millisToLocalDate(event.dateMillis)
                    _showPeriodEndPicker.value = false
                    _periodMode.value = StatsPeriodMode.PERIOD
                }
                StatsUiEvent.OnRetryClick -> _errorMessage.value = null
                StatsUiEvent.OnRefresh -> triggerRefresh()
                StatsUiEvent.OnOpenMonthPicker -> {
                    _monthPickerYear.value = _anchorDate.value.year
                    _showMonthPicker.value = true
                }
                StatsUiEvent.OnDismissMonthPicker -> _showMonthPicker.value = false
                StatsUiEvent.OnMonthPickerPreviousYear -> _monthPickerYear.update { it - 1 }
                StatsUiEvent.OnMonthPickerNextYear -> _monthPickerYear.update { it + 1 }
                is StatsUiEvent.OnMonthPickerMonthSelected -> {
                    val month = event.month.coerceIn(1, 12)
                    val day =
                        when (_periodMode.value) {
                            StatsPeriodMode.ANNUALLY -> _anchorDate.value.dayOfMonth.coerceAtMost(28)
                            else ->
                                _anchorDate.value.dayOfMonth.coerceAtMost(
                                    YearMonth.of(_monthPickerYear.value, month).lengthOfMonth(),
                                )
                        }
                    _anchorDate.value = LocalDate.of(_monthPickerYear.value, month, day)
                    _showMonthPicker.value = false
                }
                StatsUiEvent.OnMonthPickerThisMonth -> {
                    _anchorDate.value = LocalDate.now()
                    _showMonthPicker.value = false
                }
            }
        }

        private fun triggerRefresh() {
            viewModelScope.launch {
                _isRefreshing.value = true
                _refreshTick.update { it + 1 }
                _isRefreshing.value = false
            }
        }

        fun drillDownFor(
            breakdown: CategoryBreakdownUi,
            ledgerTab: StatsLedgerTab,
        ): StatsCategoryDrillDown? {
            val state = uiState.value
            val categoryId = breakdown.categoryId ?: return null
            val type =
                when (ledgerTab) {
                    StatsLedgerTab.INCOME -> com.mmr.core.model.enums.TransactionType.INCOME
                    StatsLedgerTab.EXPENSE -> com.mmr.core.model.enums.TransactionType.EXPENSE
                }
            return StatsCategoryDrillDown(
                categoryId = categoryId,
                startMillis = state.periodStartMillis,
                endMillis = state.periodEndMillis.coerceAtMost(System.currentTimeMillis()),
                transactionType = type,
                periodMode = _periodMode.value,
                anchorDate = _anchorDate.value,
                customStartDate = _customPeriodStart.value,
                customEndDate = _customPeriodEnd.value,
            )
        }

        fun ledgerTabLabel(
            tab: StatsLedgerTab,
            summary: StatsSummaryUi,
        ): String =
            when (tab) {
                StatsLedgerTab.INCOME -> "Income  ${summary.totalIncomeFormatted}"
                StatsLedgerTab.EXPENSE -> "Expenses  ${summary.totalExpenseFormatted}"
            }

        private fun formatLedgerTabAmount(
            amount: Double,
            isIncome: Boolean,
        ): String {
            val prefix = if (isIncome) "+₹" else "-₹"
            return "$prefix${"%.2f".format(amount)}"
        }

        private fun buildCategoryBreakdowns(
            spendings: List<com.mmr.core.database.model.CategorySpending>,
            tabTotal: Double,
        ): List<CategoryBreakdownUi> {
            if (tabTotal <= 0.0 && spendings.isEmpty()) return emptyList()
            val categorizedTotal = spendings.sumOf { it.totalAmount }
            val denominator = if (tabTotal > 0) tabTotal else categorizedTotal
            if (denominator <= 0.0) return emptyList()

            val rows =
                spendings.map { spending ->
                    toBreakdownUi(spending, denominator)
                }
            val remainder = tabTotal - categorizedTotal
            if (tabTotal > 0 && remainder > 0.01) {
                val pct = (remainder / denominator).toFloat()
                return rows +
                    CategoryBreakdownUi(
                        categoryId = null,
                        categoryName = "Other",
                        colorHex = "#9CA3AF",
                        amount = remainder,
                        amountFormatted = "₹ ${"%.2f".format(remainder)}",
                        percentage = pct,
                        percentageFormatted = "${"%.0f".format(pct * 100)}%",
                        transactionCount = 0,
                    )
            }
            return rows
        }

        private fun toBreakdownUi(
            spending: com.mmr.core.database.model.CategorySpending,
            denominator: Double,
        ): CategoryBreakdownUi {
            val percentage = (spending.totalAmount / denominator).toFloat()
            return CategoryBreakdownUi(
                categoryId = spending.categoryId,
                categoryName = spending.categoryName ?: "Uncategorized",
                colorHex = spending.colorHex ?: "#6B7280",
                amount = spending.totalAmount,
                amountFormatted = "₹ ${"%.2f".format(spending.totalAmount)}",
                percentage = percentage,
                percentageFormatted = "${"%.0f".format(percentage * 100)}%",
                transactionCount = spending.transactionCount,
            )
        }

        private fun shiftPeriod(forward: Boolean) {
            if (_periodMode.value == StatsPeriodMode.PERIOD) return
            _anchorDate.value =
                StatsPeriodUtils.shiftAnchor(
                    _periodMode.value,
                    _anchorDate.value,
                    forward,
                )
        }

        private fun millisToLocalDate(millis: Long): LocalDate =
            Instant.ofEpochMilli(millis).atZone(zoneId).toLocalDate()

        private data class PeriodAnchors(
            val mode: StatsPeriodMode,
            val anchor: LocalDate,
            val customStart: LocalDate?,
            val customEnd: LocalDate?,
        )

        private data class ShellFlags(
            val ledgerTab: StatsLedgerTab,
            val error: String?,
            val showStartPicker: Boolean,
            val showEndPicker: Boolean,
            val showMonthPicker: Boolean,
        )

        private data class PeriodShell(
            val bounds: StatsPeriodUtils.PeriodBounds,
            val ledgerTab: StatsLedgerTab,
            val error: String?,
            val showStartPicker: Boolean,
            val showEndPicker: Boolean,
            val showMonthPicker: Boolean,
            val pickerYear: Int,
            val refreshing: Boolean,
        )
    }
