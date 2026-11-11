package com.rrparallelstudio.miapp.feature.webmanage.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.testTag
import androidx.hilt.navigation.compose.hiltViewModel
import com.rrparallelstudio.miapp.feature.webmanage.viewmodel.WebManageViewModel
import com.rrparallelstudio.miapp.testing.MmrTestTags
import com.rrparallelstudio.miapp.ui.components.MmrIosSettingsMonospaceFieldSlot
import com.rrparallelstudio.miapp.ui.components.MmrIosSettingsPrimaryActionSlot
import com.rrparallelstudio.miapp.ui.components.MmrIosSettingsSection
import com.rrparallelstudio.miapp.ui.components.MmrIosSettingsSectionBodyText
import com.rrparallelstudio.miapp.ui.components.MmrIosSettingsSecureTextRow
import com.rrparallelstudio.miapp.ui.components.MmrIosSettingsSwitchRow
import com.rrparallelstudio.miapp.ui.components.MmrSettingsMonospaceText
import com.rrparallelstudio.miapp.ui.components.MmrSettingsScreenLayout
import com.rrparallelstudio.miapp.ui.components.MmrSettingsScreenScaffold
import com.rrparallelstudio.miapp.ui.components.MmrSettingsStatusBanners

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
            footer =
                "Open the URL on a laptop on the same Wi‑Fi or phone hotspot. Password changes apply without restarting the server.",
        ) {
            MmrIosSettingsSectionBodyText(
                text =
                    "Browser access on your local network only. Traffic uses unencrypted HTTP — " +
                        "use a network you trust (not guest or public Wi‑Fi). Sign-in sessions last 2 hours " +
                        "and stay bound to the browser device that logged in.",
            )
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
                    "Turning Web Manage on disables Peer Device Sync. Anyone on the same LAN could reach this server; keep HTTP traffic on trusted Wi‑Fi or your own hotspot only."
                } else {
                    "Local HTTP server for browser access on your LAN. HTTPS with a local certificate is planned for a future release."
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
                    text = "Connect to Wi‑Fi or join a phone hotspot to use Web Manage from another device.",
                )
            }
            when {
                uiState.enabled && uiState.serverListening -> {
                    if (!uiState.onWifi) {
                        MmrIosSettingsSectionBodyText(
                            text =
                                "Server is running on this phone; connect Wi‑Fi or join a hotspot so a laptop on the same network can reach it.",
                        )
                    }
                    MmrIosSettingsMonospaceFieldSlot(
                        fieldLabel = "IP address",
                        value = uiState.lanIpv4 ?: "Unknown — check Wi‑Fi details in system settings",
                        accentValue = uiState.lanIpv4 != null,
                    )
                    MmrIosSettingsMonospaceFieldSlot(
                        fieldLabel = "Port",
                        value = uiState.serverPort.toString(),
                        accentValue = true,
                        showDividerBelow = false,
                    )
                    uiState.localUrl?.let { url ->
                        MmrIosSettingsSectionBodyText(text = "On your laptop browser:")
                        MmrSettingsMonospaceText(
                            text = url,
                            accent = true,
                        )
                    }
                }
                uiState.enabled -> {
                    MmrIosSettingsSectionBodyText(
                        text = "Server is starting… Turn Web Manage off and on if this message stays.",
                    )
                }
            }
        }

        if (uiState.onWifi && uiState.enabled) {
            MmrIosSettingsSection(
                title = "Connection help",
                footer =
                    "Use home Wi‑Fi or your phone’s hotspot (not guest or public Wi‑Fi). " +
                        "Turn off VPN on phone and laptop. " +
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
                            "On home Wi‑Fi this is often the router, not your phone. " +
                                "If this phone is sharing a hotspot, .1 can be correct for the phone. " +
                                "Check Wi‑Fi or hotspot details if the laptop cannot connect.",
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
