package com.mmr.feature.scheduled.model

enum class ScheduledSection(
    val title: String,
) {
    LOANS("Loans"),
    RECURRING("Recurring"),
    ;

    companion object {
        fun fromRouteArg(value: String?): ScheduledSection =
            when (value?.lowercase()) {
                "recurring" -> RECURRING
                else -> LOANS
            }
    }
}
