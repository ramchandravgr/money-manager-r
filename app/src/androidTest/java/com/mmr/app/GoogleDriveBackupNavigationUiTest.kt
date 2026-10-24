package com.mmr.app

import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.mmr.app.testing.MmrHiltUiTest
import com.mmr.app.testing.MmrTestTags
import com.mmr.app.testing.traceClickTag
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@HiltAndroidTest
class GoogleDriveBackupNavigationUiTest : MmrHiltUiTest() {
    @Test
    fun backupScreen_googleDriveLink_opensGoogleDriveBackupScreen() {
        composeRule.traceClickTag(MmrTestTags.navSettings)
        composeRule.traceClickTag(MmrTestTags.settingsNavBackup)
        composeRule.waitForIdle()

        composeRule.onNodeWithTag(MmrTestTags.googleDriveBackupLink).assertExists()
        composeRule.traceClickTag(MmrTestTags.googleDriveBackupLink)
        composeRule.waitForIdle()

        composeRule.onNodeWithTag(MmrTestTags.googleDriveBackupScreen).assertExists()
        composeRule.onNodeWithText("Google Drive backup").assertExists()
        composeRule.onNodeWithText("MoneyInbox/Backups", substring = true).assertExists()
    }
}
