package com.rrparallelstudio.miapp.feature.settings.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import com.rrparallelstudio.miapp.feature.settings.viewmodel.ConfigurationViewModel
import com.rrparallelstudio.miapp.ui.components.MmrSettingsGroup
import com.rrparallelstudio.miapp.ui.components.MmrSettingsGroupNavRow
import com.rrparallelstudio.miapp.ui.components.MmrSettingsGroupSwitchRow
import com.rrparallelstudio.miapp.ui.components.MmrSettingsScreenLayout
import com.rrparallelstudio.miapp.ui.components.MmrSettingsScreenScaffold

@Composable
fun AppearanceSettingsScreen(
    onNavigateBack: () -> Unit,
    onOpenPicker: (ConfigurationPicker) -> Unit,
    viewModel: ConfigurationViewModel = hiltViewModel(),
) {
    val settings by viewModel.settings.collectAsState()

    MmrSettingsScreenScaffold(
        title = "Appearance",
        onNavigateBack = onNavigateBack,
        layout = MmrSettingsScreenLayout.InsetGrouped,
    ) {
        MmrSettingsGroup(
            title = "Colors",
            isFirstInScreen = true,
            footer =
                "Light or dark can follow your phone. The color theme is the accent color used on " +
                    "buttons and headers.",
        ) {
            MmrSettingsGroupNavRow(
                label = "Light Or Dark",
                value = settings.themeMode.label,
                onClick = { onOpenPicker(ConfigurationPicker.ThemeMode) },
            )
            MmrSettingsGroupNavRow(
                label = "Color Theme",
                value = settings.appColorTheme.forUi().label,
                onClick = { onOpenPicker(ConfigurationPicker.AppTheme) },
            )
            MmrSettingsGroupNavRow(
                label = "Income / Expense Colors",
                value = settings.amountColorScheme.label,
                onClick = { onOpenPicker(ConfigurationPicker.AmountColors) },
                showDividerBelow = false,
            )
        }

        MmrSettingsGroup(
            title = "Transactions",
            footer = "Autocomplete suggests notes while you add a transaction.",
        ) {
            MmrSettingsGroupNavRow(
                label = "Time On Transactions",
                value = settings.timeDisplay.label,
                onClick = { onOpenPicker(ConfigurationPicker.TimeDisplay) },
            )
            MmrSettingsGroupSwitchRow(
                label = "Show Description On Home",
                checked = settings.showDescription,
                onCheckedChange = viewModel::setShowDescription,
            )
            MmrSettingsGroupSwitchRow(
                label = "Suggest Notes",
                checked = settings.autocompleteEnabled,
                onCheckedChange = viewModel::setAutocomplete,
                showDividerBelow = false,
            )
        }
    }
}
