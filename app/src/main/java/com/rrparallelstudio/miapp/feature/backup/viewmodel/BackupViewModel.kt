package com.rrparallelstudio.miapp.feature.backup.viewmodel

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rrparallelstudio.miapp.backup.LocalAutoBackupWorkScheduler
import com.rrparallelstudio.miapp.core.backup.BackupImportMode
import com.rrparallelstudio.miapp.core.backup.LocalAutoBackupExporter
import com.rrparallelstudio.miapp.core.data.preferences.AppDisplaySettingsStore
import com.rrparallelstudio.miapp.core.data.preferences.LocalAutoBackupPreferencesStore
import com.rrparallelstudio.miapp.core.domain.repository.BackupRepository
import com.rrparallelstudio.miapp.core.export.PeriodExcelExporter
import com.rrparallelstudio.miapp.core.export.PeriodLedgerSpreadsheetFormat
import com.rrparallelstudio.miapp.core.monefy.MonefyBackupFormat
import com.rrparallelstudio.miapp.core.moneymanager.MoneyManagerMmbakFormat
import com.rrparallelstudio.miapp.feature.backup.model.BackupUiEvent
import com.rrparallelstudio.miapp.feature.backup.model.BackupUiState
import com.rrparallelstudio.miapp.feature.backup.monefyImportStatusMessage
import com.rrparallelstudio.miapp.feature.backup.moneyManagerImportStatusMessage
import com.rrparallelstudio.miapp.feature.backup.spreadsheetImportStatusMessage
import com.rrparallelstudio.miapp.ui.state.MmrUiMessages
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.DateFormat
import java.util.Date
import javax.inject.Inject

@HiltViewModel
class BackupViewModel
    @Inject
    constructor(
        private val backupRepository: BackupRepository,
        private val periodExcelExporter: PeriodExcelExporter,
        private val displaySettingsStore: AppDisplaySettingsStore,
        private val localAutoBackupPreferencesStore: LocalAutoBackupPreferencesStore,
        private val localAutoBackupWorkScheduler: LocalAutoBackupWorkScheduler,
        private val localAutoBackupExporter: LocalAutoBackupExporter,
        @ApplicationContext private val context: Context,
    ) : ViewModel() {
        private var pendingImportUri: Uri? = null
        private var pendingSpreadsheetUri: Uri? = null

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
                    val isMonefy = !isMoneyManager && MonefyBackupFormat.matchesDisplayName(label)
                    _uiState.update {
                        it.copy(
                            showImportConfirmDialog = true,
                            pendingImportLabel = label,
                            pendingImportIsMoneyManager = isMoneyManager,
                            pendingImportIsMonefy = isMonefy,
                            importMode =
                                if (isMoneyManager || isMonefy) {
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
                            pendingImportIsMonefy = false,
                        )
                    }
                }
                BackupUiEvent.OnDismissMessage -> {
                    _uiState.update { it.copy(statusMessage = null, errorMessage = null) }
                }
                BackupUiEvent.OnRetryClick -> loadSummary()
                BackupUiEvent.OnExportSpreadsheetTemplate -> exportSpreadsheetTemplate()
                is BackupUiEvent.OnSpreadsheetImportUriSelected -> {
                    pendingSpreadsheetUri = event.uri
                    _uiState.update {
                        it.copy(
                            showSpreadsheetImportConfirmDialog = true,
                            pendingSpreadsheetLabel = event.displayName ?: "selected spreadsheet",
                            errorMessage = null,
                            statusMessage = null,
                        )
                    }
                }
                BackupUiEvent.OnConfirmSpreadsheetImport -> confirmSpreadsheetImport()
                BackupUiEvent.OnDismissSpreadsheetImportDialog -> {
                    pendingSpreadsheetUri = null
                    _uiState.update {
                        it.copy(
                            showSpreadsheetImportConfirmDialog = false,
                            pendingSpreadsheetLabel = null,
                        )
                    }
                }
                is BackupUiEvent.OnSetLocalAutoSchedule -> {
                    localAutoBackupPreferencesStore.setAutoSchedule(event.schedule)
                    localAutoBackupWorkScheduler.syncPeriodicAutoBackupSchedule()
                    _uiState.update { it.copy(localAutoSchedule = event.schedule) }
                }
                BackupUiEvent.OnLocalBackupNow -> localBackupNow()
            }
        }

        private fun exportSpreadsheetTemplate() {
            viewModelScope.launch {
                beginBusy("Saving template to Downloads…")
                try {
                    val display = displaySettingsStore.settings.first()
                    val saved =
                        periodExcelExporter.exportImportTemplateToDownloads(
                            currencySymbol = display.mainCurrencySymbol,
                            currencyCode = display.mainCurrencyCode,
                        )
                    endBusy(
                        statusMessage = "Template saved to Downloads: $saved",
                    )
                } catch (e: Exception) {
                    endBusy(errorMessage = MmrUiMessages.friendly(e.message, MmrUiMessages.GENERIC_ACTION))
                }
            }
        }

        private fun confirmSpreadsheetImport() {
            val uri = pendingSpreadsheetUri ?: return
            val label = _uiState.value.pendingSpreadsheetLabel
            viewModelScope.launch {
                beginBusy(
                    "Importing spreadsheet…",
                    showSpreadsheetImportConfirmDialog = false,
                )
                try {
                    val data =
                        withContext(Dispatchers.IO) {
                            context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                                ?: throw IllegalStateException("Unable to read spreadsheet.")
                        }
                    if (!PeriodLedgerSpreadsheetFormat.isProbablyLedgerSpreadsheet(data, label)) {
                        throw IllegalArgumentException("File is not a supported ledger spreadsheet.")
                    }
                    val result = backupRepository.importLedgerSpreadsheet(data, label)
                    val summary = backupRepository.getLocalDataSummary()
                    pendingSpreadsheetUri = null
                    endBusy(
                        pendingSpreadsheetLabel = null,
                        summary = summary,
                        isEmpty = summary.totalRecords == 0,
                        statusMessage = spreadsheetImportStatusMessage(result),
                    )
                } catch (e: Exception) {
                    pendingSpreadsheetUri = null
                    endBusy(
                        pendingSpreadsheetLabel = null,
                        errorMessage = MmrUiMessages.friendly(e.message, MmrUiMessages.GENERIC_ACTION),
                    )
                }
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
                            localAutoSchedule = localAutoBackupPreferencesStore.getAutoSchedule(),
                            lastLocalBackupLabel = formatLastBackupLabel(),
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

        private fun localBackupNow() {
            viewModelScope.launch {
                beginBusy("Saving backup to Downloads…")
                try {
                    val result = localAutoBackupExporter.exportNow()
                    endBusy(
                        statusMessage = "Backup saved to Downloads: ${result.savedFileName}",
                        lastLocalBackupLabel = formatLastBackupLabel(),
                    )
                } catch (e: Exception) {
                    endBusy(errorMessage = MmrUiMessages.friendly(e.message, MmrUiMessages.GENERIC_ACTION))
                }
            }
        }

        private fun exportToUri(uri: Uri) {
            viewModelScope.launch {
                beginBusy("Exporting backup…")
                try {
                    val archive = backupRepository.exportBackupArchive()
                    withContext(Dispatchers.IO) {
                        context.contentResolver.openOutputStream(uri)?.use { stream ->
                            stream.write(archive)
                        } ?: throw IllegalStateException("Unable to write backup file.")
                    }
                    val summary = backupRepository.getLocalDataSummary()
                    endBusy(
                        summary = summary,
                        isEmpty = summary.totalRecords == 0,
                        statusMessage = "Backup exported successfully.",
                    )
                } catch (e: Exception) {
                    endBusy(errorMessage = MmrUiMessages.friendly(e.message, MmrUiMessages.GENERIC_ACTION))
                }
            }
        }

        private fun confirmImport() {
            val uri = pendingImportUri ?: return
            val importMode = _uiState.value.importMode
            val displayName = _uiState.value.pendingImportLabel
            val isMoneyManager = _uiState.value.pendingImportIsMoneyManager
            val isMonefy = _uiState.value.pendingImportIsMonefy
            viewModelScope.launch {
                beginBusy(
                    "Importing backup…",
                    showImportConfirmDialog = false,
                )
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
                    val inferredMonefy =
                        !inferredMoneyManager &&
                            (
                                isMonefy ||
                                    MonefyBackupFormat.isProbablyMonefyBackup(data, displayName)
                            )
                    val statusMessage =
                        if (inferredMoneyManager) {
                            moneyManagerImportStatusMessage(backupRepository.importMoneyManagerMmbak(data))
                        } else if (inferredMonefy) {
                            monefyImportStatusMessage(backupRepository.importMonefyBackup(data))
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
                    endBusy(
                        pendingImportLabel = null,
                        pendingImportIsMoneyManager = false,
                        pendingImportIsMonefy = false,
                        summary = summary,
                        isEmpty = summary.totalRecords == 0,
                        statusMessage = statusMessage,
                    )
                } catch (e: Exception) {
                    pendingImportUri = null
                    endBusy(
                        pendingImportLabel = null,
                        pendingImportIsMoneyManager = false,
                        pendingImportIsMonefy = false,
                        errorMessage = MmrUiMessages.friendly(e.message, MmrUiMessages.GENERIC_ACTION),
                    )
                }
            }
        }

        private fun beginBusy(
            message: String,
            showImportConfirmDialog: Boolean = _uiState.value.showImportConfirmDialog,
            showSpreadsheetImportConfirmDialog: Boolean = _uiState.value.showSpreadsheetImportConfirmDialog,
        ) {
            _uiState.update {
                it.copy(
                    isBusy = true,
                    busyMessage = message,
                    showImportConfirmDialog = showImportConfirmDialog,
                    showSpreadsheetImportConfirmDialog = showSpreadsheetImportConfirmDialog,
                    errorMessage = null,
                    statusMessage = null,
                )
            }
        }

        private fun endBusy(
            statusMessage: String? = null,
            errorMessage: String? = null,
            summary: com.rrparallelstudio.miapp.core.backup.model.BackupRestoreSummary? = null,
            isEmpty: Boolean? = null,
            pendingImportLabel: String? = _uiState.value.pendingImportLabel,
            pendingImportIsMoneyManager: Boolean = _uiState.value.pendingImportIsMoneyManager,
            pendingImportIsMonefy: Boolean = _uiState.value.pendingImportIsMonefy,
            pendingSpreadsheetLabel: String? = _uiState.value.pendingSpreadsheetLabel,
            lastLocalBackupLabel: String? = _uiState.value.lastLocalBackupLabel,
        ) {
            _uiState.update { current ->
                current.copy(
                    isBusy = false,
                    busyMessage = null,
                    statusMessage = statusMessage,
                    errorMessage = errorMessage,
                    summary = summary ?: current.summary,
                    isEmpty = isEmpty ?: current.isEmpty,
                    pendingImportLabel = pendingImportLabel,
                    pendingImportIsMoneyManager = pendingImportIsMoneyManager,
                    pendingImportIsMonefy = pendingImportIsMonefy,
                    pendingSpreadsheetLabel = pendingSpreadsheetLabel,
                    lastLocalBackupLabel = lastLocalBackupLabel ?: current.lastLocalBackupLabel,
                )
            }
        }

        private fun formatLastBackupLabel(): String? {
            val millis = localAutoBackupPreferencesStore.getLastSuccessfulBackupMillis()
            return if (millis > 0L) {
                DateFormat.getDateTimeInstance().format(Date(millis))
            } else {
                null
            }
        }
    }
