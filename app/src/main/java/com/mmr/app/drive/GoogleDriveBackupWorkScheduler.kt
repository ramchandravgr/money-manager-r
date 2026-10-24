package com.mmr.app.drive

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.mmr.core.data.preferences.GoogleDriveBackupPreferencesStore
import com.mmr.core.drive.GoogleDriveBackupContracts
import dagger.hilt.android.qualifiers.ApplicationContext
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
            val constraints =
                Constraints
                    .Builder()
                    .setRequiredNetworkType(
                        if (preferencesStore.isWifiOnly()) NetworkType.UNMETERED else NetworkType.CONNECTED,
                    ).build()
            val request =
                OneTimeWorkRequestBuilder<GoogleDriveBackupWorker>()
                    .setConstraints(constraints)
                    .build()
            WorkManager
                .getInstance(context)
                .enqueueUniqueWork(
                    GoogleDriveBackupContracts.WORK_NAME_AUTO,
                    ExistingWorkPolicy.REPLACE,
                    request,
                )
        }
    }
