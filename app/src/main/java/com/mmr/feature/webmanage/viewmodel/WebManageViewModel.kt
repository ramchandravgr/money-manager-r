package com.mmr.feature.webmanage.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mmr.core.domain.repository.WebManageRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class WebManageUiState(
    val enabled: Boolean = false,
    val serverListening: Boolean = false,
    val onWifi: Boolean = false,
    val wifiSsid: String? = null,
    val localUrl: String? = null,
    val loopbackUrl: String? = null,
    val lanIpv4: String? = null,
    val hasAccessPassword: Boolean = false,
    val pendingApprovalCount: Int = 0,
    val passwordInput: String = "",
    val isBusy: Boolean = false,
    val statusMessage: String? = null,
    val errorMessage: String? = null,
)

@HiltViewModel
class WebManageViewModel
    @Inject
    constructor(
        private val webManageRepository: WebManageRepository,
    ) : ViewModel() {
        private val passwordInput = MutableStateFlow("")
        private val isBusy = MutableStateFlow(false)
        private val statusMessage = MutableStateFlow<String?>(null)
        private val errorMessage = MutableStateFlow<String?>(null)

        val uiState: StateFlow<WebManageUiState> =
            combine(
                webManageRepository.sessionState,
                passwordInput,
                isBusy,
                statusMessage,
                errorMessage,
            ) { session, password, busy, status, error ->
                WebManageUiState(
                    enabled = session.enabled,
                    serverListening = session.serverListening,
                    onWifi = session.onWifi,
                    wifiSsid = session.wifiSsid,
                    localUrl = session.localUrl,
                    loopbackUrl = session.loopbackUrl,
                    lanIpv4 = session.lanIpv4,
                    hasAccessPassword = session.hasAccessPassword,
                    pendingApprovalCount = session.pendingApprovalCount,
                    passwordInput = password,
                    isBusy = busy,
                    statusMessage = status,
                    errorMessage = error,
                )
            }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), WebManageUiState())

        init {
            viewModelScope.launch { webManageRepository.refreshSessionState() }
        }

        fun refreshConnectionInfo() {
            viewModelScope.launch { webManageRepository.refreshSessionState() }
        }

        fun onPasswordChange(value: String) {
            passwordInput.value = value
        }

        fun savePassword() {
            viewModelScope.launch {
                isBusy.value = true
                errorMessage.value = null
                statusMessage.value = null
                try {
                    webManageRepository.setAccessPassword(passwordInput.value)
                    passwordInput.value = ""
                    val serverOn = webManageRepository.sessionState.value.enabled
                    statusMessage.value =
                        if (serverOn) {
                            "Web password updated. Sign in again in the browser with the new password."
                        } else {
                            "Web password saved. Turn on Web Manage when you are ready."
                        }
                } catch (e: Exception) {
                    errorMessage.value = e.message ?: "Could not save password."
                } finally {
                    isBusy.value = false
                }
            }
        }

        fun setEnabled(enabled: Boolean) {
            viewModelScope.launch {
                isBusy.value = true
                errorMessage.value = null
                statusMessage.value = null
                try {
                    if (enabled && !webManageRepository.sessionState.value.hasAccessPassword) {
                        errorMessage.value = "Set a Web Manage password first."
                        return@launch
                    }
                    webManageRepository.setWebManageEnabled(enabled)
                    statusMessage.value =
                        when {
                            enabled -> "Web Manage is on. Peer Device Sync was turned off."
                            else -> "Web Manage is off."
                        }
                    webManageRepository.refreshSessionState()
                } catch (e: Exception) {
                    errorMessage.value = e.message ?: "Could not update Web Manage."
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
