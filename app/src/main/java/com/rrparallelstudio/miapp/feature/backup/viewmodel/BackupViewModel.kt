package com.rrparallelstudio.miapp.feature.backup.viewmodel

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rrparallelstudio.miapp.core.backup.BackupImportMode
import com.rrparallelstudio.miapp.core.domain.repository.BackupRepository
import com.rrparallelstudio.miapp.core.moneymanager.MoneyManagerMmbakFormat
import com.rrparallelstudio.miapp.feature.backup.model.BackupUiEvent
import com.rrparallelstudio.miapp.feature.backup.model.BackupUiState
import com.rrparallelstudio.miapp.feature.backup.moneyManagerImportStatusMessage
import com.rrparallelstudio.miapp.ui.state.MmrUiMessages
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class BackupViewModel
    @Inject
    constructor(
        private val backupRepository: BackupRepository,
        @ApplicationContext private val context: Context,
    ) : ViewModel() {
        private var pendingImportUri: Uri? = null

        private val _uiState = MutableStateFlow(BackupUiState())
        val uiState: StateFlow<BackupUiState> = _uiState.asStateFlow()

        init {
            loadSummary()
        }

        fun onEvent(event: BackupUiEvent) {
            when (event) {
                is BackupUiEvent.OnExportToUri -> exportToUri(event.uri)
                is BackupUiEvent.OnImportUriSelected -> {
                    pendingImportUri = event.uri
                    val label = event.displayName ?: "selected backup file"
                    val isMoneyManager = label.endsWith(".mmbak", ignoreCase = true)
                    _uiState.update {
                        it.copy(
                            showImportConfirmDialog = true,
                            pendingImportLabel = label,
                            pendingImportIsMoneyManager = isMoneyManager,
                            importMode =
                                if (isMoneyManager) {
                                    BackupImportMode.MERGE
                                } else {
                                    BackupImportMode.REPLACE
                                },
                            errorMessage = null,
                            statusMessage = null,
                        )
                    }
                }
                BackupUiEvent.OnConfirmImport -> confirmImport()
                is BackupUiEvent.OnImportModeSelected -> {
                    _uiState.update { it.copy(importMode = event.mode) }
                }
                BackupUiEvent.OnDismissImportDialog -> {
                    pendingImportUri = null
                    _uiState.update {
                        it.copy(
                            showImportConfirmDialog = false,
                            pendingImportLabel = null,
                            pendingImportIsMoneyManager = false,
                        )
                    }
                }
                BackupUiEvent.OnDismissMessage -> {
                    _uiState.update { it.copy(statusMessage = null, errorMessage = null) }
                }
                BackupUiEvent.OnRetryClick -> loadSummary()
            }
        }

        private fun loadSummary() {
            viewModelScope.launch {
                _uiState.update { it.copy(isLoading = true, errorMessage = null) }
                try {
                    val summary = backupRepository.getLocalDataSummary()
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            summary = summary,
                            isEmpty = summary.totalRecords == 0,
                        )
                    }
                } catch (e: Exception) {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = MmrUiMessages.friendly(e.message, MmrUiMessages.GENERIC_LOAD),
                        )
                    }
                }
            }
        }

        private fun exportToUri(uri: Uri) {
            viewModelScope.launch {
                _uiState.update { it.copy(isBusy = true, errorMessage = null, statusMessage = null) }
                try {
                    val archive = backupRepository.exportBackupArchive()
                    withContext(Dispatchers.IO) {
                        context.contentResolver.openOutputStream(uri)?.use { stream ->
                            stream.write(archive)
                        } ?: throw IllegalStateException("Unable to write backup file.")
                    }
                    val summary = backupRepository.getLocalDataSummary()
                    _uiState.update {
                        it.copy(
                            isBusy = false,
                            summary = summary,
                            isEmpty = summary.totalRecords == 0,
                            statusMessage = "Backup exported successfully.",
                        )
                    }
                } catch (e: Exception) {
                    _uiState.update {
                        it.copy(
                            isBusy = false,
                            errorMessage = MmrUiMessages.friendly(e.message, MmrUiMessages.GENERIC_ACTION),
                        )
                    }
                }
            }
        }

        private fun confirmImport() {
            val uri = pendingImportUri ?: return
            val importMode = _uiState.value.importMode
            val displayName = _uiState.value.pendingImportLabel
            val isMoneyManager = _uiState.value.pendingImportIsMoneyManager
            viewModelScope.launch {
                _uiState.update {
                    it.copy(
                        isBusy = true,
                        showImportConfirmDialog = false,
                        errorMessage = null,
                        statusMessage = null,
                    )
                }
                try {
                    val data =
                        withContext(Dispatchers.IO) {
                            context.contentResolver.openInputStream(uri)?.use { stream ->
                                stream.readBytes()
                            } ?: throw IllegalStateException("Unable to read backup file.")
                        }
                    val inferredMoneyManager =
                        isMoneyManager ||
                            MoneyManagerMmbakFormat.isProbablyMoneyManagerBackup(data, displayName)
                    val statusMessage =
                        if (inferredMoneyManager) {
                            moneyManagerImportStatusMessage(backupRepository.importMoneyManagerMmbak(data))
                        } else {
                            val imported = backupRepository.importBackup(data, importMode, displayName)
                            buildString {
                                append(
                                    if (importMode == BackupImportMode.MERGE) {
                                        "Merge complete. ${imported.totalRecords} new record(s) added."
                                    } else {
                                        "Restore complete. ${imported.totalRecords} records imported."
                                    },
                                )
                                if (imported.appSettingsRestored) append(" App settings restored.")
                            }
                        }
                    val summary = backupRepository.getLocalDataSummary()
                    pendingImportUri = null
                    _uiState.update {
                        it.copy(
                            isBusy = false,
                            pendingImportLabel = null,
                            pendingImportIsMoneyManager = false,
                            summary = summary,
                            isEmpty = summary.totalRecords == 0,
                            statusMessage = statusMessage,
                        )
                    }
                } catch (e: Exception) {
                    pendingImportUri = null
                    _uiState.update {
                        it.copy(
                            isBusy = false,
                            pendingImportLabel = null,
                            errorMessage = MmrUiMessages.friendly(e.message, MmrUiMessages.GENERIC_ACTION),
                        )
                    }
                }
            }
        }
    }
