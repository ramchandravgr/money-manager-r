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

        /** Initial principal at loan opening — same transfer shape as top-up, dated on [dateTime] (disbursal day). */
        suspend fun createInitialDisbursalTransaction(
            loan: LoanEntity,
            dateTime: Long,
        ): Long {
            require(loan.principalAmount > 0.0) { "Principal must be positive." }
            return createPrincipalMovementTransaction(
                loan = loan,
                amount = loan.principalAmount,
                dateTime = dateTime,
                sourceReferenceId = disbursalSourceRef(loan.id, dateTime),
                defaultNote = "Loan disbursal: ${loan.name}",
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

            return createPrincipalMovementTransaction(
                loan = loan,
                amount = amount,
                dateTime = dateTime,
                sourceReferenceId = topUpSourceRef(loan.id, dateTime),
                defaultNote = bodyNote,
                noteOverride = bodyNote,
                categories = categories,
                fromAccountId = fromAccountId,
                toAccountId = toAccountId,
            )
        }

        private suspend fun createPrincipalMovementTransaction(
            loan: LoanEntity,
            amount: Double,
            dateTime: Long,
            sourceReferenceId: String,
            defaultNote: String,
            noteOverride: String? = null,
            categories: LoanCategoryIds? = null,
            fromAccountId: Long? = null,
            toAccountId: Long? = null,
        ): Long {
            require(amount > 0.0) { "Amount must be positive." }
            val primary =
                accountRepository.getDefaultAccount()
                    ?: throw IllegalStateException("Set a primary account under Accounts (star icon).")
            val loanAccountId =
                loan.linkedLoanAccountId
                    ?: throw IllegalStateException("Link a loan liability account on this loan.")
            val resolvedCategories = categories ?: loanCategoryResolver.resolveForLoan(loan.name, loan.loanType)
            val (fromId, toId) =
                if (fromAccountId != null && toAccountId != null) {
                    fromAccountId to toAccountId
                } else {
                    when (loan.loanType) {
                        LoanType.BORROWED -> loanAccountId to primary.id
                        LoanType.LENT -> primary.id to loanAccountId
                    }
                }
            val bodyNote = noteOverride ?: defaultNote
            return transactionRepository.insertTransaction(
                TransactionEntity(
                    dateTime = dateTime,
                    amount = amount,
                    type = TransactionType.TRANSFER,
                    categoryId = resolvedCategories.categoryId,
                    subcategoryId = resolvedCategories.subcategoryId,
                    note = bodyNote,
                    accountId = fromId,
                    relatedAccountId = toId,
                    loanId = loan.id,
                    sourceType = TransactionSourceType.LOAN,
                    sourceReferenceId = sourceReferenceId,
                    status = RecordStatus.APPROVED,
                ),
            )
        }

        companion object {
            fun scheduleSourceRef(scheduleEntryId: Long): String = "loan_schedule:$scheduleEntryId"

            fun topUpSourceRef(
                loanId: Long,
                dateTimeMillis: Long,
            ): String = "loan_topup:$loanId:$dateTimeMillis"

            fun disbursalSourceRef(
                loanId: Long,
                dateTimeMillis: Long,
            ): String = "loan_disbursal:$loanId:$dateTimeMillis"
        }
    }
