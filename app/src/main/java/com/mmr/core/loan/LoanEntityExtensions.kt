package com.mmr.core.loan

import com.mmr.core.database.entity.LoanEntity
import com.mmr.core.model.enums.LoanRepaymentMode

fun LoanEntity.repaymentModeEnum(): LoanRepaymentMode =
    repaymentMode?.let { runCatching { LoanRepaymentMode.valueOf(it) }.getOrNull() }
        ?: LoanRepaymentMode.FLEXIBLE

/** Funds disbursed / interest accrual start (persisted as [LoanEntity.openingDate]). */
fun LoanEntity.disbursalDateMillis(): Long = openingDate
