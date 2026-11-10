package com.rrparallelstudio.miapp.ads

import com.rrparallelstudio.miapp.feature.home.HomeCarryOverInjector
import com.rrparallelstudio.miapp.feature.home.model.HomeViewTab
import com.rrparallelstudio.miapp.feature.home.model.TransactionGroupUi

data class HomeAdPlacementContext(
    val viewTab: HomeViewTab,
    val isSelectionMode: Boolean,
    val isLoading: Boolean,
    val errorMessage: String?,
    val groupedTransactions: List<TransactionGroupUi>,
)

data class StatsAdPlacementContext(
    val isLoading: Boolean,
    val errorMessage: String?,
    val tabHasData: Boolean,
)

object MmrAdPlacementPolicy {
    fun hasUserTransactions(groups: List<TransactionGroupUi>): Boolean =
        groups.any { group ->
            group.items.any { item -> !HomeCarryOverInjector.isCarryOverRow(item.id) }
        }

    fun homeDailyBanner(context: HomeAdPlacementContext): MmrAdPlacement? {
        if (!homeDailyAdsAllowed(context)) return null
        if (!hasUserTransactions(context.groupedTransactions)) return null
        return MmrAdPlacement.HOME_DAILY_BANNER
    }

    fun homeDailyMrec(context: HomeAdPlacementContext): MmrAdPlacement? {
        if (!homeDailyAdsAllowed(context)) return null
        if (hasUserTransactions(context.groupedTransactions)) return null
        return MmrAdPlacement.HOME_DAILY_MREC
    }

    fun statsNative(context: StatsAdPlacementContext): MmrAdPlacement? {
        if (context.isLoading || context.errorMessage != null) return null
        if (!context.tabHasData) return null
        return MmrAdPlacement.STATS_NATIVE
    }

    private fun homeDailyAdsAllowed(context: HomeAdPlacementContext): Boolean {
        if (context.viewTab != HomeViewTab.DAILY) return false
        if (context.isSelectionMode) return false
        if (context.isLoading) return false
        if (context.errorMessage != null) return false
        return true
    }
}
