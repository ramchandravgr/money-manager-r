package com.mmr.feature.home

import org.junit.Assert.assertEquals
import org.junit.Test

class HomeTotalUtilsTest {
    @Test
    fun comparedExpensePeriodPercent_usesPeriodOverPeriodIndex() {
        assertEquals(100L, HomeTotalUtils.comparedExpensePeriodPercent(100.0, 100.0))
        assertEquals(200L, HomeTotalUtils.comparedExpensePeriodPercent(200.0, 100.0))
        assertEquals(50L, HomeTotalUtils.comparedExpensePeriodPercent(50.0, 100.0))
        assertEquals(100L, HomeTotalUtils.comparedExpensePeriodPercent(400.0, 0.0))
    }
}
