package com.rrparallelstudio.miapp.navigation

import com.rrparallelstudio.miapp.feature.approval.model.ApprovalWidgetLaunchAction
import com.rrparallelstudio.miapp.navigation.Screen.AddTransaction
import com.rrparallelstudio.miapp.navigation.Screen.ApprovalCenter
import com.rrparallelstudio.miapp.navigation.Screen.Home
import com.rrparallelstudio.miapp.navigation.Screen.LanSync
import com.rrparallelstudio.miapp.navigation.Screen.WebManage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MmrExternalNavRouteAllowlistTest {
    @Test
    fun sanitize_allowsWidgetRoutes() {
        assertEquals(Home.route, MmrExternalNavRouteAllowlist.sanitize(Home.route))
        assertEquals(
            ApprovalCenter.createRoute(approvalId = 42L),
            MmrExternalNavRouteAllowlist.sanitize(ApprovalCenter.createRoute(approvalId = 42L)),
        )
        assertEquals(
            AddTransaction.createRoute(approvalId = 1L),
            MmrExternalNavRouteAllowlist.sanitize(AddTransaction.createRoute(approvalId = 1L)),
        )
        assertEquals(LanSync.route, MmrExternalNavRouteAllowlist.sanitize(LanSync.route))
        assertEquals(WebManage.route, MmrExternalNavRouteAllowlist.sanitize(WebManage.route))
    }

    @Test
    fun sanitize_rejectsArbitraryRoute() {
        assertNull(MmrExternalNavRouteAllowlist.sanitize("settings/../web_manage"))
        assertNull(MmrExternalNavRouteAllowlist.sanitize("evil_route"))
    }

    @Test
    fun sanitize_stripsSensitiveQueryWhenUntrusted() {
        val withAction =
            ApprovalCenter.createRoute(
                approvalId = 42L,
                action = ApprovalWidgetLaunchAction.LINK,
            )
        assertEquals(
            ApprovalCenter.route,
            MmrExternalNavRouteAllowlist.sanitize(
                withAction,
                trustSensitiveQueryParams = false,
            ),
        )
        assertEquals(
            AddTransaction.route,
            MmrExternalNavRouteAllowlist.sanitize(
                AddTransaction.createRoute(approvalId = 9L),
                trustSensitiveQueryParams = false,
            ),
        )
    }
}
