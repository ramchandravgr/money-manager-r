package com.mmr.core.loan

import com.mmr.core.database.entity.LoanScheduleEntryEntity
import com.mmr.core.model.enums.LoanScheduleStatus
import kotlin.math.ln
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToInt

data class PaymentSplit(
    val interestComponent: Double,
    val principalComponent: Double,
    val newPrincipalOutstanding: Double,
)

object LoanAmortizationEngine {
    /** EMI must exceed first month's interest by at least this much (currency units). */
    const val MIN_PRINCIPAL_ABOVE_INTEREST_PER_EMI = 1.0

    fun monthlyRateFromAnnual(annualRatePercent: Double): Double = annualRatePercent / 12.0 / 100.0

    fun firstMonthInterest(
        outstanding: Double,
        annualRatePercent: Double,
    ): Double {
        if (outstanding <= 0.0) return 0.0
        return outstanding * monthlyRateFromAnnual(annualRatePercent)
    }

    fun computeEmi(
        principal: Double,
        annualRatePercent: Double,
        tenureMonths: Int,
    ): Double {
        if (principal <= 0.0 || tenureMonths <= 0) return 0.0
        val r = monthlyRateFromAnnual(annualRatePercent)
        if (r <= 0.0) return principal / tenureMonths
        val factor = (1.0 + r).pow(tenureMonths.toDouble())
        return principal * r * factor / (factor - 1.0)
    }

    fun computeTenureMonths(
        outstanding: Double,
        annualRatePercent: Double,
        emi: Double,
    ): Int {
        if (outstanding <= 0.0 || emi <= 0.0) return 0
        val r = monthlyRateFromAnnual(annualRatePercent)
        if (r <= 0.0) return max(1, (outstanding / emi).roundToInt())
        if (emi < minimumEmiForAmortization(outstanding, annualRatePercent)) return 0
        val months = ln(emi / (emi - outstanding * r)) / ln(1.0 + r)
        return months.roundToInt().coerceIn(1, 600)
    }

    fun minimumEmiForAmortization(
        outstanding: Double,
        annualRatePercent: Double,
    ): Double {
        if (outstanding <= 0.0) return 0.0
        val interest = firstMonthInterest(outstanding, annualRatePercent)
        return interest + MIN_PRINCIPAL_ABOVE_INTEREST_PER_EMI
    }

    fun isEmiValidForAmortization(
        outstanding: Double,
        annualRatePercent: Double,
        emi: Double,
    ): Boolean = emi >= minimumEmiForAmortization(outstanding, annualRatePercent)

    /**
     * Reducing-balance: interest on principal outstanding only, for [monthFraction] of a month (1.0 = full month).
     */
    fun splitPayment(
        principalOutstanding: Double,
        annualRatePercent: Double,
        paymentAmount: Double,
        monthFraction: Double = 1.0,
    ): PaymentSplit {
        val safeOutstanding = max(0.0, principalOutstanding)
        val safePayment = max(0.0, paymentAmount)
        val interestDue = safeOutstanding * monthlyRateFromAnnual(annualRatePercent) * max(0.0, monthFraction)
        val interestPaid = min(safePayment, interestDue)
        val principalPaid = min(safePayment - interestPaid, safeOutstanding)
        val newOutstanding = max(0.0, safeOutstanding - principalPaid)
        return PaymentSplit(
            interestComponent = interestPaid,
            principalComponent = principalPaid,
            newPrincipalOutstanding = newOutstanding,
        )
    }

    fun monthFractionBetween(
        lastPaymentMillis: Long?,
        paymentMillis: Long,
    ): Double {
        if (lastPaymentMillis == null || lastPaymentMillis <= 0L) return 1.0
        val days = (paymentMillis - lastPaymentMillis).toDouble() / (24.0 * 60.0 * 60.0 * 1000.0)
        if (days < 1.0) return 1.0 / 30.0
        return min(1.0, days / 30.0)
    }

    fun daysBetween(
        startMillis: Long,
        endMillis: Long,
    ): Double {
        if (endMillis <= startMillis) return 0.0
        return (endMillis - startMillis).toDouble() / (24.0 * 60.0 * 60.0 * 1000.0)
    }

    fun interestForActualDays(
        principalOutstanding: Double,
        annualRatePercent: Double,
        days: Double,
    ): Double {
        if (principalOutstanding <= 0.0 || days <= 0.0 || annualRatePercent <= 0.0) return 0.0
        return principalOutstanding * (annualRatePercent / 100.0) * (days / 365.0)
    }

    fun buildPlannedSchedule(
        loanId: Long,
        principalOutstanding: Double,
        annualRatePercent: Double,
        emiAmount: Double,
        remainingTenureMonths: Int,
        firstDueMillis: Long,
        emiDayOfMonth: Int,
        periodStartMillis: Long,
        startSequence: Int = 1,
        firstPeriodPrincipalStep: PrincipalStepAccrual? = null,
    ): List<LoanScheduleEntryEntity> {
        if (principalOutstanding <= 0.0 || remainingTenureMonths <= 0) return emptyList()
        var balance = principalOutstanding
        val entries = mutableListOf<LoanScheduleEntryEntity>()
        var due = firstDueMillis
        var periodStart = periodStartMillis
        val lastSeq = startSequence + remainingTenureMonths - 1
        for (seq in startSequence..lastSeq) {
            if (balance <= 0.01) break
            val interest =
                if (seq == startSequence && firstPeriodPrincipalStep != null) {
                    firstPeriodPrincipalStep.interestThrough(due, annualRatePercent)
                } else {
                    val days = daysBetween(periodStart, due).coerceAtLeast(1.0)
                    interestForActualDays(balance, annualRatePercent, days)
                }
            val isLast = seq == lastSeq
            val principalPart =
                if (isLast) {
                    balance
                } else {
                    min(emiAmount - interest, balance).coerceAtLeast(0.0)
                }
            val actualEmi = principalPart + interest
            entries +=
                LoanScheduleEntryEntity(
                    loanId = loanId,
                    sequenceNumber = seq,
                    dueDateMillis = due,
                    emiAmount = actualEmi,
                    principalPlanned = principalPart,
                    interestPlanned = interest,
                    status = LoanScheduleStatus.PLANNED,
                    annualRatePercentAtPlan = annualRatePercent,
                )
            balance -= principalPart
            periodStart = due
            due = LoanSchedulePlanner.advanceEmiDueOneMonth(due, emiDayOfMonth)
        }
        return entries
    }
}
