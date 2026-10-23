package com.mmr.core.domain.model

data class SmsMergeCandidate(
    val transactionId: Long,
    val amount: Double,
    val dateTimeMillis: Long,
    val note: String?,
)
