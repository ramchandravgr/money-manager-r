package com.rrparallelstudio.miapp

import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.rrparallelstudio.miapp.feature.scheduled.model.ScheduledSection
import com.rrparallelstudio.miapp.testing.MmrHiltUiTest
import com.rrparallelstudio.miapp.testing.MmrTestTags
import com.rrparallelstudio.miapp.testing.traceClickTag
import com.rrparallelstudio.miapp.testing.traceClickText
import com.rrparallelstudio.miapp.testing.traceTextInputTag
import com.rrparallelstudio.miapp.testing.traceWaitUntil
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@HiltAndroidTest
class LoanCreateFlowUiTest : MmrHiltUiTest() {
    @Test
    fun createFlexibleLoan_appearsOnLoanList() {
        val loanName = "UiTest loan ${System.nanoTime()}"

        composeRule.traceClickTag(MmrTestTags.navScheduled)
        composeRule.traceClickTag(MmrTestTags.scheduledSectionTab(ScheduledSection.LOANS))
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
