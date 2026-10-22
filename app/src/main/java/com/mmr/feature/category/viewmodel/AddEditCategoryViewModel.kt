package com.mmr.feature.category.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mmr.app.ui.state.MmrUiMessages
import com.mmr.core.database.entity.CategoryEntity
import com.mmr.core.domain.repository.CategoryRepository
import com.mmr.core.model.enums.CategoryType
import com.mmr.feature.category.model.AddEditCategoryUiEvent
import com.mmr.feature.category.model.AddEditCategoryUiState
import com.mmr.feature.category.model.SubcategoryItemUi
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AddEditCategoryViewModel @Inject constructor(
    private val categoryRepository: CategoryRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val categoryId: Long? = savedStateHandle.get<Long>("categoryId")?.takeIf { it > 0L }

    private val _uiState = MutableStateFlow(AddEditCategoryUiState(isEditMode = categoryId != null))
    val uiState: StateFlow<AddEditCategoryUiState> = _uiState.asStateFlow()

    init {
        if (categoryId != null) {
            loadCategory(categoryId)
            observeSubcategories(categoryId)
        } else {
            _uiState.update {
                it.copy(isLoading = false, canSave = validate(it))
            }
        }
    }

    fun onEvent(event: AddEditCategoryUiEvent) {
        when (event) {
            is AddEditCategoryUiEvent.OnNameChanged -> {
                _uiState.update {
                    val next = it.copy(name = event.name, errorMessage = null)
                    next.copy(canSave = validate(next))
                }
            }

            is AddEditCategoryUiEvent.OnColorSelected -> {
                _uiState.update {
                    val next = it.copy(colorHex = event.colorHex, errorMessage = null)
                    next.copy(canSave = validate(next))
                }
            }

            is AddEditCategoryUiEvent.OnTypeSelected -> {
                if (_uiState.value.isDefaultCategory) return
                _uiState.update {
                    val next = it.copy(type = event.type, errorMessage = null)
                    next.copy(canSave = validate(next))
                }
            }

            is AddEditCategoryUiEvent.OnSortOrderChanged -> {
                val digits = event.sortOrder.filter { it.isDigit() }
                _uiState.update {
                    val next = it.copy(sortOrder = digits, errorMessage = null)
                    next.copy(canSave = validate(next))
                }
            }

            AddEditCategoryUiEvent.OnSaveClick -> saveCategory()
            AddEditCategoryUiEvent.OnDismissError -> {
                _uiState.update { it.copy(errorMessage = null, loadErrorMessage = null) }
            }
        }
    }

    fun deleteSubcategory(subcategoryId: Long) {
        viewModelScope.launch {
            runCatching {
                categoryRepository.deleteSubcategory(subcategoryId)
            }.onFailure { e ->
                _uiState.update {
                    it.copy(errorMessage = MmrUiMessages.friendly(e.message, MmrUiMessages.GENERIC_ACTION))
                }
            }
        }
    }

    private fun observeSubcategories(parentId: Long) {
        viewModelScope.launch {
            categoryRepository.observeCategoryTree(parentId).collect { tree ->
                val subs = tree?.subcategories?.map { sub ->
                    SubcategoryItemUi(
                        id = sub.id.value,
                        categoryId = sub.categoryId.value,
                        name = sub.name,
                        sortOrder = sub.sortOrder
                    )
                } ?: emptyList()
                _uiState.update { it.copy(subcategories = subs) }
            }
        }
    }

    private fun loadCategory(id: Long) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, loadErrorMessage = null) }
            try {
                val entity = categoryRepository.resolveCategory(id)
                if (entity == null) {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            loadErrorMessage = "Category not found."
                        )
                    }
                    return@launch
                }
                _uiState.update {
                    val loaded = it.copy(
                        isLoading = false,
                        isEditMode = true,
                        categoryId = entity.id,
                        name = entity.name,
                        colorHex = entity.colorHex,
                        type = entity.type,
                        iconName = entity.iconName,
                        sortOrder = entity.orderIndex.toString(),
                        isDefaultCategory = entity.isDefault
                    )
                    loaded.copy(canSave = validate(loaded))
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        loadErrorMessage = MmrUiMessages.friendly(e.message, MmrUiMessages.GENERIC_LOAD)
                    )
                }
            }
        }
    }

    private fun saveCategory() {
        val state = _uiState.value
        if (!validate(state)) {
            _uiState.update { it.copy(errorMessage = "Enter a valid category name and color.") }
            return
        }
        val sortOrder = state.sortOrder.toIntOrNull() ?: 0

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, errorMessage = null) }
            try {
                if (state.isEditMode) {
                    val existing = categoryRepository.resolveCategory(state.categoryId)
                        ?: throw IllegalArgumentException("Category not found.")
                    categoryRepository.updateCategory(
                        existing.copy(
                            name = state.name.trim(),
                            colorHex = state.colorHex,
                            type = state.type,
                            iconName = state.iconName,
                            orderIndex = sortOrder
                        )
                    )
                } else {
                    categoryRepository.createCategory(
                        CategoryEntity(
                            name = state.name.trim(),
                            colorHex = state.colorHex,
                            type = state.type,
                            iconName = state.iconName,
                            orderIndex = sortOrder
                        )
                    )
                }
                _uiState.update { it.copy(isSaving = false, isSaved = true) }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isSaving = false,
                        errorMessage = MmrUiMessages.friendly(e.message, MmrUiMessages.GENERIC_SAVE)
                    )
                }
            }
        }
    }

    private fun validate(state: AddEditCategoryUiState): Boolean {
        if (state.name.isBlank()) return false
        if (!state.colorHex.matches(Regex("^#[0-9A-Fa-f]{6}$"))) return false
        if (state.type == CategoryType.ALL || state.type == CategoryType.TRANSFER) return false
        val order = state.sortOrder.toIntOrNull() ?: return false
        return order >= 0
    }
}
