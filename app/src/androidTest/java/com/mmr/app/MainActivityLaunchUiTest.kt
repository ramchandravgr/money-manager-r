package com.mmr.app

import androidx.compose.ui.test.assertIsSelected
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
class MainActivityLaunchUiTest : MmrHiltUiTest() {
    @Test
    fun launch_showsHomeTabSelected() {
        composeRule.onNodeWithTag(MmrTestTags.navHome).assertIsSelected()
    }

    @Test
    fun bottomNav_opensStatsTab() {
        composeRule.traceClickTag(MmrTestTags.navStats)
        composeRule.waitForIdle()
        composeRule.onNodeWithTag(MmrTestTags.navStats).assertIsSelected()
    }
}
