package com.mmr.feature.settings.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.mmr.app.ui.components.MmrIosSettingsChoiceRow
import com.mmr.app.ui.components.MmrIosSettingsNavRow
import com.mmr.app.ui.components.MmrIosSettingsSection
import com.mmr.app.ui.components.MmrIosSettingsSingleChoiceSheet
import com.mmr.app.ui.components.MmrIosSettingsSwitchRow
import com.mmr.app.ui.components.MmrSettingsChoiceLabelWithSwatches
import com.mmr.app.ui.components.MmrSettingsColorSwatchStrip
import com.mmr.app.ui.components.MmrSettingsScreenLayout
import com.mmr.app.ui.components.MmrSettingsScreenScaffold
import com.mmr.app.ui.theme.MmrBrandColors
import com.mmr.app.ui.theme.MmrThemePresets
import com.mmr.core.data.preferences.AppDisplaySettingsSnapshot
import com.mmr.core.model.settings.MmrAmountColorScheme
import com.mmr.core.model.settings.MmrAppColorTheme
import com.mmr.core.model.settings.MmrStartScreenTab
import com.mmr.core.model.settings.MmrSwipeBehavior
import com.mmr.core.model.settings.MmrTimeDisplay
import com.mmr.core.model.settings.MmrWeekStartDay
import com.mmr.feature.settings.viewmodel.ConfigurationViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConfigurationScreen(
    onNavigateBack: () -> Unit,
    viewModel: ConfigurationViewModel = hiltViewModel(),
) {
    val settings by viewModel.settings.collectAsState()
    var picker by remember { mutableStateOf<ConfigPicker?>(null) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    picker?.let { active ->
        ConfigPickerSheet(
            picker = active,
            settings = settings,
            sheetState = sheetState,
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
            onSelectAppTheme = {
                viewModel.setAppColorTheme(it)
                picker = null
            },
        )
    }

    MmrSettingsScreenScaffold(
        title = "Transaction Appearance",
        onNavigateBack = onNavigateBack,
        layout = MmrSettingsScreenLayout.InsetGrouped,
    ) {
        MmrIosSettingsSection(
            title = "Home & periods",
            isFirstInScreen = true,
            footer = "Month and week boundaries, default tab, and running balance.",
        ) {
            MmrIosSettingsNavRow(
                label = "Start screen",
                value = settings.startScreenTab.label,
                onClick = { picker = ConfigPicker.StartScreen },
            )
            MmrIosSettingsNavRow(
                label = "Monthly start date",
                value = "Every ${settings.monthStartDay}",
                onClick = { picker = ConfigPicker.MonthStart },
            )
            MmrIosSettingsNavRow(
                label = "Weekly start day",
                value = settings.weekStartDay.label,
                onClick = { picker = ConfigPicker.WeekStart },
            )
            MmrIosSettingsNavRow(
                label = "Swipe on home & stats",
                value = settings.swipeBehavior.label,
                onClick = { picker = ConfigPicker.Swipe },
            )
            MmrIosSettingsSwitchRow(
                label = "Balance carry-over",
                checked = settings.budgetCarryOver,
                onCheckedChange = viewModel::setCarryOver,
                showDividerBelow = false,
            )
        }

        MmrIosSettingsSection(
            title = "Appearance",
            footer = "Colors and how transactions look on Home.",
        ) {
            MmrIosSettingsNavRow(
                label = "App color theme",
                onClick = { picker = ConfigPicker.AppTheme },
                valueContent = {
                    ConfigurationNavValueWithSwatches(
                        title = settings.appColorTheme.label,
                        swatchColors = MmrThemePresets.appThemePreviewColors(settings.appColorTheme),
                    )
                },
            )
            MmrIosSettingsNavRow(
                label = "Income / expense colors",
                onClick = { picker = ConfigPicker.AmountColors },
                valueContent = {
                    ConfigurationNavValueWithSwatches(
                        title = settings.amountColorScheme.label,
                        swatchColors =
                            MmrThemePresets.amountSchemePreviewColors(
                                settings.appColorTheme,
                                settings.amountColorScheme,
                            ),
                    )
                },
            )
            MmrIosSettingsNavRow(
                label = "Time on transactions",
                value = settings.timeDisplay.label,
                onClick = { picker = ConfigPicker.TimeDisplay },
            )
            MmrIosSettingsSwitchRow(
                label = "Show description on home",
                checked = settings.showDescription,
                onCheckedChange = viewModel::setShowDescription,
                showDividerBelow = false,
            )
        }

        MmrIosSettingsSection(
            title = "Data entry",
            footer = "Suggestions while you add transactions.",
        ) {
            MmrIosSettingsSwitchRow(
                label = "Note autocomplete",
                checked = settings.autocompleteEnabled,
                onCheckedChange = viewModel::setAutocomplete,
                showDividerBelow = false,
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
    AppTheme,
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ConfigPickerSheet(
    picker: ConfigPicker,
    settings: AppDisplaySettingsSnapshot,
    sheetState: SheetState,
    onDismiss: () -> Unit,
    onSelectStartScreen: (MmrStartScreenTab) -> Unit,
    onSelectMonthDay: (Int) -> Unit,
    onSelectWeekDay: (MmrWeekStartDay) -> Unit,
    onSelectSwipe: (MmrSwipeBehavior) -> Unit,
    onSelectColors: (MmrAmountColorScheme) -> Unit,
    onSelectTime: (MmrTimeDisplay) -> Unit,
    onSelectAppTheme: (MmrAppColorTheme) -> Unit,
) {
    val title =
        when (picker) {
            ConfigPicker.StartScreen -> "Start screen"
            ConfigPicker.MonthStart -> "Monthly start date"
            ConfigPicker.WeekStart -> "Weekly start day"
            ConfigPicker.Swipe -> "Swipe behavior"
            ConfigPicker.AmountColors -> "Income / expense colors"
            ConfigPicker.TimeDisplay -> "Time on transactions"
            ConfigPicker.AppTheme -> "App color theme"
        }
    val message =
        when (picker) {
            ConfigPicker.StartScreen ->
                "Switches the Transactions tab to Daily or Calendar when you change this setting."
            else -> null
        }
    MmrIosSettingsSingleChoiceSheet(
        title = title,
        onDismiss = onDismiss,
        sheetState = sheetState,
        message = message,
    ) {
        when (picker) {
            ConfigPicker.StartScreen ->
                MmrStartScreenTab.entries.forEachIndexed { index, option ->
                    MmrIosSettingsChoiceRow(
                        label = option.label,
                        selected = settings.startScreenTab == option,
                        onClick = { onSelectStartScreen(option) },
                        showDividerBelow = index < MmrStartScreenTab.entries.lastIndex,
                    )
                }
            ConfigPicker.MonthStart ->
                (1..28).forEachIndexed { index, day ->
                    MmrIosSettingsChoiceRow(
                        label = "Every $day",
                        selected = settings.monthStartDay == day,
                        onClick = { onSelectMonthDay(day) },
                        showDividerBelow = index < 27,
                    )
                }
            ConfigPicker.WeekStart ->
                MmrWeekStartDay.entries.forEachIndexed { index, option ->
                    MmrIosSettingsChoiceRow(
                        label = option.label,
                        selected = settings.weekStartDay == option,
                        onClick = { onSelectWeekDay(option) },
                        showDividerBelow = index < MmrWeekStartDay.entries.lastIndex,
                    )
                }
            ConfigPicker.Swipe ->
                MmrSwipeBehavior.entries.forEachIndexed { index, option ->
                    MmrIosSettingsChoiceRow(
                        label = option.label,
                        selected = settings.swipeBehavior == option,
                        onClick = { onSelectSwipe(option) },
                        showDividerBelow = index < MmrSwipeBehavior.entries.lastIndex,
                    )
                }
            ConfigPicker.AmountColors ->
                MmrAmountColorScheme.entries.forEachIndexed { index, option ->
                    val selected = settings.amountColorScheme == option
                    MmrIosSettingsChoiceRow(
                        label = option.label,
                        selected = selected,
                        onClick = { onSelectColors(option) },
                        showDividerBelow = index < MmrAmountColorScheme.entries.lastIndex,
                        labelContent = {
                            MmrSettingsChoiceLabelWithSwatches(
                                label = option.label,
                                swatchColors =
                                    MmrThemePresets.amountSchemePreviewColors(
                                        settings.appColorTheme,
                                        option,
                                    ),
                                labelFontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                            )
                        },
                    )
                }
            ConfigPicker.TimeDisplay ->
                MmrTimeDisplay.entries.forEachIndexed { index, option ->
                    MmrIosSettingsChoiceRow(
                        label = option.label,
                        selected = settings.timeDisplay == option,
                        onClick = { onSelectTime(option) },
                        showDividerBelow = index < MmrTimeDisplay.entries.lastIndex,
                    )
                }
            ConfigPicker.AppTheme ->
                MmrAppColorTheme.entries.forEachIndexed { index, option ->
                    val selected = settings.appColorTheme == option
                    MmrIosSettingsChoiceRow(
                        label = option.label,
                        selected = selected,
                        onClick = { onSelectAppTheme(option) },
                        showDividerBelow = index < MmrAppColorTheme.entries.lastIndex,
                        labelContent = {
                            MmrSettingsChoiceLabelWithSwatches(
                                label = option.label,
                                swatchColors = MmrThemePresets.appThemePreviewColors(option),
                                labelFontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                            )
                        },
                    )
                }
        }
    }
}

@Composable
private fun ConfigurationNavValueWithSwatches(
    title: String,
    swatchColors: List<androidx.compose.ui.graphics.Color>,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = title,
            color = MmrBrandColors.TextMedium,
            fontSize = 16.sp,
            maxLines = 1,
        )
        MmrSettingsColorSwatchStrip(colors = swatchColors)
    }
}
