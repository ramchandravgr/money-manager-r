package com.mmr.core.data.preferences

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppSecuritySettingsStore
    @Inject
    constructor(
        @ApplicationContext context: Context,
    ) {
        private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        private val _appLockOnLaunch = MutableStateFlow(prefs.getBoolean(KEY_APP_LOCK, false))
        val appLockOnLaunch: StateFlow<Boolean> = _appLockOnLaunch.asStateFlow()

        fun isAppLockOnLaunchEnabled(): Boolean = _appLockOnLaunch.value

        fun setAppLockOnLaunch(enabled: Boolean) {
            prefs.edit().putBoolean(KEY_APP_LOCK, enabled).apply()
            _appLockOnLaunch.value = enabled
        }

        private companion object {
            const val PREFS_NAME = "mmr_app_security"
            const val KEY_APP_LOCK = "app_lock_on_launch"
        }
    }
