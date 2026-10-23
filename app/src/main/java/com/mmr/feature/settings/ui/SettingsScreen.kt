package com.mmr.feature.settings.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.mmr.app.testing.MmrTestTags
import com.mmr.app.ui.components.MmrMmHeaderBar
import com.mmr.app.ui.components.MmrMmLightNavRow
import com.mmr.app.ui.theme.MmrMmColors
import com.mmr.feature.settings.viewmodel.SettingsViewModel
import kotlin.math.roundToInt

@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    onNavigateToConfiguration: () -> Unit,
    onNavigateToBackup: () -> Unit,
    onNavigateToLanSync: () -> Unit,
    onNavigateToCategories: () -> Unit,
    onNavigateToAccounts: () -> Unit,
    showBackNavigation: Boolean = true,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val smsScanMonths by viewModel.smsScanMonths.collectAsState()

    Scaffold(
        containerColor = MmrMmColors.ListBackground,
        contentWindowInsets = com.mmr.app.ui.components.MmrNestedScaffoldInsets,
        topBar = {
            MmrMmHeaderBar(
                title = "Configuration",
                showBack = showBackNavigation,
                onBack = onNavigateBack,
            )
        },
    ) { innerPadding ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
        ) {
            Column(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState()),
            ) {
                MmrMmLightNavRow(
                    label = "Display & periods",
                    value = "Open",
                    onClick = onNavigateToConfiguration,
                )
                MmrMmLightNavRow(
                    label = "Manage accounts",
                    onClick = onNavigateToAccounts,
                    testTag = MmrTestTags.settingsNavAccounts,
                )
                MmrMmLightNavRow(
                    label = "Manage categories",
                    onClick = onNavigateToCategories,
                    testTag = MmrTestTags.settingsNavCategories,
                )
                MmrMmLightNavRow(
                    label = "Backup & restore",
                    onClick = onNavigateToBackup,
                    testTag = MmrTestTags.settingsNavBackup,
                )
                MmrMmLightNavRow(
                    label = "Wi‑Fi sync",
                    value = "Devices",
                    onClick = onNavigateToLanSync,
                )

                Column(
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .background(MmrMmColors.SurfaceWhite)
                            .padding(horizontal = 18.dp, vertical = 14.dp),
                ) {
                    Text(
                        text = "SMS approvals",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MmrMmColors.Reversal,
                    )
                    Text(
                        text = "Inbox scan period: $smsScanMonths month${if (smsScanMonths == 1) "" else "s"}",
                        color = MmrMmColors.TextMedium,
                        modifier = Modifier.padding(top = 6.dp, bottom = 8.dp),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Slider(
                        value = smsScanMonths.toFloat(),
                        onValueChange = { value ->
                            viewModel.setSmsScanMonths(value.roundToInt())
                        },
                        valueRange = viewModel.minScanMonths.toFloat()..viewModel.maxScanMonths.toFloat(),
                        steps = viewModel.maxScanMonths - viewModel.minScanMonths - 1,
                    )
                }
            }
        }
    }
}
