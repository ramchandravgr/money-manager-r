package com.rrparallelstudio.miapp.core.domain.repository

import com.rrparallelstudio.miapp.core.backup.BackupImportMode
import com.rrparallelstudio.miapp.core.backup.model.BackupRestoreSummary

interface BackupRepository {
    suspend fun getLocalDataSummary(): BackupRestoreSummary

    suspend fun exportBackupJson(): String

    /** Signed ZIP archive (default export format). */
    suspend fun exportBackupArchive(): ByteArray

    /** Imports signed `.mibxbac` zip, Money Manager `.mmbak`, or a Monefy backup (file picker). */
    suspend fun importBackup(
        data: ByteArray,
        mode: BackupImportMode = BackupImportMode.REPLACE,
        displayName: String? = null,
    ): BackupRestoreSummary

    /** Merge Money Manager export into the ledger (used when [importBackup] routes an `.mmbak` file). */
    suspend fun importMoneyManagerMmbak(
        data: ByteArray,
    ): com.rrparallelstudio.miapp.core.moneymanager.MoneyManagerMmbakLedgerImporter.Result

    /** Merge a Monefy backup into the ledger (accounts, categories, transactions, transfers). */
    suspend fun importMonefyBackup(data: ByteArray): com.rrparallelstudio.miapp.core.monefy.MonefyLedgerImporter.Result

    /** Merge transactions from a period export spreadsheet (.xlsx / .xml). */
    suspend fun importLedgerSpreadsheet(
        data: ByteArray,
        displayName: String?,
    ): com.rrparallelstudio.miapp.core.export.PeriodLedgerSpreadsheetImporter.Result
}
