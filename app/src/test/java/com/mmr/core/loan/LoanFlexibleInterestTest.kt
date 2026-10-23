package com.mmr.core.loan

import com.mmr.core.database.entity.LoanEntity
import com.mmr.core.database.entity.LoanPaymentEntity
import com.mmr.core.model.enums.LoanPaymentType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar
import java.util.TimeZone

class LoanFlexibleInterestTest {
    private fun millis(
        y: Int,
        month: Int,
        day: Int,
    ): Long {
        val tz = TimeZone.getDefault()
        return Calendar
            .getInstance(tz)
            .apply {
                set(y, month, day, 12, 0, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis
    }

    @Test
    fun accrualStart_usesDisbursalWhenNoPayments() {
        val loan =
            LoanEntity(
                id = 1L,
                name = "Test",
                lenderName = "Bank",
                principalAmount = 100_000.0,
                outstandingAmount = 100_000.0,
                interestRate = 12.0,
                openingDate = millis(2026, Calendar.JANUARY, 1),
                createdAt = 0L,
                updatedAt = 0L,
            )
        assertEquals(loan.openingDate, LoanFlexibleInterest.accrualStartMillis(loan, emptyList()))
    }

    @Test
    fun accrualStart_usesLastPaymentDate() {
        val disbursal = millis(2026, Calendar.JANUARY, 1)
        val paid = millis(2026, Calendar.MARCH, 15)
        val loan =
            LoanEntity(
                id = 1L,
                name = "Test",
                lenderName = "Bank",
                principalAmount = 100_000.0,
                outstandingAmount = 90_000.0,
                interestRate = 12.0,
                openingDate = disbursal,
                createdAt = 0L,
                updatedAt = 0L,
            )
        val payments =
            listOf(
                LoanPaymentEntity(
                    id = 1L,
                    loanId = 1L,
                    paymentAmount = 10_000.0,
                    paymentDate = paid,
                    paymentType = LoanPaymentType.PART_PAYMENT,
                    outstandingAfterPayment = 90_000.0,
                ),
            )
        assertEquals(paid, LoanFlexibleInterest.accrualStartMillis(loan, payments))
    }

    @Test
    fun splitPayment_paysInterestBeforePrincipal() {
        val start = millis(2026, Calendar.JANUARY, 1)
        val payDate = millis(2026, Calendar.APRIL, 1)
        val principal = 100_000.0
        val rate = 12.0
        val interestDue = LoanFlexibleInterest.pendingInterest(principal, rate, start, payDate)
        assertTrue(interestDue > 0.0)

        val payment = interestDue + 5_000.0
        val split = LoanFlexibleInterest.splitPayment(principal, rate, payment, start, payDate)
        assertEquals(interestDue, split.interestComponent, 0.01)
        assertEquals(5_000.0, split.principalComponent, 0.01)
        assertEquals(95_000.0, split.newPrincipalOutstanding, 0.01)
    }

    @Test
    fun splitPayment_smallPaymentGoesToInterestOnly() {
        val start = millis(2026, Calendar.JANUARY, 1)
        val payDate = millis(2026, Calendar.FEBRUARY, 1)
        val principal = 100_000.0
        val split = LoanFlexibleInterest.splitPayment(principal, 12.0, 500.0, start, payDate)
        assertEquals(500.0, split.interestComponent, 0.01)
        assertEquals(0.0, split.principalComponent, 0.01)
        assertEquals(principal, split.newPrincipalOutstanding, 0.01)
    }
}
