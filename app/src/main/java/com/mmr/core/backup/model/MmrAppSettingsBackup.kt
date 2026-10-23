package com.mmr.core.backup.model

/**
 * User preferences stored outside Room (SharedPreferences, etc.).
 */
data class MmrAppSettingsBackup(
    val smsScanMonths: Int,
)
