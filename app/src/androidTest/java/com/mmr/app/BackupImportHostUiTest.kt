package com.mmr.app

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.mmr.app.testing.BackupImportTestFiles
import com.mmr.app.testing.BackupScreenHostTestBridge
import com.mmr.app.testing.MmrTestTags
import com.mmr.app.testing.MmrUiTestFixtures
import com.mmr.app.testing.MmrUiTestTraceRule
import com.mmr.app.testing.traceAction
import com.mmr.app.testing.traceClickTag
import com.mmr.app.testing.traceWaitUntil
import com.mmr.core.backup.BackupImportMode
import com.mmr.core.database.MmrDatabase
import com.mmr.core.domain.repository.BackupRepository
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import javax.inject.Inject

@RunWith(AndroidJUnit4::class)
@HiltAndroidTest
class BackupImportHostUiTest {
    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeRule = createAndroidComposeRule<BackupScreenHostActivity>()

    @get:Rule(order = 2)
    val uiTraceRule = MmrUiTestTraceRule(composeRule)

    @Inject
    lateinit var database: MmrDatabase

    @Inject
    lateinit var backupRepository: BackupRepository

    @Before
    fun setUp() {
        hiltRule.inject()
        BackupScreenHostTestBridge.clear()
        runBlocking { MmrUiTestFixtures.resetUiTestDatabase(database) }
    }

    @After
    fun tearDown() {
        BackupScreenHostTestBridge.clear()
    }

    private fun launchImportFromPreparedJson(json: String) {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val uri =
            BackupImportTestFiles.writeJsonAndGetUri(
                context,
                json,
                "mmr-host-import-${System.nanoTime()}.json",
            )
        BackupScreenHostTestBridge.pendingImportUri = uri
        composeRule.traceAction("host_activity_recreate") {
            composeRule.activityRule.scenario.recreate()
        }
        composeRule.waitForIdle()
        composeRule.traceWaitUntil("wait_until", timeoutMillis = 10_000) {
            composeRule.onAllNodesWithTag(MmrTestTags.backupImportConfirm).fetchSemanticsNodes().isNotEmpty()
        }
    }

    @Test
    fun importReplace_viaHost_showsDialogAndRestoresTransaction() {
        val marker = "Backup host replace ${System.nanoTime()}"
        val json =
            runBlocking {
                MmrUiTestFixtures.seedSingleExpense(database, note = marker, amount = 66.0)
                backupRepository.exportBackupJson()
            }
        runBlocking { MmrUiTestFixtures.resetUiTestDatabase(database) }

        launchImportFromPreparedJson(json)
        composeRule.onNodeWithTag(MmrTestTags.backupImportModeReplace).assertExists()
        composeRule.traceClickTag(MmrTestTags.backupImportConfirm)

        composeRule.traceWaitUntil("wait_until", timeoutMillis = 25_000) {
            composeRule.onAllNodesWithText("Restore complete", substring = true).fetchSemanticsNodes().isNotEmpty()
        }

        runBlocking {
            val rows = database.transactionDao().getApprovedTransactionsDetailed().first()
            check(rows.any { it.transaction.note == marker }) { "Expected restored transaction $marker" }
            check(rows.single { it.transaction.note == marker }.transaction.amount == 66.0) {
                "Expected amount 66.0 after restore"
            }
        }
    }

    @Test
    fun importMerge_viaHost_reportsMergeComplete() {
        val marker = "Backup host merge ${System.nanoTime()}"
        val json =
            runBlocking {
                MmrUiTestFixtures.seedSingleExpense(database, note = marker, amount = 44.0)
                backupRepository.exportBackupJson()
            }
        runBlocking {
            MmrUiTestFixtures.resetUiTestDatabase(database)
            backupRepository.importBackupJson(json, BackupImportMode.REPLACE)
        }

        launchImportFromPreparedJson(json)
        composeRule.traceClickTag(MmrTestTags.backupImportModeMerge)
        composeRule.traceClickTag(MmrTestTags.backupImportConfirm)

        composeRule.traceWaitUntil("wait_until", timeoutMillis = 25_000) {
            composeRule.onAllNodesWithText("Merge complete", substring = true).fetchSemanticsNodes().isNotEmpty() ||
                composeRule.onAllNodesWithText("0 new record", substring = true).fetchSemanticsNodes().isNotEmpty()
        }
    }
}
