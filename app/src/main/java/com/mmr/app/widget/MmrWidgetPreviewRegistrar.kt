package com.mmr.app.widget

import android.appwidget.AppWidgetProviderInfo
import android.content.Context
import android.os.Build
import android.util.Log
import androidx.collection.intSetOf
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.setWidgetPreviews
import kotlin.reflect.KClass

/**
 * Publishes Glance-generated widget previews for the system picker (API 35+).
 * One UI uses these instead of inflating [previewLayout] for Glance widgets.
 */
object MmrWidgetPreviewRegistrar {
    suspend fun registerAll(context: Context) {
        if (Build.VERSION.SDK_INT < 35) return
        val manager = GlanceAppWidgetManager(context.applicationContext)
        val receivers: List<KClass<out GlanceAppWidgetReceiver>> =
            listOf(
                MmrMonthSpendWidgetReceiver::class,
                MmrBudgetsWidgetReceiver::class,
                MmrPendingApprovalsWidgetReceiver::class,
            )
        val homeOnly = intSetOf(AppWidgetProviderInfo.WIDGET_CATEGORY_HOME_SCREEN)
        receivers.forEach { receiver ->
            val result =
                runCatching { manager.setWidgetPreviews(receiver, homeOnly) }.getOrElse { return@forEach }
            when (result) {
                GlanceAppWidgetManager.SET_WIDGET_PREVIEWS_RESULT_SUCCESS -> Unit
                GlanceAppWidgetManager.SET_WIDGET_PREVIEWS_RESULT_RATE_LIMITED ->
                    Log.w(
                        TAG,
                        "Widget preview publish rate-limited for ${receiver.java.name}; " +
                            "picker may still use previewLayout until the limit resets (~2/h).",
                    )
            }
        }
    }

    private const val TAG = "MmrWidgetPreview"
}
