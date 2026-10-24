package com.mmr.core.data.preferences

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LanSyncWifiTrustStore
    @Inject
    constructor(
        @ApplicationContext context: Context,
    ) {
        private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        fun isTrusted(ssid: String?): Boolean {
            val key = ssid?.trim()?.takeIf { it.isNotEmpty() } ?: return false
            return prefs.getStringSet(KEY_TRUSTED_SSIDS, emptySet())?.contains(key) == true
        }

        fun markTrusted(ssid: String) {
            val key = ssid.trim()
            if (key.isEmpty()) return
            val updated = prefs.getStringSet(KEY_TRUSTED_SSIDS, emptySet())?.toMutableSet() ?: mutableSetOf()
            updated.add(key)
            prefs.edit().putStringSet(KEY_TRUSTED_SSIDS, updated).apply()
        }

        fun trustedSsids(): Set<String> = prefs.getStringSet(KEY_TRUSTED_SSIDS, emptySet())?.toSet() ?: emptySet()

        fun replaceTrustedSsids(ssids: Set<String>) {
            val cleaned = ssids.map { it.trim() }.filter { it.isNotEmpty() }.toSet()
            prefs.edit().putStringSet(KEY_TRUSTED_SSIDS, cleaned).apply()
        }

        fun clearAll() {
            prefs.edit().remove(KEY_TRUSTED_SSIDS).apply()
        }

        private companion object {
            const val PREFS_NAME = "mmr_lan_sync_wifi_trust"
            const val KEY_TRUSTED_SSIDS = "trusted_ssids"
        }
    }
