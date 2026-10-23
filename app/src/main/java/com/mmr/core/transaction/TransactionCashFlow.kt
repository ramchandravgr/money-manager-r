package com.mmr.core.transaction

import com.mmr.core.database.entity.TransactionEntity
import com.mmr.core.model.enums.LoanType
import com.mmr.core.model.enums.RecordStatus
import com.mmr.core.model.enums.TransactionType

/**
 * How approved transactions contribute to Home / Stats income & expense totals.
 * Loan repayments are stored as [TransactionType.TRANSFER] but count as cash out (borrowed)
 * or cash in (lent). Loan top-ups are excluded (liability / principal movement).
 */
object TransactionCashFlow {
    fun isLoanTopUp(transaction: TransactionEntity): Boolean =
        transaction.sourceReferenceId?.startsWith("loan_topup:") == true

    fun expenseAmount(
        transaction: TransactionEntity,
        loanType: LoanType?,
    ): Double {
        if (transaction.status != RecordStatus.APPROVED) return 0.0
        return when (transaction.type) {
            TransactionType.EXPENSE -> transaction.amount
            TransactionType.TRANSFER ->
                if (
                    isLoanRepaymentTransfer(transaction, loanType, LoanType.BORROWED)
                ) {
                    transaction.amount
                } else {
                    0.0
                }
            else -> 0.0
        }
    }

    fun incomeAmount(
        transaction: TransactionEntity,
        loanType: LoanType?,
    ): Double {
        if (transaction.status != RecordStatus.APPROVED) return 0.0
        return when (transaction.type) {
            TransactionType.INCOME -> transaction.amount
            TransactionType.TRANSFER ->
                if (
                    isLoanRepaymentTransfer(transaction, loanType, LoanType.LENT)
                ) {
                    transaction.amount
                } else {
                    0.0
                }
            else -> 0.0
        }
    }

    fun countsAsExpense(
        transaction: TransactionEntity,
        loanType: LoanType?,
    ): Boolean = expenseAmount(transaction, loanType) > 0.0

    fun countsAsIncome(
        transaction: TransactionEntity,
        loanType: LoanType?,
    ): Boolean = incomeAmount(transaction, loanType) > 0.0

    /** Signed cash impact: income positive, expense negative (for running balance). */
    fun netSignedAmount(
        transaction: TransactionEntity,
        loanType: LoanType?,
    ): Double = incomeAmount(transaction, loanType) - expenseAmount(transaction, loanType)

    private fun isLoanRepaymentTransfer(
        transaction: TransactionEntity,
        loanType: LoanType?,
        expectedType: LoanType,
    ): Boolean {
        val loanId = transaction.loanId ?: return false
        if (loanId <= 0L) return false
        if (loanType != expectedType) return false
        if (isLoanTopUp(transaction)) return false
        return transaction.type == TransactionType.TRANSFER
    }
}
