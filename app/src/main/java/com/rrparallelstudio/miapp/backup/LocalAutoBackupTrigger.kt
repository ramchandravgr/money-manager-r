package com.rrparallelstudio.miapp.backup

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LocalAutoBackupTrigger
    @Inject
    constructor(
        private val workScheduler: LocalAutoBackupWorkScheduler,
    ) {
        fun onAppForeground() {
            workScheduler.syncPeriodicAutoBackupSchedule()
            workScheduler.enqueueAutoBackupIfDue()
        }
    }
