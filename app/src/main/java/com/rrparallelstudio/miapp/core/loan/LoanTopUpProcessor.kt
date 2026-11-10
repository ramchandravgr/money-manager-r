package com.rrparallelstudio.miapp.core.loan

import com.rrparallelstudio.miapp.core.database.entity.LoanEntity
import com.rrparallelstudio.miapp.core.database.entity.LoanPaymentEntity
import com.rrparallelstudio.miapp.core.model.enums.LoanPaymentType
import com.rrparallelstudio.miapp.core.model.enums.LoanRepaymentMode
import com.rrparallelstudio.miapp.core.model.enums.LoanStatus
import com.rrparallelstudio.miapp.core.model.enums.LoanTopUpAdjustment

data class LoanTopUpResult(
    val payment: LoanPaymentEntity,
    val updatedLoan: LoanEntity,
)

object LoanTopUpProcessor {
    fun process(
        loan: LoanEntity,
        topUpAmount: Double,
        topUpDateMillis: Long,
        topUpAdjustment: LoanTopUpAdjustment?,
        transactionId: Long?,
        notes: String?,
    ): LoanTopUpResult {
        require(topUpAmount > 0.0) { "Top-up amount must be positive." }
        if (loan.status == LoanStatus.CLOSED) {
            throw IllegalStateException("Closed loans cannot receive a top-up.")
        }

        val newPrincipal = loan.principalAmount + topUpAmount
        val newOutstanding = loan.outstandingAmount + topUpAmount
        var updatedLoan =
            loan.copy(
                principalAmount = newPrincipal,
                outstandingAmount = newOutstanding,
                updatedAt = System.currentTimeMillis(),
            )

        if (
            loan.repaymentModeEnum() == LoanRepaymentMode.FIXED_EMI &&
            topUpAdjustment != null
        ) {
            updatedLoan = LoanSchedulePlanner.applyTopUpAdjustment(updatedLoan, topUpAdjustment)
        }

        val payment =
            LoanPaymentEntity(
                loanId = loan.id,
                transactionId = transactionId?.takeIf { it > 0L },
                paymentAmount = topUpAmount,
                paymentDate = topUpDateMillis,
                paymentType = LoanPaymentType.TOP_UP,
                principalComponent = topUpAmount,
                interestComponent = 0.0,
                outstandingAfterPayment = newOutstanding,
                notes = notes?.trim()?.takeIf { it.isNotEmpty() },
            )

        return LoanTopUpResult(payment, updatedLoan)
    }
}
