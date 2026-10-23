package com.mmr.core.backup

import com.mmr.core.backup.model.MmrBackupPayload
import com.mmr.core.database.entity.AccountEntity
import com.mmr.core.database.entity.CategoryEntity
import com.mmr.core.database.entity.LoanEntity
import com.mmr.core.database.entity.LoanPaymentEntity
import com.mmr.core.database.entity.TransactionEntity
import com.mmr.core.model.enums.AccountType
import com.mmr.core.model.enums.LoanStatus
import com.mmr.core.model.enums.LoanType
import com.mmr.core.model.enums.RecordStatus
import com.mmr.core.model.enums.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class BackupImportPlannerTest {
    @Test
    fun prepareReplace_clearsBrokenPaymentTransactionLink() {
        val payload =
            samplePayload(
                transactions = emptyList(),
                payments =
                    listOf(
                        LoanPaymentEntity(
                            id = 1L,
                            loanId = 10L,
                            transactionId = 99L,
                            paymentAmount = 1000.0,
                            outstandingAfterPayment = 9000.0,
                        ),
                    ),
            )
        val prepared = BackupImportPlanner.prepareReplace(payload)
        assertNull(prepared.loanPayments.single().transactionId)
    }

    @Test
    fun prepareMerge_skipsExistingIdsAndAddsNewOnes() {
        val existing =
            BackupImportPlanner.ExistingIds(
                categories = setOf(1L),
                subcategories = emptySet(),
                accounts = setOf(5L),
                loans = emptySet(),
                loanPayments = emptySet(),
                transactions = setOf(100L),
                smsRecords = emptySet(),
                recurringRules = emptySet(),
                notificationRules = emptySet(),
                loanRevisions = emptySet(),
                loanScheduleEntries = emptySet(),
                transactionApprovals = emptySet(),
            )
        val payload =
            samplePayload(
                categories = listOf(category(1L), category(2L)),
                accounts = listOf(account(5L), account(6L)),
                loans =
                    listOf(
                        loan(10L, linkedLoanAccountId = 6L),
                    ),
                transactions =
                    listOf(
                        transaction(100L, accountId = 5L),
                        transaction(101L, accountId = 6L),
                    ),
            )
        val merged = BackupImportPlanner.prepareMerge(payload, existing)
        assertEquals(1, merged.categories.size)
        assertEquals(2L, merged.categories.single().id)
        assertEquals(1, merged.accounts.size)
        assertEquals(6L, merged.accounts.single().id)
        assertEquals(1, merged.loans.size)
        assertEquals(1, merged.transactions.size)
        assertEquals(101L, merged.transactions.single().id)
    }

    private fun samplePayload(
        categories: List<CategoryEntity> = emptyList(),
        accounts: List<AccountEntity> = emptyList(),
        loans: List<LoanEntity> = emptyList(),
        transactions: List<TransactionEntity> = emptyList(),
        payments: List<LoanPaymentEntity> = emptyList(),
    ) = MmrBackupPayload(
        formatVersion = 2,
        appId = "com.mmr.app",
        exportedAtEpochMillis = 0L,
        databaseVersion = 7,
        categories = categories,
        subcategories = emptyList(),
        accounts = accounts,
        loans = loans,
        transactions = transactions,
        loanPayments = payments,
        recurringRules = emptyList(),
        notificationRules = emptyList(),
        smsMessageRecords = emptyList(),
        transactionApprovals = emptyList(),
    )

    private fun category(id: Long) =
        CategoryEntity(
            id = id,
            name = "Cat$id",
            type = com.mmr.core.model.enums.CategoryType.EXPENSE,
            colorHex = "#000000",
        )

    private fun account(id: Long) =
        AccountEntity(
            id = id,
            name = "Acc$id",
            type = AccountType.BANK,
            openingBalance = 0.0,
            currentBalance = 0.0,
        )

    private fun loan(
        id: Long,
        linkedLoanAccountId: Long?,
    ) = LoanEntity(
        id = id,
        name = "Loan$id",
        lenderName = "Bank",
        principalAmount = 10000.0,
        outstandingAmount = 10000.0,
        interestRate = 10.0,
        openingDate = 0L,
        loanType = LoanType.BORROWED,
        status = LoanStatus.ACTIVE,
        linkedLoanAccountId = linkedLoanAccountId,
    )

    private fun transaction(
        id: Long,
        accountId: Long,
    ) = TransactionEntity(
        id = id,
        amount = 100.0,
        type = TransactionType.EXPENSE,
        dateTime = 1L,
        accountId = accountId,
        status = RecordStatus.APPROVED,
    )
}
