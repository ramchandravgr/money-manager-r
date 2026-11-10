package com.rrparallelstudio.miapp

import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.rrparallelstudio.miapp.feature.home.model.HomeViewTab
import com.rrparallelstudio.miapp.testing.MmrHiltUiTest
import com.rrparallelstudio.miapp.testing.MmrTestTags
import com.rrparallelstudio.miapp.testing.MmrUiTestFixtures
import com.rrparallelstudio.miapp.testing.traceClickTag
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
    private fun selectHomeViewTab(tab: HomeViewTab) {
        composeRule.traceClickTag(MmrTestTags.homeViewModeTrigger)
        composeRule.waitForIdle()
        composeRule.traceClickTag(MmrTestTags.homeViewTab(tab))
    }

    @Test
    fun calendarTab_showsWeekdayHeader() {
        runBlocking { MmrUiTestFixtures.seedHomeSummarySample(database) }
        traceActivityRecreate()

        selectHomeViewTab(HomeViewTab.CALENDAR)
        composeRule.waitForIdle()
        composeRule.onNodeWithTag(MmrTestTags.homeCalendarWeekdayHeader).assertExists()
    }

    @Test
    fun monthlyTab_showsCurrentMonthRow() {
        runBlocking { MmrUiTestFixtures.seedHomeSummarySample(database) }
        traceActivityRecreate()

        val monthLabel = YearMonth.now().month.getDisplayName(TextStyle.SHORT, Locale.getDefault())
        selectHomeViewTab(HomeViewTab.MONTHLY)
        composeRule.waitForIdle()
        composeRule.onNodeWithText(monthLabel).assertExists()
    }

    @Test
    fun totalTab_opensExcelExportDialog() {
        runBlocking { MmrUiTestFixtures.seedHomeSummarySample(database) }
        traceActivityRecreate()

        selectHomeViewTab(HomeViewTab.TOTAL)
        composeRule.waitForIdle()
        composeRule.traceClickTag(MmrTestTags.homeExportExcel)
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Money Inbox - Excel").assertExists()
    }
}
