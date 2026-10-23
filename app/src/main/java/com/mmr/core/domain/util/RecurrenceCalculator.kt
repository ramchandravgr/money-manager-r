package com.mmr.core.domain.util

import com.mmr.core.model.enums.RecurrenceInterval
import java.util.Calendar

object RecurrenceCalculator {
    /**
     * Calculates the next execution timestamp based on the current run date and interval.
     * Preserves the specific target day of month where applicable.
     */
    fun calculateNextRunDate(
        fromTimestamp: Long,
        interval: RecurrenceInterval,
        targetDayOfMonth: Int? = null,
    ): Long {
        val calendar =
            Calendar.getInstance().apply {
                timeInMillis = fromTimestamp
            }

        when (interval) {
            RecurrenceInterval.DAILY -> {
                calendar.add(Calendar.DAY_OF_YEAR, 1)
            }

            RecurrenceInterval.WEEKLY -> {
                calendar.add(Calendar.WEEK_OF_YEAR, 1)
            }

            RecurrenceInterval.MONTHLY -> {
                calendar.add(Calendar.MONTH, 1)
                if (targetDayOfMonth != null && targetDayOfMonth in 1..31) {
                    val maxDay = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
                    calendar.set(Calendar.DAY_OF_MONTH, targetDayOfMonth.coerceAtMost(maxDay))
                }
            }

            RecurrenceInterval.YEARLY -> {
                calendar.add(Calendar.YEAR, 1)
                if (targetDayOfMonth != null && targetDayOfMonth in 1..31) {
                    val maxDay = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
                    calendar.set(Calendar.DAY_OF_MONTH, targetDayOfMonth.coerceAtMost(maxDay))
                }
            }

            RecurrenceInterval.CUSTOM -> {
                calendar.add(Calendar.DAY_OF_YEAR, 30)
            }
        }

        return calendar.timeInMillis
    }

    /**
     * Calculates the reminder notification trigger timestamp, subtracting reminderMinutes.
     */
    fun calculateReminderScheduleTime(
        nextRunDate: Long,
        reminderMinutesBefore: Int,
    ): Long = (nextRunDate - (reminderMinutesBefore * 60 * 1000L)).coerceAtLeast(0L)

    /**
     * Checks if a recurring rule has expired relative to its end date.
     */
    fun isExpired(
        nextRunDate: Long,
        endDate: Long?,
    ): Boolean = endDate != null && nextRunDate > endDate
}
