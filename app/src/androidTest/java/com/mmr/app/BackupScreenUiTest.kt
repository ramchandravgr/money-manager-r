package com.mmr.app

import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.mmr.app.testing.MmrHiltUiTest
import com.mmr.app.testing.MmrTestTags
import com.mmr.app.testing.MmrUiTestFixtures
import com.mmr.app.testing.traceClickTag
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

        composeRule.traceClickTag(MmrTestTags.navSettings)
        composeRule.traceClickTag(MmrTestTags.settingsNavBackup)
        composeRule.waitForIdle()

        composeRule.onNodeWithText("Backup & Restore").assertExists()
        composeRule.onNodeWithText("Total records", substring = true).assertExists()
        composeRule.onNodeWithText("Transactions", substring = true).assertExists()
        composeRule.onNodeWithTag(MmrTestTags.backupExport).assertExists()
        composeRule.onNodeWithTag(MmrTestTags.googleDriveBackupLink).assertExists()
        composeRule.onNodeWithTag(MmrTestTags.backupImport).assertExists()
        composeRule.traceClickTag(MmrTestTags.backupExport)
    }
}
