package com.mmr.core.data.repository

import com.mmr.core.backup.BackupJsonCodec
import com.mmr.core.backup.model.BackupRestoreSummary
import com.mmr.core.backup.model.MmrBackupPayload
import com.mmr.core.common.dispatcher.IoDispatcher
import com.mmr.core.database.dao.BackupDao
import com.mmr.core.domain.repository.BackupRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BackupRepositoryImpl @Inject constructor(
    private val backupDao: BackupDao,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : BackupRepository {

    override suspend fun getLocalDataSummary(): BackupRestoreSummary = withContext(ioDispatcher) {
        readLocalSummary()
    }

    override suspend fun exportBackupJson(): String = withContext(ioDispatcher) {
        val payload = MmrBackupPayload(
            formatVersion = BackupJsonCodec.CURRENT_FORMAT_VERSION,
            appId = BackupJsonCodec.EXPECTED_APP_ID,
            exportedAtEpochMillis = System.currentTimeMillis(),
            databaseVersion = DATABASE_VERSION,
            categories = backupDao.getAllCategories(),
            subcategories = backupDao.getAllSubcategories(),
            accounts = backupDao.getAllAccounts(),
            loans = backupDao.getAllLoans(),
            transactions = backupDao.getAllTransactions(),
            loanPayments = backupDao.getAllLoanPayments(),
            recurringRules = backupDao.getAllRecurringRules(),
            notificationRules = backupDao.getAllNotificationRules(),
            smsMessageRecords = backupDao.getAllSmsMessageRecords(),
            transactionApprovals = backupDao.getAllTransactionApprovals()
        )
        BackupJsonCodec.encode(payload)
    }

    override suspend fun importBackupJson(json: String): BackupRestoreSummary = withContext(ioDispatcher) {
        val payload = try {
            BackupJsonCodec.decode(json)
        } catch (e: IllegalArgumentException) {
            throw e
        } catch (e: Exception) {
            throw IllegalArgumentException("Unable to restore backup: ${e.message ?: "invalid file"}", e)
        }
        backupDao.restoreAllUserData(
            categories = payload.categories,
            subcategories = payload.subcategories,
            accounts = payload.accounts,
            loans = payload.loans,
            transactions = payload.transactions,
            loanPayments = payload.loanPayments,
            recurringRules = payload.recurringRules,
            notificationRules = payload.notificationRules,
            smsMessageRecords = payload.smsMessageRecords,
            transactionApprovals = payload.transactionApprovals
        )
        BackupRestoreSummary(
            categories = payload.categories.size,
            subcategories = payload.subcategories.size,
            accounts = payload.accounts.size,
            loans = payload.loans.size,
            transactions = payload.transactions.size,
            loanPayments = payload.loanPayments.size,
            recurringRules = payload.recurringRules.size,
            notificationRules = payload.notificationRules.size,
            smsMessageRecords = payload.smsMessageRecords.size,
            transactionApprovals = payload.transactionApprovals.size
        )
    }

    private suspend fun readLocalSummary(): BackupRestoreSummary {
        return BackupRestoreSummary(
            categories = backupDao.getAllCategories().size,
            subcategories = backupDao.getAllSubcategories().size,
            accounts = backupDao.getAllAccounts().size,
            loans = backupDao.getAllLoans().size,
            transactions = backupDao.getAllTransactions().size,
            loanPayments = backupDao.getAllLoanPayments().size,
            recurringRules = backupDao.getAllRecurringRules().size,
            notificationRules = backupDao.getAllNotificationRules().size,
            smsMessageRecords = backupDao.getAllSmsMessageRecords().size,
            transactionApprovals = backupDao.getAllTransactionApprovals().size
        )
    }

    private companion object {
        const val DATABASE_VERSION = 2
    }
}
