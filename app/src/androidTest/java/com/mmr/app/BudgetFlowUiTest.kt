package com.mmr.app

import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.mmr.app.testing.MmrHiltUiTest
import com.mmr.app.testing.MmrTestTags
import com.mmr.app.testing.MmrUiTestFixtures
import com.mmr.app.testing.traceClickTag
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@HiltAndroidTest
class BudgetFlowUiTest : MmrHiltUiTest() {
    @Test
    fun budgetList_showsOverBudgetPercent() {
        runBlocking {
            val fixture = MmrUiTestFixtures.seedOverBudgetMonthly(database, limit = 100.0, spent = 150.0)
            composeRule.waitForIdle()
            waitForMainNavReady()
            composeRule.traceClickTag(MmrTestTags.navBudgets)
            composeRule.waitForIdle()
            composeRule.onNodeWithTag(MmrTestTags.navBudgets).assertIsSelected()
            composeRule.onNodeWithTag(MmrTestTags.budgetRow(fixture.budgetId)).assertExists()
            composeRule.onNodeWithText("${fixture.percentUsed}%").assertExists()
            composeRule.onNodeWithText(fixture.categoryLabel, substring = true).assertExists()
        }
    }
}
