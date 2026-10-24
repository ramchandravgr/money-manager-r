package com.mmr.core.transaction

/**
 * Pure purchase amount math for card refund application (unit-tested without Room).
 */
object CardRefundLedgerMath {
    data class Result(
        val netAmount: Double,
        val originalAmount: Double,
        val refundedTotal: Double,
    )

    fun applyRefund(
        currentNetAmount: Double,
        currentOriginalAmount: Double?,
        currentRefundedTotal: Double,
        refundAmount: Double,
    ): Result {
        require(refundAmount > 0.0 && refundAmount.isFinite()) { "Refund amount must be greater than zero." }
        require(currentNetAmount.isFinite() && currentNetAmount >= 0.0) { "Invalid purchase amount." }
        if (refundAmount > currentNetAmount + 0.001) {
            throw IllegalArgumentException("Refund cannot exceed the remaining purchase amount.")
        }
        val gross = currentOriginalAmount ?: currentNetAmount
        val newNet = (currentNetAmount - refundAmount).coerceAtLeast(0.0)
        return Result(
            netAmount = newNet,
            originalAmount = gross,
            refundedTotal = currentRefundedTotal + refundAmount,
        )
    }
}
