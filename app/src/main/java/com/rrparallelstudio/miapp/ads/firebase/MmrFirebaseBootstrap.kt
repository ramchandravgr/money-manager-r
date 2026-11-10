package com.rrparallelstudio.miapp.ads.firebase

import android.content.Context
import com.rrparallelstudio.miapp.ads.MmrAdCoordinator
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MmrFirebaseBootstrap
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
        private val remoteConfigSync: MmrFirebaseRemoteConfigSync,
        private val adCoordinator: MmrAdCoordinator,
        private val crashlytics: MmrCrashlytics,
    ) {
        fun start(scope: CoroutineScope) {
            if (!MmrFirebaseAvailability.isConfigured(context)) {
                return
            }
            crashlytics.applyStartupPolicy()
            remoteConfigSync.applyCached()
            scope.launch {
                if (adCoordinator.adsEnabled.value) {
                    adCoordinator.ensureMobileAdsInitialized()
                }
                remoteConfigSync.refreshFromNetworkIfDueToday()
            }
        }
    }
