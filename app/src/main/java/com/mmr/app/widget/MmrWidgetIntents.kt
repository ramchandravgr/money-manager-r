package com.mmr.app.widget

import android.content.Context
import android.content.Intent
import com.mmr.app.MainActivity
import com.mmr.app.navigation.Screen
import com.mmr.feature.approval.model.ApprovalWidgetLaunchAction

internal fun mainActivityIntent(
    context: Context,
    route: String,
): Intent =
    Intent(context, MainActivity::class.java).apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        putExtra(MainActivity.EXTRA_NAV_ROUTE, route)
    }

internal fun approvalCenterIntent(
    context: Context,
    approvalId: Long = 0L,
    action: String = "",
): Intent =
    mainActivityIntent(
        context,
        Screen.ApprovalCenter.createRoute(approvalId = approvalId, action = action),
    )

internal fun homeIntent(context: Context): Intent = mainActivityIntent(context, Screen.Home.route)

internal val ApprovalWidgetLaunch = ApprovalWidgetLaunchAction
