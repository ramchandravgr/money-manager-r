package com.mmr.feature.stats.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mmr.app.navigation.Screen
import com.mmr.app.ui.state.MmrUiMessages
import com.mmr.core.common.validation.MmrValidation
import com.mmr.core.data.preferences.AppDisplaySettingsStore
import com.mmr.core.domain.repository.CategoryRepository
import com.mmr.core.domain.repository.TransactionRepository
import com.mmr.core.model.enums.TransactionType
import com.mmr.core.model.settings.MmrSwipeBehavior
import com.mmr.feature.home.HomePeriodUtils
import com.mmr.feature.home.TransactionGrouping
import com.mmr.feature.stats.StatsCategoryChartUtils
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
class StatsCategoryDetailViewModel
    @Inject
    constructor(
        savedStateHandle: SavedStateHandle,
        private val transactionRepository: TransactionRepository,
        private val categoryRepository: CategoryRepository,
        displaySettingsStore: AppDisplaySettingsStore,
    ) : ViewModel() {
        private val displaySettings =
            displaySettingsStore.settings
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), displaySettingsStore.snapshot())

        companion object {
            const val UNCATEGORIZED_SUBCATEGORY_ID = -2L
        }

        private val zoneId = ZoneId.systemDefault()
        private val categoryId: Long = savedStateHandle.get<Long>(Screen.StatsCategoryDetail.ARG_CATEGORY_ID) ?: 0L
        private val transactionType: TransactionType =
            runCatching {
                TransactionType.valueOf(
                    savedStateHandle.get<String>(Screen.StatsCategoryDetail.ARG_TRANSACTION_TYPE)
                        ?: TransactionType.EXPENSE.name,
                )
            }.getOrDefault(TransactionType.EXPENSE)

        private val _periodMode =
            MutableStateFlow(
                runCatching {
                    StatsPeriodMode.valueOf(
                        savedStateHandle.get<String>(Screen.StatsCategoryDetail.ARG_PERIOD_MODE)
                            ?: StatsPeriodMode.MONTHLY.name,
                    )
                }.getOrDefault(StatsPeriodMode.MONTHLY),
            )
        private val _anchorDate =
            MutableStateFlow(
                millisToLocalDate(
                    savedStateHandle.get<Long>(Screen.StatsCategoryDetail.ARG_ANCHOR_MILLIS)
                        ?: System.currentTimeMillis(),
                ),
            )
        private val _customPeriodStart =
            MutableStateFlow<LocalDate?>(
                savedStateHandle
                    .get<Long>(Screen.StatsCategoryDetail.ARG_CUSTOM_START_MILLIS)
                    ?.takeIf { it >= 0 }
                    ?.let { millisToLocalDate(it) },
            )
        private val _customPeriodEnd =
            MutableStateFlow<LocalDate?>(
                savedStateHandle
                    .get<Long>(Screen.StatsCategoryDetail.ARG_CUSTOM_END_MILLIS)
                    ?.takeIf { it >= 0 }
                    ?.let { millisToLocalDate(it) },
            )
        private val _selectedSubcategoryId = MutableStateFlow<Long?>(null)
        private val _errorMessage = MutableStateFlow<String?>(null)
        private val _showPeriodStartPicker = MutableStateFlow(false)
        private val _showPeriodEndPicker = MutableStateFlow(false)
        private val _showMonthPicker = MutableStateFlow(false)
        private val _monthPickerYear = MutableStateFlow(YearMonth.now().year)
        private val _isSelectionMode = MutableStateFlow(false)
        private val _selectedTransactionIds = MutableStateFlow<Set<Long>>(emptySet())
        private val _showDeleteConfirmDialog = MutableStateFlow(false)
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

        private val categoryMeta =
            categoryRepository
                .getActiveCategories()
                .map { categories -> categories.firstOrNull { it.id == categoryId } }
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

        private val detailFlags =
            combine(
                _selectedSubcategoryId,
                _errorMessage,
                _showPeriodStartPicker,
                _showPeriodEndPicker,
                _showMonthPicker,
            ) { selectedSubcategoryId, error, showStart, showEnd, showMonthPicker ->
                DetailFlagsPart1(selectedSubcategoryId, error, showStart, showEnd, showMonthPicker)
            }

        private val detailFlags2 =
            combine(
                _monthPickerYear,
                _isSelectionMode,
                _selectedTransactionIds,
                _showDeleteConfirmDialog,
                _isRefreshing,
            ) { pickerYear, selectionMode, selectedIds, showDeleteConfirm, refreshing ->
                DetailFlagsPart2(pickerYear, selectionMode, selectedIds, showDeleteConfirm, refreshing)
            }

        val uiState: StateFlow<StatsCategoryDetailUiState> =
            combine(
                periodConfig,
                detailFlags,
                detailFlags2,
                categoryMeta,
            ) { bounds, flags1, flags2, category ->
                val meta =
                    DetailShellMeta(
                        selectedSubcategoryId = flags1.selectedSubcategoryId,
                        error = flags1.error,
                        showStart = flags1.showStart,
                        showEnd = flags1.showEnd,
                        showMonthPicker = flags1.showMonthPicker,
                        pickerYear = flags2.pickerYear,
                        selectionMode = flags2.selectionMode,
                        selectedIds = flags2.selectedIds,
                        showDeleteConfirm = flags2.showDeleteConfirm,
                        refreshing = flags2.refreshing,
                    )
                DetailShell(bounds, meta, category)
            }.flatMapLatest { shell ->
                val bounds = shell.bounds
                val chartRange = StatsCategoryChartUtils.chartRange(bounds.endDate)
                val siblingFlow =
                    if (transactionType == TransactionType.INCOME) {
                        transactionRepository.getCategoryIncomeBetween(bounds.startMillis, bounds.endMillis)
                    } else {
                        transactionRepository.getCategorySpendingBetween(bounds.startMillis, bounds.endMillis)
                    }.catch { emit(emptyList()) }

                combine(
                    transactionRepository
                        .getSubcategorySpendingForCategory(
                            categoryId,
                            transactionType,
                            bounds.startMillis,
                            bounds.endMillis,
                        ).catch {
                            _errorMessage.value = MmrUiMessages.friendly(it.message, MmrUiMessages.GENERIC_LOAD)
                            emit(emptyList())
                        },
                    transactionRepository
                        .getCategoryMonthlyTotals(
                            categoryId,
                            transactionType,
                            chartRange.first,
                            chartRange.second,
                        ).catch { emit(emptyList()) },
                    transactionRepository
                        .getFilteredTransactions(
                            startDate = bounds.startMillis,
                            endDate = bounds.endMillis,
                            accountId = null,
                            categoryId = categoryId,
                            subcategoryId = null,
                            type = transactionType,
                        ).catch { emit(emptyList()) },
                    siblingFlow,
                ) { subcategoryRows, monthlyRows, transactions, siblings ->
                    val filteredTransactions =
                        when (shell.meta.selectedSubcategoryId) {
                            null -> transactions
                            UNCATEGORIZED_SUBCATEGORY_ID -> transactions.filter { it.subcategory == null }
                            else -> transactions.filter { it.subcategory?.id == shell.meta.selectedSubcategoryId }
                        }
                    val total = subcategoryRows.sumOf { it.totalAmount }
                    val subcategoryUi = buildSubcategoryUi(subcategoryRows, total)
                    val grouped = TransactionGrouping.groupByDate(filteredTransactions, zoneId)
                    val categoryName = shell.category?.name ?: "Category"
                    val mode = _periodMode.value
                    val searchSubcategoryId =
                        when (shell.meta.selectedSubcategoryId) {
                            null, UNCATEGORIZED_SUBCATEGORY_ID -> -1L
                            else -> shell.meta.selectedSubcategoryId
                        }
                    val siblingCategories =
                        siblings
                            .mapNotNull { row ->
                                val id = row.categoryId ?: return@mapNotNull null
                                StatsSiblingCategoryUi(
                                    categoryId = id,
                                    name = row.categoryName ?: "Category",
                                    colorHex = row.colorHex ?: "#6B7280",
                                    amount = row.totalAmount,
                                )
                            }.sortedByDescending { it.amount }
                    StatsCategoryDetailUiState(
                        isLoading = false,
                        isRefreshing = shell.meta.refreshing,
                        isSelectionMode = shell.meta.selectionMode,
                        selectedTransactionIds = shell.meta.selectedIds,
                        showDeleteConfirmDialog = shell.meta.showDeleteConfirm,
                        siblingCategories = siblingCategories,
                        categoryId = categoryId,
                        transactionType = transactionType.name,
                        searchSubcategoryId = searchSubcategoryId,
                        categoryName = categoryName,
                        categoryColorHex = shell.category?.colorHex ?: "#6B7280",
                        periodMode = mode,
                        navigationLabel = bounds.navigationLabel,
                        periodStartMillis = bounds.startMillis,
                        periodEndMillis = bounds.endMillis,
                        periodStartDate = bounds.startDate,
                        periodEndDate = bounds.endDate,
                        canNavigatePeriod = mode != StatsPeriodMode.PERIOD,
                        periodSwipeEnabled = displaySettings.value.swipeBehavior == MmrSwipeBehavior.CHANGE_DATE,
                        totalFormatted = "₹ ${"%.2f".format(total)}",
                        subcategoryBreakdowns = subcategoryUi,
                        selectedSubcategoryId = shell.meta.selectedSubcategoryId,
                        chartPoints = StatsCategoryChartUtils.buildChartPoints(bounds.endDate, monthlyRows),
                        groupedTransactions = grouped,
                        errorMessage = shell.meta.error,
                        isEmpty = shell.meta.error == null && total == 0.0 && grouped.isEmpty(),
                        showPeriodStartPicker = shell.meta.showStart,
                        showPeriodEndPicker = shell.meta.showEnd,
                        showMonthPicker = shell.meta.showMonthPicker,
                        monthPickerYear = shell.meta.pickerYear,
                    )
                }
            }.stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = StatsCategoryDetailUiState(isLoading = true),
            )

        fun onEvent(event: StatsCategoryDetailUiEvent) {
            when (event) {
                StatsCategoryDetailUiEvent.OnPreviousPeriod -> shiftPeriod(forward = false)
                StatsCategoryDetailUiEvent.OnNextPeriod -> shiftPeriod(forward = true)
                is StatsCategoryDetailUiEvent.OnSubcategorySelected -> {
                    _selectedSubcategoryId.value = event.subcategoryId
                }
                StatsCategoryDetailUiEvent.OnPeriodStartFieldClick -> _showPeriodStartPicker.value = true
                StatsCategoryDetailUiEvent.OnPeriodEndFieldClick -> _showPeriodEndPicker.value = true
                StatsCategoryDetailUiEvent.OnDismissPeriodStartPicker -> _showPeriodStartPicker.value = false
                StatsCategoryDetailUiEvent.OnDismissPeriodEndPicker -> _showPeriodEndPicker.value = false
                is StatsCategoryDetailUiEvent.OnCustomPeriodStartSelected -> {
                    if (!MmrValidation.isValidEpochMillis(event.dateMillis)) {
                        _errorMessage.value = "Please choose a valid start date."
                        return
                    }
                    _errorMessage.value = null
                    _customPeriodStart.value = millisToLocalDate(event.dateMillis)
                    _showPeriodStartPicker.value = false
                    _periodMode.value = StatsPeriodMode.PERIOD
                }
                is StatsCategoryDetailUiEvent.OnCustomPeriodEndSelected -> {
                    if (!MmrValidation.isValidEpochMillis(event.dateMillis)) {
                        _errorMessage.value = "Please choose a valid end date."
                        return
                    }
                    _errorMessage.value = null
                    _customPeriodEnd.value = millisToLocalDate(event.dateMillis)
                    _showPeriodEndPicker.value = false
                    _periodMode.value = StatsPeriodMode.PERIOD
                }
                StatsCategoryDetailUiEvent.OnRetryClick -> _errorMessage.value = null
                StatsCategoryDetailUiEvent.OnRefresh -> triggerRefresh()
                StatsCategoryDetailUiEvent.OnEnterSelectionMode -> _isSelectionMode.value = true
                StatsCategoryDetailUiEvent.OnExitSelectionMode -> clearSelection()
                is StatsCategoryDetailUiEvent.OnToggleTransactionSelection -> {
                    _selectedTransactionIds.update { current ->
                        if (event.transactionId in
                            current
                        ) {
                            current - event.transactionId
                        } else {
                            current + event.transactionId
                        }
                    }
                }
                StatsCategoryDetailUiEvent.OnSelectAllTransactions -> {
                    val ids = uiState.value.groupedTransactions.flatMap { group -> group.items.map { it.id } }
                    _selectedTransactionIds.value = ids.toSet()
                }
                StatsCategoryDetailUiEvent.OnRequestDeleteSelected -> {
                    if (_selectedTransactionIds.value.isNotEmpty()) {
                        _showDeleteConfirmDialog.value = true
                    }
                }
                StatsCategoryDetailUiEvent.OnDismissDeleteConfirm -> _showDeleteConfirmDialog.value = false
                StatsCategoryDetailUiEvent.OnConfirmDeleteSelected -> confirmDeleteSelected()
                is StatsCategoryDetailUiEvent.OnRecategorizeSelected -> recategorizeSelected(event.targetCategoryId)
                is StatsCategoryDetailUiEvent.OnSiblingCategorySelected -> {
                    // Navigation handled in UI; no-op in VM
                }
                StatsCategoryDetailUiEvent.OnOpenMonthPicker -> {
                    _monthPickerYear.value = _anchorDate.value.year
                    _showMonthPicker.value = true
                }
                StatsCategoryDetailUiEvent.OnDismissMonthPicker -> _showMonthPicker.value = false
                StatsCategoryDetailUiEvent.OnMonthPickerPreviousYear -> _monthPickerYear.update { it - 1 }
                StatsCategoryDetailUiEvent.OnMonthPickerNextYear -> _monthPickerYear.update { it + 1 }
                is StatsCategoryDetailUiEvent.OnMonthPickerMonthSelected -> {
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
                StatsCategoryDetailUiEvent.OnMonthPickerThisMonth -> {
                    _anchorDate.value = LocalDate.now()
                    _showMonthPicker.value = false
                }
                is StatsCategoryDetailUiEvent.OnPeriodModeSelected -> {
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
            }
        }

        fun currentNavigationArgs(): StatsCategoryDrillDown? {
            val state = uiState.value
            return StatsCategoryDrillDown(
                categoryId = state.categoryId,
                startMillis = state.periodStartMillis,
                endMillis = state.periodEndMillis,
                transactionType = transactionType,
                periodMode = _periodMode.value,
                anchorDate = _anchorDate.value,
                customStartDate = _customPeriodStart.value,
                customEndDate = _customPeriodEnd.value,
            )
        }

        private fun buildSubcategoryUi(
            subcategoryRows: List<com.mmr.core.database.model.SubcategorySpending>,
            total: Double,
        ): List<SubcategoryBreakdownUi> =
            buildList {
                add(
                    SubcategoryBreakdownUi(
                        subcategoryId = null,
                        name = "All",
                        amount = total,
                        amountFormatted = "₹ ${"%.2f".format(total)}",
                        percentage = if (total > 0f) 1f else 0f,
                        percentageFormatted = if (total > 0) "100%" else "0%",
                    ),
                )
                subcategoryRows.forEach { row ->
                    val pct = if (total > 0) (row.totalAmount / total).toFloat() else 0f
                    val rowSubcategoryId = row.subcategoryId ?: UNCATEGORIZED_SUBCATEGORY_ID
                    add(
                        SubcategoryBreakdownUi(
                            subcategoryId = rowSubcategoryId,
                            name = row.subcategoryName?.takeIf { it.isNotBlank() } ?: "Other",
                            amount = row.totalAmount,
                            amountFormatted = "₹ ${"%.2f".format(row.totalAmount)}",
                            percentage = pct,
                            percentageFormatted = "${"%.0f".format(pct * 100)}%",
                        ),
                    )
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

        private fun recategorizeSelected(targetCategoryId: Long) {
            val ids = _selectedTransactionIds.value.toList()
            if (ids.isEmpty()) return
            viewModelScope.launch {
                try {
                    for (id in ids) {
                        val detailed = transactionRepository.getTransactionDetailedById(id) ?: continue
                        val updated =
                            detailed.transaction.copy(
                                categoryId = targetCategoryId,
                                subcategoryId = null,
                            )
                        transactionRepository.updateTransaction(updated)
                    }
                    clearSelection()
                } catch (e: Exception) {
                    _errorMessage.value = MmrUiMessages.friendly(e.message, MmrUiMessages.GENERIC_ACTION)
                }
            }
        }

        private fun clearSelection() {
            _isSelectionMode.value = false
            _selectedTransactionIds.value = emptySet()
            _showDeleteConfirmDialog.value = false
        }

        private fun triggerRefresh() {
            viewModelScope.launch {
                _isRefreshing.value = true
                _refreshTick.update { it + 1 }
                _isRefreshing.value = false
            }
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

        private data class DetailShell(
            val bounds: StatsPeriodUtils.PeriodBounds,
            val meta: DetailShellMeta,
            val category: com.mmr.core.database.entity.CategoryEntity?,
        )

        private data class PeriodAnchors(
            val mode: StatsPeriodMode,
            val anchor: LocalDate,
            val customStart: LocalDate?,
            val customEnd: LocalDate?,
        )

        private data class DetailFlagsPart1(
            val selectedSubcategoryId: Long?,
            val error: String?,
            val showStart: Boolean,
            val showEnd: Boolean,
            val showMonthPicker: Boolean,
        )

        private data class DetailFlagsPart2(
            val pickerYear: Int,
            val selectionMode: Boolean,
            val selectedIds: Set<Long>,
            val showDeleteConfirm: Boolean,
            val refreshing: Boolean,
        )

        private data class DetailShellMeta(
            val selectedSubcategoryId: Long?,
            val error: String?,
            val showStart: Boolean,
            val showEnd: Boolean,
            val showMonthPicker: Boolean,
            val pickerYear: Int,
            val selectionMode: Boolean,
            val selectedIds: Set<Long>,
            val showDeleteConfirm: Boolean,
            val refreshing: Boolean,
        )
    }
