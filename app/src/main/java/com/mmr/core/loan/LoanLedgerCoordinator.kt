package com.mmr.core.loan

import com.mmr.core.database.entity.LoanEntity
import com.mmr.core.database.entity.TransactionEntity
import com.mmr.core.domain.repository.AccountRepository
import com.mmr.core.domain.repository.TransactionRepository
import com.mmr.core.model.enums.LoanType
import com.mmr.core.model.enums.RecordStatus
import com.mmr.core.model.enums.TransactionSourceType
import com.mmr.core.model.enums.TransactionType
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LoanLedgerCoordinator
    @Inject
    constructor(
        private val accountRepository: AccountRepository,
        private val transactionRepository: TransactionRepository,
        private val loanCategoryResolver: LoanCategoryResolver,
    ) {
        suspend fun createRepaymentTransaction(
            loan: LoanEntity,
            amount: Double,
            dateTime: Long,
            note: String?,
            scheduleEntryId: Long? = null,
        ): Long {
            require(amount > 0.0) { "Amount must be positive." }
            val primary =
                accountRepository.getDefaultAccount()
                    ?: throw IllegalStateException("Set a primary account under Accounts (star icon).")
            val loanAccountId =
                loan.linkedLoanAccountId
                    ?: throw IllegalStateException("Link a loan liability account on this loan.")
            val categories = loanCategoryResolver.resolveForLoan(loan.name, loan.loanType)

            val (fromAccountId, toAccountId) =
                when (loan.loanType) {
                    LoanType.BORROWED -> primary.id to loanAccountId
                    LoanType.LENT -> loanAccountId to primary.id
                }

            val ref = scheduleEntryId?.let { scheduleSourceRef(it) }
            val bodyNote =
                note?.trim()?.takeIf { it.isNotEmpty() }
                    ?: "Loan payment: ${loan.name}"

            return transactionRepository.insertTransaction(
                TransactionEntity(
                    dateTime = dateTime,
                    amount = amount,
                    type = TransactionType.TRANSFER,
                    categoryId = categories.categoryId,
                    subcategoryId = categories.subcategoryId,
                    note = bodyNote,
                    accountId = fromAccountId,
                    relatedAccountId = toAccountId,
                    loanId = loan.id,
                    sourceType = TransactionSourceType.LOAN,
                    sourceReferenceId = ref,
                    status = RecordStatus.APPROVED,
                ),
            )
        }

        /** Additional disbursal on the same loan (cash in, liability up for borrowed loans). */
        suspend fun createTopUpTransaction(
            loan: LoanEntity,
            amount: Double,
            dateTime: Long,
            note: String?,
        ): Long {
            require(amount > 0.0) { "Amount must be positive." }
            val primary =
                accountRepository.getDefaultAccount()
                    ?: throw IllegalStateException("Set a primary account under Accounts (star icon).")
            val loanAccountId =
                loan.linkedLoanAccountId
                    ?: throw IllegalStateException("Link a loan liability account on this loan.")
            val categories = loanCategoryResolver.resolveForLoan(loan.name, loan.loanType)

            val (fromAccountId, toAccountId) =
                when (loan.loanType) {
                    LoanType.BORROWED -> loanAccountId to primary.id
                    LoanType.LENT -> primary.id to loanAccountId
                }

            val bodyNote =
                note?.trim()?.takeIf { it.isNotEmpty() }
                    ?: "Loan top-up: ${loan.name}"

            return transactionRepository.insertTransaction(
                TransactionEntity(
                    dateTime = dateTime,
                    amount = amount,
                    type = TransactionType.TRANSFER,
                    categoryId = categories.categoryId,
                    subcategoryId = categories.subcategoryId,
                    note = bodyNote,
                    accountId = fromAccountId,
                    relatedAccountId = toAccountId,
                    loanId = loan.id,
                    sourceType = TransactionSourceType.LOAN,
                    sourceReferenceId = "loan_topup:${loan.id}:$dateTime",
                    status = RecordStatus.APPROVED,
                ),
            )
        }

        companion object {
            fun scheduleSourceRef(scheduleEntryId: Long): String = "loan_schedule:$scheduleEntryId"
        }
    }
