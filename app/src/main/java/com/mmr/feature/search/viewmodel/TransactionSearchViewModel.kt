package com.mmr.feature.search.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mmr.app.ui.state.MmrUiMessages
import com.mmr.core.common.validation.MmrValidation
import com.mmr.core.database.relation.TransactionWithDetails
import com.mmr.core.domain.repository.AccountRepository
import com.mmr.core.domain.repository.CategoryRepository
import com.mmr.core.domain.repository.TransactionRepository
import com.mmr.core.model.category.CategoryWithChildren
import com.mmr.feature.home.HomePeriodUtils
import com.mmr.feature.home.TransactionGrouping
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
class TransactionSearchViewModel @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val categoryRepository: CategoryRepository,
    private val accountRepository: AccountRepository
) : ViewModel() {

    private val zoneId = ZoneId.systemDefault()

    private val noteQuery = MutableStateFlow("")
    private val periodStart = MutableStateFlow<LocalDate?>(null)
    private val periodEnd = MutableStateFlow<LocalDate?>(null)
    private val selectedAccountId = MutableStateFlow<Long?>(null)
    private val selectedCategoryId = MutableStateFlow<Long?>(null)
    private val selectedSubcategoryId = MutableStateFlow<Long?>(null)
    private val amountMin = MutableStateFlow("")
    private val amountMax = MutableStateFlow("")
    private val showDateRangePicker = MutableStateFlow(false)
    private val showCategoryPicker = MutableStateFlow(false)
    private val pickerHighlightedCategoryId = MutableStateFlow<Long?>(null)
    private val errorMessage = MutableStateFlow<String?>(null)

    private val categoryTreeFlow = categoryRepository.observeActiveCategoryTree()
        .catch { emit(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val filterInputs: Flow<SearchTrigger> = combine(
        combine(
            noteQuery.debounce(300),
            periodStart,
            periodEnd,
            selectedAccountId
        ) { note, start, end, accountId ->
            PartialSearchTrigger(note, start, end, accountId)
        },
        combine(
            selectedCategoryId,
            selectedSubcategoryId,
            amountMin.debounce(300),
            amountMax.debounce(300)
        ) { categoryId, subcategoryId, min, max ->
            PartialSearchTrigger2(categoryId, subcategoryId, min, max)
        }
    ) { partial, partial2 ->
        SearchTrigger(
            noteQuery = partial.noteQuery,
            periodStart = partial.periodStart,
            periodEnd = partial.periodEnd,
            accountId = partial.accountId,
            categoryId = partial2.categoryId,
            subcategoryId = partial2.subcategoryId,
            amountMin = partial2.amountMin,
            amountMax = partial2.amountMax
        )
    }

    private val shellState = combine(
        filterInputs,
        combine(
            accountRepository.getAccounts().catch { emit(emptyList()) },
            categoryTreeFlow,
            showDateRangePicker,
            showCategoryPicker,
            pickerHighlightedCategoryId
        ) { accountEntities, tree, showDates, showCats, highlight ->
            SearchShellMeta(
                accounts = accountEntities.map { SearchAccountUi(it.id, it.name) },
                categories = mapCategoryTree(tree),
                showDateRangePicker = showDates,
                showCategoryPicker = showCats,
                pickerHighlightedCategoryId = highlight
            )
        },
        errorMessage
    ) { trigger, meta, error ->
        SearchShell(
            trigger = trigger,
            accounts = meta.accounts,
            categories = meta.categories,
            showDateRangePicker = meta.showDateRangePicker,
            showCategoryPicker = meta.showCategoryPicker,
            pickerHighlightedCategoryId = meta.pickerHighlightedCategoryId,
            errorMessage = error
        )
    }

    val uiState: StateFlow<TransactionSearchUiState> = shellState
        .flatMapLatest { shell ->
            val (startMillis, endMillis) = resolvePeriodMillis(shell.trigger.periodStart, shell.trigger.periodEnd)
            transactionRepository.getFilteredTransactions(
                startDate = startMillis,
                endDate = endMillis,
                accountId = shell.trigger.accountId,
                categoryId = shell.trigger.categoryId,
                subcategoryId = shell.trigger.subcategoryId,
                type = null
            ).map { transactions ->
                val filtered = applyClientFilters(transactions, shell.trigger)
                val grouped = TransactionGrouping.groupByDate(filtered, zoneId)
                TransactionSearchUiState(
                    noteQuery = shell.trigger.noteQuery,
                    periodStart = shell.trigger.periodStart,
                    periodEnd = shell.trigger.periodEnd,
                    selectedAccountId = shell.trigger.accountId,
                    selectedCategoryId = shell.trigger.categoryId,
                    selectedSubcategoryId = shell.trigger.subcategoryId,
                    amountMin = shell.trigger.amountMin,
                    amountMax = shell.trigger.amountMax,
                    accounts = shell.accounts,
                    categories = shell.categories,
                    groupedResults = grouped,
                    isLoading = false,
                    showDateRangePicker = shell.showDateRangePicker,
                    showCategoryPicker = shell.showCategoryPicker,
                    pickerHighlightedCategoryId = shell.pickerHighlightedCategoryId,
                    errorMessage = shell.errorMessage,
                    isEmpty = grouped.isEmpty()
                )
            }.catch { e ->
                emit(
                    TransactionSearchUiState(
                        noteQuery = shell.trigger.noteQuery,
                        periodStart = shell.trigger.periodStart,
                        periodEnd = shell.trigger.periodEnd,
                        selectedAccountId = shell.trigger.accountId,
                        selectedCategoryId = shell.trigger.categoryId,
                        selectedSubcategoryId = shell.trigger.subcategoryId,
                        amountMin = shell.trigger.amountMin,
                        amountMax = shell.trigger.amountMax,
                        accounts = shell.accounts,
                        categories = shell.categories,
                        isLoading = false,
                        showDateRangePicker = shell.showDateRangePicker,
                        showCategoryPicker = shell.showCategoryPicker,
                        pickerHighlightedCategoryId = shell.pickerHighlightedCategoryId,
                        errorMessage = MmrUiMessages.friendly(e.message, MmrUiMessages.GENERIC_LOAD),
                        isEmpty = true
                    )
                )
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = TransactionSearchUiState(isLoading = true)
        )

    fun onEvent(event: TransactionSearchUiEvent) {
        when (event) {
            is TransactionSearchUiEvent.OnNoteQueryChanged -> noteQuery.value = event.query
            TransactionSearchUiEvent.OnPeriodClick -> showDateRangePicker.value = true
            TransactionSearchUiEvent.OnDismissDateRangePicker -> showDateRangePicker.value = false
            is TransactionSearchUiEvent.OnDateRangeSelected -> {
                periodStart.value = event.start
                periodEnd.value = event.end
                showDateRangePicker.value = false
            }
            TransactionSearchUiEvent.OnClearPeriod -> {
                periodStart.value = null
                periodEnd.value = null
            }
            is TransactionSearchUiEvent.OnAccountSelected -> selectedAccountId.value = event.accountId
            TransactionSearchUiEvent.OnCategoryRowClick -> {
                showCategoryPicker.value = true
                pickerHighlightedCategoryId.value = selectedCategoryId.value
                    ?: categoryTreeFlow.value.firstOrNull()?.category?.id?.value
            }
            TransactionSearchUiEvent.OnDismissCategoryPicker -> showCategoryPicker.value = false
            is TransactionSearchUiEvent.OnPickerParentSelected -> {
                val category = uiState.value.categories.firstOrNull { it.id == event.categoryId }
                if (category == null) return
                if (category.subcategories.isEmpty()) {
                    selectedCategoryId.value = category.id
                    selectedSubcategoryId.value = null
                    pickerHighlightedCategoryId.value = category.id
                    showCategoryPicker.value = false
                } else {
                    pickerHighlightedCategoryId.value = category.id
                }
            }
            is TransactionSearchUiEvent.OnPickerSubcategorySelected -> {
                val parentId = pickerHighlightedCategoryId.value ?: return
                selectedCategoryId.value = parentId
                selectedSubcategoryId.value = event.subcategoryId
                showCategoryPicker.value = false
            }
            TransactionSearchUiEvent.OnPickerConfirmParentOnly -> {
                val parentId = pickerHighlightedCategoryId.value ?: return
                selectedCategoryId.value = parentId
                selectedSubcategoryId.value = null
                showCategoryPicker.value = false
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

    private data class PartialSearchTrigger(
        val noteQuery: String,
        val periodStart: LocalDate?,
        val periodEnd: LocalDate?,
        val accountId: Long?
    )

    private data class PartialSearchTrigger2(
        val categoryId: Long?,
        val subcategoryId: Long?,
        val amountMin: String,
        val amountMax: String
    )

    private data class SearchTrigger(
        val noteQuery: String,
        val periodStart: LocalDate?,
        val periodEnd: LocalDate?,
        val accountId: Long?,
        val categoryId: Long?,
        val subcategoryId: Long?,
        val amountMin: String,
        val amountMax: String
    )

    private data class SearchShellMeta(
        val accounts: List<SearchAccountUi>,
        val categories: List<CategoryUi>,
        val showDateRangePicker: Boolean,
        val showCategoryPicker: Boolean,
        val pickerHighlightedCategoryId: Long?
    )

    private data class SearchShell(
        val trigger: SearchTrigger,
        val accounts: List<SearchAccountUi>,
        val categories: List<CategoryUi>,
        val showDateRangePicker: Boolean,
        val showCategoryPicker: Boolean,
        val pickerHighlightedCategoryId: Long?,
        val errorMessage: String?
    )

    private fun resolvePeriodMillis(start: LocalDate?, end: LocalDate?): Pair<Long, Long> {
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
        trigger: SearchTrigger
    ): List<TransactionWithDetails> {
        val noteQ = trigger.noteQuery.trim().lowercase()
        val min = trigger.amountMin.toDoubleOrNull()
        val max = trigger.amountMax.toDoubleOrNull()
        return items.filter { item ->
            val noteMatch = if (noteQ.isEmpty()) {
                true
            } else {
                item.transaction.note?.lowercase()?.contains(noteQ) == true
            }
            val amount = item.transaction.amount
            val minMatch = min == null || amount >= min
            val maxMatch = max == null || amount <= max
            noteMatch && minMatch && maxMatch
        }
    }

    private fun mapCategoryTree(tree: List<CategoryWithChildren>): List<CategoryUi> {
        return tree.map { node ->
            CategoryUi(
                id = node.category.id.value,
                name = node.category.name,
                colorHex = node.category.color.hex,
                iconName = node.category.iconName,
                subcategories = node.subcategories.map { sub ->
                    SubcategoryUi(id = sub.id.value, name = sub.name)
                }
            )
        }
    }
}
