package com.mmr.core.domain.repository

import com.mmr.core.data.preferences.LanSyncPeerMode
import com.mmr.core.database.entity.LanSyncLogEntryEntity
import com.mmr.core.database.entity.TrustedLanDeviceEntity
import com.mmr.core.lansync.LanSyncMergeOptions
import com.mmr.core.lansync.LanSyncMergeSummary
import com.mmr.core.lansync.LanSyncPairingOffer
import com.mmr.core.lansync.transport.DiscoveredLanPeer
import com.mmr.core.lansync.transport.LanSyncHttpConstants
import kotlinx.coroutines.flow.StateFlow

data class LanSyncSessionState(
    val enabled: Boolean = false,
    val deviceId: String = "",
    val displayName: String = "",
    val pairingOfferText: String = "",
    val trustedDevices: List<TrustedLanDeviceEntity> = emptyList(),
    val discoveredPeers: List<DiscoveredLanPeer> = emptyList(),
    val onWifi: Boolean = false,
    val wifiSsid: String? = null,
    val localSyncUrl: String? = null,
    val serverPort: Int = LanSyncHttpConstants.DEFAULT_PORT,
    val syncLog: List<LanSyncLogEntryEntity> = emptyList(),
    val hasLocalLedgerData: Boolean = true,
    val peerMode: LanSyncPeerMode = LanSyncPeerMode.ALL_TRUSTED_ON_LAN,
    val selectedPeerDeviceId: String? = null,
    val includeAttachments: Boolean = false,
    val pendingApprovalCount: Int = 0,
)

data class LanSyncNowResult(
    val summary: LanSyncMergeSummary,
    val peerSuccesses: List<String> = emptyList(),
    val peerFailures: List<String>,
    val warnings: List<String> = emptyList(),
)

interface LanSyncRepository {
    val sessionState: StateFlow<LanSyncSessionState>

    suspend fun refreshSessionState()

    fun setLanSyncEnabled(enabled: Boolean)

    fun setDisplayName(name: String)

    fun pairingOffer(): LanSyncPairingOffer

    /** Resolves a short pairing PIN from the LAN or a legacy pasted `mmr-lan-sync:` payload. */
    suspend fun resolvePairingInput(raw: String): LanSyncPairingOffer

    suspend fun trustDeviceFromPairingOffer(offer: LanSyncPairingOffer)

    suspend fun removeTrustedDevice(deviceId: String)

    suspend fun markCurrentWifiTrusted()

    suspend fun clearSyncLog()

    fun setPeerMode(mode: LanSyncPeerMode)

    fun setSelectedPeerDeviceId(deviceId: String?)

    fun setIncludeAttachments(enabled: Boolean)

    suspend fun syncNow(mergeOptions: LanSyncMergeOptions = LanSyncMergeOptions()): LanSyncNowResult

    suspend fun restoreFromPeer(
        remoteDeviceId: String,
        mergeOptions: LanSyncMergeOptions = LanSyncMergeOptions(),
    ): LanSyncMergeSummary
}
