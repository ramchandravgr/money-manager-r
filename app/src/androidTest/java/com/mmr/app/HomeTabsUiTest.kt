package com.mmr.app

import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.mmr.app.testing.MmrHiltUiTest
import com.mmr.app.testing.MmrTestTags
import com.mmr.app.testing.MmrUiTestFixtures
import com.mmr.app.testing.traceClickTag
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
        traceActivityRecreate()

        composeRule.traceClickTag(MmrTestTags.homeViewTab(HomeViewTab.CALENDAR))
        composeRule.waitForIdle()
        composeRule.onNodeWithTag(MmrTestTags.homeCalendarWeekdayHeader).assertExists()
    }

    @Test
    fun monthlyTab_showsCurrentMonthRow() {
        runBlocking { MmrUiTestFixtures.seedHomeSummarySample(database) }
        traceActivityRecreate()

        val monthLabel = YearMonth.now().month.getDisplayName(TextStyle.SHORT, Locale.getDefault())
        composeRule.traceClickTag(MmrTestTags.homeViewTab(HomeViewTab.MONTHLY))
        composeRule.waitForIdle()
        composeRule.onNodeWithText(monthLabel).assertExists()
    }

    @Test
    fun totalTab_opensExcelExportDialog() {
        runBlocking { MmrUiTestFixtures.seedHomeSummarySample(database) }
        traceActivityRecreate()

        composeRule.traceClickTag(MmrTestTags.homeViewTab(HomeViewTab.TOTAL))
        composeRule.waitForIdle()
        composeRule.traceClickTag(MmrTestTags.homeExportExcel)
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Money Inbox - Excel").assertExists()
    }
}
