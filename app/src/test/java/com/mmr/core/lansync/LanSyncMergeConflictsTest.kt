package com.mmr.core.lansync

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LanSyncMergeConflictsTest {
    @Test
    fun needsUserInput_falseWhenNoConflicts() {
        val conflicts = LanSyncMergeConflicts()
        assertFalse(conflicts.needsUserInput)
    }

    @Test
    fun needsUserInput_trueWhenRecurringUnresolved() {
        val conflicts =
            LanSyncMergeConflicts(
                recurring =
                    listOf(
                        LanSyncRecurringConflict(
                            syncId = "rule-1",
                            name = "Rent",
                            localUpdatedAt = 1L,
                            remoteUpdatedAt = 2L,
                        ),
                    ),
            )
        assertTrue(conflicts.needsUserInput)
    }
}
