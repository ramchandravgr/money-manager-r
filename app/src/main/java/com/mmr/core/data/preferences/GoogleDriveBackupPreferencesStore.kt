package com.mmr.core.data.preferences

import android.content.Context
import com.mmr.core.drive.GoogleDriveAutoSchedule
import dagger.hilt.android.qualifiers.ApplicationContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GoogleDriveBackupPreferencesStore
    @Inject
    constructor(
        @ApplicationContext context: Context,
    ) {
        private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        fun isLinked(): Boolean = !getLinkedEmail().isNullOrBlank()

        fun getLinkedEmail(): String? = prefs.getString(KEY_LINKED_EMAIL, null)?.takeIf { it.isNotBlank() }

        fun setLinkedEmail(email: String?) {
            prefs.edit().putString(KEY_LINKED_EMAIL, email?.trim()).apply()
        }

        fun clearLink() {
            prefs
                .edit()
                .remove(KEY_LINKED_EMAIL)
                .remove(KEY_BACKUPS_FOLDER_ID)
                .apply()
        }

        fun getBackupsFolderId(): String? = prefs.getString(KEY_BACKUPS_FOLDER_ID, null)?.takeIf { it.isNotBlank() }

        fun setBackupsFolderId(folderId: String) {
            prefs.edit().putString(KEY_BACKUPS_FOLDER_ID, folderId).apply()
        }

        fun getAutoSchedule(): GoogleDriveAutoSchedule =
            GoogleDriveAutoSchedule.fromStored(prefs.getString(KEY_AUTO_SCHEDULE, null))

        fun setAutoSchedule(schedule: GoogleDriveAutoSchedule) {
            prefs.edit().putString(KEY_AUTO_SCHEDULE, schedule.name).apply()
        }

        fun isWifiOnly(): Boolean = prefs.getBoolean(KEY_WIFI_ONLY, true)

        fun setWifiOnly(enabled: Boolean) {
            prefs.edit().putBoolean(KEY_WIFI_ONLY, enabled).apply()
        }

        fun getLastSuccessfulBackupMillis(): Long = prefs.getLong(KEY_LAST_SUCCESS_MILLIS, 0L)

        fun setLastSuccessfulBackupMillis(epochMillis: Long) {
            prefs.edit().putLong(KEY_LAST_SUCCESS_MILLIS, epochMillis).apply()
        }

        fun getConsecutiveFailureCount(): Int = prefs.getInt(KEY_CONSECUTIVE_FAILURES, 0)

        fun setConsecutiveFailureCount(count: Int) {
            prefs.edit().putInt(KEY_CONSECUTIVE_FAILURES, count.coerceAtLeast(0)).apply()
        }

        fun getLastAutoRequestDay(): String? = prefs.getString(KEY_LAST_AUTO_REQUEST_DAY, null)

        fun markAutoBackupRequestedToday() {
            prefs.edit().putString(KEY_LAST_AUTO_REQUEST_DAY, todayKey()).apply()
        }

        fun shouldEnqueueAutoBackupToday(): Boolean {
            val schedule = getAutoSchedule()
            if (schedule == GoogleDriveAutoSchedule.OFF || !isLinked()) return false
            if (getLastAutoRequestDay() == todayKey()) return false
            val lastSuccess = getLastSuccessfulBackupMillis()
            return when (schedule) {
                GoogleDriveAutoSchedule.DAILY -> !isSameCalendarDay(lastSuccess, System.currentTimeMillis())
                GoogleDriveAutoSchedule.WEEKLY ->
                    lastSuccess <= 0L ||
                        daysBetween(lastSuccess, System.currentTimeMillis()) >= 7
                GoogleDriveAutoSchedule.OFF -> false
            }
        }

        private fun todayKey(): String = SimpleDateFormat("yyyyMMdd", Locale.US).format(Date())

        private fun isSameCalendarDay(
            firstMillis: Long,
            secondMillis: Long,
        ): Boolean {
            if (firstMillis <= 0L) return false
            val fmt = SimpleDateFormat("yyyyMMdd", Locale.US)
            return fmt.format(Date(firstMillis)) == fmt.format(Date(secondMillis))
        }

        private fun daysBetween(
            startMillis: Long,
            endMillis: Long,
        ): Long = ((endMillis - startMillis) / (24L * 60L * 60L * 1000L)).coerceAtLeast(0L)

        private companion object {
            const val PREFS_NAME = "mmr_google_drive_backup"
            const val KEY_LINKED_EMAIL = "linked_email"
            const val KEY_BACKUPS_FOLDER_ID = "backups_folder_id"
            const val KEY_AUTO_SCHEDULE = "auto_schedule"
            const val KEY_WIFI_ONLY = "wifi_only"
            const val KEY_LAST_SUCCESS_MILLIS = "last_success_millis"
            const val KEY_CONSECUTIVE_FAILURES = "consecutive_failures"
            const val KEY_LAST_AUTO_REQUEST_DAY = "last_auto_request_day"
        }
    }
