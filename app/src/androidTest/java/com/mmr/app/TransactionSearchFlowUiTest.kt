package com.mmr.app

import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.mmr.app.testing.MmrHiltUiTest
import com.mmr.app.testing.MmrTestTags
import com.mmr.app.testing.MmrUiTestFixtures
import com.mmr.app.testing.assertTransactionRowShowsExpense
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@HiltAndroidTest
class TransactionSearchFlowUiTest : MmrHiltUiTest() {
    @Test
    fun searchByNote_findsSeededTransaction() {
        val note = "UiTest search ${System.currentTimeMillis()}"
        val fixture =
            runBlocking {
                MmrUiTestFixtures.seedSearchTarget(database, note = note, amount = 88.0)
            }

        composeRule.activityRule.scenario.recreate()
        composeRule.waitForIdle()
        composeRule.assertTransactionRowShowsExpense(fixture.transactionId, 88.0)

        composeRule.onNodeWithTag(MmrTestTags.homeTopBarSearch).performClick()
        composeRule.waitUntil(timeoutMillis = 10_000) {
            composeRule.onAllNodesWithTag(MmrTestTags.searchNoteQuery).fetchSemanticsNodes().isNotEmpty()
        }

        composeRule.onNodeWithTag(MmrTestTags.searchPeriodChip).performClick()
        composeRule.onNodeWithText("Year").performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithText("Hide filters", substring = true).performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag(MmrTestTags.searchNoteQuery).performClick()
        composeRule.onNodeWithTag(MmrTestTags.searchNoteQuery).performTextInput(note)
        composeRule.waitUntil(timeoutMillis = 15_000) {
            composeRule
                .onAllNodesWithTag(MmrTestTags.transactionRow(fixture.transactionId))
                .fetchSemanticsNodes()
                .isNotEmpty()
        }
        composeRule.assertTransactionRowShowsExpense(fixture.transactionId, 88.0)
    }
}
