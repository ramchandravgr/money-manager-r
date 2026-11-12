package com.rrparallelstudio.miapp.feature.lansync.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rrparallelstudio.miapp.core.data.preferences.LanSyncPeerMode
import com.rrparallelstudio.miapp.core.domain.repository.LanSyncRepository
import com.rrparallelstudio.miapp.core.lansync.LanSyncMergeOptions
import com.rrparallelstudio.miapp.core.lansync.LanSyncOverrideJson
import com.rrparallelstudio.miapp.core.lansync.LanSyncPairingPin
import com.rrparallelstudio.miapp.core.lansync.LanSyncWifiNotTrustedException
import com.rrparallelstudio.miapp.feature.lansync.LanSyncStatusMessages
import com.rrparallelstudio.miapp.ui.state.MmrUiMessages
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
    APPROVE_PAIR_REQUEST,
}

data class LanSyncDiscoveredPeerRow(
    val deviceId: String,
    val displayName: String,
    val address: String,
)

data class LanSyncUiState(
    val enabled: Boolean = false,
    val displayName: String = "",
    val pairingOfferText: String = "",
    val pairingInput: String = "",
    val pairingManualPeerInput: String = "",
    val trustedDeviceLabels: List<String> = emptyList(),
    val trustedDeviceIds: List<String> = emptyList(),
    val discoveredPeers: List<LanSyncDiscoveredPeerRow> = emptyList(),
    val selectedPairingPeerAddress: String? = null,
    val onWifi: Boolean = false,
    val wifiSsid: String? = null,
    val localSyncUrl: String? = null,
    val hasLocalLedgerData: Boolean = true,
    val syncLogLines: List<LanSyncLogLine> = emptyList(),
    val peerMode: LanSyncPeerMode = LanSyncPeerMode.ALL_TRUSTED_ON_LAN,
    val selectedPeerDeviceId: String? = null,
    val includeAttachments: Boolean = false,
    val autoSyncOnAppOpen: Boolean = false,
    val pendingApprovalCount: Int = 0,
    val isBusy: Boolean = false,
    val statusMessage: String? = null,
    val errorMessage: String? = null,
    val wifiTrustPromptSsid: String? = null,
    val sensitiveAction: LanSyncSensitiveAction? = null,
    val historyValue: String = "None yet",
    val removeTrustedIndex: Int? = null,
    val syncNowButtonText: String = "Sync now",
    val syncNowEnabled: Boolean = false,
    val syncNowDisabledHint: String? = null,
    val pendingPairLabels: List<String> = emptyList(),
    val pendingPairDeviceIds: List<String> = emptyList(),
)

@HiltViewModel
class LanSyncViewModel
    @Inject
    constructor(
        private val lanSyncRepository: LanSyncRepository,
    ) : ViewModel() {
        private val pairingInput = MutableStateFlow("")
        private val pairingManualPeerInput = MutableStateFlow("")
        private val selectedPairingPeerAddress = MutableStateFlow<String?>(null)
        private val isBusy = MutableStateFlow(false)
        private val statusMessage = MutableStateFlow<String?>(null)
        private val errorMessage = MutableStateFlow<String?>(null)
        private val wifiTrustPromptSsid = MutableStateFlow<String?>(null)
        private val sensitiveAction = MutableStateFlow<LanSyncSensitiveAction?>(null)
        private val removeTrustedIndex = MutableStateFlow<Int?>(null)
        private val approvePairDeviceId = MutableStateFlow<String?>(null)
        private var mergeOptions = LanSyncMergeOptions()

        private val _navigateHomeAfterRestore = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
        val navigateHomeAfterRestore = _navigateHomeAfterRestore.asSharedFlow()

        val uiState: StateFlow<LanSyncUiState> =
            combine(
                combine(
                    combine(
                        lanSyncRepository.sessionState,
                        pairingInput,
                        pairingManualPeerInput,
                        selectedPairingPeerAddress,
                    ) { session, input, manualPeer, selectedPeerAddress ->
                        Quad(session, input, manualPeer, selectedPeerAddress)
                    },
                    combine(isBusy, statusMessage, errorMessage) { busy, status, error ->
                        Triple(busy, status, error)
                    },
                ) { sessionQuad, busyTriple ->
                    val (session, input, manualPeer, selectedPeerAddress) = sessionQuad
                    val (busy, status, error) = busyTriple
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
                            "Sync With ${selectedPeer.displayName}"
                        } else {
                            "Sync Now"
                        }
                    val syncNowDisabledHint =
                        if (missingSingleDeviceSelection) {
                            "Choose one trusted phone above. Sync Now will use only that phone."
                        } else {
                            null
                        }
                    LanSyncUiState(
                        enabled = session.enabled,
                        displayName = session.displayName,
                        pairingOfferText = session.pairingOfferText,
                        pairingInput = input,
                        pairingManualPeerInput = manualPeer,
                        trustedDeviceLabels = session.trustedDevices.map { it.displayName },
                        trustedDeviceIds = session.trustedDevices.map { it.remoteDeviceId },
                        discoveredPeers =
                            session.discoveredPeers
                                .filter { it.deviceId != session.deviceId }
                                .map {
                                    LanSyncDiscoveredPeerRow(
                                        deviceId = it.deviceId,
                                        displayName = it.displayName,
                                        address = "${it.host}:${it.port}",
                                    )
                                },
                        selectedPairingPeerAddress = selectedPeerAddress,
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
                        autoSyncOnAppOpen = session.autoSyncOnAppOpen,
                        pendingApprovalCount = session.pendingApprovalCount,
                        isBusy = busy,
                        statusMessage = status,
                        errorMessage = error,
                        syncNowButtonText = syncNowButtonText,
                        syncNowEnabled = syncNowEnabled,
                        syncNowDisabledHint = syncNowDisabledHint,
                        pendingPairLabels = session.pendingPairRequests.map { it.displayName },
                        pendingPairDeviceIds = session.pendingPairRequests.map { it.remoteDeviceId },
                        historyValue = historyValueFor(session.syncLog.firstOrNull()),
                    )
                },
                wifiTrustPromptSsid,
                sensitiveAction,
                removeTrustedIndex,
            ) { base, wifiPrompt, action, removeIndex ->
                base.copy(
                    wifiTrustPromptSsid = wifiPrompt,
                    sensitiveAction = action,
                    removeTrustedIndex = removeIndex,
                )
            }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), LanSyncUiState())

        init {
            viewModelScope.launch { lanSyncRepository.refreshSessionState() }
        }

        fun refreshPeerDiscovery() {
            lanSyncRepository.refreshPeerDiscovery()
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

        fun setAutoSyncOnAppOpen(enabled: Boolean) {
            lanSyncRepository.setAutoSyncOnAppOpen(enabled)
        }

        fun onPairingInputChanged(value: String) {
            pairingInput.value = LanSyncPairingPin.sanitizeEntry(value)
        }

        fun onPairingManualPeerInputChanged(value: String) {
            pairingManualPeerInput.value = value
            val trimmed = value.trim()
            val matchesDiscovery =
                lanSyncRepository.sessionState.value.discoveredPeers.any {
                    "${it.host}:${it.port}" == trimmed
                }
            selectedPairingPeerAddress.value = if (matchesDiscovery && trimmed.isNotEmpty()) trimmed else null
        }

        fun selectDiscoveredPeerForPairing(address: String) {
            pairingManualPeerInput.value = address
            selectedPairingPeerAddress.value = address
        }

        fun clearSelectedPairingPeer() {
            pairingManualPeerInput.value = ""
            selectedPairingPeerAddress.value = null
        }

        private data class Quad<A, B, C, D>(
            val first: A,
            val second: B,
            val third: C,
            val fourth: D,
        )

        fun requestTrustDevice() {
            if (pairingInput.value.trim().isEmpty()) return
            sensitiveAction.value = LanSyncSensitiveAction.TRUST_DEVICE
        }

        fun requestRemoveTrusted(index: Int) {
            removeTrustedIndex.value = index
            sensitiveAction.value = LanSyncSensitiveAction.REMOVE_TRUSTED
        }

        fun requestApprovePairRequest(deviceId: String) {
            approvePairDeviceId.value = deviceId
            sensitiveAction.value = LanSyncSensitiveAction.APPROVE_PAIR_REQUEST
        }

        fun rejectPairRequest(deviceId: String) {
            viewModelScope.launch {
                try {
                    lanSyncRepository.rejectPendingPairRequest(deviceId)
                    statusMessage.value = "Pair request declined."
                    lanSyncRepository.refreshSessionState()
                } catch (e: Exception) {
                    errorMessage.value = MmrUiMessages.friendly(e.message, MmrUiMessages.GENERIC_ACTION)
                }
            }
        }

        fun clearSensitiveAction() {
            sensitiveAction.value = null
            removeTrustedIndex.value = null
            approvePairDeviceId.value = null
        }

        fun completeTrustAfterAuth() {
            sensitiveAction.value = null
            trustFromPairingInputInternal()
        }

        fun completeApprovePairAfterAuth() {
            val deviceId = approvePairDeviceId.value
            sensitiveAction.value = null
            approvePairDeviceId.value = null
            removeTrustedIndex.value = null
            if (deviceId.isNullOrBlank()) return
            viewModelScope.launch {
                isBusy.value = true
                errorMessage.value = null
                try {
                    lanSyncRepository.approvePendingPairRequest(deviceId)
                    statusMessage.value = "Device trusted. You can sync when both phones are on the same Wi‑Fi."
                    lanSyncRepository.refreshSessionState()
                } catch (e: Exception) {
                    errorMessage.value = MmrUiMessages.friendly(e.message, MmrUiMessages.GENERIC_ACTION)
                } finally {
                    isBusy.value = false
                }
            }
        }

        fun completeRemoveTrustedAfterAuth() {
            val index = removeTrustedIndex.value
            sensitiveAction.value = null
            removeTrustedIndex.value = null
            approvePairDeviceId.value = null
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
                    val pairingCodeForPeer =
                        com.rrparallelstudio.miapp.core.lansync.LanSyncPairingPin
                            .normalize(raw)
                    val manual =
                        selectedPairingPeerAddress.value?.trim()?.ifBlank { null }
                            ?: pairingManualPeerInput.value.trim().ifBlank { null }
                    val offer = lanSyncRepository.resolvePairingInput(raw, manual)
                    val alreadyTrusted =
                        lanSyncRepository.sessionState.value.trustedDevices.any {
                            it.remoteDeviceId == offer.deviceId
                        }
                    lanSyncRepository.trustDeviceFromPairingOffer(offer, pairingCodeForPeer)
                    pairingInput.value = ""
                    statusMessage.value =
                        if (alreadyTrusted) {
                            "This phone is already trusted. Tap Sync now. You do not need to approve anything on the other phone."
                        } else {
                            "This phone now trusts the other one. If the other phone asks, tap Approve there."
                        }
                    lanSyncRepository.refreshSessionState()
                } catch (e: Exception) {
                    errorMessage.value = MmrUiMessages.friendly(e.message, MmrUiMessages.GENERIC_ACTION)
                } finally {
                    isBusy.value = false
                }
            }
        }

        fun syncNow() {
            val manual = pairingManualPeerInput.value.trim().ifBlank { null }
            runSync { lanSyncRepository.syncNow(mergeOptions, manual) }
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
                } catch (e: Exception) {
                    errorMessage.value = MmrUiMessages.friendly(e.message, MmrUiMessages.GENERIC_ACTION)
                } finally {
                    isBusy.value = false
                }
            }
        }

        private fun historyValueFor(
            entry: com.rrparallelstudio.miapp.core.database.entity.LanSyncLogEntryEntity?,
        ): String {
            if (entry == null) return "None yet"
            val replaced = LanSyncOverrideJson.decode(entry.overridesJson).size
            return LanSyncStatusMessages.formatHistorySubtitle(
                success = entry.success,
                replacedCount = replaced,
                warningsText = entry.warningsText,
            )
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

        private fun runSync(block: suspend () -> com.rrparallelstudio.miapp.core.domain.repository.LanSyncNowResult) {
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
                } catch (e: Exception) {
                    errorMessage.value = MmrUiMessages.friendly(e.message, MmrUiMessages.GENERIC_ACTION)
                } finally {
                    isBusy.value = false
                }
            }
        }
    }
