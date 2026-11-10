package com.rrparallelstudio.miapp.core.common.math

import com.rrparallelstudio.miapp.core.model.enums.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Test

class MmrAmountMathTest {
    @Test
    fun add_twoDecimals_noBinaryNoise() {
        assertEquals(29683.49, MmrAmountMath.add(13428.0, 16255.49), 0.0)
    }

    @Test
    fun sum_matchesAddChain() {
        assertEquals(29683.49, MmrAmountMath.sum(listOf(13428.0, 16255.49)), 0.0)
    }

    @Test
    fun netIncomeAndExpense_examples() {
        var net = MmrAmountMath.netIncomeAndExpense(expenseTotal = 500.0, incomeTotal = 600.0)
        assertEquals(100.0, net.amount, 0.0)
        assertEquals(TransactionType.INCOME, net.type)

        net = MmrAmountMath.netIncomeAndExpense(600.0, 100.0)
        assertEquals(500.0, net.amount, 0.0)
        assertEquals(TransactionType.EXPENSE, net.type)

        net = MmrAmountMath.netIncomeAndExpense(800.0, 800.0)
        assertEquals(0.0, net.amount, 0.0)
        assertEquals(TransactionType.EXPENSE, net.type)
    }
}
