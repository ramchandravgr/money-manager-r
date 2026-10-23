package com.mmr.app

import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.mmr.app.testing.MmrHiltUiTest
import com.mmr.app.testing.MmrTestTags
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@HiltAndroidTest
class SettingsNavigationUiTest : MmrHiltUiTest() {
    @Test
    fun settingsNav_opensAccountsCategoriesAndBackup() {
        composeRule.onNodeWithTag(MmrTestTags.navSettings).performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag(MmrTestTags.settingsNavAccounts).performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Accounts").assertExists()

        composeRule.onNodeWithContentDescription("Back").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag(MmrTestTags.settingsNavCategories).performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Categories").assertExists()

        composeRule.onNodeWithContentDescription("Back").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag(MmrTestTags.settingsNavBackup).performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Backup & Restore").assertExists()
    }
}
