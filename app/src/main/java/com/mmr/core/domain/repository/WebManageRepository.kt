package com.mmr.core.domain.repository

import com.mmr.core.lansync.transport.LanSyncHttpConstants
import kotlinx.coroutines.flow.StateFlow

data class WebManageSessionState(
    val enabled: Boolean = false,
    val serverListening: Boolean = false,
    val onWifi: Boolean = false,
    val wifiSsid: String? = null,
    val localUrl: String? = null,
    val loopbackUrl: String? = null,
    val serverPort: Int = LanSyncHttpConstants.DEFAULT_PORT,
    val hasAccessPassword: Boolean = false,
    val pendingApprovalCount: Int = 0,
    val lanIpv4: String? = null,
)

interface WebManageRepository {
    val sessionState: StateFlow<WebManageSessionState>

    suspend fun refreshSessionState()

    fun setWebManageEnabled(enabled: Boolean)

    fun setAccessPassword(password: String)

    fun onPeerSyncStarting()
}
