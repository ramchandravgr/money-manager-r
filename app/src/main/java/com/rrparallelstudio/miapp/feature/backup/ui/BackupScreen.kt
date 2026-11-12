package com.rrparallelstudio.miapp.feature.backup.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import com.rrparallelstudio.miapp.R
import com.rrparallelstudio.miapp.core.backup.BackupImportMode
import com.rrparallelstudio.miapp.core.backup.BackupSignedZipCodec
import com.rrparallelstudio.miapp.core.backup.model.BackupRestoreSummary
import com.rrparallelstudio.miapp.core.drive.GoogleDriveAutoSchedule
import com.rrparallelstudio.miapp.core.platform.MmrDeveloperContactIntents
import com.rrparallelstudio.miapp.feature.backup.model.BackupUiEvent
import com.rrparallelstudio.miapp.feature.backup.viewmodel.BackupViewModel
import com.rrparallelstudio.miapp.testing.MmrTestTags
import com.rrparallelstudio.miapp.ui.components.MmrConfirmDialog
import com.rrparallelstudio.miapp.ui.components.MmrIosSettingsBackupImportModeSheet
import com.rrparallelstudio.miapp.ui.components.MmrIosSettingsNavRow
import com.rrparallelstudio.miapp.ui.components.MmrIosSettingsPrimaryActionSlot
import com.rrparallelstudio.miapp.ui.components.MmrIosSettingsRadioRow
import com.rrparallelstudio.miapp.ui.components.MmrIosSettingsSecondaryActionSlot
import com.rrparallelstudio.miapp.ui.components.MmrIosSettingsSection
import com.rrparallelstudio.miapp.ui.components.MmrIosSettingsSectionBodyText
import com.rrparallelstudio.miapp.ui.components.MmrIosSettingsValueRow
import com.rrparallelstudio.miapp.ui.components.MmrProcessingOverlay
import com.rrparallelstudio.miapp.ui.components.MmrSettingsScreenLayout
import com.rrparallelstudio.miapp.ui.components.MmrSettingsScreenScaffold
import com.rrparallelstudio.miapp.ui.components.MmrSettingsStatusBanners
import com.rrparallelstudio.miapp.ui.state.MmrErrorState
import com.rrparallelstudio.miapp.ui.state.MmrLoadingState
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackupScreen(
    onNavigateBack: () -> Unit,
    onNavigateToGoogleDrive: () -> Unit = {},
    viewModel: BackupViewModel = hiltViewModel(),
    testImportUri: Uri? = null,
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val importSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    LaunchedEffect(testImportUri) {
        testImportUri?.let { uri ->
            viewModel.onEvent(
                BackupUiEvent.OnImportUriSelected(uri, "mmr-test-import.json"),
            )
        }
    }

    val exportLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.CreateDocument("application/zip"),
        ) { uri: Uri? ->
            if (uri != null) {
                viewModel.onEvent(BackupUiEvent.OnExportToUri(uri))
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
            title = "Import Spreadsheet?",
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
                        localAutoSchedule = uiState.localAutoSchedule,
                        lastLocalBackupLabel = uiState.lastLocalBackupLabel,
                        onSetLocalAutoSchedule = {
                            viewModel.onEvent(BackupUiEvent.OnSetLocalAutoSchedule(it))
                        },
                        onLocalBackupNow = { viewModel.onEvent(BackupUiEvent.OnLocalBackupNow) },
                        statusMessage = uiState.statusMessage,
                        errorMessage = uiState.errorMessage,
                        onDismissMessage = { viewModel.onEvent(BackupUiEvent.OnDismissMessage) },
                        onExport = {
                            val stamp =
                                SimpleDateFormat("yyyyMMdd-HHmmss", Locale.getDefault())
                                    .format(Date())
                            exportLauncher.launch(
                                "money-inbox-backup-$stamp.${BackupSignedZipCodec.SUGGESTED_FILE_EXTENSION}",
                            )
                        },
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
    onExport: () -> Unit,
    onImport: () -> Unit,
    onNavigateToGoogleDrive: () -> Unit,
    onExportSpreadsheetTemplate: () -> Unit,
    onImportSpreadsheet: () -> Unit,
    localAutoSchedule: GoogleDriveAutoSchedule,
    lastLocalBackupLabel: String?,
    onSetLocalAutoSchedule: (GoogleDriveAutoSchedule) -> Unit,
    onLocalBackupNow: () -> Unit,
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
        MmrIosSettingsSection(
            title = "About backups",
            isFirstInScreen = true,
            footer =
                "Includes transactions, accounts, categories, budgets, loans, recurring rules, processed SMS, " +
                    "approval history, and app settings. Pending approvals are not included.",
        ) {
            if (isEmpty) {
                MmrIosSettingsSectionBodyText(
                    text = "No local data yet. Add transactions from Home or import a backup file.",
                )
            } else {
                summary?.let { s ->
                    MmrIosSettingsValueRow(label = "Total records", value = s.totalRecords.toString())
                    MmrIosSettingsValueRow(label = "Transactions", value = s.transactions.toString())
                    MmrIosSettingsValueRow(label = "Accounts", value = s.accounts.toString())
                    MmrIosSettingsValueRow(label = "Categories", value = s.categories.toString())
                    MmrIosSettingsValueRow(label = "Loans", value = s.loans.toString())
                    MmrIosSettingsValueRow(label = "Recurring rules", value = s.recurringRules.toString())
                    MmrIosSettingsValueRow(label = "Budgets", value = s.budgets.toString())
                    MmrIosSettingsValueRow(
                        label = "SMS / approvals",
                        value = (s.smsMessageRecords + s.transactionApprovals).toString(),
                        showDividerBelow = false,
                    )
                }
            }
        }

        MmrIosSettingsSection(
            title = "Automatic Local Backup",
            footer =
                buildString {
                    append(
                        "Saves signed .mibxbac files to Downloads on a schedule (keeps the latest ",
                    )
                    append("${com.rrparallelstudio.miapp.core.backup.LocalAutoBackupContracts.MAX_RETAINED_FILES}). ")
                    lastLocalBackupLabel?.let { append("Last backup: $it") }
                }.takeIf { it.isNotBlank() },
        ) {
            MmrIosSettingsRadioRow(
                label = "Off",
                selected = localAutoSchedule == GoogleDriveAutoSchedule.OFF,
                onSelect = { onSetLocalAutoSchedule(GoogleDriveAutoSchedule.OFF) },
            )
            MmrIosSettingsRadioRow(
                label = "Daily",
                selected = localAutoSchedule == GoogleDriveAutoSchedule.DAILY,
                onSelect = { onSetLocalAutoSchedule(GoogleDriveAutoSchedule.DAILY) },
            )
            MmrIosSettingsRadioRow(
                label = "Weekly",
                selected = localAutoSchedule == GoogleDriveAutoSchedule.WEEKLY,
                onSelect = { onSetLocalAutoSchedule(GoogleDriveAutoSchedule.WEEKLY) },
            )
            MmrIosSettingsRadioRow(
                label = "Monthly",
                selected = localAutoSchedule == GoogleDriveAutoSchedule.MONTHLY,
                onSelect = { onSetLocalAutoSchedule(GoogleDriveAutoSchedule.MONTHLY) },
            )
            MmrIosSettingsPrimaryActionSlot(
                text = "Back Up Now to Downloads",
                onClick = onLocalBackupNow,
                enabled = !isBusy,
                showDividerBelow = false,
            )
        }

        MmrIosSettingsSection(
            title = "Actions",
            footer =
                "Export saves a signed .mibxbac file. Import also accepts Money Manager `.mmbak` exports " +
                    "and Monefy backup files (merge into this ledger; nothing on this phone is erased). " +
                    "Google Drive opens cloud backup settings.",
        ) {
            MmrIosSettingsPrimaryActionSlot(
                text = "Export backup",
                onClick = onExport,
                enabled = !isBusy,
                testTag = MmrTestTags.backupExport,
            )
            MmrIosSettingsSecondaryActionSlot(
                text = "Import backup",
                onClick = onImport,
                enabled = !isBusy,
                testTag = MmrTestTags.backupImport,
            )
            MmrIosSettingsNavRow(
                label = "Google Drive backup",
                onClick = onNavigateToGoogleDrive,
                showDividerBelow = false,
                testTag = MmrTestTags.googleDriveBackupLink,
            )
        }

        MmrIosSettingsSection(
            title = "Ledger Spreadsheet",
            footer =
                "Download the Excel template, fill rows using the same columns as Home → export, " +
                    "then import .xlsx or .xml exports here (merge only).",
        ) {
            MmrIosSettingsSecondaryActionSlot(
                text = "Download Import Template",
                onClick = onExportSpreadsheetTemplate,
                enabled = !isBusy,
            )
            MmrIosSettingsSecondaryActionSlot(
                text = "Import Spreadsheet",
                onClick = onImportSpreadsheet,
                enabled = !isBusy,
                showDividerBelow = false,
            )
        }

        MmrIosSettingsSection(
            title = "Migrate from another app",
            footer = stringResource(R.string.backup_migration_section_footer, supportEmail),
        ) {
            MmrIosSettingsNavRow(
                label = stringResource(R.string.backup_migration_request_row),
                value = supportEmail,
                onClick = { MmrDeveloperContactIntents.openMigrationImportRequest(context) },
                showDividerBelow = false,
                testTag = MmrTestTags.backupMigrationImportRequest,
            )
        }
    }
}
