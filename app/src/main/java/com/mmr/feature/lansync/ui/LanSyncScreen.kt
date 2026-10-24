package com.mmr.feature.lansync.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.fragment.app.FragmentActivity
import androidx.hilt.navigation.compose.hiltViewModel
import com.mmr.app.security.MmrDeviceCredentialGate
import com.mmr.app.testing.MmrTestTags
import com.mmr.app.ui.components.MmrContentDialog
import com.mmr.app.ui.components.MmrIosSettingsBusyIndicator
import com.mmr.app.ui.components.MmrIosSettingsCopyableCodeSlot
import com.mmr.app.ui.components.MmrIosSettingsDestructiveRow
import com.mmr.app.ui.components.MmrIosSettingsInlineErrorSection
import com.mmr.app.ui.components.MmrIosSettingsInlineTextRow
import com.mmr.app.ui.components.MmrIosSettingsLogLine
import com.mmr.app.ui.components.MmrIosSettingsNavRow
import com.mmr.app.ui.components.MmrIosSettingsPrimaryActionSlot
import com.mmr.app.ui.components.MmrIosSettingsSecondaryActionSlot
import com.mmr.app.ui.components.MmrIosSettingsSection
import com.mmr.app.ui.components.MmrIosSettingsSectionBodyText
import com.mmr.app.ui.components.MmrIosSettingsSwitchRow
import com.mmr.app.ui.components.MmrSettingsScreenLayout
import com.mmr.app.ui.components.MmrSettingsScreenScaffold
import com.mmr.app.ui.components.MmrSettingsStatusBanners
import com.mmr.core.data.preferences.LanSyncPeerMode
import com.mmr.feature.lansync.viewmodel.LanSyncSensitiveAction
import com.mmr.feature.lansync.viewmodel.LanSyncViewModel

@Composable
fun LanSyncScreen(
    onNavigateBack: () -> Unit,
    onNavigateHomeAfterRestore: () -> Unit = {},
    viewModel: LanSyncViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    var showAdvanced by rememberSaveable { mutableStateOf(false) }
    val context = LocalContext.current
    val activity = context as? FragmentActivity

    LaunchedEffect(viewModel) {
        viewModel.navigateHomeAfterRestore.collect {
            onNavigateHomeAfterRestore()
        }
    }

    LaunchedEffect(uiState.sensitiveAction) {
        val action = uiState.sensitiveAction ?: return@LaunchedEffect
        val host =
            activity ?: run {
                viewModel.clearSensitiveAction()
                return@LaunchedEffect
            }
        when (action) {
            LanSyncSensitiveAction.TRUST_DEVICE ->
                MmrDeviceCredentialGate.authenticate(
                    activity = host,
                    title = "Trust device",
                    subtitle = "Confirm to add a trusted sync device",
                    onSuccess = viewModel::completeTrustAfterAuth,
                    onFailure = viewModel::clearSensitiveAction,
                )
            LanSyncSensitiveAction.REMOVE_TRUSTED ->
                MmrDeviceCredentialGate.authenticate(
                    activity = host,
                    title = "Remove trusted device",
                    subtitle = "Confirm to revoke sync trust",
                    onSuccess = viewModel::completeRemoveTrustedAfterAuth,
                    onFailure = viewModel::clearSensitiveAction,
                )
        }
    }

    uiState.mergeConflicts?.let { conflicts ->
        val detail =
            buildList {
                if (conflicts.recurring.isNotEmpty()) {
                    add("Recurring rules changed on both devices (${conflicts.recurring.size}).")
                }
            }.joinToString("\n")
        MmrContentDialog(
            title = "Sync needs your choice",
            message = detail,
            confirmLabel = "Use other device",
            onConfirm = viewModel::resolveConflictsUseRemoteForAll,
            dismissLabel = "Keep mine",
            onDismiss = viewModel::resolveConflictsKeepLocalForAll,
            showDismissButton = true,
        )
    }

    uiState.wifiTrustPromptSsid?.let { ssid ->
        MmrContentDialog(
            title = "Trust this Wi‑Fi?",
            message = "Sync only runs on networks you trust. Trust \"$ssid\" for LAN sync?",
            confirmLabel = "Trust network",
            onConfirm = viewModel::confirmWifiTrust,
            onDismiss = viewModel::dismissWifiTrustPrompt,
        )
    }

    MmrSettingsScreenScaffold(
        title = "Peer Device Sync",
        onNavigateBack = onNavigateBack,
        layout = MmrSettingsScreenLayout.InsetGrouped,
    ) {
        MmrSettingsStatusBanners(
            statusMessage = uiState.statusMessage,
            errorMessage = uiState.errorMessage,
            onDismissStatus = viewModel::dismissMessages,
            onDismissError = viewModel::dismissMessages,
        )

        MmrIosSettingsSection(
            title = "Enable",
            isFirstInScreen = true,
            footer =
                "Approved ledger data syncs between trusted devices on the same Wi‑Fi. " +
                    "SMS and pending approvals stay on each phone. Tap Sync now when you are ready — no background sync.",
        ) {
            MmrIosSettingsSwitchRow(
                label = "Sync on same Wi‑Fi",
                checked = uiState.enabled,
                onCheckedChange = viewModel::setEnabled,
                switchTestTag = MmrTestTags.lanSyncEnabledSwitch,
                showDividerBelow = false,
            )
        }

        if (uiState.isBusy) {
            MmrIosSettingsBusyIndicator()
        }

        if (!uiState.onWifi) {
            MmrIosSettingsInlineErrorSection(
                message = "Connect to Wi‑Fi before syncing.",
                isFirstInScreen = false,
            )
        } else {
            MmrIosSettingsSection(
                title = "Network",
                footer = uiState.localSyncUrl?.let { "This phone: $it" },
            ) {
                uiState.wifiSsid?.let { ssid ->
                    MmrIosSettingsSectionBodyText(text = "Network: $ssid")
                }
            }
        }

        MmrIosSettingsSection(title = "This device") {
            MmrIosSettingsInlineTextRow(
                label = "Device name",
                value = uiState.displayName,
                onValueChange = viewModel::setDisplayName,
                placeholder = "Name",
            )
            MmrIosSettingsCopyableCodeSlot(
                fieldLabel = "Pairing code",
                value = uiState.pairingOfferText,
                showDividerBelow = false,
            )
        }

        MmrIosSettingsSection(title = "Trust a device") {
            MmrIosSettingsInlineTextRow(
                label = "Pairing code",
                value = uiState.pairingInput,
                onValueChange = viewModel::onPairingInputChanged,
                placeholder = "Code from other device",
                singleLine = true,
            )
            MmrIosSettingsSecondaryActionSlot(
                text = "Add trusted device",
                onClick = viewModel::requestTrustDevice,
                enabled = uiState.enabled && uiState.pairingInput.isNotBlank() && !uiState.isBusy,
                showDividerBelow = false,
            )
        }

        if (uiState.trustedDeviceLabels.isNotEmpty()) {
            MmrIosSettingsSection(
                title = "Trusted devices",
                footer = "Remove revokes sync trust for that device.",
            ) {
                uiState.trustedDeviceLabels.forEachIndexed { index, label ->
                    MmrIosSettingsDestructiveRow(
                        label = "Remove $label",
                        onClick = { viewModel.requestRemoveTrusted(index) },
                        showDividerBelow = index < uiState.trustedDeviceLabels.lastIndex,
                    )
                }
            }
        }

        if (uiState.discoveredLabels.isNotEmpty()) {
            MmrIosSettingsSection(title = "On this network") {
                uiState.discoveredLabels.forEach { label ->
                    MmrIosSettingsSectionBodyText(text = label)
                }
            }
        }

        MmrIosSettingsSection(
            title = "Sync options",
            footer =
                if (uiState.pendingApprovalCount > 0) {
                    "Pending SMS approvals are not synced (${uiState.pendingApprovalCount} waiting)."
                } else {
                    "Include attachments to sync receipt photos (may be slower on Wi‑Fi)."
                },
        ) {
            MmrIosSettingsSwitchRow(
                label = "Sync one device only",
                checked = uiState.peerMode == LanSyncPeerMode.SINGLE_DEVICE,
                onCheckedChange = { enabled ->
                    viewModel.setPeerMode(
                        if (enabled) LanSyncPeerMode.SINGLE_DEVICE else LanSyncPeerMode.ALL_TRUSTED_ON_LAN,
                    )
                },
            )
            if (uiState.peerMode == LanSyncPeerMode.SINGLE_DEVICE && uiState.trustedDeviceLabels.isNotEmpty()) {
                uiState.trustedDeviceLabels.forEachIndexed { index, label ->
                    val id = uiState.trustedDeviceIds.getOrNull(index)
                    MmrIosSettingsNavRow(
                        label = label,
                        value = if (id == uiState.selectedPeerDeviceId) "Selected" else "Select",
                        onClick = { if (id != null) viewModel.setSelectedPeer(id) },
                    )
                }
            }
            MmrIosSettingsSwitchRow(
                label = "Include attachments",
                checked = uiState.includeAttachments,
                onCheckedChange = viewModel::setIncludeAttachments,
                showDividerBelow = false,
            )
        }

        if (!uiState.hasLocalLedgerData && uiState.trustedDeviceLabels.isNotEmpty()) {
            MmrIosSettingsSection(
                title = "Restore on this phone",
                footer = "One-way pull from a trusted device when this phone has no ledger yet.",
            ) {
                uiState.trustedDeviceLabels.forEachIndexed { index, label ->
                    val restoreEnabled = uiState.enabled && uiState.onWifi && !uiState.isBusy
                    MmrIosSettingsSecondaryActionSlot(
                        text = "Replace empty ledger from $label",
                        onClick = { viewModel.restoreFromTrusted(index, navigateHomeOnSuccess = true) },
                        enabled = restoreEnabled,
                        showDividerBelow = index < uiState.trustedDeviceLabels.lastIndex,
                    )
                }
            }
        }

        MmrIosSettingsSection(
            title = "Sync now",
            footer = uiState.syncNowDisabledHint,
        ) {
            MmrIosSettingsPrimaryActionSlot(
                text = uiState.syncNowButtonText,
                onClick = viewModel::syncNow,
                enabled = uiState.syncNowEnabled,
                showDividerBelow = false,
            )
        }

        if (uiState.syncLogLines.isNotEmpty()) {
            MmrIosSettingsSection(title = "Recent syncs") {
                uiState.syncLogLines.forEach { line ->
                    MmrIosSettingsLogLine(
                        text = "${line.label} — ${line.detail}",
                        isError = !line.success,
                    )
                }
            }
        }

        MmrIosSettingsSection(title = null) {
            MmrIosSettingsNavRow(
                label = "Advanced",
                value = if (showAdvanced) "Hide" else "Show",
                onClick = { showAdvanced = !showAdvanced },
                showDividerBelow = false,
            )
        }

        if (showAdvanced) {
            MmrIosSettingsSection(
                title = "Advanced",
                footer =
                    if (uiState.hasLocalLedgerData && uiState.trustedDeviceIds.isNotEmpty()) {
                        "One-way pull replaces local ledger from a peer. Use Sync now for two-way merge."
                    } else {
                        null
                    },
            ) {
                if (uiState.hasLocalLedgerData && uiState.trustedDeviceIds.isNotEmpty()) {
                    uiState.trustedDeviceLabels.forEachIndexed { index, label ->
                        MmrIosSettingsNavRow(
                            label = "Replace ledger from $label",
                            value = "One-way pull",
                            onClick = { viewModel.restoreFromTrusted(index) },
                            showDividerBelow = index < uiState.trustedDeviceLabels.lastIndex,
                        )
                    }
                }
                MmrIosSettingsNavRow(
                    label = "Clear sync history",
                    value = "Clear",
                    onClick = viewModel::clearSyncLog,
                    showDividerBelow = false,
                )
            }
        }
    }
}
