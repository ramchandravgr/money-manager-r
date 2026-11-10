package com.rrparallelstudio.miapp.ads.firebase

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

/**
 * At most **one** Remote Config network fetch per local calendar day (release builds).
 */
@Singleton
class MmrRemoteConfigDailyFetchStore
    @Inject
    constructor(
        @ApplicationContext context: Context,
    ) {
        private val prefs =
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        fun isDueForFetchToday(): Boolean {
            val today = LocalDate.now().toEpochDay()
            return prefs.getLong(KEY_LAST_FETCH_LOCAL_DAY, Long.MIN_VALUE) != today
        }

        fun markFetchAttemptedToday() {
            prefs.edit().putLong(KEY_LAST_FETCH_LOCAL_DAY, LocalDate.now().toEpochDay()).apply()
        }

        private companion object {
            const val PREFS_NAME = "mmr_remote_config_daily"
            const val KEY_LAST_FETCH_LOCAL_DAY = "last_fetch_local_epoch_day"
        }
    }
