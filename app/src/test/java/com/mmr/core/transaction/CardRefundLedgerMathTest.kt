package com.mmr.core.transaction

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class CardRefundLedgerMathTest {
    @Test
    fun partialRefund_reducesNetAndTracksOriginal() {
        val result =
            CardRefundLedgerMath.applyRefund(
                currentNetAmount = 500.0,
                currentOriginalAmount = null,
                currentRefundedTotal = 0.0,
                refundAmount = 150.0,
            )
        assertEquals(350.0, result.netAmount, 0.01)
        assertEquals(500.0, result.originalAmount, 0.01)
        assertEquals(150.0, result.refundedTotal, 0.01)
    }

    @Test
    fun fullRefund_zerosNet() {
        val result =
            CardRefundLedgerMath.applyRefund(
                currentNetAmount = 200.0,
                currentOriginalAmount = 200.0,
                currentRefundedTotal = 0.0,
                refundAmount = 200.0,
            )
        assertEquals(0.0, result.netAmount, 0.01)
        assertEquals(200.0, result.refundedTotal, 0.01)
    }

    @Test
    fun secondPartialRefund_stacksOnRefundedTotal() {
        val first =
            CardRefundLedgerMath.applyRefund(
                currentNetAmount = 500.0,
                currentOriginalAmount = null,
                currentRefundedTotal = 0.0,
                refundAmount = 100.0,
            )
        val second =
            CardRefundLedgerMath.applyRefund(
                currentNetAmount = first.netAmount,
                currentOriginalAmount = first.originalAmount,
                currentRefundedTotal = first.refundedTotal,
                refundAmount = 50.0,
            )
        assertEquals(350.0, second.netAmount, 0.01)
        assertEquals(500.0, second.originalAmount, 0.01)
        assertEquals(150.0, second.refundedTotal, 0.01)
    }

    @Test
    fun refundExceedsRemaining_throws() {
        assertThrows(IllegalArgumentException::class.java) {
            CardRefundLedgerMath.applyRefund(
                currentNetAmount = 50.0,
                currentOriginalAmount = null,
                currentRefundedTotal = 0.0,
                refundAmount = 100.0,
            )
        }
    }
}
