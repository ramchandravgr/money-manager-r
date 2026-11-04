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
class RecurringSaveFlowUiTest : MmrHiltUiTest() {
    @Test
    fun saveRecurringRule_appearsOnList() {
        val ruleName = "Ui recurring ${System.nanoTime()}"

        composeRule.traceClickTag(MmrTestTags.navRecurring)
        composeRule.traceClickTag(MmrTestTags.recurringFabAdd)
        composeRule.waitForIdle()

        composeRule.traceTextInputTag(MmrTestTags.recurringFieldName, ruleName)
        composeRule.traceTextInputTag(MmrTestTags.transactionAmount, "1500")
        composeRule.traceClickText("Save rule")

        composeRule.traceWaitUntil("wait_until", timeoutMillis = 15_000) {
            composeRule.onAllNodesWithText(ruleName).fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText(ruleName).assertExists()
    }
}
