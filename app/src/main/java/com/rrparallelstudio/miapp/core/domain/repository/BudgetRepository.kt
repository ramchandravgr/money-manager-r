package com.rrparallelstudio.miapp.core.domain.repository

import com.rrparallelstudio.miapp.core.database.entity.BudgetEntity
import com.rrparallelstudio.miapp.core.domain.model.BudgetProgressSnapshot
import kotlinx.coroutines.flow.Flow

interface BudgetRepository {
    fun observeActiveBudgetProgress(): Flow<List<BudgetProgressSnapshot>>

    suspend fun getById(id: Long): BudgetEntity?

    suspend fun upsert(entity: BudgetEntity): Long

    suspend fun delete(id: Long)

    suspend fun evaluateAlertsForTransaction(
        categoryId: Long?,
        subcategoryId: Long?,
        transactionMillis: Long,
    )
}
