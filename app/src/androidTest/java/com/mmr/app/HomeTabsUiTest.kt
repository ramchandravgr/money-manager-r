package com.mmr.app

import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.mmr.app.testing.MmrHiltUiTest
import com.mmr.app.testing.MmrTestTags
import com.mmr.app.testing.MmrUiTestFixtures
import com.mmr.feature.home.model.HomeViewTab
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale

@RunWith(AndroidJUnit4::class)
@HiltAndroidTest
class HomeTabsUiTest : MmrHiltUiTest() {
    @Test
    fun calendarTab_showsWeekdayHeader() {
        runBlocking { MmrUiTestFixtures.seedHomeSummarySample(database) }
        composeRule.activityRule.scenario.recreate()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag(MmrTestTags.homeViewTab(HomeViewTab.CALENDAR)).performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag(MmrTestTags.homeCalendarWeekdayHeader).assertExists()
    }

    @Test
    fun monthlyTab_showsCurrentMonthRow() {
        runBlocking { MmrUiTestFixtures.seedHomeSummarySample(database) }
        composeRule.activityRule.scenario.recreate()
        composeRule.waitForIdle()

        val monthLabel = YearMonth.now().month.getDisplayName(TextStyle.SHORT, Locale.getDefault())
        composeRule.onNodeWithTag(MmrTestTags.homeViewTab(HomeViewTab.MONTHLY)).performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText(monthLabel).assertExists()
    }

    @Test
    fun totalTab_opensExcelExportDialog() {
        runBlocking { MmrUiTestFixtures.seedHomeSummarySample(database) }
        composeRule.activityRule.scenario.recreate()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag(MmrTestTags.homeViewTab(HomeViewTab.TOTAL)).performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag(MmrTestTags.homeExportExcel).performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("MoneyManagerR - Excel").assertExists()
    }
}
