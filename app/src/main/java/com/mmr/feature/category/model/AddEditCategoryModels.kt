package com.mmr.feature.category.model

import com.mmr.core.model.enums.CategoryType

data class AddEditCategoryUiState(
    val isLoading: Boolean = true,
    val isEditMode: Boolean = false,
    val categoryId: Long = 0L,
    val name: String = "",
    val colorHex: String = CategoryColorPresets.defaultHex,
    val type: CategoryType = CategoryType.EXPENSE,
    val iconName: String = "category_default",
    val sortOrder: String = "0",
    val isDefaultCategory: Boolean = false,
    val isSaving: Boolean = false,
    val isSaved: Boolean = false,
    val loadErrorMessage: String? = null,
    val errorMessage: String? = null,
    val canSave: Boolean = false,
    val subcategories: List<SubcategoryItemUi> = emptyList(),
)

sealed interface AddEditCategoryUiEvent {
    data class OnNameChanged(
        val name: String,
    ) : AddEditCategoryUiEvent

    data class OnColorSelected(
        val colorHex: String,
    ) : AddEditCategoryUiEvent

    data class OnTypeSelected(
        val type: CategoryType,
    ) : AddEditCategoryUiEvent

    data class OnSortOrderChanged(
        val sortOrder: String,
    ) : AddEditCategoryUiEvent

    data object OnSaveClick : AddEditCategoryUiEvent

    data object OnDismissError : AddEditCategoryUiEvent
}

object CategoryColorPresets {
    const val defaultHex = "#3B82F6"

    val presets: List<String> =
        listOf(
            "#EF4444",
            "#F59E0B",
            "#10B981",
            "#3B82F6",
            "#8B5CF6",
            "#EC4899",
            "#06B6D4",
            "#64748B",
        )
}
