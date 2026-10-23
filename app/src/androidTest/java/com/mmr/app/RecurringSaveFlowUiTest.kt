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
class RecurringSaveFlowUiTest : MmrHiltUiTest() {
    @Test
    fun saveRecurringRule_appearsOnList() {
        val ruleName = "Ui recurring ${System.nanoTime()}"

        composeRule.onNodeWithTag(MmrTestTags.navRecurring).performClick()
        composeRule.onNodeWithTag(MmrTestTags.recurringFabAdd).performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag(MmrTestTags.recurringFieldName).performTextInput(ruleName)
        composeRule.onNodeWithTag(MmrTestTags.transactionAmount).performTextInput("1500")
        composeRule.onNodeWithText("Save rule").performClick()

        composeRule.waitUntil(timeoutMillis = 15_000) {
            composeRule.onAllNodesWithText(ruleName).fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText(ruleName).assertExists()
    }
}
