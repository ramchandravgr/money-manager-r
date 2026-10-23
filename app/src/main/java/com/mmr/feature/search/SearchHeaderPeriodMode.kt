package com.mmr.feature.search

import com.mmr.core.model.settings.MmrWeekStartDay
import com.mmr.feature.home.HomePeriodUtils
import java.time.LocalDate
import java.time.YearMonth

enum class SearchHeaderPeriodMode(
    val label: String,
) {
    WEEK("Week"),
    MONTH("Month"),
    YEAR("Year"),
    CUSTOM("Custom"),
}

object SearchHeaderPeriodRanges {
    fun rangeFor(
        mode: SearchHeaderPeriodMode,
        anchor: LocalDate = LocalDate.now(),
        monthStartDay: Int = 1,
        weekStartDay: MmrWeekStartDay = MmrWeekStartDay.MONDAY,
        customStart: LocalDate? = null,
        customEnd: LocalDate? = null,
    ): Pair<LocalDate, LocalDate> {
        val weekStart = HomePeriodUtils.toDayOfWeek(weekStartDay)
        return when (mode) {
            SearchHeaderPeriodMode.WEEK -> {
                val (start, end) = HomePeriodUtils.weekRangeContaining(anchor, weekStart)
                start to end
            }
            SearchHeaderPeriodMode.MONTH -> {
                HomePeriodUtils.monthDateRange(YearMonth.from(anchor), monthStartDay)
            }
            SearchHeaderPeriodMode.YEAR -> {
                LocalDate.of(anchor.year, 1, 1) to LocalDate.of(anchor.year, 12, 31)
            }
            SearchHeaderPeriodMode.CUSTOM -> {
                val start = customStart ?: anchor.minusDays(6)
                val end = customEnd ?: anchor
                if (start.isAfter(end)) end to start else start to end
            }
        }
    }
}
