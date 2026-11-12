package com.rrparallelstudio.miapp.feature.home

import com.rrparallelstudio.miapp.feature.home.model.HomeExcelExportPreset
import com.rrparallelstudio.miapp.feature.home.model.HomeExcelExportRange
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth

class HomeExcelExportPeriodsTest {
    @Test
    fun buildOptions_includesWeekMonthYearTotalAndCustom() {
        val anchorDate = LocalDate.of(2025, 10, 15)
        val options =
            HomeExcelExportPeriods.buildOptions(
                YearMonth.of(2025, 10),
                monthStartDay = 1,
                weekStart = DayOfWeek.MONDAY,
                anchorDate = anchorDate,
            )
        assertEquals(8, options.size)
        assertEquals(HomeExcelExportPreset.CURRENT_WEEK, options[0].preset)
        assertEquals(HomeExcelExportPreset.LAST_WEEK, options[1].preset)
        assertEquals(HomeExcelExportPreset.CURRENT_MONTH, options[2].preset)
        assertEquals(HomeExcelExportPreset.CUSTOM, options.last().preset)
        assertEquals(HomeExcelExportRange.AllTime, options[6].range)
        assertTrue(options[2].label.startsWith("Monthly ("))
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
