package com.mmr.feature.transaction.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.mmr.feature.transaction.model.AccountUi

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountPickerSheet(
    title: String,
    accounts: List<AccountUi>,
    selectedAccountId: Long?,
    excludedAccountId: Long? = null,
    onAccountSelected: (Long) -> Unit,
    sheetState: SheetState,
    onDismiss: () -> Unit
) {
    val choices = accounts.filter { it.id != excludedAccountId }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                TextButton(onClick = onDismiss) {
                    Text("Close")
                }
            }
            if (choices.isEmpty()) {
                Text(
                    text = "No accounts available.",
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    textAlign = TextAlign.Center
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 360.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    items(choices, key = { it.id }) { account ->
                        val selected = account.id == selectedAccountId
                        Text(
                            text = account.name,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onAccountSelected(account.id) }
                                .background(
                                    if (selected) {
                                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                                    } else {
                                        Color.Transparent
                                    }
                                )
                                .padding(horizontal = 24.dp, vertical = 16.dp),
                            textAlign = TextAlign.Center,
                            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                            color = if (selected) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurface
                            }
                        )
                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
