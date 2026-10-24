package com.mmr.core.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.mmr.core.model.enums.CategorizationMatchType

@Entity(
    tableName = "categorization_rules",
    indices = [
        Index(value = ["priority"]),
        Index(value = ["isEnabled"]),
        Index(value = ["syncId"], unique = true),
    ],
)
data class CategorizationRuleEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val syncId: String =
        com.mmr.core.common.sync.MmrSyncId
            .newId(),
    val priority: Int = 50,
    val matchType: CategorizationMatchType,
    val pattern: String,
    val categoryName: String,
    val subcategoryName: String? = null,
    val isEnabled: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
)
