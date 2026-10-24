package com.mmr.feature.drivebackup.viewmodel

import android.app.PendingIntent
import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mmr.app.drive.GoogleDriveAuthGateway
import com.mmr.app.drive.GoogleDriveAuthorizeStep
import com.mmr.app.ui.state.MmrUiMessages
import com.mmr.core.backup.BackupImportMode
import com.mmr.core.data.preferences.GoogleDriveBackupPreferencesStore
import com.mmr.core.domain.repository.BackupRepository
import com.mmr.core.domain.repository.GoogleDriveBackupRepository
import com.mmr.core.drive.GoogleDriveAutoSchedule
import com.mmr.core.drive.GoogleDriveFileRef
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.DateFormat
import java.util.Date
import javax.inject.Inject

data class GoogleDriveBackupUiState(
    val isConfigured: Boolean = false,
    val linkedEmail: String? = null,
    val autoSchedule: GoogleDriveAutoSchedule = GoogleDriveAutoSchedule.OFF,
    val wifiOnly: Boolean = true,
    val lastBackupLabel: String? = null,
    val remoteBackups: List<GoogleDriveFileRef> = emptyList(),
    val isBusy: Boolean = false,
    val statusMessage: String? = null,
    val errorMessage: String? = null,
    val showRestoreDialog: Boolean = false,
    val pendingRestoreFile: GoogleDriveFileRef? = null,
    val pendingImportMode: BackupImportMode = BackupImportMode.REPLACE,
    val authorizationPendingIntent: PendingIntent? = null,
)

@HiltViewModel
class GoogleDriveBackupViewModel
    @Inject
    constructor(
        private val authGateway: GoogleDriveAuthGateway,
        private val preferencesStore: GoogleDriveBackupPreferencesStore,
        private val driveBackupRepository: GoogleDriveBackupRepository,
        private val backupRepository: BackupRepository,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow(GoogleDriveBackupUiState())
        val uiState: StateFlow<GoogleDriveBackupUiState> = _uiState.asStateFlow()

        init {
            refreshStatus()
        }

        fun refreshStatus() {
            val lastMillis = preferencesStore.getLastSuccessfulBackupMillis()
            _uiState.update {
                it.copy(
                    isConfigured = authGateway.isConfigured(),
                    linkedEmail = preferencesStore.getLinkedEmail(),
                    autoSchedule = preferencesStore.getAutoSchedule(),
                    wifiOnly = preferencesStore.isWifiOnly(),
                    lastBackupLabel =
                        if (lastMillis > 0L) {
                            DateFormat.getDateTimeInstance().format(Date(lastMillis))
                        } else {
                            null
                        },
                )
            }
        }

        fun turnOnDriveBackup() {
            viewModelScope.launch {
                _uiState.update { it.copy(isBusy = true, errorMessage = null, statusMessage = null) }
                runCatching { authGateway.authorize() }
                    .onSuccess { step ->
                        when (step) {
                            is GoogleDriveAuthorizeStep.NeedsUi -> {
                                _uiState.update {
                                    it.copy(
                                        isBusy = false,
                                        authorizationPendingIntent = step.pendingIntent,
                                    )
                                }
                            }

                            is GoogleDriveAuthorizeStep.Complete -> {
                                onDriveAuthorized(step.email, step.accessToken)
                            }
                        }
                    }.onFailure { error ->
                        _uiState.update {
                            it.copy(
                                isBusy = false,
                                errorMessage = MmrUiMessages.friendly(error.message, MmrUiMessages.GENERIC_ACTION),
                            )
                        }
                    }
            }
        }

        fun onAuthorizationResult(data: Intent?) {
            viewModelScope.launch {
                _uiState.update { it.copy(isBusy = true, authorizationPendingIntent = null, errorMessage = null) }
                val result = authGateway.completeAuthorize(data)
                result
                    .onSuccess { session -> onDriveAuthorized(session.email, session.accessToken) }
                    .onFailure { error ->
                        _uiState.update {
                            it.copy(
                                isBusy = false,
                                errorMessage = MmrUiMessages.friendly(error.message, MmrUiMessages.GENERIC_ACTION),
                            )
                        }
                    }
            }
        }

        fun clearAuthorizationPendingIntent() {
            _uiState.update { it.copy(authorizationPendingIntent = null) }
        }

        private suspend fun onDriveAuthorized(
            email: String,
            accessToken: String,
        ) {
            preferencesStore.setAutoSchedule(GoogleDriveAutoSchedule.DAILY)
            preferencesStore.setWifiOnly(true)
            _uiState.update { it.copy(linkedEmail = email) }
            refreshStatus()
            runCatching {
                driveBackupRepository.uploadBackup(accessToken)
            }.onSuccess {
                _uiState.update {
                    it.copy(
                        isBusy = false,
                        statusMessage = "Google Drive backup is on. First backup uploaded.",
                    )
                }
                refreshRemoteBackups()
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        isBusy = false,
                        statusMessage = "Google Drive backup is on (daily, Wi‑Fi only).",
                        errorMessage = MmrUiMessages.friendly(error.message, MmrUiMessages.GENERIC_ACTION),
                    )
                }
                refreshRemoteBackups()
            }
        }

        fun disconnect() {
            viewModelScope.launch {
                _uiState.update { it.copy(isBusy = true) }
                authGateway.disconnect()
                _uiState.update {
                    it.copy(
                        isBusy = false,
                        linkedEmail = null,
                        remoteBackups = emptyList(),
                        statusMessage = "Disconnected from Google Drive.",
                    )
                }
                refreshStatus()
            }
        }

        fun setAutoSchedule(schedule: GoogleDriveAutoSchedule) {
            preferencesStore.setAutoSchedule(schedule)
            refreshStatus()
        }

        fun setWifiOnly(enabled: Boolean) {
            preferencesStore.setWifiOnly(enabled)
            refreshStatus()
        }

        fun uploadNow() {
            viewModelScope.launch {
                _uiState.update { it.copy(isBusy = true, errorMessage = null, statusMessage = null) }
                runCatching {
                    val token =
                        authGateway.fetchAccessToken()
                            ?: throw IllegalStateException("Connect Google Drive again.")
                    driveBackupRepository.uploadBackup(token)
                }.onSuccess {
                    _uiState.update {
                        it.copy(isBusy = false, statusMessage = "Backup uploaded to Google Drive.")
                    }
                    refreshStatus()
                    refreshRemoteBackups()
                }.onFailure { error ->
                    _uiState.update {
                        it.copy(
                            isBusy = false,
                            errorMessage = MmrUiMessages.friendly(error.message, MmrUiMessages.GENERIC_ACTION),
                        )
                    }
                }
            }
        }

        fun refreshRemoteBackups() {
            viewModelScope.launch {
                if (!preferencesStore.isLinked()) return@launch
                runCatching {
                    val token =
                        authGateway.fetchAccessToken()
                            ?: throw IllegalStateException("Connect Google Drive again.")
                    driveBackupRepository.listRemoteBackups(token)
                }.onSuccess { files ->
                    _uiState.update { it.copy(remoteBackups = files) }
                }
            }
        }

        fun beginRestore(file: GoogleDriveFileRef) {
            _uiState.update {
                it.copy(
                    showRestoreDialog = true,
                    pendingRestoreFile = file,
                    pendingImportMode = BackupImportMode.REPLACE,
                )
            }
        }

        fun setPendingImportMode(mode: BackupImportMode) {
            _uiState.update { it.copy(pendingImportMode = mode) }
        }

        fun dismissRestoreDialog() {
            _uiState.update {
                it.copy(showRestoreDialog = false, pendingRestoreFile = null)
            }
        }

        fun confirmRestore() {
            val file = _uiState.value.pendingRestoreFile ?: return
            val mode = _uiState.value.pendingImportMode
            viewModelScope.launch {
                _uiState.update {
                    it.copy(isBusy = true, showRestoreDialog = false, errorMessage = null, statusMessage = null)
                }
                runCatching {
                    val token =
                        authGateway.fetchAccessToken()
                            ?: throw IllegalStateException("Connect Google Drive again.")
                    val bytes = driveBackupRepository.downloadBackup(token, file.id)
                    backupRepository.importBackup(bytes, mode)
                }.onSuccess { summary ->
                    _uiState.update {
                        it.copy(
                            isBusy = false,
                            pendingRestoreFile = null,
                            statusMessage =
                                buildString {
                                    append("Restore complete. ${summary.totalRecords} records.")
                                    if (summary.appSettingsRestored) append(" App settings restored.")
                                },
                        )
                    }
                }.onFailure { error ->
                    _uiState.update {
                        it.copy(
                            isBusy = false,
                            pendingRestoreFile = null,
                            errorMessage = MmrUiMessages.friendly(error.message, MmrUiMessages.GENERIC_ACTION),
                        )
                    }
                }
            }
        }

        fun dismissMessages() {
            _uiState.update { it.copy(statusMessage = null, errorMessage = null) }
        }
    }
