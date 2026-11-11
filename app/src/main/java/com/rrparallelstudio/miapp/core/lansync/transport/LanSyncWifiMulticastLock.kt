package com.rrparallelstudio.miapp.core.lansync.transport

import android.content.Context
import android.net.wifi.WifiManager
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * NSD / mDNS discovery often fails on Wi‑Fi unless a multicast lock is held
 * ([WifiManager.MulticastLock]).
 */
@Singleton
class LanSyncWifiMulticastLock
    @Inject
    constructor(
        @ApplicationContext context: Context,
    ) {
        private val lock: WifiManager.MulticastLock =
            (context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager)
                .createMulticastLock("mmr_lan_sync_mdns")
                .apply { setReferenceCounted(true) }

        fun acquire() {
            if (!lock.isHeld) {
                runCatching { lock.acquire() }
            }
        }

        fun release() {
            if (lock.isHeld) {
                runCatching { lock.release() }
            }
        }
    }
