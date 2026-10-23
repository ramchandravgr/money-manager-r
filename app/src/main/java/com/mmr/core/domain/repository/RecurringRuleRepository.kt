package com.mmr.core.domain.repository

import com.mmr.core.database.entity.NotificationRuleEntity
import com.mmr.core.database.entity.RecurringRuleEntity
import com.mmr.core.database.relation.RecurringRuleWithDetails
import kotlinx.coroutines.flow.Flow

interface RecurringRuleRepository {
    fun getAllRules(): Flow<List<RecurringRuleEntity>>

    fun getAllRulesWithDetails(): Flow<List<RecurringRuleWithDetails>>

    fun getEnabledRulesWithDetails(): Flow<List<RecurringRuleWithDetails>>

    fun getRuleWithDetailsById(id: Long): Flow<RecurringRuleWithDetails?>

    suspend fun getRuleById(id: Long): RecurringRuleEntity?

    fun getRuleByIdFlow(id: Long): Flow<RecurringRuleEntity?>

    fun getEnabledRulesCount(): Flow<Int>

    suspend fun createRecurringRule(rule: RecurringRuleEntity): Long

    suspend fun updateRecurringRule(rule: RecurringRuleEntity)

    suspend fun setRuleEnabled(
        ruleId: Long,
        isEnabled: Boolean,
    )

    suspend fun deleteRecurringRule(rule: RecurringRuleEntity)

    suspend fun advanceNextRunDate(ruleId: Long): Long

    suspend fun getDueRules(now: Long = System.currentTimeMillis()): List<RecurringRuleEntity>

    // Reminder / Notification operations
    fun getEnabledNotifications(): Flow<List<NotificationRuleEntity>>

    fun getUpcomingReminders(): Flow<List<com.mmr.core.database.model.UpcomingReminder>>

    suspend fun getPendingNotifications(now: Long = System.currentTimeMillis()): List<NotificationRuleEntity>

    suspend fun markNotificationAsFired(notificationId: Long)

    suspend fun createNotificationRule(notification: NotificationRuleEntity): Long
}
