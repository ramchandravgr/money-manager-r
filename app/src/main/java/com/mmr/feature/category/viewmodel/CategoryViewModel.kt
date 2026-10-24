package com.mmr.feature.category.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mmr.app.ui.state.MmrUiMessages
import com.mmr.core.database.entity.CategoryEntity
import com.mmr.core.database.entity.SubcategoryEntity
import com.mmr.core.domain.repository.CategoryRepository
import com.mmr.core.model.category.CategoryWithChildren
import com.mmr.core.model.enums.CategoryType
import com.mmr.feature.category.model.CategoryTreeItemUi
import com.mmr.feature.category.model.CategoryUiEvent
import com.mmr.feature.category.model.CategoryUiState
import com.mmr.feature.category.model.SubcategoryItemUi
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CategoryViewModel
    @Inject
    constructor(
        private val categoryRepository: CategoryRepository,
    ) : ViewModel() {
        private val _typeFilter = MutableStateFlow<CategoryType?>(null)
        private val _retryKey = MutableStateFlow(0)
        private val _selectedCategoryId = MutableStateFlow<Long?>(null)
        private val _selectedSubcategoryId = MutableStateFlow<Long?>(null)
        private val _loadErrorMessage = MutableStateFlow<String?>(null)
        private val _statusMessage = MutableStateFlow<String?>(null)
        private val _isSaving = MutableStateFlow(false)

        private val categoryTreeFlow =
            combine(_typeFilter, _retryKey) { filter, _ -> filter }
                .flatMapLatest { filter ->
                    when (filter) {
                        null -> categoryRepository.observeActiveCategoryTree()
                        else -> categoryRepository.observeActiveCategoryTreeByType(filter)
                    }.catch { e ->
                        _loadErrorMessage.value = MmrUiMessages.friendly(e.message, MmrUiMessages.GENERIC_LOAD)
                        emit(emptyList())
                    }
                }

        private val selectionAndMessages =
            combine(
                combine(
                    _typeFilter,
                    _selectedCategoryId,
                    _selectedSubcategoryId,
                    _loadErrorMessage,
                    _statusMessage,
                ) { typeFilter, selectedCategoryId, selectedSubcategoryId, loadError, status ->
                    SelectionSnapshot(
                        typeFilter = typeFilter,
                        selectedCategoryId = selectedCategoryId,
                        selectedSubcategoryId = selectedSubcategoryId,
                        loadError = loadError,
                        status = status,
                        isSaving = false,
                    )
                },
                _isSaving,
            ) { snapshot, isSaving ->
                snapshot.copy(isSaving = isSaving)
            }

        val uiState: StateFlow<CategoryUiState> =
            combine(
                categoryTreeFlow,
                selectionAndMessages,
            ) { tree, snapshot ->
                val typeFilter = snapshot.typeFilter
                val selectedCategoryId = snapshot.selectedCategoryId
                val selectedSubcategoryId = snapshot.selectedSubcategoryId
                val loadError = snapshot.loadError
                val status = snapshot.status
                val isSaving = snapshot.isSaving
                val categories = tree.map { it.toTreeItemUi() }
                val subcategoryOptions =
                    tree
                        .firstOrNull { it.category.id.value == selectedCategoryId }
                        ?.subcategories
                        ?.map { it.toItemUi() }
                        ?: emptyList()

                val validSubcategoryId =
                    selectedSubcategoryId?.takeIf { subId ->
                        subcategoryOptions.any { it.id == subId }
                    }

                CategoryUiState(
                    isLoading = false,
                    typeFilter = typeFilter,
                    categories = categories,
                    selectedCategoryId = selectedCategoryId,
                    selectedSubcategoryId = validSubcategoryId,
                    subcategoryOptions = subcategoryOptions,
                    isEmpty = loadError == null && categories.isEmpty(),
                    isSaving = isSaving,
                    errorMessage = loadError,
                    statusMessage = status,
                )
            }.stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = CategoryUiState(isLoading = true),
            )

        fun onEvent(event: CategoryUiEvent) {
            when (event) {
                CategoryUiEvent.OnRetryLoad -> {
                    _loadErrorMessage.value = null
                    _retryKey.value += 1
                }

                CategoryUiEvent.OnDismissMessage -> {
                    _loadErrorMessage.value = null
                    _statusMessage.value = null
                }

                is CategoryUiEvent.OnTypeFilterSelected -> {
                    _typeFilter.value = event.type
                    _selectedCategoryId.value = null
                    _selectedSubcategoryId.value = null
                }

                is CategoryUiEvent.OnParentCategorySelected -> {
                    _selectedCategoryId.value = event.categoryId
                    _selectedSubcategoryId.value = null
                }

                is CategoryUiEvent.OnSubcategorySelected -> {
                    _selectedSubcategoryId.value = event.subcategoryId
                }

                is CategoryUiEvent.OnCreateCategory -> createCategory(event)
                is CategoryUiEvent.OnUpdateCategory -> updateCategory(event)
                is CategoryUiEvent.OnDeleteCategory ->
                    deleteCategory(event.categoryId, event.transactionMode, event.migrateToCategoryId)
                is CategoryUiEvent.OnCreateSubcategory -> createSubcategory(event)
                is CategoryUiEvent.OnUpdateSubcategory -> updateSubcategory(event)
                is CategoryUiEvent.OnDeleteSubcategory -> deleteSubcategory(event.subcategoryId)
            }
        }

        private fun createCategory(event: CategoryUiEvent.OnCreateCategory) {
            viewModelScope.launch {
                runCatching {
                    _isSaving.value = true
                    categoryRepository.createCategory(
                        CategoryEntity(
                            name = event.name.trim(),
                            iconName = event.iconName,
                            colorHex = event.colorHex,
                            type = event.type,
                            orderIndex = event.sortOrder,
                        ),
                    )
                }.onSuccess {
                    _statusMessage.value = "Category created."
                    _loadErrorMessage.value = null
                }.onFailure { e ->
                    _loadErrorMessage.value = MmrUiMessages.friendly(e.message, MmrUiMessages.GENERIC_SAVE)
                }
                _isSaving.value = false
            }
        }

        private fun updateCategory(event: CategoryUiEvent.OnUpdateCategory) {
            viewModelScope.launch {
                runCatching {
                    _isSaving.value = true
                    val existing =
                        categoryRepository.resolveCategory(event.id)
                            ?: throw IllegalArgumentException("Category not found.")
                    categoryRepository.updateCategory(
                        existing.copy(
                            name = event.name.trim(),
                            iconName = event.iconName,
                            colorHex = event.colorHex,
                            type = event.type,
                            orderIndex = event.sortOrder,
                        ),
                    )
                }.onSuccess {
                    _statusMessage.value = "Category updated."
                    _loadErrorMessage.value = null
                }.onFailure { e ->
                    _loadErrorMessage.value = MmrUiMessages.friendly(e.message, MmrUiMessages.GENERIC_SAVE)
                }
                _isSaving.value = false
            }
        }

        suspend fun countTransactionsForCategory(categoryId: Long): Int =
            categoryRepository.countTransactionsForCategory(categoryId)

        private fun deleteCategory(
            categoryId: Long,
            transactionMode: com.mmr.core.domain.model.CategoryDeleteTransactionMode,
            migrateToCategoryId: Long?,
        ) {
            viewModelScope.launch {
                runCatching {
                    _isSaving.value = true
                    categoryRepository.deleteCategory(categoryId, transactionMode, migrateToCategoryId)
                }.onSuccess {
                    if (_selectedCategoryId.value == categoryId) {
                        _selectedCategoryId.value = null
                        _selectedSubcategoryId.value = null
                    }
                    _statusMessage.value = "Category removed."
                    _loadErrorMessage.value = null
                }.onFailure { e ->
                    _loadErrorMessage.value = MmrUiMessages.friendly(e.message, MmrUiMessages.GENERIC_ACTION)
                }
                _isSaving.value = false
            }
        }

        private fun createSubcategory(event: CategoryUiEvent.OnCreateSubcategory) {
            viewModelScope.launch {
                runCatching {
                    _isSaving.value = true
                    categoryRepository.createSubcategory(
                        SubcategoryEntity(
                            categoryId = event.categoryId,
                            name = event.name.trim(),
                            orderIndex = event.sortOrder,
                        ),
                    )
                }.onSuccess {
                    _statusMessage.value = "Subcategory created."
                    _loadErrorMessage.value = null
                }.onFailure { e ->
                    _loadErrorMessage.value = MmrUiMessages.friendly(e.message, MmrUiMessages.GENERIC_SAVE)
                }
                _isSaving.value = false
            }
        }

        private fun updateSubcategory(event: CategoryUiEvent.OnUpdateSubcategory) {
            viewModelScope.launch {
                runCatching {
                    _isSaving.value = true
                    val existing =
                        categoryRepository.resolveSubcategory(event.id)
                            ?: throw IllegalArgumentException("Subcategory not found.")
                    categoryRepository.updateSubcategory(
                        existing.copy(
                            categoryId = event.categoryId,
                            name = event.name.trim(),
                            orderIndex = event.sortOrder,
                        ),
                    )
                }.onSuccess {
                    _statusMessage.value = "Subcategory updated."
                    _loadErrorMessage.value = null
                }.onFailure { e ->
                    _loadErrorMessage.value = MmrUiMessages.friendly(e.message, MmrUiMessages.GENERIC_SAVE)
                }
                _isSaving.value = false
            }
        }

        private fun deleteSubcategory(subcategoryId: Long) {
            viewModelScope.launch {
                runCatching {
                    _isSaving.value = true
                    categoryRepository.deleteSubcategory(subcategoryId)
                }.onSuccess {
                    if (_selectedSubcategoryId.value == subcategoryId) {
                        _selectedSubcategoryId.value = null
                    }
                    _statusMessage.value = "Subcategory removed."
                    _loadErrorMessage.value = null
                }.onFailure { e ->
                    _loadErrorMessage.value = MmrUiMessages.friendly(e.message, MmrUiMessages.GENERIC_ACTION)
                }
                _isSaving.value = false
            }
        }

        private fun CategoryWithChildren.toTreeItemUi(): CategoryTreeItemUi =
            CategoryTreeItemUi(
                id = category.id.value,
                name = category.name,
                colorHex = category.color.hex,
                iconName = category.iconName,
                type = category.type,
                isDefault = category.isDefault,
                sortOrder = category.sortOrder,
                subcategories = subcategories.map { it.toItemUi() },
            )

        private fun com.mmr.core.model.category.Subcategory.toItemUi(): SubcategoryItemUi =
            SubcategoryItemUi(
                id = id.value,
                categoryId = categoryId.value,
                name = name,
                sortOrder = sortOrder,
            )

        private data class SelectionSnapshot(
            val typeFilter: CategoryType?,
            val selectedCategoryId: Long?,
            val selectedSubcategoryId: Long?,
            val loadError: String?,
            val status: String?,
            val isSaving: Boolean,
        )
    }
