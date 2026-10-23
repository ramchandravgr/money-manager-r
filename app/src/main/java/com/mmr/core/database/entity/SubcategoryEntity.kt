package com.mmr.core.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "subcategories",
    foreignKeys = [
        ForeignKey(
            entity = CategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(value = ["categoryId"]),
        Index(value = ["name", "categoryId"], unique = true),
        Index(value = ["isActive"]),
        Index(value = ["orderIndex"]),
        Index(value = ["syncId"], unique = true),
    ],
)
data class SubcategoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val syncId: String =
        com.mmr.core.common.sync.MmrSyncId
            .newId(),
    val categoryId: Long,
    val name: String,
    val isActive: Boolean = true,
    val orderIndex: Int = 0,
)
