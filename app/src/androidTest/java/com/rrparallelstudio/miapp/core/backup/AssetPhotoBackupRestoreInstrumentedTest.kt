package com.rrparallelstudio.miapp.core.backup

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.rrparallelstudio.miapp.core.attachment.AssetAttachmentStore
import com.rrparallelstudio.miapp.core.attachment.TransactionAttachmentStore
import com.rrparallelstudio.miapp.core.data.preferences.AppDisplaySettingsStore
import com.rrparallelstudio.miapp.core.data.preferences.LanSyncIdentityStore
import com.rrparallelstudio.miapp.core.data.preferences.SmsInboundSettingsStore
import com.rrparallelstudio.miapp.core.data.preferences.SmsScanSettingsStore
import com.rrparallelstudio.miapp.core.data.preferences.WebManagePreferencesStore
import com.rrparallelstudio.miapp.core.data.repository.AssetAttachmentRepositoryImpl
import com.rrparallelstudio.miapp.core.data.repository.BackupRepositoryImpl
import com.rrparallelstudio.miapp.core.data.repository.MmrInstrumentedRepositoryGraph
import com.rrparallelstudio.miapp.core.data.repository.TransactionAttachmentRepositoryImpl
import com.rrparallelstudio.miapp.core.database.MmrDatabase
import com.rrparallelstudio.miapp.core.database.entity.AssetAttachmentEntity
import com.rrparallelstudio.miapp.core.database.entity.AssetEntity
import com.rrparallelstudio.miapp.core.lansync.LanSyncTombstoneMerger
import com.rrparallelstudio.miapp.core.lansync.LanSyncTombstoneRecorder
import com.rrparallelstudio.miapp.core.model.enums.AssetStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Simulates backup → clear app data → restore for an asset photo (reinstall path).
 */
@RunWith(AndroidJUnit4::class)
class AssetPhotoBackupRestoreInstrumentedTest {
    private lateinit var database: MmrDatabase
    private lateinit var repository: BackupRepositoryImpl
    private lateinit var assetAttachmentStore: AssetAttachmentStore

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        assetAttachmentStore = AssetAttachmentStore(context)
        runBlocking { assetAttachmentStore.clearAllFiles() }
        database =
            Room
                .inMemoryDatabaseBuilder(context, MmrDatabase::class.java)
                .allowMainThreadQueries()
                .fallbackToDestructiveMigration(dropAllTables = true)
                .build()
        val tombstoneRecorder = LanSyncTombstoneRecorder(database.syncTombstoneDao(), LanSyncIdentityStore(context))
        val transactionAttachmentRepository =
            TransactionAttachmentRepositoryImpl(
                context = context,
                database = database,
                attachmentDao = database.transactionAttachmentDao(),
                transactionDao = database.transactionDao(),
                store = TransactionAttachmentStore(context),
                tombstoneRecorder = tombstoneRecorder,
            )
        val assetAttachmentRepository =
            AssetAttachmentRepositoryImpl(
                context = context,
                database = database,
                attachmentDao = database.assetAttachmentDao(),
                store = assetAttachmentStore,
                tombstoneRecorder = tombstoneRecorder,
            )
        val tombstoneMerger =
            LanSyncTombstoneMerger(
                database = database,
                tombstoneDao = database.syncTombstoneDao(),
                attachmentRepository = transactionAttachmentRepository,
                assetAttachmentRepository = assetAttachmentRepository,
            )
        val ownerProfileCoordinator =
            com.rrparallelstudio.miapp.core.owner.MmrOwnerProfileCoordinator(
                LanSyncIdentityStore(context),
                database.ownerProfileDao(),
            )
        val appSettingsCoordinator =
            AppSettingsBackupCoordinator(
                smsScanSettingsStore = SmsScanSettingsStore(context),
                displaySettingsStore = AppDisplaySettingsStore(context),
                smsInboundSettingsStore = SmsInboundSettingsStore(context),
                webManagePreferencesStore = WebManagePreferencesStore(context),
                ownerProfileCoordinator = ownerProfileCoordinator,
            )
        val repoGraph = MmrInstrumentedRepositoryGraph(context)
        repository =
            BackupRepositoryImpl(
                context = context,
                backupDao = database.backupDao(),
                lanSyncDao = database.lanSyncDao(),
                syncTombstoneDao = database.syncTombstoneDao(),
                tombstoneMerger = tombstoneMerger,
                appSettingsBackupCoordinator = appSettingsCoordinator,
                attachmentStore = TransactionAttachmentStore(context),
                assetAttachmentStore = assetAttachmentStore,
                moneyManagerMmbakLedgerImporter = repoGraph.moneyManagerMmbakLedgerImporter,
                monefyLedgerImporter = repoGraph.monefyLedgerImporter,
                ledgerSpreadsheetImporter = repoGraph.periodLedgerSpreadsheetImporter,
                ioDispatcher = Dispatchers.IO,
            )
        repoGraph.close()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun exportThenRestore_keepsAssetPhotoBytes() =
        runBlocking {
            val syncId = "asset-photo-sync-1"
            val photoBytes = byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47, 0x01, 0x02, 0x03)
            val now = 1_700_000_000_000L
            val backupDao = database.backupDao()
            val assetId =
                database.assetDao().insert(
                    AssetEntity(
                        syncId = "asset-sync-1",
                        name = "Gold chain",
                        assetType = "Gold / Jewellery",
                        status = AssetStatus.ACTIVE,
                        purchaseDateMillis = now,
                        currentValue = 5000.0,
                        createdAt = now,
                        updatedAt = now,
                    ),
                )
            assetAttachmentStore.writeBytes(syncId, photoBytes)
            backupDao.insertAssetAttachments(
                listOf(
                    AssetAttachmentEntity(
                        syncId = syncId,
                        assetId = assetId,
                        mimeType = "image/png",
                        fileSizeBytes = photoBytes.size.toLong(),
                        createdAt = now,
                        updatedAt = now,
                    ),
                ),
            )

            val archive = repository.exportBackupArchive()

            backupDao.clearAllUserData()
            assetAttachmentStore.clearAllFiles()
            assertEquals(0, backupDao.getAllAssets().size)
            assertEquals(null, assetAttachmentStore.readBytes(syncId))

            repository.importBackup(archive)

            val restoredAsset = backupDao.getAllAssets().single()
            assertEquals("Gold chain", restoredAsset.name)
            val restoredAttachment = backupDao.getAllAssetAttachments().single()
            assertEquals(syncId, restoredAttachment.syncId)
            val restoredBytes = assetAttachmentStore.readBytes(syncId)
            assertNotNull(restoredBytes)
            assertArrayEquals(photoBytes, restoredBytes)
        }
}
