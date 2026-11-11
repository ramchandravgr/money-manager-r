package com.rrparallelstudio.miapp.core.lansync

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.rrparallelstudio.miapp.core.data.repository.MmrInstrumentedRepositoryGraph
import com.rrparallelstudio.miapp.core.database.entity.AccountEntity
import com.rrparallelstudio.miapp.core.database.entity.TransactionEntity
import com.rrparallelstudio.miapp.core.model.enums.AccountType
import com.rrparallelstudio.miapp.core.model.enums.RecordStatus
import com.rrparallelstudio.miapp.core.model.enums.TransactionType
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
    fun reconcileAfterMerge_setsStoredBalanceFromTransactions() =
        runBlocking {
            val accountId =
                graph.database.accountDao().insert(
                    AccountEntity(
                        name = "Shared",
                        type = AccountType.CASH,
                        openingBalance = 5_000.0,
                        currentBalance = 2_000.0,
                        isDefault = true,
                    ),
                )
            graph.database.transactionDao().insert(
                TransactionEntity(
                    dateTime = System.currentTimeMillis(),
                    amount = 3_000.0,
                    type = TransactionType.EXPENSE,
                    accountId = accountId,
                    status = RecordStatus.APPROVED,
                ),
            )
            graph.database.transactionDao().insert(
                TransactionEntity(
                    dateTime = System.currentTimeMillis() + 1,
                    amount = 200.0,
                    type = TransactionType.EXPENSE,
                    accountId = accountId,
                    status = RecordStatus.APPROVED,
                ),
            )

            assertTrue(validator.reconcileAfterMerge().isEmpty())
            val stored = graph.database.accountDao().getById(accountId)!!
            assertEquals(1_800.0, stored.currentBalance, 0.001)
        }

    @Test
    fun reconcileAfterMerge_emptyWhenBalanceAlreadyMatches() =
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

            assertTrue(validator.reconcileAfterMerge().isEmpty())
            assertEquals(
                400.0,
                graph.database
                    .accountDao()
                    .getById(accountId)!!
                    .currentBalance,
                0.001,
            )
        }
}
