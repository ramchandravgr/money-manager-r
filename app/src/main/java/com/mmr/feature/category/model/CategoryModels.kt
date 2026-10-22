package com.mmr.feature.category.model

import com.mmr.core.model.enums.CategoryType

data class CategoryTreeItemUi(
    val id: Long,
    val name: String,
    val colorHex: String,
    val iconName: String,
    val type: CategoryType,
    val isDefault: Boolean,
    val sortOrder: Int,
    val subcategories: List<SubcategoryItemUi>
)

data class SubcategoryItemUi(
    val id: Long,
    val categoryId: Long,
    val name: String,
    val sortOrder: Int
)

data class CategoryUiState(
    val isLoading: Boolean = true,
    val typeFilter: CategoryType? = null,
    val categories: List<CategoryTreeItemUi> = emptyList(),
    val selectedCategoryId: Long? = null,
    val selectedSubcategoryId: Long? = null,
    val subcategoryOptions: List<SubcategoryItemUi> = emptyList(),
    val isEmpty: Boolean = false,
    val isSaving: Boolean = false,
    val errorMessage: String? = null,
    val statusMessage: String? = null
)

sealed interface CategoryUiEvent {
    data object OnRetryLoad : CategoryUiEvent
    data object OnDismissMessage : CategoryUiEvent
    data class OnTypeFilterSelected(val type: CategoryType?) : CategoryUiEvent
    data class OnParentCategorySelected(val categoryId: Long?) : CategoryUiEvent
    data class OnSubcategorySelected(val subcategoryId: Long?) : CategoryUiEvent
    data class OnCreateCategory(
        val name: String,
        val colorHex: String,
        val type: CategoryType,
        val iconName: String = "category_default",
        val sortOrder: Int = 0
    ) : CategoryUiEvent

    data class OnUpdateCategory(
        val id: Long,
        val name: String,
        val colorHex: String,
        val type: CategoryType,
        val iconName: String,
        val sortOrder: Int
    ) : CategoryUiEvent

    data class OnDeleteCategory(val categoryId: Long) : CategoryUiEvent

    data class OnCreateSubcategory(
        val categoryId: Long,
        val name: String,
        val sortOrder: Int = 0
    ) : CategoryUiEvent

    data class OnUpdateSubcategory(
        val id: Long,
        val categoryId: Long,
        val name: String,
        val sortOrder: Int
    ) : CategoryUiEvent

    data class OnDeleteSubcategory(val subcategoryId: Long) : CategoryUiEvent
}
