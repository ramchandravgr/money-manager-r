package com.mmr.feature.category.model

data class AddEditSubcategoryUiState(
    val isLoading: Boolean = true,
    val isEditMode: Boolean = false,
    val subcategoryId: Long = 0L,
    val parentCategoryId: Long = 0L,
    val parentCategoryName: String = "",
    val parentColorHex: String = "#3B82F6",
    val name: String = "",
    val sortOrder: String = "0",
    val isSaving: Boolean = false,
    val isSaved: Boolean = false,
    val loadErrorMessage: String? = null,
    val errorMessage: String? = null,
    val canSave: Boolean = false,
)

sealed interface AddEditSubcategoryUiEvent {
    data class OnNameChanged(
        val name: String,
    ) : AddEditSubcategoryUiEvent

    data class OnSortOrderChanged(
        val sortOrder: String,
    ) : AddEditSubcategoryUiEvent

    data object OnSaveClick : AddEditSubcategoryUiEvent

    data object OnDismissError : AddEditSubcategoryUiEvent

    data object OnDeleteClick : AddEditSubcategoryUiEvent
}
