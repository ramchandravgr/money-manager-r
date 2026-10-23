package com.mmr.core.lansync

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LanSyncNetworkUtils
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
    ) {
        fun isOnWifi(): Boolean {
            val connectivity = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val network = connectivity.activeNetwork ?: return false
            val capabilities = connectivity.getNetworkCapabilities(network) ?: return false
            return capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
        }
    }
