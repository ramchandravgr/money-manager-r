package com.mmr.feature.search

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class SearchPeriodPresetsTest {
    @Test
    fun currentFinancialYear_midYear_startsApril() {
        val today = LocalDate.of(2026, 10, 22)
        val range = SearchPeriodPresets.currentFinancialYear(today)
        assertEquals(LocalDate.of(2026, 4, 1), range.start)
        assertEquals(LocalDate.of(2027, 3, 31), range.end)
    }

    @Test
    fun currentFinancialYear_beforeApril_usesPreviousStartYear() {
        val today = LocalDate.of(2026, 2, 10)
        val range = SearchPeriodPresets.currentFinancialYear(today)
        assertEquals(LocalDate.of(2025, 4, 1), range.start)
        assertEquals(LocalDate.of(2026, 3, 31), range.end)
    }

    @Test
    fun lastFinancialYear_isOneYearBeforeCurrent() {
        val today = LocalDate.of(2026, 10, 22)
        val range = SearchPeriodPresets.rangeFor(SearchPeriodPresets.QuickPick.LAST_FY, today)
        assertEquals(LocalDate.of(2025, 4, 1), range.start)
        assertEquals(LocalDate.of(2026, 3, 31), range.end)
    }

    @Test
    fun lastWeek_isPreviousMondayThroughSunday() {
        val today = LocalDate.of(2026, 10, 22) // Wednesday
        val range = SearchPeriodPresets.rangeFor(SearchPeriodPresets.QuickPick.LAST_WEEK, today)
        assertEquals(LocalDate.of(2026, 10, 12), range.start) // Monday
        assertEquals(LocalDate.of(2026, 10, 18), range.end) // Sunday
    }

    @Test
    fun last30Days_includesToday() {
        val today = LocalDate.of(2026, 10, 22)
        val range = SearchPeriodPresets.rangeFor(SearchPeriodPresets.QuickPick.LAST_30_DAYS, today)
        assertEquals(LocalDate.of(2026, 9, 23), range.start)
        assertEquals(today, range.end)
    }
}
