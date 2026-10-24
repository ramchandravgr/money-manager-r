package com.mmr.app

import androidx.compose.ui.test.onNodeWithTag
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.mmr.app.testing.MmrHiltUiTest
import com.mmr.app.testing.MmrTestTags
import com.mmr.app.testing.traceClickTag
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@HiltAndroidTest
class RecurringAddFlowUiTest : MmrHiltUiTest() {
    @Test
    fun recurringFab_opensAddRuleScreen() {
        composeRule.traceClickTag(MmrTestTags.navRecurring)
        composeRule.waitForIdle()
        composeRule.traceClickTag(MmrTestTags.recurringFabAdd)
        composeRule.waitForIdle()
        composeRule.onNodeWithTag(MmrTestTags.recurringFieldName).assertExists()
    }
}
