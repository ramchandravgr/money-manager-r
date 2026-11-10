package com.rrparallelstudio.miapp.core.database.relation

import androidx.room.Embedded
import androidx.room.Relation
import com.rrparallelstudio.miapp.core.database.entity.CategoryEntity
import com.rrparallelstudio.miapp.core.database.entity.SubcategoryEntity

/**
 * Parent category with optional active child rows loaded via [categoryId] FK.
 */
data class CategoryWithSubcategories(
    @Embedded
    val category: CategoryEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "categoryId",
    )
    val subcategories: List<SubcategoryEntity>,
)
