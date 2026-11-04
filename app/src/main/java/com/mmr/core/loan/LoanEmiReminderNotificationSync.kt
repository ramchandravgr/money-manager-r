package com.mmr.core.loan

import com.mmr.core.common.display.MmrMoneyFormat
import com.mmr.core.database.dao.LoanDao
import com.mmr.core.database.dao.LoanScheduleDao
import com.mmr.core.database.dao.NotificationRuleDao
import com.mmr.core.database.entity.LoanEntity
import com.mmr.core.database.entity.NotificationRuleEntity
import com.mmr.core.lansync.LanSyncTombstoneRecorder
import com.mmr.core.lansync.SyncEntityType
import com.mmr.core.model.enums.LoanRepaymentMode
import com.mmr.core.model.enums.LoanStatus
import com.mmr.core.model.enums.LoanType
import com.mmr.core.model.enums.NotificationRuleType
import com.mmr.core.reminder.MmrReminderPresets

internal object LoanEmiReminderNotificationSync {
    suspend fun syncForLoan(
        loan: LoanEntity,
        loanScheduleDao: LoanScheduleDao,
        notificationRuleDao: NotificationRuleDao,
        tombstoneRecorder: LanSyncTombstoneRecorder,
        now: Long = System.currentTimeMillis(),
    ) {
        tombstoneUnfiredForLoan(loan.id, notificationRuleDao, tombstoneRecorder)
        notificationRuleDao.deleteUnfiredForLoan(loan.id)

        if (!shouldSchedule(loan)) return

        val nextEntry = loanScheduleDao.getNextPlannedAfter(loan.id, now) ?: return
        val dueAt = nextEntry.dueDateMillis
        if (dueAt <= now) return

        val days = loan.emiReminderDaysBefore
        if (!MmrReminderPresets.isValidLoanEmiReminderDays(days)) return

        val remindAt = dueAt - days * MmrReminderPresets.DAY_MILLIS
        val alreadyReminded =
            notificationRuleDao.listForLoan(loan.id).any { rule ->
                rule.isFired && (rule.scheduleTime ?: 0L) >= remindAt
            }
        if (alreadyReminded) return

        val scheduleTime = if (remindAt <= now) now else remindAt

        notificationRuleDao.insert(
            NotificationRuleEntity(
                type = NotificationRuleType.LOAN_DUE,
                enabled = true,
                scheduleTime = scheduleTime,
                targetLoanId = loan.id,
                thresholdValue = nextEntry.emiAmount,
                title = "EMI due soon: ${loan.name}",
                message =
                    "${MmrMoneyFormat.formatInr(nextEntry.emiAmount)} due on " +
                        formatDueDate(dueAt),
            ),
        )
    }

    suspend fun reconcileAllActiveLoans(
        loanDao: LoanDao,
        loanScheduleDao: LoanScheduleDao,
        notificationRuleDao: NotificationRuleDao,
        tombstoneRecorder: LanSyncTombstoneRecorder,
        now: Long = System.currentTimeMillis(),
    ) {
        val loans = loanDao.getActiveLoansList()
        for (loan in loans) {
            syncForLoan(loan, loanScheduleDao, notificationRuleDao, tombstoneRecorder, now)
        }
    }

    private fun shouldSchedule(loan: LoanEntity): Boolean {
        if (loan.status != LoanStatus.ACTIVE) return false
        if (loan.loanType != LoanType.BORROWED) return false
        if (loan.repaymentModeEnum() != LoanRepaymentMode.FIXED_EMI) return false
        if (loan.emiReminderDaysBefore <= 0) return false
        return true
    }

    private suspend fun tombstoneUnfiredForLoan(
        loanId: Long,
        notificationRuleDao: NotificationRuleDao,
        tombstoneRecorder: LanSyncTombstoneRecorder,
    ) {
        notificationRuleDao.listForLoan(loanId).filter { !it.isFired }.forEach { notification ->
            tombstoneRecorder.recordDeletion(notification.syncId, SyncEntityType.NOTIFICATION_RULE)
        }
    }

    private fun formatDueDate(dueAt: Long): String =
        java.text
            .SimpleDateFormat("dd MMM yyyy", java.util.Locale.getDefault())
            .format(java.util.Date(dueAt))
}
