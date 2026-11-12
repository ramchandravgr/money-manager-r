package com.rrparallelstudio.miapp.feature.home.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.rrparallelstudio.miapp.R
import com.rrparallelstudio.miapp.ui.components.MmrContentDialog

@Composable
fun HomePeriodExportShareDialog(
    savedFileName: String,
    onDismiss: () -> Unit,
    onShareExcel: () -> Unit,
    onShareXml: () -> Unit,
) {
    MmrContentDialog(
        title = "Export Saved",
        message = stringResource(R.string.home_excel_saved_to_downloads, savedFileName),
        onDismiss = onDismiss,
        confirmLabel = "Share As Excel",
        onConfirm = onShareExcel,
        secondaryConfirmLabel = "Share As Xml",
        onSecondaryConfirm = onShareXml,
        dismissLabel = "Done",
    )
}
