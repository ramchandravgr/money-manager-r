package com.mmr.feature.home

import com.mmr.feature.home.model.HomeExcelExportPreset
import com.mmr.feature.home.model.HomeExcelExportRange
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.YearMonth

class HomeExcelExportPeriodsTest {
    @Test
    fun buildOptions_includesFiveMmPeriods() {
        val options = HomeExcelExportPeriods.buildOptions(YearMonth.of(2025, 10), monthStartDay = 1)
        assertEquals(5, options.size)
        assertEquals(HomeExcelExportPreset.CURRENT_MONTH, options[0].preset)
        assertEquals(HomeExcelExportPreset.LAST_MONTH, options[1].preset)
        assertEquals(HomeExcelExportPreset.CURRENT_YEAR, options[2].preset)
        assertEquals(HomeExcelExportPreset.LAST_YEAR, options[3].preset)
        assertEquals(HomeExcelExportPreset.ALL, options[4].preset)
        assertTrue(options[0].label.startsWith("Monthly ("))
        assertEquals(HomeExcelExportRange.AllTime, options[4].range)
    }

    @Test
    fun calendarYearDateRange_respectsCustomMonthStart() {
        val (start, end) = HomeExcelExportPeriods.calendarYearDateRange(2025, 26)
        assertEquals(26, start.dayOfMonth)
        assertEquals(2025, start.year)
        assertEquals(25, end.dayOfMonth)
        assertEquals(2026, end.year)
    }
}
