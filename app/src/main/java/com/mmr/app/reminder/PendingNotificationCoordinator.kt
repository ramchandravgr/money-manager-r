package com.mmr.app.reminder

import com.mmr.core.domain.repository.AssetRepository
import com.mmr.core.domain.repository.LoanRepository
import com.mmr.core.domain.repository.RecurringRuleRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PendingNotificationCoordinator
    @Inject
    constructor(
        private val recurringRuleRepository: RecurringRuleRepository,
        private val loanRepository: LoanRepository,
        private val assetRepository: AssetRepository,
        private val notifier: ScheduledReminderNotifier,
    ) {
        suspend fun reconcileAndDispatch(now: Long = System.currentTimeMillis()) {
            loanRepository.reconcileAllEmiReminderNotifications(now)
            assetRepository.reconcileAllAssetValuationReminders()
            val pending = recurringRuleRepository.getPendingNotifications(now)
            for (rule in pending) {
                notifier.show(rule)
                recurringRuleRepository.markNotificationAsFired(rule.id)
            }
        }
    }
