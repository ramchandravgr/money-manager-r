package com.rrparallelstudio.miapp.ads.firebase

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MmrCrashlyticsPolicyTest {
    @Test
    fun offWhenFirebaseNotConfigured() {
        assertFalse(
            MmrCrashlyticsPolicy.shouldCollectCrashReports(
                firebaseConfigured = false,
                isDebugBuild = false,
                crashlyticsDebugInDebugBuild = true,
            ),
        )
    }

    @Test
    fun onForReleaseWhenConfigured() {
        assertTrue(
            MmrCrashlyticsPolicy.shouldCollectCrashReports(
                firebaseConfigured = true,
                isDebugBuild = false,
                crashlyticsDebugInDebugBuild = false,
            ),
        )
    }

    @Test
    fun debugBuildOffUnlessExplicitOptIn() {
        assertFalse(
            MmrCrashlyticsPolicy.shouldCollectCrashReports(
                firebaseConfigured = true,
                isDebugBuild = true,
                crashlyticsDebugInDebugBuild = false,
            ),
        )
        assertTrue(
            MmrCrashlyticsPolicy.shouldCollectCrashReports(
                firebaseConfigured = true,
                isDebugBuild = true,
                crashlyticsDebugInDebugBuild = true,
            ),
        )
    }
}
