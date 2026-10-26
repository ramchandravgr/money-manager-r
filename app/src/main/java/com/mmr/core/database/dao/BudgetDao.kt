package com.mmr.core.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.mmr.core.database.entity.BudgetEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BudgetDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(entity: BudgetEntity): Long

    @Update
    suspend fun update(entity: BudgetEntity)

    @Delete
    suspend fun delete(entity: BudgetEntity)

    @Query("SELECT * FROM budgets WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): BudgetEntity?

    @Query("SELECT * FROM budgets WHERE isActive = 1 ORDER BY updatedAt DESC")
    fun observeActive(): Flow<List<BudgetEntity>>

    @Query("SELECT * FROM budgets WHERE isActive = 1 ORDER BY updatedAt DESC")
    suspend fun getActiveOnce(): List<BudgetEntity>

    @Query("SELECT * FROM budgets")
    suspend fun getAll(): List<BudgetEntity>

    @Query("SELECT * FROM budgets WHERE syncId = :syncId LIMIT 1")
    suspend fun getBySyncId(syncId: String): BudgetEntity?

    @Query("DELETE FROM budgets WHERE syncId = :syncId")
    suspend fun deleteBySyncId(syncId: String)

    @Query(
        """
        SELECT COALESCE(SUM(amount), 0)
        FROM transactions
        WHERE status = 'APPROVED'
          AND type = 'EXPENSE'
          AND dateTime BETWEEN :startMillis AND :endMillis
          AND categoryId = :categoryId
        """,
    )
    suspend fun sumExpenseForCategory(
        categoryId: Long,
        startMillis: Long,
        endMillis: Long,
    ): Double

    @Query(
        """
        SELECT COALESCE(SUM(amount), 0)
        FROM transactions
        WHERE status = 'APPROVED'
          AND type = 'EXPENSE'
          AND dateTime BETWEEN :startMillis AND :endMillis
          AND categoryId = :categoryId
          AND subcategoryId = :subcategoryId
        """,
    )
    suspend fun sumExpenseForSubcategory(
        categoryId: Long,
        subcategoryId: Long,
        startMillis: Long,
        endMillis: Long,
    ): Double
}
