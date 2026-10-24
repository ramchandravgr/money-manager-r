package com.mmr.core.lansync

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.mmr.core.data.repository.MmrInstrumentedRepositoryGraph
import com.mmr.core.database.entity.RecurringRuleEntity
import com.mmr.core.lansync.model.LanRecurringRuleRecord
import com.mmr.core.lansync.model.MmrLanSyncPayload
import com.mmr.core.model.enums.RecurrenceInterval
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LanSyncConflictDetectorInstrumentedTest {
    private lateinit var graph: MmrInstrumentedRepositoryGraph
    private lateinit var detector: LanSyncConflictDetector

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        graph = MmrInstrumentedRepositoryGraph(context)
        detector = LanSyncConflictDetector(database = graph.database)
    }

    @After
    fun tearDown() {
        graph.close()
    }

    @Test
    fun detect_reportsRecurringConflictWhenUpdatedAtDiffers() =
        runBlocking {
            val accountId =
                graph.database.accountDao().insert(
                    com.mmr.core.database.entity.AccountEntity(
                        syncId = "acct",
                        name = "Cash",
                        type = com.mmr.core.model.enums.AccountType.CASH,
                    ),
                )
            graph.database.recurringRuleDao().insert(
                RecurringRuleEntity(
                    syncId = "rule-sync",
                    name = "Rent",
                    accountId = accountId,
                    amount = 1000.0,
                    recurrenceInterval = RecurrenceInterval.MONTHLY,
                    nextRunDate = 1L,
                    updatedAt = 100L,
                ),
            )
            val remote =
                emptyPayload().copy(
                    recurringRules =
                        listOf(
                            LanRecurringRuleRecord(
                                syncId = "rule-sync",
                                name = "Rent",
                                transactionTemplateType = "EXPENSE",
                                amount = 1200.0,
                                categorySyncId = null,
                                subcategorySyncId = null,
                                accountSyncId = "acct",
                                dayOfMonth = 1,
                                recurrenceInterval = "MONTHLY",
                                nextRunDate = 1L,
                                isEnabled = true,
                                lastGeneratedDate = null,
                                endDate = null,
                                noteTemplate = null,
                                reminderMinutesBefore = 0,
                                createdAt = 1L,
                                updatedAt = 200L,
                            ),
                        ),
                )
            val conflicts = detector.detect(remote, LanSyncMergeOptions())
            assertEquals(1, conflicts.recurring.size)
            assertTrue(conflicts.needsUserInput)
        }

    private fun emptyPayload(): MmrLanSyncPayload =
        MmrLanSyncPayload(
            formatVersion = LanSyncJsonCodec.CURRENT_FORMAT_VERSION,
            exportedAtEpochMillis = 0L,
            sourceDeviceId = "peer",
            categories = emptyList(),
            subcategories = emptyList(),
            accounts = emptyList(),
            loans = emptyList(),
            transactions = emptyList(),
        )
}
