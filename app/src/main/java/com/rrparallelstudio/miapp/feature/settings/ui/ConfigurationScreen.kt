package com.rrparallelstudio.miapp.feature.settings.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SheetState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.fragment.app.FragmentActivity
import androidx.hilt.navigation.compose.hiltViewModel
import com.rrparallelstudio.miapp.core.data.preferences.AppDisplaySettingsSnapshot
import com.rrparallelstudio.miapp.core.model.settings.MmrAmountColorScheme
import com.rrparallelstudio.miapp.core.model.settings.MmrAppColorTheme
import com.rrparallelstudio.miapp.core.model.settings.MmrStartScreenTab
import com.rrparallelstudio.miapp.core.model.settings.MmrThemeMode
import com.rrparallelstudio.miapp.core.model.settings.MmrTimeDisplay
import com.rrparallelstudio.miapp.core.model.settings.MmrWeekStartDay
import com.rrparallelstudio.miapp.feature.settings.viewmodel.ConfigurationViewModel
import com.rrparallelstudio.miapp.navigation.MmrToggleableMainDestinations
import com.rrparallelstudio.miapp.security.MmrDeviceCredentialGate
import com.rrparallelstudio.miapp.ui.components.MmrIosSettingsChoiceRow
import com.rrparallelstudio.miapp.ui.components.MmrIosSettingsNavRow
import com.rrparallelstudio.miapp.ui.components.MmrIosSettingsSection
import com.rrparallelstudio.miapp.ui.components.MmrIosSettingsSingleChoiceSheet
import com.rrparallelstudio.miapp.ui.components.MmrIosSettingsSwitchRow
import com.rrparallelstudio.miapp.ui.components.MmrSettingsChoiceLabelWithSwatches
import com.rrparallelstudio.miapp.ui.components.MmrSettingsColorSwatchStrip
import com.rrparallelstudio.miapp.ui.components.MmrSettingsScreenLayout
import com.rrparallelstudio.miapp.ui.components.MmrSettingsScreenScaffold
import com.rrparallelstudio.miapp.ui.theme.MmrSystemThemeAppearance
import com.rrparallelstudio.miapp.ui.theme.MmrThemeAppearance
import com.rrparallelstudio.miapp.ui.theme.MmrThemePresets

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConfigurationScreen(
    onNavigateBack: () -> Unit,
    viewModel: ConfigurationViewModel = hiltViewModel(),
) {
    val settings by viewModel.settings.collectAsState()
    val appLockOnLaunch by viewModel.appLockOnLaunch.collectAsState()
    val context = LocalContext.current
    val activity = context as? FragmentActivity
    var picker by remember { mutableStateOf<ConfigPicker?>(null) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val systemInDarkTheme = isSystemInDarkTheme()
    val previewAppearance =
        remember(settings.themeMode, systemInDarkTheme) {
            MmrSystemThemeAppearance.resolve(settings.themeMode, systemInDarkTheme)
        }

    picker?.let { active ->
        ConfigPickerSheet(
            picker = active,
            settings = settings,
            previewAppearance = previewAppearance,
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
            onSelectThemeMode = {
                viewModel.setThemeMode(it)
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
            MmrIosSettingsSwitchRow(
                label = "Balance carry-over",
                checked = settings.budgetCarryOver,
                onCheckedChange = viewModel::setCarryOver,
                showDividerBelow = false,
            )
        }

        MmrIosSettingsSection(
            title = "Main menu",
            footer =
                "Transactions and Settings always stay on the bar. Turn off optional tabs to hide them; " +
                    "remaining items share the space evenly.",
        ) {
            MmrToggleableMainDestinations.forEachIndexed { index, destination ->
                MmrIosSettingsSwitchRow(
                    label = "Show ${destination.label}",
                    checked = destination.navId !in settings.hiddenMainNavIds,
                    onCheckedChange = { visible ->
                        viewModel.setMainNavVisible(destination.navId, visible)
                    },
                    showDividerBelow = index < MmrToggleableMainDestinations.lastIndex,
                )
            }
        }

        MmrIosSettingsSection(
            title = "Appearance",
            footer = "Colors and how transactions look on Home.",
        ) {
            MmrIosSettingsNavRow(
                label = "Appearance",
                value = settings.themeMode.label,
                onClick = { picker = ConfigPicker.ThemeMode },
            )
            MmrIosSettingsNavRow(
                label = "App color theme",
                onClick = { picker = ConfigPicker.AppTheme },
                valueContent = {
                    MmrSettingsColorSwatchStrip(
                        colors =
                            MmrThemePresets.appThemePreviewColors(
                                settings.appColorTheme,
                                previewAppearance,
                            ),
                    )
                },
            )
            MmrIosSettingsNavRow(
                label = "Income / expense colors",
                onClick = { picker = ConfigPicker.AmountColors },
                valueContent = {
                    MmrSettingsColorSwatchStrip(
                        colors =
                            MmrThemePresets.amountSchemePreviewColors(
                                settings.appColorTheme,
                                settings.amountColorScheme,
                                previewAppearance,
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

        MmrIosSettingsSection(
            title = "Security",
            footer =
                "When enabled, unlock with your device PIN, pattern, or biometrics after opening the app " +
                    "or returning from the background. Requires a screen lock in Android settings.",
        ) {
            MmrIosSettingsSwitchRow(
                label = "Lock app on launch",
                checked = appLockOnLaunch,
                onCheckedChange = { enabled ->
                    if (!enabled) {
                        viewModel.setAppLockOnLaunch(false)
                        return@MmrIosSettingsSwitchRow
                    }
                    val host = activity
                    if (host == null) {
                        viewModel.setAppLockOnLaunch(true)
                        return@MmrIosSettingsSwitchRow
                    }
                    MmrDeviceCredentialGate.authenticate(
                        activity = host,
                        title = "Turn on app lock",
                        subtitle = "Confirm with your device screen lock or biometrics",
                        onSuccess = { viewModel.setAppLockOnLaunch(true) },
                        onFailure = { },
                    )
                },
                showDividerBelow = false,
            )
        }
    }
}

private enum class ConfigPicker {
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
private fun ConfigPickerSheet(
    picker: ConfigPicker,
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
            ConfigPicker.StartScreen -> "Start screen"
            ConfigPicker.MonthStart -> "Monthly start date"
            ConfigPicker.WeekStart -> "Weekly start day"
            ConfigPicker.AmountColors -> "Income / expense colors"
            ConfigPicker.TimeDisplay -> "Time on transactions"
            ConfigPicker.AppTheme -> "App color theme"
            ConfigPicker.ThemeMode -> "Appearance"
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
                                        previewAppearance,
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
            ConfigPicker.ThemeMode ->
                MmrThemeMode.entries.forEachIndexed { index, option ->
                    MmrIosSettingsChoiceRow(
                        label = option.label,
                        selected = settings.themeMode == option,
                        onClick = { onSelectThemeMode(option) },
                        showDividerBelow = index < MmrThemeMode.entries.lastIndex,
                    )
                }
            ConfigPicker.AppTheme ->
                MmrAppColorTheme.pickerEntries.forEachIndexed { index, option ->
                    val selected = settings.appColorTheme.forUi() == option
                    MmrIosSettingsChoiceRow(
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
}
