package com.rrparallelstudio.miapp.feature.settings.ui

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import com.rrparallelstudio.miapp.R
import com.rrparallelstudio.miapp.feature.settings.viewmodel.TransactionScanSettingsViewModel
import com.rrparallelstudio.miapp.testing.MmrTestTags
import com.rrparallelstudio.miapp.ui.components.MmrIosSettingsDestructiveRow
import com.rrparallelstudio.miapp.ui.components.MmrIosSettingsNavRow
import com.rrparallelstudio.miapp.ui.components.MmrIosSettingsPrimaryActionSlot
import com.rrparallelstudio.miapp.ui.components.MmrIosSettingsSection
import com.rrparallelstudio.miapp.ui.components.MmrIosSettingsSliderSlot
import com.rrparallelstudio.miapp.ui.components.MmrSettingsScreenLayout
import com.rrparallelstudio.miapp.ui.components.MmrSettingsScreenScaffold

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionScanSettingsScreen(
    onNavigateBack: () -> Unit,
    onNavigateToSmsTemplateOverlay: () -> Unit = {},
    viewModel: TransactionScanSettingsViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val scanMonths by viewModel.scanMonths.collectAsState()
    val folderName by viewModel.folderDisplayName.collectAsState()
    val folderUri by viewModel.folderTreeUri.collectAsState()
    var showSbrGuide by remember { mutableStateOf(false) }
    val sbrGuideSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

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
        title = "Transaction scan",
        onNavigateBack = onNavigateBack,
        layout = MmrSettingsScreenLayout.InsetGrouped,
    ) {
        MmrIosSettingsSection(
            title = "Scan range",
            isFirstInScreen = true,
            footer =
                "When importing SMS Backup & Restore XML, only bank messages within this window " +
                    "are considered. Approval Center refresh and app launch use the same range.",
        ) {
            val steps = (viewModel.maxScanMonths - viewModel.minScanMonths - 1).coerceAtLeast(0)
            MmrIosSettingsSliderSlot(
                caption = "Look back",
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

        MmrIosSettingsSection(
            title = "Data use",
            footer = stringResource(R.string.transaction_scan_sms_data_declaration),
        ) {}

        MmrIosSettingsSection(
            title = "SMS backups",
            footer =
                "Choose the folder where SMS Backup & Restore writes XML (hourly overwrite of one file is fine). " +
                    "The app reads the latest compatible sms-*.xml when you refresh Approval Center or open the app.",
        ) {
            MmrIosSettingsNavRow(
                label = "SMS Backup & Restore setup",
                value = "Step-by-step guide",
                onClick = { showSbrGuide = true },
                testTag = MmrTestTags.settingsTransactionScanSbrGuide,
            )
            MmrIosSettingsNavRow(
                label = "Backup folder",
                value = folderName ?: "Not set",
                onClick = {
                    val initial = folderUri
                    if (initial != null) {
                        folderLauncher.launch(initial)
                    } else {
                        folderLauncher.launch(null)
                    }
                },
                testTag = MmrTestTags.settingsTransactionScanBackupFolder,
            )
            MmrIosSettingsPrimaryActionSlot(
                text = if (folderUri == null) "Choose folder…" else "Change folder…",
                onClick = {
                    val initial = folderUri
                    if (initial != null) {
                        folderLauncher.launch(initial)
                    } else {
                        folderLauncher.launch(null)
                    }
                },
                showDividerBelow = folderUri != null,
            )
            if (folderUri != null) {
                MmrIosSettingsDestructiveRow(
                    label = "Clear folder",
                    onClick = { viewModel.clearBackupFolder() },
                    showDividerBelow = false,
                )
            }
        }

        MmrIosSettingsSection(
            title = "Advanced",
            footer = "Optional JSON rules layered on built-in bank SMS templates.",
        ) {
            MmrIosSettingsNavRow(
                label = "SMS template overlay",
                onClick = onNavigateToSmsTemplateOverlay,
                showDividerBelow = false,
            )
        }
    }

    if (showSbrGuide) {
        SmsBackupRestoreSetupGuideSheet(
            sheetState = sbrGuideSheetState,
            onDismiss = { showSbrGuide = false },
            onChooseBackupFolder = {
                val initial = folderUri
                if (initial != null) {
                    folderLauncher.launch(initial)
                } else {
                    folderLauncher.launch(null)
                }
            },
        )
    }
}
