package com.rrparallelstudio.miapp.ads.firebase

import android.content.Context
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.rrparallelstudio.miapp.BuildConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Firebase Crashlytics wiring: collection policy, session keys, and safe helpers for optional non-fatals.
 */
@Singleton
class MmrCrashlytics
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
    ) {
        private val configured = MmrFirebaseAvailability.isConfigured(context)

        fun isCollectionEnabled(): Boolean =
            if (!configured) {
                false
            } else {
                runCatching { FirebaseCrashlytics.getInstance().isCrashlyticsCollectionEnabled }.getOrDefault(false)
            }

        fun applyStartupPolicy() {
            if (!configured) return
            val enabled =
                MmrCrashlyticsPolicy.shouldCollectCrashReports(
                    firebaseConfigured = true,
                    isDebugBuild = BuildConfig.DEBUG,
                    crashlyticsDebugInDebugBuild = BuildConfig.CRASHLYTICS_DEBUG,
                )
            runCatching {
                FirebaseCrashlytics.getInstance().apply {
                    setCrashlyticsCollectionEnabled(enabled)
                    setCustomKey("version_name", BuildConfig.VERSION_NAME)
                    setCustomKey("version_code", BuildConfig.VERSION_CODE)
                    setCustomKey("build_type", if (BuildConfig.DEBUG) "debug" else "release")
                }
            }
        }

        fun log(message: String) {
            if (!configured || !isCollectionEnabled()) return
            runCatching { FirebaseCrashlytics.getInstance().log(message) }
        }

        fun recordException(throwable: Throwable) {
            if (!configured || !isCollectionEnabled()) return
            runCatching { FirebaseCrashlytics.getInstance().recordException(throwable) }
        }
    }
