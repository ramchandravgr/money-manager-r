package com.mmr.core.lansync

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.mmr.core.data.preferences.LanSyncDuplicateEntityPolicy
import com.mmr.core.data.repository.MmrInstrumentedRepositoryGraph
import com.mmr.core.database.entity.TransactionEntity
import com.mmr.core.lansync.SyncEntityType
import com.mmr.core.lansync.model.LanAccountRecord
import com.mmr.core.lansync.model.LanSyncTombstoneRecord
import com.mmr.core.lansync.model.LanTransactionRecord
import com.mmr.core.lansync.model.MmrLanSyncPayload
import com.mmr.core.model.enums.RecordStatus
import com.mmr.core.model.enums.TransactionSourceType
import com.mmr.core.model.enums.TransactionType
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LanSyncMergerInstrumentedTest {
    private lateinit var graph: MmrInstrumentedRepositoryGraph
    private lateinit var merger: LanSyncMerger

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        graph = MmrInstrumentedRepositoryGraph(context)
        val tombstoneMerger =
            LanSyncTombstoneMerger(
                database = graph.database,
                tombstoneDao = graph.database.syncTombstoneDao(),
                attachmentRepository = graph.transactionAttachmentRepository,
            )
        merger =
            LanSyncMerger(
                database = graph.database,
                balanceValidator = LanSyncAccountBalanceValidator(graph.database),
                tombstoneMerger = tombstoneMerger,
            )
    }

    @After
    fun tearDown() {
        graph.close()
    }

    @Test
    fun merge_insertsRemoteAccountAndTransaction() =
        runBlocking {
            val accountSyncId = "acct-remote-1"
            val payload =
                MmrLanSyncPayload(
                    formatVersion = LanSyncJsonCodec.CURRENT_FORMAT_VERSION,
                    exportedAtEpochMillis = 1L,
                    sourceDeviceId = "peer",
                    categories = emptyList(),
                    subcategories = emptyList(),
                    accounts =
                        listOf(
                            LanAccountRecord(
                                syncId = accountSyncId,
                                name = "Synced Cash",
                                type = "CASH",
                                openingBalance = 0.0,
                                currentBalance = 0.0,
                                currencyCode = "INR",
                                isActive = true,
                                isDefault = true,
                                cardLastFour = null,
                                createdAt = 1L,
                                updatedAt = 1L,
                            ),
                        ),
                    loans = emptyList(),
                    transactions =
                        listOf(
                            LanTransactionRecord(
                                syncId = "txn-remote-1",
                                dateTime = 1_700_000_000_000L,
                                amount = 75.0,
                                type = "EXPENSE",
                                categorySyncId = null,
                                subcategorySyncId = null,
                                note = "LAN lunch",
                                accountSyncId = accountSyncId,
                                relatedAccountSyncId = null,
                                loanSyncId = null,
                                sourceType = "MANUAL",
                                sourceReferenceId = null,
                                attachmentCount = 0,
                                createdAt = 1L,
                                updatedAt = 1L,
                                isEdited = false,
                                paymentChannel = null,
                                paymentInstrument = null,
                                cardLastFour = null,
                                cardNetwork = null,
                            ),
                        ),
                )

            val summary =
                merger.merge(
                    payload,
                    LanSyncMergeOptions(
                        duplicateAccountPolicy = LanSyncDuplicateEntityPolicy.KEEP_SEPARATE,
                    ),
                )

            assertEquals(1, summary.accountsInserted)
            assertEquals(1, summary.transactionsInserted)
            val txns = graph.database.transactionDao().getAllApprovedTransactions()
            assertEquals(1, txns.size)
            assertEquals(75.0, txns.single().amount, 0.001)
        }

    @Test
    fun merge_byNamePolicy_linksRemoteAccountToExistingLocalName() =
        runBlocking {
            val localId =
                graph.database.accountDao().insert(
                    com.mmr.core.database.entity.AccountEntity(
                        syncId = "local-acct",
                        name = "Shared Cash",
                        type = com.mmr.core.model.enums.AccountType.CASH,
                        isDefault = true,
                    ),
                )
            val remoteAccountSyncId = "remote-acct"
            val payload =
                MmrLanSyncPayload(
                    formatVersion = LanSyncJsonCodec.CURRENT_FORMAT_VERSION,
                    exportedAtEpochMillis = 1L,
                    sourceDeviceId = "peer",
                    categories = emptyList(),
                    subcategories = emptyList(),
                    accounts =
                        listOf(
                            LanAccountRecord(
                                syncId = remoteAccountSyncId,
                                name = "Shared Cash",
                                type = "CASH",
                                openingBalance = 0.0,
                                currentBalance = 0.0,
                                currencyCode = "INR",
                                isActive = true,
                                isDefault = false,
                                cardLastFour = null,
                                createdAt = 1L,
                                updatedAt = 2L,
                            ),
                        ),
                    loans = emptyList(),
                    transactions =
                        listOf(
                            LanTransactionRecord(
                                syncId = "txn-remote-2",
                                dateTime = 1_700_000_000_000L,
                                amount = 10.0,
                                type = "EXPENSE",
                                categorySyncId = null,
                                subcategorySyncId = null,
                                note = null,
                                accountSyncId = remoteAccountSyncId,
                                relatedAccountSyncId = null,
                                loanSyncId = null,
                                sourceType = "MANUAL",
                                sourceReferenceId = null,
                                attachmentCount = 0,
                                createdAt = 1L,
                                updatedAt = 1L,
                                isEdited = false,
                                paymentChannel = null,
                                paymentInstrument = null,
                                cardLastFour = null,
                                cardNetwork = null,
                            ),
                        ),
                )

            val summary =
                merger.merge(
                    payload,
                    LanSyncMergeOptions(
                        duplicateAccountPolicy = LanSyncDuplicateEntityPolicy.MERGE_BY_NAME,
                    ),
                )

            assertEquals(0, summary.accountsInserted)
            assertEquals(1, summary.transactionsInserted)
            val txns = graph.database.transactionDao().getAllApprovedTransactions()
            assertEquals(localId, txns.single().accountId)
        }

    @Test
    fun merge_appliesRemoteTransactionTombstone() =
        runBlocking {
            val accountId =
                graph.database.accountDao().insert(
                    com.mmr.core.database.entity.AccountEntity(
                        syncId = "acct-local",
                        name = "Cash",
                        type = com.mmr.core.model.enums.AccountType.CASH,
                    ),
                )
            graph.database.transactionDao().insert(
                TransactionEntity(
                    syncId = "txn-to-delete",
                    dateTime = 1_700_000_000_000L,
                    amount = 50.0,
                    type = TransactionType.EXPENSE,
                    accountId = accountId,
                    sourceType = TransactionSourceType.MANUAL,
                    status = RecordStatus.APPROVED,
                    updatedAt = 100L,
                ),
            )
            val payload =
                MmrLanSyncPayload(
                    formatVersion = LanSyncJsonCodec.CURRENT_FORMAT_VERSION,
                    exportedAtEpochMillis = 2L,
                    sourceDeviceId = "peer",
                    categories = emptyList(),
                    subcategories = emptyList(),
                    accounts = emptyList(),
                    loans = emptyList(),
                    transactions = emptyList(),
                    tombstones =
                        listOf(
                            LanSyncTombstoneRecord(
                                syncId = "txn-to-delete",
                                entityType = SyncEntityType.TRANSACTION,
                                deletedAt = 200L,
                                sourceDeviceId = "peer-device",
                            ),
                        ),
                )
            val summary = merger.merge(payload)
            assertEquals(1, summary.tombstonesApplied)
            assertEquals(
                0,
                graph.database
                    .transactionDao()
                    .getAllApprovedTransactions()
                    .size,
            )
        }
}
