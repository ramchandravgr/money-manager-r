package com.rrparallelstudio.miapp.feature.settings.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.text.font.FontWeight
import androidx.hilt.navigation.compose.hiltViewModel
import com.rrparallelstudio.miapp.core.data.preferences.AppDisplaySettingsSnapshot
import com.rrparallelstudio.miapp.core.model.settings.MmrAmountColorScheme
import com.rrparallelstudio.miapp.core.model.settings.MmrAppColorTheme
import com.rrparallelstudio.miapp.core.model.settings.MmrStartScreenTab
import com.rrparallelstudio.miapp.core.model.settings.MmrThemeMode
import com.rrparallelstudio.miapp.core.model.settings.MmrTimeDisplay
import com.rrparallelstudio.miapp.core.model.settings.MmrWeekStartDay
import com.rrparallelstudio.miapp.feature.settings.viewmodel.ConfigurationViewModel
import com.rrparallelstudio.miapp.ui.components.MmrIosSettingsSingleChoiceSheet
import com.rrparallelstudio.miapp.ui.components.MmrSettingsChoiceLabelWithSwatches
import com.rrparallelstudio.miapp.ui.components.MmrSettingsGroup
import com.rrparallelstudio.miapp.ui.components.MmrSettingsGroupChoiceRow
import com.rrparallelstudio.miapp.ui.components.MmrSettingsScreenLayout
import com.rrparallelstudio.miapp.ui.components.MmrSettingsScreenScaffold
import com.rrparallelstudio.miapp.ui.theme.MmrSystemThemeAppearance
import com.rrparallelstudio.miapp.ui.theme.MmrThemeAppearance
import com.rrparallelstudio.miapp.ui.theme.MmrThemePresets

enum class ConfigurationPicker {
    StartScreen,
    MonthStart,
    WeekStart,
    AmountColors,
    TimeDisplay,
    AppTheme,
    ThemeMode,
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ConfigurationPickerSheetLayer(
    activePicker: ConfigurationPicker?,
    onDismissPicker: () -> Unit,
    settings: AppDisplaySettingsSnapshot,
    viewModel: ConfigurationViewModel,
    sheetState: SheetState,
) {
    val picker = activePicker ?: return
    val systemInDarkTheme = isSystemInDarkTheme()
    val previewAppearance =
        remember(settings.themeMode, systemInDarkTheme) {
            MmrSystemThemeAppearance.resolve(settings.themeMode, systemInDarkTheme)
        }
    ConfigurationPickerSheet(
        picker = picker,
        settings = settings,
        previewAppearance = previewAppearance,
        sheetState = sheetState,
        onDismiss = onDismissPicker,
        onSelectStartScreen = {
            viewModel.setStartScreen(it)
            onDismissPicker()
        },
        onSelectMonthDay = {
            viewModel.setMonthStartDay(it)
            onDismissPicker()
        },
        onSelectWeekDay = {
            viewModel.setWeekStartDay(it)
            onDismissPicker()
        },
        onSelectColors = {
            viewModel.setAmountColorScheme(it)
            onDismissPicker()
        },
        onSelectTime = {
            viewModel.setTimeDisplay(it)
            onDismissPicker()
        },
        onSelectAppTheme = {
            viewModel.setAppColorTheme(it)
            onDismissPicker()
        },
        onSelectThemeMode = {
            viewModel.setThemeMode(it)
            onDismissPicker()
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ConfigurationPickerSheet(
    picker: ConfigurationPicker,
    settings: AppDisplaySettingsSnapshot,
    previewAppearance: MmrThemeAppearance,
    sheetState: SheetState,
    onDismiss: () -> Unit,
    onSelectStartScreen: (MmrStartScreenTab) -> Unit,
    onSelectMonthDay: (Int) -> Unit,
    onSelectWeekDay: (MmrWeekStartDay) -> Unit,
    onSelectColors: (MmrAmountColorScheme) -> Unit,
    onSelectTime: (MmrTimeDisplay) -> Unit,
    onSelectAppTheme: (MmrAppColorTheme) -> Unit,
    onSelectThemeMode: (MmrThemeMode) -> Unit,
) {
    val title =
        when (picker) {
            ConfigurationPicker.StartScreen -> "Start screen"
            ConfigurationPicker.MonthStart -> "Monthly start date"
            ConfigurationPicker.WeekStart -> "Weekly start day"
            ConfigurationPicker.AmountColors -> "Income / expense colors"
            ConfigurationPicker.TimeDisplay -> "Time on transactions"
            ConfigurationPicker.AppTheme -> "Color theme"
            ConfigurationPicker.ThemeMode -> "Light or dark"
        }
    val message =
        when (picker) {
            ConfigurationPicker.StartScreen ->
                "Transactions opens on the day list, or on the month calendar."
            ConfigurationPicker.MonthStart ->
                "Your budget month starts on this day. A short month, such as February, still ends on its last day."
            ConfigurationPicker.WeekStart ->
                "Trends, budgets, and the week view start on this day."
            ConfigurationPicker.AmountColors ->
                "Income uses the first color. Expense uses the second."
            ConfigurationPicker.TimeDisplay ->
                "Choose whether a transaction shows its time, and whether the newest or the oldest is at the top."
            ConfigurationPicker.AppTheme ->
                "This color is used for buttons, headers, and charts."
            ConfigurationPicker.ThemeMode -> null
        }
    MmrIosSettingsSingleChoiceSheet(
        title = title,
        onDismiss = onDismiss,
        sheetState = sheetState,
        message = message,
    ) {
        ConfigurationPickerChoices(
            picker = picker,
            settings = settings,
            previewAppearance = previewAppearance,
            onSelectStartScreen = onSelectStartScreen,
            onSelectMonthDay = onSelectMonthDay,
            onSelectWeekDay = onSelectWeekDay,
            onSelectColors = onSelectColors,
            onSelectTime = onSelectTime,
            onSelectAppTheme = onSelectAppTheme,
            onSelectThemeMode = onSelectThemeMode,
        )
    }
}

@Composable
private fun ConfigurationPickerChoices(
    picker: ConfigurationPicker,
    settings: AppDisplaySettingsSnapshot,
    previewAppearance: MmrThemeAppearance,
    onSelectStartScreen: (MmrStartScreenTab) -> Unit,
    onSelectMonthDay: (Int) -> Unit,
    onSelectWeekDay: (MmrWeekStartDay) -> Unit,
    onSelectColors: (MmrAmountColorScheme) -> Unit,
    onSelectTime: (MmrTimeDisplay) -> Unit,
    onSelectAppTheme: (MmrAppColorTheme) -> Unit,
    onSelectThemeMode: (MmrThemeMode) -> Unit,
) {
    when (picker) {
        ConfigurationPicker.StartScreen ->
            MmrStartScreenTab.entries.forEachIndexed { index, option ->
                MmrSettingsGroupChoiceRow(
                    label = option.label,
                    selected = settings.startScreenTab == option,
                    onClick = { onSelectStartScreen(option) },
                    showDividerBelow = index < MmrStartScreenTab.entries.lastIndex,
                )
            }
        ConfigurationPicker.MonthStart ->
            (1..28).forEachIndexed { index, day ->
                MmrSettingsGroupChoiceRow(
                    label = "Every $day",
                    selected = settings.monthStartDay == day,
                    onClick = { onSelectMonthDay(day) },
                    showDividerBelow = index < 27,
                )
            }
        ConfigurationPicker.WeekStart ->
            MmrWeekStartDay.entries.forEachIndexed { index, option ->
                MmrSettingsGroupChoiceRow(
                    label = option.label,
                    selected = settings.weekStartDay == option,
                    onClick = { onSelectWeekDay(option) },
                    showDividerBelow = index < MmrWeekStartDay.entries.lastIndex,
                )
            }
        ConfigurationPicker.AmountColors ->
            MmrAmountColorScheme.entries.forEachIndexed { index, option ->
                val selected = settings.amountColorScheme == option
                MmrSettingsGroupChoiceRow(
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
                                    previewAppearance,
                                ),
                            labelFontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                        )
                    },
                )
            }
        ConfigurationPicker.TimeDisplay ->
            MmrTimeDisplay.entries.forEachIndexed { index, option ->
                MmrSettingsGroupChoiceRow(
                    label = option.label,
                    selected = settings.timeDisplay == option,
                    onClick = { onSelectTime(option) },
                    showDividerBelow = index < MmrTimeDisplay.entries.lastIndex,
                )
            }
        ConfigurationPicker.ThemeMode ->
            MmrThemeMode.entries.forEachIndexed { index, option ->
                MmrSettingsGroupChoiceRow(
                    label = option.label,
                    selected = settings.themeMode == option,
                    onClick = { onSelectThemeMode(option) },
                    showDividerBelow = index < MmrThemeMode.entries.lastIndex,
                )
            }
        ConfigurationPicker.AppTheme ->
            MmrAppColorTheme.pickerEntries.forEachIndexed { index, option ->
                val selected = settings.appColorTheme.forUi() == option
                MmrSettingsGroupChoiceRow(
                    label = option.label,
                    selected = selected,
                    onClick = { onSelectAppTheme(option) },
                    showDividerBelow = index < MmrAppColorTheme.pickerEntries.lastIndex,
                    labelContent = {
                        MmrSettingsChoiceLabelWithSwatches(
                            label = option.label,
                            swatchColors =
                                MmrThemePresets.appThemePreviewColors(option, previewAppearance),
                            labelFontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                        )
                    },
                )
            }
    }
}

@Composable
fun SettingsChoiceScreen(
    pickerName: String,
    onNavigateBack: () -> Unit,
    viewModel: ConfigurationViewModel = hiltViewModel(),
) {
    val picker = runCatching { ConfigurationPicker.valueOf(pickerName) }.getOrNull()
    if (picker == null) {
        LaunchedEffect(Unit) { onNavigateBack() }
        return
    }
    val settings by viewModel.settings.collectAsState()
    val systemInDarkTheme = isSystemInDarkTheme()
    val previewAppearance =
        remember(settings.themeMode, systemInDarkTheme) {
            MmrSystemThemeAppearance.resolve(settings.themeMode, systemInDarkTheme)
        }
    val title =
        when (picker) {
            ConfigurationPicker.StartScreen -> "Start Screen"
            ConfigurationPicker.MonthStart -> "Monthly Start Date"
            ConfigurationPicker.WeekStart -> "Weekly Start Day"
            ConfigurationPicker.AmountColors -> "Income / Expense Colors"
            ConfigurationPicker.TimeDisplay -> "Time On Transactions"
            ConfigurationPicker.AppTheme -> "Color Theme"
            ConfigurationPicker.ThemeMode -> "Light Or Dark"
        }
    val sectionTitle =
        when (picker) {
            ConfigurationPicker.StartScreen -> "Open Transactions On"
            ConfigurationPicker.MonthStart -> "Month Begins"
            ConfigurationPicker.WeekStart -> "Week Begins"
            ConfigurationPicker.AmountColors -> "Amount Colors"
            ConfigurationPicker.TimeDisplay -> "Home Order"
            ConfigurationPicker.AppTheme -> "Theme"
            ConfigurationPicker.ThemeMode -> "Choose"
        }
    val footer =
        when (picker) {
            ConfigurationPicker.StartScreen ->
                "Transactions opens on the day list, or on the month calendar."
            ConfigurationPicker.MonthStart ->
                "Your budget month starts on this day. A short month, such as February, still ends on its last day."
            ConfigurationPicker.WeekStart ->
                "Trends, budgets, and the week view start on this day."
            ConfigurationPicker.AmountColors ->
                "Income uses the first color. Expense uses the second."
            ConfigurationPicker.TimeDisplay ->
                "Choose whether a transaction shows its time, and whether the newest or the oldest is at the top."
            ConfigurationPicker.AppTheme ->
                "This color is used for buttons, headers, and charts."
            ConfigurationPicker.ThemeMode -> null
        }
    MmrSettingsScreenScaffold(
        title = title,
        onNavigateBack = onNavigateBack,
        layout = MmrSettingsScreenLayout.InsetGrouped,
    ) {
        MmrSettingsGroup(
            title = sectionTitle,
            isFirstInScreen = true,
            footer = footer,
        ) {
            ConfigurationPickerChoices(
                picker = picker,
                settings = settings,
                previewAppearance = previewAppearance,
                onSelectStartScreen = viewModel::setStartScreen,
                onSelectMonthDay = viewModel::setMonthStartDay,
                onSelectWeekDay = viewModel::setWeekStartDay,
                onSelectColors = viewModel::setAmountColorScheme,
                onSelectTime = viewModel::setTimeDisplay,
                onSelectAppTheme = viewModel::setAppColorTheme,
                onSelectThemeMode = viewModel::setThemeMode,
            )
        }
    }
}
