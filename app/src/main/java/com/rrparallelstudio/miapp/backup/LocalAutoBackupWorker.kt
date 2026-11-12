package com.rrparallelstudio.miapp.backup

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.rrparallelstudio.miapp.core.backup.UnifiedBackupRunner
import com.rrparallelstudio.miapp.core.data.preferences.LocalAutoBackupPreferencesStore
import com.rrparallelstudio.miapp.core.drive.GoogleDriveAutoSchedule
import com.rrparallelstudio.miapp.drive.GoogleDriveBackupWorkScheduler
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class LocalAutoBackupWorker
    @AssistedInject
    constructor(
        @Assisted context: Context,
        @Assisted params: WorkerParameters,
        private val unifiedBackupRunner: UnifiedBackupRunner,
        private val preferencesStore: LocalAutoBackupPreferencesStore,
        private val driveWorkScheduler: GoogleDriveBackupWorkScheduler,
    ) : CoroutineWorker(context, params) {
        override suspend fun doWork(): Result {
            return try {
                if (preferencesStore.getAutoSchedule() == GoogleDriveAutoSchedule.OFF) {
                    return Result.success()
                }
                val run = unifiedBackupRunner.exportScheduledBackup() ?: return Result.success()
                if (run.driveDeferredWifi) {
                    driveWorkScheduler.enqueueDeferredDriveUpload()
                }
                Result.success()
            } catch (_: Exception) {
                Result.retry()
            }
        }
    }
