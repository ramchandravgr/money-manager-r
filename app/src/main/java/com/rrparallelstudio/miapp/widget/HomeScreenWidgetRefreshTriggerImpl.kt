package com.rrparallelstudio.miapp.widget

import com.rrparallelstudio.miapp.core.common.widget.HomeScreenWidgetRefreshTrigger
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

        override suspend fun refreshNow() {
            widgetRefresher.refreshAllAndWait()
        }
    }
