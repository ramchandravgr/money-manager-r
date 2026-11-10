package com.rrparallelstudio.miapp.core.lansync

class LanSyncWifiNotTrustedException(
    val ssid: String,
) : IllegalStateException("Trust this Wi‑Fi network before syncing.")
