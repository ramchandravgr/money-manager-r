package com.mmr.core.database.dao

import androidx.room.*
import com.mmr.core.database.entity.NotificationRuleEntity
import com.mmr.core.database.entity.RecurringRuleEntity
import com.mmr.core.database.relation.RecurringRuleWithDetails
import kotlinx.coroutines.flow.Flow

@Dao
interface RecurringRuleDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(rule: RecurringRuleEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRules(rules: List<RecurringRuleEntity>)

    @Update
    suspend fun update(rule: RecurringRuleEntity)

    @Delete
    suspend fun delete(rule: RecurringRuleEntity)

    @Query("SELECT * FROM recurring_rules WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): RecurringRuleEntity?

    @Query("SELECT * FROM recurring_rules WHERE syncId = :syncId LIMIT 1")
    suspend fun getBySyncId(syncId: String): RecurringRuleEntity?

    @Query("SELECT * FROM recurring_rules WHERE id = :id LIMIT 1")
    fun getByIdFlow(id: Long): Flow<RecurringRuleEntity?>

    @Transaction
    @Query("SELECT * FROM recurring_rules WHERE id = :id LIMIT 1")
    fun getRuleWithDetailsById(id: Long): Flow<RecurringRuleWithDetails?>

    @Transaction
    @Query("SELECT * FROM recurring_rules ORDER BY nextRunDate ASC")
    fun getAllRulesWithDetails(): Flow<List<RecurringRuleWithDetails>>

    @Query("SELECT * FROM recurring_rules ORDER BY nextRunDate ASC")
    fun getAllRules(): Flow<List<RecurringRuleEntity>>

    @Query("SELECT * FROM recurring_rules WHERE isEnabled = 1 ORDER BY nextRunDate ASC")
    fun getEnabledRules(): Flow<List<RecurringRuleEntity>>

    @Transaction
    @Query("SELECT * FROM recurring_rules WHERE isEnabled = 1 ORDER BY nextRunDate ASC")
    fun getEnabledRulesWithDetails(): Flow<List<RecurringRuleWithDetails>>

    @Query("SELECT * FROM recurring_rules WHERE isEnabled = 1 AND nextRunDate <= :now ORDER BY nextRunDate ASC")
    suspend fun getDueRules(now: Long = System.currentTimeMillis()): List<RecurringRuleEntity>

    @Query("UPDATE recurring_rules SET isEnabled = :isEnabled, updatedAt = :now WHERE id = :ruleId")
    suspend fun updateEnabledState(
        ruleId: Long,
        isEnabled: Boolean,
        now: Long = System.currentTimeMillis(),
    )

    @Query(
        "UPDATE recurring_rules SET nextRunDate = :nextRunDate, lastGeneratedDate = :lastGenDate, updatedAt = :now WHERE id = :ruleId",
    )
    suspend fun updateNextRunDate(
        ruleId: Long,
        nextRunDate: Long,
        lastGenDate: Long,
        now: Long = System.currentTimeMillis(),
    )

    @Query("SELECT COUNT(*) FROM recurring_rules WHERE isEnabled = 1")
    fun getEnabledRulesCount(): Flow<Int>

    @Query(
        """
        SELECT COUNT(*) FROM recurring_rules
        WHERE accountId = :accountId OR relatedAccountId = :accountId
        """,
    )
    suspend fun countInvolvingAccount(accountId: Long): Int

    @Query(
        """
        UPDATE recurring_rules
        SET accountId = :toAccountId, updatedAt = :now
        WHERE accountId = :fromAccountId
        """,
    )
    suspend fun reassignPrimaryAccount(
        fromAccountId: Long,
        toAccountId: Long,
        now: Long = System.currentTimeMillis(),
    )

    @Query(
        """
        UPDATE recurring_rules
        SET relatedAccountId = :toAccountId, updatedAt = :now
        WHERE relatedAccountId = :fromAccountId
        """,
    )
    suspend fun reassignRelatedAccount(
        fromAccountId: Long,
        toAccountId: Long,
        now: Long = System.currentTimeMillis(),
    )

    @Query(
        """
        UPDATE recurring_rules
        SET categoryId = :toCategoryId, subcategoryId = NULL, updatedAt = :now
        WHERE categoryId = :fromCategoryId
        """,
    )
    suspend fun migrateCategory(
        fromCategoryId: Long,
        toCategoryId: Long,
        now: Long = System.currentTimeMillis(),
    )

    @Query(
        """
        UPDATE recurring_rules
        SET categoryId = NULL, subcategoryId = NULL, updatedAt = :now
        WHERE categoryId = :categoryId
        """,
    )
    suspend fun clearCategory(
        categoryId: Long,
        now: Long = System.currentTimeMillis(),
    )
}

@Dao
interface NotificationRuleDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(rule: NotificationRuleEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRules(rules: List<NotificationRuleEntity>)

    @Update
    suspend fun update(rule: NotificationRuleEntity)

    @Delete
    suspend fun delete(rule: NotificationRuleEntity)

    @Query("SELECT * FROM notification_rules WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): NotificationRuleEntity?

    @Query("SELECT * FROM notification_rules WHERE syncId = :syncId LIMIT 1")
    suspend fun getBySyncId(syncId: String): NotificationRuleEntity?

    @Query("SELECT * FROM notification_rules WHERE enabled = 1 ORDER BY scheduleTime ASC")
    fun getEnabledRules(): Flow<List<NotificationRuleEntity>>

    @Query(
        "SELECT * FROM notification_rules WHERE enabled = 1 AND scheduleTime <= :now AND isFired = 0 ORDER BY scheduleTime ASC",
    )
    suspend fun getPendingNotifications(now: Long = System.currentTimeMillis()): List<NotificationRuleEntity>

    @Query("UPDATE notification_rules SET isFired = 1 WHERE id = :ruleId")
    suspend fun markAsFired(ruleId: Long)

    @Query("SELECT * FROM notification_rules WHERE targetRecurringRuleId = :recurringRuleId")
    suspend fun listForRecurringRule(recurringRuleId: Long): List<NotificationRuleEntity>

    @Query("SELECT * FROM notification_rules WHERE targetLoanId = :loanId")
    suspend fun listForLoan(loanId: Long): List<NotificationRuleEntity>

    @Query("DELETE FROM notification_rules WHERE targetRecurringRuleId = :recurringRuleId")
    suspend fun deleteForRecurringRule(recurringRuleId: Long)

    @Query("DELETE FROM notification_rules WHERE targetLoanId = :loanId")
    suspend fun deleteForLoan(loanId: Long)
}
