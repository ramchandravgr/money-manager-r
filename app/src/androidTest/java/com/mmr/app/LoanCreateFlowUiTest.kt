package com.mmr.app

import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.mmr.app.testing.MmrHiltUiTest
import com.mmr.app.testing.MmrTestTags
import com.mmr.app.testing.traceClickTag
import com.mmr.app.testing.traceClickText
import com.mmr.app.testing.traceTextInputTag
import com.mmr.app.testing.traceWaitUntil
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@HiltAndroidTest
class LoanCreateFlowUiTest : MmrHiltUiTest() {
    @Test
    fun createFlexibleLoan_appearsOnLoanList() {
        val loanName = "UiTest loan ${System.nanoTime()}"

        composeRule.traceClickTag(MmrTestTags.navLoans)
        composeRule.waitForIdle()
        composeRule.traceClickTag(MmrTestTags.loanFabAdd)
        composeRule.waitForIdle()
        composeRule.onNodeWithText("New Loan Entry").assertExists()

        composeRule.traceTextInputTag(MmrTestTags.loanFieldName, loanName)
        composeRule.traceTextInputTag(MmrTestTags.loanFieldLender, "Test Bank")
        composeRule.traceTextInputTag(MmrTestTags.loanFieldPrincipal, "5000")
        composeRule.traceClickText("Flexible payments")
        composeRule.traceClickTag(MmrTestTags.appBarSave)

        composeRule.traceWaitUntil("wait_until", timeoutMillis = 10_000) {
            composeRule.onAllNodesWithText(loanName).fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText(loanName).assertExists()
    }
}
