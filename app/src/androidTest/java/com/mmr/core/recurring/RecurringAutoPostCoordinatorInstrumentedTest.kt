package com.mmr.core.recurring

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.mmr.core.data.repository.MmrInstrumentedRepositoryGraph
import com.mmr.core.database.entity.AccountEntity
import com.mmr.core.database.entity.RecurringRuleEntity
import com.mmr.core.model.enums.AccountType
import com.mmr.core.model.enums.RecurrenceInterval
import com.mmr.core.model.enums.TransactionType
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.TimeUnit

@RunWith(AndroidJUnit4::class)
class RecurringAutoPostCoordinatorInstrumentedTest {
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
    fun postDueRecurringTransactions_createsLedgerRowAndAdvancesSchedule() =
        runBlocking {
            graph.database.accountDao().insert(
                AccountEntity(id = 1L, name = "Cash", type = AccountType.CASH, isDefault = true),
            )
            val dueDate = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(2)
            val ruleId =
                graph.recurringRuleRepository.createRecurringRule(
                    RecurringRuleEntity(
                        name = "Monthly SIP",
                        transactionTemplateType = TransactionType.EXPENSE,
                        amount = 5000.0,
                        accountId = 1L,
                        recurrenceInterval = RecurrenceInterval.MONTHLY,
                        dayOfMonth = 1,
                        nextRunDate = dueDate,
                        isEnabled = true,
                    ),
                )
            assertTrue(ruleId > 0L)

            val created = graph.recurringAutoPostCoordinator.postDueRecurringTransactions()
            assertTrue(created >= 1)

            val ref = RecurringSourceRefs.occurrence(ruleId, dueDate)
            val txnId = graph.database.transactionDao().findIdBySourceReference(ref)
            assertNotNull(txnId)

            val rule = graph.recurringRuleRepository.getRuleById(ruleId)!!
            assertTrue(rule.nextRunDate > dueDate)
            assertNotNull(rule.lastGeneratedDate)
        }
}
