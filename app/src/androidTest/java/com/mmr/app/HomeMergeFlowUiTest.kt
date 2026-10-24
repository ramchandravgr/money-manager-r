package com.mmr.app

import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.test.espresso.Espresso
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.mmr.app.testing.MmrHiltUiTest
import com.mmr.app.testing.MmrTestTags
import com.mmr.app.testing.MmrUiTestFixtures
import com.mmr.app.testing.assertTransactionRowShowsExpense
import com.mmr.app.testing.traceAction
import com.mmr.app.testing.traceClickTag
import com.mmr.app.testing.traceLongClickTag
import com.mmr.app.testing.traceMatcherClick
import com.mmr.app.testing.traceNodeTextInput
import com.mmr.app.testing.traceWaitUntil
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@HiltAndroidTest
class HomeMergeFlowUiTest : MmrHiltUiTest() {
    @Test
    fun mergeSelectedExpenses_combinesAmountOnHome() {
        val (idA, idB) =
            runBlocking {
                val accountId = MmrUiTestFixtures.requireDefaultAccountId(database)
                MmrUiTestFixtures.seedTwoMergeableExpenses(database, accountId, 10.0, 20.0)
            }

        traceActivityRecreate()

        composeRule.traceLongClickTag(MmrTestTags.transactionRow(idA))
        composeRule.traceClickTag(MmrTestTags.transactionRow(idB))
        composeRule.traceWaitUntil("wait_until", timeoutMillis = 10_000) {
            composeRule.onAllNodesWithTag(MmrTestTags.homeSelectionMerge).fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.traceClickTag(MmrTestTags.homeSelectionMerge)

        val mergedName = "Merged UI ${System.nanoTime()}"
        composeRule.traceWaitUntil("wait_until", timeoutMillis = 10_000) {
            composeRule.onAllNodesWithText("Merge transactions").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.traceNodeTextInput("set_text_action", { composeRule.onNode(hasSetTextAction()) }, mergedName)
        composeRule.traceMatcherClick(hasText("Merge") and hasClickAction(), "merge_confirm")
        composeRule.waitForIdle()
        composeRule.traceAction("system_back") {
            Espresso.pressBack()
        }

        composeRule.traceWaitUntil("wait_until", timeoutMillis = 10_000) {
            runBlocking {
                database
                    .transactionDao()
                    .getApprovedTransactionsDetailed()
                    .first()
                    .any { it.transaction.note == mergedName }
            }
        }
        val mergedId =
            runBlocking {
                database
                    .transactionDao()
                    .getApprovedTransactionsDetailed()
                    .first()
                    .single { it.transaction.note == mergedName }
                    .transaction.id
            }
        composeRule.assertTransactionRowShowsExpense(mergedId, 30.0)
    }
}
