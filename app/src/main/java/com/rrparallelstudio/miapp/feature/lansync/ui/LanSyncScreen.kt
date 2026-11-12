package com.rrparallelstudio.miapp.feature.lansync.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import androidx.hilt.navigation.compose.hiltViewModel
import com.rrparallelstudio.miapp.core.data.preferences.LanSyncPeerMode
import com.rrparallelstudio.miapp.feature.lansync.viewmodel.LanSyncSensitiveAction
import com.rrparallelstudio.miapp.feature.lansync.viewmodel.LanSyncViewModel
import com.rrparallelstudio.miapp.security.MmrDeviceCredentialGate
import com.rrparallelstudio.miapp.testing.MmrTestTags
import com.rrparallelstudio.miapp.ui.components.MmrContentDialog
import com.rrparallelstudio.miapp.ui.components.MmrIosSettingsBusyIndicator
import com.rrparallelstudio.miapp.ui.components.MmrSettingsGroup
import com.rrparallelstudio.miapp.ui.components.MmrSettingsGroupAction
import com.rrparallelstudio.miapp.ui.components.MmrSettingsGroupCode
import com.rrparallelstudio.miapp.ui.components.MmrSettingsGroupLabelRow
import com.rrparallelstudio.miapp.ui.components.MmrSettingsGroupNavRow
import com.rrparallelstudio.miapp.ui.components.MmrSettingsGroupNote
import com.rrparallelstudio.miapp.ui.components.MmrSettingsGroupSwitchRow
import com.rrparallelstudio.miapp.ui.components.MmrSettingsGroupTextField
import com.rrparallelstudio.miapp.ui.components.MmrSettingsGroupValueRow
import com.rrparallelstudio.miapp.ui.components.MmrSettingsGroupWarn
import com.rrparallelstudio.miapp.ui.components.MmrSettingsScreenLayout
import com.rrparallelstudio.miapp.ui.components.MmrSettingsScreenScaffold
import com.rrparallelstudio.miapp.ui.components.MmrSettingsStatusBanners

@Composable
fun LanSyncScreen(
    onNavigateBack: () -> Unit,
    onNavigateHomeAfterRestore: () -> Unit = {},
    onNavigateToHistory: () -> Unit = {},
    viewModel: LanSyncViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val activity = context as? FragmentActivity

    LaunchedEffect(viewModel) {
        viewModel.navigateHomeAfterRestore.collect {
            onNavigateHomeAfterRestore()
        }
    }

    LaunchedEffect(uiState.enabled) {
        if (!uiState.enabled) return@LaunchedEffect
        viewModel.refreshPeerDiscovery()
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
                    subtitle = "Confirm to let this phone sync with you",
                    onSuccess = viewModel::completeTrustAfterAuth,
                    onFailure = viewModel::clearSensitiveAction,
                )
            LanSyncSensitiveAction.REMOVE_TRUSTED ->
                MmrDeviceCredentialGate.authenticate(
                    activity = host,
                    title = "Remove trusted device",
                    subtitle = "Confirm to stop syncing with this phone",
                    onSuccess = viewModel::completeRemoveTrustedAfterAuth,
                    onFailure = viewModel::clearSensitiveAction,
                )
            LanSyncSensitiveAction.APPROVE_PAIR_REQUEST ->
                MmrDeviceCredentialGate.authenticate(
                    activity = host,
                    title = "Approve sync device",
                    subtitle = "Confirm to let this phone sync with you",
                    onSuccess = viewModel::completeApprovePairAfterAuth,
                    onFailure = viewModel::clearSensitiveAction,
                )
        }
    }

    uiState.wifiTrustPromptSsid?.let { ssid ->
        MmrContentDialog(
            title = "Trust this Wi‑Fi?",
            message = "Sync runs only on Wi‑Fi you allow. Allow \"$ssid\"?",
            confirmLabel = "Trust network",
            onConfirm = viewModel::confirmWifiTrust,
            onDismiss = viewModel::dismissWifiTrustPrompt,
        )
    }

    val address = lanHostPort(uiState.localSyncUrl)
    val canTrust =
        uiState.enabled &&
            uiState.pairingInput.isNotBlank() &&
            !uiState.isBusy &&
            (
                uiState.selectedPairingPeerAddress != null ||
                    uiState.pairingManualPeerInput.isNotBlank() ||
                    uiState.discoveredPeers.isEmpty()
            )

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

        MmrSettingsGroupWarn(
            text =
                "This connection is not locked. Use your home Wi‑Fi or your own hotspot, " +
                    "not a public or guest network.",
            modifier = Modifier.padding(top = 4.dp),
        )

        MmrSettingsGroup(
            title = "Enable",
            footer =
                "Copies transactions you have already approved. " +
                    "Messages still waiting in Approval Center stay on the phone that received them.",
        ) {
            MmrSettingsGroupSwitchRow(
                label = "Sync On Same Wi‑Fi",
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
            MmrSettingsGroupWarn(
                text = "Connect to Wi‑Fi or join a phone hotspot before syncing.",
                modifier = Modifier.padding(top = 14.dp),
            )
        }

        MmrSettingsGroup(title = "This Phone") {
            MmrSettingsGroupValueRow(
                label = "Network",
                value = uiState.wifiSsid?.takeIf { it.isNotBlank() } ?: "—",
            )
            MmrSettingsGroupTextField(
                label = "Your Name",
                value = uiState.displayName,
                onValueChange = viewModel::setDisplayName,
                placeholder = "Name",
            )
            MmrSettingsGroupValueRow(
                label = "Address",
                value = address,
                onClick =
                    if (address == "—") {
                        null
                    } else {
                        { copyPlain(context, "Address", address) }
                    },
            )
            MmrSettingsGroupCode(
                code = uiState.pairingOfferText,
                onClick =
                    uiState.pairingOfferText.takeIf { it.isNotBlank() }?.let { pin ->
                        { copyPlain(context, "Pairing code", pin) }
                    },
                testTag = MmrTestTags.lanSyncPairingOfferValue,
            )
            MmrSettingsGroupNote(
                text =
                    "Read this code to the other phone. It changes after 15 minutes. " +
                        "They will see your name on transactions you add.",
                showDividerBelow = false,
            )
        }

        MmrSettingsGroup(
            title = "On This Network",
            footer =
                "Tap their phone, then type the code shown on their screen. " +
                    "Skip this if you already trust that phone.",
        ) {
            if (uiState.discoveredPeers.isEmpty()) {
                MmrSettingsGroupNote(
                    text = "No other phones found yet.",
                    showDividerBelow = false,
                )
            } else {
                uiState.discoveredPeers.forEachIndexed { index, peer ->
                    val isSelected = uiState.selectedPairingPeerAddress == peer.address
                    MmrSettingsGroupNavRow(
                        label = peer.displayName,
                        value = if (isSelected) "Selected" else "",
                        onClick = {
                            if (!uiState.enabled || uiState.isBusy) return@MmrSettingsGroupNavRow
                            if (isSelected) {
                                viewModel.clearSelectedPairingPeer()
                            } else {
                                viewModel.selectDiscoveredPeerForPairing(peer.address)
                            }
                        },
                        testTag = "${MmrTestTags.lanSyncDiscoveredPeerPrefix}${peer.deviceId}",
                        showDividerBelow = index < uiState.discoveredPeers.lastIndex,
                    )
                }
            }
        }

        MmrSettingsGroup(
            title = "Trust A Device",
            footer =
                "Type the code from their screen, not the code on this phone. " +
                    "Type an address only if their phone is not in the list above.",
        ) {
            MmrSettingsGroupTextField(
                label = "Other Phone Address",
                value = uiState.pairingManualPeerInput,
                onValueChange = viewModel::onPairingManualPeerInputChanged,
                placeholder = "192.168.43.2:54321",
            )
            MmrSettingsGroupTextField(
                label = "Pairing Code",
                value = uiState.pairingInput,
                onValueChange = viewModel::onPairingInputChanged,
                placeholder = "Code On Their Phone",
                keyboardOptions =
                    KeyboardOptions(
                        capitalization = KeyboardCapitalization.Characters,
                        keyboardType = KeyboardType.Ascii,
                    ),
                testTag = MmrTestTags.lanSyncPairingInput,
            )
            MmrSettingsGroupAction(
                text = "Add Trusted Device",
                onClick = viewModel::requestTrustDevice,
                enabled = canTrust,
                showDividerBelow = false,
                testTag = MmrTestTags.lanSyncTrustAdd,
            )
        }

        if (uiState.pendingPairLabels.isNotEmpty()) {
            MmrSettingsGroup(title = "Waiting For You") {
                uiState.pendingPairLabels.forEachIndexed { index, label ->
                    val deviceId = uiState.pendingPairDeviceIds.getOrNull(index)
                    val last = index == uiState.pendingPairLabels.lastIndex
                    MmrSettingsGroupLabelRow(label = "$label Asked To Sync")
                    MmrSettingsGroupAction(
                        text = "Approve",
                        onClick = {
                            if (deviceId != null) viewModel.requestApprovePairRequest(deviceId)
                        },
                        enabled = uiState.enabled && !uiState.isBusy && deviceId != null,
                        testTag = deviceId?.let { "${MmrTestTags.lanSyncApprovePairPrefix}$it" },
                    )
                    MmrSettingsGroupAction(
                        text = "Decline",
                        onClick = {
                            if (deviceId != null) viewModel.rejectPairRequest(deviceId)
                        },
                        danger = true,
                        enabled = uiState.enabled && !uiState.isBusy && deviceId != null,
                        showDividerBelow = !last,
                    )
                }
            }
        }

        MmrSettingsGroup(title = "Trusted Devices") {
            if (uiState.trustedDeviceLabels.isEmpty()) {
                MmrSettingsGroupNote(
                    text = "No trusted phones yet.",
                    showDividerBelow = false,
                )
            } else {
                uiState.trustedDeviceLabels.forEachIndexed { index, label ->
                    MmrSettingsGroupAction(
                        text = "Remove $label",
                        onClick = { viewModel.requestRemoveTrusted(index) },
                        danger = true,
                        enabled = !uiState.isBusy,
                        showDividerBelow = index < uiState.trustedDeviceLabels.lastIndex,
                    )
                }
            }
        }

        if (!uiState.hasLocalLedgerData && uiState.trustedDeviceLabels.isNotEmpty()) {
            MmrSettingsGroup(
                title = "This Phone Has No Transactions",
                footer =
                    "Use this only when this phone has no transactions yet. " +
                        "It copies their data here. It does not combine both phones.",
            ) {
                uiState.trustedDeviceLabels.forEachIndexed { index, label ->
                    MmrSettingsGroupAction(
                        text = "Copy All Data From $label",
                        onClick = { viewModel.restoreFromTrusted(index, navigateHomeOnSuccess = true) },
                        enabled = uiState.enabled && uiState.onWifi && !uiState.isBusy,
                        showDividerBelow = index < uiState.trustedDeviceLabels.lastIndex,
                    )
                }
            }
        }

        MmrSettingsGroup(
            title = "Sync Options",
            footer =
                "With auto sync on, opening the app syncs when a trusted phone is on this Wi‑Fi. " +
                    "On a new Wi‑Fi name, tap Sync Now once so that network is allowed. " +
                    "Receipt photos are included only when Include Attachments is on.",
        ) {
            MmrSettingsGroupSwitchRow(
                label = "Auto Sync On App Open",
                checked = uiState.autoSyncOnAppOpen,
                onCheckedChange = viewModel::setAutoSyncOnAppOpen,
                enabled = uiState.enabled,
                switchTestTag = MmrTestTags.lanSyncAutoSyncOnOpenSwitch,
            )
            MmrSettingsGroupSwitchRow(
                label = "Sync One Device Only",
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
                    MmrSettingsGroupNavRow(
                        label = label,
                        value = if (id == uiState.selectedPeerDeviceId) "Selected" else "",
                        onClick = { if (id != null) viewModel.setSelectedPeer(id) },
                    )
                }
            }
            MmrSettingsGroupSwitchRow(
                label = "Include Attachments",
                checked = uiState.includeAttachments,
                onCheckedChange = viewModel::setIncludeAttachments,
                showDividerBelow = false,
            )
        }

        MmrSettingsGroup(
            title = "Sync Now",
            footer = uiState.syncNowDisabledHint,
        ) {
            MmrSettingsGroupAction(
                text = uiState.syncNowButtonText,
                onClick = viewModel::syncNow,
                enabled = uiState.syncNowEnabled,
                showDividerBelow = uiState.pendingApprovalCount > 0,
                testTag = MmrTestTags.lanSyncSyncNow,
            )
            if (uiState.pendingApprovalCount > 0) {
                MmrSettingsGroupNote(
                    text =
                        "${uiState.pendingApprovalCount} bank messages are still waiting for you to accept them. " +
                            "They are not sent to the other phone.",
                    showDividerBelow = false,
                )
            }
        }

        MmrSettingsGroup(
            title = "Sync History",
            footer = "Shows what changed on this phone. Colors and theme stay on this phone.",
        ) {
            MmrSettingsGroupNavRow(
                label = "Last 30 Syncs",
                value = uiState.historyValue,
                onClick = onNavigateToHistory,
                showDividerBelow = false,
            )
        }

        val showReplace = uiState.hasLocalLedgerData && uiState.trustedDeviceIds.isNotEmpty()
        MmrSettingsGroup(
            title = "Advanced",
            footer =
                if (showReplace) {
                    "Replace deletes everything on this phone and copies the other phone. " +
                        "Sync Now combines both phones instead."
                } else {
                    null
                },
        ) {
            if (showReplace) {
                uiState.trustedDeviceLabels.forEachIndexed { index, label ->
                    MmrSettingsGroupNavRow(
                        label = "Replace This Phone From $label",
                        value = "Deletes Yours",
                        onClick = { viewModel.restoreFromTrusted(index) },
                    )
                }
            }
            MmrSettingsGroupNavRow(
                label = "Clear Sync History",
                value = "Clear",
                onClick = viewModel::clearSyncLog,
                showDividerBelow = false,
            )
        }
    }
}

private fun lanHostPort(url: String?): String {
    if (url.isNullOrBlank()) return "—"
    return url.removePrefix("https://").removePrefix("http://").trimEnd('/')
}

private fun copyPlain(
    context: Context,
    label: String,
    value: String,
) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboard.setPrimaryClip(ClipData.newPlainText(label, value))
}
