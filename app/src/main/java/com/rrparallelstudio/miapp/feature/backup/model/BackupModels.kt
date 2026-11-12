package com.rrparallelstudio.miapp.feature.backup.model

import android.net.Uri
import com.rrparallelstudio.miapp.core.backup.BackupImportMode
import com.rrparallelstudio.miapp.core.backup.model.BackupRestoreSummary
import com.rrparallelstudio.miapp.core.drive.GoogleDriveAutoSchedule

data class BackupUiState(
    val isLoading: Boolean = true,
    val isBusy: Boolean = false,
    val busyMessage: String? = null,
    val summary: BackupRestoreSummary? = null,
    val isEmpty: Boolean = false,
    val statusMessage: String? = null,
    val errorMessage: String? = null,
    val showImportConfirmDialog: Boolean = false,
    val pendingImportLabel: String? = null,
    val pendingImportIsMoneyManager: Boolean = false,
    val pendingImportIsMonefy: Boolean = false,
    val importMode: BackupImportMode = BackupImportMode.REPLACE,
    val showSpreadsheetImportConfirmDialog: Boolean = false,
    val pendingSpreadsheetLabel: String? = null,
    val localAutoSchedule: GoogleDriveAutoSchedule = GoogleDriveAutoSchedule.OFF,
    val lastLocalBackupLabel: String? = null,
)

sealed interface BackupUiEvent {
    data class OnExportToUri(
        val uri: Uri,
    ) : BackupUiEvent

    data class OnImportUriSelected(
        val uri: Uri,
        val displayName: String?,
    ) : BackupUiEvent

    object OnConfirmImport : BackupUiEvent

    data class OnImportModeSelected(
        val mode: BackupImportMode,
    ) : BackupUiEvent

    object OnDismissImportDialog : BackupUiEvent

    object OnDismissMessage : BackupUiEvent

    object OnRetryClick : BackupUiEvent

    object OnExportSpreadsheetTemplate : BackupUiEvent

    data class OnSpreadsheetImportUriSelected(
        val uri: Uri,
        val displayName: String?,
    ) : BackupUiEvent

    object OnConfirmSpreadsheetImport : BackupUiEvent

    object OnDismissSpreadsheetImportDialog : BackupUiEvent

    data class OnSetLocalAutoSchedule(
        val schedule: GoogleDriveAutoSchedule,
    ) : BackupUiEvent

    object OnLocalBackupNow : BackupUiEvent
}
