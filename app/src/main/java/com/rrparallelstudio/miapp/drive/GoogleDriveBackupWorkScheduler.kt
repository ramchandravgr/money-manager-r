package com.rrparallelstudio.miapp.drive

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.rrparallelstudio.miapp.core.data.preferences.GoogleDriveBackupPreferencesStore
import com.rrparallelstudio.miapp.core.drive.GoogleDriveAutoSchedule
import com.rrparallelstudio.miapp.core.drive.GoogleDriveBackupContracts
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GoogleDriveBackupWorkScheduler
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
        private val preferencesStore: GoogleDriveBackupPreferencesStore,
    ) {
        fun enqueueAutoBackupIfDue() {
            if (!preferencesStore.shouldEnqueueAutoBackupToday()) return
            preferencesStore.markAutoBackupRequestedToday()
            enqueueOneTimeBackup()
        }

        fun syncPeriodicAutoBackupSchedule() {
            val wm = WorkManager.getInstance(context)
            wm.cancelUniqueWork(GoogleDriveBackupContracts.WORK_NAME_PERIODIC)
            val schedule = preferencesStore.getAutoSchedule()
            if (schedule == GoogleDriveAutoSchedule.OFF || !preferencesStore.isLinked()) return
            val intervalDays =
                when (schedule) {
                    GoogleDriveAutoSchedule.DAILY -> 1L
                    GoogleDriveAutoSchedule.WEEKLY -> 7L
                    GoogleDriveAutoSchedule.MONTHLY -> 30L
                    GoogleDriveAutoSchedule.OFF -> return
                }
            val request =
                PeriodicWorkRequestBuilder<GoogleDriveBackupWorker>(intervalDays, TimeUnit.DAYS)
                    .setConstraints(networkConstraints())
                    .build()
            wm.enqueueUniquePeriodicWork(
                GoogleDriveBackupContracts.WORK_NAME_PERIODIC,
                ExistingPeriodicWorkPolicy.UPDATE,
                request,
            )
        }

        private fun enqueueOneTimeBackup() {
            val request =
                OneTimeWorkRequestBuilder<GoogleDriveBackupWorker>()
                    .setConstraints(networkConstraints())
                    .build()
            WorkManager
                .getInstance(context)
                .enqueueUniqueWork(
                    GoogleDriveBackupContracts.WORK_NAME_AUTO,
                    ExistingWorkPolicy.REPLACE,
                    request,
                )
        }

        private fun networkConstraints(): Constraints =
            Constraints
                .Builder()
                .setRequiredNetworkType(
                    if (preferencesStore.isWifiOnly()) NetworkType.UNMETERED else NetworkType.CONNECTED,
                ).build()
    }
