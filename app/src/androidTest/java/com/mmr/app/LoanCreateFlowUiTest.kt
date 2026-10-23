package com.mmr.app

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
class LoanCreateFlowUiTest : MmrHiltUiTest() {
    @Test
    fun createFlexibleLoan_appearsOnLoanList() {
        val loanName = "UiTest loan ${System.nanoTime()}"

        composeRule.onNodeWithTag(MmrTestTags.navLoans).performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag(MmrTestTags.loanFabAdd).performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("New Loan Entry").assertExists()

        composeRule.onNodeWithTag(MmrTestTags.loanFieldName).performTextInput(loanName)
        composeRule.onNodeWithTag(MmrTestTags.loanFieldLender).performTextInput("Test Bank")
        composeRule.onNodeWithTag(MmrTestTags.loanFieldPrincipal).performTextInput("5000")
        composeRule.onNodeWithText("Flexible payments").performClick()
        composeRule.onNodeWithTag(MmrTestTags.appBarSave).performClick()

        composeRule.waitUntil(timeoutMillis = 10_000) {
            composeRule.onAllNodesWithText(loanName).fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText(loanName).assertExists()
    }
}
