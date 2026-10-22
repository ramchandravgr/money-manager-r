package com.mmr.core.database.model

data class CategorySpending(
    val categoryId: Long?,
    val categoryName: String?,
    val colorHex: String?,
    val totalAmount: Double,
    val transactionCount: Int
)
