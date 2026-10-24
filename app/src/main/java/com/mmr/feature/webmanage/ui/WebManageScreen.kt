package com.mmr.feature.webmanage.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.testTag
import androidx.hilt.navigation.compose.hiltViewModel
import com.mmr.app.testing.MmrTestTags
import com.mmr.app.ui.components.MmrIosSettingsPrimaryActionSlot
import com.mmr.app.ui.components.MmrIosSettingsSection
import com.mmr.app.ui.components.MmrIosSettingsSectionBodyText
import com.mmr.app.ui.components.MmrIosSettingsSecureTextRow
import com.mmr.app.ui.components.MmrIosSettingsSwitchRow
import com.mmr.app.ui.components.MmrSettingsMonospaceText
import com.mmr.app.ui.components.MmrSettingsScreenLayout
import com.mmr.app.ui.components.MmrSettingsScreenScaffold
import com.mmr.app.ui.components.MmrSettingsStatusBanners
import com.mmr.feature.webmanage.viewmodel.WebManageViewModel

@Composable
fun WebManageScreen(
    onNavigateBack: () -> Unit,
    viewModel: WebManageViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.refreshConnectionInfo()
    }

    MmrSettingsScreenScaffold(
        title = "Web Manage",
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
            title = "About",
            isFirstInScreen = true,
            footer = "Open the URL on a laptop on the same Wi‑Fi. Password changes apply without restarting the server.",
        ) {
            MmrIosSettingsSectionBodyText(text = "Browser access on your local network only.")
        }

        MmrIosSettingsSection(
            title = "Access",
            footer = "Password must be at least 6 characters.",
        ) {
            MmrIosSettingsSecureTextRow(
                label = "Password",
                value = uiState.passwordInput,
                onValueChange = viewModel::onPasswordChange,
                placeholder = "Required",
                showDividerBelow = true,
                testTag = MmrTestTags.webManagePasswordField,
            )
            MmrIosSettingsPrimaryActionSlot(
                text = if (uiState.hasAccessPassword) "Update password" else "Set password",
                onClick = viewModel::savePassword,
                enabled = !uiState.isBusy && uiState.passwordInput.length >= 6,
                showDividerBelow = false,
                testTag = MmrTestTags.webManagePasswordSave,
            )
        }

        MmrIosSettingsSection(
            title = "Server",
            footer =
                if (uiState.enabled) {
                    "Turning Web Manage on disables Peer Device Sync."
                } else {
                    "Local HTTP server for browser access on your LAN."
                },
        ) {
            MmrIosSettingsSwitchRow(
                label = "Web Manage",
                checked = uiState.enabled,
                onCheckedChange = { viewModel.setEnabled(it) },
                switchTestTag = MmrTestTags.webManageEnabledSwitch,
                showDividerBelow = uiState.onWifi && uiState.enabled,
            )
            if (!uiState.onWifi) {
                MmrIosSettingsSectionBodyText(
                    text = "Connect to Wi‑Fi to use Web Manage from another device.",
                )
            } else if (uiState.enabled) {
                if (!uiState.serverListening) {
                    MmrIosSettingsSectionBodyText(
                        text = "Server is starting… Turn Web Manage off and on if this message stays.",
                    )
                }
                uiState.localUrl?.let { url ->
                    MmrIosSettingsSectionBodyText(text = "On your laptop browser:")
                    MmrSettingsMonospaceText(
                        text = url,
                        accent = true,
                    )
                }
            }
        }

        if (uiState.onWifi && uiState.enabled) {
            MmrIosSettingsSection(
                title = "Connection help",
                footer =
                    "Use main Wi‑Fi (not guest). Turn off VPN on phone and laptop. " +
                        "Disable AP isolation on the router if the laptop cannot connect.",
            ) {
                uiState.loopbackUrl?.let { loopback ->
                    MmrIosSettingsSectionBodyText(text = "On this phone only (test): $loopback")
                }
                uiState.wifiSsid?.let { ssid ->
                    MmrIosSettingsSectionBodyText(text = "Network: $ssid")
                }
                if (uiState.lanIpv4?.endsWith(".1") == true) {
                    MmrIosSettingsSectionBodyText(
                        text =
                            "This address often belongs to the router, not your phone. " +
                                "Check Wi‑Fi settings for your phone’s IP if the laptop cannot connect.",
                    )
                }
                if (uiState.pendingApprovalCount > 0) {
                    MmrIosSettingsSectionBodyText(
                        text = "${uiState.pendingApprovalCount} pending approval(s) on this phone.",
                    )
                }
            }
        }
    }
}
