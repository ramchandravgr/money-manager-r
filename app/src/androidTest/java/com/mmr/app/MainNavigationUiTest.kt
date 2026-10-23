package com.mmr.app

import androidx.compose.ui.test.assertIsSelected
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
class MainNavigationUiTest : MmrHiltUiTest() {
    @Test
    fun bottomNav_opensLoansTab() {
        composeRule.onNodeWithTag(MmrTestTags.navLoans).performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag(MmrTestTags.navLoans).assertIsSelected()
        composeRule.onNodeWithTag(MmrTestTags.loanFabAdd).assertExists()
    }

    @Test
    fun bottomNav_opensRecurringTab() {
        composeRule.onNodeWithTag(MmrTestTags.navRecurring).performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag(MmrTestTags.navRecurring).assertIsSelected()
        composeRule.onNodeWithTag(MmrTestTags.recurringFabAdd).assertExists()
    }

    @Test
    fun bottomNav_opensSettingsTab() {
        composeRule.onNodeWithTag(MmrTestTags.navSettings).performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag(MmrTestTags.navSettings).assertIsSelected()
        composeRule.onNodeWithTag(MmrTestTags.settingsNavBackup).assertExists()
    }
}
