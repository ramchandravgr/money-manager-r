package com.mmr.core.backup

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.mmr.core.attachment.TransactionAttachmentStore
import com.mmr.core.backup.AppSettingsBackupCoordinator
import com.mmr.core.data.preferences.AppDisplaySettingsStore
import com.mmr.core.data.preferences.LanSyncIdentityStore
import com.mmr.core.data.preferences.LanSyncPreferencesStore
import com.mmr.core.data.preferences.LanSyncWifiTrustStore
import com.mmr.core.data.preferences.SmsInboundSettingsStore
import com.mmr.core.data.preferences.SmsScanSettingsStore
import com.mmr.core.data.preferences.WebManagePreferencesStore
import com.mmr.core.data.repository.BackupRepositoryImpl
import com.mmr.core.database.MmrDatabase
import com.mmr.core.database.entity.AccountEntity
import com.mmr.core.database.entity.CategoryEntity
import com.mmr.core.database.entity.SmsMessageRecordEntity
import com.mmr.core.database.entity.TransactionApprovalEntity
import com.mmr.core.database.entity.TransactionEntity
import com.mmr.core.lansync.LanSyncTombstoneMerger
import com.mmr.core.model.enums.AccountType
import com.mmr.core.model.enums.CategoryType
import com.mmr.core.model.enums.RecordStatus
import com.mmr.core.model.enums.SmsApprovalState
import com.mmr.core.model.enums.TransactionType
import com.mmr.core.model.settings.MmrAppColorTheme
import com.mmr.core.model.settings.MmrStartScreenTab
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.text.Charsets

@RunWith(AndroidJUnit4::class)
class BackupRestoreInstrumentedTest {
    private lateinit var database: MmrDatabase
    private lateinit var settingsStore: SmsScanSettingsStore
    private lateinit var displaySettingsStore: AppDisplaySettingsStore
    private lateinit var repository: BackupRepositoryImpl

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database =
            Room
                .inMemoryDatabaseBuilder(context, MmrDatabase::class.java)
                .allowMainThreadQueries()
                .build()
        settingsStore = SmsScanSettingsStore(context)
        displaySettingsStore = AppDisplaySettingsStore(context)
        val appSettingsCoordinator =
            AppSettingsBackupCoordinator(
                smsScanSettingsStore = settingsStore,
                displaySettingsStore = displaySettingsStore,
                smsInboundSettingsStore = SmsInboundSettingsStore(context),
                lanSyncIdentityStore = LanSyncIdentityStore(context),
                lanSyncPreferencesStore = LanSyncPreferencesStore(context),
                lanSyncWifiTrustStore = LanSyncWifiTrustStore(context),
                webManagePreferencesStore = WebManagePreferencesStore(context),
            )
        val tombstoneMerger =
            LanSyncTombstoneMerger(
                database = database,
                tombstoneDao = database.syncTombstoneDao(),
                attachmentRepository =
                    com.mmr.core.data.repository.TransactionAttachmentRepositoryImpl(
                        context = context,
                        database = database,
                        attachmentDao = database.transactionAttachmentDao(),
                        transactionDao = database.transactionDao(),
                        store =
                            com.mmr.core.attachment
                                .TransactionAttachmentStore(context),
                        tombstoneRecorder =
                            com.mmr.core.lansync.LanSyncTombstoneRecorder(
                                database.syncTombstoneDao(),
                                com.mmr.core.data.preferences
                                    .LanSyncIdentityStore(context),
                            ),
                    ),
            )
        repository =
            BackupRepositoryImpl(
                backupDao = database.backupDao(),
                syncTombstoneDao = database.syncTombstoneDao(),
                tombstoneMerger = tombstoneMerger,
                appSettingsBackupCoordinator = appSettingsCoordinator,
                attachmentStore = TransactionAttachmentStore(context),
                ioDispatcher = Dispatchers.IO,
            )
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun exportThenRestore_replacesDatabaseWithExportedSnapshot() =
        runBlocking {
            val backupDao = database.backupDao()
            backupDao.insertCategories(
                listOf(
                    CategoryEntity(id = 1L, name = "Salary", type = CategoryType.INCOME, isDefault = true),
                ),
            )
            backupDao.insertAccounts(
                listOf(
                    AccountEntity(id = 1L, name = "Bank", type = AccountType.BANK, isDefault = true),
                ),
            )
            backupDao.insertTransactions(
                listOf(
                    TransactionEntity(
                        id = 10L,
                        dateTime = 1_700_000_000_000L,
                        amount = 1000.0,
                        type = TransactionType.INCOME,
                        categoryId = 1L,
                        accountId = 1L,
                        status = RecordStatus.APPROVED,
                    ),
                ),
            )

            val exportedArchive = repository.exportBackupArchive()
            backupDao.clearAllUserData()

            val emptySummary = repository.getLocalDataSummary()
            assertEquals(0, emptySummary.totalRecords)

            val restored = repository.importBackup(exportedArchive)
            assertEquals(1, restored.categories)
            assertEquals(1, restored.accounts)
            assertEquals(1, restored.transactions)

            val afterSummary = repository.getLocalDataSummary()
            assertEquals(3, afterSummary.totalRecords)
            assertEquals(1000.0, backupDao.getAllTransactions().first().amount, 0.001)
        }

    @Test
    fun export_omitsPendingApprovalQueue() =
        runBlocking {
            val backupDao = database.backupDao()
            backupDao.insertSmsMessageRecords(
                listOf(
                    SmsMessageRecordEntity(
                        id = 1L,
                        sender = "BANK",
                        messageBodyHash = "pending-hash",
                        messageBodySnippet = "pending",
                        receivedTime = 1_700_000_000_000L,
                        approvalState = SmsApprovalState.PENDING,
                    ),
                    SmsMessageRecordEntity(
                        id = 2L,
                        sender = "BANK",
                        messageBodyHash = "approved-hash",
                        messageBodySnippet = "approved",
                        receivedTime = 1_700_000_000_001L,
                        approvalState = SmsApprovalState.APPROVED,
                    ),
                ),
            )
            backupDao.insertTransactionApprovals(
                listOf(
                    TransactionApprovalEntity(
                        id = 1L,
                        smsMessageRecordId = 1L,
                        extractedAmount = 50.0,
                        extractedDateTime = 1_700_000_000_000L,
                        extractedSender = "BANK",
                        approvalState = SmsApprovalState.PENDING,
                    ),
                    TransactionApprovalEntity(
                        id = 2L,
                        smsMessageRecordId = 2L,
                        extractedAmount = 75.0,
                        extractedDateTime = 1_700_000_000_001L,
                        extractedSender = "BANK",
                        approvalState = SmsApprovalState.APPROVED,
                    ),
                ),
            )

            val payload = BackupJsonCodec.decode(repository.exportBackupJson())
            assertEquals(1, payload.smsMessageRecords.size)
            assertEquals(2L, payload.smsMessageRecords.single().id)
            assertEquals(1, payload.transactionApprovals.size)
            assertEquals(2L, payload.transactionApprovals.single().id)
        }

    @Test
    fun exportThenRestore_includesAppSettings() =
        runBlocking {
            settingsStore.setScanMonths(9)
            displaySettingsStore.setStartScreenTab(MmrStartScreenTab.CALENDAR)
            displaySettingsStore.setAppColorTheme(MmrAppColorTheme.SLATE)

            val exportedArchive = repository.exportBackupArchive()
            settingsStore.setScanMonths(3)
            displaySettingsStore.setStartScreenTab(MmrStartScreenTab.DAILY)

            repository.importBackup(exportedArchive)
            assertEquals(9, settingsStore.getScanMonths())
            assertEquals(MmrStartScreenTab.CALENDAR, displaySettingsStore.snapshot().startScreenTab)
            assertEquals(MmrAppColorTheme.SLATE, displaySettingsStore.snapshot().appColorTheme)
        }

    @Test
    fun importBackup_legacyJsonStillSupported() =
        runBlocking {
            val json = repository.exportBackupJson()
            database.backupDao().clearAllUserData()
            repository.importBackup(json.toByteArray(Charsets.UTF_8))
            assertEquals(0, repository.getLocalDataSummary().transactions)
        }
}
