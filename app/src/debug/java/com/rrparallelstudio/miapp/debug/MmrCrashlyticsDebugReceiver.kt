package com.rrparallelstudio.miapp.debug

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.rrparallelstudio.miapp.ads.firebase.MmrCrashlytics
import com.rrparallelstudio.miapp.ads.firebase.MmrFirebaseAvailability

/**
 * Debug-only: force a crash to verify Crashlytics end-to-end.
 * Requires `google-services.json`, `CRASHLYTICS_DEBUG=true`, and a debug install.
 */
class MmrCrashlyticsDebugReceiver : BroadcastReceiver() {
    override fun onReceive(
        context: Context,
        intent: Intent?,
    ) {
        if (intent?.action != ACTION) return
        if (!MmrFirebaseAvailability.isConfigured(context)) {
            error("Firebase not configured — add app/google-services.json and rebuild")
        }
        val crashlytics = MmrCrashlytics(context)
        crashlytics.applyStartupPolicy()
        if (!crashlytics.isCollectionEnabled()) {
            error(
                "Crashlytics collection disabled — set CRASHLYTICS_DEBUG=true in local.properties, rebuild, or use a release build",
            )
        }
        crashlytics.log("crashlytics_verification_requested")
        throw RuntimeException("Money Inbox Crashlytics verification crash (safe to ignore)")
    }

    companion object {
        const val ACTION = "com.rrparallelstudio.miapp.action.CRASHLYTICS_TEST_CRASH"
    }
}
