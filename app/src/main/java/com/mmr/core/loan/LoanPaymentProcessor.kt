package com.mmr.core.loan

import com.mmr.core.database.entity.LoanEntity
import com.mmr.core.database.entity.LoanPaymentEntity
import com.mmr.core.model.enums.LoanPartPaymentAdjustment
import com.mmr.core.model.enums.LoanPaymentType
import com.mmr.core.model.enums.LoanRepaymentMode
import com.mmr.core.model.enums.LoanScheduleStatus
import com.mmr.core.model.enums.LoanStatus

data class LoanPaymentResult(
    val payment: LoanPaymentEntity,
    val updatedLoan: LoanEntity,
    val newStatus: LoanStatus,
)

object LoanPaymentProcessor {
    fun process(
        loan: LoanEntity,
        priorPayments: List<LoanPaymentEntity>,
        paymentAmount: Double,
        paymentType: LoanPaymentType,
        paymentDate: Long,
        annualRatePercent: Double,
        partPaymentAdjustment: LoanPartPaymentAdjustment?,
        transactionId: Long?,
        notes: String?,
    ): LoanPaymentResult {
        if (loan.outstandingAmount <= 0.0 && loan.status == LoanStatus.CLOSED) {
            throw IllegalStateException("Loan is already closed.")
        }

        val split =
            if (loan.repaymentModeEnum() == LoanRepaymentMode.FLEXIBLE) {
                val accrualStart = LoanFlexibleInterest.accrualStartMillis(loan, priorPayments)
                LoanFlexibleInterest.splitPayment(
                    principalOutstanding = loan.outstandingAmount,
                    annualRatePercent = annualRatePercent,
                    paymentAmount = paymentAmount,
                    periodStartMillis = accrualStart,
                    paymentDateMillis = paymentDate,
                )
            } else {
                LoanAmortizationEngine.splitPayment(
                    principalOutstanding = loan.outstandingAmount,
                    annualRatePercent = annualRatePercent,
                    paymentAmount = paymentAmount,
                    monthFraction = 1.0,
                )
            }

        var updatedLoan =
            loan.copy(
                outstandingAmount = split.newPrincipalOutstanding,
                updatedAt = System.currentTimeMillis(),
            )

        if (
            loan.repaymentModeEnum() == LoanRepaymentMode.FIXED_EMI &&
            paymentType == LoanPaymentType.PART_PAYMENT &&
            split.principalComponent > 0.0 &&
            partPaymentAdjustment != null
        ) {
            updatedLoan = LoanSchedulePlanner.applyPartPaymentAdjustment(updatedLoan, partPaymentAdjustment)
        }

        val newStatus =
            when {
                split.newPrincipalOutstanding <= 0.0 -> LoanStatus.CLOSED
                paymentType == LoanPaymentType.CLOSING_PAYMENT -> LoanStatus.CLOSED
                else -> loan.status
            }
        if (newStatus == LoanStatus.CLOSED) {
            updatedLoan =
                updatedLoan.copy(
                    outstandingAmount = 0.0,
                    remainingTenureMonths = 0,
                )
        }

        val payment =
            LoanPaymentEntity(
                loanId = loan.id,
                transactionId = transactionId?.takeIf { it > 0L },
                paymentAmount = paymentAmount,
                paymentDate = paymentDate,
                paymentType = paymentType,
                principalComponent = split.principalComponent,
                interestComponent = split.interestComponent,
                outstandingAfterPayment = updatedLoan.outstandingAmount,
                notes = notes?.trim()?.takeIf { it.isNotEmpty() },
            )

        return LoanPaymentResult(payment, updatedLoan, newStatus)
    }

    fun markNearestPlannedEntryPaid(
        scheduleEntries: List<com.mmr.core.database.entity.LoanScheduleEntryEntity>,
        paymentId: Long,
    ): Long? {
        val entry =
            scheduleEntries
                .filter { it.status == LoanScheduleStatus.PLANNED }
                .minByOrNull { it.dueDateMillis }
        return entry?.id
    }
}
