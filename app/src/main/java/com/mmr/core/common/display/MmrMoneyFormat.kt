package com.mmr.core.common.display

import java.text.NumberFormat
import java.util.Locale

object MmrMoneyFormat {
    const val DEFAULT_CURRENCY_SYMBOL: String = "₹"

    fun numberLocale(locale: Locale = Locale.getDefault()): Locale = locale

    fun stripGroupingSeparators(input: String): String = input.replace(",", "").replace(" ", "").trim()

    fun formatInr(
        amount: Double,
        locale: Locale = Locale.getDefault(),
    ): String = format(amount, DEFAULT_CURRENCY_SYMBOL, locale)

    fun formatInrWithSpace(
        amount: Double,
        locale: Locale = Locale.getDefault(),
    ): String = formatWithSpace(amount, DEFAULT_CURRENCY_SYMBOL, locale)

    fun format(
        amount: Double,
        currencySymbol: String,
        locale: Locale = Locale.getDefault(),
    ): String {
        val formatter = amountFormatter(locale)
        return "$currencySymbol${formatter.format(amount)}"
    }

    /** `₹ 2,30,898.00` style with symbol and space (transaction list rows). */
    fun formatWithSpace(
        amount: Double,
        currencySymbol: String,
        locale: Locale = Locale.getDefault(),
    ): String = "${currencySymbol.trim()} ${amountFormatter(locale).format(amount)}"

    fun amountFormatter(locale: Locale = Locale.getDefault()): NumberFormat =
        NumberFormat.getNumberInstance(locale).apply {
            minimumFractionDigits = 2
            maximumFractionDigits = 2
        }

    fun formatNumber(
        amount: Double,
        locale: Locale = Locale.getDefault(),
    ): String = amountFormatter(locale).format(amount)
}
