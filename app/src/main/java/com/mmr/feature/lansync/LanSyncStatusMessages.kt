package com.mmr.feature.lansync

import com.mmr.core.domain.repository.LanSyncNowResult

object LanSyncStatusMessages {
    fun formatSyncNowStatus(result: LanSyncNowResult): String {
        val changeNote = "${result.summary.totalChanges} change(s) applied."
        val warningNote =
            if (result.warnings.isNotEmpty()) {
                " Warnings: ${result.warnings.joinToString("; ")}."
            } else {
                ""
            }
        val peerLines = mutableListOf<String>()
        if (result.peerSuccesses.isNotEmpty()) {
            peerLines += "Synced with ${result.peerSuccesses.joinToString(", ")}."
        }
        if (result.peerFailures.isNotEmpty()) {
            peerLines += "Failed: ${result.peerFailures.joinToString("; ")}."
        }
        return when {
            peerLines.isEmpty() -> "Sync complete. $changeNote$warningNote"
            else -> peerLines.joinToString(" ") + " $changeNote$warningNote"
        }.trim()
    }
}
