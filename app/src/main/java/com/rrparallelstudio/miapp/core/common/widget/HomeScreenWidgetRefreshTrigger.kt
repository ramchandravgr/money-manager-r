package com.rrparallelstudio.miapp.core.common.widget

/**
 * App-layer hook to refresh home screen Glance widgets after ledger or approval data changes.
 */
interface HomeScreenWidgetRefreshTrigger {
    /** Schedules a widget republish. Safe from short UI transactions. */
    fun requestRefresh()

    /**
     * Republishes every home-screen widget before returning.
     * Widget [androidx.glance.appwidget.action.ActionCallback]s must await this so the launcher
     * receives the new card before the click broadcast ends.
     */
    suspend fun refreshNow() {
        requestRefresh()
    }
}

object NoOpHomeScreenWidgetRefreshTrigger : HomeScreenWidgetRefreshTrigger {
    override fun requestRefresh() = Unit
}
