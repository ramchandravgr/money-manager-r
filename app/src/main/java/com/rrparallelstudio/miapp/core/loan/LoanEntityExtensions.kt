package com.rrparallelstudio.miapp.core.loan

import com.rrparallelstudio.miapp.core.database.entity.LoanEntity
import com.rrparallelstudio.miapp.core.model.enums.LoanRepaymentMode

fun LoanEntity.repaymentModeEnum(): LoanRepaymentMode =
    repaymentMode?.let { runCatching { LoanRepaymentMode.valueOf(it) }.getOrNull() }
        ?: LoanRepaymentMode.FLEXIBLE

/** Funds disbursed / interest accrual start (persisted as [LoanEntity.openingDate]). */
fun LoanEntity.disbursalDateMillis(): Long = openingDate
