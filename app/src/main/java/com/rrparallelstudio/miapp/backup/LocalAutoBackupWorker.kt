package com.rrparallelstudio.miapp.backup

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.rrparallelstudio.miapp.core.backup.LocalAutoBackupExporter
import com.rrparallelstudio.miapp.core.data.preferences.LocalAutoBackupPreferencesStore
import com.rrparallelstudio.miapp.core.drive.GoogleDriveAutoSchedule
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class LocalAutoBackupWorker
    @AssistedInject
    constructor(
        @Assisted context: Context,
        @Assisted params: WorkerParameters,
        private val exporter: LocalAutoBackupExporter,
        private val preferencesStore: LocalAutoBackupPreferencesStore,
    ) : CoroutineWorker(context, params) {
        override suspend fun doWork(): Result {
            return try {
                if (preferencesStore.getAutoSchedule() == GoogleDriveAutoSchedule.OFF) {
                    return Result.success()
                }
                exporter.exportScheduledBackup()
                Result.success()
            } catch (_: Exception) {
                Result.retry()
            }
        }
    }
