package com.rrparallelstudio.miapp.core.domain.repository

import com.rrparallelstudio.miapp.core.database.entity.LoanEntity
import com.rrparallelstudio.miapp.core.database.entity.LoanInterestRevisionEntity
import com.rrparallelstudio.miapp.core.database.entity.LoanPaymentEntity
import com.rrparallelstudio.miapp.core.database.entity.LoanScheduleEntryEntity
import com.rrparallelstudio.miapp.core.database.model.LoanSummaryAggregate
import com.rrparallelstudio.miapp.core.database.relation.LoanWithPayments
import com.rrparallelstudio.miapp.core.model.enums.LoanPartPaymentAdjustment
import com.rrparallelstudio.miapp.core.model.enums.LoanPaymentType
import com.rrparallelstudio.miapp.core.model.enums.LoanRepaymentMode
import com.rrparallelstudio.miapp.core.model.enums.LoanScheduleStatus
import com.rrparallelstudio.miapp.core.model.enums.LoanStatus
import com.rrparallelstudio.miapp.core.model.enums.LoanTopUpAdjustment
import com.rrparallelstudio.miapp.core.model.enums.LoanType
import kotlinx.coroutines.flow.Flow

interface LoanRepository {
    fun getAllLoans(): Flow<List<LoanEntity>>

    fun getLoansWithPayments(): Flow<List<LoanWithPayments>>

    fun getLoanWithPaymentsById(id: Long): Flow<LoanWithPayments?>

    fun getLoansByType(type: LoanType): Flow<List<LoanEntity>>

    fun getLoansByStatus(status: LoanStatus): Flow<List<LoanEntity>>

    fun getActiveLoans(): Flow<List<LoanEntity>>

    suspend fun getLoanById(id: Long): LoanEntity?

    fun getLoanByIdFlow(id: Long): Flow<LoanEntity?>

    suspend fun createLoan(loan: LoanEntity): Long

    suspend fun updateLoan(loan: LoanEntity)

    /**
     * Persists loan edits and, when [noPaymentsYet], realigns opening interest revision + disbursal ledger date.
     * Rebuilds fixed-EMI schedule when applicable.
     */
    suspend fun updateLoanFromEdit(
        updated: LoanEntity,
        previous: LoanEntity,
        noPaymentsYet: Boolean,
    )

    suspend fun updateLoanStatus(
        loanId: Long,
        status: LoanStatus,
    )

    suspend fun deleteLoan(loan: LoanEntity)

    fun getPaymentsForLoan(loanId: Long): Flow<List<LoanPaymentEntity>>

    suspend fun recordLoanPayment(
        loanId: Long,
        paymentAmount: Double,
        paymentType: LoanPaymentType = LoanPaymentType.PART_PAYMENT,
        paymentDate: Long = System.currentTimeMillis(),
        partPaymentAdjustment: LoanPartPaymentAdjustment? = null,
        principalComponent: Double? = null,
        interestComponent: Double? = null,
        transactionId: Long? = null,
        notes: String? = null,
    ): Long

    suspend fun recordLoanTopUp(
        loanId: Long,
        topUpAmount: Double,
        topUpDate: Long = System.currentTimeMillis(),
        topUpAdjustment: LoanTopUpAdjustment? = null,
        transactionId: Long? = null,
        notes: String? = null,
        postLedgerTransfer: Boolean = true,
    ): Long

    suspend fun changeInterestRate(
        loanId: Long,
        newAnnualRatePercent: Double,
        effectiveFromMillis: Long = System.currentTimeMillis(),
        note: String? = null,
    )

    suspend fun updateFutureEmi(
        loanId: Long,
        newEmiAmount: Double,
    )

    /** Rebuilds planned EMIs (e.g. after EMI day / tenure / rate changes). */

    /** Rebuilds planned EMIs from disbursal date, or last payment date if any exist. */
    suspend fun refreshPlannedEmiSchedule(loanId: Long)

    suspend fun changeRepaymentMode(
        loanId: Long,
        newMode: LoanRepaymentMode,
        fixedEmiOverride: Double? = null,
        tenureMonths: Int? = null,
    )

    fun getInterestRevisions(loanId: Long): Flow<List<LoanInterestRevisionEntity>>

    fun getScheduleEntries(
        loanId: Long,
        status: LoanScheduleStatus? = null,
    ): Flow<List<LoanScheduleEntryEntity>>

    fun getLoanSummary(): Flow<LoanSummaryAggregate>

    /** Rebuilds [NotificationRuleType.LOAN_DUE] rows for all active fixed-EMI loans. */
    suspend fun reconcileAllEmiReminderNotifications(now: Long = System.currentTimeMillis())
}
