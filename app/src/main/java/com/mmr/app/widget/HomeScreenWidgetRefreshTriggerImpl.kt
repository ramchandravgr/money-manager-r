package com.mmr.app.widget

import com.mmr.core.common.widget.HomeScreenWidgetRefreshTrigger
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HomeScreenWidgetRefreshTriggerImpl
    @Inject
    constructor(
        private val widgetRefresher: MmrWidgetRefresher,
    ) : HomeScreenWidgetRefreshTrigger {
        override fun requestRefresh() {
            widgetRefresher.refreshAll()
        }
    }
