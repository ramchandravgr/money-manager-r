package com.mmr.feature.settings.ui

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.mmr.app.testing.MmrTestTags
import com.mmr.app.ui.components.MmrContentDialog
import com.mmr.app.ui.components.MmrIosSettingsNavRow
import com.mmr.app.ui.components.MmrIosSettingsSection
import com.mmr.app.ui.components.MmrIosSettingsSliderSlot
import com.mmr.app.ui.components.MmrIosSettingsSwitchRow
import com.mmr.app.ui.components.MmrSettingsFieldGrouping
import com.mmr.app.ui.components.MmrSettingsScreenLayout
import com.mmr.app.ui.components.MmrSettingsScreenScaffold
import com.mmr.core.parser.permission.SmsPermissionHelper
import com.mmr.feature.settings.viewmodel.SettingsViewModel
import kotlin.math.roundToInt

@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    onNavigateToConfiguration: () -> Unit,
    onNavigateToBackup: () -> Unit,
    onNavigateToLanSync: () -> Unit,
    onNavigateToWebManage: () -> Unit,
    onNavigateToCategories: () -> Unit,
    onNavigateToAccounts: () -> Unit,
    onNavigateToSmsTemplateOverlay: () -> Unit = {},
    showBackNavigation: Boolean = true,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val smsScanMonths by viewModel.smsScanMonths.collectAsState()
    val smsNotifyOnNewPending by viewModel.smsNotifyOnNewPending.collectAsState()
    val context = LocalContext.current
    var showReadSmsOptionalDialog by remember { mutableStateOf(false) }
    var showLiveInboundPermissionDeniedDialog by remember { mutableStateOf(false) }
    val liveInboundMissing = SmsPermissionHelper.missingForLiveInbound(context)

    fun openAppPermissionSettings() {
        val intent =
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.fromParts("package", context.packageName, null)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        context.startActivity(intent)
    }

    fun finishLiveSmsNotifyEnableAfterReceiveGranted() {
        when {
            SmsPermissionHelper.hasSmsPermission(context) -> viewModel.setSmsNotifyOnNewPending(true)
            viewModel.hasReadSmsInboundPromptBeenShown() -> viewModel.setSmsNotifyOnNewPending(true)
            else -> showReadSmsOptionalDialog = true
        }
    }

    val receiveSmsPermissionLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestPermission(),
            onResult = { granted ->
                if (granted) {
                    finishLiveSmsNotifyEnableAfterReceiveGranted()
                } else {
                    showLiveInboundPermissionDeniedDialog = true
                }
            },
        )

    fun completeLiveSmsNotifyEnable() {
        if (!SmsPermissionHelper.hasReceiveSmsPermission(context)) {
            receiveSmsPermissionLauncher.launch(SmsPermissionHelper.LIVE_RECEIVE_SMS_PERMISSION)
            return
        }
        finishLiveSmsNotifyEnableAfterReceiveGranted()
    }

    val readSmsPermissionLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestPermission(),
            onResult = {
                viewModel.markReadSmsInboundPromptShown()
                viewModel.setSmsNotifyOnNewPending(true)
            },
        )

    val notifyPermissionLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestPermission(),
            onResult = { granted ->
                if (granted) {
                    completeLiveSmsNotifyEnable()
                } else {
                    showLiveInboundPermissionDeniedDialog = true
                }
            },
        )

    if (showLiveInboundPermissionDeniedDialog) {
        MmrContentDialog(
            title = "Permission needed for live SMS",
            message =
                "Android did not grant access, or it was turned off earlier in system settings. " +
                    "Open App permissions and allow SMS (receive) and Notifications for Money Inbox.",
            onDismiss = { showLiveInboundPermissionDeniedDialog = false },
            confirmLabel = "Open app permissions",
            onConfirm = {
                showLiveInboundPermissionDeniedDialog = false
                openAppPermissionSettings()
            },
            showDismissButton = true,
            dismissLabel = "Not now",
        )
    }

    if (showReadSmsOptionalDialog) {
        MmrContentDialog(
            title = "Allow SMS inbox access?",
            message =
                "Optional. Money Inbox can read your inbox the same way as a manual scan in Approval Center, " +
                    "so account and card mapping stay consistent. Live bank-SMS capture uses a separate " +
                    "Receive SMS permission; inbox read is only for mapping and manual scan.",
            onDismiss = {
                showReadSmsOptionalDialog = false
                viewModel.markReadSmsInboundPromptShown()
                viewModel.setSmsNotifyOnNewPending(true)
            },
            confirmLabel = "Allow inbox access",
            onConfirm = {
                showReadSmsOptionalDialog = false
                readSmsPermissionLauncher.launch(Manifest.permission.READ_SMS)
            },
            showDismissButton = true,
            dismissLabel = "Skip",
        )
    }

    MmrSettingsScreenScaffold(
        title = "Settings",
        onNavigateBack = onNavigateBack,
        showBackNavigation = showBackNavigation,
        layout = MmrSettingsScreenLayout.InsetGrouped,
        fieldGrouping = MmrSettingsFieldGrouping.Grouped,
    ) {
        MmrIosSettingsSection(
            title = "General",
            isFirstInScreen = true,
        ) {
            MmrIosSettingsNavRow(
                label = "Transaction Appearance",
                onClick = onNavigateToConfiguration,
            )
            MmrIosSettingsNavRow(
                label = "Accounts",
                onClick = onNavigateToAccounts,
                testTag = MmrTestTags.settingsNavAccounts,
            )
            MmrIosSettingsNavRow(
                label = "Categories",
                onClick = onNavigateToCategories,
                testTag = MmrTestTags.settingsNavCategories,
                showDividerBelow = false,
            )
        }

        MmrIosSettingsSection(
            title = "Backup & sync",
            footer = "Back up locally or sync with another phone on the same Wi‑Fi. Web Manage works from a PC browser.",
        ) {
            MmrIosSettingsNavRow(
                label = "Backup & Restore",
                onClick = onNavigateToBackup,
                testTag = MmrTestTags.settingsNavBackup,
            )
            MmrIosSettingsNavRow(
                label = "Peer Device Sync",
                onClick = onNavigateToLanSync,
                testTag = MmrTestTags.settingsNavLanSync,
            )
            MmrIosSettingsNavRow(
                label = "Web Manage",
                onClick = onNavigateToWebManage,
                testTag = MmrTestTags.settingsNavWebManage,
                showDividerBelow = false,
            )
        }

        MmrIosSettingsSection(
            title = "SMS approvals",
            footer =
                when {
                    smsNotifyOnNewPending && liveInboundMissing.isEmpty() ->
                        "Live capture is on. Receive SMS and notifications are already allowed, " +
                            "so the system will not show a prompt when you toggle off and on again."
                    smsNotifyOnNewPending ->
                        "Live capture is on but still needs: " +
                            liveInboundMissing.joinToString { it.displayLabel() } +
                            ". Use App permissions below if no system prompt appeared."
                    else ->
                        "Turn on notify to capture bank SMS as they arrive. Inbox read is optional " +
                            "(manual scan and account mapping)."
                },
        ) {
            MmrIosSettingsSliderSlot(
                caption = "Inbox scan: $smsScanMonths month${if (smsScanMonths == 1) "" else "s"}",
                value = smsScanMonths.toFloat(),
                onValueChange = { value -> viewModel.setSmsScanMonths(value.roundToInt()) },
                valueRange = viewModel.minScanMonths.toFloat()..viewModel.maxScanMonths.toFloat(),
                steps = viewModel.maxScanMonths - viewModel.minScanMonths - 1,
                minLabel = "${viewModel.minScanMonths} Mo",
                maxLabel = "${viewModel.maxScanMonths} Mos",
            )
            MmrIosSettingsSwitchRow(
                label = "Notify for new approvals",
                checked = smsNotifyOnNewPending,
                onCheckedChange = { enabled ->
                    if (!enabled) {
                        viewModel.setSmsNotifyOnNewPending(false)
                        return@MmrIosSettingsSwitchRow
                    }
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        val granted =
                            ContextCompat.checkSelfPermission(
                                context,
                                Manifest.permission.POST_NOTIFICATIONS,
                            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
                        if (granted) {
                            completeLiveSmsNotifyEnable()
                        } else {
                            notifyPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                    } else {
                        completeLiveSmsNotifyEnable()
                    }
                },
                showDividerBelow = true,
            )
            if (smsNotifyOnNewPending && liveInboundMissing.isNotEmpty()) {
                MmrIosSettingsNavRow(
                    label = "App permissions",
                    onClick = { openAppPermissionSettings() },
                    showDividerBelow = true,
                )
            }
            MmrIosSettingsNavRow(
                label = "SMS template overlay",
                onClick = onNavigateToSmsTemplateOverlay,
                showDividerBelow = false,
            )
        }
    }
}

private fun SmsPermissionHelper.LiveInboundPermission.displayLabel(): String =
    when (this) {
        SmsPermissionHelper.LiveInboundPermission.RECEIVE_SMS -> "Receive SMS"
        SmsPermissionHelper.LiveInboundPermission.POST_NOTIFICATIONS -> "Notifications"
    }
