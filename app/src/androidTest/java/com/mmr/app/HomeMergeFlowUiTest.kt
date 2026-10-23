package com.mmr.app

import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTouchInput
import androidx.test.espresso.Espresso
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.mmr.app.testing.MmrHiltUiTest
import com.mmr.app.testing.MmrTestTags
import com.mmr.app.testing.MmrUiTestFixtures
import com.mmr.app.testing.assertTransactionRowShowsExpense
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

        composeRule.activityRule.scenario.recreate()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag(MmrTestTags.transactionRow(idA)).performTouchInput { longClick() }
        composeRule.onNodeWithTag(MmrTestTags.transactionRow(idB)).performClick()
        composeRule.waitUntil(timeoutMillis = 10_000) {
            composeRule.onAllNodesWithTag(MmrTestTags.homeSelectionMerge).fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithTag(MmrTestTags.homeSelectionMerge).performClick()

        val mergedName = "Merged UI ${System.nanoTime()}"
        composeRule.waitUntil(timeoutMillis = 10_000) {
            composeRule.onAllNodesWithText("Merge transactions").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNode(hasSetTextAction()).performTextInput(mergedName)
        composeRule.onNode(hasText("Merge") and hasClickAction()).performClick()
        composeRule.waitForIdle()
        Espresso.pressBack()
        composeRule.waitForIdle()

        composeRule.waitUntil(timeoutMillis = 10_000) {
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
