package com.rrparallelstudio.miapp.ads

import com.rrparallelstudio.miapp.feature.home.HomeCarryOverInjector
import com.rrparallelstudio.miapp.feature.home.model.HomeViewTab
import com.rrparallelstudio.miapp.feature.home.model.TransactionGroupUi
import com.rrparallelstudio.miapp.feature.home.model.TransactionRowUi
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test

class MmrAdPlacementPolicyTest {
    private fun context(
        tab: HomeViewTab = HomeViewTab.DAILY,
        groups: List<TransactionGroupUi> = emptyList(),
        selection: Boolean = false,
        loading: Boolean = false,
        error: String? = null,
    ) = HomeAdPlacementContext(
        viewTab = tab,
        isSelectionMode = selection,
        isLoading = loading,
        errorMessage = error,
        groupedTransactions = groups,
    )

    private fun carryOnlyGroup(): List<TransactionGroupUi> =
        listOf(
            TransactionGroupUi(
                dateKey = "2025-11-01",
                dayOfMonth = 1,
                weekdayShort = "Sat",
                monthYearLabel = "Nov 2025",
                items = listOf(HomeCarryOverInjector.rowForAmount(100.0)),
                dayExpenseTotal = 0.0,
                dayIncomeTotal = 0.0,
            ),
        )

    private fun userTxnGroup(): List<TransactionGroupUi> =
        listOf(
            TransactionGroupUi(
                dateKey = "2025-11-08",
                dayOfMonth = 8,
                weekdayShort = "Sat",
                monthYearLabel = "Nov 2025",
                items =
                    listOf(
                        TransactionRowUi(
                            id = 1L,
                            title = "Food",
                            categoryLabel = "Food",
                            detailLine = "",
                            subtitle = "",
                            amountLabel = "₹10",
                            amountFormatted = "₹10",
                            isIncome = false,
                            timeLabel = "",
                        ),
                    ),
                dayExpenseTotal = 10.0,
                dayIncomeTotal = 0.0,
            ),
        )

    @Test
    fun carryOverOnly_isNotUserTransaction() {
        assertFalse(MmrAdPlacementPolicy.hasUserTransactions(carryOnlyGroup()))
    }

    @Test
    fun daily_empty_getsMrec_notBanner() {
        val ctx = context(groups = emptyList())
        assertEquals(MmrAdPlacement.HOME_DAILY_MREC, MmrAdPlacementPolicy.homeDailyMrec(ctx))
        assertNull(MmrAdPlacementPolicy.homeDailyBanner(ctx))
    }

    @Test
    fun daily_carryOnly_getsMrec() {
        val ctx = context(groups = carryOnlyGroup())
        assertEquals(MmrAdPlacement.HOME_DAILY_MREC, MmrAdPlacementPolicy.homeDailyMrec(ctx))
        assertNull(MmrAdPlacementPolicy.homeDailyBanner(ctx))
    }

    @Test
    fun daily_withTxn_getsBanner() {
        val ctx = context(groups = userTxnGroup())
        assertEquals(MmrAdPlacement.HOME_DAILY_BANNER, MmrAdPlacementPolicy.homeDailyBanner(ctx))
        assertNull(MmrAdPlacementPolicy.homeDailyMrec(ctx))
    }

    @Test
    fun weekly_neitherSlot() {
        val ctx = context(tab = HomeViewTab.MONTHLY, groups = userTxnGroup())
        assertNull(MmrAdPlacementPolicy.homeDailyBanner(ctx))
        assertNull(MmrAdPlacementPolicy.homeDailyMrec(ctx))
    }

    @Test
    fun selectionMode_hidesBanner() {
        val ctx = context(groups = userTxnGroup(), selection = true)
        assertNull(MmrAdPlacementPolicy.homeDailyBanner(ctx))
    }

    @Test
    fun stats_withData_eligible() {
        val ctx =
            StatsAdPlacementContext(
                isLoading = false,
                errorMessage = null,
                tabHasData = true,
            )
        assertEquals(MmrAdPlacement.STATS_NATIVE, MmrAdPlacementPolicy.statsNative(ctx))
    }
}
