package com.mmr.core.transaction

import com.mmr.core.database.entity.TransactionEntity
import com.mmr.core.model.enums.LoanType
import com.mmr.core.model.enums.RecordStatus
import com.mmr.core.model.enums.TransactionSourceType
import com.mmr.core.model.enums.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TransactionCashFlowTest {
    @Test
    fun borrowedLoanRepayment_countsAsExpense() {
        val txn =
            TransactionEntity(
                dateTime = 1L,
                amount = 15_000.0,
                type = TransactionType.TRANSFER,
                categoryId = 1L,
                accountId = 1L,
                relatedAccountId = 2L,
                loanId = 10L,
                sourceType = TransactionSourceType.LOAN,
                status = RecordStatus.APPROVED,
            )
        assertEquals(15_000.0, TransactionCashFlow.expenseAmount(txn, LoanType.BORROWED), 0.01)
        assertFalse(TransactionCashFlow.countsAsIncome(txn, LoanType.BORROWED))
    }

    @Test
    fun loanDisbursal_excludedFromTotals() {
        val txn =
            TransactionEntity(
                dateTime = 1L,
                amount = 100_000.0,
                type = TransactionType.TRANSFER,
                categoryId = 1L,
                accountId = 1L,
                relatedAccountId = 2L,
                loanId = 10L,
                sourceType = TransactionSourceType.LOAN,
                sourceReferenceId = "loan_disbursal:10:123",
                status = RecordStatus.APPROVED,
            )
        assertEquals(0.0, TransactionCashFlow.expenseAmount(txn, LoanType.BORROWED), 0.01)
        assertEquals(0.0, TransactionCashFlow.incomeAmount(txn, LoanType.BORROWED), 0.01)
        assertTrue(TransactionCashFlow.isLoanPrincipalMovement(txn))
    }

    @Test
    fun loanTopUp_excludedFromTotals() {
        val txn =
            TransactionEntity(
                dateTime = 1L,
                amount = 5_000.0,
                type = TransactionType.TRANSFER,
                categoryId = 1L,
                accountId = 1L,
                relatedAccountId = 2L,
                loanId = 10L,
                sourceType = TransactionSourceType.LOAN,
                sourceReferenceId = "loan_topup:10:123",
                status = RecordStatus.APPROVED,
            )
        assertEquals(0.0, TransactionCashFlow.expenseAmount(txn, LoanType.BORROWED), 0.01)
        assertEquals(0.0, TransactionCashFlow.incomeAmount(txn, LoanType.BORROWED), 0.01)
    }

    @Test
    fun plainExpense_unchanged() {
        val txn =
            TransactionEntity(
                dateTime = 1L,
                amount = 100.0,
                type = TransactionType.EXPENSE,
                categoryId = 1L,
                accountId = 1L,
                status = RecordStatus.APPROVED,
            )
        assertEquals(100.0, TransactionCashFlow.expenseAmount(txn, null), 0.01)
    }

    @Test
    fun lentLoanRepayment_countsAsIncome() {
        val txn =
            TransactionEntity(
                dateTime = 1L,
                amount = 2_000.0,
                type = TransactionType.TRANSFER,
                categoryId = 1L,
                accountId = 1L,
                relatedAccountId = 2L,
                loanId = 3L,
                sourceType = TransactionSourceType.LOAN,
                status = RecordStatus.APPROVED,
            )
        assertTrue(TransactionCashFlow.countsAsIncome(txn, LoanType.LENT))
        assertEquals(2_000.0, TransactionCashFlow.incomeAmount(txn, LoanType.LENT), 0.01)
    }
}
