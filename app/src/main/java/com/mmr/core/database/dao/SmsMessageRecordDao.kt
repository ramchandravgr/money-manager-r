package com.mmr.core.database.dao

import androidx.room.*
import com.mmr.core.database.entity.SmsMessageRecordEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SmsMessageRecordDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(record: SmsMessageRecordEntity): Long

    @Update
    suspend fun update(record: SmsMessageRecordEntity)

    @Query("SELECT * FROM sms_message_records WHERE approvalState = 'PENDING' ORDER BY receivedTime DESC")
    fun getPendingSmsRecords(): Flow<List<SmsMessageRecordEntity>>

    @Query("SELECT * FROM sms_message_records WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): SmsMessageRecordEntity?

    @Query("SELECT * FROM sms_message_records WHERE messageBodyHash = :hash LIMIT 1")
    suspend fun findByHash(hash: String): SmsMessageRecordEntity?

    @Query("SELECT EXISTS(SELECT 1 FROM sms_message_records WHERE messageBodyHash = :hash)")
    suspend fun isDuplicateHash(hash: String): Boolean
}
