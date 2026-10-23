package com.mmr.feature.backup.model

import android.net.Uri
import com.mmr.core.backup.BackupImportMode
import com.mmr.core.backup.model.BackupRestoreSummary

data class BackupUiState(
    val isLoading: Boolean = true,
    val isBusy: Boolean = false,
    val summary: BackupRestoreSummary? = null,
    val isEmpty: Boolean = false,
    val statusMessage: String? = null,
    val errorMessage: String? = null,
    val showImportConfirmDialog: Boolean = false,
    val pendingImportLabel: String? = null,
    val importMode: BackupImportMode = BackupImportMode.REPLACE,
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
}
