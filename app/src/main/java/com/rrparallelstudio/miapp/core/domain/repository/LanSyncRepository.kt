package com.rrparallelstudio.miapp.core.domain.repository

import com.rrparallelstudio.miapp.core.data.preferences.LanSyncPeerMode
import com.rrparallelstudio.miapp.core.database.entity.LanSyncLogEntryEntity
import com.rrparallelstudio.miapp.core.database.entity.PendingLanPairRequestEntity
import com.rrparallelstudio.miapp.core.database.entity.TrustedLanDeviceEntity
import com.rrparallelstudio.miapp.core.lansync.LanSyncMergeOptions
import com.rrparallelstudio.miapp.core.lansync.LanSyncMergeSummary
import com.rrparallelstudio.miapp.core.lansync.LanSyncPairingOffer
import com.rrparallelstudio.miapp.core.lansync.transport.DiscoveredLanPeer
import com.rrparallelstudio.miapp.core.lansync.transport.LanSyncHttpConstants
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
    val autoSyncOnAppOpen: Boolean = false,
    val pendingApprovalCount: Int = 0,
    val pendingPairRequests: List<PendingLanPairRequestEntity> = emptyList(),
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

    /** Re-advertise and re-browse on LAN (e.g. when the sync settings screen is open). */
    fun refreshPeerDiscovery()

    fun setLanSyncEnabled(enabled: Boolean)

    fun setDisplayName(name: String)

    fun pairingOffer(): LanSyncPairingOffer

    /**
     * Resolves a short pairing PIN via `GET /mmr/v1/pairing-offer`.
     * [manualPeerAddress] optional `host:port` or `http://host:port` when mDNS discovery fails.
     */
    suspend fun resolvePairingInput(
        raw: String,
        manualPeerAddress: String? = null,
    ): LanSyncPairingOffer

    suspend fun trustDeviceFromPairingOffer(
        offer: LanSyncPairingOffer,
        /** Host pairing code from the other device; required for mutual inbound `/pair`. */
        pairingCode: String? = null,
    )

    suspend fun approvePendingPairRequest(remoteDeviceId: String)

    suspend fun rejectPendingPairRequest(remoteDeviceId: String)

    suspend fun removeTrustedDevice(deviceId: String)

    suspend fun markCurrentWifiTrusted()

    suspend fun clearSyncLog()

    fun setPeerMode(mode: LanSyncPeerMode)

    fun setSelectedPeerDeviceId(deviceId: String?)

    fun setIncludeAttachments(enabled: Boolean)

    fun setAutoSyncOnAppOpen(enabled: Boolean)

    /**
     * Syncs trusted peers that are currently discovered on LAN (respects single-device mode).
     * No-op when auto sync is off, sync is unavailable, or no active peers. Used on app foreground.
     */
    suspend fun syncActiveTrustedPeersOnAppOpen(
        mergeOptions: LanSyncMergeOptions = LanSyncMergeOptions(),
    ): LanSyncNowResult?

    suspend fun syncNow(
        mergeOptions: LanSyncMergeOptions = LanSyncMergeOptions(),
        /** `host:port` from the other phone when discovery did not list it. */
        manualPeerAddress: String? = null,
    ): LanSyncNowResult

    suspend fun restoreFromPeer(
        remoteDeviceId: String,
        mergeOptions: LanSyncMergeOptions = LanSyncMergeOptions(),
    ): LanSyncMergeSummary
}
