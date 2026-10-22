package com.mmr.core.data.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.mmr.core.database.MmrDatabase
import com.mmr.core.database.entity.AccountEntity
import com.mmr.core.model.enums.AccountType
import com.mmr.core.model.enums.SmsApprovalState
import com.mmr.core.parser.model.ParserConfidence
import com.mmr.core.model.enums.TransactionType
import com.mmr.core.parser.account.SmsAccountResolver
import com.mmr.core.parser.engine.SmsParserEngine
import com.mmr.core.parser.model.ParsedSmsCandidate
import com.mmr.core.parser.template.SmsTemplateRegistry
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

    private lateinit var database: MmrDatabase
    private lateinit var repository: ApprovalRepositoryImpl

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, MmrDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = ApprovalRepositoryImpl(
            transactionApprovalDao = database.transactionApprovalDao(),
            approvalLedgerDao = database.approvalLedgerDao(),
            categoryDao = database.categoryDao(),
            transactionDao = database.transactionDao(),
            smsAccountResolver = SmsAccountResolver(database.accountDao()),
            smsParserEngine = SmsParserEngine(SmsTemplateRegistry(context))
        )
        runBlocking {
            database.accountDao().insert(
                AccountEntity(id = 1L, name = "Bank", type = AccountType.BANK, isDefault = true)
            )
        }
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun insertCandidates_skipsDuplicateHash() = runBlocking {
        val candidate = sampleCandidate(hash = "hash-1")
        assertEquals(1, repository.insertCandidates(listOf(candidate)))
        assertEquals(0, repository.insertCandidates(listOf(candidate)))

        val pending = repository.getApprovalsByState(SmsApprovalState.PENDING).first()
        assertEquals(1, pending.size)
    }

    @Test
    fun approveCandidate_createsLedgerAndMarksApproved() = runBlocking {
        val inserted = repository.insertCandidates(listOf(sampleCandidate(hash = "hash-approve")))
        assertEquals(1, inserted)

        val pending = repository.getApprovalsByState(SmsApprovalState.PENDING).first()
        val approvalId = pending.first().id

        val transactionId = repository.approveCandidate(approvalId, fallbackAccountId = 1L, fallbackCategoryId = null)
        assertTrue(transactionId > 0L)

        val after = repository.getApprovalById(approvalId)!!
        assertEquals(SmsApprovalState.APPROVED, after.approvalState)
        assertEquals(1, database.transactionDao().getApprovedTransactionsDetailed().first().size)
    }

    @Test
    fun approveCandidate_twice_throwsForSecondCall() {
        runBlocking {
            repository.insertCandidates(listOf(sampleCandidate(hash = "hash-twice")))
            val approvalId = repository.getApprovalsByState(SmsApprovalState.PENDING).first().first().id

            repository.approveCandidate(approvalId, 1L, null)
            assertThrows(IllegalStateException::class.java) {
                runBlocking {
                    repository.approveCandidate(approvalId, 1L, null)
                }
            }
        }
    }

    @Test
    fun rejectCandidate_marksRejectedAtomically() = runBlocking {
        repository.insertCandidates(listOf(sampleCandidate(hash = "hash-reject")))
        val approvalId = repository.getApprovalsByState(SmsApprovalState.PENDING).first().first().id

        repository.rejectCandidate(approvalId)

        val approval = repository.getApprovalById(approvalId)!!
        assertEquals(SmsApprovalState.REJECTED, approval.approvalState)
        val sms = database.smsMessageRecordDao().getById(approval.smsMessageRecordId)!!
        assertEquals(SmsApprovalState.REJECTED, sms.approvalState)
    }

    private fun sampleCandidate(hash: String) = ParsedSmsCandidate(
        sender = "BANK",
        amount = 250.0,
        type = TransactionType.EXPENSE,
        timestamp = 1_700_000_000_000L,
        merchant = "Store",
        referenceNumber = null,
        accountLastDigits = null,
        balanceAfter = null,
        categoryHint = null,
        rawMessageId = "sms-$hash",
        messageHash = hash,
        confidenceScore = 90,
        confidenceBand = ParserConfidence.HIGH,
        rawBody = "spent 250"
    )
}
