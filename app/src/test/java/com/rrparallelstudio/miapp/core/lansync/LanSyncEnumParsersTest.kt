package com.rrparallelstudio.miapp.core.lansync

import com.rrparallelstudio.miapp.core.model.enums.LoanStatus
import com.rrparallelstudio.miapp.core.model.enums.LoanType
import com.rrparallelstudio.miapp.core.model.enums.TransactionSourceType
import com.rrparallelstudio.miapp.core.model.enums.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Test

class LanSyncEnumParsersTest {
    @Test
    fun transactionSourceType_knownValue() {
        assertEquals(TransactionSourceType.MONEY_MANAGER, LanSyncEnumParsers.transactionSourceType("MONEY_MANAGER"))
    }

    @Test
    fun transactionSourceType_unknownFallsBackToManual() {
        assertEquals(TransactionSourceType.MANUAL, LanSyncEnumParsers.transactionSourceType("FUTURE_SOURCE"))
    }

    @Test
    fun transactionType_unknownFallsBackToExpense() {
        assertEquals(TransactionType.EXPENSE, LanSyncEnumParsers.transactionType("NOT_A_TYPE"))
    }

    @Test
    fun loanEnums_unknownFallBackToDefaults() {
        assertEquals(LoanType.BORROWED, LanSyncEnumParsers.loanType("???"))
        assertEquals(LoanStatus.ACTIVE, LanSyncEnumParsers.loanStatus("???"))
    }
}
