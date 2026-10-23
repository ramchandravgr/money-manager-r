package com.mmr.feature.search

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters

object SearchPeriodPresets {
    data class Range(
        val start: LocalDate,
        val end: LocalDate,
    )

    enum class QuickPick(
        val label: String,
    ) {
        LAST_WEEK("Last week"),
        LAST_MONTH("Last month"),
        LAST_30_DAYS("Last 30 days"),
        LAST_YEAR("Last year"),
        CURRENT_FY("Current FY"),
        LAST_FY("Last FY"),
    }

    fun rangeFor(
        pick: QuickPick,
        today: LocalDate = LocalDate.now(),
    ): Range =
        when (pick) {
            QuickPick.LAST_WEEK -> lastCalendarWeek(today)
            QuickPick.LAST_MONTH -> lastCalendarMonth(today)
            QuickPick.LAST_30_DAYS -> Range(today.minusDays(29), today)
            QuickPick.LAST_YEAR ->
                Range(
                    LocalDate.of(today.year - 1, 1, 1),
                    LocalDate.of(today.year - 1, 12, 31),
                )
            QuickPick.CURRENT_FY -> currentFinancialYear(today)
            QuickPick.LAST_FY -> {
                val current = currentFinancialYear(today)
                Range(current.start.minusYears(1), current.end.minusYears(1))
            }
        }

    /** Financial year: 1 April – 31 March (inclusive). */
    fun currentFinancialYear(today: LocalDate): Range {
        val fyStartYear = if (today.monthValue >= 4) today.year else today.year - 1
        return Range(
            LocalDate.of(fyStartYear, 4, 1),
            LocalDate.of(fyStartYear + 1, 3, 31),
        )
    }

    /** Previous Monday–Sunday block before the current week (ISO week). */
    private fun lastCalendarWeek(today: LocalDate): Range {
        val startOfThisWeek = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        val endLastWeek = startOfThisWeek.minusDays(1)
        val startLastWeek = endLastWeek.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        return Range(startLastWeek, endLastWeek)
    }

    private fun lastCalendarMonth(today: LocalDate): Range {
        val lastDayPrevMonth = today.withDayOfMonth(1).minusDays(1)
        return Range(lastDayPrevMonth.withDayOfMonth(1), lastDayPrevMonth)
    }

    fun normalizeRange(
        start: LocalDate,
        end: LocalDate,
    ): Range = if (start.isAfter(end)) Range(end, start) else Range(start, end)

    fun clampDay(
        year: Int,
        month: Int,
        day: Int,
    ): LocalDate {
        val maxDay = LocalDate.of(year, month, 1).lengthOfMonth()
        return LocalDate.of(year, month, day.coerceIn(1, maxDay))
    }
}
