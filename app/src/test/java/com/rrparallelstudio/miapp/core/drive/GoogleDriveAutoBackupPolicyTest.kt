package com.rrparallelstudio.miapp.core.drive

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.TimeUnit

class GoogleDriveAutoBackupPolicyTest {
    @Test
    fun monthly_requiresThirtyDaysSinceLastSuccess() {
        val now = System.currentTimeMillis()
        val twentyNineDaysAgo = now - TimeUnit.DAYS.toMillis(29)
        assertFalse(
            GoogleDriveAutoBackupPolicy.isBackupDue(
                GoogleDriveAutoSchedule.MONTHLY,
                twentyNineDaysAgo,
                now,
            ),
        )
        val thirtyDaysAgo = now - TimeUnit.DAYS.toMillis(30)
        assertTrue(
            GoogleDriveAutoBackupPolicy.isBackupDue(
                GoogleDriveAutoSchedule.MONTHLY,
                thirtyDaysAgo,
                now,
            ),
        )
    }
}
