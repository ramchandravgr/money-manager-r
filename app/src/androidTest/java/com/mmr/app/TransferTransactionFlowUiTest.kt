package com.mmr.app

import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isEnabled
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.mmr.app.testing.MmrHiltUiTest
import com.mmr.app.testing.MmrTestTags
import com.mmr.app.testing.traceClickTag
import com.mmr.app.testing.traceClickText
import com.mmr.app.testing.traceMatcherClick
import com.mmr.app.testing.traceTextInputTag
import com.mmr.app.testing.traceWaitUntil
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@HiltAndroidTest
class TransferTransactionFlowUiTest : MmrHiltUiTest() {
    @Test
    fun addTransfer_betweenCashAndBank_appearsOnHome() {
        val note = "UiTest transfer ${System.currentTimeMillis()}"
        composeRule.traceClickTag(MmrTestTags.homeFabAdd)
        composeRule.waitForIdle()

        composeRule.traceClickTag(MmrTestTags.transactionTypeTransfer)
        composeRule.traceTextInputTag(MmrTestTags.transactionAmount, "75")
        composeRule.traceTextInputTag(MmrTestTags.transactionNote, note)

        composeRule.traceClickText("From account")
        composeRule.waitForIdle()
        composeRule.traceMatcherClick(hasText("Cash") and hasClickAction() and isEnabled(), "pick_cash_account")

        composeRule.traceClickText("To")
        composeRule.waitForIdle()
        composeRule.traceMatcherClick(
            hasText("Main Bank Account") and hasClickAction() and isEnabled(),
            "pick_main_bank_account",
        )

        composeRule.traceClickTag(MmrTestTags.appBarSave)

        composeRule.traceWaitUntil("wait_until", timeoutMillis = 10_000) {
            composeRule.onAllNodesWithText(note).fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText(note).assertExists()
    }
}
