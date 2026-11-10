package com.rrparallelstudio.miapp.core.data.preferences

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FtueStore
    @Inject
    constructor(
        @ApplicationContext context: Context,
    ) {
        private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        private val _showFtue = MutableStateFlow(!isCompletedInPrefs())
        val showFtue: StateFlow<Boolean> = _showFtue.asStateFlow()

        fun hasCompletedFtue(): Boolean = isCompletedInPrefs()

        fun markCompleted() {
            prefs.edit().putBoolean(KEY_COMPLETED, true).apply()
            _showFtue.value = false
        }

        /** Settings → Replay introduction. */
        fun requestReplay() {
            prefs.edit().putBoolean(KEY_COMPLETED, false).apply()
            _showFtue.value = true
        }

        private fun isCompletedInPrefs(): Boolean = prefs.getBoolean(KEY_COMPLETED, false)

        private companion object {
            const val PREFS_NAME = "mmr_ftue"
            const val KEY_COMPLETED = "welcome_completed"
        }
    }
