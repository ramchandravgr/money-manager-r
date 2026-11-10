package com.rrparallelstudio.miapp.core.model.enums

enum class BudgetPeriodType {
    WEEKLY,
    MONTHLY,
    ANNUAL,
    CUSTOM,
    ;

    val label: String
        get() =
            when (this) {
                WEEKLY -> "Weekly"
                MONTHLY -> "Monthly"
                ANNUAL -> "Annual"
                CUSTOM -> "Custom period"
            }
}
