package com.mmr.core.model.category

import com.mmr.core.model.enums.CategoryType

@JvmInline
value class CategoryId(val value: Long) {
    companion object {
        val Unset = CategoryId(0L)
    }
}

@JvmInline
value class SubcategoryId(val value: Long) {
    companion object {
        val Unset = SubcategoryId(0L)
    }
}

@JvmInline
value class CategoryColor(val hex: String)

data class Category(
    val id: CategoryId,
    val name: String,
    val iconName: String,
    val color: CategoryColor,
    val type: CategoryType,
    val isDefault: Boolean,
    val isActive: Boolean,
    val sortOrder: Int
)

data class Subcategory(
    val id: SubcategoryId,
    val categoryId: CategoryId,
    val name: String,
    val isActive: Boolean,
    val sortOrder: Int
)

/** Category with zero or more subcategories (optional children). */
data class CategoryWithChildren(
    val category: Category,
    val subcategories: List<Subcategory>
)
