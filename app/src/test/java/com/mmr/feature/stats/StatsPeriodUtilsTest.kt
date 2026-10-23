package com.mmr.feature.stats

import com.mmr.feature.stats.model.StatsPeriodMode
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class StatsPeriodUtilsTest {
    @Test
    fun annually_respectsCustomMonthStartDay() {
        val anchor = LocalDate.of(2026, 6, 15)
        val bounds =
            StatsPeriodUtils.resolveBounds(
                mode = StatsPeriodMode.ANNUALLY,
                anchorDate = anchor,
                customStart = null,
                customEnd = null,
                monthStartDay = 15,
            )
        assertEquals(LocalDate.of(2026, 1, 15), bounds.startDate)
        assertEquals(LocalDate.of(2027, 1, 14), bounds.endDate)
    }

    @Test
    fun annually_calendarYearWhenMonthStartIsFirst() {
        val bounds =
            StatsPeriodUtils.resolveBounds(
                mode = StatsPeriodMode.ANNUALLY,
                anchorDate = LocalDate.of(2026, 8, 1),
                customStart = null,
                customEnd = null,
                monthStartDay = 1,
            )
        assertEquals(LocalDate.of(2026, 1, 1), bounds.startDate)
        assertEquals(LocalDate.of(2026, 12, 31), bounds.endDate)
        assertEquals("2026", bounds.navigationLabel)
    }
}
