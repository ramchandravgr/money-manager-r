package com.rrparallelstudio.miapp.core.data.preferences

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Last successful LAN address per trusted peer (for hotspot / mDNS gaps).
 * Value is `host:port` (same format as manual peer entry).
 */
@Singleton
class LanSyncLastPeerAddressStore
    @Inject
    constructor(
        @ApplicationContext context: Context,
    ) {
        private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        fun get(deviceId: String): String? = prefs.getString(key(deviceId), null)?.takeIf { it.isNotBlank() }

        fun put(
            deviceId: String,
            host: String,
            port: Int,
        ) {
            if (deviceId.isBlank() || host.isBlank() || port <= 0) return
            prefs.edit().putString(key(deviceId), "$host:$port").apply()
        }

        fun remove(deviceId: String) {
            prefs.edit().remove(key(deviceId)).apply()
        }

        private fun key(deviceId: String) = "last_peer_$deviceId"

        private companion object {
            const val PREFS_NAME = "mmr_lan_sync_last_peer_addr"
        }
    }
