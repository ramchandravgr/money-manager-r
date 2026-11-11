package com.rrparallelstudio.miapp.feature.backup

import com.rrparallelstudio.miapp.core.moneymanager.MoneyManagerMmbakLedgerImporter

internal fun moneyManagerImportStatusMessage(result: MoneyManagerMmbakLedgerImporter.Result): String =
    buildString {
        append("Money Manager import complete. ${result.added} transaction(s) added.")
        if (result.rowsInBackup == 0) {
            append(
                " No transaction rows were found in the backup " +
                    "(unsupported layout or wrong file).",
            )
        } else if (result.added == 0) {
            append(" ${result.rowsInBackup} row(s) in file; none were added.")
        }
        if (result.skippedExisting > 0) {
            append(" ${result.skippedExisting} already on this device.")
        }
        if (result.skippedUnsupported > 0) {
            append(" ${result.skippedUnsupported} unsupported row(s) skipped.")
        }
    }
