package com.rrparallelstudio.miapp.feature.lansync

import com.rrparallelstudio.miapp.core.domain.repository.LanSyncNowResult

object LanSyncStatusMessages {
    fun formatSyncNowStatus(result: LanSyncNowResult): String {
        val lines = mutableListOf<String>()

        when (result.peerSuccesses.size) {
            0 -> Unit
            1 -> lines += "Sync with ${result.peerSuccesses.first()} finished."
            else -> lines += "Sync finished with ${result.peerSuccesses.joinToString(", ")}."
        }

        for (failure in result.peerFailures) {
            lines += failure
        }

        val changes = result.summary.totalChanges
        when {
            result.peerSuccesses.isNotEmpty() && changes > 0 ->
                lines += pluralize(changes, "item", "items") + " updated from the other phone."
            result.peerSuccesses.isNotEmpty() && changes == 0 ->
                lines += "Both phones already had the same data."
        }

        if (result.warnings.isNotEmpty()) {
            if (lines.isNotEmpty()) lines += ""
            lines += "Notes (sync still completed):"
            result.warnings.distinct().forEach { warning ->
                lines += "• $warning"
            }
        }

        if (lines.isEmpty()) {
            return "Sync finished."
        }
        return lines.joinToString("\n")
    }

    fun formatHistorySubtitle(
        success: Boolean,
        replacedCount: Int,
        warningsText: String?,
    ): String {
        if (!success) return "Did not complete"
        val base =
            when (replacedCount) {
                0 -> "No conflicting updates"
                1 -> "1 item updated from the other phone"
                else -> "$replacedCount items updated from the other phone"
            }
        val warningCount =
            warningsText
                ?.lineSequence()
                ?.map { it.trim() }
                ?.count { it.isNotEmpty() }
                ?: 0
        return when {
            warningCount == 0 -> base
            warningCount == 1 -> "$base · See sync notes"
            else -> "$base · $warningCount notes"
        }
    }

    fun formatHistoryCounts(
        replaced: Int,
        unchanged: Int,
        added: Int,
    ): String =
        buildString {
            append(pluralize(replaced, "update", "updates"))
            append(" from other phone · ")
            append(pluralize(unchanged, "unchanged item", "unchanged items"))
            append(" · ")
            append(pluralize(added, "new item", "new items"))
        }

    private fun pluralize(
        count: Int,
        singular: String,
        plural: String,
    ): String = "$count ${if (count == 1) singular else plural}"
}
