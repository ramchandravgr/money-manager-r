package com.mmr.core.budget

import com.mmr.core.database.entity.BudgetEntity
import com.mmr.core.model.enums.BudgetPeriodType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.ZoneId

class BudgetPeriodResolverTest {
    @Test
    fun monthly_resolve_containsAnchorDate() {
        val zone = ZoneId.of("Asia/Kolkata")
        val anchor =
            java.time.LocalDate
                .of(2026, 10, 15)
                .atStartOfDay(zone)
                .toInstant()
                .toEpochMilli()
        val budget =
            BudgetEntity(
                syncId = "b1",
                categoryId = 1L,
                amountLimit = 1_000.0,
                periodType = BudgetPeriodType.MONTHLY,
                createdAt = anchor,
                updatedAt = anchor,
            )
        val bounds = BudgetPeriodResolver.resolve(budget, anchorMillis = anchor, zoneId = zone, monthStartDay = 1)
        assertTrue(anchor in bounds.startMillis..bounds.endMillis)
        assertEquals("M-2026-10-d1", bounds.periodKey)
    }

    @Test
    fun custom_resolve_usesStoredRange() {
        val start = 1_000L
        val end = 9_000L
        val budget =
            BudgetEntity(
                syncId = "b2",
                categoryId = 1L,
                amountLimit = 500.0,
                periodType = BudgetPeriodType.CUSTOM,
                customPeriodStartMillis = start,
                customPeriodEndMillis = end,
                createdAt = start,
                updatedAt = start,
            )
        val bounds = BudgetPeriodResolver.resolve(budget, anchorMillis = 5_000L)
        assertEquals(start, bounds.startMillis)
        assertEquals(end, bounds.endMillis)
    }
}
