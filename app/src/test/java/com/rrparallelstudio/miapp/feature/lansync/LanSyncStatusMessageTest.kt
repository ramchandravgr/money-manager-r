package com.rrparallelstudio.miapp.feature.lansync

import com.rrparallelstudio.miapp.core.domain.repository.LanSyncNowResult
import com.rrparallelstudio.miapp.core.lansync.LanSyncMergeSummary
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LanSyncStatusMessageTest {
    @Test
    fun formatSyncNowStatus_listsSuccessesAndFailures() {
        val message =
            LanSyncStatusMessages.formatSyncNowStatus(
                LanSyncNowResult(
                    summary = emptySummary(totalChanges = 3),
                    peerSuccesses = listOf("Pixel 8", "Galaxy Tab"),
                    peerFailures = listOf("Old phone is not on this Wi‑Fi right now."),
                ),
            )
        assertTrue(message.contains("Sync finished with Pixel 8, Galaxy Tab."))
        assertTrue(message.contains("Old phone is not on this Wi‑Fi right now."))
        assertTrue(message.contains("3 items updated from the other phone."))
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
        assertEquals(
            "Sync with Pixel 8 finished.\n1 item updated from the other phone.",
            message,
        )
    }

    @Test
    fun formatSyncNowStatus_includesBulletedWarnings() {
        val message =
            LanSyncStatusMessages.formatSyncNowStatus(
                LanSyncNowResult(
                    summary =
                        emptySummary(totalChanges = 2).copy(
                            warnings =
                                listOf(
                                    "Salary — saved balance does not match your transaction list.",
                                ),
                        ),
                    peerSuccesses = listOf("ONEPLUS A5010"),
                    peerFailures = emptyList(),
                    warnings = listOf("Salary — saved balance does not match your transaction list."),
                ),
            )
        assertTrue(message.contains("Notes (sync still completed):"))
        assertTrue(message.contains("• Salary — saved balance does not match your transaction list."))
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
