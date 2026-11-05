package com.mmr.core.domain.repository

import com.mmr.core.backup.BackupImportMode
import com.mmr.core.backup.model.BackupRestoreSummary

interface BackupRepository {
    suspend fun getLocalDataSummary(): BackupRestoreSummary

    suspend fun exportBackupJson(): String

    /** Signed ZIP archive (default export format). */
    suspend fun exportBackupArchive(): ByteArray

    /** Imports signed `.mibxbac` zip only (file picker, Drive, other untrusted sources). */
    suspend fun importBackup(
        data: ByteArray,
        mode: BackupImportMode = BackupImportMode.REPLACE,
    ): BackupRestoreSummary
}
