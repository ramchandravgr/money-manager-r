package com.rrparallelstudio.miapp.core.common.math

import com.rrparallelstudio.miapp.core.model.enums.TransactionType
import java.math.BigDecimal
import java.math.RoundingMode

/** Stable money arithmetic for ledger amounts (2 decimal places, HALF_UP). */
object MmrAmountMath {
    private const val SCALE = 2

    data class NetIncomeExpenseResult(
        val amount: Double,
        val type: TransactionType,
    )

    /**
     * Net opposite cashflow types: higher side wins; equal totals → ₹0 expense.
     * Examples: expense 500 + income 600 → 100 income; 600 + 100 → 500 expense; 800 + 800 → 0 expense.
     */
    fun netIncomeAndExpense(
        expenseTotal: Double,
        incomeTotal: Double,
    ): NetIncomeExpenseResult {
        val expense = toMoney(expenseTotal)
        val income = toMoney(incomeTotal)
        return when (expense.compareTo(income)) {
            1 ->
                NetIncomeExpenseResult(
                    expense.subtract(income).toDouble(),
                    TransactionType.EXPENSE,
                )
            -1 ->
                NetIncomeExpenseResult(
                    income.subtract(expense).toDouble(),
                    TransactionType.INCOME,
                )
            else -> NetIncomeExpenseResult(0.0, TransactionType.EXPENSE)
        }
    }

    fun add(
        a: Double,
        b: Double,
    ): Double = toMoney(a).add(toMoney(b)).toDouble()

    fun sum(values: Iterable<Double>): Double =
        values.fold(BigDecimal.ZERO) { acc, value -> acc.add(toMoney(value)) }.toDouble()

    fun toMoney(value: Double): BigDecimal = BigDecimal.valueOf(value).setScale(SCALE, RoundingMode.HALF_UP)
}
