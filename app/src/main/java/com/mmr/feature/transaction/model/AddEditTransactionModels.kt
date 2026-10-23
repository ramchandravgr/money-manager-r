package com.mmr.feature.transaction.model

import com.mmr.core.loan.LoanLinkedTransactionDeleteMode
import com.mmr.core.model.enums.TransactionType

enum class AccountPickerTarget {
    SINGLE_ACCOUNT,
    TRANSFER_FROM,
}

data class LoanPickerUi(
    val id: Long,
    val name: String,
)

data class AddEditTransactionUiState(
    val isEditMode: Boolean = false,
    val isLoading: Boolean = false,
    val amount: String = "",
    val dateTimeMillis: Long = System.currentTimeMillis(),
    val selectedType: TransactionType = TransactionType.EXPENSE,
    val categories: List<CategoryUi> = emptyList(),
    val accounts: List<AccountUi> = emptyList(),
    val loans: List<LoanPickerUi> = emptyList(),
    val selectedCategoryId: Long? = null,
    val selectedSubcategoryId: Long? = null,
    val selectedAccountId: Long? = null,
    val selectedRelatedAccountId: Long? = null,
    val selectedTransferLoanId: Long? = null,
    val accountPickerVisible: Boolean = false,
    val accountPickerTarget: AccountPickerTarget = AccountPickerTarget.SINGLE_ACCOUNT,
    val transferToPickerVisible: Boolean = false,
    val note: String = "",
    val noteSuggestions: List<String> = emptyList(),
    val autocompleteEnabled: Boolean = true,
    val canSave: Boolean = false,
    val isSaved: Boolean = false,
    val loadErrorMessage: String? = null,
    val errorMessage: String? = null,
    val categoryPickerVisible: Boolean = false,
    val pickerHighlightedCategoryId: Long? = null,
    val pickerNameEditor: PickerNameEditorState? = null,
    val showLoanDeleteDialog: Boolean = false,
    val hasLinkedLoanPayment: Boolean = false,
    val showDeleteButton: Boolean = false,
    val isDuplicatePrefilled: Boolean = false,
)

enum class PickerNameEditorKind {
    ADD_CATEGORY,
    EDIT_CATEGORY,
    ADD_SUBCATEGORY,
    EDIT_SUBCATEGORY,
}

data class PickerNameEditorState(
    val kind: PickerNameEditorKind,
    val name: String,
    val categoryId: Long? = null,
    val subcategoryId: Long? = null,
    val parentCategoryId: Long? = null,
    val isSaving: Boolean = false,
    val errorMessage: String? = null,
)

data class CategoryUi(
    val id: Long,
    val name: String,
    val colorHex: String,
    val iconName: String = "category_default",
    val subcategories: List<SubcategoryUi> = emptyList(),
)

data class SubcategoryUi(
    val id: Long,
    val name: String,
)

data class AccountUi(
    val id: Long,
    val name: String,
    val type: com.mmr.core.model.enums.AccountType = com.mmr.core.model.enums.AccountType.BANK,
    val typeLabel: String = "Bank",
)

/** Accounts grouped by type; [TransferTo] adds a **Loans** section. */
enum class AccountPickerMode {
    AccountsGrouped,
    TransferTo,
}

sealed interface AddEditTransactionUiEvent {
    data class OnAmountChanged(
        val value: String,
    ) : AddEditTransactionUiEvent

    data class OnDateTimeChanged(
        val millis: Long,
    ) : AddEditTransactionUiEvent

    data class OnTypeSelected(
        val type: TransactionType,
    ) : AddEditTransactionUiEvent

    data class OnCategorySelected(
        val categoryId: Long,
    ) : AddEditTransactionUiEvent

    data class OnSubcategorySelected(
        val subcategoryId: Long?,
    ) : AddEditTransactionUiEvent

    data object OnCategoryRowClick : AddEditTransactionUiEvent

    data object OnDismissCategoryPicker : AddEditTransactionUiEvent

    data class OnPickerParentSelected(
        val categoryId: Long,
    ) : AddEditTransactionUiEvent

    data class OnPickerSubcategorySelected(
        val subcategoryId: Long,
    ) : AddEditTransactionUiEvent

    data object OnPickerConfirmParentOnly : AddEditTransactionUiEvent

    data object OnPickerAddCategory : AddEditTransactionUiEvent

    data class OnPickerEditCategory(
        val categoryId: Long,
    ) : AddEditTransactionUiEvent

    data object OnPickerAddSubcategory : AddEditTransactionUiEvent

    data class OnPickerEditSubcategory(
        val subcategoryId: Long,
    ) : AddEditTransactionUiEvent

    data class OnPickerNameEditorChanged(
        val name: String,
    ) : AddEditTransactionUiEvent

    data object OnPickerNameEditorSave : AddEditTransactionUiEvent

    data object OnPickerNameEditorDismiss : AddEditTransactionUiEvent

    data class OnAccountSelected(
        val accountId: Long,
    ) : AddEditTransactionUiEvent

    data object OnAccountRowClick : AddEditTransactionUiEvent

    data object OnFromAccountRowClick : AddEditTransactionUiEvent

    data object OnToAccountRowClick : AddEditTransactionUiEvent

    data object OnDismissAccountPicker : AddEditTransactionUiEvent

    data object OnDismissTransferToPicker : AddEditTransactionUiEvent

    data class OnTransferToAccountSelected(
        val accountId: Long,
    ) : AddEditTransactionUiEvent

    data class OnTransferToLoanSelected(
        val loanId: Long,
    ) : AddEditTransactionUiEvent

    data class OnNoteChanged(
        val value: String,
    ) : AddEditTransactionUiEvent

    object OnSaveClick : AddEditTransactionUiEvent

    object OnBackClick : AddEditTransactionUiEvent

    object OnDeleteClick : AddEditTransactionUiEvent

    object OnDismissLoanDeleteDialog : AddEditTransactionUiEvent

    data class OnConfirmDeleteWithLoanPolicy(
        val mode: LoanLinkedTransactionDeleteMode,
    ) : AddEditTransactionUiEvent
}
