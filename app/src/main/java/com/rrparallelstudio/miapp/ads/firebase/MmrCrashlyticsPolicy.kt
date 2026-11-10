package com.rrparallelstudio.miapp.ads.firebase

/**
 * When Firebase Crashlytics may collect reports.
 *
 * - **Release** with [firebaseConfigured]: always on (Play / internal testing).
 * - **Debug** with [firebaseConfigured]: only when [crashlyticsDebugInDebugBuild] (local `CRASHLYTICS_DEBUG=true`).
 * - No Firebase config: off (no `google-services.json` at build time).
 */
object MmrCrashlyticsPolicy {
    fun shouldCollectCrashReports(
        firebaseConfigured: Boolean,
        isDebugBuild: Boolean,
        crashlyticsDebugInDebugBuild: Boolean,
    ): Boolean {
        if (!firebaseConfigured) return false
        if (!isDebugBuild) return true
        return crashlyticsDebugInDebugBuild
    }
}
