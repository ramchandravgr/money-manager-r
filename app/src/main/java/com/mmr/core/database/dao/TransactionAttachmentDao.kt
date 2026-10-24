package com.mmr.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.mmr.core.database.entity.TransactionAttachmentEntity

@Dao
interface TransactionAttachmentDao {
    @Query("SELECT * FROM transaction_attachments WHERE transactionId = :transactionId ORDER BY createdAt ASC")
    suspend fun getByTransactionId(transactionId: Long): List<TransactionAttachmentEntity>

    @Query("SELECT * FROM transaction_attachments WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): TransactionAttachmentEntity?

    @Query("SELECT * FROM transaction_attachments WHERE syncId = :syncId LIMIT 1")
    suspend fun getBySyncId(syncId: String): TransactionAttachmentEntity?

    @Query("SELECT * FROM transaction_attachments ORDER BY id ASC")
    suspend fun getAll(): List<TransactionAttachmentEntity>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(entity: TransactionAttachmentEntity): Long

    @Update
    suspend fun update(entity: TransactionAttachmentEntity)

    @Query("DELETE FROM transaction_attachments WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM transaction_attachments WHERE transactionId = :transactionId")
    suspend fun deleteByTransactionId(transactionId: Long)
}
