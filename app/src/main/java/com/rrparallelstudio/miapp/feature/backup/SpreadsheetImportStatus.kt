package com.rrparallelstudio.miapp.feature.backup

import com.rrparallelstudio.miapp.core.export.PeriodLedgerSpreadsheetImporter

internal fun spreadsheetImportStatusMessage(result: PeriodLedgerSpreadsheetImporter.Result): String =
    buildString {
        append("Spreadsheet import complete. ")
        append("${result.transactionsAdded} transaction(s) added.")
        if (result.accountsCreated > 0) append(" ${result.accountsCreated} account(s) created.")
        if (result.categoriesCreated > 0) append(" ${result.categoriesCreated} category(ies) created.")
        if (result.skippedExisting > 0) append(" ${result.skippedExisting} row(s) already imported.")
        if (result.skippedUnsupported > 0) append(" ${result.skippedUnsupported} row(s) skipped.")
    }
