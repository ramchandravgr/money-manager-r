package com.mmr.core.data.repository

import com.mmr.core.backup.BackupImportMode
import com.mmr.core.backup.BackupImportPlanner
import com.mmr.core.backup.BackupJsonCodec
import com.mmr.core.backup.model.BackupRestoreSummary
import com.mmr.core.backup.model.MmrAppSettingsBackup
import com.mmr.core.backup.model.MmrBackupPayload
import com.mmr.core.common.dispatcher.IoDispatcher
import com.mmr.core.data.preferences.SmsScanSettingsStore
import com.mmr.core.database.MmrDatabase
import com.mmr.core.database.dao.BackupDao
import com.mmr.core.domain.repository.BackupRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BackupRepositoryImpl
    @Inject
    constructor(
        private val backupDao: BackupDao,
        private val smsScanSettingsStore: SmsScanSettingsStore,
        @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    ) : BackupRepository {
        override suspend fun getLocalDataSummary(): BackupRestoreSummary =
            withContext(ioDispatcher) {
                readLocalSummary()
            }

        override suspend fun exportBackupJson(): String =
            withContext(ioDispatcher) {
                val payload =
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
                        appSettings =
                            MmrAppSettingsBackup(
                                smsScanMonths = smsScanSettingsStore.getScanMonths(),
                            ),
                    )
                BackupJsonCodec.encode(payload)
            }

        override suspend fun importBackupJson(
            json: String,
            mode: BackupImportMode,
        ): BackupRestoreSummary =
            withContext(ioDispatcher) {
                val rawPayload = decodePayload(json)
                val settingsRestored = applySettings(rawPayload.appSettings)

                when (mode) {
                    BackupImportMode.REPLACE -> {
                        val payload = BackupImportPlanner.prepareReplace(rawPayload)
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
                        )
                        summaryFromPayload(payload, settingsRestored)
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
                        )
                        summaryFromPayload(payload, settingsRestored)
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

        private suspend fun applySettings(settings: MmrAppSettingsBackup?): Boolean =
            settings?.let {
                smsScanSettingsStore.setScanMonths(it.smsScanMonths)
                true
            } ?: false

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
