package com.mmr.feature.home

import org.junit.Assert.assertEquals
import org.junit.Test

class HomeTotalUtilsTest {
    @Test
    fun mmComparedExpensePercent_matchesMoneyManagerFormula() {
        assertEquals(100L, HomeTotalUtils.mmComparedExpensePercent(100.0, 100.0))
        assertEquals(200L, HomeTotalUtils.mmComparedExpensePercent(200.0, 100.0))
        assertEquals(50L, HomeTotalUtils.mmComparedExpensePercent(50.0, 100.0))
        assertEquals(100L, HomeTotalUtils.mmComparedExpensePercent(400.0, 0.0))
    }
}
