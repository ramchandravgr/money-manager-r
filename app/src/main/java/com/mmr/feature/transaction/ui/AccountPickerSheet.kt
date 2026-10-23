package com.mmr.feature.transaction.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mmr.feature.transaction.model.AccountPickerMode
import com.mmr.feature.transaction.model.AccountUi
import com.mmr.feature.transaction.model.LoanPickerUi

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountPickerSheet(
    title: String,
    accounts: List<AccountUi>,
    selectedAccountId: Long?,
    onAccountSelected: (Long) -> Unit,
    sheetState: SheetState,
    onDismiss: () -> Unit,
    mode: AccountPickerMode = AccountPickerMode.AccountsGrouped,
    loans: List<LoanPickerUi> = emptyList(),
    selectedLoanId: Long? = null,
    onLoanSelected: ((Long) -> Unit)? = null,
    excludedAccountId: Long? = null,
    showAllOption: Boolean = false,
    onAllSelected: (() -> Unit)? = null,
    allOptionSelected: Boolean = false,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        AccountPickerSheetContent(
            title = title,
            accounts = accounts,
            loans = loans,
            mode = mode,
            selectedAccountId = selectedAccountId,
            selectedLoanId = selectedLoanId,
            excludedAccountId = excludedAccountId,
            showAllOption = showAllOption,
            allOptionSelected = allOptionSelected,
            onAllSelected = onAllSelected,
            onAccountSelected = onAccountSelected,
            onLoanSelected = onLoanSelected,
            onDismiss = onDismiss,
        )
    }
}

@Composable
fun AccountPickerSheetContent(
    title: String,
    accounts: List<AccountUi>,
    loans: List<LoanPickerUi>,
    mode: AccountPickerMode,
    selectedAccountId: Long?,
    selectedLoanId: Long?,
    excludedAccountId: Long?,
    showAllOption: Boolean,
    allOptionSelected: Boolean,
    onAllSelected: (() -> Unit)?,
    onAccountSelected: (Long) -> Unit,
    onLoanSelected: ((Long) -> Unit)?,
    onDismiss: () -> Unit,
) {
    var query by remember { mutableStateOf("") }
    val normalizedQuery = query.trim()
    val baseSections =
        remember(accounts, excludedAccountId, mode) {
            AccountPickerGrouping.sections(
                accounts = accounts,
                excludedAccountId = excludedAccountId,
                includeLoanAccounts = false,
            )
        }
    val sections =
        remember(baseSections, normalizedQuery) {
            if (normalizedQuery.isEmpty()) {
                baseSections
            } else {
                baseSections.mapNotNull { section ->
                    val matches =
                        section.accounts.filter {
                            it.name.contains(normalizedQuery, ignoreCase = true)
                        }
                    if (matches.isEmpty()) null else section.copy(accounts = matches)
                }
            }
        }
    val filteredLoans =
        remember(loans, normalizedQuery) {
            if (normalizedQuery.isEmpty()) {
                loans
            } else {
                loans.filter { it.name.contains(normalizedQuery, ignoreCase = true) }
            }
        }

    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.92f)
                .navigationBarsPadding(),
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }

        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
            singleLine = true,
            label = { Text("Search") },
            placeholder = { Text("Account or loan name") },
        )

        Spacer(modifier = Modifier.height(8.dp))

        if (
            sections.isEmpty() &&
            (mode != AccountPickerMode.TransferTo || filteredLoans.isEmpty()) &&
            !showAllOption
        ) {
            Text(
                text =
                    if (normalizedQuery.isEmpty()) {
                        "No accounts available."
                    } else {
                        "No matches for \"$normalizedQuery\"."
                    },
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            LazyColumn(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .weight(1f),
            ) {
                if (showAllOption && onAllSelected != null) {
                    item(key = "all") {
                        AccountPickerRow(
                            title = "All accounts",
                            subtitle = null,
                            selected = allOptionSelected,
                            onClick = onAllSelected,
                        )
                    }
                }
                sections.forEach { section ->
                    item(key = "header-${section.title}") {
                        SectionHeader(section.title)
                    }
                    items(section.accounts, key = { "acc-${it.id}" }) { account ->
                        val selected =
                            account.id == selectedAccountId &&
                                (mode != AccountPickerMode.TransferTo || selectedLoanId == null)
                        AccountPickerRow(
                            title = account.name,
                            subtitle = null,
                            selected = selected,
                            onClick = { onAccountSelected(account.id) },
                        )
                    }
                }
                if (mode == AccountPickerMode.TransferTo && filteredLoans.isNotEmpty()) {
                    item(key = "header-loans") {
                        SectionHeader("Loans")
                    }
                    items(filteredLoans, key = { "loan-${it.id}" }) { loan ->
                        AccountPickerRow(
                            title = loan.name,
                            subtitle = "Loan",
                            selected = loan.id == selectedLoanId,
                            onClick = { onLoanSelected?.invoke(loan.id) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.primary,
    )
}

@Composable
private fun AccountPickerRow(
    title: String,
    subtitle: String?,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .background(
                    if (selected) {
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
                    } else {
                        Color.Transparent
                    },
                ).padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            )
            subtitle?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        if (selected) {
            Icon(
                imageVector = Icons.Filled.Check,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
            )
        }
    }
    HorizontalDivider(
        modifier = Modifier.padding(start = 16.dp),
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f),
    )
}
