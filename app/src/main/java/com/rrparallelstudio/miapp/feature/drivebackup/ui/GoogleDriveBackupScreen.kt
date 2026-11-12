package com.rrparallelstudio.miapp.feature.drivebackup.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.hilt.navigation.compose.hiltViewModel
import com.rrparallelstudio.miapp.feature.drivebackup.viewmodel.GoogleDriveBackupViewModel
import com.rrparallelstudio.miapp.testing.MmrTestTags
import com.rrparallelstudio.miapp.ui.components.MmrIosSettingsBackupImportModeSheet
import com.rrparallelstudio.miapp.ui.components.MmrProcessingOverlay
import com.rrparallelstudio.miapp.ui.components.MmrSettingsGroup
import com.rrparallelstudio.miapp.ui.components.MmrSettingsGroupAction
import com.rrparallelstudio.miapp.ui.components.MmrSettingsGroupEmphasis
import com.rrparallelstudio.miapp.ui.components.MmrSettingsGroupNavRow
import com.rrparallelstudio.miapp.ui.components.MmrSettingsGroupNote
import com.rrparallelstudio.miapp.ui.components.MmrSettingsScreenLayout
import com.rrparallelstudio.miapp.ui.components.MmrSettingsScreenScaffold
import com.rrparallelstudio.miapp.ui.components.MmrSettingsStatusBanners
import java.text.DateFormat
import java.util.Date

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoogleDriveBackupScreen(
    onNavigateBack: () -> Unit,
    viewModel: GoogleDriveBackupViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val restoreSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val authorizeLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
            viewModel.onAuthorizationResult(result.data)
        }

    LaunchedEffect(uiState.authorizationPendingIntent) {
        val pendingIntent = uiState.authorizationPendingIntent
        if (pendingIntent != null) {
            authorizeLauncher.launch(IntentSenderRequest.Builder(pendingIntent.intentSender).build())
            viewModel.clearAuthorizationPendingIntent()
        }
    }

    LaunchedEffect(uiState.linkedEmail) {
        if (uiState.linkedEmail != null) {
            viewModel.refreshRemoteBackups()
        }
    }

    if (uiState.showRestoreDialog) {
        MmrIosSettingsBackupImportModeSheet(
            title = "Restore This File",
            fileLabel = uiState.pendingRestoreFile?.name ?: "backup",
            selectedMode = uiState.pendingImportMode,
            onSelectMode = viewModel::setPendingImportMode,
            onDismiss = viewModel::dismissRestoreDialog,
            onConfirm = viewModel::confirmRestore,
            sheetState = restoreSheetState,
            confirmLabel = "Restore",
            confirmTestTag = MmrTestTags.backupImportConfirm,
            replaceModeTestTag = MmrTestTags.backupImportModeReplace,
            mergeModeTestTag = MmrTestTags.backupImportModeMerge,
        )
    }

    MmrProcessingOverlay(
        visible = uiState.isBusy,
        message = uiState.busyMessage ?: "Working…",
    )

    MmrSettingsScreenScaffold(
        title = "Google Drive",
        onNavigateBack = {
            if (!uiState.isBusy) {
                onNavigateBack()
            }
        },
        layout = MmrSettingsScreenLayout.InsetGrouped,
        modifier = Modifier.testTag(MmrTestTags.googleDriveBackupScreen),
    ) {
        MmrSettingsStatusBanners(
            statusMessage = uiState.statusMessage,
            errorMessage = uiState.errorMessage,
            onDismissStatus = viewModel::dismissMessages,
            onDismissError = viewModel::dismissMessages,
        )

        MmrSettingsGroup(
            title = "About",
            isFirstInScreen = true,
        ) {
            MmrSettingsGroupNote(
                text =
                    "These copies use space in your Google account. " +
                        "They are the same files saved in your backup folder, under MoneyInbox / Backups.",
                showDividerBelow = false,
            )
        }

        if (!uiState.isConfigured) {
            MmrSettingsGroup(
                title = "Setup",
                footer = "This copy of the app cannot connect to Google Drive.",
            ) {
                MmrSettingsGroupNote(text = "Google Drive backup is not available.", showDividerBelow = false)
            }
            return@MmrSettingsScreenScaffold
        }

        MmrSettingsGroup(title = "Account") {
            if (uiState.linkedEmail == null) {
                MmrSettingsGroupEmphasis(
                    text = "Not connected",
                    helperBelow =
                        "Pick your Google account and allow Drive access. Backup schedule and Back Up Now " +
                            "live on Backup & Restore.",
                )
                MmrSettingsGroupAction(
                    text = "Turn On Google Drive Backup",
                    onClick = viewModel::turnOnDriveBackup,
                    enabled = !uiState.isBusy,
                    showDividerBelow = false,
                    testTag = MmrTestTags.googleDriveBackupTurnOn,
                )
            } else {
                MmrSettingsGroupEmphasis(text = uiState.linkedEmail!!, showDividerBelow = true)
                MmrSettingsGroupAction(
                    text = "Disconnect",
                    onClick = viewModel::disconnect,
                    enabled = !uiState.isBusy,
                    showDividerBelow = false,
                )
            }
        }

        if (uiState.linkedEmail != null) {
            MmrSettingsGroup(
                title = "Backups On Drive",
                footer =
                    buildString {
                        append("Tap a backup to put it on this phone. ")
                        append("To create a new one, use Back Up Now on Backup & Restore. ")
                        uiState.lastBackupLabel?.let { append("Last upload: $it") }
                    }.trim().takeIf { it.isNotBlank() },
            ) {
                if (uiState.remoteBackups.isEmpty()) {
                    MmrSettingsGroupNote(
                        text = "Files you back up appear here when Drive is connected.",
                        showDividerBelow = false,
                    )
                } else {
                    uiState.remoteBackups.forEachIndexed { index, file ->
                        val whenLabel =
                            DateFormat.getDateTimeInstance().format(Date(file.modifiedTimeMillis))
                        MmrSettingsGroupNavRow(
                            label = file.name,
                            value = whenLabel,
                            onClick = { if (!uiState.isBusy) viewModel.beginRestore(file) },
                            showDividerBelow = index < uiState.remoteBackups.lastIndex,
                        )
                    }
                }
            }
        }
    }
}
