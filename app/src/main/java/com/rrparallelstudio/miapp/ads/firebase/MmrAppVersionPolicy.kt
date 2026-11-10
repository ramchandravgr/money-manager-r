package com.rrparallelstudio.miapp.ads.firebase

import android.content.Context
import com.rrparallelstudio.miapp.BuildConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Enforces Firebase Remote Config [MmrAdsRemoteConfig.KEY_MIN_VERSION_CODE].
 * When [mustUpdate] is true, block the main app and send the user to Play.
 */
@Singleton
class MmrAppVersionPolicy
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
        private val adsRemoteConfig: MmrAdsRemoteConfig,
    ) {
        private val _mustUpdate = MutableStateFlow(false)
        val mustUpdate: StateFlow<Boolean> = _mustUpdate.asStateFlow()

        fun applyCached() {
            evaluate(adsRemoteConfig.minVersionCodeCached())
        }

        fun refreshAfterRemoteConfig() {
            evaluate(adsRemoteConfig.minVersionCodeAfterFetch())
        }

        private fun evaluate(minRequired: Long) {
            val configured = MmrFirebaseAvailability.isConfigured(context)
            _mustUpdate.value =
                mustForceUpdateForVersion(
                    installedVersionCode = BuildConfig.VERSION_CODE,
                    minRequired = minRequired,
                    firebaseConfigured = configured,
                )
        }
    }

internal fun mustForceUpdateForVersion(
    installedVersionCode: Int,
    minRequired: Long,
    firebaseConfigured: Boolean,
): Boolean = firebaseConfigured && minRequired > 0L && installedVersionCode.toLong() < minRequired
