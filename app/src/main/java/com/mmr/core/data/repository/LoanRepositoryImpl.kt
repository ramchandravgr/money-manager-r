package com.mmr.core.data.repository

import androidx.room.withTransaction
import com.mmr.core.common.display.MmrMoneyFormat
import com.mmr.core.database.MmrDatabase
import com.mmr.core.database.dao.LoanDao
import com.mmr.core.database.dao.LoanInterestRevisionDao
import com.mmr.core.database.dao.LoanPaymentDao
import com.mmr.core.database.dao.LoanScheduleDao
import com.mmr.core.database.dao.TransactionDao
import com.mmr.core.database.entity.LoanEntity
import com.mmr.core.database.entity.LoanInterestRevisionEntity
import com.mmr.core.database.entity.LoanPaymentEntity
import com.mmr.core.database.entity.LoanScheduleEntryEntity
import com.mmr.core.database.model.LoanSummaryAggregate
import com.mmr.core.database.relation.LoanWithPayments
import com.mmr.core.domain.repository.AccountRepository
import com.mmr.core.domain.repository.LoanRepository
import com.mmr.core.lansync.LanSyncTombstoneRecorder
import com.mmr.core.lansync.SyncEntityType
import com.mmr.core.loan.LoanAmortizationEngine
import com.mmr.core.loan.LoanDateUtils
import com.mmr.core.loan.LoanLedgerCoordinator
import com.mmr.core.loan.LoanLedgerCoordinator.Companion.scheduleSourceRef
import com.mmr.core.loan.LoanPaymentProcessor
import com.mmr.core.loan.LoanSchedulePlanner
import com.mmr.core.loan.LoanTopUpProcessor
import com.mmr.core.loan.PrincipalStepAccrual
import com.mmr.core.loan.disbursalDateMillis
import com.mmr.core.loan.repaymentModeEnum
import com.mmr.core.model.enums.AccountType
import com.mmr.core.model.enums.LoanPartPaymentAdjustment
import com.mmr.core.model.enums.LoanPaymentType
import com.mmr.core.model.enums.LoanRepaymentMode
import com.mmr.core.model.enums.LoanScheduleStatus
import com.mmr.core.model.enums.LoanStatus
import com.mmr.core.model.enums.LoanTopUpAdjustment
import com.mmr.core.model.enums.LoanType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LoanRepositoryImpl
    @Inject
    constructor(
        private val database: MmrDatabase,
        private val loanDao: LoanDao,
        private val loanPaymentDao: LoanPaymentDao,
        private val loanInterestRevisionDao: LoanInterestRevisionDao,
        private val loanScheduleDao: LoanScheduleDao,
        private val transactionDao: TransactionDao,
        private val loanLedgerCoordinator: LoanLedgerCoordinator,
        private val accountRepository: AccountRepository,
        private val tombstoneRecorder: LanSyncTombstoneRecorder,
    ) : LoanRepository {
        override fun getAllLoans(): Flow<List<LoanEntity>> = loanDao.getAllLoans()

        override fun getLoansWithPayments(): Flow<List<LoanWithPayments>> = loanDao.getLoansWithPayments()

        override fun getLoanWithPaymentsById(id: Long): Flow<LoanWithPayments?> =
            if (id <= 0L) kotlinx.coroutines.flow.flowOf(null) else loanDao.getLoanWithPaymentsById(id)

        override fun getLoansByType(type: LoanType): Flow<List<LoanEntity>> = loanDao.getLoansByType(type)

        override fun getLoansByStatus(status: LoanStatus): Flow<List<LoanEntity>> = loanDao.getLoansByStatus(status)

        override fun getActiveLoans(): Flow<List<LoanEntity>> = loanDao.getActiveLoans()

        override suspend fun getLoanById(id: Long): LoanEntity? = if (id <= 0L) null else loanDao.getById(id)

        override fun getLoanByIdFlow(id: Long): Flow<LoanEntity?> =
            if (id <= 0L) kotlinx.coroutines.flow.flowOf(null) else loanDao.getByIdFlow(id)

        override suspend fun createLoan(loan: LoanEntity): Long {
            RepositoryGuards.requireValidLoanEntity(loan)
            return database.withTransaction {
                val loanToStore =
                    if (loan.linkedLoanAccountId != null) {
                        loan
                    } else {
                        val liabilityAccountId =
                            accountRepository.createAccount(
                                name = loan.name.trim(),
                                type = AccountType.LOAN,
                                openingBalance = loan.principalAmount,
                            )
                        loan.copy(linkedLoanAccountId = liabilityAccountId)
                    }
                val loanId = loanDao.insert(loanToStore)
                loanInterestRevisionDao.insert(
                    LoanSchedulePlanner.initialRevision(loanId, loan.interestRate, loan.openingDate),
                )
                val stored = loanDao.getById(loanId) ?: return@withTransaction loanId
                if (stored.repaymentModeEnum() == LoanRepaymentMode.FIXED_EMI) {
                    LoanSchedulePlanner.regenerateFixedEmiSchedule(
                        stored,
                        loanScheduleDao,
                        scheduleAnchorMillis = stored.openingDate,
                    )
                }
                postInitialDisbursalLedger(stored)
                loanId
            }
        }

        private suspend fun postInitialDisbursalLedger(loan: LoanEntity) {
            if (loan.principalAmount <= 0.0 || loan.linkedLoanAccountId == null) return
            val disbursalMillis = LoanDateUtils.normalizeLoanCalendarDay(loan.openingDate)
            val ref = LoanLedgerCoordinator.disbursalSourceRef(loan.id, disbursalMillis)
            if (transactionDao.findIdBySourceReference(ref) != null) return
            runCatching {
                loanLedgerCoordinator.createInitialDisbursalTransaction(loan, disbursalMillis)
            }
        }

        override suspend fun updateLoan(loan: LoanEntity) {
            require(loan.id > 0L) { "Loan id is required." }
            RepositoryGuards.requireValidLoanEntity(loan)
            loanDao.update(loan)
        }

        override suspend fun updateLoanStatus(
            loanId: Long,
            status: LoanStatus,
        ) {
            if (loanId <= 0L) throw IllegalArgumentException("Loan id is required.")
            val loan = loanDao.getById(loanId) ?: throw IllegalArgumentException("Loan with id $loanId not found")
            loanDao.updateOutstandingAndStatus(loanId, loan.outstandingAmount, status)
        }

        override suspend fun deleteLoan(loan: LoanEntity) {
            require(loan.id > 0L) { "Loan id is required." }
            tombstoneRecorder.recordDeletion(loan.syncId, SyncEntityType.LOAN)
            val liabilityAccountId = loan.linkedLoanAccountId
            database.withTransaction {
                database.notificationRuleDao().listForLoan(loan.id).forEach { notification ->
                    tombstoneRecorder.recordDeletion(notification.syncId, SyncEntityType.NOTIFICATION_RULE)
                }
                database.notificationRuleDao().deleteForLoan(loan.id)
                loanScheduleDao.deleteAllForLoan(loan.id)
                loanPaymentDao.deletePaymentsForLoan(loan.id)
                loanDao.delete(loan)
                liabilityAccountId?.let { deactivateUnusedLoanLiabilityAccount(it) }
            }
        }

        private suspend fun deactivateUnusedLoanLiabilityAccount(accountId: Long) {
            val account = database.accountDao().getById(accountId) ?: return
            if (account.type != AccountType.LOAN || !account.isActive) return
            if (loanDao.countByLinkedLoanAccountId(accountId) > 0) return
            if (transactionDao.countInvolvingAccount(accountId) > 0) return
            tombstoneRecorder.recordDeletion(account.syncId, SyncEntityType.ACCOUNT)
            database.accountDao().update(
                account.copy(
                    isActive = false,
                    updatedAt = System.currentTimeMillis(),
                ),
            )
        }

        override fun getPaymentsForLoan(loanId: Long): Flow<List<LoanPaymentEntity>> =
            if (loanId <= 0L) kotlinx.coroutines.flow.flowOf(emptyList()) else loanPaymentDao.getPaymentsForLoan(loanId)

        override suspend fun recordLoanPayment(
            loanId: Long,
            paymentAmount: Double,
            paymentType: LoanPaymentType,
            paymentDate: Long,
            partPaymentAdjustment: LoanPartPaymentAdjustment?,
            principalComponent: Double?,
            interestComponent: Double?,
            transactionId: Long?,
            notes: String?,
        ): Long {
            if (loanId <= 0L) throw IllegalArgumentException("Loan id is required.")
            RepositoryGuards.requireValidPaymentAmount(paymentAmount)

            return database.withTransaction {
                val loan =
                    loanDao.getById(loanId)
                        ?: throw IllegalArgumentException("Loan with id $loanId not found")
                val priorPayments = loanPaymentDao.getPaymentsForLoan(loanId).first()
                val rateRevision = loanInterestRevisionDao.getRateEffectiveAt(loanId, paymentDate)
                val annualRate = rateRevision?.annualRatePercent ?: loan.interestRate

                val plannedEntries =
                    if (loan.repaymentModeEnum() == LoanRepaymentMode.FIXED_EMI) {
                        loanScheduleDao.getByStatus(loanId, LoanScheduleStatus.PLANNED).first()
                    } else {
                        emptyList()
                    }
                val nextScheduleEntry = plannedEntries.minByOrNull { it.dueDateMillis }

                val resolvedTransactionId =
                    transactionId
                        ?: nextScheduleEntry?.let { entry ->
                            transactionDao.findIdBySourceReference(scheduleSourceRef(entry.id))
                        }
                        ?: loanLedgerCoordinator.createRepaymentTransaction(
                            loan = loan,
                            amount = paymentAmount,
                            dateTime = paymentDate,
                            note = notes,
                            scheduleEntryId = nextScheduleEntry?.id,
                        )

                val result =
                    LoanPaymentProcessor.process(
                        loan = loan,
                        priorPayments = priorPayments,
                        paymentAmount = paymentAmount,
                        paymentType = paymentType,
                        paymentDate = paymentDate,
                        annualRatePercent = annualRate,
                        partPaymentAdjustment = partPaymentAdjustment,
                        transactionId = resolvedTransactionId,
                        notes = notes,
                    )

                val payment =
                    result.payment.copy(
                        principalComponent = principalComponent ?: result.payment.principalComponent,
                        interestComponent = interestComponent ?: result.payment.interestComponent,
                    )
                val paymentId = loanPaymentDao.insert(payment)
                loanDao.update(result.updatedLoan)
                loanDao.updateOutstandingAndStatus(loanId, result.updatedLoan.outstandingAmount, result.newStatus)

                if (loan.repaymentModeEnum() == LoanRepaymentMode.FIXED_EMI) {
                    LoanPaymentProcessor.markNearestPlannedEntryPaid(plannedEntries, paymentId)?.let { entryId ->
                        loanScheduleDao.markEntry(entryId, LoanScheduleStatus.PAID, paymentId)
                    }
                    if (result.newStatus != LoanStatus.CLOSED) {
                        LoanSchedulePlanner.regenerateFixedEmiSchedule(
                            loanDao.getById(loanId)!!,
                            loanScheduleDao,
                            paymentDate,
                        )
                    } else {
                        loanScheduleDao.deletePlannedFrom(loanId, paymentDate)
                    }
                }
                paymentId
            }
        }

        override suspend fun recordLoanTopUp(
            loanId: Long,
            topUpAmount: Double,
            topUpDate: Long,
            topUpAdjustment: LoanTopUpAdjustment?,
            transactionId: Long?,
            notes: String?,
            postLedgerTransfer: Boolean,
        ): Long {
            if (loanId <= 0L) throw IllegalArgumentException("Loan id is required.")
            RepositoryGuards.requireValidPaymentAmount(topUpAmount)
            val topUpDateMillis = LoanDateUtils.normalizeLoanCalendarDay(topUpDate)

            return database.withTransaction {
                val loan =
                    loanDao.getById(loanId)
                        ?: throw IllegalArgumentException("Loan with id $loanId not found")
                val priorAccrualStart = emiScheduleAnchorMillis(loanId, loan)
                val balanceBeforeTopUp = loan.outstandingAmount
                val adjustment =
                    when {
                        loan.repaymentModeEnum() != LoanRepaymentMode.FIXED_EMI -> null
                        topUpAdjustment != null -> topUpAdjustment
                        else -> LoanTopUpAdjustment.EXTEND_TENURE
                    }
                if (adjustment == LoanTopUpAdjustment.EXTEND_TENURE) {
                    val emi =
                        loan.fixedEmiAmount
                            ?: throw IllegalStateException("Fixed-EMI loan is missing EMI amount.")
                    val tenure =
                        LoanAmortizationEngine.computeTenureMonths(
                            loan.outstandingAmount + topUpAmount,
                            loan.interestRate,
                            emi,
                        )
                    if (tenure <= 0) {
                        val minEmi =
                            LoanAmortizationEngine.minimumEmiForAmortization(
                                loan.outstandingAmount + topUpAmount,
                                loan.interestRate,
                            )
                        throw IllegalArgumentException(
                            "After top-up, current EMI is too low. Raise EMI (≥ ${MmrMoneyFormat.formatInr(
                                minEmi,
                            )}) or choose increase EMI.",
                        )
                    }
                }

                val resolvedTransactionId =
                    transactionId
                        ?: if (postLedgerTransfer) {
                            loanLedgerCoordinator.createTopUpTransaction(
                                loan = loan,
                                amount = topUpAmount,
                                dateTime = topUpDateMillis,
                                note = notes,
                            )
                        } else {
                            null
                        }

                val result =
                    LoanTopUpProcessor.process(
                        loan = loan,
                        topUpAmount = topUpAmount,
                        topUpDateMillis = topUpDateMillis,
                        topUpAdjustment = adjustment,
                        transactionId = resolvedTransactionId,
                        notes = notes,
                    )
                val paymentId = loanPaymentDao.insert(result.payment)
                loanDao.update(result.updatedLoan)
                increaseLoanLiabilityAccountBalance(
                    accountId = result.updatedLoan.linkedLoanAccountId,
                    principalIncrease = topUpAmount,
                )

                if (loan.repaymentModeEnum() == LoanRepaymentMode.FIXED_EMI) {
                    loanScheduleDao.deletePlannedForLoan(loanId)
                    val firstPeriodStep =
                        if (priorAccrualStart < topUpDateMillis && balanceBeforeTopUp > 0.0) {
                            PrincipalStepAccrual(
                                periodStartMillis = priorAccrualStart,
                                stepMillis = topUpDateMillis,
                                balanceBeforeStep = balanceBeforeTopUp,
                                balanceAfterStep = balanceBeforeTopUp + topUpAmount,
                            )
                        } else {
                            null
                        }
                    LoanSchedulePlanner.regenerateFixedEmiSchedule(
                        result.updatedLoan,
                        loanScheduleDao,
                        scheduleAnchorMillis = topUpDateMillis,
                        firstPeriodPrincipalStep = firstPeriodStep,
                    )
                }
                paymentId
            }
        }

        private suspend fun increaseLoanLiabilityAccountBalance(
            accountId: Long?,
            principalIncrease: Double,
        ) {
            if (accountId == null || accountId <= 0L || principalIncrease <= 0.0) return
            val account = database.accountDao().getById(accountId) ?: return
            if (account.type != AccountType.LOAN) return
            val now = System.currentTimeMillis()
            database.accountDao().update(
                account.copy(
                    openingBalance = account.openingBalance + principalIncrease,
                    currentBalance = account.currentBalance + principalIncrease,
                    updatedAt = now,
                ),
            )
        }

        override suspend fun changeInterestRate(
            loanId: Long,
            newAnnualRatePercent: Double,
            effectiveFromMillis: Long,
            note: String?,
        ) {
            require(newAnnualRatePercent >= 0.0) { "Interest rate cannot be negative." }
            database.withTransaction {
                val loan = loanDao.getById(loanId) ?: throw IllegalArgumentException("Loan not found.")
                loanInterestRevisionDao.insert(
                    LoanInterestRevisionEntity(
                        loanId = loanId,
                        effectiveFromMillis = effectiveFromMillis,
                        annualRatePercent = newAnnualRatePercent,
                        note = note?.trim()?.takeIf { it.isNotEmpty() },
                    ),
                )
                val updated = loan.copy(interestRate = newAnnualRatePercent, updatedAt = System.currentTimeMillis())
                loanDao.update(updated)
                if (loan.repaymentModeEnum() == LoanRepaymentMode.FIXED_EMI) {
                    val emi = updated.fixedEmiAmount ?: return@withTransaction
                    val tenure = updated.remainingTenureMonths ?: return@withTransaction
                    val newEmi =
                        if (updated.outstandingAmount > 0.0 && tenure > 0) {
                            LoanAmortizationEngine.computeEmi(updated.outstandingAmount, newAnnualRatePercent, tenure)
                        } else {
                            emi
                        }
                    loanDao.update(updated.copy(fixedEmiAmount = newEmi))
                    LoanSchedulePlanner.regenerateFixedEmiSchedule(
                        loanDao.getById(loanId)!!,
                        loanScheduleDao,
                        effectiveFromMillis,
                    )
                }
            }
        }

        override suspend fun changeRepaymentMode(
            loanId: Long,
            newMode: LoanRepaymentMode,
            fixedEmiOverride: Double?,
            tenureMonths: Int?,
        ) {
            if (loanId <= 0L) throw IllegalArgumentException("Loan id is required.")
            database.withTransaction {
                val loan = loanDao.getById(loanId) ?: throw IllegalArgumentException("Loan not found.")
                if (loan.status == LoanStatus.CLOSED) {
                    throw IllegalStateException("Closed loans cannot change repayment mode.")
                }
                if (loan.repaymentModeEnum() == newMode) return@withTransaction

                val now = System.currentTimeMillis()
                when (newMode) {
                    LoanRepaymentMode.FIXED_EMI -> {
                        val tenure =
                            tenureMonths ?: loan.remainingTenureMonths ?: loan.tenureMonths
                                ?: throw IllegalArgumentException("Enter tenure in months for fixed EMI.")
                        require(tenure > 0) { "Tenure must be positive." }
                        val emi =
                            fixedEmiOverride
                                ?: loan.fixedEmiAmount
                                ?: LoanAmortizationEngine.computeEmi(
                                    loan.outstandingAmount,
                                    loan.interestRate,
                                    tenure,
                                )
                        RepositoryGuards.requireValidPaymentAmount(emi)
                        val updated =
                            loan.copy(
                                repaymentMode = newMode.name,
                                tenureMonths = tenure,
                                remainingTenureMonths = tenure,
                                fixedEmiAmount = emi,
                                updatedAt = now,
                            )
                        loanDao.update(updated)
                        loanScheduleDao.deletePlannedForLoan(loanId)
                        LoanSchedulePlanner.regenerateFixedEmiSchedule(
                            loanDao.getById(loanId)!!,
                            loanScheduleDao,
                            emiScheduleAnchorMillis(loanId, updated),
                        )
                    }
                    LoanRepaymentMode.FLEXIBLE -> {
                        loanScheduleDao.deletePlannedForLoan(loanId)
                        loanDao.update(
                            loan.copy(
                                repaymentMode = newMode.name,
                                fixedEmiAmount = null,
                                updatedAt = now,
                            ),
                        )
                    }
                }
            }
        }

        override suspend fun refreshPlannedEmiSchedule(loanId: Long) {
            if (loanId <= 0L) return
            database.withTransaction {
                val loan = loanDao.getById(loanId) ?: return@withTransaction
                if (loan.repaymentModeEnum() != LoanRepaymentMode.FIXED_EMI) return@withTransaction
                loanScheduleDao.deletePlannedForLoan(loanId)
                LoanSchedulePlanner.regenerateFixedEmiSchedule(
                    loan,
                    loanScheduleDao,
                    emiScheduleAnchorMillis(loanId, loan),
                )
            }
        }

        override suspend fun updateFutureEmi(
            loanId: Long,
            newEmiAmount: Double,
        ) {
            RepositoryGuards.requireValidPaymentAmount(newEmiAmount)
            database.withTransaction {
                val loan = loanDao.getById(loanId) ?: throw IllegalArgumentException("Loan not found.")
                if (loan.repaymentModeEnum() != LoanRepaymentMode.FIXED_EMI) {
                    throw IllegalStateException("EMI updates apply only to fixed-EMI loans.")
                }
                val tenure =
                    LoanAmortizationEngine.computeTenureMonths(
                        loan.outstandingAmount,
                        loan.interestRate,
                        newEmiAmount,
                    )
                if (tenure <= 0) {
                    val minEmi =
                        LoanAmortizationEngine.minimumEmiForAmortization(
                            loan.outstandingAmount,
                            loan.interestRate,
                        )
                    throw IllegalArgumentException(
                        "EMI must be at least ₹1 above monthly interest (≥ ${MmrMoneyFormat.formatInr(minEmi)}).",
                    )
                }
                loanDao.update(
                    loan.copy(
                        fixedEmiAmount = newEmiAmount,
                        remainingTenureMonths = tenure,
                        updatedAt = System.currentTimeMillis(),
                    ),
                )
                val refreshed = loanDao.getById(loanId)!!
                LoanSchedulePlanner.regenerateFixedEmiSchedule(
                    refreshed,
                    loanScheduleDao,
                    emiScheduleAnchorMillis(loanId, refreshed),
                )
            }
        }

        private suspend fun emiScheduleAnchorMillis(
            loanId: Long,
            loan: LoanEntity,
        ): Long {
            val lastPaymentDate =
                loanPaymentDao
                    .getPaymentsForLoan(loanId)
                    .first()
                    .maxOfOrNull { it.paymentDate }
            return lastPaymentDate ?: loan.disbursalDateMillis()
        }

        override fun getInterestRevisions(loanId: Long): Flow<List<LoanInterestRevisionEntity>> =
            if (loanId <= 0L) {
                kotlinx.coroutines.flow.flowOf(emptyList())
            } else {
                loanInterestRevisionDao.getForLoan(loanId)
            }

        override fun getScheduleEntries(
            loanId: Long,
            status: LoanScheduleStatus?,
        ): Flow<List<LoanScheduleEntryEntity>> =
            when {
                loanId <= 0L -> kotlinx.coroutines.flow.flowOf(emptyList())
                status != null -> loanScheduleDao.getByStatus(loanId, status)
                else -> loanScheduleDao.getAllForLoan(loanId)
            }

        override fun getLoanSummary(): Flow<LoanSummaryAggregate> =
            combine(
                loanDao.getTotalBorrowedPrincipal(),
                loanDao.getTotalBorrowedOutstanding(),
                loanDao.getTotalLentPrincipal(),
                loanDao.getTotalLentOutstanding(),
                loanDao.getActiveLoansCount(),
            ) { borrowedPrincipal, borrowedOutstanding, lentPrincipal, lentOutstanding, activeCount ->
                LoanSummaryAggregate(
                    totalBorrowedPrincipal = borrowedPrincipal ?: 0.0,
                    totalBorrowedOutstanding = borrowedOutstanding ?: 0.0,
                    totalLentPrincipal = lentPrincipal ?: 0.0,
                    totalLentOutstanding = lentOutstanding ?: 0.0,
                    activeLoansCount = activeCount,
                )
            }
    }
