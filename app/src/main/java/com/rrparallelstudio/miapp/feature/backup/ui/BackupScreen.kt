package com.rrparallelstudio.miapp.feature.backup.ui

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import com.rrparallelstudio.miapp.R
import com.rrparallelstudio.miapp.core.backup.BackupImportMode
import com.rrparallelstudio.miapp.core.backup.model.BackupRestoreSummary
import com.rrparallelstudio.miapp.core.drive.GoogleDriveAutoSchedule
import com.rrparallelstudio.miapp.core.platform.MmrDeveloperContactIntents
import com.rrparallelstudio.miapp.feature.backup.model.BackupUiEvent
import com.rrparallelstudio.miapp.feature.backup.viewmodel.BackupViewModel
import com.rrparallelstudio.miapp.testing.MmrTestTags
import com.rrparallelstudio.miapp.ui.components.MmrConfirmDialog
import com.rrparallelstudio.miapp.ui.components.MmrIosSettingsBackupImportModeSheet
import com.rrparallelstudio.miapp.ui.components.MmrProcessingOverlay
import com.rrparallelstudio.miapp.ui.components.MmrSettingsGroup
import com.rrparallelstudio.miapp.ui.components.MmrSettingsGroupAction
import com.rrparallelstudio.miapp.ui.components.MmrSettingsGroupNavRow
import com.rrparallelstudio.miapp.ui.components.MmrSettingsGroupNote
import com.rrparallelstudio.miapp.ui.components.MmrSettingsGroupRadioRow
import com.rrparallelstudio.miapp.ui.components.MmrSettingsGroupSwitchRow
import com.rrparallelstudio.miapp.ui.components.MmrSettingsGroupValueRow
import com.rrparallelstudio.miapp.ui.components.MmrSettingsScreenLayout
import com.rrparallelstudio.miapp.ui.components.MmrSettingsScreenScaffold
import com.rrparallelstudio.miapp.ui.components.MmrSettingsStatusBanners
import com.rrparallelstudio.miapp.ui.state.MmrErrorState
import com.rrparallelstudio.miapp.ui.state.MmrLoadingState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackupScreen(
    onNavigateBack: () -> Unit,
    onNavigateToGoogleDrive: () -> Unit = {},
    viewModel: BackupViewModel = hiltViewModel(),
    testImportUri: Uri? = null,
    testImportDisplayName: String? = null,
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val importSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    LaunchedEffect(testImportUri, testImportDisplayName) {
        testImportUri?.let { uri ->
            viewModel.onEvent(
                BackupUiEvent.OnImportUriSelected(
                    uri,
                    testImportDisplayName ?: "mmr-test-import.json",
                ),
            )
        }
    }

    val backupFolderLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.OpenDocumentTree(),
        ) { uri: Uri? ->
            if (uri != null) {
                try {
                    val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                    context.contentResolver.takePersistableUriPermission(uri, flags)
                } catch (_: SecurityException) {
                    try {
                        context.contentResolver.takePersistableUriPermission(
                            uri,
                            Intent.FLAG_GRANT_READ_URI_PERMISSION,
                        )
                    } catch (_: SecurityException) {
                    }
                }
                viewModel.onEvent(BackupUiEvent.OnBackupFolderSelected(uri))
            } else {
                viewModel.onEvent(BackupUiEvent.OnBackupFolderPickerDismissed)
            }
        }

    LaunchedEffect(uiState.requestBackupFolderPicker) {
        if (uiState.requestBackupFolderPicker) {
            viewModel.onEvent(BackupUiEvent.OnConsumeBackupFolderPickerRequest)
            backupFolderLauncher.launch(null)
        }
    }

    val spreadsheetImportLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.OpenDocument(),
        ) { uri: Uri? ->
            if (uri != null) {
                val displayName =
                    context.contentResolver
                        .query(
                            uri,
                            arrayOf(android.provider.OpenableColumns.DISPLAY_NAME),
                            null,
                            null,
                            null,
                        )?.use { cursor ->
                            if (cursor.moveToFirst()) cursor.getString(0) else null
                        }
                viewModel.onEvent(BackupUiEvent.OnSpreadsheetImportUriSelected(uri, displayName))
            }
        }

    val importLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.OpenDocument(),
        ) { uri: Uri? ->
            if (uri != null) {
                try {
                    context.contentResolver.takePersistableUriPermission(
                        uri,
                        android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION,
                    )
                } catch (_: SecurityException) {
                    // Some providers do not support persistable permissions.
                }
                val displayName =
                    context.contentResolver
                        .query(
                            uri,
                            arrayOf(android.provider.OpenableColumns.DISPLAY_NAME),
                            null,
                            null,
                            null,
                        )?.use { cursor ->
                            if (cursor.moveToFirst()) cursor.getString(0) else null
                        }
                viewModel.onEvent(BackupUiEvent.OnImportUriSelected(uri, displayName))
            }
        }

    if (uiState.showImportConfirmDialog) {
        val isMoneyManager = uiState.pendingImportIsMoneyManager
        val isMonefy = uiState.pendingImportIsMonefy
        val externalLedgerImport = isMoneyManager || isMonefy
        MmrIosSettingsBackupImportModeSheet(
            title =
                when {
                    isMonefy -> "Import Monefy"
                    isMoneyManager -> "Import Money Manager"
                    else -> "Import Backup"
                },
            fileLabel = uiState.pendingImportLabel ?: "selected backup",
            selectedMode = uiState.importMode,
            onSelectMode = { mode ->
                viewModel.onEvent(BackupUiEvent.OnImportModeSelected(mode))
            },
            onDismiss = { viewModel.onEvent(BackupUiEvent.OnDismissImportDialog) },
            onConfirm = { viewModel.onEvent(BackupUiEvent.OnConfirmImport) },
            sheetState = importSheetState,
            confirmLabel =
                when {
                    isMonefy -> "Import Ledger"
                    isMoneyManager -> "Import Transactions"
                    uiState.importMode == BackupImportMode.REPLACE -> "Replace & Restore"
                    else -> "Merge"
                },
            confirmTestTag = MmrTestTags.backupImportConfirm,
            replaceModeTestTag = MmrTestTags.backupImportModeReplace,
            mergeModeTestTag = MmrTestTags.backupImportModeMerge,
            showReplaceOption = !externalLedgerImport,
            mergeChoiceLabel = if (isMonefy) "Merge Into Ledger" else null,
            mergeHelper =
                when {
                    isMonefy ->
                        "Reads the Monefy file on this device only. Accounts, categories, income, " +
                            "expense, and transfers merge into your ledger. Importing the same file again skips rows already added."
                    isMoneyManager ->
                        "Reads the export on this device only. Income, expense, and transfers merge into " +
                            "your ledger; paired transfer legs dedupe to one row. Duplicates are skipped."
                    else -> null
                },
        )
    }

    if (uiState.showSpreadsheetImportConfirmDialog) {
        MmrConfirmDialog(
            title = "Import Spreadsheet",
            message =
                "Rows from ${uiState.pendingSpreadsheetLabel} merge into your ledger. " +
                    "Use the import template column layout. Re-importing skips rows already added.",
            confirmLabel = "Import",
            onConfirm = { viewModel.onEvent(BackupUiEvent.OnConfirmSpreadsheetImport) },
            onDismiss = { viewModel.onEvent(BackupUiEvent.OnDismissSpreadsheetImportDialog) },
        )
    }

    MmrProcessingOverlay(
        visible = uiState.isBusy,
        message = uiState.busyMessage ?: "Working…",
    )

    Box(modifier = Modifier.fillMaxSize()) {
        when {
            uiState.isLoading -> MmrLoadingState()

            uiState.errorMessage != null && uiState.summary == null -> {
                MmrSettingsScreenScaffold(
                    title = "Backup & Restore",
                    onNavigateBack = onNavigateBack,
                    layout = MmrSettingsScreenLayout.InsetGrouped,
                ) {
                    MmrErrorState(
                        message = uiState.errorMessage ?: "Something went wrong.",
                        onRetry = { viewModel.onEvent(BackupUiEvent.OnRetryClick) },
                    )
                }
            }

            else -> {
                MmrSettingsScreenScaffold(
                    title = "Backup & Restore",
                    onNavigateBack = {
                        if (!uiState.isBusy) {
                            onNavigateBack()
                        }
                    },
                    layout = MmrSettingsScreenLayout.InsetGrouped,
                ) {
                    BackupContent(
                        summary = uiState.summary,
                        isEmpty = uiState.isEmpty,
                        isBusy = uiState.isBusy,
                        autoBackupSchedule = uiState.autoBackupSchedule,
                        lastBackupLabel = uiState.lastBackupLabel,
                        backupFolderLabel = uiState.backupFolderLabel,
                        driveLinked = uiState.driveLinked,
                        driveWifiOnly = uiState.driveWifiOnly,
                        onSetAutoBackupSchedule = {
                            viewModel.onEvent(BackupUiEvent.OnSetAutoBackupSchedule(it))
                        },
                        onBackupNow = { viewModel.onEvent(BackupUiEvent.OnBackupNow) },
                        onChangeBackupFolder = { viewModel.onEvent(BackupUiEvent.OnRequestChangeBackupFolder) },
                        onSetDriveWifiOnly = { enabled ->
                            viewModel.onEvent(BackupUiEvent.OnSetDriveWifiOnly(enabled))
                        },
                        statusMessage = uiState.statusMessage,
                        errorMessage = uiState.errorMessage,
                        onDismissMessage = { viewModel.onEvent(BackupUiEvent.OnDismissMessage) },
                        onImport = {
                            importLauncher.launch(
                                arrayOf(
                                    "application/zip",
                                    "application/json",
                                    "application/octet-stream",
                                    "application/*",
                                ),
                            )
                        },
                        onNavigateToGoogleDrive = onNavigateToGoogleDrive,
                        onExportSpreadsheetTemplate = {
                            viewModel.onEvent(BackupUiEvent.OnExportSpreadsheetTemplate)
                        },
                        onImportSpreadsheet = {
                            spreadsheetImportLauncher.launch(
                                arrayOf(
                                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                                    "application/xml",
                                    "text/xml",
                                ),
                            )
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun BackupContent(
    summary: BackupRestoreSummary?,
    isEmpty: Boolean,
    isBusy: Boolean,
    statusMessage: String?,
    errorMessage: String?,
    onDismissMessage: () -> Unit,
    onImport: () -> Unit,
    onNavigateToGoogleDrive: () -> Unit,
    onExportSpreadsheetTemplate: () -> Unit,
    onImportSpreadsheet: () -> Unit,
    autoBackupSchedule: GoogleDriveAutoSchedule,
    lastBackupLabel: String?,
    backupFolderLabel: String?,
    driveLinked: Boolean,
    driveWifiOnly: Boolean,
    onSetAutoBackupSchedule: (GoogleDriveAutoSchedule) -> Unit,
    onBackupNow: () -> Unit,
    onChangeBackupFolder: () -> Unit,
    onSetDriveWifiOnly: (Boolean) -> Unit,
) {
    val context = LocalContext.current
    val supportEmail = stringResource(R.string.developer_support_email)
    Column(modifier = Modifier.fillMaxWidth()) {
        MmrSettingsStatusBanners(
            statusMessage = statusMessage,
            errorMessage = errorMessage,
            onDismissStatus = onDismissMessage,
            onDismissError = onDismissMessage,
        )
        MmrSettingsGroup(
            title = "On This Phone",
            isFirstInScreen = true,
            footer =
                "A backup includes these, plus your settings. " +
                    "Bank messages you have not accepted yet are left out.",
        ) {
            if (isEmpty) {
                MmrSettingsGroupNote(
                    text = "No local data yet. Add transactions from Home or import a backup file.",
                    showDividerBelow = false,
                )
            } else {
                summary?.let { s ->
                    MmrSettingsGroupValueRow(label = "Total records", value = s.totalRecords.toString())
                    MmrSettingsGroupValueRow(label = "Transactions", value = s.transactions.toString())
                    MmrSettingsGroupValueRow(label = "Accounts", value = s.accounts.toString())
                    MmrSettingsGroupValueRow(label = "Categories", value = s.categories.toString())
                    MmrSettingsGroupValueRow(label = "Loans", value = s.loans.toString())
                    MmrSettingsGroupValueRow(label = "Recurring rules", value = s.recurringRules.toString())
                    MmrSettingsGroupValueRow(label = "Budgets", value = s.budgets.toString())
                    MmrSettingsGroupValueRow(
                        label = "SMS / approvals",
                        value = (s.smsMessageRecords + s.transactionApprovals).toString(),
                        showDividerBelow = false,
                    )
                }
            }
        }

        MmrSettingsGroup(
            title = "Backup",
            footer =
                buildString {
                    append(
                        "Saves a backup in the folder you chose and keeps the latest " +
                            "${com.rrparallelstudio.miapp.core.backup.LocalAutoBackupContracts.MAX_RETAINED_FILES}. ",
                    )
                    if (driveLinked) {
                        append("If Google Drive is connected, that same file is uploaded. ")
                    }
                    lastBackupLabel?.let { append("Last backup: $it") }
                }.trim().takeIf { it.isNotBlank() },
        ) {
            MmrSettingsGroupNavRow(
                label = "Backup Folder",
                value = backupFolderLabel ?: "Not Set",
                onClick = onChangeBackupFolder,
            )
            MmrSettingsGroupRadioRow(
                label = "Off",
                selected = autoBackupSchedule == GoogleDriveAutoSchedule.OFF,
                onSelect = { onSetAutoBackupSchedule(GoogleDriveAutoSchedule.OFF) },
            )
            MmrSettingsGroupRadioRow(
                label = "Daily",
                selected = autoBackupSchedule == GoogleDriveAutoSchedule.DAILY,
                onSelect = { onSetAutoBackupSchedule(GoogleDriveAutoSchedule.DAILY) },
            )
            MmrSettingsGroupRadioRow(
                label = "Weekly",
                selected = autoBackupSchedule == GoogleDriveAutoSchedule.WEEKLY,
                onSelect = { onSetAutoBackupSchedule(GoogleDriveAutoSchedule.WEEKLY) },
            )
            MmrSettingsGroupRadioRow(
                label = "Monthly",
                selected = autoBackupSchedule == GoogleDriveAutoSchedule.MONTHLY,
                onSelect = { onSetAutoBackupSchedule(GoogleDriveAutoSchedule.MONTHLY) },
            )
            if (driveLinked) {
                MmrSettingsGroupSwitchRow(
                    label = "Google Drive Wi‑Fi Only",
                    checked = driveWifiOnly,
                    onCheckedChange = onSetDriveWifiOnly,
                )
            }
            MmrSettingsGroupAction(
                text = "Back Up Now",
                onClick = onBackupNow,
                enabled = !isBusy,
                showDividerBelow = false,
                testTag = MmrTestTags.backupBackupNow,
            )
        }

        MmrSettingsGroup(
            title = "Restore & Cloud",
            footer =
                "Import a Money Inbox backup, or a file from Money Manager or Monefy. " +
                    "Google Drive shows copies already uploaded so you can restore one.",
        ) {
            MmrSettingsGroupAction(
                text = "Import Backup",
                onClick = onImport,
                enabled = !isBusy,
                testTag = MmrTestTags.backupImport,
            )
            MmrSettingsGroupNavRow(
                label = "Google Drive Backup",
                onClick = onNavigateToGoogleDrive,
                showDividerBelow = false,
                testTag = MmrTestTags.googleDriveBackupLink,
            )
        }

        MmrSettingsGroup(
            title = "Spreadsheet",
            footer =
                "Download the sample spreadsheet, fill in the rows, then import it. " +
                    "Rows you already added are skipped.",
        ) {
            MmrSettingsGroupAction(
                text = "Download Import Template",
                onClick = onExportSpreadsheetTemplate,
                enabled = !isBusy,
            )
            MmrSettingsGroupAction(
                text = "Import Spreadsheet",
                onClick = onImportSpreadsheet,
                enabled = !isBusy,
                showDividerBelow = false,
            )
        }

        MmrSettingsGroup(
            title = "Migrate From Another App",
            footer = stringResource(R.string.backup_migration_section_footer, supportEmail),
        ) {
            MmrSettingsGroupNavRow(
                label = stringResource(R.string.backup_migration_request_row),
                value = supportEmail,
                onClick = { MmrDeveloperContactIntents.openMigrationImportRequest(context) },
                showDividerBelow = false,
                testTag = MmrTestTags.backupMigrationImportRequest,
            )
        }
    }
}
