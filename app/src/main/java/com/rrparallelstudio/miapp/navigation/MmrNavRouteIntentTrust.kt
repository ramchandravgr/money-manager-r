package com.rrparallelstudio.miapp.navigation

import android.app.Activity
import android.content.Intent
import android.os.Build
import android.os.Process

/**
 * Ensures [com.rrparallelstudio.miapp.MainActivity.EXTRA_NAV_ROUTE] query params (approval actions, etc.)
 * are honored only for intents started by this app (widgets, notifications).
 */
object MmrNavRouteIntentTrust {
    /** Set only from in-app [com.rrparallelstudio.miapp.widget.mainActivityIntent] and notifiers. */
    const val EXTRA_INTERNAL_DELIVERY = "mmr_nav_internal_delivery"

    fun markInternalDelivery(intent: Intent) {
        intent.putExtra(EXTRA_INTERNAL_DELIVERY, true)
    }

    fun isTrustedDelivery(
        activity: Activity,
        intent: Intent?,
    ): Boolean {
        if (intent == null) return false
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            val launchedFromUid = activity.launchedFromUid
            if (launchedFromUid != Process.INVALID_UID && launchedFromUid != 0) {
                return launchedFromUid == activity.applicationInfo.uid
            }
        }
        return intent.getBooleanExtra(EXTRA_INTERNAL_DELIVERY, false)
    }
}
