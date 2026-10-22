package com.mmr.core.data.repository

import androidx.room.withTransaction
import com.mmr.core.database.MmrDatabase
import com.mmr.core.database.dao.NotificationRuleDao
import com.mmr.core.database.dao.RecurringRuleDao
import com.mmr.core.database.entity.NotificationRuleEntity
import com.mmr.core.database.entity.RecurringRuleEntity
import com.mmr.core.database.relation.RecurringRuleWithDetails
import com.mmr.core.domain.repository.RecurringRuleRepository
import com.mmr.core.domain.util.RecurrenceCalculator
import com.mmr.core.model.enums.NotificationRuleType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RecurringRuleRepositoryImpl @Inject constructor(
    private val database: MmrDatabase,
    private val recurringRuleDao: RecurringRuleDao,
    private val notificationRuleDao: NotificationRuleDao
) : RecurringRuleRepository {

    override fun getAllRules(): Flow<List<RecurringRuleEntity>> =
        recurringRuleDao.getAllRules()

    override fun getAllRulesWithDetails(): Flow<List<RecurringRuleWithDetails>> =
        recurringRuleDao.getAllRulesWithDetails()

    override fun getEnabledRulesWithDetails(): Flow<List<RecurringRuleWithDetails>> =
        recurringRuleDao.getEnabledRulesWithDetails()

    override fun getRuleWithDetailsById(id: Long): Flow<RecurringRuleWithDetails?> =
        if (id <= 0L) flowOf(null) else recurringRuleDao.getRuleWithDetailsById(id)

    override suspend fun getRuleById(id: Long): RecurringRuleEntity? =
        if (id <= 0L) null else recurringRuleDao.getById(id)

    override fun getRuleByIdFlow(id: Long): Flow<RecurringRuleEntity?> =
        if (id <= 0L) flowOf(null) else recurringRuleDao.getByIdFlow(id)

    override fun getEnabledRulesCount(): Flow<Int> =
        recurringRuleDao.getEnabledRulesCount()

    override suspend fun createRecurringRule(rule: RecurringRuleEntity): Long {
        RepositoryGuards.requireValidRecurringRule(rule)
        return database.withTransaction {
            val ruleId = recurringRuleDao.insert(rule)
            syncReminderNotification(rule.copy(id = ruleId))
            ruleId
        }
    }

    override suspend fun updateRecurringRule(rule: RecurringRuleEntity) {
        require(rule.id > 0L) { "Recurring rule id is required." }
        RepositoryGuards.requireValidRecurringRule(rule)
        database.withTransaction {
            recurringRuleDao.update(rule)
            notificationRuleDao.deleteForRecurringRule(rule.id)
            syncReminderNotification(rule)
        }
    }

    override suspend fun setRuleEnabled(ruleId: Long, isEnabled: Boolean) {
        if (ruleId <= 0L) return
        database.withTransaction {
            recurringRuleDao.updateEnabledState(ruleId, isEnabled)
            val rule = recurringRuleDao.getById(ruleId) ?: return@withTransaction
            notificationRuleDao.deleteForRecurringRule(ruleId)
            if (isEnabled) {
                syncReminderNotification(rule)
            }
        }
    }

    override suspend fun deleteRecurringRule(rule: RecurringRuleEntity) {
        require(rule.id > 0L) { "Recurring rule id is required." }
        database.withTransaction {
            notificationRuleDao.deleteForRecurringRule(rule.id)
            recurringRuleDao.delete(rule)
        }
    }

    override suspend fun advanceNextRunDate(ruleId: Long): Long {
        if (ruleId <= 0L) {
            throw IllegalArgumentException("Rule id is required.")
        }
        return database.withTransaction {
            val rule = recurringRuleDao.getById(ruleId)
                ?: throw IllegalArgumentException("Rule with id $ruleId not found")

            val currentRunDate = rule.nextRunDate
            val nextRun = RecurrenceCalculator.calculateNextRunDate(
                fromTimestamp = currentRunDate,
                interval = rule.recurrenceInterval,
                targetDayOfMonth = rule.dayOfMonth
            )

            if (RecurrenceCalculator.isExpired(nextRun, rule.endDate)) {
                recurringRuleDao.updateEnabledState(ruleId, false)
                notificationRuleDao.deleteForRecurringRule(ruleId)
            } else {
                recurringRuleDao.updateNextRunDate(ruleId, nextRun, currentRunDate)
                notificationRuleDao.deleteForRecurringRule(ruleId)
                syncReminderNotification(rule.copy(nextRunDate = nextRun))
            }

            nextRun
        }
    }

    override suspend fun getDueRules(now: Long): List<RecurringRuleEntity> =
        recurringRuleDao.getDueRules(now)

    override fun getEnabledNotifications(): Flow<List<NotificationRuleEntity>> =
        notificationRuleDao.getEnabledRules()

    override fun getUpcomingReminders(): Flow<List<com.mmr.core.database.model.UpcomingReminder>> {
        val dateFormat = java.text.SimpleDateFormat("dd MMM yyyy, hh:mm a", java.util.Locale.getDefault())
        return notificationRuleDao.getEnabledRules().map { list ->
            list.map { notif ->
                com.mmr.core.database.model.UpcomingReminder(
                    notificationId = notif.id,
                    ruleId = notif.targetRecurringRuleId,
                    title = notif.title ?: "Reminder",
                    message = notif.message ?: "",
                    scheduleTime = notif.scheduleTime ?: 0L,
                    formattedScheduleDate = notif.scheduleTime?.let { dateFormat.format(java.util.Date(it)) } ?: "",
                    amountFormatted = notif.thresholdValue?.let { "₹${"%.2f".format(it)}" }
                )
            }
        }
    }

    override suspend fun getPendingNotifications(now: Long): List<NotificationRuleEntity> =
        notificationRuleDao.getPendingNotifications(now)

    override suspend fun markNotificationAsFired(notificationId: Long) {
        if (notificationId <= 0L) return
        notificationRuleDao.markAsFired(notificationId)
    }

    override suspend fun createNotificationRule(notification: NotificationRuleEntity): Long =
        notificationRuleDao.insert(notification)

    private suspend fun syncReminderNotification(rule: RecurringRuleEntity) {
        if (!rule.isEnabled || rule.reminderMinutesBefore <= 0 || rule.id <= 0L) return
        val scheduleTime = RecurrenceCalculator.calculateReminderScheduleTime(
            rule.nextRunDate,
            rule.reminderMinutesBefore
        )
        val notification = NotificationRuleEntity(
            type = NotificationRuleType.RECURRING_REMINDER,
            enabled = true,
            scheduleTime = scheduleTime,
            targetRecurringRuleId = rule.id,
            title = "Upcoming: ${rule.name}",
            message = "₹${"%.2f".format(rule.amount)} due soon"
        )
        notificationRuleDao.insert(notification)
    }
}
