package com.rrparallelstudio.miapp.feature.settings.ui

import androidx.compose.runtime.Composable
import com.rrparallelstudio.miapp.testing.MmrTestTags
import com.rrparallelstudio.miapp.ui.components.MmrSettingsGroup
import com.rrparallelstudio.miapp.ui.components.MmrSettingsGroupNavRow
import com.rrparallelstudio.miapp.ui.components.MmrSettingsScreenLayout
import com.rrparallelstudio.miapp.ui.components.MmrSettingsScreenScaffold

@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    onNavigateToHomeMenu: () -> Unit,
    onNavigateToAppearance: () -> Unit,
    onNavigateToSecurity: () -> Unit,
    onNavigateToBackup: () -> Unit,
    onNavigateToLanSync: () -> Unit,
    onNavigateToWebManage: () -> Unit,
    onNavigateToCategories: () -> Unit,
    onNavigateToAccounts: () -> Unit,
    onNavigateToTransactionScan: () -> Unit,
    onReplayIntroduction: () -> Unit,
    showBackNavigation: Boolean = true,
) {
    MmrSettingsScreenScaffold(
        title = "Settings",
        onNavigateBack = onNavigateBack,
        showBackNavigation = showBackNavigation,
        layout = MmrSettingsScreenLayout.InsetGrouped,
    ) {
        MmrSettingsGroup(
            title = "General",
            isFirstInScreen = true,
        ) {
            MmrSettingsGroupNavRow(
                label = "Home & Menu",
                onClick = onNavigateToHomeMenu,
                testTag = MmrTestTags.settingsNavHomeMenu,
            )
            MmrSettingsGroupNavRow(
                label = "Appearance",
                onClick = onNavigateToAppearance,
                testTag = MmrTestTags.settingsNavAppearance,
            )
            MmrSettingsGroupNavRow(
                label = "Security",
                onClick = onNavigateToSecurity,
                testTag = MmrTestTags.settingsNavSecurity,
            )
            MmrSettingsGroupNavRow(
                label = "Accounts",
                onClick = onNavigateToAccounts,
                testTag = MmrTestTags.settingsNavAccounts,
            )
            MmrSettingsGroupNavRow(
                label = "Categories",
                onClick = onNavigateToCategories,
                testTag = MmrTestTags.settingsNavCategories,
            )
            MmrSettingsGroupNavRow(
                label = "Replay Introduction",
                onClick = onReplayIntroduction,
                testTag = MmrTestTags.settingsReplayIntroduction,
                showDividerBelow = false,
            )
        }

        MmrSettingsGroup(
            title = "Approvals",
            footer = "Bank messages you bring in are checked here, then offered in Approval Center.",
        ) {
            MmrSettingsGroupNavRow(
                label = "Transaction Scan",
                onClick = onNavigateToTransactionScan,
                testTag = MmrTestTags.settingsNavTransactionScan,
                showDividerBelow = false,
            )
        }

        MmrSettingsGroup(
            title = "Backup & Sync",
            footer =
                "Save a copy on this phone, send that copy to Google Drive, share with another phone, " +
                    "or open Money Inbox in a browser on the same Wi‑Fi.",
        ) {
            MmrSettingsGroupNavRow(
                label = "Backup & Restore",
                onClick = onNavigateToBackup,
                testTag = MmrTestTags.settingsNavBackup,
            )
            MmrSettingsGroupNavRow(
                label = "Peer Device Sync",
                onClick = onNavigateToLanSync,
                testTag = MmrTestTags.settingsNavLanSync,
            )
            MmrSettingsGroupNavRow(
                label = "Web Manage",
                onClick = onNavigateToWebManage,
                testTag = MmrTestTags.settingsNavWebManage,
                showDividerBelow = false,
            )
        }
    }
}
