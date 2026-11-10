package com.rrparallelstudio.miapp.ads

import android.content.Context
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.RequestConfiguration
import com.rrparallelstudio.miapp.ads.firebase.MmrAdsRemoteConfig
import com.rrparallelstudio.miapp.ads.firebase.MmrFirebaseAvailability
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

@Singleton
class MmrAdCoordinator
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
        private val adsRemoteConfig: MmrAdsRemoteConfig,
    ) {
        private val initMutex = Mutex()
        private var mobileAdsInitialized = false

        private val _adsEnabled = MutableStateFlow(false)
        val adsEnabled: StateFlow<Boolean> = _adsEnabled.asStateFlow()

        suspend fun applyAfterRemoteFetch() {
            if (!MmrFirebaseAvailability.isConfigured(context)) {
                _adsEnabled.value = false
                return
            }
            val enabled = adsRemoteConfig.adsEnabledAfterFetch()
            _adsEnabled.value = enabled
            if (enabled) {
                ensureMobileAdsInitialized()
            }
        }

        fun applyCachedRemoteConfig() {
            _adsEnabled.value =
                MmrFirebaseAvailability.isConfigured(context) &&
                adsRemoteConfig.adsEnabledCached()
        }

        suspend fun ensureMobileAdsInitialized() {
            initMutex.withLock {
                if (mobileAdsInitialized) return
                suspendCoroutine { cont ->
                    MobileAds.initialize(context) {
                        mobileAdsInitialized = true
                        cont.resume(Unit)
                    }
                }
                val requestConfig =
                    RequestConfiguration
                        .Builder()
                        .setMaxAdContentRating(RequestConfiguration.MAX_AD_CONTENT_RATING_T)
                        .build()
                MobileAds.setRequestConfiguration(requestConfig)
            }
        }
    }
