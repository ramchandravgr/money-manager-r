package com.rrparallelstudio.miapp.feature.home

import com.rrparallelstudio.miapp.feature.home.model.TransactionGroupUi
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class HomeCarryOverInjectorTest {
    @Test
    fun injectIntoGrouped_prependsRowOnPeriodStartDay() {
        val periodStart = LocalDate.of(2026, 11, 1)
        val groups =
            listOf(
                TransactionGroupUi(
                    dateKey = periodStart.toString(),
                    dayOfMonth = 1,
                    weekdayShort = "Sat",
                    monthYearLabel = "11.2026",
                    dayExpenseTotal = 100.0,
                    dayIncomeTotal = 0.0,
                    items =
                        listOf(
                            HomeCarryOverInjector.rowForAmount(0.0).copy(
                                id = 42L,
                                title = "Coffee",
                                isSynthetic = false,
                            ),
                        ),
                ),
            )
        val result =
            HomeCarryOverInjector.injectIntoGrouped(
                groups = groups,
                periodStart = periodStart,
                carriedForward = 250.0,
                withinDayAscending = true,
            )
        val day = result.first { it.dateKey == periodStart.toString() }
        assertEquals(HomeCarryOverInjector.ROW_ID, day.items.first().id)
        assertEquals(42L, day.items[1].id)
        assertTrue(day.items.first().isSynthetic)
    }

    @Test
    fun injectIntoGrouped_createsDayWhenMissing() {
        val periodStart = LocalDate.of(2026, 11, 1)
        val result =
            HomeCarryOverInjector.injectIntoGrouped(
                groups = emptyList(),
                periodStart = periodStart,
                carriedForward = -50.0,
                withinDayAscending = true,
            )
        assertEquals(1, result.size)
        assertEquals(periodStart.toString(), result.single().dateKey)
        assertEquals(
            false,
            result
                .single()
                .items
                .single()
                .isIncome,
        )
    }
}
