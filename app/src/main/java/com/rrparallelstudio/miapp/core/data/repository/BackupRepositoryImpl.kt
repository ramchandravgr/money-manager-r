package com.rrparallelstudio.miapp.core.data.repository

import android.content.Context
import android.util.Base64
import com.rrparallelstudio.miapp.core.attachment.AssetAttachmentStore
import com.rrparallelstudio.miapp.core.attachment.MmrAttachmentSyncId
import com.rrparallelstudio.miapp.core.attachment.TransactionAttachmentStore
import com.rrparallelstudio.miapp.core.backup.AppSettingsBackupCoordinator
import com.rrparallelstudio.miapp.core.backup.BackupImportGuard
import com.rrparallelstudio.miapp.core.backup.BackupImportMode
import com.rrparallelstudio.miapp.core.backup.BackupImportPlanner
import com.rrparallelstudio.miapp.core.backup.BackupJsonCodec
import com.rrparallelstudio.miapp.core.backup.BackupPayloadSanitizer
import com.rrparallelstudio.miapp.core.backup.BackupSignedZipCodec
import com.rrparallelstudio.miapp.core.backup.MmrBackupAttachmentBytes
import com.rrparallelstudio.miapp.core.backup.model.BackupRestoreSummary
import com.rrparallelstudio.miapp.core.backup.model.MmrBackupAssetAttachmentRecord
import com.rrparallelstudio.miapp.core.backup.model.MmrBackupAttachmentRecord
import com.rrparallelstudio.miapp.core.backup.model.MmrBackupPayload
import com.rrparallelstudio.miapp.core.common.dispatcher.IoDispatcher
import com.rrparallelstudio.miapp.core.database.MmrDatabase
import com.rrparallelstudio.miapp.core.database.dao.BackupDao
import com.rrparallelstudio.miapp.core.database.dao.LanSyncDao
import com.rrparallelstudio.miapp.core.database.dao.SyncTombstoneDao
import com.rrparallelstudio.miapp.core.database.entity.AssetAttachmentEntity
import com.rrparallelstudio.miapp.core.database.entity.TransactionAttachmentEntity
import com.rrparallelstudio.miapp.core.domain.repository.BackupRepository
import com.rrparallelstudio.miapp.core.lansync.LanSyncTombstoneMerger
import com.rrparallelstudio.miapp.core.lansync.model.LanSyncTombstoneRecord
import com.rrparallelstudio.miapp.core.moneymanager.MoneyManagerMmbakFormat
import com.rrparallelstudio.miapp.core.moneymanager.MoneyManagerMmbakLedgerImporter
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BackupRepositoryImpl
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
        private val backupDao: BackupDao,
        private val lanSyncDao: LanSyncDao,
        private val syncTombstoneDao: SyncTombstoneDao,
        private val tombstoneMerger: LanSyncTombstoneMerger,
        private val appSettingsBackupCoordinator: AppSettingsBackupCoordinator,
        private val attachmentStore: TransactionAttachmentStore,
        private val assetAttachmentStore: AssetAttachmentStore,
        private val moneyManagerMmbakLedgerImporter: MoneyManagerMmbakLedgerImporter,
        @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    ) : BackupRepository {
        override suspend fun getLocalDataSummary(): BackupRestoreSummary =
            withContext(ioDispatcher) {
                readLocalSummary()
            }

        override suspend fun exportBackupJson(): String =
            withContext(ioDispatcher) {
                BackupJsonCodec.encode(buildPayload(includeAttachmentBytesInJson = true))
            }

        override suspend fun exportBackupArchive(): ByteArray =
            withContext(ioDispatcher) {
                val payload = buildPayload(includeAttachmentBytesInJson = false)
                val payloadJson = BackupJsonCodec.encode(payload)
                val attachmentBytes = collectAllAttachmentBytesForArchive()
                BackupSignedZipCodec.encode(payloadJson, attachmentBytes)
            }

        override suspend fun importMoneyManagerMmbak(data: ByteArray): MoneyManagerMmbakLedgerImporter.Result =
            withContext(ioDispatcher) {
                moneyManagerMmbakLedgerImporter.importFromBytes(data, context.cacheDir)
            }

        override suspend fun importBackup(
            data: ByteArray,
            mode: BackupImportMode,
            displayName: String?,
        ): BackupRestoreSummary =
            withContext(ioDispatcher) {
                if (MoneyManagerMmbakFormat.isProbablyMoneyManagerBackup(data, displayName)) {
                    if (mode == BackupImportMode.REPLACE) {
                        error(
                            "Money Manager .mmbak import only merges new transactions. " +
                                "Choose merge — it will not erase data on this device.",
                        )
                    }
                    importMoneyManagerMmbak(data)
                    return@withContext readLocalSummary()
                }
                if (!BackupSignedZipCodec.isZipArchive(data)) {
                    throw IllegalArgumentException(BackupImportGuard.UNSIGNED_FILE_MESSAGE)
                }
                val archive = BackupSignedZipCodec.decode(data)
                val payload = decodePayload(archive.payloadJson)
                importPayload(
                    payload,
                    mode,
                    attachmentBytesBySyncId = archive.attachmentBytesBySyncId,
                )
            }

        private suspend fun buildPayload(includeAttachmentBytesInJson: Boolean): MmrBackupPayload =
            MmrBackupPayload(
                formatVersion = BackupJsonCodec.CURRENT_FORMAT_VERSION,
                appId = BackupJsonCodec.EXPECTED_APP_ID,
                exportedAtEpochMillis = System.currentTimeMillis(),
                databaseVersion = MmrDatabase.SCHEMA_VERSION,
                categories = backupDao.getAllCategories(),
                subcategories = backupDao.getAllSubcategories(),
                accounts = backupDao.getAllAccounts(),
                loans = backupDao.getAllLoans(),
                loanInterestRevisions = backupDao.getAllLoanInterestRevisions(),
                loanScheduleEntries = backupDao.getAllLoanScheduleEntries(),
                transactions = backupDao.getAllTransactions(),
                loanPayments = backupDao.getAllLoanPayments(),
                recurringRules = backupDao.getAllRecurringRules(),
                budgets = backupDao.getAllBudgets(),
                assets = backupDao.getAllAssets(),
                assetValuations = backupDao.getAllAssetValuations(),
                notificationRules = backupDao.getAllNotificationRules(),
                smsMessageRecords = backupDao.getSmsMessageRecordsForExport(),
                transactionApprovals = backupDao.getTransactionApprovalsForExport(),
                appSettings = appSettingsBackupCoordinator.capture(),
                tombstones = syncTombstoneDao.getAll(),
                categorizationRules = backupDao.getAllCategorizationRules(),
                trustedLanDevices = emptyList(),
                transactionAttachments =
                    if (includeAttachmentBytesInJson) {
                        buildAttachmentRecordsWithBase64()
                    } else {
                        buildAttachmentMetadataOnly()
                    },
                assetAttachments =
                    if (includeAttachmentBytesInJson) {
                        buildAssetAttachmentRecordsWithBase64()
                    } else {
                        buildAssetAttachmentMetadataOnly()
                    },
            )

        private suspend fun importPayload(
            rawPayload: MmrBackupPayload,
            mode: BackupImportMode,
            attachmentBytesBySyncId: Map<String, ByteArray>?,
        ): BackupRestoreSummary {
            val payload = BackupPayloadSanitizer.forImport(rawPayload)
            val settingsRestored = appSettingsBackupCoordinator.apply(payload.appSettings)
            if (settingsRestored) {
                clearLocalLanTrustAfterBackupImport()
            }

            when (mode) {
                BackupImportMode.REPLACE -> {
                    val planned = BackupImportPlanner.prepareReplace(payload)
                    attachmentStore.clearAllFiles()
                    assetAttachmentStore.clearAllFiles()
                    backupDao.restoreAllUserData(
                        categories = planned.categories,
                        subcategories = planned.subcategories,
                        accounts = planned.accounts,
                        loans = planned.loans,
                        loanInterestRevisions = planned.loanInterestRevisions,
                        loanScheduleEntries = planned.loanScheduleEntries,
                        transactions = planned.transactions,
                        loanPayments = planned.loanPayments,
                        recurringRules = planned.recurringRules,
                        budgets = planned.budgets,
                        assets = planned.assets,
                        assetValuations = planned.assetValuations,
                        notificationRules = planned.notificationRules,
                        smsMessageRecords = planned.smsMessageRecords,
                        transactionApprovals = planned.transactionApprovals,
                        categorizationRules = planned.categorizationRules,
                        trustedLanDevices = planned.trustedLanDevices,
                        transactionAttachments = planned.transactionAttachments.map { it.toEntity() },
                        assetAttachments = planned.assetAttachments.map { it.toAssetEntity() },
                    )
                    restoreAttachments(planned.transactionAttachments, attachmentBytesBySyncId)
                    restoreAssetAttachments(planned.assetAttachments, attachmentBytesBySyncId)
                    syncTombstoneDao.replaceAll(planned.tombstones)
                    return summaryFromPayload(planned, settingsRestored)
                }
                BackupImportMode.MERGE -> {
                    val existing = readExistingIds()
                    val planned = BackupImportPlanner.prepareMerge(payload, existing)
                    backupDao.mergeUserData(
                        categories = planned.categories,
                        subcategories = planned.subcategories,
                        accounts = planned.accounts,
                        loans = planned.loans,
                        loanInterestRevisions = planned.loanInterestRevisions,
                        loanScheduleEntries = planned.loanScheduleEntries,
                        transactions = planned.transactions,
                        loanPayments = planned.loanPayments,
                        recurringRules = planned.recurringRules,
                        budgets = planned.budgets,
                        assets = planned.assets,
                        assetValuations = planned.assetValuations,
                        notificationRules = planned.notificationRules,
                        smsMessageRecords = planned.smsMessageRecords,
                        transactionApprovals = planned.transactionApprovals,
                        categorizationRules = planned.categorizationRules,
                        trustedLanDevices = planned.trustedLanDevices,
                        transactionAttachments = planned.transactionAttachments.map { it.toEntity() },
                        assetAttachments = planned.assetAttachments.map { it.toAssetEntity() },
                    )
                    restoreAttachments(planned.transactionAttachments, attachmentBytesBySyncId)
                    restoreAssetAttachments(planned.assetAttachments, attachmentBytesBySyncId)
                    if (planned.tombstones.isNotEmpty()) {
                        tombstoneMerger.ingestRemoteTombstones(
                            planned.tombstones.map {
                                LanSyncTombstoneRecord(
                                    syncId = it.syncId,
                                    entityType = it.entityType,
                                    deletedAt = it.deletedAt,
                                    sourceDeviceId = it.sourceDeviceId,
                                )
                            },
                        )
                    }
                    return summaryFromPayload(planned, settingsRestored)
                }
            }
        }

        private suspend fun clearLocalLanTrustAfterBackupImport() {
            backupDao.deleteAllTrustedLanDevices()
            lanSyncDao.clearPendingPairRequests()
        }

        private fun decodePayload(json: String): MmrBackupPayload =
            try {
                BackupJsonCodec.decode(json)
            } catch (e: IllegalArgumentException) {
                throw e
            } catch (e: Exception) {
                throw IllegalArgumentException("Unable to restore backup: ${e.message ?: "invalid file"}", e)
            }

        private suspend fun buildAttachmentMetadataOnly(): List<MmrBackupAttachmentRecord> =
            backupDao.getAllTransactionAttachments().map { entity ->
                MmrBackupAttachmentRecord(
                    id = entity.id,
                    syncId = entity.syncId,
                    transactionId = entity.transactionId,
                    mimeType = entity.mimeType,
                    fileSizeBytes = entity.fileSizeBytes,
                    createdAt = entity.createdAt,
                    updatedAt = entity.updatedAt,
                    contentBase64 = "",
                )
            }

        private suspend fun buildAttachmentRecordsWithBase64(): List<MmrBackupAttachmentRecord> =
            backupDao.getAllTransactionAttachments().mapNotNull { entity ->
                val bytes = attachmentStore.readBytes(entity.syncId) ?: return@mapNotNull null
                MmrBackupAttachmentRecord(
                    id = entity.id,
                    syncId = entity.syncId,
                    transactionId = entity.transactionId,
                    mimeType = entity.mimeType,
                    fileSizeBytes = entity.fileSizeBytes,
                    createdAt = entity.createdAt,
                    updatedAt = entity.updatedAt,
                    contentBase64 = Base64.encodeToString(bytes, Base64.NO_WRAP),
                )
            }

        private suspend fun collectAllAttachmentBytesForArchive(): Map<String, ByteArray> {
            val txn =
                backupDao
                    .getAllTransactionAttachments()
                    .mapNotNull { entity ->
                        val bytes = attachmentStore.readBytes(entity.syncId) ?: return@mapNotNull null
                        entity.syncId to bytes
                    }
            val asset =
                backupDao
                    .getAllAssetAttachments()
                    .mapNotNull { entity ->
                        val bytes = assetAttachmentStore.readBytes(entity.syncId) ?: return@mapNotNull null
                        entity.syncId to bytes
                    }
            return (txn + asset).toMap()
        }

        private suspend fun buildAssetAttachmentMetadataOnly(): List<MmrBackupAssetAttachmentRecord> =
            backupDao.getAllAssetAttachments().map { entity ->
                MmrBackupAssetAttachmentRecord(
                    id = entity.id,
                    syncId = entity.syncId,
                    assetId = entity.assetId,
                    mimeType = entity.mimeType,
                    fileSizeBytes = entity.fileSizeBytes,
                    createdAt = entity.createdAt,
                    updatedAt = entity.updatedAt,
                    contentBase64 = "",
                )
            }

        private suspend fun buildAssetAttachmentRecordsWithBase64(): List<MmrBackupAssetAttachmentRecord> =
            backupDao.getAllAssetAttachments().mapNotNull { entity ->
                val bytes = assetAttachmentStore.readBytes(entity.syncId) ?: return@mapNotNull null
                MmrBackupAssetAttachmentRecord(
                    id = entity.id,
                    syncId = entity.syncId,
                    assetId = entity.assetId,
                    mimeType = entity.mimeType,
                    fileSizeBytes = entity.fileSizeBytes,
                    createdAt = entity.createdAt,
                    updatedAt = entity.updatedAt,
                    contentBase64 = Base64.encodeToString(bytes, Base64.NO_WRAP),
                )
            }

        private fun restoreAssetAttachments(
            records: List<MmrBackupAssetAttachmentRecord>,
            attachmentBytesBySyncId: Map<String, ByteArray>?,
        ) {
            var totalRestoredBytes = 0L
            val maxTotal = MmrBackupAttachmentBytes.MAX_TOTAL_RESTORE_BYTES
            val maxFile = MmrBackupAttachmentBytes.maxBytesForAssetAttachment()

            fun writeIfAllowed(
                syncId: String,
                bytes: ByteArray,
            ) {
                if (bytes.size > maxFile) return
                if (totalRestoredBytes + bytes.size > maxTotal) return
                if (runCatching { MmrAttachmentSyncId.requireSafeSyncId(syncId) }.isFailure) return
                totalRestoredBytes += bytes.size
                assetAttachmentStore.writeBytes(syncId, bytes)
            }

            if (attachmentBytesBySyncId != null) {
                records.forEach { record ->
                    val bytes = attachmentBytesBySyncId[record.syncId] ?: return@forEach
                    writeIfAllowed(record.syncId, bytes)
                }
            } else {
                records.forEach { record ->
                    val bytes =
                        MmrBackupAttachmentBytes.decodeBase64Content(record.contentBase64, maxFile)
                            ?: return@forEach
                    writeIfAllowed(record.syncId, bytes)
                }
            }
        }

        private fun restoreAttachments(
            records: List<MmrBackupAttachmentRecord>,
            attachmentBytesBySyncId: Map<String, ByteArray>?,
        ) {
            var totalRestoredBytes = 0L
            val maxTotal = MmrBackupAttachmentBytes.MAX_TOTAL_RESTORE_BYTES
            val maxFile = MmrBackupAttachmentBytes.maxBytesForTransactionAttachment()

            fun writeIfAllowed(
                syncId: String,
                bytes: ByteArray,
            ) {
                if (bytes.size > maxFile) return
                if (totalRestoredBytes + bytes.size > maxTotal) return
                if (runCatching { MmrAttachmentSyncId.requireSafeSyncId(syncId) }.isFailure) return
                totalRestoredBytes += bytes.size
                attachmentStore.writeBytes(syncId, bytes)
            }

            if (attachmentBytesBySyncId != null) {
                records.forEach { record ->
                    val bytes = attachmentBytesBySyncId[record.syncId] ?: return@forEach
                    writeIfAllowed(record.syncId, bytes)
                }
            } else {
                records.forEach { record ->
                    val bytes =
                        MmrBackupAttachmentBytes.decodeBase64Content(record.contentBase64, maxFile)
                            ?: return@forEach
                    writeIfAllowed(record.syncId, bytes)
                }
            }
        }

        private fun MmrBackupAttachmentRecord.toEntity() =
            TransactionAttachmentEntity(
                id = id,
                syncId = syncId,
                transactionId = transactionId,
                mimeType = mimeType,
                fileSizeBytes = fileSizeBytes,
                createdAt = createdAt,
                updatedAt = updatedAt,
            )

        private fun MmrBackupAssetAttachmentRecord.toAssetEntity() =
            AssetAttachmentEntity(
                id = id,
                syncId = syncId,
                assetId = assetId,
                mimeType = mimeType,
                fileSizeBytes = fileSizeBytes,
                createdAt = createdAt,
                updatedAt = updatedAt,
            )

        private suspend fun readExistingIds(): BackupImportPlanner.ExistingIds =
            BackupImportPlanner.ExistingIds(
                categories = backupDao.getAllCategories().map { it.id }.toSet(),
                subcategories = backupDao.getAllSubcategories().map { it.id }.toSet(),
                accounts = backupDao.getAllAccounts().map { it.id }.toSet(),
                loans = backupDao.getAllLoans().map { it.id }.toSet(),
                loanPayments = backupDao.getAllLoanPayments().map { it.id }.toSet(),
                transactions = backupDao.getAllTransactions().map { it.id }.toSet(),
                smsRecords = backupDao.getAllSmsMessageRecords().map { it.id }.toSet(),
                recurringRules = backupDao.getAllRecurringRules().map { it.id }.toSet(),
                budgets = backupDao.getAllBudgets().map { it.id }.toSet(),
                assets = backupDao.getAllAssets().map { it.id }.toSet(),
                assetValuations = backupDao.getAllAssetValuations().map { it.id }.toSet(),
                assetAttachments = backupDao.getAllAssetAttachments().map { it.id }.toSet(),
                notificationRules = backupDao.getAllNotificationRules().map { it.id }.toSet(),
                loanRevisions = backupDao.getAllLoanInterestRevisions().map { it.id }.toSet(),
                loanScheduleEntries = backupDao.getAllLoanScheduleEntries().map { it.id }.toSet(),
                transactionApprovals = backupDao.getAllTransactionApprovals().map { it.id }.toSet(),
                categorizationRules = backupDao.getAllCategorizationRules().map { it.id }.toSet(),
                trustedLanDeviceIds = backupDao.getAllTrustedLanDevices().map { it.remoteDeviceId }.toSet(),
                transactionAttachments = backupDao.getAllTransactionAttachments().map { it.id }.toSet(),
            )

        private fun summaryFromPayload(
            payload: MmrBackupPayload,
            settingsRestored: Boolean,
        ): BackupRestoreSummary =
            BackupRestoreSummary(
                categories = payload.categories.size,
                subcategories = payload.subcategories.size,
                accounts = payload.accounts.size,
                loans = payload.loans.size,
                loanInterestRevisions = payload.loanInterestRevisions.size,
                loanScheduleEntries = payload.loanScheduleEntries.size,
                transactions = payload.transactions.size,
                loanPayments = payload.loanPayments.size,
                recurringRules = payload.recurringRules.size,
                budgets = payload.budgets.size,
                assets = payload.assets.size,
                assetValuations = payload.assetValuations.size,
                assetAttachments = payload.assetAttachments.size,
                notificationRules = payload.notificationRules.size,
                smsMessageRecords = payload.smsMessageRecords.size,
                transactionApprovals = payload.transactionApprovals.size,
                appSettingsRestored = settingsRestored,
            )

        private suspend fun readLocalSummary(): BackupRestoreSummary =
            BackupRestoreSummary(
                categories = backupDao.getAllCategories().size,
                subcategories = backupDao.getAllSubcategories().size,
                accounts = backupDao.getAllAccounts().size,
                loans = backupDao.getAllLoans().size,
                loanInterestRevisions = backupDao.getAllLoanInterestRevisions().size,
                loanScheduleEntries = backupDao.getAllLoanScheduleEntries().size,
                transactions = backupDao.getAllTransactions().size,
                loanPayments = backupDao.getAllLoanPayments().size,
                recurringRules = backupDao.getAllRecurringRules().size,
                budgets = backupDao.getAllBudgets().size,
                assets = backupDao.getAllAssets().size,
                assetValuations = backupDao.getAllAssetValuations().size,
                assetAttachments = backupDao.getAllAssetAttachments().size,
                notificationRules = backupDao.getAllNotificationRules().size,
                smsMessageRecords = backupDao.getSmsMessageRecordsForExport().size,
                transactionApprovals = backupDao.getTransactionApprovalsForExport().size,
                appSettingsRestored = false,
            )
    }
