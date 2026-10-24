package com.mmr.app

import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.mmr.app.testing.MmrHiltUiTest
import com.mmr.app.testing.MmrTestTags
import com.mmr.app.testing.MmrUiTestFixtures
import com.mmr.app.testing.traceClickTag
import com.mmr.app.testing.traceClickText
import com.mmr.app.testing.traceTextClearanceTag
import com.mmr.app.testing.traceTextInputTag
import com.mmr.app.testing.traceWaitUntil
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@HiltAndroidTest
class TransactionDetailFlowUiTest : MmrHiltUiTest() {
    @Test
    fun editNote_fromDetail_updatesHomeList() {
        val originalNote = "Detail edit ${System.nanoTime()}"
        val updatedNote = "$originalNote updated"
        val fixture =
            runBlocking {
                MmrUiTestFixtures.seedSingleExpense(database, note = originalNote, amount = 99.0)
            }

        traceActivityRecreate()

        composeRule.traceClickTag(MmrTestTags.transactionRow(fixture.transactionId))
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Transaction").assertExists()

        composeRule.traceClickTag(MmrTestTags.transactionDetailEdit)
        composeRule.waitForIdle()
        composeRule.traceTextClearanceTag(MmrTestTags.transactionNote)
        composeRule.traceTextInputTag(MmrTestTags.transactionNote, updatedNote)
        composeRule.traceClickTag(MmrTestTags.appBarSave)

        composeRule.traceWaitUntil("wait_until", timeoutMillis = 10_000) {
            composeRule.onAllNodesWithText(updatedNote).fetchSemanticsNodes().isNotEmpty()
        }
    }

    @Test
    fun duplicateToday_fromDetail_createsSecondHomeRow() {
        val note = "Detail duplicate ${System.nanoTime()}"
        val fixture =
            runBlocking {
                MmrUiTestFixtures.seedSingleExpense(database, note = note, amount = 99.0)
            }

        traceActivityRecreate()

        composeRule.traceClickTag(MmrTestTags.transactionRow(fixture.transactionId))
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Transaction").assertExists()

        composeRule.traceClickTag(MmrTestTags.transactionDetailDuplicate)
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Duplicate transaction").assertExists()
        composeRule.traceClickTag(MmrTestTags.transactionDuplicateToday)

        composeRule.waitForIdle()
        composeRule.onNodeWithTag(MmrTestTags.transactionNote).assertTextContains(note)
        composeRule.traceClickTag(MmrTestTags.appBarSave)

        composeRule.traceWaitUntil("wait_until", timeoutMillis = 10_000) {
            composeRule.onAllNodesWithText(note).fetchSemanticsNodes().size >= 2
        }
    }

    @Test
    fun deleteFromDetail_removesTransaction() {
        val note = "Detail delete ${System.nanoTime()}"
        val fixture =
            runBlocking {
                MmrUiTestFixtures.seedSingleExpense(database, note = note, amount = 11.0)
            }

        traceActivityRecreate()

        composeRule.traceClickTag(MmrTestTags.transactionRow(fixture.transactionId))
        composeRule.traceClickTag(MmrTestTags.transactionDetailDelete)
        composeRule.traceClickText("Delete")

        composeRule.traceWaitUntil("wait_until", timeoutMillis = 10_000) {
            runCatching {
                composeRule.onNodeWithText(note).assertDoesNotExist()
                true
            }.getOrDefault(false)
        }
    }
}
