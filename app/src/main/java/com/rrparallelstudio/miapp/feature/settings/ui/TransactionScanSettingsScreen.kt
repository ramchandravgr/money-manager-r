package com.rrparallelstudio.miapp.feature.settings.ui

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import com.rrparallelstudio.miapp.R
import com.rrparallelstudio.miapp.feature.settings.viewmodel.TransactionScanSettingsViewModel
import com.rrparallelstudio.miapp.testing.MmrTestTags
import com.rrparallelstudio.miapp.ui.components.MmrSettingsGroup
import com.rrparallelstudio.miapp.ui.components.MmrSettingsGroupDestructiveRow
import com.rrparallelstudio.miapp.ui.components.MmrSettingsGroupNavRow
import com.rrparallelstudio.miapp.ui.components.MmrSettingsGroupSlider
import com.rrparallelstudio.miapp.ui.components.MmrSettingsScreenLayout
import com.rrparallelstudio.miapp.ui.components.MmrSettingsScreenScaffold

@Composable
fun TransactionScanSettingsScreen(
    onNavigateBack: () -> Unit,
    onNavigateToSmsTemplateOverlay: () -> Unit = {},
    onNavigateToSetupGuide: () -> Unit = {},
    viewModel: TransactionScanSettingsViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val scanMonths by viewModel.scanMonths.collectAsState()
    val folderName by viewModel.folderDisplayName.collectAsState()
    val folderUri by viewModel.folderTreeUri.collectAsState()

    val folderLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.OpenDocumentTree(),
        ) { uri ->
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
                        // Provider may not support persistable permissions.
                    }
                }
                viewModel.onBackupFolderTreeSelected(uri)
            }
        }

    MmrSettingsScreenScaffold(
        title = "Transaction Scan",
        onNavigateBack = onNavigateBack,
        layout = MmrSettingsScreenLayout.InsetGrouped,
    ) {
        MmrSettingsGroup(
            title = "How Far Back To Read",
            isFirstInScreen = true,
            footer = "Approval Center only offers bank messages from this far back.",
        ) {
            val steps = (viewModel.maxScanMonths - viewModel.minScanMonths - 1).coerceAtLeast(0)
            MmrSettingsGroupSlider(
                caption = "Look Back",
                value = scanMonths.toFloat(),
                onValueChange = { viewModel.setScanMonths(it.toInt()) },
                valueRange = viewModel.minScanMonths.toFloat()..viewModel.maxScanMonths.toFloat(),
                steps = steps,
                minLabel = "${viewModel.minScanMonths} mo",
                maxLabel = "${viewModel.maxScanMonths} mo",
                showDividerBelow = false,
                sliderTestTag = MmrTestTags.settingsTransactionScanMonthsSlider,
            )
        }

        MmrSettingsGroup(
            title = "Privacy",
            footer = stringResource(R.string.transaction_scan_sms_data_declaration),
            showCard = false,
        ) {}

        MmrSettingsGroup(
            title = "SMS Backup & Restore",
            footer =
                "Choose the folder where SMS Backup & Restore saves messages. " +
                    "Money Inbox reads the newest file when you open the app or refresh Approval Center.",
        ) {
            MmrSettingsGroupNavRow(
                label = "Setup Guide",
                onClick = onNavigateToSetupGuide,
                testTag = MmrTestTags.settingsTransactionScanSbrGuide,
            )
            MmrSettingsGroupNavRow(
                label = "Backup Folder",
                value = folderName ?: "Not Set",
                onClick = {
                    val initial = folderUri
                    if (initial != null) {
                        folderLauncher.launch(initial)
                    } else {
                        folderLauncher.launch(null)
                    }
                },
                testTag = MmrTestTags.settingsTransactionScanBackupFolder,
                showDividerBelow = folderUri != null,
            )
            if (folderUri != null) {
                MmrSettingsGroupDestructiveRow(
                    label = "Forget Folder",
                    onClick = { viewModel.clearBackupFolder() },
                    showDividerBelow = false,
                )
            }
        }

        MmrSettingsGroup(
            title = "Advanced",
            footer = "Most people can skip this. Open it only if a bank message is not recognized.",
        ) {
            MmrSettingsGroupNavRow(
                label = "SMS Templates",
                onClick = onNavigateToSmsTemplateOverlay,
                showDividerBelow = false,
            )
        }
    }
}
