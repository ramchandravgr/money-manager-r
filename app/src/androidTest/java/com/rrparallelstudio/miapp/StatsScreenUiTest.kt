package com.rrparallelstudio.miapp

import androidx.compose.ui.test.onNodeWithTag
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.rrparallelstudio.miapp.testing.MmrHiltUiTest
import com.rrparallelstudio.miapp.testing.MmrTestTags
import com.rrparallelstudio.miapp.testing.traceClickTag
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@HiltAndroidTest
class StatsScreenUiTest : MmrHiltUiTest() {
    @Test
    fun statsScreen_showsIncomeAndExpenseLedgerTabs() {
        composeRule.traceClickTag(MmrTestTags.navStats)
        composeRule.waitForIdle()
        composeRule.onNodeWithTag(MmrTestTags.statsTabIncome).assertExists()
        composeRule.onNodeWithTag(MmrTestTags.statsTabExpense).assertExists()
        composeRule.traceClickTag(MmrTestTags.statsTabExpense)
        composeRule.waitForIdle()
        composeRule.onNodeWithTag(MmrTestTags.statsTabExpense).assertExists()
    }
}
