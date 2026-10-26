package com.mmr.app.widget

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.util.Log
import androidx.glance.GlanceId
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.compose
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Pushes current ledger and approval data to home-screen widgets.
 *
 * Glance [androidx.glance.appwidget.updateAll] only enqueues a WorkManager session. That session
 * does not re-read [GlanceAppWidget.provideGlance] while it is already running, and the worker
 * often never runs before a widget broadcast ends. Composing [RemoteViews] here finishes before
 * the caller returns, so Approve, Reject, scan, and peer sync update the launcher immediately.
 */
@Singleton
class MmrWidgetRefresher
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
    ) {
        private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        private val publishMutex = Mutex()

        fun refreshAll() {
            scope.launch { refreshAllAndWait() }
        }

        suspend fun refreshAllAndWait() {
            publishMutex.withLock {
                val pendingIds =
                    resolveGlanceIds(
                        MmrPendingApprovalsWidget::class.java,
                        MmrPendingApprovalsWidgetReceiver::class.java,
                    )
                val monthIds =
                    resolveGlanceIds(
                        MmrMonthSpendWidget::class.java,
                        MmrMonthSpendWidgetReceiver::class.java,
                    )
                val budgetIds =
                    resolveGlanceIds(
                        MmrBudgetsWidget::class.java,
                        MmrBudgetsWidgetReceiver::class.java,
                    )
                withContext(Dispatchers.Main) {
                    publish(MmrPendingApprovalsWidget(), pendingIds)
                    publish(MmrMonthSpendWidget(), monthIds)
                    publish(MmrBudgetsWidget(), budgetIds)
                }
            }
        }

        private suspend fun resolveGlanceIds(
            widgetClass: Class<out GlanceAppWidget>,
            receiverClass: Class<out GlanceAppWidgetReceiver>,
        ): List<GlanceId> {
            val manager = GlanceAppWidgetManager(context)
            val mapped = runCatching { manager.getGlanceIds(widgetClass) }.getOrDefault(emptyList())
            if (mapped.isNotEmpty()) return mapped
            val appWidgetManager = AppWidgetManager.getInstance(context)
            return buildList {
                for (appWidgetId in appWidgetManager.getAppWidgetIds(ComponentName(context, receiverClass))) {
                    runCatching { manager.getGlanceIdBy(appWidgetId) }.getOrNull()?.let(::add)
                }
            }
        }

        private suspend fun publish(
            widget: GlanceAppWidget,
            glanceIds: List<GlanceId>,
        ) {
            if (glanceIds.isEmpty()) return
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val glanceManager = GlanceAppWidgetManager(context)
            glanceIds.forEach { glanceId ->
                runCatching {
                    val remoteViews = widget.compose(context, glanceId)
                    appWidgetManager.updateAppWidget(glanceManager.getAppWidgetId(glanceId), remoteViews)
                }.onFailure { error ->
                    Log.w(TAG, "Failed to publish ${widget.javaClass.simpleName}", error)
                }
            }
        }

        private companion object {
            const val TAG = "MmrWidgetRefresher"
        }
    }
