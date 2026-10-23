package com.mmr.core.loan

import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Calendar
import java.util.TimeZone

class LoanSchedulePlannerTest {
    @Test
    fun advanceEmiDueOneMonth_keepsSameDayOfMonth() {
        val tz = TimeZone.getDefault()
        val nov5 =
            Calendar
                .getInstance(tz)
                .apply {
                    set(2026, Calendar.NOVEMBER, 5, 12, 0, 0)
                    set(Calendar.MILLISECOND, 0)
                }.timeInMillis
        val dec5 = LoanSchedulePlanner.advanceEmiDueOneMonth(nov5, 5)
        val jan5 = LoanSchedulePlanner.advanceEmiDueOneMonth(dec5, 5)

        fun dayOfMonth(millis: Long): Int {
            val c = Calendar.getInstance(tz)
            c.timeInMillis = millis
            return c.get(Calendar.DAY_OF_MONTH)
        }

        assertEquals(5, dayOfMonth(dec5))
        assertEquals(5, dayOfMonth(jan5))
    }

    @Test
    fun buildPlannedSchedule_lastInstallmentUsesSameDueDay() {
        val tz = TimeZone.getDefault()
        val opening =
            Calendar
                .getInstance(tz)
                .apply {
                    set(2026, Calendar.OCTOBER, 1, 12, 0, 0)
                    set(Calendar.MILLISECOND, 0)
                }.timeInMillis
        val firstDue = LoanSchedulePlanner.nextEmiDueMillis(5, opening)
        val principal = 650_000.0
        val emi = LoanAmortizationEngine.computeEmi(principal, 7.75, 3)
        val entries =
            LoanAmortizationEngine.buildPlannedSchedule(
                loanId = 1L,
                principalOutstanding = principal,
                annualRatePercent = 7.75,
                emiAmount = emi,
                remainingTenureMonths = 3,
                firstDueMillis = firstDue,
                emiDayOfMonth = 5,
                periodStartMillis = opening,
            )
        assertEquals(3, entries.size)
        val lastDue = entries.last().dueDateMillis
        val c = Calendar.getInstance(tz)
        c.timeInMillis = lastDue
        assertEquals(5, c.get(Calendar.DAY_OF_MONTH))
        assertEquals(principal, entries.sumOf { it.principalPlanned }, 1.0)
    }

    @Test
    fun buildPlannedSchedule_includesInterestFromDisbursalToFirstEmi() {
        val tz = TimeZone.getDefault()
        val disbursal =
            Calendar
                .getInstance(tz)
                .apply {
                    set(2025, Calendar.OCTOBER, 28, 12, 0, 0)
                    set(Calendar.MILLISECOND, 0)
                }.timeInMillis
        val firstDue = LoanSchedulePlanner.nextEmiDueMillis(5, disbursal)
        val principal = 100_000.0
        val rate = 12.0
        val emi = LoanAmortizationEngine.computeEmi(principal, rate, 12)
        val entries =
            LoanAmortizationEngine.buildPlannedSchedule(
                loanId = 1L,
                principalOutstanding = principal,
                annualRatePercent = rate,
                emiAmount = emi,
                remainingTenureMonths = 12,
                firstDueMillis = firstDue,
                emiDayOfMonth = 5,
                periodStartMillis = disbursal,
            )
        assertEquals(12, entries.size)
        val firstInterest = entries.first().interestPlanned
        val fullMonthInterest = LoanAmortizationEngine.interestForActualDays(principal, rate, 30.0)
        assert(firstInterest < fullMonthInterest) { "first period should be shorter than a full month" }
        assert(firstInterest > 0.0)
        val expectedDays = LoanAmortizationEngine.daysBetween(disbursal, firstDue)
        val expectedInterest = LoanAmortizationEngine.interestForActualDays(principal, rate, expectedDays)
        assertEquals(expectedInterest, firstInterest, 0.05)
    }

    @Test
    fun computeTenureMonths_returnsZeroWhenEmiDoesNotCoverInterest() {
        val principal = 43_363.0
        val rate = 45.0
        val emi = 444.0
        assertEquals(0, LoanAmortizationEngine.computeTenureMonths(principal, rate, emi))
        val minEmi = LoanAmortizationEngine.minimumEmiForAmortization(principal, rate)
        assertEquals(0, LoanAmortizationEngine.computeTenureMonths(principal, rate, minEmi - 0.01))
        assert(LoanAmortizationEngine.computeTenureMonths(principal, rate, minEmi) > 0)
        val viableEmi = LoanAmortizationEngine.computeEmi(principal, rate, 24)
        assert(LoanAmortizationEngine.computeTenureMonths(principal, rate, viableEmi) >= 20)
    }

    @Test
    fun principalStepAccrual_splitsInterestAtTopUpDate() {
        val tz = TimeZone.getDefault()
        val periodStart =
            Calendar
                .getInstance(tz)
                .apply {
                    set(2026, Calendar.OCTOBER, 1, 12, 0, 0)
                    set(Calendar.MILLISECOND, 0)
                }.timeInMillis
        val topUp =
            Calendar
                .getInstance(tz)
                .apply {
                    set(2026, Calendar.OCTOBER, 15, 12, 0, 0)
                    set(Calendar.MILLISECOND, 0)
                }.timeInMillis
        val periodEnd =
            Calendar
                .getInstance(tz)
                .apply {
                    set(2026, Calendar.NOVEMBER, 5, 12, 0, 0)
                    set(Calendar.MILLISECOND, 0)
                }.timeInMillis
        val rate = 12.0
        val before = 100_000.0
        val after = 150_000.0
        val stepped =
            PrincipalStepAccrual(periodStart, topUp, before, after)
                .interestThrough(periodEnd, rate)
        val expected =
            LoanAmortizationEngine.interestForActualDays(
                before,
                rate,
                LoanAmortizationEngine.daysBetween(periodStart, topUp),
            ) +
                LoanAmortizationEngine.interestForActualDays(
                    after,
                    rate,
                    LoanAmortizationEngine.daysBetween(topUp, periodEnd),
                )
        assertEquals(expected, stepped, 0.05)
        val flatOnNewBalance =
            LoanAmortizationEngine.interestForActualDays(
                after,
                rate,
                LoanAmortizationEngine.daysBetween(topUp, periodEnd),
            )
        assert(stepped > flatOnNewBalance)
    }
}
