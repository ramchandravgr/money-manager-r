package com.mmr.core.domain.repository

import com.mmr.core.backup.BackupImportMode
import com.mmr.core.backup.model.BackupRestoreSummary

interface BackupRepository {
    suspend fun getLocalDataSummary(): BackupRestoreSummary

    suspend fun exportBackupJson(): String

    suspend fun importBackupJson(
        json: String,
        mode: BackupImportMode = BackupImportMode.REPLACE,
    ): BackupRestoreSummary
}
