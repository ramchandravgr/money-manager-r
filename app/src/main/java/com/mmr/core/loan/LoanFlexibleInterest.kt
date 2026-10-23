package com.mmr.core.loan

import com.mmr.core.database.entity.LoanEntity
import com.mmr.core.database.entity.LoanPaymentEntity

object LoanFlexibleInterest {
    /** Last payment/top-up date, or loan disbursal when nothing was recorded yet. */
    fun accrualStartMillis(
        loan: LoanEntity,
        payments: List<LoanPaymentEntity>,
    ): Long {
        val lastEvent = payments.maxOfOrNull { it.paymentDate }
        return lastEvent ?: loan.disbursalDateMillis()
    }

    fun pendingInterest(
        principalOutstanding: Double,
        annualRatePercent: Double,
        periodStartMillis: Long,
        asOfMillis: Long,
    ): Double {
        if (principalOutstanding <= 0.0 || annualRatePercent <= 0.0) return 0.0
        if (asOfMillis <= periodStartMillis) return 0.0
        return LoanAmortizationEngine.interestForActualDays(
            principalOutstanding,
            annualRatePercent,
            LoanAmortizationEngine.daysBetween(periodStartMillis, asOfMillis),
        )
    }

    /**
     * Applies payment to interest accrued since [periodStartMillis], then to principal.
     */
    fun splitPayment(
        principalOutstanding: Double,
        annualRatePercent: Double,
        paymentAmount: Double,
        periodStartMillis: Long,
        paymentDateMillis: Long,
    ): PaymentSplit {
        val safeOutstanding = maxOf(0.0, principalOutstanding)
        val safePayment = maxOf(0.0, paymentAmount)
        val interestDue =
            pendingInterest(
                safeOutstanding,
                annualRatePercent,
                periodStartMillis,
                paymentDateMillis,
            )
        val interestPaid = minOf(safePayment, interestDue)
        val principalPaid = minOf(safePayment - interestPaid, safeOutstanding)
        val newOutstanding = maxOf(0.0, safeOutstanding - principalPaid)
        return PaymentSplit(
            interestComponent = interestPaid,
            principalComponent = principalPaid,
            newPrincipalOutstanding = newOutstanding,
        )
    }
}
