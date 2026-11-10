package com.rrparallelstudio.miapp.ads.firebase

import com.rrparallelstudio.miapp.ads.MmrAdCoordinator
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Applies cached Remote Config on startup and fetches from the network at most **once per local day**
 * (release). Re-open on a new calendar day triggers fetch via [MainActivity] lifecycle + cold start.
 */
@Singleton
class MmrFirebaseRemoteConfigSync
    @Inject
    constructor(
        private val adsRemoteConfig: MmrAdsRemoteConfig,
        private val adCoordinator: MmrAdCoordinator,
        private val appVersionPolicy: MmrAppVersionPolicy,
        private val dailyFetchStore: MmrRemoteConfigDailyFetchStore,
    ) {
        fun applyCached() {
            adCoordinator.applyCachedRemoteConfig()
            appVersionPolicy.applyCached()
        }

        suspend fun refreshFromNetworkIfDueToday() {
            if (!dailyFetchStore.isDueForFetchToday()) {
                return
            }
            dailyFetchStore.markFetchAttemptedToday()
            adsRemoteConfig.fetchAndActivate()
            adCoordinator.applyAfterRemoteFetch()
            appVersionPolicy.refreshAfterRemoteConfig()
        }
    }
