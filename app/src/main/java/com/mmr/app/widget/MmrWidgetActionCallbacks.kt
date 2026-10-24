package com.mmr.app.widget

import android.content.Context
import androidx.glance.GlanceId
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.action.ActionCallback
import dagger.hilt.android.EntryPointAccessors

class MmrWidgetApproveActionCallback : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters,
    ) {
        val approvalId = parameters[MmrWidgetActionKeys.approvalId] ?: return
        EntryPointAccessors
            .fromApplication(context.applicationContext, MmrWidgetActionEntryPoint::class.java)
            .widgetActionExecutor()
            .approve(approvalId)
    }
}

class MmrWidgetRejectActionCallback : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters,
    ) {
        val approvalId = parameters[MmrWidgetActionKeys.approvalId] ?: return
        EntryPointAccessors
            .fromApplication(context.applicationContext, MmrWidgetActionEntryPoint::class.java)
            .widgetActionExecutor()
            .reject(approvalId)
    }
}

class MmrWidgetLaterActionCallback : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters,
    ) {
        val approvalId = parameters[MmrWidgetActionKeys.approvalId] ?: return
        EntryPointAccessors
            .fromApplication(context.applicationContext, MmrWidgetActionEntryPoint::class.java)
            .widgetActionExecutor()
            .defer(approvalId)
    }
}

class MmrWidgetScanActionCallback : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters,
    ) {
        EntryPointAccessors
            .fromApplication(context.applicationContext, MmrWidgetActionEntryPoint::class.java)
            .widgetActionExecutor()
            .scanInbox()
    }
}
