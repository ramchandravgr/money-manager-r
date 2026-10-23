package com.mmr.core.database.model

data class LoanSummaryAggregate(
    val totalBorrowedPrincipal: Double,
    val totalBorrowedOutstanding: Double,
    val totalLentPrincipal: Double,
    val totalLentOutstanding: Double,
    val activeLoansCount: Int,
)
