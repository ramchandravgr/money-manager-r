package com.mmr.core.domain.repository

import com.mmr.core.database.entity.PendingLanPairRequestEntity
import com.mmr.core.database.entity.TrustedLanDeviceEntity
import com.mmr.core.lansync.LanSyncMergeSummary
import com.mmr.core.lansync.LanSyncPairingOffer
import com.mmr.core.lansync.transport.DiscoveredLanPeer
import kotlinx.coroutines.flow.StateFlow

data class LanSyncSessionState(
    val enabled: Boolean = false,
    val deviceId: String = "",
    val displayName: String = "",
    val pairingOfferText: String = "",
    val trustedDevices: List<TrustedLanDeviceEntity> = emptyList(),
    val pendingPairRequests: List<PendingLanPairRequestEntity> = emptyList(),
    val discoveredPeers: List<DiscoveredLanPeer> = emptyList(),
    val onWifi: Boolean = false,
)

interface LanSyncRepository {
    val sessionState: StateFlow<LanSyncSessionState>

    suspend fun refreshSessionState()

    fun setLanSyncEnabled(enabled: Boolean)

    fun setDisplayName(name: String)

    fun pairingOffer(): LanSyncPairingOffer

    suspend fun trustDeviceFromPairingOffer(offer: LanSyncPairingOffer)

    suspend fun approvePairRequest(deviceId: String)

    suspend fun rejectPairRequest(deviceId: String)

    suspend fun removeTrustedDevice(deviceId: String)

    suspend fun syncNow(): LanSyncMergeSummary
}
