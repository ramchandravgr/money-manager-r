package com.mmr.core.loan

import com.mmr.core.database.dao.LoanDao
import com.mmr.core.database.dao.LoanScheduleDao
import com.mmr.core.database.dao.TransactionDao
import com.mmr.core.database.entity.LoanScheduleEntryEntity
import com.mmr.core.loan.LoanLedgerCoordinator.Companion.scheduleSourceRef
import com.mmr.core.loan.repaymentModeEnum
import com.mmr.core.model.enums.LoanRepaymentMode
import com.mmr.core.model.enums.LoanStatus
import com.mmr.core.model.enums.LoanType
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EmiAutoPostCoordinator
    @Inject
    constructor(
        private val loanScheduleDao: LoanScheduleDao,
        private val loanDao: LoanDao,
        private val transactionDao: TransactionDao,
        private val loanLedgerCoordinator: LoanLedgerCoordinator,
    ) {
        /**
         * Creates silent ledger transfers for planned EMIs due on or before [upToMillis]
         * when no transaction exists yet for that schedule row.
         */
        suspend fun postDueEmiLedgerEntries(upToMillis: Long = System.currentTimeMillis()): Int {
            val dueEntries = loanScheduleDao.getPlannedDueUntil(upToMillis)
            var created = 0
            for (entry in dueEntries) {
                if (createIfMissing(entry)) created++
            }
            return created
        }

        private suspend fun createIfMissing(entry: LoanScheduleEntryEntity): Boolean {
            val ref = scheduleSourceRef(entry.id)
            if (transactionDao.findIdBySourceReference(ref) != null) return false

            val loan = loanDao.getById(entry.loanId) ?: return false
            if (loan.status != LoanStatus.ACTIVE) return false
            if (loan.loanType != LoanType.BORROWED) return false
            if (loan.repaymentModeEnum() != LoanRepaymentMode.FIXED_EMI) return false
            if (loan.linkedLoanAccountId == null) return false

            loanLedgerCoordinator.createRepaymentTransaction(
                loan = loan,
                amount = entry.emiAmount,
                dateTime = entry.dueDateMillis,
                note = "EMI (auto): ${loan.name}",
                scheduleEntryId = entry.id,
            )
            return true
        }
    }
