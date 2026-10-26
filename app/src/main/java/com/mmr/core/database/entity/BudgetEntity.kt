package com.mmr.core.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.mmr.core.model.enums.BudgetPeriodType

@Entity(
    tableName = "budgets",
    foreignKeys = [
        ForeignKey(
            entity = CategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = SubcategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["subcategoryId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(value = ["categoryId"]),
        Index(value = ["subcategoryId"]),
        Index(value = ["syncId"], unique = true),
    ],
)
data class BudgetEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val syncId: String,
    val categoryId: Long,
    val subcategoryId: Long? = null,
    val amountLimit: Double,
    val periodType: BudgetPeriodType,
    val customPeriodStartMillis: Long? = null,
    val customPeriodEndMillis: Long? = null,
    val alertOnExceeded: Boolean = false,
    /** Dedupes alert per resolved period (see [com.mmr.core.budget.BudgetPeriodResolver.periodKey]). */
    val lastAlertPeriodKey: String? = null,
    val isActive: Boolean = true,
    val createdAt: Long,
    val updatedAt: Long,
)
