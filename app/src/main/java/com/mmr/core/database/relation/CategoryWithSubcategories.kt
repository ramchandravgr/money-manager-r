package com.mmr.core.database.relation

import androidx.room.Embedded
import androidx.room.Relation
import com.mmr.core.database.entity.CategoryEntity
import com.mmr.core.database.entity.SubcategoryEntity

/**
 * Parent category with optional active child rows loaded via [categoryId] FK.
 */
data class CategoryWithSubcategories(
    @Embedded
    val category: CategoryEntity,

    @Relation(
        parentColumn = "id",
        entityColumn = "categoryId"
    )
    val subcategories: List<SubcategoryEntity>
)
