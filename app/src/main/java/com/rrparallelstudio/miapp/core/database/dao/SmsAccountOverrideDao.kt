package com.rrparallelstudio.miapp.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.rrparallelstudio.miapp.core.database.entity.SmsAccountOverrideEntity

@Dao
interface SmsAccountOverrideDao {
    @Query("SELECT * FROM sms_account_overrides")
    suspend fun getAll(): List<SmsAccountOverrideEntity>

    @Query(
        """
        SELECT * FROM sms_account_overrides
        WHERE templateId = :templateId AND smsLastFour = :smsLastFour
        LIMIT 1
        """,
    )
    suspend fun findByTemplate(
        templateId: String,
        smsLastFour: String,
    ): SmsAccountOverrideEntity?

    @Query(
        """
        SELECT * FROM sms_account_overrides
        WHERE instrument = :instrument AND smsLastFour = :smsLastFour
        ORDER BY updatedAt DESC
        LIMIT 1
        """,
    )
    suspend fun findByInstrument(
        instrument: String,
        smsLastFour: String,
    ): SmsAccountOverrideEntity?

    @Insert
    suspend fun insert(entity: SmsAccountOverrideEntity): Long

    @Update
    suspend fun update(entity: SmsAccountOverrideEntity)
}
