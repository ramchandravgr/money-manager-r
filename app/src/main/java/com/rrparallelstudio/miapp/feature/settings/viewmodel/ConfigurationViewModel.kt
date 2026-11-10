package com.rrparallelstudio.miapp.feature.settings.viewmodel

import androidx.lifecycle.ViewModel
import com.rrparallelstudio.miapp.core.common.widget.HomeScreenWidgetRefreshTrigger
import com.rrparallelstudio.miapp.core.data.preferences.AppDisplaySettingsSnapshot
import com.rrparallelstudio.miapp.core.data.preferences.AppDisplaySettingsStore
import com.rrparallelstudio.miapp.core.data.preferences.AppSecuritySettingsStore
import com.rrparallelstudio.miapp.core.model.settings.MmrAmountColorScheme
import com.rrparallelstudio.miapp.core.model.settings.MmrAppColorTheme
import com.rrparallelstudio.miapp.core.model.settings.MmrInputOrder
import com.rrparallelstudio.miapp.core.model.settings.MmrStartScreenTab
import com.rrparallelstudio.miapp.core.model.settings.MmrThemeMode
import com.rrparallelstudio.miapp.core.model.settings.MmrTimeDisplay
import com.rrparallelstudio.miapp.core.model.settings.MmrWeekStartDay
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@HiltViewModel
class ConfigurationViewModel
    @Inject
    constructor(
        private val displaySettingsStore: AppDisplaySettingsStore,
        private val appSecuritySettingsStore: AppSecuritySettingsStore,
        private val widgetRefreshTrigger: HomeScreenWidgetRefreshTrigger,
    ) : ViewModel() {
        val settings: StateFlow<AppDisplaySettingsSnapshot> = displaySettingsStore.settings

        val appLockOnLaunch: StateFlow<Boolean> = appSecuritySettingsStore.appLockOnLaunch

        fun setStartScreen(tab: MmrStartScreenTab) = displaySettingsStore.setStartScreenTab(tab)

        fun setMonthStartDay(day: Int) = displaySettingsStore.setMonthStartDay(day)

        fun setWeekStartDay(day: MmrWeekStartDay) = displaySettingsStore.setWeekStartDay(day)

        fun setCarryOver(enabled: Boolean) = displaySettingsStore.setBudgetCarryOver(enabled)

        fun setAmountColorScheme(scheme: MmrAmountColorScheme) = displaySettingsStore.setAmountColorScheme(scheme)

        fun setTimeDisplay(display: MmrTimeDisplay) = displaySettingsStore.setTimeDisplay(display)

        fun setShowDescription(show: Boolean) = displaySettingsStore.setShowDescription(show)

        fun setAutocomplete(enabled: Boolean) = displaySettingsStore.setAutocompleteEnabled(enabled)

        fun setInputOrder(order: MmrInputOrder) = displaySettingsStore.setInputOrder(order)

        fun setAppColorTheme(theme: MmrAppColorTheme) = displaySettingsStore.setAppColorTheme(theme)

        fun setThemeMode(mode: MmrThemeMode) {
            displaySettingsStore.setThemeMode(mode)
            widgetRefreshTrigger.requestRefresh()
        }

        fun setMainNavVisible(
            navId: String,
            visible: Boolean,
        ) = displaySettingsStore.setMainNavVisible(navId, visible)

        fun setAppLockOnLaunch(enabled: Boolean) = appSecuritySettingsStore.setAppLockOnLaunch(enabled)
    }
