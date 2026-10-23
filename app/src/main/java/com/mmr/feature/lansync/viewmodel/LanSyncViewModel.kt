package com.mmr.feature.lansync.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mmr.app.ui.state.MmrUiMessages
import com.mmr.core.domain.repository.LanSyncRepository
import com.mmr.core.lansync.LanSyncPairingCodec
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LanSyncUiState(
    val enabled: Boolean = false,
    val displayName: String = "",
    val pairingOfferText: String = "",
    val pairingInput: String = "",
    val trustedDeviceLabels: List<String> = emptyList(),
    val pendingRequestLabels: List<String> = emptyList(),
    val discoveredLabels: List<String> = emptyList(),
    val onWifi: Boolean = false,
    val isBusy: Boolean = false,
    val statusMessage: String? = null,
    val errorMessage: String? = null,
)

@HiltViewModel
class LanSyncViewModel
    @Inject
    constructor(
        private val lanSyncRepository: LanSyncRepository,
    ) : ViewModel() {
        private val pairingInput = MutableStateFlow("")
        private val isBusy = MutableStateFlow(false)
        private val statusMessage = MutableStateFlow<String?>(null)
        private val errorMessage = MutableStateFlow<String?>(null)

        val uiState: StateFlow<LanSyncUiState> =
            combine(
                lanSyncRepository.sessionState,
                pairingInput,
                isBusy,
                statusMessage,
                errorMessage,
            ) { session, input, busy, status, error ->
                LanSyncUiState(
                    enabled = session.enabled,
                    displayName = session.displayName,
                    pairingOfferText = session.pairingOfferText,
                    pairingInput = input,
                    trustedDeviceLabels =
                        session.trustedDevices.map {
                            "${it.displayName} (${it.remoteDeviceId.take(
                                8,
                            )}…)"
                        },
                    pendingRequestLabels =
                        session.pendingPairRequests.map {
                            "${it.displayName} (${it.remoteDeviceId.take(
                                8,
                            )}…)"
                        },
                    discoveredLabels = session.discoveredPeers.map { "${it.displayName} @ ${it.host}" },
                    onWifi = session.onWifi,
                    isBusy = busy,
                    statusMessage = status,
                    errorMessage = error,
                )
            }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), LanSyncUiState())

        init {
            viewModelScope.launch { lanSyncRepository.refreshSessionState() }
        }

        fun setEnabled(enabled: Boolean) {
            lanSyncRepository.setLanSyncEnabled(enabled)
            viewModelScope.launch { lanSyncRepository.refreshSessionState() }
        }

        fun setDisplayName(name: String) {
            lanSyncRepository.setDisplayName(name)
        }

        fun onPairingInputChanged(value: String) {
            pairingInput.value = value
        }

        fun trustFromPairingInput() {
            val raw = pairingInput.value.trim()
            if (raw.isEmpty()) return
            viewModelScope.launch {
                isBusy.value = true
                errorMessage.value = null
                try {
                    val offer = LanSyncPairingCodec.decode(raw)
                    lanSyncRepository.trustDeviceFromPairingOffer(offer)
                    pairingInput.value = ""
                    statusMessage.value = "Device trusted. Approve this phone on the other device if prompted."
                    lanSyncRepository.refreshSessionState()
                } catch (e: Exception) {
                    errorMessage.value = MmrUiMessages.friendly(e.message, MmrUiMessages.GENERIC_ACTION)
                } finally {
                    isBusy.value = false
                }
            }
        }

        fun approvePending(index: Int) {
            viewModelScope.launch {
                val deviceId =
                    lanSyncRepository.sessionState.value.pendingPairRequests
                        .getOrNull(index)
                        ?.remoteDeviceId
                if (deviceId == null) return@launch
                isBusy.value = true
                try {
                    lanSyncRepository.approvePairRequest(deviceId)
                    statusMessage.value = "Device approved."
                    lanSyncRepository.refreshSessionState()
                } catch (e: Exception) {
                    errorMessage.value = MmrUiMessages.friendly(e.message, MmrUiMessages.GENERIC_ACTION)
                } finally {
                    isBusy.value = false
                }
            }
        }

        fun rejectPending(index: Int) {
            viewModelScope.launch {
                val deviceId =
                    lanSyncRepository.sessionState.value.pendingPairRequests
                        .getOrNull(index)
                        ?.remoteDeviceId
                if (deviceId == null) return@launch
                lanSyncRepository.rejectPairRequest(deviceId)
                lanSyncRepository.refreshSessionState()
            }
        }

        fun removeTrusted(index: Int) {
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

        fun syncNow() {
            viewModelScope.launch {
                isBusy.value = true
                errorMessage.value = null
                statusMessage.value = null
                try {
                    val summary = lanSyncRepository.syncNow()
                    statusMessage.value = "Sync complete. ${summary.totalChanges} change(s) applied."
                    lanSyncRepository.refreshSessionState()
                } catch (e: Exception) {
                    errorMessage.value = MmrUiMessages.friendly(e.message, MmrUiMessages.GENERIC_ACTION)
                } finally {
                    isBusy.value = false
                }
            }
        }

        fun dismissMessages() {
            statusMessage.value = null
            errorMessage.value = null
        }
    }
