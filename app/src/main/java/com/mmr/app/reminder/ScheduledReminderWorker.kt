package com.mmr.app.reminder

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class ScheduledReminderWorker
    @AssistedInject
    constructor(
        @Assisted context: Context,
        @Assisted params: WorkerParameters,
        private val coordinator: PendingNotificationCoordinator,
    ) : CoroutineWorker(context, params) {
        override suspend fun doWork(): Result {
            coordinator.reconcileAndDispatch()
            return Result.success()
        }
    }
