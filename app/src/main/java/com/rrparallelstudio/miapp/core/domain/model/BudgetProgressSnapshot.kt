package com.rrparallelstudio.miapp.core.domain.model

import com.rrparallelstudio.miapp.core.model.enums.BudgetPeriodType

data class BudgetProgressSnapshot(
    val budgetId: Long,
    val categoryId: Long,
    val subcategoryId: Long?,
    val targetLabel: String,
    val periodLabel: String,
    val periodType: BudgetPeriodType,
    val limitAmount: Double,
    val spentAmount: Double,
    val alertOnExceeded: Boolean,
) {
    val progressFraction: Float
        get() = if (limitAmount <= 0.0) 0f else (spentAmount / limitAmount).toFloat()

    val percentUsed: Int
        get() = (progressFraction * 100f).toInt().coerceAtLeast(0)

    val isOverBudget: Boolean
        get() = limitAmount > 0.0 && spentAmount > limitAmount
}
