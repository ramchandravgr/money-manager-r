package com.mmr.app.widget

import android.content.Context
import androidx.glance.appwidget.updateAll
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MmrWidgetRefresher
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
    ) {
        private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

        fun refreshAll() {
            scope.launch {
                runCatching {
                    MmrPendingApprovalsWidget().updateAll(context)
                    MmrMonthSpendWidget().updateAll(context)
                }
            }
        }
    }
