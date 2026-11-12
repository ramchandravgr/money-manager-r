package com.rrparallelstudio.miapp.core.data.preferences

import android.content.Context
import com.rrparallelstudio.miapp.core.drive.GoogleDriveAutoBackupPolicy
import com.rrparallelstudio.miapp.core.drive.GoogleDriveAutoSchedule
import dagger.hilt.android.qualifiers.ApplicationContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LocalAutoBackupPreferencesStore
    @Inject
    constructor(
        @ApplicationContext context: Context,
    ) {
        private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        fun getAutoSchedule(): GoogleDriveAutoSchedule =
            GoogleDriveAutoSchedule.fromStored(prefs.getString(KEY_AUTO_SCHEDULE, null))

        fun setAutoSchedule(schedule: GoogleDriveAutoSchedule) {
            prefs.edit().putString(KEY_AUTO_SCHEDULE, schedule.name).apply()
        }

        fun getLastSuccessfulBackupMillis(): Long = prefs.getLong(KEY_LAST_SUCCESS_MILLIS, 0L)

        fun setLastSuccessfulBackupMillis(epochMillis: Long) {
            prefs.edit().putLong(KEY_LAST_SUCCESS_MILLIS, epochMillis).apply()
        }

        fun markAutoBackupRequestedToday() {
            prefs.edit().putString(KEY_LAST_AUTO_REQUEST_DAY, todayKey()).apply()
        }

        fun shouldEnqueueAutoBackupToday(): Boolean {
            val schedule = getAutoSchedule()
            if (schedule == GoogleDriveAutoSchedule.OFF) return false
            if (prefs.getString(KEY_LAST_AUTO_REQUEST_DAY, null) == todayKey()) return false
            return GoogleDriveAutoBackupPolicy.isBackupDue(schedule, getLastSuccessfulBackupMillis())
        }

        private fun todayKey(): String = SimpleDateFormat("yyyyMMdd", Locale.US).format(Date())

        private companion object {
            const val PREFS_NAME = "mmr_local_auto_backup"
            const val KEY_AUTO_SCHEDULE = "auto_schedule"
            const val KEY_LAST_SUCCESS_MILLIS = "last_success_millis"
            const val KEY_LAST_AUTO_REQUEST_DAY = "last_auto_request_day"
        }
    }
