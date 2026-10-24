package com.mmr.core.data.repository

import android.util.Base64
import com.mmr.core.attachment.TransactionAttachmentStore
import com.mmr.core.backup.AppSettingsBackupCoordinator
import com.mmr.core.backup.BackupImportMode
import com.mmr.core.backup.BackupImportPlanner
import com.mmr.core.backup.BackupJsonCodec
import com.mmr.core.backup.BackupSignedZipCodec
import com.mmr.core.backup.model.BackupRestoreSummary
import com.mmr.core.backup.model.MmrBackupAttachmentRecord
import com.mmr.core.backup.model.MmrBackupPayload
import com.mmr.core.common.dispatcher.IoDispatcher
import com.mmr.core.database.MmrDatabase
import com.mmr.core.database.dao.BackupDao
import com.mmr.core.database.dao.SyncTombstoneDao
import com.mmr.core.database.entity.TransactionAttachmentEntity
import com.mmr.core.domain.repository.BackupRepository
import com.mmr.core.lansync.LanSyncTombstoneMerger
import com.mmr.core.lansync.model.LanSyncTombstoneRecord
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BackupRepositoryImpl
    @Inject
    constructor(
        private val backupDao: BackupDao,
        private val syncTombstoneDao: SyncTombstoneDao,
        private val tombstoneMerger: LanSyncTombstoneMerger,
        private val appSettingsBackupCoordinator: AppSettingsBackupCoordinator,
        private val attachmentStore: TransactionAttachmentStore,
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
                val attachmentBytes = collectAttachmentBytesForArchive()
                BackupSignedZipCodec.encode(payloadJson, attachmentBytes)
            }

        override suspend fun importBackupJson(
            json: String,
            mode: BackupImportMode,
        ): BackupRestoreSummary =
            withContext(ioDispatcher) {
                importPayload(decodePayload(json), mode, attachmentBytesBySyncId = null)
            }

        override suspend fun importBackup(
            data: ByteArray,
            mode: BackupImportMode,
        ): BackupRestoreSummary =
            withContext(ioDispatcher) {
                if (BackupSignedZipCodec.isZipArchive(data)) {
                    val archive = BackupSignedZipCodec.decode(data)
                    importPayload(
                        decodePayload(archive.payloadJson),
                        mode,
                        attachmentBytesBySyncId = archive.attachmentBytesBySyncId,
                    )
                } else {
                    val json = data.toString(Charsets.UTF_8)
                    importPayload(decodePayload(json), mode, attachmentBytesBySyncId = null)
                }
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
                notificationRules = backupDao.getAllNotificationRules(),
                smsMessageRecords = backupDao.getSmsMessageRecordsForExport(),
                transactionApprovals = backupDao.getTransactionApprovalsForExport(),
                appSettings = appSettingsBackupCoordinator.capture(),
                tombstones = syncTombstoneDao.getAll(),
                categorizationRules = backupDao.getAllCategorizationRules(),
                trustedLanDevices = backupDao.getAllTrustedLanDevices(),
                transactionAttachments =
                    if (includeAttachmentBytesInJson) {
                        buildAttachmentRecordsWithBase64()
                    } else {
                        buildAttachmentMetadataOnly()
                    },
            )

        private suspend fun importPayload(
            rawPayload: MmrBackupPayload,
            mode: BackupImportMode,
            attachmentBytesBySyncId: Map<String, ByteArray>?,
        ): BackupRestoreSummary {
            val settingsRestored = appSettingsBackupCoordinator.apply(rawPayload.appSettings)

            when (mode) {
                BackupImportMode.REPLACE -> {
                    val payload = BackupImportPlanner.prepareReplace(rawPayload)
                    attachmentStore.clearAllFiles()
                    backupDao.restoreAllUserData(
                        categories = payload.categories,
                        subcategories = payload.subcategories,
                        accounts = payload.accounts,
                        loans = payload.loans,
                        loanInterestRevisions = payload.loanInterestRevisions,
                        loanScheduleEntries = payload.loanScheduleEntries,
                        transactions = payload.transactions,
                        loanPayments = payload.loanPayments,
                        recurringRules = payload.recurringRules,
                        notificationRules = payload.notificationRules,
                        smsMessageRecords = payload.smsMessageRecords,
                        transactionApprovals = payload.transactionApprovals,
                        categorizationRules = payload.categorizationRules,
                        trustedLanDevices = payload.trustedLanDevices,
                        transactionAttachments = payload.transactionAttachments.map { it.toEntity() },
                    )
                    restoreAttachments(payload.transactionAttachments, attachmentBytesBySyncId)
                    syncTombstoneDao.replaceAll(payload.tombstones)
                    return summaryFromPayload(payload, settingsRestored)
                }
                BackupImportMode.MERGE -> {
                    val existing = readExistingIds()
                    val payload = BackupImportPlanner.prepareMerge(rawPayload, existing)
                    backupDao.mergeUserData(
                        categories = payload.categories,
                        subcategories = payload.subcategories,
                        accounts = payload.accounts,
                        loans = payload.loans,
                        loanInterestRevisions = payload.loanInterestRevisions,
                        loanScheduleEntries = payload.loanScheduleEntries,
                        transactions = payload.transactions,
                        loanPayments = payload.loanPayments,
                        recurringRules = payload.recurringRules,
                        notificationRules = payload.notificationRules,
                        smsMessageRecords = payload.smsMessageRecords,
                        transactionApprovals = payload.transactionApprovals,
                        categorizationRules = payload.categorizationRules,
                        trustedLanDevices = payload.trustedLanDevices,
                        transactionAttachments = payload.transactionAttachments.map { it.toEntity() },
                    )
                    restoreAttachments(payload.transactionAttachments, attachmentBytesBySyncId)
                    if (payload.tombstones.isNotEmpty()) {
                        tombstoneMerger.ingestRemoteTombstones(
                            payload.tombstones.map {
                                LanSyncTombstoneRecord(
                                    syncId = it.syncId,
                                    entityType = it.entityType,
                                    deletedAt = it.deletedAt,
                                    sourceDeviceId = it.sourceDeviceId,
                                )
                            },
                        )
                    }
                    return summaryFromPayload(payload, settingsRestored)
                }
            }
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

        private suspend fun collectAttachmentBytesForArchive(): Map<String, ByteArray> =
            backupDao
                .getAllTransactionAttachments()
                .mapNotNull { entity ->
                    val bytes = attachmentStore.readBytes(entity.syncId) ?: return@mapNotNull null
                    entity.syncId to bytes
                }.toMap()

        private fun restoreAttachments(
            records: List<MmrBackupAttachmentRecord>,
            attachmentBytesBySyncId: Map<String, ByteArray>?,
        ) {
            if (attachmentBytesBySyncId != null) {
                records.forEach { record ->
                    val bytes = attachmentBytesBySyncId[record.syncId] ?: return@forEach
                    attachmentStore.writeBytes(record.syncId, bytes)
                }
            } else {
                records.forEach { record ->
                    if (record.contentBase64.isBlank()) return@forEach
                    val bytes =
                        runCatching { Base64.decode(record.contentBase64, Base64.DEFAULT) }.getOrNull()
                            ?: return@forEach
                    attachmentStore.writeBytes(record.syncId, bytes)
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
                notificationRules = backupDao.getAllNotificationRules().size,
                smsMessageRecords = backupDao.getSmsMessageRecordsForExport().size,
                transactionApprovals = backupDao.getTransactionApprovalsForExport().size,
                appSettingsRestored = false,
            )
    }
