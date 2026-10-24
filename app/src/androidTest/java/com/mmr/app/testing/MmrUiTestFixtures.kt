package com.mmr.app.testing

import com.mmr.core.database.MmrDatabase
import com.mmr.core.database.entity.LoanEntity
import com.mmr.core.database.entity.TransactionEntity
import com.mmr.core.domain.repository.ApprovalRepository
import com.mmr.core.domain.repository.LoanRepository
import com.mmr.core.model.enums.LoanRepaymentMode
import com.mmr.core.model.enums.LoanStatus
import com.mmr.core.model.enums.LoanType
import com.mmr.core.model.enums.RecordStatus
import com.mmr.core.model.enums.SmsApprovalKind
import com.mmr.core.model.enums.SmsApprovalState
import com.mmr.core.model.enums.TransactionSourceType
import com.mmr.core.model.enums.TransactionType
import com.mmr.core.parser.model.ParsedSmsCandidate
import com.mmr.core.parser.model.ParserConfidence
import kotlinx.coroutines.flow.first

/**
 * Seeds [MmrDatabase] for instrumented UI tests (Hilt + in-memory DB from [com.mmr.app.di.TestDatabaseModule]).
 */
object MmrUiTestFixtures {
    data class HomeSummaryFixture(
        val totalIncome: Double,
        val totalExpense: Double,
        val netBalance: Double,
    )

    data class SearchTargetFixture(
        val note: String,
        val amount: Double,
        val transactionId: Long,
    )

    data class SingleTransactionFixture(
        val note: String,
        val transactionId: Long,
        val amount: Double,
    )

    data class PendingApprovalFixture(
        val approvalId: Long,
        val merchant: String,
        val amount: Double,
    )

    data class SmsMergeFixture(
        val approvalId: Long,
        val transactionId: Long,
        val ledgerNote: String,
        val amount: Double,
    )

    data class CardRefundApplyFixture(
        val purchaseMerchant: String,
        val purchaseTransactionId: Long,
        val refundApprovalId: Long,
        val purchaseAmount: Double,
        val refundAmount: Double,
        val netAmountAfterRefund: Double,
    )

    suspend fun clearApprovals(database: MmrDatabase) {
        database.backupDao().deleteAllTransactionApprovals()
        database.backupDao().deleteAllSmsMessageRecords()
    }

    suspend fun clearTransactions(database: MmrDatabase) {
        database.backupDao().deleteAllTransactions()
    }

    suspend fun clearLedgerForApprovalTest(database: MmrDatabase) {
        clearTransactions(database)
        clearApprovals(database)
    }

    /** Clears user ledger data between UI tests (keeps seeded accounts/categories). */
    suspend fun resetUiTestDatabase(database: MmrDatabase) {
        clearTransactions(database)
        clearApprovals(database)
        database.backupDao().deleteAllLoanPayments()
        database.backupDao().deleteAllLoanScheduleEntries()
        database.backupDao().deleteAllLoanInterestRevisions()
        database.backupDao().deleteAllLoans()
        database.backupDao().deleteAllRecurringRules()
    }

    suspend fun seedTwoMergeableExpenses(
        database: MmrDatabase,
        accountId: Long,
        amountA: Double = 10.0,
        amountB: Double = 20.0,
    ): Pair<Long, Long> {
        val now = System.currentTimeMillis()
        val idA =
            insertApprovedTransaction(
                database,
                accountId,
                amountA,
                TransactionType.EXPENSE,
                now,
                note = "Merge A",
            )
        val idB =
            insertApprovedTransaction(
                database,
                accountId,
                amountB,
                TransactionType.EXPENSE,
                now + 1,
                note = "Merge B",
            )
        return idA to idB
    }

    suspend fun requireDefaultAccountId(database: MmrDatabase): Long {
        val default = database.accountDao().getDefaultAccount()
        if (default != null) return default.id
        val first =
            database.accountDao().getAccountsOnce().firstOrNull()
                ?: throw IllegalStateException("No accounts in test DB; check TestDatabaseModule seeding.")
        return first.id
    }

    suspend fun insertApprovedTransaction(
        database: MmrDatabase,
        accountId: Long,
        amount: Double,
        type: TransactionType,
        dateTimeMillis: Long = System.currentTimeMillis(),
        note: String? = null,
        categoryId: Long? = null,
    ): Long =
        database.transactionDao().insert(
            TransactionEntity(
                dateTime = dateTimeMillis,
                amount = amount,
                type = type,
                categoryId = categoryId,
                accountId = accountId,
                note = note,
                sourceType = TransactionSourceType.MANUAL,
                status = RecordStatus.APPROVED,
            ),
        )

    /**
     * Clears transactions, then inserts income ₹50 and expenses ₹100 + ₹200 in the current month.
     */
    suspend fun seedHomeSummarySample(database: MmrDatabase): HomeSummaryFixture {
        clearTransactions(database)
        val accountId = requireDefaultAccountId(database)
        val now = System.currentTimeMillis()
        insertApprovedTransaction(database, accountId, 100.0, TransactionType.EXPENSE, now, note = "Fixture expense 1")
        insertApprovedTransaction(database, accountId, 200.0, TransactionType.EXPENSE, now, note = "Fixture expense 2")
        insertApprovedTransaction(database, accountId, 50.0, TransactionType.INCOME, now, note = "Fixture income")
        return HomeSummaryFixture(
            totalIncome = 50.0,
            totalExpense = 300.0,
            netBalance = -250.0,
        )
    }

    /**
     * Clears transactions and inserts one expense with a unique [note] for search UI tests.
     */
    suspend fun seedSearchTarget(
        database: MmrDatabase,
        note: String,
        amount: Double = 88.0,
    ): SearchTargetFixture {
        clearTransactions(database)
        val accountId = requireDefaultAccountId(database)
        val id =
            insertApprovedTransaction(
                database = database,
                accountId = accountId,
                amount = amount,
                type = TransactionType.EXPENSE,
                note = note,
            )
        return SearchTargetFixture(note = note, amount = amount, transactionId = id)
    }

    /**
     * Inserts one PENDING SMS approval (no inbox scan). Requires default account from [TestDatabaseModule].
     */
    suspend fun seedPendingApproval(
        database: MmrDatabase,
        approvalRepository: ApprovalRepository,
        merchant: String,
        amount: Double = 315.0,
    ): PendingApprovalFixture {
        clearLedgerForApprovalTest(database)
        val hash = "ui-fixture-${System.nanoTime()}"
        val inserted =
            approvalRepository.insertCandidates(
                listOf(
                    ParsedSmsCandidate(
                        sender = "BANK-UI-TEST",
                        amount = amount,
                        type = TransactionType.EXPENSE,
                        timestamp = System.currentTimeMillis(),
                        merchant = merchant,
                        referenceNumber = null,
                        accountLastDigits = null,
                        balanceAfter = null,
                        categoryHint = null,
                        rawMessageId = "sms-$hash",
                        messageHash = hash,
                        confidenceScore = 90,
                        confidenceBand = ParserConfidence.HIGH,
                        rawBody = "spent $amount at $merchant",
                    ),
                ),
            )
        require(inserted == 1) { "Expected one pending approval, inserted $inserted" }
        val approvalId =
            approvalRepository
                .getApprovalsByState(SmsApprovalState.PENDING)
                .first()
                .first()
                .id
        return PendingApprovalFixture(approvalId = approvalId, merchant = merchant, amount = amount)
    }

    /**
     * Ledger row plus a pending SMS approval that matches [findMergeCandidatesForApproval] (same account, amount, ±3 days).
     */
    suspend fun seedSmsMergeScenario(
        database: MmrDatabase,
        approvalRepository: ApprovalRepository,
        amount: Double = 250.0,
    ): SmsMergeFixture {
        clearLedgerForApprovalTest(database)
        val accountId = requireDefaultAccountId(database)
        val now = System.currentTimeMillis()
        val note = "Merge ledger ${System.nanoTime()}"
        val transactionId =
            insertApprovedTransaction(
                database = database,
                accountId = accountId,
                amount = amount,
                type = TransactionType.EXPENSE,
                dateTimeMillis = now,
                note = note,
            )
        val hash = "merge-fixture-${System.nanoTime()}"
        val merchant = "Merge merchant"
        val inserted =
            approvalRepository.insertCandidates(
                listOf(
                    ParsedSmsCandidate(
                        sender = "BANK-UI-TEST",
                        amount = amount,
                        type = TransactionType.EXPENSE,
                        timestamp = now,
                        merchant = merchant,
                        referenceNumber = null,
                        accountLastDigits = null,
                        balanceAfter = null,
                        categoryHint = null,
                        rawMessageId = "sms-$hash",
                        messageHash = hash,
                        confidenceScore = 90,
                        confidenceBand = ParserConfidence.HIGH,
                        rawBody = "spent $amount at $merchant",
                    ),
                ),
            )
        require(inserted == 1) { "Expected one pending approval, inserted $inserted" }
        val approvalId =
            approvalRepository
                .getApprovalsByState(SmsApprovalState.PENDING)
                .first()
                .first()
                .id
        approvalRepository.updateApproval(
            id = approvalId,
            amount = amount,
            type = TransactionType.EXPENSE,
            categoryId = null,
            accountId = accountId,
            note = merchant,
            dateTimeMillis = now,
        )
        return SmsMergeFixture(
            approvalId = approvalId,
            transactionId = transactionId,
            ledgerNote = note,
            amount = amount,
        )
    }

    suspend fun seedSingleExpense(
        database: MmrDatabase,
        note: String,
        amount: Double = 42.0,
    ): SingleTransactionFixture {
        clearTransactions(database)
        val accountId = requireDefaultAccountId(database)
        val id =
            insertApprovedTransaction(
                database = database,
                accountId = accountId,
                amount = amount,
                type = TransactionType.EXPENSE,
                note = note,
            )
        return SingleTransactionFixture(note = note, transactionId = id, amount = amount)
    }

    suspend fun seedFlexibleLoan(
        loanRepository: LoanRepository,
        name: String,
        principal: Double = 5_000.0,
    ): Long =
        loanRepository.createLoan(
            LoanEntity(
                name = name,
                lenderName = "Test Bank",
                principalAmount = principal,
                interestRate = 0.0,
                outstandingAmount = principal,
                openingDate = System.currentTimeMillis(),
                loanType = LoanType.BORROWED,
                status = LoanStatus.ACTIVE,
                repaymentMode = LoanRepaymentMode.FLEXIBLE.name,
            ),
        )

    /**
     * Approved card purchase plus a pending [SmsApprovalKind.CARD_REFUND] for the same card last four.
     */
    suspend fun seedCardRefundApplyScenario(
        database: MmrDatabase,
        approvalRepository: ApprovalRepository,
        purchaseAmount: Double = 500.0,
        refundAmount: Double = 150.0,
        cardLastFour: String = "4148",
    ): CardRefundApplyFixture {
        clearLedgerForApprovalTest(database)
        val accountId = requireDefaultAccountId(database)
        val merchant = "UiTest CC purchase ${System.nanoTime()}"
        val now = System.currentTimeMillis()
        val purchaseHash = "ui-cc-purchase-$merchant"
        val purchaseInserted =
            approvalRepository.insertCandidates(
                listOf(
                    ParsedSmsCandidate(
                        sender = "HDFCBK",
                        amount = purchaseAmount,
                        type = TransactionType.EXPENSE,
                        timestamp = now,
                        merchant = merchant,
                        referenceNumber = null,
                        accountLastDigits = cardLastFour,
                        balanceAfter = null,
                        categoryHint = null,
                        rawMessageId = "sms-$purchaseHash",
                        messageHash = purchaseHash,
                        confidenceScore = 90,
                        confidenceBand = ParserConfidence.HIGH,
                        rawBody = "spent $purchaseAmount on card xx$cardLastFour at $merchant",
                    ),
                ),
            )
        require(purchaseInserted == 1) { "Expected one purchase approval, inserted $purchaseInserted" }
        val purchaseApprovalId =
            approvalRepository
                .getApprovalsByState(SmsApprovalState.PENDING)
                .first()
                .first()
                .id
        val purchaseTxnId = approvalRepository.approveCandidate(purchaseApprovalId, accountId, null)

        val refundHash = "ui-cc-refund-$merchant"
        val refundInserted =
            approvalRepository.insertCandidates(
                listOf(
                    ParsedSmsCandidate(
                        sender = "HDFCBK",
                        amount = refundAmount,
                        type = TransactionType.EXPENSE,
                        timestamp = now + 1,
                        merchant = "Amazon",
                        referenceNumber = null,
                        accountLastDigits = cardLastFour,
                        balanceAfter = null,
                        categoryHint = null,
                        rawMessageId = "sms-$refundHash",
                        messageHash = refundHash,
                        confidenceScore = 88,
                        confidenceBand = ParserConfidence.HIGH,
                        rawBody = "Transaction Reversed! card xx$cardLastFour Amt: Rs.$refundAmount",
                        approvalKind = SmsApprovalKind.CARD_REFUND,
                    ),
                ),
            )
        require(refundInserted == 1) { "Expected one refund approval, inserted $refundInserted" }
        val refundApprovalId =
            approvalRepository
                .getApprovalsByState(SmsApprovalState.PENDING)
                .first()
                .first()
                .id

        return CardRefundApplyFixture(
            purchaseMerchant = merchant,
            purchaseTransactionId = purchaseTxnId,
            refundApprovalId = refundApprovalId,
            purchaseAmount = purchaseAmount,
            refundAmount = refundAmount,
            netAmountAfterRefund = purchaseAmount - refundAmount,
        )
    }
}
