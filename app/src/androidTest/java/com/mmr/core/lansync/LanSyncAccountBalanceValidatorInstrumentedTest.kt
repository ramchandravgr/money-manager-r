package com.mmr.core.lansync

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.mmr.core.data.repository.MmrInstrumentedRepositoryGraph
import com.mmr.core.database.entity.AccountEntity
import com.mmr.core.database.entity.TransactionEntity
import com.mmr.core.model.enums.AccountType
import com.mmr.core.model.enums.RecordStatus
import com.mmr.core.model.enums.TransactionType
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LanSyncAccountBalanceValidatorInstrumentedTest {
    private lateinit var graph: MmrInstrumentedRepositoryGraph
    private lateinit var validator: LanSyncAccountBalanceValidator

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        graph = MmrInstrumentedRepositoryGraph(context)
        validator = LanSyncAccountBalanceValidator(graph.database)
    }

    @After
    fun tearDown() {
        graph.close()
    }

    @Test
    fun warningsAfterMerge_flagsStoredBalanceMismatch() =
        runBlocking {
            val accountId =
                graph.database.accountDao().insert(
                    AccountEntity(
                        name = "Test Cash",
                        type = AccountType.CASH,
                        openingBalance = 1_000.0,
                        currentBalance = 1_000.0,
                        isDefault = true,
                    ),
                )
            graph.database.transactionDao().insert(
                TransactionEntity(
                    dateTime = System.currentTimeMillis(),
                    amount = 100.0,
                    type = TransactionType.EXPENSE,
                    accountId = accountId,
                    status = RecordStatus.APPROVED,
                ),
            )

            val warnings = validator.warningsAfterMerge()
            assertEquals(1, warnings.size)
            assertTrue(warnings.single().contains("Test Cash"))
        }

    @Test
    fun warningsAfterMerge_emptyWhenBalanceMatchesLedger() =
        runBlocking {
            val accountId =
                graph.database.accountDao().insert(
                    AccountEntity(
                        name = "Aligned",
                        type = AccountType.CASH,
                        openingBalance = 500.0,
                        currentBalance = 400.0,
                        isDefault = true,
                    ),
                )
            graph.database.transactionDao().insert(
                TransactionEntity(
                    dateTime = System.currentTimeMillis(),
                    amount = 100.0,
                    type = TransactionType.EXPENSE,
                    accountId = accountId,
                    status = RecordStatus.APPROVED,
                ),
            )

            assertTrue(validator.warningsAfterMerge().isEmpty())
        }
}
