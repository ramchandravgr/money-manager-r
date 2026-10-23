package com.mmr.app

import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.mmr.app.testing.MmrHiltUiTest
import com.mmr.app.testing.MmrTestTags
import com.mmr.app.testing.MmrUiTestFixtures
import com.mmr.core.backup.BackupImportMode
import com.mmr.core.domain.repository.BackupRepository
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith
import javax.inject.Inject

@RunWith(AndroidJUnit4::class)
@HiltAndroidTest
class BackupImportFlowUiTest : MmrHiltUiTest() {
    @Inject
    lateinit var backupRepository: BackupRepository

    @Test
    fun importReplace_viaRepository_restoresTransactionOnHome() {
        val marker = "Backup restore ${System.nanoTime()}"
        runBlocking {
            MmrUiTestFixtures.seedSingleExpense(database, note = marker, amount = 77.0)
            val json = backupRepository.exportBackupJson()
            MmrUiTestFixtures.resetUiTestDatabase(database)
            backupRepository.importBackupJson(json, BackupImportMode.REPLACE)
        }

        composeRule.activityRule.scenario.recreate()
        composeRule.waitForIdle()
        composeRule.onNodeWithText(marker).assertExists()
    }

    @Test
    fun repositoryImportReplace_reflectedOnBackupScreenSummary() {
        val marker = "Backup summary ${System.nanoTime()}"
        val json =
            runBlocking {
                MmrUiTestFixtures.seedSingleExpense(database, note = marker, amount = 55.0)
                backupRepository.exportBackupJson()
            }
        runBlocking {
            MmrUiTestFixtures.resetUiTestDatabase(database)
            backupRepository.importBackupJson(json, BackupImportMode.REPLACE)
            check(backupRepository.getLocalDataSummary().transactions == 1) {
                "Expected one transaction in backup summary after import"
            }
        }

        composeRule.activityRule.scenario.recreate()
        composeRule.waitForIdle()
        composeRule.onNodeWithText(marker).assertExists()

        composeRule.onNodeWithTag(MmrTestTags.navSettings).performClick()
        composeRule.onNodeWithTag(MmrTestTags.settingsNavBackup).performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Transactions", substring = true).assertExists()
        composeRule.onNodeWithText("total records", substring = true).assertExists()
    }
}
