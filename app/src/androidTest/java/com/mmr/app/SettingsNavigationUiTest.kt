package com.mmr.app

import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.mmr.app.testing.MmrHiltUiTest
import com.mmr.app.testing.MmrTestTags
import com.mmr.app.testing.traceClickContentDescription
import com.mmr.app.testing.traceClickTag
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@HiltAndroidTest
class SettingsNavigationUiTest : MmrHiltUiTest() {
    @Test
    fun settingsNav_opensAccountsCategoriesAndBackup() {
        composeRule.traceClickTag(MmrTestTags.navSettings)
        composeRule.waitForIdle()

        composeRule.traceClickTag(MmrTestTags.settingsNavAccounts)
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Accounts").assertExists()

        composeRule.traceClickContentDescription("Back")
        composeRule.waitForIdle()
        composeRule.traceClickTag(MmrTestTags.settingsNavCategories)
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Categories").assertExists()

        composeRule.traceClickContentDescription("Back")
        composeRule.waitForIdle()
        composeRule.traceClickTag(MmrTestTags.settingsNavBackup)
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Backup & Restore").assertExists()

        composeRule.traceClickContentDescription("Back")
        composeRule.waitForIdle()
        composeRule.traceClickTag(MmrTestTags.settingsNavLanSync)
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Peer Device Sync").assertExists()
        composeRule.onNodeWithTag(MmrTestTags.lanSyncEnabledSwitch).assertExists()

        composeRule.traceClickContentDescription("Back")
        composeRule.waitForIdle()
        composeRule.traceClickTag(MmrTestTags.settingsNavWebManage)
        composeRule.waitForIdle()
        composeRule.onNodeWithTag(MmrTestTags.webManagePasswordField).assertExists()
        composeRule.onNodeWithTag(MmrTestTags.webManageEnabledSwitch).assertExists()
    }
}
