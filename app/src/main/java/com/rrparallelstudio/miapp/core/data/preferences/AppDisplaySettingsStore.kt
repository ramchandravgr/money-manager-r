package com.rrparallelstudio.miapp.core.data.preferences

import android.content.Context
import com.rrparallelstudio.miapp.core.model.settings.MmrAmountColorScheme
import com.rrparallelstudio.miapp.core.model.settings.MmrAppColorTheme
import com.rrparallelstudio.miapp.core.model.settings.MmrInputOrder
import com.rrparallelstudio.miapp.core.model.settings.MmrStartScreenTab
import com.rrparallelstudio.miapp.core.model.settings.MmrThemeMode
import com.rrparallelstudio.miapp.core.model.settings.MmrTimeDisplay
import com.rrparallelstudio.miapp.core.model.settings.MmrWeekStartDay
import com.rrparallelstudio.miapp.feature.home.model.HomeViewTab
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

data class AppDisplaySettingsSnapshot(
    val mainCurrencyCode: String = "INR",
    val mainCurrencySymbol: String = "₹",
    val subCurrencyCode: String? = null,
    val startScreenTab: MmrStartScreenTab = MmrStartScreenTab.DAILY,
    val monthStartDay: Int = 1,
    val weekStartDay: MmrWeekStartDay = MmrWeekStartDay.MONDAY,
    val budgetCarryOver: Boolean = true,
    val amountColorScheme: MmrAmountColorScheme = MmrAmountColorScheme.SET_B,
    val timeDisplay: MmrTimeDisplay = MmrTimeDisplay.NONE_ASC,
    val showDescription: Boolean = true,
    val autocompleteEnabled: Boolean = true,
    val inputOrder: MmrInputOrder = MmrInputOrder.FROM_AMOUNT,
    val appColorTheme: MmrAppColorTheme = MmrAppColorTheme.ROSE_GOLD,
    val themeMode: MmrThemeMode = MmrThemeMode.SYSTEM,
    /** [com.rrparallelstudio.miapp.navigation.MmrMainNavIds] hidden from bottom/rail navigation. */
    val hiddenMainNavIds: Set<String> = emptySet(),
)

@Singleton
class AppDisplaySettingsStore
    @Inject
    constructor(
        @ApplicationContext context: Context,
    ) {
        private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        private val _settings = MutableStateFlow(readAll())
        val settings: StateFlow<AppDisplaySettingsSnapshot> = _settings.asStateFlow()

        fun snapshot(): AppDisplaySettingsSnapshot = _settings.value

        fun defaultHomeTab(): HomeViewTab = _settings.value.startScreenTab.toHomeViewTab()

        fun update(transform: (AppDisplaySettingsSnapshot) -> AppDisplaySettingsSnapshot) {
            val next = transform(_settings.value)
            persist(next)
            _settings.value = next
        }

        fun setStartScreenTab(tab: MmrStartScreenTab) = update { it.copy(startScreenTab = tab) }

        fun setMonthStartDay(day: Int) =
            update {
                it.copy(monthStartDay = day.coerceIn(1, 28))
            }

        fun setWeekStartDay(day: MmrWeekStartDay) = update { it.copy(weekStartDay = day) }

        fun setBudgetCarryOver(enabled: Boolean) {
            prefs.edit().putBoolean(KEY_CARRY_OVER_USER_SET, true).apply()
            update { it.copy(budgetCarryOver = enabled) }
        }

        fun setAmountColorScheme(scheme: MmrAmountColorScheme) = update { it.copy(amountColorScheme = scheme) }

        fun setTimeDisplay(display: MmrTimeDisplay) = update { it.copy(timeDisplay = display) }

        fun setShowDescription(show: Boolean) = update { it.copy(showDescription = show) }

        fun setAutocompleteEnabled(enabled: Boolean) = update { it.copy(autocompleteEnabled = enabled) }

        fun setInputOrder(order: MmrInputOrder) = update { it.copy(inputOrder = order) }

        fun setAppColorTheme(theme: MmrAppColorTheme) = update { it.copy(appColorTheme = theme) }

        fun setThemeMode(mode: MmrThemeMode) = update { it.copy(themeMode = mode) }

        fun setMainNavVisible(
            navId: String,
            visible: Boolean,
        ) = update { snapshot ->
            val nextHidden =
                if (visible) {
                    snapshot.hiddenMainNavIds - navId
                } else {
                    snapshot.hiddenMainNavIds + navId
                }
            snapshot.copy(hiddenMainNavIds = nextHidden)
        }

        fun applySnapshot(snapshot: AppDisplaySettingsSnapshot) {
            persist(snapshot)
            prefs.edit().putBoolean(KEY_CARRY_OVER_USER_SET, true).apply()
            _settings.value = snapshot
        }

        private fun persist(snapshot: AppDisplaySettingsSnapshot) {
            prefs
                .edit()
                .putString(KEY_MAIN_CURRENCY, snapshot.mainCurrencyCode)
                .putString(KEY_SUB_CURRENCY, snapshot.subCurrencyCode)
                .putString(KEY_START_SCREEN, snapshot.startScreenTab.name)
                .putInt(KEY_MONTH_START, snapshot.monthStartDay)
                .putString(KEY_WEEK_START, snapshot.weekStartDay.name)
                .putBoolean(KEY_CARRY_OVER, snapshot.budgetCarryOver)
                .putString(KEY_AMOUNT_COLORS, snapshot.amountColorScheme.name)
                .putString(KEY_TIME_DISPLAY, snapshot.timeDisplay.name)
                .putBoolean(KEY_SHOW_DESC, snapshot.showDescription)
                .putBoolean(KEY_AUTOCOMPLETE, snapshot.autocompleteEnabled)
                .putString(KEY_INPUT_ORDER, snapshot.inputOrder.name)
                .putString(KEY_APP_COLOR_THEME, snapshot.appColorTheme.name)
                .putString(KEY_THEME_MODE, snapshot.themeMode.name)
                .putStringSet(KEY_HIDDEN_MAIN_NAV, snapshot.hiddenMainNavIds)
                .apply()
        }

        private fun readAll(): AppDisplaySettingsSnapshot =
            AppDisplaySettingsSnapshot(
                mainCurrencyCode = prefs.getString(KEY_MAIN_CURRENCY, "INR") ?: "INR",
                mainCurrencySymbol = currencySymbol(prefs.getString(KEY_MAIN_CURRENCY, "INR") ?: "INR"),
                subCurrencyCode = prefs.getString(KEY_SUB_CURRENCY, null)?.takeIf { it.isNotBlank() },
                startScreenTab =
                    enumOrDefault(
                        prefs.getString(KEY_START_SCREEN, null),
                        MmrStartScreenTab.DAILY,
                    ),
                monthStartDay = prefs.getInt(KEY_MONTH_START, 1).coerceIn(1, 28),
                weekStartDay =
                    enumOrDefault(
                        prefs.getString(KEY_WEEK_START, null),
                        MmrWeekStartDay.MONDAY,
                    ),
                budgetCarryOver = readBudgetCarryOver(),
                amountColorScheme =
                    enumOrDefault(
                        prefs.getString(KEY_AMOUNT_COLORS, null),
                        MmrAmountColorScheme.SET_B,
                    ),
                timeDisplay =
                    enumOrDefault(
                        prefs.getString(KEY_TIME_DISPLAY, null),
                        MmrTimeDisplay.NONE_ASC,
                    ),
                showDescription = prefs.getBoolean(KEY_SHOW_DESC, true),
                autocompleteEnabled = prefs.getBoolean(KEY_AUTOCOMPLETE, true),
                inputOrder =
                    enumOrDefault(
                        prefs.getString(KEY_INPUT_ORDER, null),
                        MmrInputOrder.FROM_AMOUNT,
                    ),
                appColorTheme =
                    enumOrDefault(
                        prefs.getString(KEY_APP_COLOR_THEME, null),
                        MmrAppColorTheme.ROSE_GOLD,
                    ).forUi(),
                themeMode =
                    enumOrDefault(
                        prefs.getString(KEY_THEME_MODE, null),
                        MmrThemeMode.SYSTEM,
                    ),
                hiddenMainNavIds = prefs.getStringSet(KEY_HIDDEN_MAIN_NAV, emptySet()) ?: emptySet(),
            )

        /**
         * Default on unless the user (or backup restore) explicitly chose otherwise.
         * Older builds persisted `carry_over=false` when other settings were saved.
         */
        private fun readBudgetCarryOver(): Boolean {
            if (!prefs.getBoolean(KEY_CARRY_OVER_USER_SET, false)) {
                return true
            }
            return prefs.getBoolean(KEY_CARRY_OVER, true)
        }

        private inline fun <reified T : Enum<T>> enumOrDefault(
            raw: String?,
            default: T,
        ): T {
            if (raw.isNullOrBlank()) return default
            return runCatching { enumValueOf<T>(raw) }.getOrDefault(default)
        }

        private fun currencySymbol(code: String): String =
            when (code.uppercase()) {
                "INR" -> "₹"
                "USD" -> "$"
                "EUR" -> "€"
                "GBP" -> "£"
                else -> code
            }

        private companion object {
            const val PREFS_NAME = "mmr_display_settings"
            const val KEY_MAIN_CURRENCY = "main_currency"
            const val KEY_SUB_CURRENCY = "sub_currency"
            const val KEY_START_SCREEN = "start_screen"
            const val KEY_MONTH_START = "month_start_day"
            const val KEY_WEEK_START = "week_start"
            const val KEY_CARRY_OVER = "carry_over"
            const val KEY_CARRY_OVER_USER_SET = "carry_over_user_set"
            const val KEY_AMOUNT_COLORS = "amount_colors"
            const val KEY_TIME_DISPLAY = "time_display"
            const val KEY_SHOW_DESC = "show_description"
            const val KEY_AUTOCOMPLETE = "autocomplete"
            const val KEY_INPUT_ORDER = "input_order"
            const val KEY_APP_COLOR_THEME = "app_color_theme"
            const val KEY_THEME_MODE = "theme_mode"
            const val KEY_HIDDEN_MAIN_NAV = "hidden_main_nav_ids"
        }
    }

fun MmrStartScreenTab.toHomeViewTab(): HomeViewTab =
    when (this) {
        MmrStartScreenTab.DAILY -> HomeViewTab.DAILY
        MmrStartScreenTab.CALENDAR -> HomeViewTab.CALENDAR
    }
