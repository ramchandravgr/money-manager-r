package com.mmr.app

import androidx.compose.ui.test.onAllNodesWithTag
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.mmr.app.testing.MmrHiltUiTest
import com.mmr.app.testing.MmrTestTags
import com.mmr.app.testing.MmrUiTestFixtures
import com.mmr.app.testing.assertTransactionRowShowsExpense
import com.mmr.app.testing.traceClickTag
import com.mmr.app.testing.traceClickText
import com.mmr.app.testing.traceTextInputTag
import com.mmr.app.testing.traceWaitUntil
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

        traceActivityRecreate()
        composeRule.assertTransactionRowShowsExpense(fixture.transactionId, 88.0)

        composeRule.traceClickTag(MmrTestTags.homeTopBarSearch)
        composeRule.traceWaitUntil("wait_until", timeoutMillis = 10_000) {
            composeRule.onAllNodesWithTag(MmrTestTags.searchNoteQuery).fetchSemanticsNodes().isNotEmpty()
        }

        composeRule.traceClickTag(MmrTestTags.searchPeriodChip)
        composeRule.traceClickText("Year")
        composeRule.waitForIdle()

        composeRule.traceClickText("Hide filters", substring = true)
        composeRule.waitForIdle()

        composeRule.traceClickTag(MmrTestTags.searchNoteQuery)
        composeRule.traceTextInputTag(MmrTestTags.searchNoteQuery, note)
        composeRule.traceWaitUntil("wait_until", timeoutMillis = 15_000) {
            composeRule
                .onAllNodesWithTag(MmrTestTags.transactionRow(fixture.transactionId))
                .fetchSemanticsNodes()
                .isNotEmpty()
        }
        composeRule.assertTransactionRowShowsExpense(fixture.transactionId, 88.0)
    }
}
