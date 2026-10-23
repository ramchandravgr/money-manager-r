package com.mmr.feature.lansync.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.mmr.app.ui.components.MmrMmHeaderBar
import com.mmr.app.ui.components.MmrMmLightNavRow
import com.mmr.app.ui.components.MmrMmSettingsSectionTitle
import com.mmr.app.ui.theme.MmrMmColors
import com.mmr.feature.lansync.viewmodel.LanSyncViewModel

@Composable
fun LanSyncScreen(
    onNavigateBack: () -> Unit,
    viewModel: LanSyncViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        containerColor = MmrMmColors.ListBackground,
        contentWindowInsets = com.mmr.app.ui.components.MmrNestedScaffoldInsets,
        topBar = {
            MmrMmHeaderBar(
                title = "Wi‑Fi sync",
                showBack = true,
                onBack = onNavigateBack,
            )
        },
    ) { innerPadding ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 18.dp, vertical = 12.dp),
        ) {
            if (uiState.isBusy) {
                CircularProgressIndicator(modifier = Modifier.padding(bottom = 12.dp))
            }
            uiState.errorMessage?.let { message ->
                Text(
                    text = message,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(bottom = 8.dp),
                )
                TextButton(onClick = viewModel::dismissMessages) { Text("Dismiss") }
            }
            uiState.statusMessage?.let { message ->
                Text(text = message, color = MmrMmColors.TextMedium, modifier = Modifier.padding(bottom = 8.dp))
            }

            MmrMmSettingsSectionTitle(title = "Enable")
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .background(MmrMmColors.SurfaceWhite)
                        .padding(horizontal = 18.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Sync on same Wi‑Fi",
                    modifier = Modifier.weight(1f),
                    color = MmrMmColors.Reversal,
                )
                Switch(checked = uiState.enabled, onCheckedChange = viewModel::setEnabled)
            }
            Text(
                text = "Syncs approved transactions and app settings. SMS and the approval queue stay on each device.",
                style = MaterialTheme.typography.bodySmall,
                color = MmrMmColors.TextMedium,
                modifier = Modifier.padding(bottom = 12.dp),
            )

            if (!uiState.onWifi) {
                Text(
                    text = "Connect to Wi‑Fi before syncing.",
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(bottom = 8.dp),
                )
            }

            MmrMmSettingsSectionTitle(title = "This device")
            OutlinedTextField(
                value = uiState.displayName,
                onValueChange = viewModel::setDisplayName,
                label = { Text("Device name") },
                modifier = Modifier.fillMaxWidth(),
            )
            Text(
                text = "Pairing code (share with your other phone):",
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(top = 12.dp, bottom = 4.dp),
            )
            Text(
                text = uiState.pairingOfferText,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(bottom = 12.dp),
            )

            MmrMmSettingsSectionTitle(title = "Trust a device")
            OutlinedTextField(
                value = uiState.pairingInput,
                onValueChange = viewModel::onPairingInputChanged,
                label = { Text("Paste pairing code from other device") },
                modifier = Modifier.fillMaxWidth(),
            )
            Button(
                onClick = viewModel::trustFromPairingInput,
                enabled = uiState.enabled && uiState.pairingInput.isNotBlank() && !uiState.isBusy,
                modifier = Modifier.padding(top = 8.dp),
            ) {
                Text("Add trusted device")
            }

            if (uiState.pendingRequestLabels.isNotEmpty()) {
                MmrMmSettingsSectionTitle(title = "Approve pairing")
                uiState.pendingRequestLabels.forEachIndexed { index, label ->
                    Text(text = label, modifier = Modifier.padding(vertical = 4.dp))
                    Button(onClick = { viewModel.approvePending(index) }, modifier = Modifier.padding(end = 8.dp)) {
                        Text("Approve")
                    }
                    TextButton(onClick = { viewModel.rejectPending(index) }) { Text("Reject") }
                }
            }

            if (uiState.trustedDeviceLabels.isNotEmpty()) {
                MmrMmSettingsSectionTitle(title = "Trusted devices")
                uiState.trustedDeviceLabels.forEachIndexed { index, label ->
                    MmrMmLightNavRow(
                        label = label,
                        value = "Remove",
                        onClick = { viewModel.removeTrusted(index) },
                    )
                }
            }

            if (uiState.discoveredLabels.isNotEmpty()) {
                MmrMmSettingsSectionTitle(title = "On this network")
                uiState.discoveredLabels.forEach { label ->
                    Text(text = label, modifier = Modifier.padding(vertical = 2.dp))
                }
            }

            Button(
                onClick = viewModel::syncNow,
                enabled =
                    uiState.enabled && uiState.onWifi && uiState.trustedDeviceLabels.isNotEmpty() && !uiState.isBusy,
                modifier = Modifier.padding(top = 20.dp),
            ) {
                Text("Sync now")
            }
        }
    }
}
