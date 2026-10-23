package com.mmr.feature.settings.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import com.mmr.app.ui.components.MmrContentDialog
import com.mmr.app.ui.components.MmrMmHeaderBar
import com.mmr.app.ui.components.MmrMmLightNavRow
import com.mmr.app.ui.components.MmrMmLightSwitchRow
import com.mmr.app.ui.components.MmrMmSettingsSectionTitle
import com.mmr.app.ui.theme.MmrMmColors
import com.mmr.core.model.settings.MmrAmountColorScheme
import com.mmr.core.model.settings.MmrStartScreenTab
import com.mmr.core.model.settings.MmrSwipeBehavior
import com.mmr.core.model.settings.MmrTimeDisplay
import com.mmr.core.model.settings.MmrWeekStartDay
import com.mmr.feature.settings.viewmodel.ConfigurationViewModel

@Composable
fun ConfigurationScreen(
    onNavigateBack: () -> Unit,
    viewModel: ConfigurationViewModel = hiltViewModel(),
) {
    val settings by viewModel.settings.collectAsState()
    var picker by remember { mutableStateOf<ConfigPicker?>(null) }

    picker?.let { active ->
        ConfigPickerDialog(
            picker = active,
            onDismiss = { picker = null },
            onSelectStartScreen = {
                viewModel.setStartScreen(it)
                picker = null
            },
            onSelectMonthDay = {
                viewModel.setMonthStartDay(it)
                picker = null
            },
            onSelectWeekDay = {
                viewModel.setWeekStartDay(it)
                picker = null
            },
            onSelectSwipe = {
                viewModel.setSwipeBehavior(it)
                picker = null
            },
            onSelectColors = {
                viewModel.setAmountColorScheme(it)
                picker = null
            },
            onSelectTime = {
                viewModel.setTimeDisplay(it)
                picker = null
            },
        )
    }

    Scaffold(
        containerColor = MmrMmColors.ListBackground,
        topBar = {
            MmrMmHeaderBar(
                title = "App settings",
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
                    .verticalScroll(rememberScrollState()),
        ) {
            MmrMmSettingsSectionTitle(
                title = "Home & periods",
                subtitle = "Month/week boundaries, default tab, and running balance",
            )
            MmrMmLightNavRow(
                label = "Start screen",
                value = settings.startScreenTab.label,
                onClick = { picker = ConfigPicker.StartScreen },
            )
            MmrMmLightNavRow(
                label = "Monthly start date",
                value = "Every ${settings.monthStartDay}",
                onClick = { picker = ConfigPicker.MonthStart },
            )
            MmrMmLightNavRow(
                label = "Weekly start day",
                value = settings.weekStartDay.label,
                onClick = { picker = ConfigPicker.WeekStart },
            )
            MmrMmLightNavRow(
                label = "Swipe on home & stats",
                value = settings.swipeBehavior.label,
                onClick = { picker = ConfigPicker.Swipe },
            )
            MmrMmLightSwitchRow(
                label = "Balance carry-over",
                checked = settings.budgetCarryOver,
                onCheckedChange = viewModel::setCarryOver,
            )

            MmrMmSettingsSectionTitle(
                title = "Appearance",
                subtitle = "Colors and transaction list details",
            )
            MmrMmLightNavRow(
                label = "Income / expense colors",
                value = settings.amountColorScheme.label,
                onClick = { picker = ConfigPicker.AmountColors },
            )
            MmrMmLightNavRow(
                label = "Time on transactions",
                value = settings.timeDisplay.label,
                onClick = { picker = ConfigPicker.TimeDisplay },
            )
            MmrMmLightSwitchRow(
                label = "Show description on home",
                checked = settings.showDescription,
                onCheckedChange = viewModel::setShowDescription,
            )

            MmrMmSettingsSectionTitle(
                title = "Data entry",
                subtitle = "Suggestions while you add transactions",
            )
            MmrMmLightSwitchRow(
                label = "Note autocomplete",
                checked = settings.autocompleteEnabled,
                onCheckedChange = viewModel::setAutocomplete,
            )
        }
    }
}

private enum class ConfigPicker {
    StartScreen,
    MonthStart,
    WeekStart,
    Swipe,
    AmountColors,
    TimeDisplay,
}

@Composable
private fun ConfigPickerDialog(
    picker: ConfigPicker,
    onDismiss: () -> Unit,
    onSelectStartScreen: (MmrStartScreenTab) -> Unit,
    onSelectMonthDay: (Int) -> Unit,
    onSelectWeekDay: (MmrWeekStartDay) -> Unit,
    onSelectSwipe: (MmrSwipeBehavior) -> Unit,
    onSelectColors: (MmrAmountColorScheme) -> Unit,
    onSelectTime: (MmrTimeDisplay) -> Unit,
) {
    val title =
        when (picker) {
            ConfigPicker.StartScreen -> "Start screen"
            ConfigPicker.MonthStart -> "Monthly start date"
            ConfigPicker.WeekStart -> "Weekly start day"
            ConfigPicker.Swipe -> "Swipe behavior"
            ConfigPicker.AmountColors -> "Income / expense colors"
            ConfigPicker.TimeDisplay -> "Time on transactions"
        }
    val message =
        when (picker) {
            ConfigPicker.StartScreen ->
                "Switches the Transactions tab to Daily or Calendar when you change this setting."
            else -> null
        }
    MmrContentDialog(
        title = title,
        message = message,
        onDismiss = onDismiss,
        dismissLabel = "Close",
    ) {
        Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
            when (picker) {
                ConfigPicker.StartScreen ->
                    MmrStartScreenTab.entries.forEach { option ->
                        TextButton(onClick = { onSelectStartScreen(option) }) {
                            Text(option.label)
                        }
                    }
                ConfigPicker.MonthStart ->
                    (1..28).forEach { day ->
                        TextButton(onClick = { onSelectMonthDay(day) }) {
                            Text("Every $day")
                        }
                    }
                ConfigPicker.WeekStart ->
                    MmrWeekStartDay.entries.forEach { option ->
                        TextButton(onClick = { onSelectWeekDay(option) }) {
                            Text(option.label)
                        }
                    }
                ConfigPicker.Swipe ->
                    MmrSwipeBehavior.entries.forEach { option ->
                        TextButton(onClick = { onSelectSwipe(option) }) {
                            Text(option.label)
                        }
                    }
                ConfigPicker.AmountColors ->
                    MmrAmountColorScheme.entries.forEach { option ->
                        TextButton(onClick = { onSelectColors(option) }) {
                            Text(option.label)
                        }
                    }
                ConfigPicker.TimeDisplay ->
                    MmrTimeDisplay.entries.forEach { option ->
                        TextButton(onClick = { onSelectTime(option) }) {
                            Text(option.label)
                        }
                    }
            }
        }
    }
}
