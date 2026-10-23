package com.mmr.feature.category.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mmr.app.ui.state.MmrUiMessages
import com.mmr.core.database.entity.SubcategoryEntity
import com.mmr.core.domain.repository.CategoryRepository
import com.mmr.feature.category.model.AddEditSubcategoryUiEvent
import com.mmr.feature.category.model.AddEditSubcategoryUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AddEditSubcategoryViewModel
    @Inject
    constructor(
        private val categoryRepository: CategoryRepository,
        savedStateHandle: SavedStateHandle,
    ) : ViewModel() {
        private val categoryIdArg: Long? = savedStateHandle.get<Long>("categoryId")?.takeIf { it > 0L }
        private val subcategoryIdArg: Long? = savedStateHandle.get<Long>("subcategoryId")?.takeIf { it > 0L }

        private val _uiState = MutableStateFlow(AddEditSubcategoryUiState(isEditMode = subcategoryIdArg != null))
        val uiState: StateFlow<AddEditSubcategoryUiState> = _uiState.asStateFlow()

        init {
            when {
                subcategoryIdArg != null -> loadSubcategory(subcategoryIdArg)
                categoryIdArg != null -> loadParentForCreate(categoryIdArg)
                else -> {
                    _uiState.update {
                        it.copy(isLoading = false, loadErrorMessage = "Parent category is required.")
                    }
                }
            }
        }

        fun onEvent(event: AddEditSubcategoryUiEvent) {
            when (event) {
                is AddEditSubcategoryUiEvent.OnNameChanged -> {
                    _uiState.update {
                        val next = it.copy(name = event.name, errorMessage = null)
                        next.copy(canSave = validate(next))
                    }
                }

                is AddEditSubcategoryUiEvent.OnSortOrderChanged -> {
                    val digits = event.sortOrder.filter { it.isDigit() }
                    _uiState.update {
                        val next = it.copy(sortOrder = digits, errorMessage = null)
                        next.copy(canSave = validate(next))
                    }
                }

                AddEditSubcategoryUiEvent.OnSaveClick -> saveSubcategory()
                AddEditSubcategoryUiEvent.OnDismissError -> {
                    _uiState.update { it.copy(errorMessage = null, loadErrorMessage = null) }
                }
            }
        }

        private fun loadParentForCreate(parentId: Long) {
            viewModelScope.launch {
                _uiState.update { it.copy(isLoading = true, loadErrorMessage = null) }
                try {
                    val parent =
                        categoryRepository.resolveCategory(parentId)
                            ?: throw IllegalArgumentException("Parent category not found.")
                    _uiState.update {
                        val loaded =
                            it.copy(
                                isLoading = false,
                                parentCategoryId = parent.id,
                                parentCategoryName = parent.name,
                                parentColorHex = parent.colorHex,
                            )
                        loaded.copy(canSave = validate(loaded))
                    }
                } catch (e: Exception) {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            loadErrorMessage = MmrUiMessages.friendly(e.message, MmrUiMessages.GENERIC_LOAD),
                        )
                    }
                }
            }
        }

        private fun loadSubcategory(subcategoryId: Long) {
            viewModelScope.launch {
                _uiState.update { it.copy(isLoading = true, loadErrorMessage = null) }
                try {
                    val entity =
                        categoryRepository.resolveSubcategory(subcategoryId)
                            ?: throw IllegalArgumentException("Subcategory not found.")
                    val parent =
                        categoryRepository.resolveCategory(entity.categoryId)
                            ?: throw IllegalArgumentException("Parent category not found.")
                    _uiState.update {
                        val loaded =
                            it.copy(
                                isLoading = false,
                                isEditMode = true,
                                subcategoryId = entity.id,
                                parentCategoryId = parent.id,
                                parentCategoryName = parent.name,
                                parentColorHex = parent.colorHex,
                                name = entity.name,
                                sortOrder = entity.orderIndex.toString(),
                            )
                        loaded.copy(canSave = validate(loaded))
                    }
                } catch (e: Exception) {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            loadErrorMessage = MmrUiMessages.friendly(e.message, MmrUiMessages.GENERIC_LOAD),
                        )
                    }
                }
            }
        }

        private fun saveSubcategory() {
            val state = _uiState.value
            if (!validate(state)) {
                _uiState.update { it.copy(errorMessage = "Enter a valid subcategory name.") }
                return
            }
            val sortOrder = state.sortOrder.toIntOrNull() ?: 0

            viewModelScope.launch {
                _uiState.update { it.copy(isSaving = true, errorMessage = null) }
                try {
                    if (state.isEditMode) {
                        val existing =
                            categoryRepository.resolveSubcategory(state.subcategoryId)
                                ?: throw IllegalArgumentException("Subcategory not found.")
                        categoryRepository.updateSubcategory(
                            existing.copy(
                                categoryId = state.parentCategoryId,
                                name = state.name.trim(),
                                orderIndex = sortOrder,
                            ),
                        )
                    } else {
                        categoryRepository.createSubcategory(
                            SubcategoryEntity(
                                categoryId = state.parentCategoryId,
                                name = state.name.trim(),
                                orderIndex = sortOrder,
                            ),
                        )
                    }
                    _uiState.update { it.copy(isSaving = false, isSaved = true) }
                } catch (e: Exception) {
                    _uiState.update {
                        it.copy(
                            isSaving = false,
                            errorMessage = MmrUiMessages.friendly(e.message, MmrUiMessages.GENERIC_SAVE),
                        )
                    }
                }
            }
        }

        private fun validate(state: AddEditSubcategoryUiState): Boolean {
            if (state.parentCategoryId <= 0L) return false
            if (state.name.isBlank()) return false
            val order = state.sortOrder.toIntOrNull() ?: return false
            return order >= 0
        }
    }
