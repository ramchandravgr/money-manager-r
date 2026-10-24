package com.mmr.core.data.repository

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.mmr.core.database.entity.AccountEntity
import com.mmr.core.model.enums.AccountType
import com.mmr.core.model.enums.SmsApprovalKind
import com.mmr.core.model.enums.SmsApprovalState
import com.mmr.core.model.enums.SmsLinkRole
import com.mmr.core.model.enums.TransactionType
import com.mmr.core.parser.model.ParsedSmsCandidate
import com.mmr.core.parser.model.ParserConfidence
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ApprovalRepositoryInstrumentedTest {
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
    fun insertCandidates_skipsDuplicateHash() =
        runBlocking {
            val candidate = sampleCandidate(hash = "hash-1")
            assertEquals(1, repository.insertCandidates(listOf(candidate)))
            assertEquals(0, repository.insertCandidates(listOf(candidate)))

            val pending = repository.getApprovalsByState(SmsApprovalState.PENDING).first()
            assertEquals(1, pending.size)
        }

    @Test
    fun approveCandidate_createsLedgerAndMarksApproved() =
        runBlocking {
            val inserted = repository.insertCandidates(listOf(sampleCandidate(hash = "hash-approve")))
            assertEquals(1, inserted)

            val pending = repository.getApprovalsByState(SmsApprovalState.PENDING).first()
            val approvalId = pending.first().id

            val transactionId =
                repository.approveCandidate(
                    approvalId,
                    fallbackAccountId = 1L,
                    fallbackCategoryId = null,
                )
            assertTrue(transactionId > 0L)

            val after = repository.getApprovalById(approvalId)!!
            assertEquals(SmsApprovalState.APPROVED, after.approvalState)
            assertEquals(
                1,
                graph.database
                    .transactionDao()
                    .getApprovedTransactionsDetailed()
                    .first()
                    .size,
            )
        }

    @Test
    fun approveCandidate_twice_throwsForSecondCall() {
        runBlocking {
            repository.insertCandidates(listOf(sampleCandidate(hash = "hash-twice")))
            val approvalId =
                repository
                    .getApprovalsByState(SmsApprovalState.PENDING)
                    .first()
                    .first()
                    .id

            repository.approveCandidate(approvalId, 1L, null)
            assertThrows(IllegalStateException::class.java) {
                runBlocking {
                    repository.approveCandidate(approvalId, 1L, null)
                }
            }
        }
    }

    @Test
    fun applyCardRefund_partialReducesPurchaseAndLinksBothSms() =
        runBlocking {
            val purchaseHash = "hash-purchase-cc"
            val purchaseCandidate =
                sampleCandidate(hash = purchaseHash, amount = 500.0, last4 = "4148")
            repository.insertCandidates(listOf(purchaseCandidate))
            val purchaseApprovalId =
                repository
                    .getApprovalsByState(SmsApprovalState.PENDING)
                    .first()
                    .first()
                    .id
            val purchaseTxnId = repository.approveCandidate(purchaseApprovalId, 1L, null)

            val refundCandidate =
                refundCandidate(hash = "hash-refund-partial", amount = 150.0, last4 = "4148")
            repository.insertCandidates(listOf(refundCandidate))
            val refundApprovalId =
                repository
                    .getApprovalsByState(SmsApprovalState.PENDING)
                    .first()
                    .first { it.approvalKind == SmsApprovalKind.CARD_REFUND }
                    .id

            val txnId =
                repository.applyCardRefund(
                    approvalId = refundApprovalId,
                    purchaseTransactionId = purchaseTxnId,
                    refundAmount = 150.0,
                )
            assertEquals(purchaseTxnId, txnId)

            val txn = graph.database.transactionDao().getById(purchaseTxnId)!!
            assertEquals(350.0, txn.amount, 0.01)
            assertEquals(500.0, txn.originalAmount!!, 0.01)
            assertEquals(150.0, txn.refundedTotal, 0.01)

            val linked =
                graph.database
                    .smsMessageRecordDao()
                    .getByMatchedTransactionId(purchaseTxnId)
                    .first()
            assertEquals(2, linked.size)
            assertTrue(linked.any { it.linkRole == SmsLinkRole.PURCHASE })
            assertTrue(linked.any { it.linkRole == SmsLinkRole.REFUND })

            val refundApproval = repository.getApprovalById(refundApprovalId)!!
            assertEquals(SmsApprovalState.APPROVED, refundApproval.approvalState)
        }

    @Test
    fun applyCardRefund_fullRefundZerosNetAmount() =
        runBlocking {
            repository.insertCandidates(
                listOf(sampleCandidate(hash = "hash-purchase-full", amount = 200.0, last4 = "0099")),
            )
            val purchaseApprovalId =
                repository
                    .getApprovalsByState(SmsApprovalState.PENDING)
                    .first()
                    .first()
                    .id
            val purchaseTxnId = repository.approveCandidate(purchaseApprovalId, 1L, null)

            repository.insertCandidates(
                listOf(refundCandidate(hash = "hash-refund-full", amount = 200.0, last4 = "0099")),
            )
            val refundApprovalId =
                repository
                    .getApprovalsByState(SmsApprovalState.PENDING)
                    .first()
                    .first()
                    .id

            repository.applyCardRefund(refundApprovalId, purchaseTxnId, 200.0)

            val txn = graph.database.transactionDao().getById(purchaseTxnId)!!
            assertEquals(0.0, txn.amount, 0.01)
            assertEquals(200.0, txn.refundedTotal, 0.01)
        }

    @Test
    fun applyCardRefund_exceedsPurchaseAmount_throws() {
        runBlocking {
            repository.insertCandidates(
                listOf(sampleCandidate(hash = "hash-purchase-small", amount = 50.0, last4 = "1111")),
            )
            val purchaseTxnId =
                repository.approveCandidate(
                    repository
                        .getApprovalsByState(SmsApprovalState.PENDING)
                        .first()
                        .first()
                        .id,
                    1L,
                    null,
                )
            repository.insertCandidates(
                listOf(refundCandidate(hash = "hash-refund-big", amount = 100.0, last4 = "1111")),
            )
            val refundApprovalId =
                repository
                    .getApprovalsByState(SmsApprovalState.PENDING)
                    .first()
                    .first()
                    .id

            assertThrows(IllegalArgumentException::class.java) {
                runBlocking {
                    repository.applyCardRefund(refundApprovalId, purchaseTxnId, 100.0)
                }
            }
        }
    }

    @Test
    fun rejectCandidate_marksRejectedAtomically() =
        runBlocking {
            repository.insertCandidates(listOf(sampleCandidate(hash = "hash-reject")))
            val approvalId =
                repository
                    .getApprovalsByState(SmsApprovalState.PENDING)
                    .first()
                    .first()
                    .id

            repository.rejectCandidate(approvalId)

            val approval = repository.getApprovalById(approvalId)!!
            assertEquals(SmsApprovalState.REJECTED, approval.approvalState)
            val sms = graph.database.smsMessageRecordDao().getById(approval.smsMessageRecordId)!!
            assertEquals(SmsApprovalState.REJECTED, sms.approvalState)
        }

    private fun sampleCandidate(
        hash: String,
        amount: Double = 250.0,
        last4: String? = null,
    ) = ParsedSmsCandidate(
        sender = "BANK",
        amount = amount,
        type = TransactionType.EXPENSE,
        timestamp = 1_700_000_000_000L,
        merchant = "Store",
        referenceNumber = null,
        accountLastDigits = last4,
        balanceAfter = null,
        categoryHint = null,
        rawMessageId = "sms-$hash",
        messageHash = hash,
        confidenceScore = 90,
        confidenceBand = ParserConfidence.HIGH,
        rawBody = "spent $amount",
    )

    private fun refundCandidate(
        hash: String,
        amount: Double,
        last4: String,
    ) = ParsedSmsCandidate(
        sender = "HDFCBK",
        amount = amount,
        type = TransactionType.EXPENSE,
        timestamp = 1_700_000_100_000L,
        merchant = "Amazon",
        referenceNumber = null,
        accountLastDigits = last4,
        balanceAfter = null,
        categoryHint = null,
        rawMessageId = "sms-$hash",
        messageHash = hash,
        confidenceScore = 88,
        confidenceBand = ParserConfidence.HIGH,
        rawBody = "Transaction Reversed! card xx$last4 Amt: Rs.$amount",
        approvalKind = SmsApprovalKind.CARD_REFUND,
    )
}
