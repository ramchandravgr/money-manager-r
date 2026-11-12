package com.rrparallelstudio.miapp.feature.home.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.rrparallelstudio.miapp.R
import com.rrparallelstudio.miapp.ui.copy.mmrStaticLabelCase

@Composable
fun HomePeriodExportShareDialog(
    savedFileName: String,
    onDismiss: () -> Unit,
    onShareExcel: () -> Unit,
    onShareXml: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Export Saved".mmrStaticLabelCase()) },
        text = {
            Column {
                Text(stringResource(R.string.home_excel_saved_to_downloads, savedFileName))
            }
        },
        confirmButton = {
            TextButton(onClick = onShareExcel) {
                Text("Share As Excel".mmrStaticLabelCase())
            }
        },
        dismissButton = {
            Column {
                TextButton(onClick = onShareXml) {
                    Text("Share As Xml".mmrStaticLabelCase())
                }
                TextButton(onClick = onDismiss) {
                    Text("Done".mmrStaticLabelCase())
                }
            }
        },
    )
}
