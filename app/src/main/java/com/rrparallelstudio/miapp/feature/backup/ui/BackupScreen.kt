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
import com.rrparallelstudio.miapp.core.platform.MmrDeveloperContactIntents
import com.rrparallelstudio.miapp.feature.backup.model.BackupUiEvent
import com.rrparallelstudio.miapp.feature.backup.viewmodel.BackupViewModel
import com.rrparallelstudio.miapp.testing.MmrTestTags
import com.rrparallelstudio.miapp.ui.components.MmrIosSettingsBackupImportModeSheet
import com.rrparallelstudio.miapp.ui.components.MmrIosSettingsBusyIndicator
import com.rrparallelstudio.miapp.ui.components.MmrIosSettingsNavRow
import com.rrparallelstudio.miapp.ui.components.MmrIosSettingsPrimaryActionSlot
import com.rrparallelstudio.miapp.ui.components.MmrIosSettingsSecondaryActionSlot
import com.rrparallelstudio.miapp.ui.components.MmrIosSettingsSection
import com.rrparallelstudio.miapp.ui.components.MmrIosSettingsSectionBodyText
import com.rrparallelstudio.miapp.ui.components.MmrIosSettingsValueRow
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
        MmrIosSettingsBackupImportModeSheet(
            title = if (isMoneyManager) "Import Money Manager" else "Import backup",
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
                    isMoneyManager -> "Import transactions"
                    uiState.importMode == BackupImportMode.REPLACE -> "Replace & restore"
                    else -> "Merge"
                },
            confirmTestTag = MmrTestTags.backupImportConfirm,
            replaceModeTestTag = MmrTestTags.backupImportModeReplace,
            mergeModeTestTag = MmrTestTags.backupImportModeMerge,
            showReplaceOption = !isMoneyManager,
            mergeHelper =
                if (isMoneyManager) {
                    "Reads the export on this device only. Income and expense rows merge into your " +
                        "ledger; duplicates are skipped. Transfers and other Money Manager row types are not imported."
                } else {
                    null
                },
        )
    }

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
                    onNavigateBack = onNavigateBack,
                    layout = MmrSettingsScreenLayout.InsetGrouped,
                ) {
                    BackupContent(
                        summary = uiState.summary,
                        isEmpty = uiState.isEmpty,
                        isBusy = uiState.isBusy,
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
        if (isBusy) {
            MmrIosSettingsBusyIndicator()
        }
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
            title = "Actions",
            footer =
                "Export saves a signed .mibxbac file. Import also accepts Money Manager `.mmbak` exports " +
                    "(merge transactions only). Google Drive opens cloud backup settings.",
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
