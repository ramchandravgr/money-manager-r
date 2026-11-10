package com.rrparallelstudio.miapp.core.data.preferences

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LaunchPermissionStore
    @Inject
    constructor(
        @ApplicationContext context: Context,
    ) {
        private val prefs =
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        fun hasCompletedLaunchPermissionFlow(): Boolean = prefs.getBoolean(KEY_COMPLETED, false)

        fun markLaunchPermissionFlowCompleted() {
            prefs.edit().putBoolean(KEY_COMPLETED, true).apply()
        }

        private companion object {
            const val PREFS_NAME = "mmr_launch_permissions"
            const val KEY_COMPLETED = "launch_permission_flow_completed"
        }
    }
