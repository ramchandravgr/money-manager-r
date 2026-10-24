package com.mmr.core.common.widget

/**
 * App-layer hook to refresh home screen Glance widgets after ledger or approval data changes.
 */
interface HomeScreenWidgetRefreshTrigger {
    fun requestRefresh()
}

object NoOpHomeScreenWidgetRefreshTrigger : HomeScreenWidgetRefreshTrigger {
    override fun requestRefresh() = Unit
}
