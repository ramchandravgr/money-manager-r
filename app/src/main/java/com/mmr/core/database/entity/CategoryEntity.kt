package com.mmr.core.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.mmr.core.model.enums.CategoryType

@Entity(
    tableName = "categories",
    indices = [
        Index(value = ["name", "type"], unique = true),
        Index(value = ["isActive"]),
        Index(value = ["orderIndex"])
    ]
)
data class CategoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val iconName: String = "category_default",
    val colorHex: String = "#3B82F6",
    val type: CategoryType = CategoryType.EXPENSE,
    val isDefault: Boolean = false,
    val isActive: Boolean = true,
    val orderIndex: Int = 0
)
