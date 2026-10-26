package com.mmr.core.data.catalog

import com.mmr.core.database.dao.CategoryDao
import com.mmr.core.database.entity.CategoryEntity
import com.mmr.core.model.enums.CategoryType
import com.mmr.core.model.enums.TransactionType

/** Default shipped category when SMS / approval has no category hint. */
object MmrUncategorisedCategory {
    const val NAME = "Uncategorised"

    fun categoryTypeFor(transactionType: TransactionType): CategoryType =
        when (transactionType) {
            TransactionType.INCOME -> CategoryType.INCOME
            TransactionType.EXPENSE,
            TransactionType.TRANSFER,
            TransactionType.LOAN_PAYMENT,
            TransactionType.ADJUSTMENT,
            -> CategoryType.EXPENSE
        }

    fun findIn(categories: List<CategoryEntity>, transactionType: TransactionType): CategoryEntity? {
        val type = categoryTypeFor(transactionType)
        return categories.find { it.type == type && it.name.equals(NAME, ignoreCase = true) }
    }

    suspend fun resolveId(
        categoryDao: CategoryDao,
        transactionType: TransactionType,
    ): Long? = findIn(categoryDao.getActiveCategoriesOnce(), transactionType)?.id
}
