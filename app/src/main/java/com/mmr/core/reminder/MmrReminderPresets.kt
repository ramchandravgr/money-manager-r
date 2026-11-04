package com.mmr.core.reminder

object MmrReminderPresets {
    const val MINUTES_PER_DAY = 24 * 60
    const val DAY_MILLIS = MINUTES_PER_DAY * 60L * 1000L

    /** Recurring rules store lead time as minutes before [nextRunDate]. */
    val recurringReminderOptionsMinutes: List<Pair<Int, String>> =
        listOf(
            MINUTES_PER_DAY to "1 day before",
            2 * MINUTES_PER_DAY to "2 days before",
            3 * MINUTES_PER_DAY to "3 days before",
            7 * MINUTES_PER_DAY to "7 days before",
        )

    val loanEmiReminderDaysOptions: List<Pair<Int, String>> =
        listOf(
            1 to "1 day before",
            2 to "2 days before",
            3 to "3 days before",
            7 to "7 days before",
        )

    fun recurringReminderLabel(minutes: Int): String =
        recurringReminderOptionsMinutes.firstOrNull { it.first == minutes }?.second
            ?: "${minutes / MINUTES_PER_DAY} days before"

    fun loanEmiReminderLabel(days: Int): String =
        loanEmiReminderDaysOptions.firstOrNull { it.first == days }?.second
            ?: "$days days before"

    fun isValidRecurringReminderMinutes(minutes: Int): Boolean =
        recurringReminderOptionsMinutes.any { it.first == minutes }

    fun isValidLoanEmiReminderDays(days: Int): Boolean = loanEmiReminderDaysOptions.any { it.first == days }
}
