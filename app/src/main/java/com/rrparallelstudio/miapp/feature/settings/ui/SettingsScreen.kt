package com.rrparallelstudio.miapp.feature.settings.ui

import androidx.compose.runtime.Composable
import com.rrparallelstudio.miapp.core.parser.policy.MmrSmsTelephonyPolicy
import com.rrparallelstudio.miapp.testing.MmrTestTags
import com.rrparallelstudio.miapp.ui.components.MmrIosSettingsNavRow
import com.rrparallelstudio.miapp.ui.components.MmrIosSettingsSection
import com.rrparallelstudio.miapp.ui.components.MmrSettingsFieldGrouping
import com.rrparallelstudio.miapp.ui.components.MmrSettingsScreenLayout
import com.rrparallelstudio.miapp.ui.components.MmrSettingsScreenScaffold

@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    onNavigateToConfiguration: () -> Unit,
    onNavigateToBackup: () -> Unit,
    onNavigateToLanSync: () -> Unit,
    onNavigateToWebManage: () -> Unit,
    onNavigateToCategories: () -> Unit,
    onNavigateToAccounts: () -> Unit,
    onNavigateToTransactionScan: () -> Unit = {},
    onNavigateToSmsTemplateOverlay: () -> Unit = {},
    onReplayIntroduction: () -> Unit = {},
    showBackNavigation: Boolean = true,
) {
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
            )
            MmrIosSettingsNavRow(
                label = "Replay introduction",
                onClick = onReplayIntroduction,
                testTag = MmrTestTags.settingsReplayIntroduction,
                showDividerBelow = false,
            )
        }

        MmrIosSettingsSection(
            title = "Approvals",
        ) {
            MmrIosSettingsNavRow(
                label = "Transaction scan",
                onClick = onNavigateToTransactionScan,
                testTag = MmrTestTags.settingsNavTransactionScan,
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

        if (MmrSmsTelephonyPolicy.SETTINGS_SMS_APPROVALS_SECTION_VISIBLE) {
            MmrIosSettingsSection(
                title = "SMS approvals",
                footer =
                    "Inbox scan and live SMS capture are not available in this version. " +
                        "Paste a bank SMS from Approval Center. Import from SMS Backup & Restore XML is planned.",
            ) {
                MmrIosSettingsNavRow(
                    label = "SMS template overlay",
                    onClick = onNavigateToSmsTemplateOverlay,
                    showDividerBelow = false,
                )
            }
        }
    }
}
