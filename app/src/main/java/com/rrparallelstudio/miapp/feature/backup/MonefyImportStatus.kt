package com.rrparallelstudio.miapp.feature.backup

import com.rrparallelstudio.miapp.core.monefy.MonefyLedgerImporter

internal fun monefyImportStatusMessage(result: MonefyLedgerImporter.Result): String =
    buildString {
        append(
            "Monefy import complete. ${result.transactionsAdded} transaction(s) and " +
                "${result.transfersAdded} transfer(s) added.",
        )
        if (result.accountsCreated > 0) {
            append(" ${result.accountsCreated} account(s) created.")
        }
        if (result.categoriesCreated > 0) {
            val label = if (result.categoriesCreated == 1) "category" else "categories"
            append(" ${result.categoriesCreated} $label created.")
        }
        if (result.skippedExisting > 0) {
            append(" ${result.skippedExisting} already on this device.")
        }
        if (result.skippedUnsupported > 0) {
            append(" ${result.skippedUnsupported} row(s) skipped.")
        }
    }
