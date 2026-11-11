package com.rrparallelstudio.miapp.feature.lansync.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
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
import com.rrparallelstudio.miapp.ui.components.MmrDimens
import com.rrparallelstudio.miapp.ui.components.MmrHelperText
import com.rrparallelstudio.miapp.ui.components.MmrIosSettingsBusyIndicator
import com.rrparallelstudio.miapp.ui.components.MmrIosSettingsCopyableCodeSlot
import com.rrparallelstudio.miapp.ui.components.MmrIosSettingsDestructiveRow
import com.rrparallelstudio.miapp.ui.components.MmrIosSettingsInlineErrorSection
import com.rrparallelstudio.miapp.ui.components.MmrIosSettingsInlineTextRow
import com.rrparallelstudio.miapp.ui.components.MmrIosSettingsNavRow
import com.rrparallelstudio.miapp.ui.components.MmrIosSettingsPrimaryActionSlot
import com.rrparallelstudio.miapp.ui.components.MmrIosSettingsSecondaryActionSlot
import com.rrparallelstudio.miapp.ui.components.MmrIosSettingsSection
import com.rrparallelstudio.miapp.ui.components.MmrIosSettingsSectionBodyText
import com.rrparallelstudio.miapp.ui.components.MmrIosSettingsSwitchRow
import com.rrparallelstudio.miapp.ui.components.MmrSettingsScreenLayout
import com.rrparallelstudio.miapp.ui.components.MmrSettingsScreenScaffold
import com.rrparallelstudio.miapp.ui.components.MmrSettingsStatusBanners
import com.rrparallelstudio.miapp.ui.theme.MmrBrandColors

@Composable
fun LanSyncScreen(
    onNavigateBack: () -> Unit,
    onNavigateHomeAfterRestore: () -> Unit = {},
    onNavigateToHistory: () -> Unit = {},
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
                "Copies approved transactions between phones you trust, on the same Wi‑Fi or your phone hotspot. " +
                    "SMS still waiting for approval stays on each phone. " +
                    "Sync on app open or tap Sync now. " +
                    "This sync is not encrypted. Use home Wi‑Fi or your own hotspot, not guest or public Wi‑Fi.",
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
                message = "Connect to Wi‑Fi or join a phone hotspot before syncing.",
                isFirstInScreen = false,
            )
        } else {
            MmrIosSettingsSection(
                title = "Network",
                footer =
                    buildString {
                        uiState.localSyncUrl?.let { append("This phone: $it. ") }
                        append(
                            "Each phone has its own address. That is normal. " +
                                "On this network shows phones found automatically. " +
                                "Both phones need Sync on same Wi‑Fi turned on.",
                        )
                        if (uiState.enabled && uiState.discoveredPeers.isEmpty()) {
                            append(
                                " If the other phone is missing, copy its This phone line into " +
                                    "Other phone address below. Turn off VPN on both phones.",
                            )
                        }
                    },
            ) {
                uiState.wifiSsid?.let { ssid ->
                    MmrIosSettingsSectionBodyText(text = "Network: $ssid")
                }
            }
        }

        MmrIosSettingsSection(title = "This device") {
            MmrIosSettingsInlineTextRow(
                label = "Your name",
                value = uiState.displayName,
                onValueChange = viewModel::setDisplayName,
                placeholder = "Name",
            )
            MmrHelperText(
                text = "The other phone shows this name as Created by after a sync.",
                modifier =
                    Modifier
                        .padding(
                            horizontal = MmrDimens.iosSettingsRowHorizontalPadding,
                            vertical = 6.dp,
                        ),
                color = MmrBrandColors.listAreaCaptionText(),
            )
            MmrIosSettingsCopyableCodeSlot(
                fieldLabel = "Pairing code",
                value = uiState.pairingOfferText,
                showDividerBelow = false,
                valueTestTag = MmrTestTags.lanSyncPairingOfferValue,
            )
            MmrIosSettingsSectionBodyText(
                text =
                    "Read this code to the other phone. It changes after 15 minutes. " +
                        "Do not share it with anyone else.",
            )
        }

        if (uiState.pendingPairLabels.isNotEmpty()) {
            MmrIosSettingsSection(
                title = "Pending pair requests",
                footer = "Another phone asked to sync. Tap Approve only if you were pairing with that phone.",
            ) {
                uiState.pendingPairLabels.forEachIndexed { index, label ->
                    val deviceId = uiState.pendingPairDeviceIds.getOrNull(index)
                    MmrIosSettingsSectionBodyText(text = label)
                    MmrIosSettingsPrimaryActionSlot(
                        text = "Approve $label",
                        onClick = {
                            if (deviceId != null) viewModel.requestApprovePairRequest(deviceId)
                        },
                        enabled = uiState.enabled && !uiState.isBusy && deviceId != null,
                        testTag = deviceId?.let { "${MmrTestTags.lanSyncApprovePairPrefix}$it" },
                    )
                    MmrIosSettingsSecondaryActionSlot(
                        text = "Decline",
                        onClick = {
                            if (deviceId != null) viewModel.rejectPairRequest(deviceId)
                        },
                        enabled = uiState.enabled && !uiState.isBusy && deviceId != null,
                        showDividerBelow = index < uiState.pendingPairLabels.lastIndex,
                    )
                }
            }
        }

        if (uiState.discoveredPeers.isNotEmpty()) {
            MmrIosSettingsSection(
                title = "On this network",
                footer =
                    "Tap the other phone, then enter its pairing code below. " +
                        "Skip if that phone is already trusted.",
            ) {
                uiState.discoveredPeers.forEachIndexed { index, peer ->
                    val isSelected = uiState.selectedPairingPeerAddress == peer.address
                    MmrIosSettingsNavRow(
                        label = peer.displayName,
                        labelSubtitle = peer.address,
                        value = if (isSelected) "Selected" else "",
                        onClick = {
                            if (!uiState.enabled || uiState.isBusy) return@MmrIosSettingsNavRow
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

        MmrIosSettingsSection(
            title = "Trust a device",
            footer =
                "Enter the 7-character code from the other phone. Do not enter the code shown on this phone.",
        ) {
            if (uiState.discoveredPeers.isEmpty()) {
                MmrIosSettingsInlineTextRow(
                    label = "Other phone address",
                    value = uiState.pairingManualPeerInput,
                    onValueChange = viewModel::onPairingManualPeerInputChanged,
                    placeholder = "192.168.43.2:54321",
                    singleLine = true,
                )
                MmrIosSettingsSectionBodyText(
                    text =
                        "On the other phone, copy the This phone line (numbers, colon, and port). " +
                            "Paste it here only if that phone is not listed above. " +
                            "If it is already under Trusted devices, you do not need to pair again. " +
                            "Tap Sync now. After that sync works, the other phone can sync back " +
                            "without typing your address, until you turn sync off and on.",
                )
            }
            MmrIosSettingsInlineTextRow(
                label = "Pairing code",
                value = uiState.pairingInput,
                onValueChange = viewModel::onPairingInputChanged,
                placeholder = "Pairing Code From Other Device",
                singleLine = true,
                keyboardOptions =
                    KeyboardOptions(
                        capitalization = KeyboardCapitalization.Characters,
                        keyboardType = KeyboardType.Ascii,
                    ),
                testTag = MmrTestTags.lanSyncPairingInput,
            )
            MmrIosSettingsSecondaryActionSlot(
                text = "Add trusted device",
                onClick = viewModel::requestTrustDevice,
                enabled =
                    uiState.enabled &&
                        uiState.pairingInput.isNotBlank() &&
                        !uiState.isBusy &&
                        (
                            uiState.discoveredPeers.isEmpty() ||
                                uiState.selectedPairingPeerAddress != null
                        ),
                showDividerBelow = false,
                testTag = MmrTestTags.lanSyncTrustAdd,
            )
        }

        if (uiState.trustedDeviceLabels.isNotEmpty()) {
            MmrIosSettingsSection(
                title = "Trusted devices",
                footer = "Remove stops that phone from syncing with this one.",
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

        MmrIosSettingsSection(
            title = "Sync options",
            footer = "Include attachments copies receipt photos. Sync can take longer.",
        ) {
            MmrIosSettingsSwitchRow(
                label = "Auto sync on app open",
                checked = uiState.autoSyncOnAppOpen,
                onCheckedChange = viewModel::setAutoSyncOnAppOpen,
                enabled = uiState.enabled,
                switchTestTag = MmrTestTags.lanSyncAutoSyncOnOpenSwitch,
                showDividerBelow = false,
            )
            MmrIosSettingsSectionBodyText(
                text =
                    if (uiState.enabled) {
                        "Syncs when you open the app, if a trusted phone is on this Wi‑Fi. " +
                            "On a new Wi‑Fi name, tap Sync now once and allow that network. " +
                            "Turning off Sync on same Wi‑Fi also turns this off. " +
                            "Turn it on again after you turn sync back on."
                    } else {
                        "Turn on Sync on same Wi‑Fi above first. " +
                            "Turning sync off also turns this off. " +
                            "Turn it on again after you turn sync back on."
                    },
            )
            MmrIosSettingsSwitchRow(
                label = "Sync one device only",
                checked = uiState.peerMode == LanSyncPeerMode.SINGLE_DEVICE,
                onCheckedChange = { enabled ->
                    viewModel.setPeerMode(
                        if (enabled) LanSyncPeerMode.SINGLE_DEVICE else LanSyncPeerMode.ALL_TRUSTED_ON_LAN,
                    )
                },
            )
            MmrIosSettingsSectionBodyText(
                text = "When this is on, Sync now talks only to the phone you select below.",
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
                footer =
                    "This phone has no transactions yet. " +
                        "This copies everything from the other phone onto this one.",
            ) {
                uiState.trustedDeviceLabels.forEachIndexed { index, label ->
                    val restoreEnabled = uiState.enabled && uiState.onWifi && !uiState.isBusy
                    MmrIosSettingsSecondaryActionSlot(
                        text = "Copy all data from $label",
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
                showDividerBelow = uiState.pendingApprovalCount <= 0,
                testTag = MmrTestTags.lanSyncSyncNow,
            )
            if (uiState.pendingApprovalCount > 0) {
                MmrIosSettingsSectionBodyText(
                    text =
                        "SMS still waiting for approval stays on this phone " +
                            "(${uiState.pendingApprovalCount} waiting). It is not copied.",
                )
            }
        }

        MmrIosSettingsSection(
            title = "Sync history",
            footer = "Shows what changed on this phone. Theme and colors stay on this phone.",
        ) {
            MmrIosSettingsNavRow(
                label = "Last 30 syncs",
                value = uiState.historyValue,
                onClick = onNavigateToHistory,
                showDividerBelow = false,
            )
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
                        "This replaces everything on this phone with that phone's data. Sync now merges both phones instead."
                    } else {
                        null
                    },
            ) {
                if (uiState.hasLocalLedgerData && uiState.trustedDeviceIds.isNotEmpty()) {
                    uiState.trustedDeviceLabels.forEachIndexed { index, label ->
                        MmrIosSettingsNavRow(
                            label = "Replace this phone from $label",
                            value = "Replaces yours",
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
