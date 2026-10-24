package com.mmr.feature.search.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mmr.app.navigation.Screen
import com.mmr.core.common.validation.MmrValidation
import com.mmr.core.data.preferences.AppDisplaySettingsSnapshot
import com.mmr.core.data.preferences.AppDisplaySettingsStore
import com.mmr.core.database.entity.AccountEntity
import com.mmr.core.database.relation.TransactionWithDetails
import com.mmr.core.domain.repository.AccountRepository
import com.mmr.core.domain.repository.CategoryRepository
import com.mmr.core.domain.repository.TransactionRepository
import com.mmr.core.model.category.CategoryWithChildren
import com.mmr.core.model.enums.TransactionType
import com.mmr.feature.home.HomePeriodUtils
import com.mmr.feature.home.TransactionGrouping
import com.mmr.feature.search.SearchHeaderPeriodMode
import com.mmr.feature.search.SearchHeaderPeriodRanges
import com.mmr.feature.search.model.SearchAccountUi
import com.mmr.feature.search.model.TransactionSearchUiEvent
import com.mmr.feature.search.model.TransactionSearchUiState
import com.mmr.feature.transaction.model.CategoryUi
import com.mmr.feature.transaction.model.SubcategoryUi
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject

@OptIn(FlowPreview::class)
@HiltViewModel
class TransactionSearchViewModel
    @Inject
    constructor(
        private val transactionRepository: TransactionRepository,
        private val categoryRepository: CategoryRepository,
        private val accountRepository: AccountRepository,
        savedStateHandle: SavedStateHandle,
        private val displaySettingsStore: AppDisplaySettingsStore,
    ) : ViewModel() {
        private val zoneId = ZoneId.systemDefault()
        private val displaySettings =
            displaySettingsStore.settings
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), displaySettingsStore.snapshot())

        private val noteQuery = MutableStateFlow("")
        private val periodStart = MutableStateFlow<LocalDate?>(null)
        private val periodEnd = MutableStateFlow<LocalDate?>(null)
        private val selectedAccountId = MutableStateFlow<Long?>(null)
        private val selectedCategoryId = MutableStateFlow<Long?>(null)
        private val selectedSubcategoryId = MutableStateFlow<Long?>(null)
        private val amountMin = MutableStateFlow("")
        private val amountMax = MutableStateFlow("")
        private val showDateRangePicker = MutableStateFlow(false)
        private val pickerHighlightedCategoryId = MutableStateFlow<Long?>(null)
        private val errorMessage = MutableStateFlow<String?>(null)
        private val filtersExpanded = MutableStateFlow(true)
        private val headerPeriodMode = MutableStateFlow(SearchHeaderPeriodMode.MONTH)
        private val lockedTransactionType = MutableStateFlow<TransactionType?>(null)

        init {
            val categoryId = savedStateHandle.get<Long>(Screen.TransactionSearch.ARG_CATEGORY_ID) ?: -1L
            if (categoryId >= 0) {
                selectedCategoryId.value = categoryId
            }
            val subcategoryId = savedStateHandle.get<Long>(Screen.TransactionSearch.ARG_SUBCATEGORY_ID) ?: -1L
            if (subcategoryId >= 0) {
                selectedSubcategoryId.value = subcategoryId
            }
            val startMillis = savedStateHandle.get<Long>(Screen.TransactionSearch.ARG_START_MILLIS) ?: -1L
            val endMillis = savedStateHandle.get<Long>(Screen.TransactionSearch.ARG_END_MILLIS) ?: -1L
            if (startMillis >= 0 && endMillis >= 0) {
                periodStart.value =
                    java.time.Instant
                        .ofEpochMilli(startMillis)
                        .atZone(zoneId)
                        .toLocalDate()
                periodEnd.value =
                    java.time.Instant
                        .ofEpochMilli(endMillis)
                        .atZone(zoneId)
                        .toLocalDate()
            }
            val typeArg = savedStateHandle.get<String>(Screen.TransactionSearch.ARG_TRANSACTION_TYPE) ?: ""
            if (typeArg.isNotBlank()) {
                runCatching { TransactionType.valueOf(typeArg) }.getOrNull()?.let {
                    lockedTransactionType.value = it
                }
            }
            if (periodStart.value == null || periodEnd.value == null) {
                applyHeaderPeriodMode(SearchHeaderPeriodMode.MONTH)
            } else {
                headerPeriodMode.value = SearchHeaderPeriodMode.CUSTOM
            }
        }

        private fun applyHeaderPeriodMode(mode: SearchHeaderPeriodMode) {
            headerPeriodMode.value = mode
            val display = displaySettingsStore.snapshot()
            val (start, end) =
                SearchHeaderPeriodRanges.rangeFor(
                    mode = mode,
                    anchor = LocalDate.now(),
                    monthStartDay = display.monthStartDay,
                    weekStartDay = display.weekStartDay,
                    customStart = periodStart.value,
                    customEnd = periodEnd.value,
                )
            periodStart.value = start
            periodEnd.value = end
        }

        private val categoryTreeFlow =
            categoryRepository
                .observeActiveCategoryTree()
                .catch { emit(emptyList()) }
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

        private val displayFilters =
            combine(
                combine(noteQuery, periodStart, periodEnd, selectedAccountId) { note, start, end, accountId ->
                    DisplayFiltersPrimary(note, start, end, accountId)
                },
                combine(
                    selectedCategoryId,
                    selectedSubcategoryId,
                    amountMin,
                    amountMax,
                ) { categoryId, subcategoryId, min, max ->
                    DisplayFiltersSecondary(categoryId, subcategoryId, min, max)
                },
            ) { primary, secondary ->
                DisplayFilters(
                    noteQuery = primary.noteQuery,
                    periodStart = primary.periodStart,
                    periodEnd = primary.periodEnd,
                    accountId = primary.accountId,
                    categoryId = secondary.categoryId,
                    subcategoryId = secondary.subcategoryId,
                    amountMin = secondary.amountMin,
                    amountMax = secondary.amountMax,
                )
            }

        private val repositoryFilters =
            combine(
                combine(periodStart, periodEnd, selectedAccountId) { start, end, accountId ->
                    Triple(start, end, accountId)
                },
                combine(
                    selectedCategoryId,
                    selectedSubcategoryId,
                    lockedTransactionType,
                ) { categoryId, subcategoryId, type ->
                    Triple(categoryId, subcategoryId, type)
                },
            ) { primary, secondary ->
                RepositoryFilters(
                    periodStart = primary.first,
                    periodEnd = primary.second,
                    accountId = primary.third,
                    categoryId = secondary.first,
                    subcategoryId = secondary.second,
                    transactionType = secondary.third,
                )
            }.distinctUntilChanged()

        private val transactionsForFilters =
            repositoryFilters.flatMapLatest { params ->
                val (startMillis, endMillis) = resolvePeriodMillis(params.periodStart, params.periodEnd)
                transactionRepository
                    .getFilteredTransactions(
                        startDate = startMillis,
                        endDate = endMillis,
                        accountId = params.accountId,
                        categoryId = params.categoryId,
                        subcategoryId = params.subcategoryId,
                        type = params.transactionType,
                    ).catch { emit(emptyList()) }
            }

        private val shellMeta =
            combine(
                combine(
                    accountRepository.getAccounts().catch { emit(emptyList()) },
                    categoryTreeFlow,
                    showDateRangePicker,
                ) { accountEntities, tree, showDates ->
                    ShellMetaPrimary(accountEntities, tree, showDates)
                },
                combine(pickerHighlightedCategoryId, errorMessage) { highlight, error ->
                    ShellMetaSecondary(highlight, error)
                },
            ) { primary, secondary ->
                SearchShellMeta(
                    accounts = primary.accounts.map { SearchAccountUi(it.id, it.name, it.type) },
                    categories = mapCategoryTree(primary.categories),
                    showDateRangePicker = primary.showDateRangePicker,
                    pickerHighlightedCategoryId = secondary.pickerHighlightedCategoryId,
                    errorMessage = secondary.errorMessage,
                )
            }

        val uiState: StateFlow<TransactionSearchUiState> =
            combine(
                combine(
                    transactionsForFilters,
                    noteQuery.debounce(300),
                    displayFilters,
                ) { transactions, debouncedNote, display ->
                    Triple(transactions, debouncedNote, display)
                },
                combine(
                    shellMeta,
                    displaySettings,
                    filtersExpanded,
                    headerPeriodMode,
                ) { meta, displayPrefs, expanded, periodMode ->
                    SearchShell(meta, displayPrefs, expanded, periodMode)
                },
            ) { primary, secondary ->
                val (transactions, debouncedNote, display) = primary
                val shell = secondary
                val meta = shell.meta
                val displayPrefs = shell.displayPrefs
                val expanded = shell.expanded
                val periodMode = shell.periodMode
                val clientTrigger =
                    SearchTrigger(
                        noteQuery = debouncedNote,
                        periodStart = display.periodStart,
                        periodEnd = display.periodEnd,
                        accountId = display.accountId,
                        categoryId = display.categoryId,
                        subcategoryId = display.subcategoryId,
                        amountMin = display.amountMin,
                        amountMax = display.amountMax,
                    )
                val filtered = applyClientFilters(transactions, clientTrigger)
                val grouped = TransactionGrouping.groupByDate(filtered, zoneId)
                val incomeTotal =
                    filtered
                        .filter { it.transaction.type == TransactionType.INCOME }
                        .sumOf { it.transaction.amount }
                val expenseTotal =
                    filtered
                        .filter { it.transaction.type == TransactionType.EXPENSE }
                        .sumOf { it.transaction.amount }
                TransactionSearchUiState(
                    noteQuery = display.noteQuery,
                    periodStart = display.periodStart,
                    periodEnd = display.periodEnd,
                    selectedAccountId = display.accountId,
                    selectedCategoryId = display.categoryId,
                    selectedSubcategoryId = display.subcategoryId,
                    amountMin = display.amountMin,
                    amountMax = display.amountMax,
                    accounts = meta.accounts,
                    categories = meta.categories,
                    groupedResults = grouped,
                    isLoading = false,
                    showDateRangePicker = meta.showDateRangePicker,
                    pickerHighlightedCategoryId = meta.pickerHighlightedCategoryId,
                    errorMessage = meta.errorMessage,
                    isEmpty = grouped.isEmpty(),
                    totalIncome = incomeTotal,
                    totalExpense = expenseTotal,
                    amountColorScheme = displayPrefs.amountColorScheme,
                    filtersExpanded = expanded,
                    headerPeriodMode = periodMode,
                )
            }.stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = TransactionSearchUiState(isLoading = true),
            )

        fun onEvent(event: TransactionSearchUiEvent) {
            when (event) {
                is TransactionSearchUiEvent.OnNoteQueryChanged -> noteQuery.value = event.query
                TransactionSearchUiEvent.OnPeriodClick -> showDateRangePicker.value = true
                TransactionSearchUiEvent.OnDismissDateRangePicker -> showDateRangePicker.value = false
                is TransactionSearchUiEvent.OnDateRangeSelected -> {
                    periodStart.value = event.start
                    periodEnd.value = event.end
                    headerPeriodMode.value = SearchHeaderPeriodMode.CUSTOM
                    showDateRangePicker.value = false
                }
                TransactionSearchUiEvent.OnClearPeriod -> {
                    periodStart.value = null
                    periodEnd.value = null
                }
                is TransactionSearchUiEvent.OnAccountSelected -> selectedAccountId.value = event.accountId
                TransactionSearchUiEvent.OnCategoryRowClick -> {
                    pickerHighlightedCategoryId.value =
                        selectedCategoryId.value
                            ?: categoryTreeFlow.value
                                .firstOrNull()
                                ?.category
                                ?.id
                                ?.value
                }
                is TransactionSearchUiEvent.OnPickerParentSelected -> {
                    val category = uiState.value.categories.firstOrNull { it.id == event.categoryId }
                    if (category == null) return
                    if (category.subcategories.isEmpty()) {
                        selectedCategoryId.value = category.id
                        selectedSubcategoryId.value = null
                        pickerHighlightedCategoryId.value = category.id
                    } else {
                        pickerHighlightedCategoryId.value = category.id
                    }
                }
                is TransactionSearchUiEvent.OnPickerSubcategorySelected -> {
                    val parentId = pickerHighlightedCategoryId.value ?: return
                    selectedCategoryId.value = parentId
                    selectedSubcategoryId.value = event.subcategoryId
                }
                TransactionSearchUiEvent.OnPickerConfirmParentOnly -> {
                    val parentId = pickerHighlightedCategoryId.value ?: return
                    selectedCategoryId.value = parentId
                    selectedSubcategoryId.value = null
                }
                TransactionSearchUiEvent.OnClearCategory -> {
                    selectedCategoryId.value = null
                    selectedSubcategoryId.value = null
                    pickerHighlightedCategoryId.value = null
                }
                is TransactionSearchUiEvent.OnAmountMinChanged -> {
                    amountMin.value = MmrValidation.sanitizeDecimalInput(event.value)
                }
                is TransactionSearchUiEvent.OnAmountMaxChanged -> {
                    amountMax.value = MmrValidation.sanitizeDecimalInput(event.value)
                }
                TransactionSearchUiEvent.OnToggleFiltersExpanded -> {
                    filtersExpanded.value = !filtersExpanded.value
                }
                is TransactionSearchUiEvent.OnHeaderPeriodModeSelected -> {
                    if (event.mode == SearchHeaderPeriodMode.CUSTOM) {
                        headerPeriodMode.value = SearchHeaderPeriodMode.CUSTOM
                        showDateRangePicker.value = true
                    } else {
                        applyHeaderPeriodMode(event.mode)
                    }
                }
                TransactionSearchUiEvent.OnClearFilters -> {
                    noteQuery.value = ""
                    periodStart.value = null
                    periodEnd.value = null
                    selectedAccountId.value = null
                    selectedCategoryId.value = null
                    selectedSubcategoryId.value = null
                    amountMin.value = ""
                    amountMax.value = ""
                }
            }
        }

        private data class DisplayFiltersPrimary(
            val noteQuery: String,
            val periodStart: LocalDate?,
            val periodEnd: LocalDate?,
            val accountId: Long?,
        )

        private data class DisplayFiltersSecondary(
            val categoryId: Long?,
            val subcategoryId: Long?,
            val amountMin: String,
            val amountMax: String,
        )

        private data class ShellMetaPrimary(
            val accounts: List<AccountEntity>,
            val categories: List<CategoryWithChildren>,
            val showDateRangePicker: Boolean,
        )

        private data class ShellMetaSecondary(
            val pickerHighlightedCategoryId: Long?,
            val errorMessage: String?,
        )

        private data class DisplayFilters(
            val noteQuery: String,
            val periodStart: LocalDate?,
            val periodEnd: LocalDate?,
            val accountId: Long?,
            val categoryId: Long?,
            val subcategoryId: Long?,
            val amountMin: String,
            val amountMax: String,
        )

        private data class RepositoryFilters(
            val periodStart: LocalDate?,
            val periodEnd: LocalDate?,
            val accountId: Long?,
            val categoryId: Long?,
            val subcategoryId: Long?,
            val transactionType: TransactionType?,
        )

        private data class SearchTrigger(
            val noteQuery: String,
            val periodStart: LocalDate?,
            val periodEnd: LocalDate?,
            val accountId: Long?,
            val categoryId: Long?,
            val subcategoryId: Long?,
            val amountMin: String,
            val amountMax: String,
        )

        private data class SearchShell(
            val meta: SearchShellMeta,
            val displayPrefs: AppDisplaySettingsSnapshot,
            val expanded: Boolean,
            val periodMode: SearchHeaderPeriodMode,
        )

        private data class SearchShellMeta(
            val accounts: List<SearchAccountUi>,
            val categories: List<CategoryUi>,
            val showDateRangePicker: Boolean,
            val pickerHighlightedCategoryId: Long?,
            val errorMessage: String?,
        )

        private fun resolvePeriodMillis(
            start: LocalDate?,
            end: LocalDate?,
        ): Pair<Long, Long> {
            if (start == null || end == null) {
                return 0L to System.currentTimeMillis() + 86_400_000L
            }
            val orderedStart = minOf(start, end)
            val orderedEnd = maxOf(start, end)
            return HomePeriodUtils.dayStartMillis(orderedStart, zoneId) to
                HomePeriodUtils.dayEndMillis(orderedEnd, zoneId)
        }

        private fun applyClientFilters(
            items: List<TransactionWithDetails>,
            trigger: SearchTrigger,
        ): List<TransactionWithDetails> {
            val noteQ = trigger.noteQuery.trim().lowercase()
            val min = trigger.amountMin.toDoubleOrNull()
            val max = trigger.amountMax.toDoubleOrNull()
            return items.filter { item ->
                val noteMatch =
                    if (noteQ.isEmpty()) {
                        true
                    } else {
                        item.transaction.note
                            ?.lowercase()
                            ?.contains(noteQ) == true
                    }
                val amount = item.transaction.amount
                val minMatch = min == null || amount >= min
                val maxMatch = max == null || amount <= max
                noteMatch && minMatch && maxMatch
            }
        }

        private fun mapCategoryTree(tree: List<CategoryWithChildren>): List<CategoryUi> =
            tree.map { node ->
                CategoryUi(
                    id = node.category.id.value,
                    name = node.category.name,
                    colorHex = node.category.color.hex,
                    iconName = node.category.iconName,
                    subcategories =
                        node.subcategories.map { sub ->
                            SubcategoryUi(id = sub.id.value, name = sub.name)
                        },
                )
            }
    }
