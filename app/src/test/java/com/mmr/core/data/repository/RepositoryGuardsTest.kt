package com.mmr.core.data.repository

import com.mmr.core.database.entity.LoanEntity
import com.mmr.core.database.entity.TransactionEntity
import com.mmr.core.model.enums.LoanType
import com.mmr.core.model.enums.RecordStatus
import com.mmr.core.model.enums.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class RepositoryGuardsTest {
    @Test
    fun normalizedQueryRange_swapsInvertedBounds() {
        val (start, end) = RepositoryGuards.normalizedQueryRange(500L, 100L)
        assertEquals(100L, start)
        assertEquals(500L, end)
    }

    @Test
    fun requireValidTransactionEntity_rejectsNonPositiveAmount() {
        val tx =
            TransactionEntity(
                dateTime = System.currentTimeMillis(),
                amount = 0.0,
                type = TransactionType.EXPENSE,
                accountId = 1L,
                status = RecordStatus.APPROVED,
            )
        assertThrows(IllegalArgumentException::class.java) {
            RepositoryGuards.requireValidTransactionEntity(tx)
        }
    }

    @Test
    fun requireValidLoanEntity_rejectsOutstandingAbovePrincipal() {
        val loan =
            LoanEntity(
                name = "Test",
                lenderName = "Bank",
                principalAmount = 1000.0,
                interestRate = 0.0,
                outstandingAmount = 1500.0,
                openingDate = System.currentTimeMillis(),
                loanType = LoanType.BORROWED,
            )
        assertThrows(IllegalArgumentException::class.java) {
            RepositoryGuards.requireValidLoanEntity(loan)
        }
    }
}
