package com.mmr.app.testing

import com.mmr.core.common.display.MmrMoneyFormat

/**
 * Mirrors [com.mmr.feature.home.TransactionRowMapping] amount labels for UI assertions.
 */
object MmrUiTestFormat {
    fun expenseAmountFormatted(amount: Double): String = MmrMoneyFormat.formatWithSpace(amount, "₹")

    fun incomeAmountFormatted(amount: Double): String = MmrMoneyFormat.formatWithSpace(amount, "₹")
}
