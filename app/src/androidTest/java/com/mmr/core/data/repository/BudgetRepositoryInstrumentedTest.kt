package com.mmr.core.data.repository

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.mmr.core.common.sync.MmrSyncId
import com.mmr.core.database.entity.AccountEntity
import com.mmr.core.database.entity.BudgetEntity
import com.mmr.core.database.entity.CategoryEntity
import com.mmr.core.database.entity.TransactionEntity
import com.mmr.core.model.enums.AccountType
import com.mmr.core.model.enums.BudgetPeriodType
import com.mmr.core.model.enums.CategoryType
import com.mmr.core.model.enums.RecordStatus
import com.mmr.core.model.enums.TransactionSourceType
import com.mmr.core.model.enums.TransactionType
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BudgetRepositoryInstrumentedTest {
    private lateinit var graph: MmrInstrumentedRepositoryGraph

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        graph = MmrInstrumentedRepositoryGraph(context)
    }

    @After
    fun tearDown() {
        graph.close()
    }

    @Test
    fun observeActiveBudgetProgress_reflectsExpenseSum() =
        runBlocking {
            val categoryId =
                graph.database.categoryDao().insert(
                    CategoryEntity(name = "Test", type = CategoryType.EXPENSE),
                )
            val accountId =
                graph.database.accountDao().insert(
                    AccountEntity(name = "Cash", type = AccountType.CASH, isDefault = true),
                )
            val now = System.currentTimeMillis()
            graph.database.budgetDao().insert(
                BudgetEntity(
                    syncId = MmrSyncId.newId(),
                    categoryId = categoryId,
                    amountLimit = 200.0,
                    periodType = BudgetPeriodType.MONTHLY,
                    createdAt = now,
                    updatedAt = now,
                ),
            )
            graph.database.transactionDao().insert(
                TransactionEntity(
                    dateTime = now,
                    amount = 50.0,
                    type = TransactionType.EXPENSE,
                    categoryId = categoryId,
                    accountId = accountId,
                    status = RecordStatus.APPROVED,
                    sourceType = TransactionSourceType.MANUAL,
                ),
            )
            val snapshots = graph.budgetRepository.observeActiveBudgetProgress().first()
            assertEquals(1, snapshots.size)
            assertEquals(50.0, snapshots.first().spentAmount, 0.001)
            assertTrue(snapshots.first().percentUsed == 25)
        }
}
