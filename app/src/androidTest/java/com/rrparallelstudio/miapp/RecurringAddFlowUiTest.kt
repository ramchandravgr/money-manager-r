package com.rrparallelstudio.miapp

import androidx.compose.ui.test.onNodeWithTag
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.rrparallelstudio.miapp.feature.scheduled.model.ScheduledSection
import com.rrparallelstudio.miapp.testing.MmrHiltUiTest
import com.rrparallelstudio.miapp.testing.MmrTestTags
import com.rrparallelstudio.miapp.testing.traceClickTag
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@HiltAndroidTest
class RecurringAddFlowUiTest : MmrHiltUiTest() {
    @Test
    fun recurringFab_opensAddRuleScreen() {
        composeRule.traceClickTag(MmrTestTags.navScheduled)
        composeRule.traceClickTag(MmrTestTags.scheduledSectionTab(ScheduledSection.RECURRING))
        composeRule.waitForIdle()
        composeRule.traceClickTag(MmrTestTags.recurringFabAdd)
        composeRule.waitForIdle()
        composeRule.onNodeWithTag(MmrTestTags.recurringFieldName).assertExists()
    }
}
