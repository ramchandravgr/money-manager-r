package com.mmr.app

import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.mmr.app.testing.MmrHiltUiTest
import com.mmr.app.testing.MmrTestTags
import com.mmr.app.testing.MmrUiTestFixtures
import com.mmr.feature.home.ui.formatRupeeAmount
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@HiltAndroidTest
class HomeSummaryStripUiTest : MmrHiltUiTest() {
    @Test
    fun summaryStrip_reflectsSeededMonthTotals() {
        val fixture = runBlocking { MmrUiTestFixtures.seedHomeSummarySample(database) }

        composeRule.activityRule.scenario.recreate()
        composeRule.waitForIdle()

        composeRule.waitUntil(timeoutMillis = 10_000) {
            runCatching {
                composeRule
                    .onNodeWithTag(MmrTestTags.homeSummaryExpense)
                    .assertTextEquals(formatRupeeAmount(fixture.totalExpense))
                true
            }.getOrDefault(false)
        }

        composeRule
            .onNodeWithTag(MmrTestTags.homeSummaryIncome)
            .assertTextEquals(formatRupeeAmount(fixture.totalIncome))
        composeRule
            .onNodeWithTag(MmrTestTags.homeSummaryExpense)
            .assertTextEquals(formatRupeeAmount(fixture.totalExpense))
        composeRule
            .onNodeWithTag(MmrTestTags.homeSummaryTotal)
            .assertTextEquals(formatRupeeAmount(fixture.netBalance))
    }
}
