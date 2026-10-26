package com.mmr.app.widget

import com.mmr.core.domain.model.BudgetProgressSnapshot
import com.mmr.core.model.enums.BudgetPeriodType

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
