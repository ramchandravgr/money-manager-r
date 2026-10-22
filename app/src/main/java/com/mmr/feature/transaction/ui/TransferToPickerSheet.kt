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
import com.mmr.feature.transaction.model.LoanPickerUi

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransferToPickerSheet(
    accounts: List<AccountUi>,
    loans: List<LoanPickerUi>,
    selectedAccountId: Long?,
    selectedLoanId: Long?,
    fromAccountId: Long?,
    onAccountSelected: (Long) -> Unit,
    onLoanSelected: (Long) -> Unit,
    sheetState: SheetState,
    onDismiss: () -> Unit
) {
    val accountChoices = accounts.filter { it.id != fromAccountId }

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
                    text = "Transfer to",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                TextButton(onClick = onDismiss) {
                    Text("Close")
                }
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 420.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (accountChoices.isNotEmpty()) {
                    item {
                        Text(
                            text = "Accounts",
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                    items(accountChoices, key = { "acc-${it.id}" }) { account ->
                        TransferToRow(
                            label = account.name,
                            selected = account.id == selectedAccountId && selectedLoanId == null,
                            onClick = { onAccountSelected(account.id) }
                        )
                    }
                }
                if (loans.isNotEmpty()) {
                    item {
                        Text(
                            text = "Loans",
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                    items(loans, key = { "loan-${it.id}" }) { loan ->
                        TransferToRow(
                            label = loan.name,
                            selected = loan.id == selectedLoanId,
                            onClick = { onLoanSelected(loan.id) }
                        )
                    }
                }
                if (accountChoices.isEmpty() && loans.isEmpty()) {
                    item {
                        Text(
                            text = "Add an account or loan to transfer to.",
                            modifier = Modifier.padding(24.dp),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun TransferToRow(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Text(
        text = label,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
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
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
}
