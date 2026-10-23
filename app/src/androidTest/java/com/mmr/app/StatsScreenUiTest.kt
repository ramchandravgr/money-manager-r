package com.mmr.app

import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.mmr.app.testing.MmrHiltUiTest
import com.mmr.app.testing.MmrTestTags
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@HiltAndroidTest
class StatsScreenUiTest : MmrHiltUiTest() {
    @Test
    fun statsScreen_showsIncomeAndExpenseLedgerTabs() {
        composeRule.onNodeWithTag(MmrTestTags.navStats).performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag(MmrTestTags.statsTabIncome).assertExists()
        composeRule.onNodeWithTag(MmrTestTags.statsTabExpense).assertExists()
        composeRule.onNodeWithTag(MmrTestTags.statsTabExpense).performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag(MmrTestTags.statsTabExpense).assertExists()
    }
}
