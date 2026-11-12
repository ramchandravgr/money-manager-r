package com.rrparallelstudio.miapp.feature.webmanage.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.rrparallelstudio.miapp.feature.webmanage.viewmodel.WebManageViewModel
import com.rrparallelstudio.miapp.testing.MmrTestTags
import com.rrparallelstudio.miapp.ui.components.MmrSettingsGroup
import com.rrparallelstudio.miapp.ui.components.MmrSettingsGroupAction
import com.rrparallelstudio.miapp.ui.components.MmrSettingsGroupInset
import com.rrparallelstudio.miapp.ui.components.MmrSettingsGroupNote
import com.rrparallelstudio.miapp.ui.components.MmrSettingsGroupSecureTextField
import com.rrparallelstudio.miapp.ui.components.MmrSettingsGroupSwitchRow
import com.rrparallelstudio.miapp.ui.components.MmrSettingsGroupValueRow
import com.rrparallelstudio.miapp.ui.components.MmrSettingsGroupWarn
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

        MmrSettingsGroupWarn(
            text =
                "The browser connection is not locked. Use home Wi‑Fi or your hotspot. " +
                    "You stay signed in for 2 hours in that browser.",
            modifier = Modifier.padding(top = 4.dp),
        )

        MmrSettingsGroup(
            title = "Access",
            footer = "Use at least 6 characters. You can change it without turning Web Manage off.",
        ) {
            MmrSettingsGroupSecureTextField(
                label = "Password",
                value = uiState.passwordInput,
                onValueChange = viewModel::onPasswordChange,
                placeholder = "Required",
                testTag = MmrTestTags.webManagePasswordField,
            )
            MmrSettingsGroupAction(
                text = if (uiState.hasAccessPassword) "Update Password" else "Set Password",
                onClick = viewModel::savePassword,
                enabled = !uiState.isBusy && uiState.passwordInput.length >= 6,
                showDividerBelow = false,
                testTag = MmrTestTags.webManagePasswordSave,
            )
        }

        MmrSettingsGroup(
            title = "Server",
            footer =
                "On a laptop on the same Wi‑Fi, or on this phone's hotspot, open the address above. " +
                    "Turn off VPN on the phone and the laptop.",
        ) {
            MmrSettingsGroupSwitchRow(
                label = "Web Manage",
                checked = uiState.enabled,
                onCheckedChange = { viewModel.setEnabled(it) },
                switchTestTag = MmrTestTags.webManageEnabledSwitch,
            )
            if (!uiState.onWifi) {
                MmrSettingsGroupNote(
                    text = "Connect to Wi‑Fi or join a phone hotspot to use Web Manage from another device.",
                )
            }
            when {
                uiState.enabled && uiState.serverListening -> {
                    if (!uiState.onWifi) {
                        MmrSettingsGroupNote(
                            text =
                                "Server is running on this phone; connect Wi‑Fi or join a hotspot so a laptop on the same network can reach it.",
                        )
                    }
                    MmrSettingsGroupValueRow(
                        label = "Network",
                        value = uiState.wifiSsid?.takeIf { it.isNotBlank() } ?: "—",
                    )
                    MmrSettingsGroupValueRow(
                        label = "IP Address",
                        value = uiState.lanIpv4 ?: "Unknown — check Wi‑Fi details in system settings",
                    )
                    MmrSettingsGroupValueRow(
                        label = "Port",
                        value = uiState.serverPort.toString(),
                        showDividerBelow = uiState.localUrl != null,
                    )
                    uiState.localUrl?.let { url ->
                        MmrSettingsGroupNote(text = "On your laptop browser:")
                        MmrSettingsGroupInset(showDividerBelow = false) {
                            MmrSettingsMonospaceText(text = url, accent = true)
                        }
                    }
                }
                uiState.enabled -> {
                    MmrSettingsGroupNote(
                        text = "Server is starting… Turn Web Manage off and on if this message stays.",
                        showDividerBelow = false,
                    )
                }
            }
        }

        if (uiState.onWifi && uiState.enabled) {
            MmrSettingsGroup(
                title = "Connection Help",
                footer =
                    "Try the test address in this phone's browser first. " +
                        "If a laptop still cannot connect, the router may be blocking phones and computers from seeing each other.",
            ) {
                uiState.loopbackUrl?.let { loopback ->
                    MmrSettingsGroupNote(text = "On this phone only (test): $loopback")
                }
                uiState.wifiSsid?.let { ssid ->
                    MmrSettingsGroupValueRow(label = "Network", value = ssid)
                }
                if (uiState.lanIpv4?.endsWith(".1") == true) {
                    MmrSettingsGroupNote(
                        text =
                            "On home Wi‑Fi this is often the router, not your phone. " +
                                "If this phone is sharing a hotspot, .1 can be correct for the phone. " +
                                "Check Wi‑Fi or hotspot details if the laptop cannot connect.",
                    )
                }
                if (uiState.pendingApprovalCount > 0) {
                    MmrSettingsGroupNote(
                        text = "${uiState.pendingApprovalCount} pending approval(s) on this phone.",
                        showDividerBelow = false,
                    )
                }
            }
        }
    }
}
