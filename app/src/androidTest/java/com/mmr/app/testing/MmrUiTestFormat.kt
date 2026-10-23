package com.mmr.app.testing

/**
 * Mirrors [com.mmr.feature.home.TransactionRowMapping] expense/income amount labels for UI assertions.
 */
object MmrUiTestFormat {
    fun expenseAmountFormatted(amount: Double): String = "-₹ ${"%.2f".format(amount)}"

    fun incomeAmountFormatted(amount: Double): String = "+₹ ${"%.2f".format(amount)}"
}
