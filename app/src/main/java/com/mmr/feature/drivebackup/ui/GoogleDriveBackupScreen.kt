package com.mmr.feature.drivebackup.ui

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
import com.mmr.app.testing.MmrTestTags
import com.mmr.app.ui.components.MmrIosSettingsBackupImportModeSheet
import com.mmr.app.ui.components.MmrIosSettingsBusyIndicator
import com.mmr.app.ui.components.MmrIosSettingsEmphasisTextSlot
import com.mmr.app.ui.components.MmrIosSettingsNavRow
import com.mmr.app.ui.components.MmrIosSettingsPrimaryActionSlot
import com.mmr.app.ui.components.MmrIosSettingsRadioRow
import com.mmr.app.ui.components.MmrIosSettingsSecondaryActionSlot
import com.mmr.app.ui.components.MmrIosSettingsSection
import com.mmr.app.ui.components.MmrIosSettingsSectionBodyText
import com.mmr.app.ui.components.MmrIosSettingsSwitchRow
import com.mmr.app.ui.components.MmrSettingsScreenLayout
import com.mmr.app.ui.components.MmrSettingsScreenScaffold
import com.mmr.app.ui.components.MmrSettingsStatusBanners
import com.mmr.core.drive.GoogleDriveAutoSchedule
import com.mmr.feature.drivebackup.viewmodel.GoogleDriveBackupViewModel
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
            title = "Restore from Drive",
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

    MmrSettingsScreenScaffold(
        title = "Google Drive backup",
        onNavigateBack = onNavigateBack,
        layout = MmrSettingsScreenLayout.InsetGrouped,
        modifier = Modifier.testTag(MmrTestTags.googleDriveBackupScreen),
    ) {
        MmrSettingsStatusBanners(
            statusMessage = uiState.statusMessage,
            errorMessage = uiState.errorMessage,
            onDismissStatus = viewModel::dismissMessages,
            onDismissError = viewModel::dismissMessages,
        )

        MmrIosSettingsSection(
            title = "About",
            isFirstInScreen = true,
            footer = "Opt-in folder MoneyInbox/Backups on your Drive. Same signed .mmrbackup format as local export.",
        ) {
            MmrIosSettingsSectionBodyText(text = "Backups use your Google account storage and API quotas.")
        }

        if (!uiState.isConfigured) {
            MmrIosSettingsSection(
                title = "Setup",
                footer = "Add GOOGLE_WEB_CLIENT_ID to local.properties and rebuild to enable Google Drive backup.",
            ) {
                MmrIosSettingsSectionBodyText(
                    text = "Google Drive backup is not configured in this build.",
                )
            }
            return@MmrSettingsScreenScaffold
        }

        if (uiState.isBusy) {
            MmrIosSettingsBusyIndicator()
        }

        MmrIosSettingsSection(title = "Account") {
            if (uiState.linkedEmail == null) {
                MmrIosSettingsEmphasisTextSlot(
                    text = "Not connected",
                    helperBelow =
                        "Pick your Google account, allow Drive access, enable daily backup (Wi‑Fi only), " +
                            "and upload once.",
                )
                MmrIosSettingsPrimaryActionSlot(
                    text = "Turn on Google Drive backup",
                    onClick = viewModel::turnOnDriveBackup,
                    enabled = !uiState.isBusy,
                    showDividerBelow = false,
                    testTag = MmrTestTags.googleDriveBackupTurnOn,
                )
            } else {
                MmrIosSettingsEmphasisTextSlot(text = uiState.linkedEmail!!)
                MmrIosSettingsSecondaryActionSlot(
                    text = "Disconnect",
                    onClick = viewModel::disconnect,
                    enabled = !uiState.isBusy,
                    showDividerBelow = false,
                )
            }
        }

        if (uiState.linkedEmail != null) {
            MmrIosSettingsSection(
                title = "Automatic backup",
                footer = uiState.lastBackupLabel?.let { "Last backup: $it" },
            ) {
                MmrIosSettingsRadioRow(
                    label = "Off",
                    selected = uiState.autoSchedule == GoogleDriveAutoSchedule.OFF,
                    onSelect = { viewModel.setAutoSchedule(GoogleDriveAutoSchedule.OFF) },
                )
                MmrIosSettingsRadioRow(
                    label = "Daily (first app open each day)",
                    selected = uiState.autoSchedule == GoogleDriveAutoSchedule.DAILY,
                    onSelect = { viewModel.setAutoSchedule(GoogleDriveAutoSchedule.DAILY) },
                )
                MmrIosSettingsRadioRow(
                    label = "Weekly",
                    selected = uiState.autoSchedule == GoogleDriveAutoSchedule.WEEKLY,
                    onSelect = { viewModel.setAutoSchedule(GoogleDriveAutoSchedule.WEEKLY) },
                )
                MmrIosSettingsSwitchRow(
                    label = "Wi‑Fi only",
                    checked = uiState.wifiOnly,
                    onCheckedChange = viewModel::setWifiOnly,
                    showDividerBelow = true,
                )
                MmrIosSettingsPrimaryActionSlot(
                    text = "Back up now",
                    onClick = viewModel::uploadNow,
                    enabled = !uiState.isBusy,
                    showDividerBelow = false,
                    testTag = MmrTestTags.googleDriveBackupNow,
                )
            }

            MmrIosSettingsSection(
                title = "Restore from Drive",
                footer = if (uiState.remoteBackups.isEmpty()) "No backups in Drive yet." else null,
            ) {
                if (uiState.remoteBackups.isNotEmpty()) {
                    uiState.remoteBackups.forEachIndexed { index, file ->
                        val whenLabel =
                            DateFormat.getDateTimeInstance().format(Date(file.modifiedTimeMillis))
                        MmrIosSettingsNavRow(
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
