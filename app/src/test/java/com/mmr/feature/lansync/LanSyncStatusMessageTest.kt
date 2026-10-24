package com.mmr.feature.lansync

import com.mmr.core.domain.repository.LanSyncNowResult
import com.mmr.core.lansync.LanSyncMergeSummary
import org.junit.Assert.assertEquals
import org.junit.Test

class LanSyncStatusMessageTest {
    @Test
    fun formatSyncNowStatus_listsSuccessesAndFailures() {
        val message =
            LanSyncStatusMessages.formatSyncNowStatus(
                LanSyncNowResult(
                    summary = emptySummary(totalChanges = 3),
                    peerSuccesses = listOf("Pixel 8", "Galaxy Tab"),
                    peerFailures = listOf("Old phone is not on this Wi‑Fi."),
                ),
            )
        assertEquals(
            "Synced with Pixel 8, Galaxy Tab. Failed: Old phone is not on this Wi‑Fi.. 3 change(s) applied.",
            message,
        )
    }

    @Test
    fun formatSyncNowStatus_singleSuccessOnly() {
        val message =
            LanSyncStatusMessages.formatSyncNowStatus(
                LanSyncNowResult(
                    summary = emptySummary(totalChanges = 1),
                    peerSuccesses = listOf("Pixel 8"),
                    peerFailures = emptyList(),
                ),
            )
        assertEquals("Synced with Pixel 8. 1 change(s) applied.", message)
    }

    private fun emptySummary(totalChanges: Int): LanSyncMergeSummary =
        LanSyncMergeSummary(
            categoriesInserted = 0,
            categoriesUpdated = 0,
            subcategoriesInserted = 0,
            subcategoriesUpdated = 0,
            accountsInserted = 0,
            accountsUpdated = 0,
            loansInserted = 0,
            loansUpdated = 0,
            transactionsInserted = totalChanges,
            transactionsUpdated = 0,
        )
}
