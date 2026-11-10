package com.rrparallelstudio.miapp

import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.rrparallelstudio.miapp.core.backup.BackupImportMode
import com.rrparallelstudio.miapp.core.domain.repository.BackupRepository
import com.rrparallelstudio.miapp.testing.MmrHiltUiTest
import com.rrparallelstudio.miapp.testing.MmrTestTags
import com.rrparallelstudio.miapp.testing.MmrUiTestFixtures
import com.rrparallelstudio.miapp.testing.traceClickTag
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
            val archive = backupRepository.exportBackupArchive()
            MmrUiTestFixtures.resetUiTestDatabase(database)
            backupRepository.importBackup(archive, BackupImportMode.REPLACE)
        }

        traceActivityRecreate()
        composeRule.onNodeWithText(marker).assertExists()
    }

    @Test
    fun repositoryImportReplace_reflectedOnBackupScreenSummary() {
        val marker = "Backup summary ${System.nanoTime()}"
        val archiveBytes =
            runBlocking {
                MmrUiTestFixtures.seedSingleExpense(database, note = marker, amount = 55.0)
                backupRepository.exportBackupArchive()
            }
        runBlocking {
            MmrUiTestFixtures.resetUiTestDatabase(database)
            backupRepository.importBackup(archiveBytes, BackupImportMode.REPLACE)
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
