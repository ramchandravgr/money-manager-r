package com.rrparallelstudio.miapp.backup

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.rrparallelstudio.miapp.core.backup.LocalAutoBackupContracts
import com.rrparallelstudio.miapp.core.data.preferences.LocalAutoBackupPreferencesStore
import com.rrparallelstudio.miapp.core.drive.GoogleDriveAutoSchedule
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LocalAutoBackupWorkScheduler
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
        private val preferencesStore: LocalAutoBackupPreferencesStore,
    ) {
        fun enqueueAutoBackupIfDue() {
            if (!preferencesStore.shouldEnqueueAutoBackupToday()) return
            preferencesStore.markAutoBackupRequestedToday()
            val request = OneTimeWorkRequestBuilder<LocalAutoBackupWorker>().build()
            WorkManager
                .getInstance(context)
                .enqueueUniqueWork(
                    LocalAutoBackupContracts.WORK_NAME_AUTO,
                    ExistingWorkPolicy.REPLACE,
                    request,
                )
        }

        fun syncPeriodicAutoBackupSchedule() {
            val wm = WorkManager.getInstance(context)
            wm.cancelUniqueWork(LocalAutoBackupContracts.WORK_NAME_PERIODIC)
            val schedule = preferencesStore.getAutoSchedule()
            if (schedule == GoogleDriveAutoSchedule.OFF) return
            val intervalDays =
                when (schedule) {
                    GoogleDriveAutoSchedule.DAILY -> 1L
                    GoogleDriveAutoSchedule.WEEKLY -> 7L
                    GoogleDriveAutoSchedule.MONTHLY -> 30L
                    GoogleDriveAutoSchedule.OFF -> return
                }
            val request =
                PeriodicWorkRequestBuilder<LocalAutoBackupWorker>(intervalDays, TimeUnit.DAYS).build()
            wm.enqueueUniquePeriodicWork(
                LocalAutoBackupContracts.WORK_NAME_PERIODIC,
                ExistingPeriodicWorkPolicy.UPDATE,
                request,
            )
        }
    }
