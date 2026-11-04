package com.mmr.app.reminder

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ScheduledReminderWorkScheduler
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
    ) {
        fun ensurePeriodicReminderChecks() {
            val request =
                PeriodicWorkRequestBuilder<ScheduledReminderWorker>(15, TimeUnit.MINUTES)
                    .build()
            WorkManager
                .getInstance(context)
                .enqueueUniquePeriodicWork(
                    WORK_NAME,
                    ExistingPeriodicWorkPolicy.KEEP,
                    request,
                )
        }

        companion object {
            const val WORK_NAME = "mmr_scheduled_reminder_checks"
        }
    }
