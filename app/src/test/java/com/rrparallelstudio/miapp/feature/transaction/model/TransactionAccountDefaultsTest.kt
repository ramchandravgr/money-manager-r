package com.rrparallelstudio.miapp.feature.transaction.model

import com.rrparallelstudio.miapp.core.model.enums.AccountType
import com.rrparallelstudio.miapp.core.model.enums.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Test

class TransactionAccountDefaultsTest {
    private val house = AccountUi(id = 1L, name = "House loan", type = AccountType.LOAN)
    private val savings = AccountUi(id = 2L, name = "Savings", type = AccountType.DEBIT)

    @Test
    fun expensePrefersDefaultOverAlphabeticalLoan() {
        assertEquals(
            savings.id,
            resolveInitialTransactionAccountId(
                currentSelectedId = null,
                accounts = listOf(house, savings),
                transactionType = TransactionType.EXPENSE,
                defaultAccountId = savings.id,
            ),
        )
    }

    @Test
    fun expenseSkipsLoanWhenNoDefaultFlag() {
        assertEquals(
            savings.id,
            resolveInitialTransactionAccountId(
                currentSelectedId = null,
                accounts = listOf(house, savings),
                transactionType = TransactionType.EXPENSE,
                defaultAccountId = null,
            ),
        )
    }
}
