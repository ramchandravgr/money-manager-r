package com.mmr.app.ui.components

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MmrTransactionMergeDialogPrepTest {
    @Test
    fun fromTransactionDates_distinctSortedAndCombinedAmount() {
        val prep =
            MmrTransactionMergeDialogPrep.fromTransactionDates(
                dateTimes = listOf(100L, 200L, 100L),
                totalAmount = 1500.5,
                currencySymbol = "₹",
                formatDateLabel = { "day-$it" },
            )
        assertEquals(listOf("day-200", "day-100"), prep.dateOptions.map { it.label })
        assertEquals(200L, prep.initialSelectedDateTimeMillis)
        assertTrue(prep.combinedAmountLabel.startsWith("₹"))
        assertTrue(
            prep.combinedAmountLabel.contains("1,500.50") ||
                prep.combinedAmountLabel.contains("1,500.5"),
        )
    }

    @Test
    fun mergedDateTimeForConfirm_onlyWhenMultipleDates() {
        assertNull(MmrTransactionMergeDialogPrep.mergedDateTimeForConfirm(1, 100L))
        assertEquals(100L, MmrTransactionMergeDialogPrep.mergedDateTimeForConfirm(2, 100L))
    }
}
