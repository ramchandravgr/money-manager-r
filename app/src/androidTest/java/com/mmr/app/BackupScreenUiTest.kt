package com.mmr.app

import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.mmr.app.testing.MmrHiltUiTest
import com.mmr.app.testing.MmrTestTags
import com.mmr.app.testing.MmrUiTestFixtures
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@HiltAndroidTest
class BackupScreenUiTest : MmrHiltUiTest() {
    @Test
    fun backupScreen_showsSummaryAndExportImportActions() {
        runBlocking { MmrUiTestFixtures.seedHomeSummarySample(database) }

        composeRule.onNodeWithTag(MmrTestTags.navSettings).performClick()
        composeRule.onNodeWithTag(MmrTestTags.settingsNavBackup).performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithText("Backup & Restore").assertExists()
        composeRule.onNodeWithText("Local data ready to back up").assertExists()
        composeRule.onNodeWithText("total records", substring = true).assertExists()
        composeRule.onNodeWithTag(MmrTestTags.backupExport).assertExists()
        composeRule.onNodeWithTag(MmrTestTags.backupImport).assertExists()
        composeRule.onNodeWithTag(MmrTestTags.backupExport).performClick()
    }
}
