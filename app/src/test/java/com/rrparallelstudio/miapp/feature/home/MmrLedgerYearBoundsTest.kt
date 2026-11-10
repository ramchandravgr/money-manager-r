package com.rrparallelstudio.miapp.feature.home

import org.junit.Assert.assertEquals
import org.junit.Test

class MmrLedgerYearBoundsTest {
    @Test
    fun selectableYears_fiveYearsAheadFor2026Anchor() {
        val range = MmrLedgerYearBounds.selectableYears(anchorYear = 2026)
        assertEquals(1976, range.first)
        assertEquals(2031, range.last)
    }
}
