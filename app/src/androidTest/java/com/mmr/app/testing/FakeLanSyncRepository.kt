package com.mmr.app.testing

import com.mmr.core.data.preferences.LanSyncPeerMode
import com.mmr.core.domain.repository.LanSyncNowResult
import com.mmr.core.domain.repository.LanSyncRepository
import com.mmr.core.domain.repository.LanSyncSessionState
import com.mmr.core.lansync.LanSyncMergeOptions
import com.mmr.core.lansync.LanSyncMergeSummary
import com.mmr.core.lansync.LanSyncPairingCodec
import com.mmr.core.lansync.LanSyncPairingOffer
import kotlinx.coroutines.flow.MutableStateFlow

/** Instrumented-test fake for [LanSyncRepository] (mirrors JVM `com.mmr.test.FakeLanSyncRepository`). */
class FakeLanSyncRepository : LanSyncRepository {
    private val stateFlow = MutableStateFlow(LanSyncSessionState())

    override val sessionState: MutableStateFlow<LanSyncSessionState> = stateFlow

    override suspend fun refreshSessionState() = Unit

    override fun setLanSyncEnabled(enabled: Boolean) = Unit

    override fun setDisplayName(name: String) = Unit

    override fun pairingOffer(): LanSyncPairingOffer =
        LanSyncPairingOffer(deviceId = "test", displayName = "Test", inboundAuthToken = "token")

    override suspend fun resolvePairingInput(raw: String): LanSyncPairingOffer {
        val trimmed = raw.trim()
        if (trimmed.startsWith(LanSyncPairingCodec.LEGACY_PREFIX)) {
            return LanSyncPairingCodec.decode(trimmed)
        }
        return pairingOffer()
    }

    override suspend fun trustDeviceFromPairingOffer(offer: LanSyncPairingOffer) = Unit

    override suspend fun removeTrustedDevice(deviceId: String) = Unit

    override suspend fun markCurrentWifiTrusted() = Unit

    override suspend fun clearSyncLog() = Unit

    override fun setPeerMode(mode: LanSyncPeerMode) = Unit

    override fun setSelectedPeerDeviceId(deviceId: String?) = Unit

    override fun setIncludeAttachments(enabled: Boolean) = Unit

    override suspend fun syncNow(mergeOptions: LanSyncMergeOptions): LanSyncNowResult =
        LanSyncNowResult(summary = emptyMergeSummary(), peerFailures = emptyList())

    override suspend fun restoreFromPeer(
        remoteDeviceId: String,
        mergeOptions: LanSyncMergeOptions,
    ): LanSyncMergeSummary = emptyMergeSummary()

    private fun emptyMergeSummary(): LanSyncMergeSummary =
        LanSyncMergeSummary(
            categoriesInserted = 0,
            categoriesUpdated = 0,
            subcategoriesInserted = 0,
            subcategoriesUpdated = 0,
            accountsInserted = 0,
            accountsUpdated = 0,
            loansInserted = 0,
            loansUpdated = 0,
            transactionsInserted = 0,
            transactionsUpdated = 0,
        )
}
