package com.rrparallelstudio.miapp.feature.settings.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import com.rrparallelstudio.miapp.feature.settings.viewmodel.ConfigurationViewModel
import com.rrparallelstudio.miapp.navigation.MmrToggleableMainDestinations
import com.rrparallelstudio.miapp.testing.MmrTestTags
import com.rrparallelstudio.miapp.ui.components.MmrSettingsGroup
import com.rrparallelstudio.miapp.ui.components.MmrSettingsGroupNavRow
import com.rrparallelstudio.miapp.ui.components.MmrSettingsGroupSwitchRow
import com.rrparallelstudio.miapp.ui.components.MmrSettingsScreenLayout
import com.rrparallelstudio.miapp.ui.components.MmrSettingsScreenScaffold

@Composable
fun HomeMenuSettingsScreen(
    onNavigateBack: () -> Unit,
    onOpenPicker: (ConfigurationPicker) -> Unit,
    viewModel: ConfigurationViewModel = hiltViewModel(),
) {
    val settings by viewModel.settings.collectAsState()

    MmrSettingsScreenScaffold(
        title = "Home & Menu",
        onNavigateBack = onNavigateBack,
        layout = MmrSettingsScreenLayout.InsetGrouped,
    ) {
        MmrSettingsGroup(
            title = "Home",
            isFirstInScreen = true,
            footer =
                "Choose which view opens first, when your month and week start, and whether today's " +
                    "balance includes what was left yesterday.",
        ) {
            MmrSettingsGroupNavRow(
                label = "Start Screen",
                value = settings.startScreenTab.label,
                onClick = { onOpenPicker(ConfigurationPicker.StartScreen) },
                testTag = MmrTestTags.settingsHomeMenuStartScreen,
            )
            MmrSettingsGroupNavRow(
                label = "Monthly Start Date",
                value = "Every ${settings.monthStartDay}",
                onClick = { onOpenPicker(ConfigurationPicker.MonthStart) },
            )
            MmrSettingsGroupNavRow(
                label = "Weekly Start Day",
                value = settings.weekStartDay.label,
                onClick = { onOpenPicker(ConfigurationPicker.WeekStart) },
            )
            MmrSettingsGroupSwitchRow(
                label = "Balance Carry-Over",
                checked = settings.budgetCarryOver,
                onCheckedChange = viewModel::setCarryOver,
                showDividerBelow = false,
            )
        }

        MmrSettingsGroup(
            title = "Main Menu",
            footer =
                "Transactions and Settings always stay at the bottom. Turn the others off to hide them.",
        ) {
            MmrToggleableMainDestinations.forEachIndexed { index, destination ->
                MmrSettingsGroupSwitchRow(
                    label = "Show ${destination.label}",
                    checked = destination.navId !in settings.hiddenMainNavIds,
                    onCheckedChange = { visible ->
                        viewModel.setMainNavVisible(destination.navId, visible)
                    },
                    showDividerBelow = index < MmrToggleableMainDestinations.lastIndex,
                )
            }
        }
    }
}
