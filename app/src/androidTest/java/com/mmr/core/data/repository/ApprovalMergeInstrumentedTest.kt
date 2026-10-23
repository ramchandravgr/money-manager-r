package com.mmr.core.data.repository

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.mmr.app.testing.MmrUiTestFixtures
import com.mmr.core.database.entity.AccountEntity
import com.mmr.core.model.enums.AccountType
import com.mmr.core.model.enums.SmsApprovalState
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ApprovalMergeInstrumentedTest {
    private lateinit var graph: MmrInstrumentedRepositoryGraph
    private lateinit var repository: ApprovalRepositoryImpl

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        graph = MmrInstrumentedRepositoryGraph(context)
        repository = graph.approvalRepository
        runBlocking {
            graph.database.accountDao().insert(
                AccountEntity(id = 1L, name = "Bank", type = AccountType.BANK, isDefault = true),
            )
        }
    }

    @After
    fun tearDown() {
        graph.close()
    }

    @Test
    fun mergeCandidateWithTransaction_doublesAmountOnLedgerRow() =
        runBlocking {
            val fixture = MmrUiTestFixtures.seedSmsMergeScenario(graph.database, repository, amount = 100.0)
            val candidates = repository.findMergeCandidatesForApproval(fixture.approvalId)
            assertEquals(1, candidates.size)
            assertEquals(fixture.transactionId, candidates.first().transactionId)

            val mergedTxnId =
                repository.mergeCandidateWithTransaction(
                    approvalId = fixture.approvalId,
                    transactionId = fixture.transactionId,
                    categoryId = null,
                    subcategoryId = null,
                    useLedgerCategory = true,
                )
            assertEquals(fixture.transactionId, mergedTxnId)

            val txn = graph.database.transactionDao().getById(fixture.transactionId)!!
            assertEquals(200.0, txn.amount, 0.001)

            val approval = repository.getApprovalById(fixture.approvalId)!!
            assertEquals(SmsApprovalState.APPROVED, approval.approvalState)
            assertTrue(
                graph.database
                    .transactionDao()
                    .getApprovedTransactionsDetailed()
                    .first()
                    .size == 1,
            )
        }
}
