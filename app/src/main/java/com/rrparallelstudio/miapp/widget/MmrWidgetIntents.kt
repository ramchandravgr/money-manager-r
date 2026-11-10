package com.rrparallelstudio.miapp.widget

import android.content.Context
import com.rrparallelstudio.miapp.feature.approval.model.ApprovalWidgetLaunchAction
import com.rrparallelstudio.miapp.navigation.Screen
import com.rrparallelstudio.miapp.navigation.buildMainActivityNavIntent

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
