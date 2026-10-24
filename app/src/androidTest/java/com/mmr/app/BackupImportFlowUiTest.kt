package com.mmr.app

import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.mmr.app.testing.MmrHiltUiTest
import com.mmr.app.testing.MmrTestTags
import com.mmr.app.testing.MmrUiTestFixtures
import com.mmr.app.testing.traceClickTag
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

        traceActivityRecreate()
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

        traceActivityRecreate()
        composeRule.onNodeWithText(marker).assertExists()

        composeRule.traceClickTag(MmrTestTags.navSettings)
        composeRule.traceClickTag(MmrTestTags.settingsNavBackup)
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Transactions", substring = true).assertExists()
        composeRule.onNodeWithText("Total records", substring = true).assertExists()
    }
}
