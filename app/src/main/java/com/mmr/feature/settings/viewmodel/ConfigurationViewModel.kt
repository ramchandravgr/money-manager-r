package com.mmr.feature.settings.viewmodel

import androidx.lifecycle.ViewModel
import com.mmr.core.common.widget.HomeScreenWidgetRefreshTrigger
import com.mmr.core.data.preferences.AppDisplaySettingsSnapshot
import com.mmr.core.data.preferences.AppDisplaySettingsStore
import com.mmr.core.data.preferences.AppSecuritySettingsStore
import com.mmr.core.model.settings.MmrAmountColorScheme
import com.mmr.core.model.settings.MmrAppColorTheme
import com.mmr.core.model.settings.MmrInputOrder
import com.mmr.core.model.settings.MmrStartScreenTab
import com.mmr.core.model.settings.MmrSwipeBehavior
import com.mmr.core.model.settings.MmrThemeMode
import com.mmr.core.model.settings.MmrTimeDisplay
import com.mmr.core.model.settings.MmrWeekStartDay
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

        fun setSwipeBehavior(behavior: MmrSwipeBehavior) = displaySettingsStore.setSwipeBehavior(behavior)

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
