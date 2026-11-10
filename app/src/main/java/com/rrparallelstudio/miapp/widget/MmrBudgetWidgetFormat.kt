package com.rrparallelstudio.miapp.widget

import com.rrparallelstudio.miapp.core.domain.model.BudgetProgressSnapshot
import com.rrparallelstudio.miapp.core.model.enums.BudgetPeriodType

internal fun BudgetPeriodType.widgetPeriodKindLabel(): String =
    when (this) {
        BudgetPeriodType.WEEKLY -> "Week"
        BudgetPeriodType.MONTHLY -> "Month"
        BudgetPeriodType.ANNUAL -> "Year"
        BudgetPeriodType.CUSTOM -> "Period"
    }

internal fun BudgetProgressSnapshot.widgetDisplayName(): String =
    if (subcategoryId != null) {
        targetLabel.substringAfter(" · ", targetLabel)
    } else {
        targetLabel
    }
