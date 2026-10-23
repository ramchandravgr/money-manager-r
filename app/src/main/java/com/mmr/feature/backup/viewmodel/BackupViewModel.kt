package com.mmr.feature.backup.viewmodel

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mmr.app.ui.state.MmrUiMessages
import com.mmr.core.backup.BackupImportMode
import com.mmr.core.domain.repository.BackupRepository
import com.mmr.feature.backup.model.BackupUiEvent
import com.mmr.feature.backup.model.BackupUiState
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
                    _uiState.update {
                        it.copy(
                            showImportConfirmDialog = true,
                            pendingImportLabel = event.displayName ?: "selected backup file",
                            importMode = BackupImportMode.REPLACE,
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
                        it.copy(showImportConfirmDialog = false, pendingImportLabel = null)
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
                    val json = backupRepository.exportBackupJson()
                    withContext(Dispatchers.IO) {
                        context.contentResolver.openOutputStream(uri)?.use { stream ->
                            stream.writer(Charsets.UTF_8).use { writer ->
                                writer.write(json)
                            }
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
                    val json =
                        withContext(Dispatchers.IO) {
                            context.contentResolver.openInputStream(uri)?.use { stream ->
                                stream.reader(Charsets.UTF_8).readText()
                            } ?: throw IllegalStateException("Unable to read backup file.")
                        }
                    val imported = backupRepository.importBackupJson(json, importMode)
                    val summary = backupRepository.getLocalDataSummary()
                    pendingImportUri = null
                    _uiState.update {
                        it.copy(
                            isBusy = false,
                            pendingImportLabel = null,
                            summary = summary,
                            isEmpty = summary.totalRecords == 0,
                            statusMessage =
                                buildString {
                                    append(
                                        if (importMode == BackupImportMode.MERGE) {
                                            "Merge complete. ${imported.totalRecords} new record(s) added."
                                        } else {
                                            "Restore complete. ${imported.totalRecords} records imported."
                                        },
                                    )
                                    if (imported.appSettingsRestored) append(" App settings restored.")
                                },
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
