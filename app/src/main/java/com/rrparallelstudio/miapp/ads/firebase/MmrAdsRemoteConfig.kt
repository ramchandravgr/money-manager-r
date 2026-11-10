package com.rrparallelstudio.miapp.ads.firebase

import android.content.Context
import com.google.firebase.ktx.Firebase
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.ktx.remoteConfig
import com.google.firebase.remoteconfig.ktx.remoteConfigSettings
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MmrAdsRemoteConfig
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
    ) {
        private val remoteConfig: FirebaseRemoteConfig? =
            if (MmrFirebaseAvailability.isConfigured(context)) {
                Firebase.remoteConfig.apply {
                    setConfigSettingsAsync(
                        remoteConfigSettings {
                            minimumFetchIntervalInSeconds =
                                if (com.rrparallelstudio.miapp.BuildConfig.DEBUG) {
                                    0L
                                } else {
                                    REMOTE_CONFIG_MIN_FETCH_INTERVAL_SECONDS
                                }
                        },
                    )
                    setDefaultsAsync(
                        mapOf(
                            KEY_ADS_ENABLED to false,
                            KEY_MIN_VERSION_CODE to 0L,
                        ),
                    )
                }
            } else {
                null
            }

        fun adsEnabledCached(): Boolean = remoteConfig?.getBoolean(KEY_ADS_ENABLED) ?: false

        suspend fun fetchAndActivate(): Boolean {
            val config = remoteConfig ?: return false
            return runCatching {
                config.fetchAndActivate().await()
            }.getOrDefault(false)
        }

        fun adsEnabledAfterFetch(): Boolean = remoteConfig?.getBoolean(KEY_ADS_ENABLED) ?: false

        fun minVersionCodeCached(): Long = remoteConfig?.getLong(KEY_MIN_VERSION_CODE) ?: 0L

        fun minVersionCodeAfterFetch(): Long = remoteConfig?.getLong(KEY_MIN_VERSION_CODE) ?: 0L

        companion object {
            const val KEY_ADS_ENABLED = "ads_enabled"
            const val KEY_MIN_VERSION_CODE = "min_version_code"

            /** SDK-level cap; app also limits release fetches to once per local day. */
            const val REMOTE_CONFIG_MIN_FETCH_INTERVAL_SECONDS = 86_400L
        }
    }
