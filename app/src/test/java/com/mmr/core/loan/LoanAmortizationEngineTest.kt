package com.mmr.core.loan

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LoanAmortizationEngineTest {
    @Test
    fun splitPayment_paysInterestBeforePrincipal() {
        val split =
            LoanAmortizationEngine.splitPayment(
                principalOutstanding = 100_000.0,
                annualRatePercent = 12.0,
                paymentAmount = 10_000.0,
                monthFraction = 1.0,
            )
        assertEquals(1_000.0, split.interestComponent, 0.01)
        assertEquals(9_000.0, split.principalComponent, 0.01)
        assertEquals(91_000.0, split.newPrincipalOutstanding, 0.01)
    }

    @Test
    fun computeEmi_isPositiveForTypicalLoan() {
        val emi = LoanAmortizationEngine.computeEmi(500_000.0, 8.5, 240)
        assertTrue(emi > 4_000.0)
    }
}
