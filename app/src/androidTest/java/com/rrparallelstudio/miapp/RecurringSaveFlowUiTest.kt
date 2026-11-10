package com.rrparallelstudio.miapp

import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
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
