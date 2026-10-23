package com.mmr.app

import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.mmr.app.testing.MmrHiltUiTest
import com.mmr.app.testing.MmrTestTags
import com.mmr.app.testing.assertTransactionRowShowsExpense
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@HiltAndroidTest
class AddTransactionFlowUiTest : MmrHiltUiTest() {
    @Test
    fun addExpense_appearsOnHomeList() {
        val note = "UiTest expense ${System.currentTimeMillis()}"
        composeRule.onNodeWithTag(MmrTestTags.homeFabAdd).performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag(MmrTestTags.transactionAmount).performTextInput("125.50")
        composeRule.onNodeWithTag(MmrTestTags.transactionNote).performTextInput(note)
        composeRule.onNodeWithTag(MmrTestTags.appBarSave).performClick()

        composeRule.waitUntil(timeoutMillis = 10_000) {
            composeRule.onAllNodesWithText(note).fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText(note).assertExists()
        val transactionId =
            runBlocking {
                database
                    .transactionDao()
                    .getApprovedTransactionsDetailed()
                    .first()
                    .single { it.transaction.note == note }
                    .transaction.id
            }
        composeRule.assertTransactionRowShowsExpense(transactionId, 125.50)
    }
}
