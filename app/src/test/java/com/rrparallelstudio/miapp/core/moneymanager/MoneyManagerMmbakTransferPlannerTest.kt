package com.rrparallelstudio.miapp.core.moneymanager

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MoneyManagerMmbakTransferPlannerTest {
    @Test
    fun plan_pairsTransferLegsByTxUidTrans() {
        val rows =
            listOf(
                leg("a-3", "grp-1", 3, "from", "to", 1_000L),
                leg("a-4", "grp-1", 4, "to", "from", 1_000L),
                expense("e-1", 1_000L),
            )
        val plan = MoneyManagerMmbakTransferPlanner.plan(rows)
        assertEquals(1, plan.incomeExpense.size)
        assertEquals(1, plan.transfers.size)
        assertEquals(0, plan.unsupportedRowKeys.size)
        val transfer = plan.transfers.single()
        assertEquals("mmbak:tx:grp-1", transfer.sourceReferenceId)
        assertEquals("from", transfer.fromAssetUid)
        assertEquals("to", transfer.toAssetUid)
        assertEquals(2, transfer.legRowKeys.size)
    }

    @Test
    fun plan_singleTransferLegWhenOnlyOneSidePresent() {
        val rows = listOf(leg("only-3", null, 3, "a", "b", 500L))
        val plan = MoneyManagerMmbakTransferPlanner.plan(rows)
        assertEquals(1, plan.transfers.size)
        assertEquals("mmbak:transfer:only-3", plan.transfers.single().sourceReferenceId)
    }

    @Test
    fun plan_marksUnknownDoTypeUnsupported() {
        val rows = listOf(expense("x", 1L).copy(doType = 9))
        val plan = MoneyManagerMmbakTransferPlanner.plan(rows)
        assertTrue(plan.unsupportedRowKeys.contains("x"))
        assertEquals(0, plan.incomeExpense.size)
    }

    private fun expense(
        key: String,
        amount: Double,
    ): MoneyManagerMmbakDatabaseOpener.MoneyManagerTransactionRow =
        MoneyManagerMmbakDatabaseOpener.MoneyManagerTransactionRow(
            rowKey = key,
            dateTimeMillis = 1_700_000_000_000L,
            amount = amount,
            doType = 1,
            assetUid = "1",
            categoryUid = null,
            note = "note",
        )

    private fun leg(
        key: String,
        group: String?,
        doType: Int,
        from: String,
        to: String,
        amount: Double,
    ): MoneyManagerMmbakDatabaseOpener.MoneyManagerTransactionRow =
        MoneyManagerMmbakDatabaseOpener.MoneyManagerTransactionRow(
            rowKey = key,
            dateTimeMillis = 1_700_000_000_000L,
            amount = amount,
            doType = doType,
            assetUid = from,
            categoryUid = null,
            note = "xfer",
            toAssetUid = to,
            transferGroupId = group,
        )
}
