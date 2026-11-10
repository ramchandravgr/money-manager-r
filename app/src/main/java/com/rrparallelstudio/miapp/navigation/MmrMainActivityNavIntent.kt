package com.rrparallelstudio.miapp.navigation

import android.content.Context
import android.content.Intent
import com.rrparallelstudio.miapp.MainActivity

/** In-app only: widgets, notifications, and other same-UID launchers. */
fun buildMainActivityNavIntent(
    context: Context,
    route: String,
): Intent =
    Intent(context, MainActivity::class.java).apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        putExtra(MainActivity.EXTRA_NAV_ROUTE, route)
        MmrNavRouteIntentTrust.markInternalDelivery(this)
    }
