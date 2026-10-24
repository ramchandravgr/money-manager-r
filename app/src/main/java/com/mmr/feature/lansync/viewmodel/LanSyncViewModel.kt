package com.mmr.feature.lansync.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mmr.app.ui.state.MmrUiMessages
import com.mmr.core.data.preferences.LanSyncPeerMode
import com.mmr.core.data.preferences.LanSyncPreferencesStore
import com.mmr.core.domain.repository.LanSyncRepository
import com.mmr.core.lansync.LanSyncMergeConflicts
import com.mmr.core.lansync.LanSyncMergeOptions
import com.mmr.core.lansync.LanSyncNeedsResolutionException
import com.mmr.core.lansync.LanSyncRecurringResolution
import com.mmr.core.lansync.LanSyncWifiNotTrustedException
import com.mmr.feature.lansync.LanSyncStatusMessages
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LanSyncLogLine(
    val label: String,
    val detail: String,
    val success: Boolean,
)

enum class LanSyncSensitiveAction {
    TRUST_DEVICE,
    REMOVE_TRUSTED,
}

data class LanSyncUiState(
    val enabled: Boolean = false,
    val displayName: String = "",
    val pairingOfferText: String = "",
    val pairingInput: String = "",
    val trustedDeviceLabels: List<String> = emptyList(),
    val trustedDeviceIds: List<String> = emptyList(),
    val discoveredLabels: List<String> = emptyList(),
    val onWifi: Boolean = false,
    val wifiSsid: String? = null,
    val localSyncUrl: String? = null,
    val hasLocalLedgerData: Boolean = true,
    val syncLogLines: List<LanSyncLogLine> = emptyList(),
    val peerMode: LanSyncPeerMode = LanSyncPeerMode.ALL_TRUSTED_ON_LAN,
    val selectedPeerDeviceId: String? = null,
    val includeAttachments: Boolean = false,
    val pendingApprovalCount: Int = 0,
    val isBusy: Boolean = false,
    val statusMessage: String? = null,
    val errorMessage: String? = null,
    val wifiTrustPromptSsid: String? = null,
    val mergeConflicts: LanSyncMergeConflicts? = null,
    val sensitiveAction: LanSyncSensitiveAction? = null,
    val removeTrustedIndex: Int? = null,
    val syncNowButtonText: String = "Sync now",
    val syncNowEnabled: Boolean = false,
    val syncNowDisabledHint: String? = null,
)

@HiltViewModel
class LanSyncViewModel
    @Inject
    constructor(
        private val lanSyncRepository: LanSyncRepository,
        private val preferencesStore: LanSyncPreferencesStore,
    ) : ViewModel() {
        private val pairingInput = MutableStateFlow("")
        private val isBusy = MutableStateFlow(false)
        private val statusMessage = MutableStateFlow<String?>(null)
        private val errorMessage = MutableStateFlow<String?>(null)
        private val wifiTrustPromptSsid = MutableStateFlow<String?>(null)
        private val mergeConflicts = MutableStateFlow<LanSyncMergeConflicts?>(null)
        private val sensitiveAction = MutableStateFlow<LanSyncSensitiveAction?>(null)
        private val removeTrustedIndex = MutableStateFlow<Int?>(null)
        private var mergeOptions = LanSyncMergeOptions()

        private val _navigateHomeAfterRestore = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
        val navigateHomeAfterRestore = _navigateHomeAfterRestore.asSharedFlow()

        val uiState: StateFlow<LanSyncUiState> =
            combine(
                combine(
                    lanSyncRepository.sessionState,
                    pairingInput,
                    isBusy,
                    statusMessage,
                    errorMessage,
                ) { session, input, busy, status, error ->
                    val singleDeviceMode = session.peerMode == LanSyncPeerMode.SINGLE_DEVICE
                    val selectedPeer =
                        session.trustedDevices.find { it.remoteDeviceId == session.selectedPeerDeviceId }
                    val missingSingleDeviceSelection =
                        singleDeviceMode &&
                            session.trustedDevices.isNotEmpty() &&
                            selectedPeer == null
                    val syncNowEnabled =
                        session.enabled &&
                            session.onWifi &&
                            session.trustedDevices.isNotEmpty() &&
                            !busy &&
                            !missingSingleDeviceSelection
                    val syncNowButtonText =
                        if (singleDeviceMode && selectedPeer != null) {
                            "Sync with: ${selectedPeer.displayName}"
                        } else {
                            "Sync now"
                        }
                    val syncNowDisabledHint =
                        if (missingSingleDeviceSelection) {
                            "Select a trusted device above to sync with one phone only."
                        } else {
                            null
                        }
                    LanSyncUiState(
                        enabled = session.enabled,
                        displayName = session.displayName,
                        pairingOfferText = session.pairingOfferText,
                        pairingInput = input,
                        trustedDeviceLabels = session.trustedDevices.map { it.displayName },
                        trustedDeviceIds = session.trustedDevices.map { it.remoteDeviceId },
                        discoveredLabels = session.discoveredPeers.map { "${it.displayName} · ${it.host}" },
                        onWifi = session.onWifi,
                        wifiSsid = session.wifiSsid,
                        localSyncUrl = session.localSyncUrl,
                        hasLocalLedgerData = session.hasLocalLedgerData,
                        syncLogLines =
                            session.syncLog.map { entry ->
                                LanSyncLogLine(
                                    label = entry.peerName,
                                    detail = entry.summaryText,
                                    success = entry.success,
                                )
                            },
                        peerMode = session.peerMode,
                        selectedPeerDeviceId = session.selectedPeerDeviceId,
                        includeAttachments = session.includeAttachments,
                        pendingApprovalCount = session.pendingApprovalCount,
                        isBusy = busy,
                        statusMessage = status,
                        errorMessage = error,
                        syncNowButtonText = syncNowButtonText,
                        syncNowEnabled = syncNowEnabled,
                        syncNowDisabledHint = syncNowDisabledHint,
                    )
                },
                wifiTrustPromptSsid,
                mergeConflicts,
                sensitiveAction,
                removeTrustedIndex,
            ) { base, wifiPrompt, conflicts, action, removeIndex ->
                base.copy(
                    wifiTrustPromptSsid = wifiPrompt,
                    mergeConflicts = conflicts,
                    sensitiveAction = action,
                    removeTrustedIndex = removeIndex,
                )
            }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), LanSyncUiState())

        init {
            viewModelScope.launch { lanSyncRepository.refreshSessionState() }
        }

        fun setEnabled(enabled: Boolean) {
            lanSyncRepository.setLanSyncEnabled(enabled)
            if (enabled) {
                statusMessage.value = "Peer Device Sync is on. Web Manage was turned off."
            }
            viewModelScope.launch { lanSyncRepository.refreshSessionState() }
        }

        fun setDisplayName(name: String) {
            lanSyncRepository.setDisplayName(name)
        }

        fun setPeerMode(mode: LanSyncPeerMode) {
            lanSyncRepository.setPeerMode(mode)
        }

        fun setSelectedPeer(deviceId: String?) {
            lanSyncRepository.setSelectedPeerDeviceId(deviceId)
        }

        fun setIncludeAttachments(enabled: Boolean) {
            lanSyncRepository.setIncludeAttachments(enabled)
        }

        fun onPairingInputChanged(value: String) {
            pairingInput.value = value
        }

        fun requestTrustDevice() {
            if (pairingInput.value.trim().isEmpty()) return
            sensitiveAction.value = LanSyncSensitiveAction.TRUST_DEVICE
        }

        fun requestRemoveTrusted(index: Int) {
            removeTrustedIndex.value = index
            sensitiveAction.value = LanSyncSensitiveAction.REMOVE_TRUSTED
        }

        fun clearSensitiveAction() {
            sensitiveAction.value = null
            removeTrustedIndex.value = null
        }

        fun completeTrustAfterAuth() {
            sensitiveAction.value = null
            trustFromPairingInputInternal()
        }

        fun completeRemoveTrustedAfterAuth() {
            val index = removeTrustedIndex.value
            sensitiveAction.value = null
            removeTrustedIndex.value = null
            if (index == null) return
            viewModelScope.launch {
                val deviceId =
                    lanSyncRepository.sessionState.value.trustedDevices
                        .getOrNull(index)
                        ?.remoteDeviceId
                if (deviceId == null) return@launch
                lanSyncRepository.removeTrustedDevice(deviceId)
                lanSyncRepository.refreshSessionState()
            }
        }

        private fun trustFromPairingInputInternal() {
            val raw = pairingInput.value.trim()
            if (raw.isEmpty()) return
            viewModelScope.launch {
                isBusy.value = true
                errorMessage.value = null
                try {
                    val offer = lanSyncRepository.resolvePairingInput(raw)
                    lanSyncRepository.trustDeviceFromPairingOffer(offer)
                    pairingInput.value = ""
                    statusMessage.value = "Device trusted. Sync when both phones are on the same Wi‑Fi."
                    lanSyncRepository.refreshSessionState()
                } catch (e: Exception) {
                    errorMessage.value = MmrUiMessages.friendly(e.message, MmrUiMessages.GENERIC_ACTION)
                } finally {
                    isBusy.value = false
                }
            }
        }

        fun syncNow() {
            runSync { lanSyncRepository.syncNow(mergeOptions) }
        }

        fun restoreFromTrusted(
            index: Int,
            navigateHomeOnSuccess: Boolean = false,
        ) {
            viewModelScope.launch {
                val device =
                    lanSyncRepository.sessionState.value.trustedDevices
                        .getOrNull(index)
                val deviceId = device?.remoteDeviceId
                if (deviceId == null) return@launch
                isBusy.value = true
                errorMessage.value = null
                statusMessage.value = null
                try {
                    val summary = lanSyncRepository.restoreFromPeer(deviceId, mergeOptions)
                    mergeOptions = LanSyncMergeOptions()
                    lanSyncRepository.refreshSessionState()
                    if (navigateHomeOnSuccess) {
                        _navigateHomeAfterRestore.emit(Unit)
                    } else {
                        statusMessage.value =
                            "Restored from ${device.displayName} (one-way pull). " +
                            "${summary.totalChanges} change(s) applied."
                    }
                } catch (e: LanSyncWifiNotTrustedException) {
                    wifiTrustPromptSsid.value = e.ssid
                } catch (e: LanSyncNeedsResolutionException) {
                    mergeConflicts.value = e.conflicts
                } catch (e: Exception) {
                    errorMessage.value = MmrUiMessages.friendly(e.message, MmrUiMessages.GENERIC_ACTION)
                } finally {
                    isBusy.value = false
                }
            }
        }

        fun dismissMergeConflicts() {
            mergeConflicts.value = null
            mergeOptions = LanSyncMergeOptions()
        }

        fun resolveConflictsUseRemoteForAll() {
            val conflicts = mergeConflicts.value ?: return
            var options = mergeOptions
            for (item in conflicts.recurring) {
                options =
                    options.copy(
                        recurringBySyncId =
                            options.recurringBySyncId +
                                (item.syncId to LanSyncRecurringResolution.USE_REMOTE),
                    )
            }
            mergeOptions = options
            mergeConflicts.value = null
            syncNow()
        }

        fun resolveConflictsKeepLocalForAll() {
            val conflicts = mergeConflicts.value ?: return
            var options = mergeOptions
            for (item in conflicts.recurring) {
                options =
                    options.copy(
                        recurringBySyncId =
                            options.recurringBySyncId +
                                (item.syncId to LanSyncRecurringResolution.USE_LOCAL),
                    )
            }
            mergeOptions = options
            mergeConflicts.value = null
            syncNow()
        }

        fun confirmWifiTrust() {
            viewModelScope.launch {
                lanSyncRepository.markCurrentWifiTrusted()
                wifiTrustPromptSsid.value = null
                statusMessage.value = "Wi‑Fi network trusted."
            }
        }

        fun dismissWifiTrustPrompt() {
            wifiTrustPromptSsid.value = null
        }

        fun clearSyncLog() {
            viewModelScope.launch {
                lanSyncRepository.clearSyncLog()
                lanSyncRepository.refreshSessionState()
            }
        }

        fun dismissMessages() {
            statusMessage.value = null
            errorMessage.value = null
        }

        private fun runSync(block: suspend () -> com.mmr.core.domain.repository.LanSyncNowResult) {
            viewModelScope.launch {
                isBusy.value = true
                errorMessage.value = null
                statusMessage.value = null
                try {
                    val result = block()
                    mergeOptions = LanSyncMergeOptions()
                    statusMessage.value = LanSyncStatusMessages.formatSyncNowStatus(result)
                    lanSyncRepository.refreshSessionState()
                } catch (e: LanSyncWifiNotTrustedException) {
                    wifiTrustPromptSsid.value = e.ssid
                } catch (e: LanSyncNeedsResolutionException) {
                    mergeConflicts.value = e.conflicts
                } catch (e: Exception) {
                    errorMessage.value = MmrUiMessages.friendly(e.message, MmrUiMessages.GENERIC_ACTION)
                } finally {
                    isBusy.value = false
                }
            }
        }
    }
