package com.rrparallelstudio.miapp.core.transaction

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.rrparallelstudio.miapp.core.data.repository.MmrInstrumentedRepositoryGraph
import com.rrparallelstudio.miapp.core.database.entity.AccountEntity
import com.rrparallelstudio.miapp.core.model.enums.AccountType
import com.rrparallelstudio.miapp.core.model.enums.TransactionType
import com.rrparallelstudio.miapp.testing.MmrUiTestFixtures
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TransactionMergeCoordinatorInstrumentedTest {
    private lateinit var graph: MmrInstrumentedRepositoryGraph

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        graph = MmrInstrumentedRepositoryGraph(context)
        runBlocking {
            graph.database.accountDao().insertAccounts(
                listOf(
                    AccountEntity(name = "Cash", type = AccountType.CASH, isDefault = true),
                ),
            )
        }
    }

    @After
    fun tearDown() {
        graph.close()
    }

    @Test
    fun mergeTransactions_combinesAmountsAndDeletesSources() =
        runBlocking {
            val accountId = MmrUiTestFixtures.requireDefaultAccountId(graph.database)
            val (idA, idB) =
                MmrUiTestFixtures.seedTwoMergeableExpenses(
                    database = graph.database,
                    accountId = accountId,
                    amountA = 10.0,
                    amountB = 25.0,
                )

            val mergedId =
                graph.transactionMergeCoordinator.mergeTransactions(
                    transactionIds = listOf(idA, idB),
                    mergedName = "Merged groceries",
                )

            assertTrue(mergedId > 0L)
            val merged = graph.database.transactionDao().getById(mergedId)!!
            assertEquals(35.0, merged.amount, 0.001)
            assertEquals(TransactionType.EXPENSE, merged.type)
            assertEquals("Merged groceries", merged.note)
            assertNull(graph.database.transactionDao().getById(idA))
            assertNull(graph.database.transactionDao().getById(idB))
        }

    @Test
    fun mergeTransactions_netsIncomeAndExpense() =
        runBlocking {
            val accountId = MmrUiTestFixtures.requireDefaultAccountId(graph.database)
            val now = System.currentTimeMillis()
            val expenseId =
                MmrUiTestFixtures.insertApprovedTransaction(
                    graph.database,
                    accountId,
                    600.0,
                    TransactionType.EXPENSE,
                    now,
                    note = "Expense",
                )
            val incomeId =
                MmrUiTestFixtures.insertApprovedTransaction(
                    graph.database,
                    accountId,
                    100.0,
                    TransactionType.INCOME,
                    now + 1,
                    note = "Income",
                )

            val mergedId =
                graph.transactionMergeCoordinator.mergeTransactions(
                    transactionIds = listOf(expenseId, incomeId),
                    mergedName = "Netted",
                )

            val merged = graph.database.transactionDao().getById(mergedId)!!
            assertEquals(500.0, merged.amount, 0.001)
            assertEquals(TransactionType.EXPENSE, merged.type)
        }
}
