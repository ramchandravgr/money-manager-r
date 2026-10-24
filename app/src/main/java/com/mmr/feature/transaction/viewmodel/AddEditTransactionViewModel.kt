package com.mmr.feature.transaction.viewmodel

import android.content.Context
import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mmr.app.ui.state.MmrUiMessages
import com.mmr.core.common.display.MmrDisplayTextFormat
import com.mmr.core.common.validation.MmrValidation
import com.mmr.core.data.preferences.AppDisplaySettingsStore
import com.mmr.core.data.repository.TransactionAttachmentRepositoryImpl
import com.mmr.core.database.entity.CategoryEntity
import com.mmr.core.database.entity.SubcategoryEntity
import com.mmr.core.database.entity.TransactionEntity
import com.mmr.core.domain.repository.AccountRepository
import com.mmr.core.domain.repository.ApprovalRepository
import com.mmr.core.domain.repository.CategoryRepository
import com.mmr.core.domain.repository.LoanRepository
import com.mmr.core.domain.repository.TransactionAttachmentRepository
import com.mmr.core.domain.repository.TransactionRepository
import com.mmr.core.loan.LoanLinkedTransactionDeleteMode
import com.mmr.core.model.category.CategoryWithChildren
import com.mmr.core.model.enums.CategoryType
import com.mmr.core.model.enums.RecordStatus
import com.mmr.core.model.enums.TransactionSourceType
import com.mmr.core.model.enums.TransactionType
import com.mmr.feature.category.model.CategoryColorPresets
import com.mmr.feature.transaction.model.AccountPickerTarget
import com.mmr.feature.transaction.model.AddEditTransactionUiEvent
import com.mmr.feature.transaction.model.AddEditTransactionUiState
import com.mmr.feature.transaction.model.CategoryUi
import com.mmr.feature.transaction.model.LoanPickerUi
import com.mmr.feature.transaction.model.PickerNameEditorKind
import com.mmr.feature.transaction.model.PickerNameEditorState
import com.mmr.feature.transaction.model.SubcategoryUi
import com.mmr.feature.transaction.model.TransactionReceiptUi
import com.mmr.feature.transaction.model.toAccountUi
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class AddEditTransactionViewModel
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
        private val transactionRepository: TransactionRepository,
        private val attachmentRepository: TransactionAttachmentRepository,
        private val approvalRepository: ApprovalRepository,
        private val categoryRepository: CategoryRepository,
        private val accountRepository: AccountRepository,
        private val loanRepository: LoanRepository,
        private val displaySettingsStore: AppDisplaySettingsStore,
        savedStateHandle: SavedStateHandle,
    ) : ViewModel() {
        private fun mapLoans(entities: List<com.mmr.core.database.entity.LoanEntity>): List<LoanPickerUi> =
            entities.map { LoanPickerUi(id = it.id, name = it.name) }

        private fun computeCanSave(state: AddEditTransactionUiState): Boolean {
            if (MmrValidation.parsePositiveAmount(state.amount) == null) return false
            if (state.selectedAccountId == null) return false
            if (state.selectedType == TransactionType.TRANSFER) {
                val hasDestination = state.selectedRelatedAccountId != null || state.selectedTransferLoanId != null
                if (!hasDestination) return false
                if (state.selectedRelatedAccountId != null &&
                    state.selectedRelatedAccountId == state.selectedAccountId
                ) {
                    return false
                }
            }
            return true
        }

        private val transactionId: Long? = savedStateHandle.get<Long>("transactionId")?.takeIf { it > 0 }
        private val approvalId: Long? = savedStateHandle.get<Long>("approvalId")?.takeIf { it > 0 }
        private val duplicateFromId: Long? =
            savedStateHandle.get<Long>("duplicateFromId")?.takeIf { it > 0 }
        private val prefillDateMillis: Long? =
            savedStateHandle.get<Long>("dateMillis")?.takeIf { it > 0 }

        private val zoneId = ZoneId.systemDefault()

        private val _uiState = MutableStateFlow(AddEditTransactionUiState(isLoading = true))
        val uiState: StateFlow<AddEditTransactionUiState> = _uiState.asStateFlow()

        private val categoryTreeCache = MutableStateFlow<List<CategoryWithChildren>>(emptyList())

        init {
            loadInitialData()
            viewModelScope.launch {
                displaySettingsStore.settings.collect { prefs ->
                    _uiState.update {
                        it.copy(
                            autocompleteEnabled = prefs.autocompleteEnabled,
                            noteSuggestions = if (prefs.autocompleteEnabled) it.noteSuggestions else emptyList(),
                        )
                    }
                }
            }
        }

        private fun refreshNoteSuggestions(query: String) {
            viewModelScope.launch {
                if (!displaySettingsStore.snapshot().autocompleteEnabled) {
                    _uiState.update { it.copy(noteSuggestions = emptyList()) }
                    return@launch
                }
                val trimmed = query.trim()
                val suggestions =
                    if (trimmed.isNotEmpty()) {
                        transactionRepository.suggestTransactionNotes(trimmed)
                    } else {
                        emptyList()
                    }
                _uiState.update { it.copy(noteSuggestions = suggestions) }
            }
        }

        private fun loadInitialData() {
            viewModelScope.launch {
                combine(
                    categoryRepository.observeActiveCategoryTree().catch { emit(emptyList()) },
                    accountRepository.getAccounts().catch { emit(emptyList()) },
                    loanRepository.getActiveLoans().catch { emit(emptyList()) },
                ) { categoryTree, accounts, loans ->
                    Triple(categoryTree, accounts, loans)
                }.collectLatest { (categoryTree, accounts, loans) ->
                    categoryTreeCache.value = categoryTree
                    val current = _uiState.value
                    val categories = categoriesForType(categoryTree, current.selectedType)
                    val accountUis = accounts.map { it.toAccountUi() }
                    val loanUis = mapLoans(loans)

                    val resolvedCategoryId =
                        resolveCategorySelection(
                            categories = categories,
                            preferredId = current.selectedCategoryId,
                        )
                    val resolvedSubcategoryId =
                        resolveSubcategorySelection(
                            categories = categories,
                            categoryId = resolvedCategoryId,
                            preferredId = current.selectedSubcategoryId,
                        )
                    val initialAccId = current.selectedAccountId ?: accountUis.firstOrNull()?.id

                    if (accountUis.isEmpty()) {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                categories = categories,
                                accounts = accountUis,
                                loans = loanUis,
                                selectedCategoryId = resolvedCategoryId,
                                selectedSubcategoryId = resolvedSubcategoryId,
                                loadErrorMessage = "Add an account before creating transactions.",
                                canSave = false,
                            )
                        }
                        return@collectLatest
                    }

                    if (duplicateFromId != null && transactionId == null && !current.isDuplicatePrefilled) {
                        val existing = transactionRepository.getTransactionDetailedById(duplicateFromId)
                        if (existing != null) {
                            val txn = existing.transaction
                            val cats = categoriesForType(categoryTree, txn.type)
                            val catId = resolveCategorySelection(cats, txn.categoryId)
                            val subId = resolveSubcategorySelection(cats, catId, txn.subcategoryId)
                            _uiState.update {
                                val loaded =
                                    it.copy(
                                        isEditMode = false,
                                        isDuplicatePrefilled = true,
                                        isLoading = false,
                                        amount = txn.amount.toString(),
                                        selectedType = txn.type,
                                        categories = cats,
                                        accounts = accountUis,
                                        loans = loanUis,
                                        selectedCategoryId = catId,
                                        selectedSubcategoryId = subId,
                                        selectedAccountId = txn.accountId,
                                        selectedRelatedAccountId = txn.relatedAccountId,
                                        selectedTransferLoanId = txn.loanId,
                                        note = txn.note ?: "",
                                        dateTimeMillis = prefillDateMillis ?: txn.dateTime,
                                        showDeleteButton = false,
                                    )
                                loaded.copy(canSave = computeCanSave(loaded))
                            }
                            return@collectLatest
                        } else {
                            _uiState.update {
                                it.copy(
                                    isLoading = false,
                                    loadErrorMessage = "Transaction not found.",
                                    canSave = false,
                                )
                            }
                            return@collectLatest
                        }
                    }

                    if (transactionId != null && !current.isEditMode) {
                        val existing = transactionRepository.getTransactionDetailedById(transactionId)
                        if (existing != null) {
                            val txn = existing.transaction
                            val linkedLoan = transactionRepository.hasLinkedLoanPayment(txn.id)
                            val cats = categoriesForType(categoryTree, txn.type)
                            val catId = resolveCategorySelection(cats, txn.categoryId)
                            val subId = resolveSubcategorySelection(cats, catId, txn.subcategoryId)
                            val receiptUis = loadReceiptsForTransaction(txn.id)
                            _uiState.update {
                                val loaded =
                                    it.copy(
                                        isEditMode = true,
                                        isLoading = false,
                                        amount = txn.amount.toString(),
                                        selectedType = txn.type,
                                        categories = cats,
                                        accounts = accountUis,
                                        loans = loanUis,
                                        selectedCategoryId = catId,
                                        selectedSubcategoryId = subId,
                                        selectedAccountId = txn.accountId,
                                        selectedRelatedAccountId = txn.relatedAccountId,
                                        selectedTransferLoanId = txn.loanId,
                                        note = txn.note ?: "",
                                        dateTimeMillis = txn.dateTime,
                                        hasLinkedLoanPayment = linkedLoan,
                                        showDeleteButton = true,
                                        receipts = receiptUis,
                                    )
                                loaded.copy(canSave = computeCanSave(loaded))
                            }
                            return@collectLatest
                        } else {
                            _uiState.update {
                                it.copy(
                                    isLoading = false,
                                    loadErrorMessage = "Transaction not found.",
                                    canSave = false,
                                )
                            }
                            return@collectLatest
                        }
                    }

                    if (approvalId != null && !current.isEditMode) {
                        val pending = approvalRepository.getApprovalById(approvalId)
                        if (pending != null) {
                            val cats = categoriesForType(categoryTree, pending.extractedType)
                            val catId = resolveCategorySelection(cats, pending.extractedCategoryId)
                            _uiState.update {
                                val loaded =
                                    it.copy(
                                        isEditMode = true,
                                        isLoading = false,
                                        amount = pending.extractedAmount.toString(),
                                        selectedType = pending.extractedType,
                                        categories = cats,
                                        accounts = accountUis,
                                        loans = loanUis,
                                        selectedCategoryId = catId,
                                        selectedSubcategoryId = null,
                                        selectedAccountId = pending.extractedAccountId ?: initialAccId,
                                        note = pending.extractedNote ?: "",
                                        dateTimeMillis = pending.extractedDateTime,
                                    )
                                loaded.copy(canSave = computeCanSave(loaded))
                            }
                            return@collectLatest
                        } else {
                            _uiState.update {
                                it.copy(
                                    isLoading = false,
                                    loadErrorMessage = "Pending approval entry not found.",
                                    canSave = false,
                                )
                            }
                            return@collectLatest
                        }
                    }

                    _uiState.update {
                        val next =
                            it.copy(
                                isLoading = false,
                                categories = categories,
                                accounts = accountUis,
                                loans = loanUis,
                                selectedCategoryId = resolvedCategoryId,
                                selectedSubcategoryId = resolvedSubcategoryId,
                                selectedAccountId = initialAccId,
                                dateTimeMillis = prefillDateMillis ?: it.dateTimeMillis,
                            )
                        next.copy(canSave = computeCanSave(next))
                    }
                }
            }
        }

        fun onEvent(event: AddEditTransactionUiEvent) {
            when (event) {
                is AddEditTransactionUiEvent.OnAmountChanged -> {
                    val clean = MmrValidation.sanitizeDecimalInput(event.value)
                    _uiState.update {
                        val next = it.copy(amount = clean)
                        next.copy(canSave = computeCanSave(next))
                    }
                }

                is AddEditTransactionUiEvent.OnDateTimeChanged -> {
                    _uiState.update { it.copy(dateTimeMillis = event.millis) }
                }

                is AddEditTransactionUiEvent.OnTypeSelected -> {
                    _uiState.update { state ->
                        val categories = categoriesForType(categoryTreeCache.value, event.type)
                        val categoryId = resolveCategorySelection(categories, state.selectedCategoryId)
                        val subcategoryId = resolveSubcategorySelection(categories, categoryId, null)
                        val next =
                            state.copy(
                                selectedType = event.type,
                                categories = categories,
                                selectedCategoryId = categoryId,
                                selectedSubcategoryId = subcategoryId,
                                selectedRelatedAccountId =
                                    if (event.type == TransactionType.TRANSFER) {
                                        state.selectedRelatedAccountId
                                    } else {
                                        null
                                    },
                                selectedTransferLoanId =
                                    if (event.type == TransactionType.TRANSFER) {
                                        state.selectedTransferLoanId
                                    } else {
                                        null
                                    },
                            )
                        next.copy(canSave = computeCanSave(next))
                    }
                }

                is AddEditTransactionUiEvent.OnCategorySelected -> {
                    _uiState.update { state ->
                        val subcategoryId =
                            resolveSubcategorySelection(
                                categories = state.categories,
                                categoryId = event.categoryId,
                                preferredId = null,
                            )
                        state.copy(
                            selectedCategoryId = event.categoryId,
                            selectedSubcategoryId = subcategoryId,
                        )
                    }
                }

                is AddEditTransactionUiEvent.OnSubcategorySelected -> {
                    _uiState.update { state ->
                        val valid =
                            resolveSubcategorySelection(
                                categories = state.categories,
                                categoryId = state.selectedCategoryId,
                                preferredId = event.subcategoryId,
                            )
                        state.copy(selectedSubcategoryId = valid)
                    }
                }

                AddEditTransactionUiEvent.OnCategoryRowClick -> {
                    _uiState.update { state ->
                        val highlight =
                            state.selectedCategoryId
                                ?: state.categories.firstOrNull()?.id
                        state.copy(
                            categoryPickerVisible = true,
                            pickerHighlightedCategoryId = highlight,
                        )
                    }
                }

                AddEditTransactionUiEvent.OnDismissCategoryPicker -> {
                    _uiState.update { it.copy(categoryPickerVisible = false) }
                }

                is AddEditTransactionUiEvent.OnPickerParentSelected -> {
                    _uiState.update { state ->
                        val category = state.categories.firstOrNull { it.id == event.categoryId }
                        if (category == null) {
                            return@update state
                        }
                        if (category.subcategories.isEmpty()) {
                            state.copy(
                                selectedCategoryId = category.id,
                                selectedSubcategoryId = null,
                                pickerHighlightedCategoryId = category.id,
                                categoryPickerVisible = false,
                            )
                        } else {
                            state.copy(pickerHighlightedCategoryId = category.id)
                        }
                    }
                }

                is AddEditTransactionUiEvent.OnPickerSubcategorySelected -> {
                    _uiState.update { state ->
                        val parentId =
                            state.pickerHighlightedCategoryId
                                ?: state.selectedCategoryId
                        if (parentId == null) return@update state
                        val validSub =
                            resolveSubcategorySelection(
                                categories = state.categories,
                                categoryId = parentId,
                                preferredId = event.subcategoryId,
                            )
                        state.copy(
                            selectedCategoryId = parentId,
                            selectedSubcategoryId = validSub,
                            categoryPickerVisible = false,
                            pickerHighlightedCategoryId = parentId,
                        )
                    }
                }

                AddEditTransactionUiEvent.OnPickerConfirmParentOnly -> {
                    _uiState.update { state ->
                        val parentId =
                            state.pickerHighlightedCategoryId
                                ?: state.selectedCategoryId
                        if (parentId == null) return@update state
                        state.copy(
                            selectedCategoryId = parentId,
                            selectedSubcategoryId = null,
                            categoryPickerVisible = false,
                            pickerHighlightedCategoryId = parentId,
                        )
                    }
                }

                AddEditTransactionUiEvent.OnPickerAddCategory -> {
                    _uiState.update {
                        it.copy(
                            pickerNameEditor =
                                PickerNameEditorState(
                                    kind = PickerNameEditorKind.ADD_CATEGORY,
                                    name = "",
                                ),
                        )
                    }
                }

                is AddEditTransactionUiEvent.OnPickerEditCategory -> {
                    val category = _uiState.value.categories.firstOrNull { it.id == event.categoryId }
                    if (category == null) return
                    _uiState.update {
                        it.copy(
                            pickerNameEditor =
                                PickerNameEditorState(
                                    kind = PickerNameEditorKind.EDIT_CATEGORY,
                                    name = category.name,
                                    categoryId = category.id,
                                ),
                        )
                    }
                }

                AddEditTransactionUiEvent.OnPickerAddSubcategory -> {
                    val parentId =
                        _uiState.value.pickerHighlightedCategoryId
                            ?: _uiState.value.selectedCategoryId
                    if (parentId == null) {
                        _uiState.update { it.copy(errorMessage = "Select a category first.") }
                        return
                    }
                    _uiState.update {
                        it.copy(
                            pickerNameEditor =
                                PickerNameEditorState(
                                    kind = PickerNameEditorKind.ADD_SUBCATEGORY,
                                    name = "",
                                    parentCategoryId = parentId,
                                ),
                        )
                    }
                }

                is AddEditTransactionUiEvent.OnPickerEditSubcategory -> {
                    val parentId =
                        _uiState.value.pickerHighlightedCategoryId
                            ?: _uiState.value.selectedCategoryId
                    val parent = _uiState.value.categories.firstOrNull { it.id == parentId }
                    val sub = parent?.subcategories?.firstOrNull { it.id == event.subcategoryId }
                    if (sub == null) return
                    _uiState.update {
                        it.copy(
                            pickerNameEditor =
                                PickerNameEditorState(
                                    kind = PickerNameEditorKind.EDIT_SUBCATEGORY,
                                    name = sub.name,
                                    subcategoryId = sub.id,
                                    parentCategoryId = parentId,
                                ),
                        )
                    }
                }

                is AddEditTransactionUiEvent.OnPickerNameEditorChanged -> {
                    _uiState.update { state ->
                        val editor = state.pickerNameEditor ?: return@update state
                        state.copy(
                            pickerNameEditor =
                                editor.copy(
                                    name = event.name,
                                    errorMessage = null,
                                ),
                        )
                    }
                }

                AddEditTransactionUiEvent.OnPickerNameEditorDismiss -> {
                    _uiState.update { it.copy(pickerNameEditor = null) }
                }

                AddEditTransactionUiEvent.OnPickerNameEditorSave -> {
                    savePickerNameEditor()
                }

                is AddEditTransactionUiEvent.OnAccountSelected -> {
                    _uiState.update { state ->
                        val next =
                            state.copy(
                                selectedAccountId = event.accountId,
                                selectedRelatedAccountId =
                                    if (state.selectedRelatedAccountId == event.accountId) {
                                        null
                                    } else {
                                        state.selectedRelatedAccountId
                                    },
                            )
                        next.copy(canSave = computeCanSave(next))
                    }
                }

                AddEditTransactionUiEvent.OnAccountRowClick -> {
                    _uiState.update {
                        it.copy(
                            accountPickerVisible = true,
                            accountPickerTarget = AccountPickerTarget.SINGLE_ACCOUNT,
                        )
                    }
                }

                AddEditTransactionUiEvent.OnFromAccountRowClick -> {
                    _uiState.update {
                        it.copy(
                            accountPickerVisible = true,
                            accountPickerTarget = AccountPickerTarget.TRANSFER_FROM,
                        )
                    }
                }

                AddEditTransactionUiEvent.OnToAccountRowClick -> {
                    _uiState.update { it.copy(transferToPickerVisible = true) }
                }

                AddEditTransactionUiEvent.OnDismissAccountPicker -> {
                    _uiState.update { it.copy(accountPickerVisible = false) }
                }

                AddEditTransactionUiEvent.OnDismissTransferToPicker -> {
                    _uiState.update { it.copy(transferToPickerVisible = false) }
                }

                is AddEditTransactionUiEvent.OnTransferToAccountSelected -> {
                    _uiState.update { state ->
                        val next =
                            state.copy(
                                selectedRelatedAccountId = event.accountId,
                                selectedTransferLoanId = null,
                                transferToPickerVisible = false,
                            )
                        next.copy(canSave = computeCanSave(next))
                    }
                }

                is AddEditTransactionUiEvent.OnTransferToLoanSelected -> {
                    _uiState.update { state ->
                        val next =
                            state.copy(
                                selectedTransferLoanId = event.loanId,
                                selectedRelatedAccountId = null,
                                transferToPickerVisible = false,
                            )
                        next.copy(canSave = computeCanSave(next))
                    }
                }

                is AddEditTransactionUiEvent.OnNoteChanged -> {
                    val formatted = MmrDisplayTextFormat.capitalizeAfterWordBoundaries(event.value)
                    _uiState.update { it.copy(note = formatted) }
                    refreshNoteSuggestions(formatted)
                }

                is AddEditTransactionUiEvent.OnReceiptPicked -> {
                    viewModelScope.launch { handleReceiptPicked(event.uriString) }
                }

                is AddEditTransactionUiEvent.OnRemoveReceipt -> {
                    viewModelScope.launch { removeSavedReceipt(event.attachmentId) }
                }

                is AddEditTransactionUiEvent.OnRemovePendingReceipt -> {
                    removePendingReceipt(event.filePath)
                }

                is AddEditTransactionUiEvent.OnSaveClick -> {
                    saveTransaction()
                }

                AddEditTransactionUiEvent.OnDeleteClick -> {
                    if (transactionId == null) return
                    _uiState.update {
                        if (it.hasLinkedLoanPayment) {
                            it.copy(showLoanDeleteDialog = true)
                        } else {
                            it
                        }
                    }
                    if (!_uiState.value.hasLinkedLoanPayment) {
                        deleteTransaction(LoanLinkedTransactionDeleteMode.UNLINK)
                    }
                }

                AddEditTransactionUiEvent.OnDismissLoanDeleteDialog -> {
                    _uiState.update { it.copy(showLoanDeleteDialog = false) }
                }

                is AddEditTransactionUiEvent.OnConfirmDeleteWithLoanPolicy -> {
                    _uiState.update { it.copy(showLoanDeleteDialog = false) }
                    deleteTransaction(event.mode)
                }

                is AddEditTransactionUiEvent.OnBackClick -> {}
            }
        }

        private fun savePickerNameEditor() {
            val state = _uiState.value
            val editor = state.pickerNameEditor ?: return
            val trimmed = editor.name.trim()
            if (trimmed.isEmpty()) {
                _uiState.update {
                    it.copy(
                        pickerNameEditor = editor.copy(errorMessage = "Name cannot be empty."),
                    )
                }
                return
            }

            viewModelScope.launch {
                _uiState.update {
                    it.copy(pickerNameEditor = editor.copy(isSaving = true, errorMessage = null))
                }
                try {
                    when (editor.kind) {
                        PickerNameEditorKind.ADD_CATEGORY -> {
                            val newId =
                                categoryRepository.createCategory(
                                    CategoryEntity(
                                        name = trimmed,
                                        colorHex = CategoryColorPresets.defaultHex,
                                        type = transactionTypeToCategoryType(state.selectedType),
                                        iconName = "category_default",
                                        orderIndex = state.categories.size,
                                    ),
                                )
                            _uiState.update {
                                it.copy(
                                    pickerNameEditor = null,
                                    pickerHighlightedCategoryId = newId,
                                    selectedCategoryId = newId,
                                    selectedSubcategoryId = null,
                                )
                            }
                        }

                        PickerNameEditorKind.EDIT_CATEGORY -> {
                            val categoryId =
                                editor.categoryId
                                    ?: throw IllegalArgumentException("Category not found.")
                            val existing =
                                categoryRepository.resolveCategory(categoryId)
                                    ?: throw IllegalArgumentException("Category not found.")
                            if (existing.isDefault) {
                                throw IllegalArgumentException("Default categories cannot be renamed here.")
                            }
                            categoryRepository.updateCategory(existing.copy(name = trimmed))
                            _uiState.update {
                                it.copy(
                                    pickerNameEditor = null,
                                    pickerHighlightedCategoryId = categoryId,
                                )
                            }
                        }

                        PickerNameEditorKind.ADD_SUBCATEGORY -> {
                            val parentId =
                                editor.parentCategoryId
                                    ?: throw IllegalArgumentException("Parent category not found.")
                            val parent = state.categories.firstOrNull { it.id == parentId }
                            val order = parent?.subcategories?.size ?: 0
                            val newSubId =
                                categoryRepository.createSubcategory(
                                    SubcategoryEntity(
                                        categoryId = parentId,
                                        name = trimmed,
                                        orderIndex = order,
                                    ),
                                )
                            _uiState.update {
                                it.copy(
                                    pickerNameEditor = null,
                                    pickerHighlightedCategoryId = parentId,
                                    selectedCategoryId = parentId,
                                    selectedSubcategoryId = newSubId,
                                )
                            }
                        }

                        PickerNameEditorKind.EDIT_SUBCATEGORY -> {
                            val subcategoryId =
                                editor.subcategoryId
                                    ?: throw IllegalArgumentException("Subcategory not found.")
                            val existing =
                                categoryRepository.resolveSubcategory(subcategoryId)
                                    ?: throw IllegalArgumentException("Subcategory not found.")
                            categoryRepository.updateSubcategory(
                                existing.copy(
                                    categoryId = existing.categoryId,
                                    name = trimmed,
                                ),
                            )
                            _uiState.update {
                                it.copy(
                                    pickerNameEditor = null,
                                    pickerHighlightedCategoryId =
                                        editor.parentCategoryId
                                            ?: existing.categoryId,
                                )
                            }
                        }
                    }
                } catch (e: Exception) {
                    _uiState.update { current ->
                        val ed = current.pickerNameEditor ?: return@update current
                        current.copy(
                            pickerNameEditor =
                                ed.copy(
                                    isSaving = false,
                                    errorMessage = MmrUiMessages.friendly(e.message, MmrUiMessages.GENERIC_SAVE),
                                ),
                        )
                    }
                }
            }
        }

        private fun transactionTypeToCategoryType(type: TransactionType): CategoryType =
            when (type) {
                TransactionType.INCOME -> CategoryType.INCOME
                TransactionType.TRANSFER -> CategoryType.EXPENSE
                else -> CategoryType.EXPENSE
            }

        /** Selected calendar date with the clock time at save (manual new / duplicate). */
        private fun manualEntryDateTimeMillis(selectedDateMillis: Long): Long {
            val date = Instant.ofEpochMilli(selectedDateMillis).atZone(zoneId).toLocalDate()
            return date
                .atTime(LocalTime.now())
                .atZone(zoneId)
                .toInstant()
                .toEpochMilli()
        }

        private fun deleteTransaction(mode: LoanLinkedTransactionDeleteMode) {
            val id = transactionId ?: return
            viewModelScope.launch {
                try {
                    val existing =
                        transactionRepository.getTransactionDetailedById(id)
                            ?: throw IllegalArgumentException("Transaction not found.")
                    transactionRepository.deleteTransactionWithLoanPolicy(existing.transaction, mode)
                    _uiState.update { it.copy(isSaved = true) }
                } catch (e: Exception) {
                    _uiState.update {
                        it.copy(errorMessage = MmrUiMessages.friendly(e.message, MmrUiMessages.GENERIC_ACTION))
                    }
                }
            }
        }

        private fun saveTransaction() {
            val state = _uiState.value
            val amount =
                MmrValidation.parsePositiveAmount(state.amount) ?: run {
                    _uiState.update { it.copy(errorMessage = "Enter a valid amount greater than zero.") }
                    return
                }
            val accountId =
                state.selectedAccountId ?: run {
                    _uiState.update { it.copy(errorMessage = "Select an account.") }
                    return
                }
            if (state.selectedType == TransactionType.TRANSFER) {
                if (state.selectedRelatedAccountId == null && state.selectedTransferLoanId == null) {
                    _uiState.update { it.copy(errorMessage = "Select where to transfer.") }
                    return
                }
                if (state.selectedRelatedAccountId != null && state.selectedRelatedAccountId == accountId) {
                    _uiState.update { it.copy(errorMessage = "From and to accounts must be different.") }
                    return
                }
            }
            val relatedAccountId =
                if (state.selectedType == TransactionType.TRANSFER) {
                    state.selectedRelatedAccountId
                } else {
                    null
                }
            val loanId =
                if (state.selectedType == TransactionType.TRANSFER) {
                    state.selectedTransferLoanId
                } else {
                    null
                }
            val note = state.note.trim().takeIf { it.isNotEmpty() }
            val subcategoryId =
                resolveSubcategorySelection(
                    categories = state.categories,
                    categoryId = state.selectedCategoryId,
                    preferredId = state.selectedSubcategoryId,
                )

            viewModelScope.launch {
                try {
                    if (state.isEditMode && approvalId != null) {
                        approvalRepository.updateApproval(
                            id = approvalId,
                            amount = amount,
                            type = state.selectedType,
                            categoryId = state.selectedCategoryId,
                            accountId = accountId,
                            note = note,
                            dateTimeMillis = state.dateTimeMillis,
                        )
                    } else if (state.isEditMode && transactionId != null) {
                        val existing =
                            transactionRepository.getTransactionDetailedById(transactionId)
                                ?: throw IllegalArgumentException("Transaction not found.")
                        transactionRepository.updateTransaction(
                            existing.transaction.copy(
                                amount = amount,
                                type = state.selectedType,
                                dateTime = state.dateTimeMillis,
                                accountId = accountId,
                                relatedAccountId = relatedAccountId,
                                loanId = loanId,
                                categoryId = state.selectedCategoryId,
                                subcategoryId = subcategoryId,
                                note = note,
                                updatedAt = System.currentTimeMillis(),
                                isEdited = true,
                            ),
                        )
                        persistPendingReceipts(transactionId, state.receipts)
                    } else {
                        val savedAt = manualEntryDateTimeMillis(state.dateTimeMillis)
                        val newId =
                            transactionRepository.insertTransaction(
                                TransactionEntity(
                                    amount = amount,
                                    type = state.selectedType,
                                    dateTime = savedAt,
                                    accountId = accountId,
                                    relatedAccountId = relatedAccountId,
                                    loanId = loanId,
                                    categoryId = state.selectedCategoryId,
                                    subcategoryId = subcategoryId,
                                    note = note,
                                    sourceType = TransactionSourceType.MANUAL,
                                    status = RecordStatus.APPROVED,
                                ),
                            )
                        persistPendingReceipts(newId, state.receipts)
                    }
                    _uiState.update { it.copy(isSaved = true) }
                } catch (e: Exception) {
                    _uiState.update {
                        it.copy(errorMessage = MmrUiMessages.friendly(e.message, MmrUiMessages.GENERIC_SAVE))
                    }
                }
            }
        }

        private fun categoriesForType(
            tree: List<CategoryWithChildren>,
            transactionType: TransactionType,
        ): List<CategoryUi> {
            val filtered =
                when (transactionType) {
                    TransactionType.EXPENSE -> tree.filter { it.category.type == CategoryType.EXPENSE }
                    TransactionType.INCOME -> tree.filter { it.category.type == CategoryType.INCOME }
                    TransactionType.TRANSFER -> tree.filter { it.category.type == CategoryType.EXPENSE }
                    else -> tree
                }
            return filtered.map { node ->
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

        private fun resolveCategorySelection(
            categories: List<CategoryUi>,
            preferredId: Long?,
        ): Long? {
            if (preferredId != null && categories.any { it.id == preferredId }) {
                return preferredId
            }
            return categories.firstOrNull()?.id
        }

        private fun resolveSubcategorySelection(
            categories: List<CategoryUi>,
            categoryId: Long?,
            preferredId: Long?,
        ): Long? {
            if (categoryId == null) return null
            val subs = categories.firstOrNull { it.id == categoryId }?.subcategories ?: emptyList()
            if (subs.isEmpty()) return null
            if (preferredId != null && subs.any { it.id == preferredId }) {
                return preferredId
            }
            return null
        }

        private suspend fun loadReceiptsForTransaction(transactionId: Long): List<TransactionReceiptUi> =
            attachmentRepository.listForTransaction(transactionId).mapNotNull { entity ->
                val path = attachmentRepository.localFile(entity)?.absolutePath ?: return@mapNotNull null
                TransactionReceiptUi(id = entity.id, filePath = path)
            }

        private suspend fun handleReceiptPicked(uriString: String) {
            val uri = Uri.parse(uriString)
            val state = _uiState.value
            if (state.receipts.size >= TransactionAttachmentRepositoryImpl.MAX_ATTACHMENTS_PER_TRANSACTION) {
                _uiState.update { it.copy(errorMessage = "At most 5 receipt photos per transaction.") }
                return
            }
            try {
                if (state.isEditMode && transactionId != null) {
                    attachmentRepository.addFromUri(transactionId, uri)
                    _uiState.update {
                        it.copy(
                            receipts = loadReceiptsForTransaction(transactionId),
                            errorMessage = null,
                        )
                    }
                } else {
                    val staged = stagePendingReceiptFile(uri)
                    _uiState.update {
                        it.copy(
                            receipts = it.receipts + TransactionReceiptUi(id = 0L, filePath = staged.absolutePath),
                            errorMessage = null,
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(errorMessage = MmrUiMessages.friendly(e.message, "Could not add receipt."))
                }
            }
        }

        private suspend fun removeSavedReceipt(attachmentId: Long) {
            try {
                attachmentRepository.delete(attachmentId)
                if (transactionId != null) {
                    _uiState.update { it.copy(receipts = loadReceiptsForTransaction(transactionId)) }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(errorMessage = MmrUiMessages.friendly(e.message, MmrUiMessages.GENERIC_ACTION))
                }
            }
        }

        private fun removePendingReceipt(filePath: String) {
            runCatching { File(filePath).delete() }
            _uiState.update { state ->
                state.copy(receipts = state.receipts.filterNot { it.id == 0L && it.filePath == filePath })
            }
        }

        private suspend fun persistPendingReceipts(
            txnId: Long,
            receipts: List<TransactionReceiptUi>,
        ) {
            for (receipt in receipts.filter { it.id == 0L }) {
                val bytes = File(receipt.filePath).readBytes()
                val mimeType = context.contentResolver.getType(Uri.fromFile(File(receipt.filePath))) ?: "image/jpeg"
                attachmentRepository.addFromBytes(txnId, mimeType, bytes)
                runCatching { File(receipt.filePath).delete() }
            }
        }

        private fun stagePendingReceiptFile(uri: Uri): File {
            val dir = File(context.cacheDir, "pending_receipts").apply { mkdirs() }
            val dest = File(dir, "pending_${UUID.randomUUID()}.img")
            context.contentResolver.openInputStream(uri)?.use { input ->
                dest.outputStream().use { output -> input.copyTo(output) }
            } ?: throw IllegalArgumentException("Could not read image.")
            if (dest.length() > TransactionAttachmentRepositoryImpl.MAX_ATTACHMENT_BYTES) {
                dest.delete()
                throw IllegalArgumentException("Image is too large.")
            }
            return dest
        }
    }
