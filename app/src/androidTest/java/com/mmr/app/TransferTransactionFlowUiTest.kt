package com.mmr.app

import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isEnabled
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.mmr.app.testing.MmrHiltUiTest
import com.mmr.app.testing.MmrTestTags
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@HiltAndroidTest
class TransferTransactionFlowUiTest : MmrHiltUiTest() {
    @Test
    fun addTransfer_betweenCashAndBank_appearsOnHome() {
        val note = "UiTest transfer ${System.currentTimeMillis()}"
        composeRule.onNodeWithTag(MmrTestTags.homeFabAdd).performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag(MmrTestTags.transactionTypeTransfer).performClick()
        composeRule.onNodeWithTag(MmrTestTags.transactionAmount).performTextInput("75")
        composeRule.onNodeWithTag(MmrTestTags.transactionNote).performTextInput(note)

        composeRule.onNodeWithText("From account").performClick()
        composeRule.waitForIdle()
        composeRule.onNode(hasText("Cash") and hasClickAction() and isEnabled()).performClick()

        composeRule.onNodeWithText("To").performClick()
        composeRule.waitForIdle()
        composeRule
            .onNode(hasText("Main Bank Account") and hasClickAction() and isEnabled())
            .performClick()

        composeRule.onNodeWithTag(MmrTestTags.appBarSave).performClick()

        composeRule.waitUntil(timeoutMillis = 10_000) {
            composeRule.onAllNodesWithText(note).fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText(note).assertExists()
    }
}
