package com.rrparallelstudio.miapp.core.lansync

import android.net.NetworkCapabilities
import android.net.NetworkRequest

/** Matches Wi‑Fi / hotspot LAN even when the default route is still cellular (no validated internet). */
object LanPeerSyncNetworkRequest {
    val localWifi: NetworkRequest =
        NetworkRequest
            .Builder()
            .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
            .removeCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()
}
