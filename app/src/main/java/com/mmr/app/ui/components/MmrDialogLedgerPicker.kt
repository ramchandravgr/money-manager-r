package com.mmr.app.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp

data class MmrLedgerPickerRowUi(
    val transactionId: Long,
    val titleLine: String,
    val dateLabel: String,
    val detailLine: String,
    val note: String? = null,
    val rowTestTag: String? = null,
)

/** Search + scrollable ledger rows inside [MmrContentDialog] (SMS link, similar pickers). */
@Composable
fun MmrDialogLedgerPickerSection(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    searchLabel: String,
    searchTestTag: String?,
    isLoading: Boolean,
    emptyMessage: String,
    noResultsMessage: String,
    rows: List<MmrLedgerPickerRowUi>,
    onRowClick: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        MmrDialogTextField(
            value = searchQuery,
            onValueChange = onSearchQueryChange,
            label = searchLabel,
            singleLine = true,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .then(if (searchTestTag != null) Modifier.testTag(searchTestTag) else Modifier),
        )
        when {
            isLoading -> {
                Column(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .heightIn(min = 120.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    CircularProgressIndicator()
                }
            }
            rows.isEmpty() -> {
                Text(
                    text = if (searchQuery.isBlank()) emptyMessage else noResultsMessage,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            else -> {
                Text(
                    "${rows.size} transaction(s)",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                LazyColumn(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .heightIn(max = 420.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(rows, key = { it.transactionId }) { row ->
                        Card(
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .then(
                                        row.rowTestTag?.let { Modifier.testTag(it) }
                                            ?: Modifier,
                                    ).clickable { onRowClick(row.transactionId) },
                        ) {
                            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(row.titleLine, style = MaterialTheme.typography.titleSmall)
                                Text(row.dateLabel, style = MaterialTheme.typography.bodySmall)
                                Text(row.detailLine, style = MaterialTheme.typography.bodySmall)
                                row.note?.let {
                                    Text(it, style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
