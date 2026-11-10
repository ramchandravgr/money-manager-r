package com.rrparallelstudio.miapp.testing

import com.rrparallelstudio.miapp.core.common.display.MmrMoneyFormat

/**
 * Mirrors [com.rrparallelstudio.miapp.feature.home.TransactionRowMapping] amount labels for UI assertions.
 */
object MmrUiTestFormat {
    fun expenseAmountFormatted(amount: Double): String = MmrMoneyFormat.formatWithSpace(amount, "₹")

    fun incomeAmountFormatted(amount: Double): String = MmrMoneyFormat.formatWithSpace(amount, "₹")
}
