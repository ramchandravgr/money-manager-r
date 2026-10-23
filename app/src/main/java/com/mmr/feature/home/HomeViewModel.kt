package com.mmr.feature.home

import android.content.Context
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mmr.app.ui.state.MmrUiMessages
import com.mmr.core.common.validation.MmrValidation
import com.mmr.core.data.preferences.AppDisplaySettingsStore
import com.mmr.core.data.preferences.toHomeViewTab
import com.mmr.core.database.relation.TransactionWithDetails
import com.mmr.core.domain.repository.ApprovalRepository
import com.mmr.core.domain.repository.CategoryRepository
import com.mmr.core.domain.repository.TransactionRepository
import com.mmr.core.export.PeriodExcelExporter
import com.mmr.core.model.enums.TransactionType
import com.mmr.core.transaction.TransactionCashFlow
import com.mmr.feature.home.model.*
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.util.*
import javax.inject.Inject

@HiltViewModel
class HomeViewModel
    @Inject
    constructor(
        @ApplicationContext private val appContext: Context,
        private val transactionRepository: TransactionRepository,
        private val categoryRepository: CategoryRepository,
        private val approvalRepository: ApprovalRepository,
        private val displaySettingsStore: AppDisplaySettingsStore,
        private val periodExcelExporter: PeriodExcelExporter,
    ) : ViewModel() {
        private val zoneId = ZoneId.systemDefault()

        private val _yearMonth = MutableStateFlow(YearMonth.now())
        private val _viewTab = MutableStateFlow(displaySettingsStore.defaultHomeTab())
        private val displaySettings =
            displaySettingsStore.settings
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), displaySettingsStore.snapshot())
        private val _selectedCategoryFilter = MutableStateFlow<String?>(null)
        private val _selectedTypeFilter = MutableStateFlow(HomeTypeFilter.ALL)
        private val _isRefreshing = MutableStateFlow(false)
        private val _errorMessage = MutableStateFlow<String?>(null)
        private val _showMonthPicker = MutableStateFlow(false)
        private val _monthPickerYear = MutableStateFlow(YearMonth.now().year)
        private val _isSelectionMode = MutableStateFlow(false)
        private val _selectedTransactionIds = MutableStateFlow<Set<Long>>(emptySet())
        private val _showMergeDialog = MutableStateFlow(false)
        private val _mergeNameInput = MutableStateFlow("")
        private val _mergeErrorMessage = MutableStateFlow<String?>(null)
        private val _showDeleteConfirmDialog = MutableStateFlow(false)
        private val _calendarSelectedDate = MutableStateFlow<LocalDate?>(null)
        private val _monthlyExpandedMonths = MutableStateFlow<Set<YearMonth>>(emptySet())
        private val _openTransactionId = MutableSharedFlow<Long>(extraBufferCapacity = 1)
        val openTransactionId = _openTransactionId.asSharedFlow()

        private val _periodExportRequest = MutableSharedFlow<HomePeriodExportRequest>(extraBufferCapacity = 1)
        val periodExportRequest = _periodExportRequest.asSharedFlow()

        private val _showExcelExportDialog = MutableStateFlow(false)
        private val _excelExportOptions = MutableStateFlow<List<HomeExcelExportOptionUi>>(emptyList())
        private val _isExcelExporting = MutableStateFlow(false)

        init {
            viewModelScope.launch {
                displaySettings
                    .map { it.startScreenTab }
                    .distinctUntilChanged()
                    .collect { startTab ->
                        _viewTab.value = startTab.toHomeViewTab()
                    }
            }
        }

        private data class MonthAggregate(
            val income: Double,
            val expense: Double,
            val categorySpending: List<com.mmr.core.database.model.CategorySpending>,
        )

        private val transactionsAndCategories =
            combine(
                transactionRepository
                    .getApprovedTransactionsDetailed()
                    .catch { e ->
                        _errorMessage.value = MmrUiMessages.friendly(e.message, MmrUiMessages.GENERIC_LOAD)
                        emit(emptyList())
                    },
                categoryRepository
                    .getActiveCategories()
                    .catch { e ->
                        _errorMessage.value = MmrUiMessages.friendly(e.message, MmrUiMessages.GENERIC_LOAD)
                        emit(emptyList())
                    },
            ) { transactions, categories -> transactions to categories }

        private val monthAggregate =
            combine(_yearMonth, displaySettings) { yearMonth, display ->
                yearMonth to display.monthStartDay
            }.flatMapLatest { (yearMonth, monthStartDay) ->
                val (start, end) = HomePeriodUtils.epochRangeMillis(yearMonth, zoneId, monthStartDay)
                combine(
                    transactionRepository.getTotalIncomeBetween(start, end).catch { emit(null) },
                    transactionRepository.getTotalExpenseBetween(start, end).catch { emit(null) },
                    transactionRepository.getCategorySpendingBetween(start, end).catch { emit(emptyList()) },
                ) { income, expense, spending ->
                    MonthAggregate(
                        income = income ?: 0.0,
                        expense = expense ?: 0.0,
                        categorySpending = spending,
                    )
                }
            }

        private val allTimeTotals =
            combine(
                transactionRepository.getTotalIncome().catch { emit(null) },
                transactionRepository.getTotalExpense().catch { emit(null) },
            ) { income, expense -> (income ?: 0.0) to (expense ?: 0.0) }

        private val filterState =
            combine(
                combine(
                    _selectedCategoryFilter,
                    _selectedTypeFilter,
                    _isRefreshing,
                ) { selectedCategory, selectedType, refreshing ->
                    FilterSnapshotPartial(
                        selectedCategory = selectedCategory,
                        selectedType = selectedType,
                        refreshing = refreshing,
                    )
                },
                combine(
                    _errorMessage,
                    _yearMonth,
                    _viewTab,
                    _showMonthPicker,
                    _monthPickerYear,
                ) { error, yearMonth, viewTab, showMonthPicker, monthPickerYear ->
                    FilterSnapshotMeta(
                        error = error,
                        yearMonth = yearMonth,
                        viewTab = viewTab,
                        showMonthPicker = showMonthPicker,
                        monthPickerYear = monthPickerYear,
                    )
                },
            ) { partial, meta ->
                FilterSnapshot(
                    selectedCategory = partial.selectedCategory,
                    selectedType = partial.selectedType,
                    refreshing = partial.refreshing,
                    error = meta.error,
                    yearMonth = meta.yearMonth,
                    viewTab = meta.viewTab,
                    showMonthPicker = meta.showMonthPicker,
                    monthPickerYear = meta.monthPickerYear,
                )
            }

        private data class FilterSnapshotMeta(
            val error: String?,
            val yearMonth: YearMonth,
            val viewTab: HomeViewTab,
            val showMonthPicker: Boolean,
            val monthPickerYear: Int,
        )

        private data class FilterSnapshotPartial(
            val selectedCategory: String?,
            val selectedType: HomeTypeFilter,
            val refreshing: Boolean,
        )

        private data class FilterSnapshot(
            val selectedCategory: String?,
            val selectedType: HomeTypeFilter,
            val refreshing: Boolean,
            val error: String?,
            val yearMonth: YearMonth,
            val viewTab: HomeViewTab,
            val showMonthPicker: Boolean,
            val monthPickerYear: Int,
        )

        private val pendingApprovalCount =
            approvalRepository
                .getPendingCount()
                .catch { emit(0) }

        private val homeCoreState =
            combine(
                combine(
                    transactionsAndCategories,
                    monthAggregate,
                    allTimeTotals,
                    filterState,
                    pendingApprovalCount,
                ) { txAndCats, monthData, allTime, filters, pendingCount ->
                    HomeCoreInputs(txAndCats, monthData, allTime, filters, pendingCount)
                },
                displaySettings,
                _calendarSelectedDate,
                _monthlyExpandedMonths,
            ) { core, display, calendarSelectedDate, monthlyExpanded ->
                val txAndCats = core.txAndCats
                val monthData = core.monthData
                val filters = core.filters
                val pendingCount = core.pendingCount
                val (allTransactions, categories) = txAndCats

                val monthScoped =
                    allTransactions.filter { item ->
                        HomePeriodUtils.isInMonth(
                            item.transaction.dateTime,
                            filters.yearMonth,
                            zoneId,
                            display.monthStartDay,
                        )
                    }

                val baseList = monthScoped

                val filtered =
                    applyFilters(
                        items = baseList,
                        selectedCategory = filters.selectedCategory,
                        selectedType = filters.selectedType,
                    )

                val timeAscending = display.timeDisplay.ascending
                val (periodStart, _) =
                    HomePeriodUtils.epochRangeMillis(
                        filters.yearMonth,
                        zoneId,
                        display.monthStartDay,
                    )
                val carriedForward =
                    if (display.budgetCarryOver && filters.viewTab != HomeViewTab.TOTAL) {
                        allTransactions
                            .filter {
                                MmrValidation.isValidEpochMillis(it.transaction.dateTime) &&
                                    it.transaction.dateTime < periodStart
                            }.sumOf {
                                TransactionCashFlow.netSignedAmount(it.transaction, it.loan?.loanType)
                            }
                    } else {
                        0.0
                    }

                val monthNet = monthData.income - monthData.expense
                val summary =
                    HomeSummaryUi(
                        totalIncome = monthData.income,
                        totalExpense = monthData.expense,
                        netBalance =
                            if (filters.viewTab == HomeViewTab.TOTAL) {
                                monthNet
                            } else {
                                monthNet + carriedForward
                            },
                        carriedForward = if (filters.viewTab == HomeViewTab.TOTAL) 0.0 else carriedForward,
                    )

                val grouped =
                    when (filters.viewTab) {
                        HomeViewTab.DAILY, HomeViewTab.TOTAL ->
                            TransactionGrouping.groupByDate(
                                items = filtered,
                                zoneId = zoneId,
                                withinDayAscending = timeAscending,
                                rowMapper = { toTransactionRow(it) },
                            )
                        else -> emptyList()
                    }

                val weekStart = HomePeriodUtils.toDayOfWeek(display.weekStartDay)
                val calendarTotalsSource =
                    applyFilters(
                        items = allTransactions,
                        selectedCategory = filters.selectedCategory,
                        selectedType = filters.selectedType,
                    )
                val totalsByDate = HomeCalendarUtils.aggregateByDate(calendarTotalsSource, zoneId)
                val calendarCells =
                    if (filters.viewTab == HomeViewTab.CALENDAR) {
                        HomeCalendarUtils.buildMonthGrid(filters.yearMonth, weekStart, totalsByDate)
                    } else {
                        emptyList()
                    }
                val calendarSelectedDayGroup =
                    calendarSelectedDate?.let { date ->
                        val start = HomePeriodUtils.dayStartMillis(date, zoneId)
                        val end = HomePeriodUtils.dayEndMillis(date, zoneId)
                        val dayItems =
                            calendarTotalsSource.filter { item ->
                                item.transaction.dateTime in start..end
                            }
                        TransactionGrouping
                            .groupByDate(
                                items = dayItems,
                                zoneId = zoneId,
                                withinDayAscending = timeAscending,
                                rowMapper = { toTransactionRow(it) },
                            ).firstOrNull()
                    }

                val totalAccountsSummary =
                    if (filters.viewTab == HomeViewTab.TOTAL) {
                        val prevMonth = filters.yearMonth.minusMonths(1)
                        val filteredAll =
                            applyFilters(
                                items = allTransactions,
                                selectedCategory = filters.selectedCategory,
                                selectedType = filters.selectedType,
                            )
                        val currentPeriod =
                            HomeTotalUtils.filterToMonth(
                                filteredAll,
                                filters.yearMonth,
                                zoneId,
                                display.monthStartDay,
                            )
                        val previousPeriod =
                            HomeTotalUtils.filterToMonth(
                                filteredAll,
                                prevMonth,
                                zoneId,
                                display.monthStartDay,
                            )
                        HomeTotalUtils.buildAccountsSummary(
                            currentPeriodItems = currentPeriod,
                            previousPeriodItems = previousPeriod,
                            yearMonth = filters.yearMonth,
                            zoneId = zoneId,
                            monthStartDay = display.monthStartDay,
                        )
                    } else {
                        null
                    }

                val monthlyPeriods =
                    if (filters.viewTab == HomeViewTab.MONTHLY) {
                        HomeMonthlyUtils.buildMonthlyPeriods(
                            transactions =
                                applyFilters(
                                    items = allTransactions,
                                    selectedCategory = filters.selectedCategory,
                                    selectedType = filters.selectedType,
                                ),
                            anchorMonth = filters.yearMonth,
                            monthCount = 12,
                            zoneId = zoneId,
                            monthStartDay = display.monthStartDay,
                            weekStart = weekStart,
                        )
                    } else {
                        emptyList()
                    }

                val isEmpty =
                    when (filters.viewTab) {
                        HomeViewTab.DAILY -> filters.error == null && grouped.isEmpty()
                        HomeViewTab.TOTAL -> false
                        HomeViewTab.CALENDAR -> false
                        HomeViewTab.MONTHLY -> false
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
                    calendarCells = calendarCells,
                    calendarSelectedDate = calendarSelectedDate,
                    calendarSelectedDayGroup = calendarSelectedDayGroup,
                    calendarWeekStart = weekStart,
                    monthlyPeriods = monthlyPeriods,
                    monthlyExpandedMonths = monthlyExpanded,
                    totalAccountsSummary = totalAccountsSummary,
                    pendingApprovalCount = pendingCount,
                    errorMessage = filters.error,
                    isEmpty = isEmpty,
                )
            }

        val uiState: StateFlow<HomeUiState> =
            combine(
                homeCoreState,
                displaySettings,
                combine(
                    combine(
                        _isSelectionMode,
                        _selectedTransactionIds,
                        _showMergeDialog,
                        _mergeNameInput,
                        _mergeErrorMessage,
                    ) { selectionMode, selected, showMerge, mergeName, mergeError ->
                        SelectionOverlay(
                            isSelectionMode = selectionMode,
                            selectedIds = selected,
                            showMergeDialog = showMerge,
                            mergeNameInput = mergeName,
                            mergeErrorMessage = mergeError,
                            showDeleteConfirmDialog = false,
                        )
                    },
                    _showDeleteConfirmDialog,
                ) { overlay, showDelete ->
                    overlay.copy(showDeleteConfirmDialog = showDelete)
                },
                combine(
                    _showExcelExportDialog,
                    _excelExportOptions,
                    _isExcelExporting,
                ) { showExcel, options, exporting ->
                    ExcelExportOverlay(showExcel, options, exporting)
                },
            ) { home, display, selection, excel ->
                val selectableIds = visibleSelectableTransactionIds(home)
                val selected = selection.selectedIds
                home.copy(
                    isSelectionMode = selection.isSelectionMode,
                    selectedTransactionIds = selected,
                    showMergeDialog = selection.showMergeDialog,
                    mergeNameInput = selection.mergeNameInput,
                    mergeErrorMessage = selection.mergeErrorMessage,
                    showDeleteConfirmDialog = selection.showDeleteConfirmDialog,
                    showExcelExportDialog = excel.showDialog,
                    excelExportOptions = excel.options,
                    isExcelExporting = excel.isExporting,
                    canMergeSelection = selected.size >= 2,
                    canDeleteSelection = selected.isNotEmpty(),
                    visibleSelectableCount = selectableIds.size,
                    allVisibleSelected = selectableIds.isNotEmpty() && selectableIds.all { it in selected },
                    amountColorScheme = display.amountColorScheme,
                    monthSwipeEnabled =
                        display.swipeBehavior == com.mmr.core.model.settings.MmrSwipeBehavior.CHANGE_DATE,
                    showTransactionDescription = display.showDescription,
                    showTransactionTime = display.timeDisplay.showTime,
                    transactionTimeAscending = display.timeDisplay.ascending,
                )
            }.stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = HomeUiState(isLoading = true),
            )

        private data class HomeCoreInputs(
            val txAndCats: Pair<List<TransactionWithDetails>, List<com.mmr.core.database.entity.CategoryEntity>>,
            val monthData: MonthAggregate,
            val allTime: Pair<Double, Double>,
            val filters: FilterSnapshot,
            val pendingCount: Int,
        )

        private data class SelectionOverlay(
            val isSelectionMode: Boolean,
            val selectedIds: Set<Long>,
            val showMergeDialog: Boolean,
            val mergeNameInput: String,
            val mergeErrorMessage: String?,
            val showDeleteConfirmDialog: Boolean,
        )

        private data class ExcelExportOverlay(
            val showDialog: Boolean,
            val options: List<HomeExcelExportOptionUi>,
            val isExporting: Boolean,
        )

        private fun visibleSelectableTransactionIds(home: HomeUiState): Set<Long> =
            when (home.selectedViewTab) {
                HomeViewTab.DAILY, HomeViewTab.TOTAL ->
                    home.groupedTransactions.flatMap { group -> group.items.map { it.id } }.toSet()
                else -> emptySet()
            }

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
                    clearSelection()
                    if (event.tab == HomeViewTab.CALENDAR && _calendarSelectedDate.value == null) {
                        _calendarSelectedDate.value = LocalDate.now()
                    }
                    if (event.tab == HomeViewTab.MONTHLY && _monthlyExpandedMonths.value.isEmpty()) {
                        _monthlyExpandedMonths.value = setOf(_yearMonth.value)
                    }
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
                is HomeUiEvent.OnTransactionClick,
                -> Unit

                HomeUiEvent.OnEnterSelectionMode -> {
                    _isSelectionMode.value = true
                }

                HomeUiEvent.OnExitSelectionMode -> clearSelection()

                is HomeUiEvent.OnToggleTransactionSelection -> {
                    _selectedTransactionIds.update { current ->
                        if (event.id in current) current - event.id else current + event.id
                    }
                }

                HomeUiEvent.OnOpenMergeDialog -> {
                    _mergeErrorMessage.value = null
                    if (_selectedTransactionIds.value.size >= 2) {
                        _showMergeDialog.value = true
                    }
                }

                HomeUiEvent.OnDismissMergeDialog -> {
                    _showMergeDialog.value = false
                    _mergeErrorMessage.value = null
                }

                is HomeUiEvent.OnMergeNameChanged -> {
                    _mergeNameInput.value = event.name
                }

                HomeUiEvent.OnConfirmMerge -> confirmMerge()

                HomeUiEvent.OnToggleSelectAllVisible -> {
                    val selectable = visibleSelectableTransactionIds(uiState.value)
                    _selectedTransactionIds.update { current ->
                        if (selectable.isNotEmpty() && selectable.all { it in current }) {
                            emptySet()
                        } else {
                            selectable
                        }
                    }
                }

                HomeUiEvent.OnOpenDeleteConfirmDialog -> {
                    if (_selectedTransactionIds.value.isNotEmpty()) {
                        _showDeleteConfirmDialog.value = true
                    }
                }

                HomeUiEvent.OnDismissDeleteConfirmDialog -> {
                    _showDeleteConfirmDialog.value = false
                }

                HomeUiEvent.OnConfirmDeleteSelected -> confirmDeleteSelected()

                is HomeUiEvent.OnCalendarDateSelected -> {
                    selectCalendarDate(event.date)
                }

                is HomeUiEvent.OnMonthlyMonthToggle -> {
                    _monthlyExpandedMonths.update { current ->
                        if (event.yearMonth in current) {
                            current - event.yearMonth
                        } else {
                            current + event.yearMonth
                        }
                    }
                }

                HomeUiEvent.OnCalendarPreviousDay -> {
                    val current = _calendarSelectedDate.value ?: LocalDate.now()
                    selectCalendarDate(current.minusDays(1))
                }

                HomeUiEvent.OnCalendarNextDay -> {
                    val current = _calendarSelectedDate.value ?: LocalDate.now()
                    selectCalendarDate(current.plusDays(1))
                }

                HomeUiEvent.OnExportPeriodClick -> openExcelExportDialog()
                HomeUiEvent.OnDismissExcelExportDialog -> _showExcelExportDialog.value = false
                is HomeUiEvent.OnExcelExportPeriodSelected -> {
                    _showExcelExportDialog.value = false
                    val option = _excelExportOptions.value.find { it.preset == event.preset }
                    if (option != null) {
                        exportExcelForRange(option.range)
                    }
                }
            }
        }

        private fun openExcelExportDialog() {
            val display = displaySettings.value
            _excelExportOptions.value =
                HomeExcelExportPeriods.buildOptions(
                    anchorMonth = _yearMonth.value,
                    monthStartDay = display.monthStartDay,
                )
            _showExcelExportDialog.value = true
        }

        private fun exportExcelForRange(range: HomeExcelExportRange) {
            viewModelScope.launch {
                if (_isExcelExporting.value) return@launch
                _isExcelExporting.value = true
                try {
                    val (transactions, _) = transactionsAndCategories.first()
                    val display = displaySettings.value
                    val filteredByType =
                        applyFilters(
                            items = transactions,
                            selectedCategory = _selectedCategoryFilter.value,
                            selectedType = _selectedTypeFilter.value,
                        )
                    val filtered =
                        when (range) {
                            is HomeExcelExportRange.Bounded ->
                                HomePeriodUtils.filterToDateRange(
                                    filteredByType,
                                    range.start,
                                    range.end,
                                    zoneId,
                                )
                            HomeExcelExportRange.AllTime -> filteredByType
                        }
                    val appName = appContext.getString(com.mmr.app.R.string.app_name)
                    val fileName = HomeExcelExportPeriods.buildFileName(appName, range)
                    val file =
                        periodExcelExporter.exportPeriod(
                            items = filtered,
                            fileName = fileName,
                            zoneId = zoneId,
                            currencySymbol = display.mainCurrencySymbol,
                            currencyCode = display.mainCurrencyCode,
                        )
                    val uri =
                        FileProvider.getUriForFile(
                            appContext,
                            "${appContext.packageName}.exportfileprovider",
                            file,
                        )
                    _periodExportRequest.emit(
                        HomePeriodExportRequest(
                            contentUri = uri,
                            subject = HomeExcelExportPeriods.formatShareSubject(appName, range),
                        ),
                    )
                } catch (_: Exception) {
                    _errorMessage.value = "Could not export Excel file. Please try again."
                } finally {
                    _isExcelExporting.value = false
                }
            }
        }

        fun calendarAddTransactionMillis(): Long? {
            val date = uiState.value.calendarSelectedDate ?: return null
            return HomeCalendarUtils.prefillMillisForDate(date, zoneId)
        }

        private fun selectCalendarDate(date: LocalDate) {
            _calendarSelectedDate.value = date
            val ym = _yearMonth.value
            if (date.year != ym.year || date.month != ym.month) {
                _yearMonth.value = YearMonth.from(date)
            }
        }

        private fun clearSelection() {
            _isSelectionMode.value = false
            _selectedTransactionIds.value = emptySet()
            _showMergeDialog.value = false
            _mergeNameInput.value = ""
            _mergeErrorMessage.value = null
            _showDeleteConfirmDialog.value = false
        }

        private fun confirmMerge() {
            val ids = _selectedTransactionIds.value.toList()
            viewModelScope.launch {
                try {
                    val mergedId = transactionRepository.mergeTransactions(ids, _mergeNameInput.value)
                    clearSelection()
                    _mergeErrorMessage.value = null
                    _openTransactionId.tryEmit(mergedId)
                } catch (e: Exception) {
                    _showMergeDialog.value = true
                    _mergeErrorMessage.value = MmrUiMessages.friendly(e.message, MmrUiMessages.GENERIC_ACTION)
                }
            }
        }

        private fun confirmDeleteSelected() {
            val ids = _selectedTransactionIds.value.toList()
            viewModelScope.launch {
                try {
                    for (id in ids) {
                        val detailed = transactionRepository.getTransactionDetailedById(id)
                        val transaction = detailed?.transaction ?: continue
                        transactionRepository.deleteTransaction(transaction)
                    }
                    clearSelection()
                } catch (e: Exception) {
                    _showDeleteConfirmDialog.value = false
                    _errorMessage.value = MmrUiMessages.friendly(e.message, MmrUiMessages.GENERIC_ACTION)
                }
            }
        }

        private fun applyFilters(
            items: List<TransactionWithDetails>,
            selectedCategory: String?,
            selectedType: HomeTypeFilter,
        ): List<TransactionWithDetails> =
            items.filter { item ->
                val matchesCategory = selectedCategory == null || item.category?.name == selectedCategory

                val loanType = item.loan?.loanType
                val matchesType =
                    when (selectedType) {
                        HomeTypeFilter.ALL -> true
                        HomeTypeFilter.EXPENSE -> TransactionCashFlow.countsAsExpense(item.transaction, loanType)
                        HomeTypeFilter.INCOME -> TransactionCashFlow.countsAsIncome(item.transaction, loanType)
                        HomeTypeFilter.TRANSFER ->
                            item.transaction.type == TransactionType.TRANSFER &&
                                !TransactionCashFlow.countsAsExpense(item.transaction, loanType) &&
                                !TransactionCashFlow.countsAsIncome(item.transaction, loanType)
                    }

                matchesCategory && matchesType
            }

        private fun toTransactionRow(item: TransactionWithDetails): TransactionRowUi {
            val display = displaySettings.value
            val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())
            return TransactionRowMapping.fromDetails(
                item = item,
                timeFormat = timeFormat,
                showDescription = display.showDescription,
                showTime = display.timeDisplay.showTime,
            )
        }
    }
