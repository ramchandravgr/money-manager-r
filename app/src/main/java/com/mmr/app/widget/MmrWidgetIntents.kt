package com.mmr.app.widget

import android.content.Context
import com.mmr.app.navigation.Screen
import com.mmr.app.navigation.buildMainActivityNavIntent
import com.mmr.feature.approval.model.ApprovalWidgetLaunchAction

internal fun mainActivityIntent(
    context: Context,
    route: String,
) = buildMainActivityNavIntent(context, route)

internal fun approvalCenterIntent(
    context: Context,
    approvalId: Long = 0L,
    action: String = "",
) = mainActivityIntent(
    context,
    Screen.ApprovalCenter.createRoute(approvalId = approvalId, action = action),
)

internal fun homeIntent(context: Context) = mainActivityIntent(context, Screen.Home.route)

internal fun budgetsIntent(context: Context) = mainActivityIntent(context, Screen.Budgets.route)

internal val ApprovalWidgetLaunch = ApprovalWidgetLaunchAction
