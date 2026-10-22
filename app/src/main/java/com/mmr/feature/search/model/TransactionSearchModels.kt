package com.mmr.feature.search.model

import com.mmr.feature.home.model.TransactionGroupUi
import com.mmr.feature.transaction.model.CategoryUi
import com.mmr.feature.transaction.model.SubcategoryUi
import java.time.LocalDate

data class TransactionSearchUiState(
    val noteQuery: String = "",
    val periodStart: LocalDate? = null,
    val periodEnd: LocalDate? = null,
    val selectedAccountId: Long? = null,
    val selectedCategoryId: Long? = null,
    val selectedSubcategoryId: Long? = null,
    val amountMin: String = "",
    val amountMax: String = "",
    val accounts: List<SearchAccountUi> = emptyList(),
    val categories: List<CategoryUi> = emptyList(),
    val groupedResults: List<TransactionGroupUi> = emptyList(),
    val isLoading: Boolean = true,
    val showDateRangePicker: Boolean = false,
    val showCategoryPicker: Boolean = false,
    val pickerHighlightedCategoryId: Long? = null,
    val errorMessage: String? = null,
    val isEmpty: Boolean = false
)

data class SearchAccountUi(
    val id: Long,
    val name: String
)

sealed interface TransactionSearchUiEvent {
    data class OnNoteQueryChanged(val query: String) : TransactionSearchUiEvent
    object OnPeriodClick : TransactionSearchUiEvent
    object OnDismissDateRangePicker : TransactionSearchUiEvent
    data class OnDateRangeSelected(val start: LocalDate, val end: LocalDate) : TransactionSearchUiEvent
    object OnClearPeriod : TransactionSearchUiEvent
    data class OnAccountSelected(val accountId: Long?) : TransactionSearchUiEvent
    object OnCategoryRowClick : TransactionSearchUiEvent
    object OnDismissCategoryPicker : TransactionSearchUiEvent
    data class OnPickerParentSelected(val categoryId: Long) : TransactionSearchUiEvent
    data class OnPickerSubcategorySelected(val subcategoryId: Long) : TransactionSearchUiEvent
    object OnPickerConfirmParentOnly : TransactionSearchUiEvent
    object OnClearCategory : TransactionSearchUiEvent
    data class OnAmountMinChanged(val value: String) : TransactionSearchUiEvent
    data class OnAmountMaxChanged(val value: String) : TransactionSearchUiEvent
    object OnClearFilters : TransactionSearchUiEvent
}
