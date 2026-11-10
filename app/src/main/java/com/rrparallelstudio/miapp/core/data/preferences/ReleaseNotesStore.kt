package com.rrparallelstudio.miapp.core.data.preferences

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReleaseNotesStore
    @Inject
    constructor(
        @ApplicationContext context: Context,
    ) {
        private val prefs =
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        fun lastAcknowledgedVersionName(): String? =
            prefs.getString(KEY_LAST_ACKNOWLEDGED, null)?.takeIf { it.isNotBlank() }

        fun shouldShowFor(appVersionName: String): Boolean {
            val last = lastAcknowledgedVersionName()
            return last == null || last != appVersionName.trim()
        }

        fun markAcknowledged(appVersionName: String) {
            prefs
                .edit()
                .putString(KEY_LAST_ACKNOWLEDGED, appVersionName.trim())
                .apply()
        }

        private companion object {
            const val PREFS_NAME = "mmr_release_notes"
            const val KEY_LAST_ACKNOWLEDGED = "last_acknowledged_version_name"
        }
    }
