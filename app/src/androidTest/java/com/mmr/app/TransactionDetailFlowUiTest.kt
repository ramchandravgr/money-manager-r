package com.mmr.app

import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.mmr.app.testing.MmrHiltUiTest
import com.mmr.app.testing.MmrTestTags
import com.mmr.app.testing.MmrUiTestFixtures
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

        composeRule.activityRule.scenario.recreate()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag(MmrTestTags.transactionRow(fixture.transactionId)).performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Transaction").assertExists()

        composeRule.onNodeWithTag(MmrTestTags.transactionDetailEdit).performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag(MmrTestTags.transactionNote).performTextClearance()
        composeRule.onNodeWithTag(MmrTestTags.transactionNote).performTextInput(updatedNote)
        composeRule.onNodeWithTag(MmrTestTags.appBarSave).performClick()

        composeRule.waitUntil(timeoutMillis = 10_000) {
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

        composeRule.activityRule.scenario.recreate()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag(MmrTestTags.transactionRow(fixture.transactionId)).performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Transaction").assertExists()

        composeRule.onNodeWithTag(MmrTestTags.transactionDetailDuplicate).performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Duplicate transaction").assertExists()
        composeRule.onNodeWithTag(MmrTestTags.transactionDuplicateToday).performClick()

        composeRule.waitForIdle()
        composeRule.onNodeWithTag(MmrTestTags.transactionNote).assertTextContains(note)
        composeRule.onNodeWithTag(MmrTestTags.appBarSave).performClick()

        composeRule.waitUntil(timeoutMillis = 10_000) {
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

        composeRule.activityRule.scenario.recreate()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag(MmrTestTags.transactionRow(fixture.transactionId)).performClick()
        composeRule.onNodeWithTag(MmrTestTags.transactionDetailDelete).performClick()
        composeRule.onNodeWithText("Delete").performClick()

        composeRule.waitUntil(timeoutMillis = 10_000) {
            runCatching {
                composeRule.onNodeWithText(note).assertDoesNotExist()
                true
            }.getOrDefault(false)
        }
    }
}
