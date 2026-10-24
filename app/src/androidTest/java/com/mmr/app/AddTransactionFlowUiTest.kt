package com.mmr.app

import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.mmr.app.testing.MmrHiltUiTest
import com.mmr.app.testing.MmrTestTags
import com.mmr.app.testing.assertTransactionRowShowsExpense
import com.mmr.app.testing.traceClickTag
import com.mmr.app.testing.traceTextInputTag
import com.mmr.app.testing.traceWaitUntil
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
        composeRule.traceClickTag(MmrTestTags.homeFabAdd)
        composeRule.waitForIdle()

        composeRule.traceTextInputTag(MmrTestTags.transactionAmount, "125.50")
        composeRule.traceTextInputTag(MmrTestTags.transactionNote, note)
        composeRule.traceClickTag(MmrTestTags.appBarSave)

        composeRule.traceWaitUntil("wait_until", timeoutMillis = 10_000) {
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
