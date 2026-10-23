package com.mmr.feature.approval.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.mmr.app.ui.components.MmrContentDialog
import com.mmr.feature.approval.model.ApprovalCategoryOptionUi
import com.mmr.feature.approval.model.PendingSmsMergePrompt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SmsMergeDialog(
    prompt: PendingSmsMergePrompt,
    categoryOptions: List<ApprovalCategoryOptionUi>,
    onCategorySelected: (Long?) -> Unit,
    onUseLedgerCategory: () -> Unit,
    onMergeInto: (Long) -> Unit,
    onApproveAsNew: () -> Unit,
    onDismiss: () -> Unit,
) {
    var categoryMenuExpanded by remember { mutableStateOf(false) }
    val selectedCategoryId = prompt.selectedCategoryId ?: prompt.defaultCategoryId
    val selectedLabel =
        when {
            prompt.useLedgerCategory -> "Keep ledger category"
            else -> categoryOptions.find { it.id == selectedCategoryId }?.name ?: prompt.defaultCategoryName
        }

    MmrContentDialog(
        title = "Merge with existing transaction?",
        message = "Adds this SMS amount to the ledger row. Choose a category to apply on merge (optional).",
        onDismiss = onDismiss,
        confirmLabel = "Create new",
        onConfirm = onApproveAsNew,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            ExposedDropdownMenuBox(
                expanded = categoryMenuExpanded,
                onExpandedChange = { categoryMenuExpanded = it },
            ) {
                OutlinedTextField(
                    value = selectedLabel,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Category on merge") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(categoryMenuExpanded) },
                    modifier =
                        Modifier
                            .menuAnchor()
                            .fillMaxWidth(),
                )
                ExposedDropdownMenu(
                    expanded = categoryMenuExpanded,
                    onDismissRequest = { categoryMenuExpanded = false },
                ) {
                    DropdownMenuItem(
                        text = { Text("Keep ledger category") },
                        onClick = {
                            onUseLedgerCategory()
                            categoryMenuExpanded = false
                        },
                    )
                    categoryOptions.forEach { option ->
                        DropdownMenuItem(
                            text = { Text(option.name) },
                            onClick = {
                                onCategorySelected(option.id)
                                categoryMenuExpanded = false
                            },
                        )
                    }
                }
            }
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(prompt.candidates, key = { it.transactionId }) { candidate ->
                    Card(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .clickable { onMergeInto(candidate.transactionId) },
                    ) {
                        Column(Modifier.padding(12.dp)) {
                            Text(
                                "Transaction #${candidate.transactionId} • ${candidate.amountFormatted}",
                                style = MaterialTheme.typography.titleSmall,
                            )
                            Text(candidate.dateLabel, style = MaterialTheme.typography.bodySmall)
                            candidate.note?.let {
                                Text(it, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            }
        }
    }
}
