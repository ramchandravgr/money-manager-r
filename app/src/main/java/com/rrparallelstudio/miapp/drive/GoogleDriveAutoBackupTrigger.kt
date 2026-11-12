package com.rrparallelstudio.miapp.drive

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GoogleDriveAutoBackupTrigger
    @Inject
    constructor(
        private val workScheduler: GoogleDriveBackupWorkScheduler,
    ) {
        fun onAppForeground() {
            workScheduler.syncPeriodicAutoBackupSchedule()
        }
    }
