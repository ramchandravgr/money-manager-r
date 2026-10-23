package com.mmr.core.loan

/**
 * Interest for one EMI period when principal increases mid-period (e.g. top-up).
 */
data class PrincipalStepAccrual(
    val periodStartMillis: Long,
    val stepMillis: Long,
    val balanceBeforeStep: Double,
    val balanceAfterStep: Double,
) {
    fun interestThrough(
        periodEndMillis: Long,
        annualRatePercent: Double,
    ): Double {
        if (periodEndMillis <= periodStartMillis) return 0.0
        val step = stepMillis.coerceIn(periodStartMillis, periodEndMillis)
        var total =
            LoanAmortizationEngine.interestForActualDays(
                balanceBeforeStep,
                annualRatePercent,
                LoanAmortizationEngine.daysBetween(periodStartMillis, step),
            )
        if (periodEndMillis > step) {
            total +=
                LoanAmortizationEngine.interestForActualDays(
                    balanceAfterStep,
                    annualRatePercent,
                    LoanAmortizationEngine.daysBetween(step, periodEndMillis),
                )
        }
        return total
    }
}
