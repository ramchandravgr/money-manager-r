package com.rrparallelstudio.miapp.core.lansync

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.rrparallelstudio.miapp.core.backup.model.MmrAppSettingsBackup
import com.rrparallelstudio.miapp.core.common.sync.MmrSyncId
import com.rrparallelstudio.miapp.core.common.widget.NoOpHomeScreenWidgetRefreshTrigger
import com.rrparallelstudio.miapp.core.data.preferences.LanSyncIdentityStore
import com.rrparallelstudio.miapp.core.data.repository.MmrInstrumentedRepositoryGraph
import com.rrparallelstudio.miapp.core.database.entity.AccountEntity
import com.rrparallelstudio.miapp.core.database.entity.AssetEntity
import com.rrparallelstudio.miapp.core.database.entity.AssetValuationEntity
import com.rrparallelstudio.miapp.core.database.entity.BudgetEntity
import com.rrparallelstudio.miapp.core.database.entity.CategorizationRuleEntity
import com.rrparallelstudio.miapp.core.database.entity.CategoryEntity
import com.rrparallelstudio.miapp.core.database.entity.LoanEntity
import com.rrparallelstudio.miapp.core.database.entity.RecurringRuleEntity
import com.rrparallelstudio.miapp.core.database.entity.SubcategoryEntity
import com.rrparallelstudio.miapp.core.database.entity.TransactionEntity
import com.rrparallelstudio.miapp.core.model.enums.AccountType
import com.rrparallelstudio.miapp.core.model.enums.AssetStatus
import com.rrparallelstudio.miapp.core.model.enums.BudgetPeriodType
import com.rrparallelstudio.miapp.core.model.enums.CategorizationMatchType
import com.rrparallelstudio.miapp.core.model.enums.CategoryType
import com.rrparallelstudio.miapp.core.model.enums.LoanStatus
import com.rrparallelstudio.miapp.core.model.enums.LoanType
import com.rrparallelstudio.miapp.core.model.enums.RecordStatus
import com.rrparallelstudio.miapp.core.model.enums.RecurrenceInterval
import com.rrparallelstudio.miapp.core.model.enums.TransactionSourceType
import com.rrparallelstudio.miapp.core.model.enums.TransactionType
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Simulates a full Peer Device Sync handshake (export → merge on peer → response export → merge)
 * with dummy ledger data for every major LAN payload entity type.
 */
@RunWith(AndroidJUnit4::class)
class LanSyncComprehensivePayloadInstrumentedTest {
    private lateinit var context: Context
    private lateinit var peerA: MmrInstrumentedRepositoryGraph
    private lateinit var peerB: MmrInstrumentedRepositoryGraph

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        peerA = MmrInstrumentedRepositoryGraph(context)
        peerB = MmrInstrumentedRepositoryGraph(context)
    }

    @After
    fun tearDown() {
        peerA.close()
        peerB.close()
    }

    @Test
    fun bidirectionalSync_mergesTransactionsRecurringLoansBudgetsAssetsAndRules() =
        runBlocking {
            seedComprehensiveLedger(peerA)

            val exporterA = buildExporter(peerA)
            val mergerB = buildMerger(peerB)
            val mergerA = buildMerger(peerA)

            val outboundFromA =
                exporterA.buildPayload(
                    sourceDeviceId = "device-a",
                    sourceDeviceName = "Peer A",
                    appSettings = MmrAppSettingsBackup(smsScanMonths = 6),
                    displaySettings = null,
                    includeAttachments = false,
                    peerDeviceId = "device-b",
                )
            val wireA = LanSyncJsonCodec.encode(outboundFromA)
            val decodedA = LanSyncJsonCodec.decode(wireA)

            val summaryOnB = mergerB.merge(decodedA, LanSyncMergeOptions())
            assertTrue(summaryOnB.totalChanges > 0)

            val txnsOnB = peerB.database.transactionDao().getAllApprovedTransactions()
            assertTrue(txnsOnB.any { it.note == "LAN sync seed txn" })
            // Loan disbursal may add a second approved transaction.
            assertTrue(txnsOnB.size >= 1)
            assertEquals(
                1,
                peerB.database
                    .recurringRuleDao()
                    .getAllRules()
                    .first()
                    .size,
            )
            assertEquals(
                1,
                peerB.database
                    .loanDao()
                    .getAllLoans()
                    .first()
                    .size,
            )
            assertEquals(
                1,
                peerB.database
                    .budgetDao()
                    .getAll()
                    .size,
            )
            assertEquals(
                1,
                peerB.database
                    .assetDao()
                    .getAllAssetsList()
                    .size,
            )
            assertEquals(
                1,
                peerB.database
                    .categorizationRuleDao()
                    .getAll()
                    .size,
            )

            // Peer B adds a local-only expense (simulates edits while offline)
            val cashOnB =
                peerB.database
                    .accountDao()
                    .getAccountsOnce()
                    .first { it.type == AccountType.CASH }
            peerB.database.transactionDao().insert(
                TransactionEntity(
                    syncId = MmrSyncId.newId(),
                    dateTime = System.currentTimeMillis(),
                    amount = 42.0,
                    type = TransactionType.EXPENSE,
                    accountId = cashOnB.id,
                    sourceType = TransactionSourceType.MANUAL,
                    status = RecordStatus.APPROVED,
                    note = "Peer B only",
                    updatedAt = System.currentTimeMillis(),
                ),
            )

            val exporterB = buildExporter(peerB)
            val responseFromB =
                exporterB.buildPayload(
                    sourceDeviceId = "device-b",
                    sourceDeviceName = "Peer B",
                    appSettings = MmrAppSettingsBackup(smsScanMonths = 12),
                    displaySettings = null,
                    includeAttachments = false,
                    peerDeviceId = "device-a",
                )
            val summaryOnA =
                mergerA.merge(
                    LanSyncJsonCodec.decode(LanSyncJsonCodec.encode(responseFromB)),
                    LanSyncMergeOptions(),
                )
            assertTrue(summaryOnA.transactionsInserted >= 1)

            val txnsOnA = peerA.database.transactionDao().getAllApprovedTransactions()
            assertTrue(txnsOnA.any { it.note == "Peer B only" })
            assertTrue(txnsOnA.any { it.note == "LAN sync seed txn" })
            assertTrue(txnsOnA.size >= 2)
        }

    private suspend fun seedComprehensiveLedger(graph: MmrInstrumentedRepositoryGraph) {
        val now = System.currentTimeMillis()
        val accountId =
            graph.database.accountDao().insert(
                AccountEntity(
                    syncId = MmrSyncId.newId(),
                    name = "Sync Cash",
                    type = AccountType.CASH,
                    isDefault = true,
                    openingBalance = 1000.0,
                    currentBalance = 1000.0,
                    updatedAt = now,
                ),
            )
        val categoryId =
            graph.database.categoryDao().insert(
                CategoryEntity(
                    syncId = MmrSyncId.newId(),
                    name = "Peer Sync Food",
                    type = CategoryType.EXPENSE,
                ),
            )
        graph.database.categoryDao().insertSubcategory(
            SubcategoryEntity(
                syncId = MmrSyncId.newId(),
                categoryId = categoryId,
                name = "Groceries",
                isActive = true,
            ),
        )
        graph.database.transactionDao().insert(
            TransactionEntity(
                syncId = MmrSyncId.newId(),
                dateTime = now,
                amount = 99.0,
                type = TransactionType.EXPENSE,
                accountId = accountId,
                categoryId = categoryId,
                sourceType = TransactionSourceType.MANUAL,
                status = RecordStatus.APPROVED,
                note = "LAN sync seed txn",
                updatedAt = now,
            ),
        )
        graph.database.recurringRuleDao().insert(
            RecurringRuleEntity(
                syncId = MmrSyncId.newId(),
                name = "Rent sync test",
                accountId = accountId,
                amount = 15000.0,
                recurrenceInterval = RecurrenceInterval.MONTHLY,
                nextRunDate = now,
                updatedAt = now,
            ),
        )
        graph.loanRepository.createLoan(
            LoanEntity(
                syncId = MmrSyncId.newId(),
                name = "Sync loan",
                lenderName = "Test Bank",
                principalAmount = 50000.0,
                interestRate = 10.0,
                outstandingAmount = 50000.0,
                openingDate = now,
                loanType = LoanType.BORROWED,
                status = LoanStatus.ACTIVE,
                updatedAt = now,
            ),
        )
        graph.database.budgetDao().insert(
            BudgetEntity(
                syncId = MmrSyncId.newId(),
                categoryId = categoryId,
                amountLimit = 5000.0,
                periodType = BudgetPeriodType.MONTHLY,
                isActive = true,
                createdAt = now,
                updatedAt = now,
            ),
        )
        val assetId =
            graph.database.assetDao().insert(
                AssetEntity(
                    syncId = MmrSyncId.newId(),
                    name = "Sync asset gold",
                    assetType = "Gold",
                    status = AssetStatus.ACTIVE,
                    purchasePrice = 100000.0,
                    currentValue = 100000.0,
                    updatedAt = now,
                ),
            )
        graph.database.assetValuationDao().insert(
            AssetValuationEntity(
                syncId = MmrSyncId.newId(),
                assetId = assetId,
                amount = 105000.0,
                recordedAt = now,
            ),
        )
        graph.database.categorizationRuleDao().upsert(
            CategorizationRuleEntity(
                syncId = MmrSyncId.newId(),
                pattern = "SWIGGY",
                categoryName = "Peer Sync Food",
                matchType = CategorizationMatchType.MERCHANT_OR_NOTE_CONTAINS,
                priority = 1,
                updatedAt = now,
            ),
        )
    }

    private fun buildExporter(graph: MmrInstrumentedRepositoryGraph): LanSyncExporter {
        val identity = LanSyncIdentityStore(context)
        val tombstoneRecorder =
            LanSyncTombstoneRecorder(
                graph.database.syncTombstoneDao(),
                identity,
            )
        val tombstoneCoordinator =
            LanSyncTombstoneCoordinator(
                tombstoneDao = graph.database.syncTombstoneDao(),
                ackDao = graph.database.lanSyncTombstoneAckDao(),
                lanSyncDao = graph.database.lanSyncDao(),
            )
        return LanSyncExporter(
            transactionDao = graph.database.transactionDao(),
            backupDao = graph.database.backupDao(),
            categorizationRuleDao = graph.database.categorizationRuleDao(),
            smsAccountOverrideDao = graph.database.smsAccountOverrideDao(),
            attachmentDao = graph.database.transactionAttachmentDao(),
            tombstoneCoordinator = tombstoneCoordinator,
            ownerProfileCoordinator = graph.ownerProfileCoordinator,
        )
    }

    private fun buildMerger(graph: MmrInstrumentedRepositoryGraph): LanSyncMerger {
        val identity = LanSyncIdentityStore(context)
        val tombstoneRecorder =
            LanSyncTombstoneRecorder(
                graph.database.syncTombstoneDao(),
                identity,
            )
        val assetAttachmentRepository =
            com.rrparallelstudio.miapp.core.data.repository.AssetAttachmentRepositoryImpl(
                context = context,
                database = graph.database,
                attachmentDao = graph.database.assetAttachmentDao(),
                store =
                    com.rrparallelstudio.miapp.core.attachment
                        .AssetAttachmentStore(context),
                tombstoneRecorder = tombstoneRecorder,
            )
        val tombstoneMerger =
            LanSyncTombstoneMerger(
                database = graph.database,
                tombstoneDao = graph.database.syncTombstoneDao(),
                attachmentRepository = graph.transactionAttachmentRepository,
                assetAttachmentRepository = assetAttachmentRepository,
            )
        return LanSyncMerger(
            database = graph.database,
            balanceValidator = LanSyncAccountBalanceValidator(graph.database),
            tombstoneMerger = tombstoneMerger,
            widgetRefreshTrigger = NoOpHomeScreenWidgetRefreshTrigger,
            ownerProfileCoordinator = graph.ownerProfileCoordinator,
        )
    }
}
