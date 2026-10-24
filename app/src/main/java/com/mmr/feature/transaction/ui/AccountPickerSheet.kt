package com.mmr.feature.transaction.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.mmr.app.ui.components.MmrDimens
import com.mmr.app.ui.components.MmrFormCompactSearchField
import com.mmr.app.ui.components.MmrLedgerPickerSheetShell
import com.mmr.app.ui.components.MmrTransactionFormFilterChip
import com.mmr.app.ui.components.rememberMmrBottomSheetNestedScrollConnection
import com.mmr.app.ui.theme.MmrBrandColors
import com.mmr.core.model.enums.AccountType
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
    modifier: Modifier = Modifier,
    subtitle: String? = null,
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
        modifier = modifier,
        containerColor = MmrBrandColors.SurfaceWhite,
        dragHandle = { BottomSheetDefaults.DragHandle() },
    ) {
        AccountPickerSheetContent(
            title = title,
            subtitle = subtitle,
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

private enum class TransferToListFilter {
    All,
    Accounts,
    Loans,
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
    subtitle: String? = null,
) {
    val maxSheetHeight = (LocalConfiguration.current.screenHeightDp * 0.85f).dp
    MmrLedgerPickerSheetShell(
        title = title,
        subtitle = subtitle,
        onDismiss = onDismiss,
        dismissLabel = "Done",
    ) {
        AccountPickerPanel(
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
            listMaxHeight = (maxSheetHeight - 160.dp).coerceAtLeast(200.dp),
        )
        Spacer(modifier = Modifier.height(MmrDimens.spaceMd))
    }
}

@Composable
fun AccountPickerPanel(
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
    modifier: Modifier = Modifier,
    listMaxHeight: Dp = 280.dp,
) {
    var query by remember { mutableStateOf("") }
    val normalizedQuery = query.trim()
    var accountTypeFilter by remember { mutableStateOf<AccountType?>(null) }
    var transferFilter by remember { mutableStateOf(TransferToListFilter.All) }

    val baseSections =
        remember(accounts, excludedAccountId, mode) {
            AccountPickerGrouping.sections(
                accounts = accounts,
                excludedAccountId = excludedAccountId,
                includeLoanAccounts = false,
            )
        }
    val availableTypes =
        remember(baseSections) {
            baseSections.mapNotNull { section -> section.accounts.firstOrNull()?.type }.distinct()
        }
    val sections =
        remember(baseSections, normalizedQuery, accountTypeFilter) {
            val byType =
                if (accountTypeFilter == null) {
                    baseSections
                } else {
                    baseSections.mapNotNull { section ->
                        val matches = section.accounts.filter { it.type == accountTypeFilter }
                        if (matches.isEmpty()) null else section.copy(accounts = matches)
                    }
                }
            if (normalizedQuery.isEmpty()) {
                byType
            } else {
                byType.mapNotNull { section ->
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
    val sheetScrollConnection = rememberMmrBottomSheetNestedScrollConnection()
    val showAccountSections =
        mode != AccountPickerMode.TransferTo ||
            transferFilter == TransferToListFilter.All ||
            transferFilter == TransferToListFilter.Accounts
    val showLoanSection =
        mode == AccountPickerMode.TransferTo &&
            filteredLoans.isNotEmpty() &&
            (transferFilter == TransferToListFilter.All || transferFilter == TransferToListFilter.Loans)

    Column(modifier = modifier.fillMaxWidth()) {
        MmrFormCompactSearchField(
            value = query,
            onValueChange = { query = it },
            placeholder =
                if (mode == AccountPickerMode.TransferTo) {
                    "Search accounts or loans…"
                } else {
                    "Search…"
                },
        )
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = MmrDimens.spaceLg, vertical = MmrDimens.spaceXs),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (mode == AccountPickerMode.TransferTo) {
                MmrTransactionFormFilterChip(
                    label = "All",
                    selected = transferFilter == TransferToListFilter.All,
                    onClick = { transferFilter = TransferToListFilter.All },
                )
                MmrTransactionFormFilterChip(
                    label = "Accounts",
                    selected = transferFilter == TransferToListFilter.Accounts,
                    onClick = { transferFilter = TransferToListFilter.Accounts },
                )
                if (loans.isNotEmpty()) {
                    MmrTransactionFormFilterChip(
                        label = "Loans",
                        selected = transferFilter == TransferToListFilter.Loans,
                        onClick = { transferFilter = TransferToListFilter.Loans },
                    )
                }
            } else {
                MmrTransactionFormFilterChip(
                    label = "All",
                    selected = accountTypeFilter == null,
                    onClick = { accountTypeFilter = null },
                )
                availableTypes.forEach { type ->
                    MmrTransactionFormFilterChip(
                        label = accountTypeLabelShort(type),
                        selected = accountTypeFilter == type,
                        onClick = { accountTypeFilter = type },
                    )
                }
            }
        }
        if (
            (!showAccountSections || sections.isEmpty()) &&
            !showLoanSection &&
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
                        .heightIn(max = listMaxHeight)
                        .nestedScroll(sheetScrollConnection),
            ) {
                if (showAllOption && onAllSelected != null && showAccountSections) {
                    item(key = "all") {
                        AccountPickerRow(
                            title = "All accounts",
                            selected = allOptionSelected,
                            onClick = onAllSelected,
                            account = null,
                            isLoan = false,
                        )
                    }
                }
                if (showAccountSections) {
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
                                selected = selected,
                                onClick = { onAccountSelected(account.id) },
                                account = account,
                                isLoan = false,
                            )
                        }
                    }
                }
                if (showLoanSection) {
                    item(key = "header-loans") {
                        SectionHeader("Loans")
                    }
                    items(filteredLoans, key = { "loan-${it.id}" }) { loan ->
                        AccountPickerRow(
                            title = loan.name,
                            selected = loan.id == selectedLoanId,
                            onClick = { onLoanSelected?.invoke(loan.id) },
                            account = null,
                            isLoan = true,
                        )
                    }
                }
            }
        }
    }
}

private fun accountTypeLabelShort(type: AccountType): String =
    when (type) {
        AccountType.BANK -> "Bank"
        AccountType.CASH -> "Cash"
        AccountType.CREDIT -> "Card"
        AccountType.DEBIT -> "Debit"
        AccountType.WALLET -> "Wallet"
        AccountType.LOAN -> "Loan"
        AccountType.OTHER -> "Other"
    }

private fun accountAvatarColor(type: AccountType): Color =
    when (type) {
        AccountType.BANK -> Color(0xFF1E3A5F)
        AccountType.CASH -> Color(0xFF64748B)
        AccountType.CREDIT -> Color(0xFF7C3AED)
        AccountType.DEBIT -> Color(0xFF0D9488)
        AccountType.WALLET -> Color(0xFFCA8A04)
        AccountType.LOAN -> Color(0xFF7C3AED)
        AccountType.OTHER -> Color(0xFF475569)
    }

private fun accountAvatarInitials(name: String): String {
    val parts = name.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }
    if (parts.isEmpty()) return "?"
    if (parts.size == 1) return parts[0].take(3).uppercase()
    return buildString {
        append(parts[0].firstOrNull()?.uppercaseChar() ?: '?')
        append(parts.getOrNull(1)?.firstOrNull()?.uppercaseChar() ?: "")
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp),
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.primary,
    )
}

@Composable
private fun AccountPickerRow(
    title: String,
    selected: Boolean,
    onClick: () -> Unit,
    account: AccountUi?,
    isLoan: Boolean,
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
                ).padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        val avatarText =
            if (account != null) {
                accountAvatarInitials(account.name)
            } else if (isLoan) {
                "L"
            } else {
                "∗"
            }
        val avatarColor =
            if (account != null) {
                accountAvatarColor(account.type)
            } else {
                Color(0xFF7C3AED)
            }
        Box(
            modifier =
                Modifier
                    .size(34.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(avatarColor),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = avatarText,
                color = Color.White,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
            )
        }
        Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            modifier = Modifier.weight(1f),
        )
        if (selected) {
            Icon(
                imageVector = Icons.Filled.Check,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
            )
        }
    }
    HorizontalDivider(
        modifier = Modifier.padding(start = 60.dp),
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f),
    )
}
