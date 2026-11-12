package com.rrparallelstudio.miapp.feature.search.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.rrparallelstudio.miapp.core.model.enums.AccountType
import com.rrparallelstudio.miapp.core.model.enums.CategoryType
import com.rrparallelstudio.miapp.feature.search.model.SearchAccountUi
import com.rrparallelstudio.miapp.feature.search.model.SearchCategoryPick
import com.rrparallelstudio.miapp.feature.transaction.model.AccountUi
import com.rrparallelstudio.miapp.feature.transaction.model.CategoryUi
import com.rrparallelstudio.miapp.feature.transaction.ui.AccountPickerGrouping
import com.rrparallelstudio.miapp.ui.components.MmrContentDialog
import com.rrparallelstudio.miapp.ui.components.MmrFormCompactSearchField
import com.rrparallelstudio.miapp.ui.components.MmrStatsTabRow
import com.rrparallelstudio.miapp.ui.theme.MmrBrandColors

@Composable
fun SearchCategoryFilterDialog(
    categories: List<CategoryUi>,
    picks: Set<SearchCategoryPick>,
    onToggle: (Long, Long?) -> Unit,
    onDismiss: () -> Unit,
) {
    var sideIndex by remember { mutableIntStateOf(1) }
    var query by remember { mutableStateOf("") }
    val side = if (sideIndex == 0) CategoryType.INCOME else CategoryType.EXPENSE
    val visible =
        categories
            .filter { it.type == side }
            .filter { category ->
                val q = query.trim()
                q.isEmpty() ||
                    category.name.contains(q, ignoreCase = true) ||
                    category.subcategories.any { it.name.contains(q, ignoreCase = true) }
            }
    MmrContentDialog(
        title = "Category",
        message = "Ticks stay when you slide to the other side.",
        confirmLabel = "Done",
        onConfirm = onDismiss,
        onDismiss = onDismiss,
        dismissLabel = "Cancel",
    ) {
        MmrStatsTabRow(
            incomeTabLabel = "Income",
            expenseTabLabel = "Expense",
            selectedIndex = sideIndex,
            onTabSelected = { sideIndex = it },
        )
        MmrFormCompactSearchField(
            value = query,
            onValueChange = { query = it },
            placeholder = "Search Category Or Subcategory",
            onLightFormCard = true,
        )
        visible.forEach { category ->
            SearchCheckRow(
                label = category.name,
                checked = SearchCategoryPick(category.id) in picks,
                onClick = { onToggle(category.id, null) },
            )
            category.subcategories
                .filter { sub ->
                    val q = query.trim()
                    q.isEmpty() ||
                        category.name.contains(q, ignoreCase = true) ||
                        sub.name.contains(q, ignoreCase = true)
                }.forEach { sub ->
                    SearchCheckRow(
                        label = sub.name,
                        checked = SearchCategoryPick(category.id, sub.id) in picks,
                        onClick = { onToggle(category.id, sub.id) },
                        indent = true,
                    )
                }
        }
    }
}

@Composable
fun SearchAccountFilterDialog(
    accounts: List<SearchAccountUi>,
    matchingIds: Set<Long>,
    selectedIds: Set<Long>,
    onToggle: (Long) -> Unit,
    onClear: () -> Unit,
    onDismiss: () -> Unit,
) {
    var query by remember { mutableStateOf("") }
    val visible =
        accounts
            .filter { it.id in matchingIds }
            .filter { account ->
                val q = query.trim()
                q.isEmpty() ||
                    account.name.contains(q, ignoreCase = true) ||
                    accountTypeLabel(account.type).contains(q, ignoreCase = true)
            }.map { account ->
                AccountUi(
                    id = account.id,
                    name = account.name,
                    type = account.type,
                    typeLabel = accountTypeLabel(account.type),
                    smsOriginLabel = account.smsOriginLabel,
                )
            }
    val sections = AccountPickerGrouping.sections(visible)
    MmrContentDialog(
        title = "Account",
        message = "Only accounts that have one of the ticked categories.",
        confirmLabel = "Done",
        onConfirm = onDismiss,
        onDismiss = onDismiss,
        dismissLabel = "Cancel",
    ) {
        MmrFormCompactSearchField(
            value = query,
            onValueChange = { query = it },
            placeholder = "Search Accounts",
            onLightFormCard = true,
        )
        SearchCheckRow(
            label = "All Accounts",
            checked = selectedIds.isEmpty(),
            onClick = onClear,
        )
        sections.forEach { section ->
            Text(
                text = section.title,
                modifier = Modifier.padding(top = 8.dp, bottom = 2.dp),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = MmrBrandColors.dialogMessageText(),
            )
            section.accounts.forEach { account ->
                SearchCheckRow(
                    label = account.name,
                    subtitle = account.typeLabel,
                    checked = account.id in selectedIds,
                    onClick = { onToggle(account.id) },
                )
            }
        }
    }
}

@Composable
private fun SearchCheckRow(
    label: String,
    checked: Boolean,
    onClick: () -> Unit,
    subtitle: String? = null,
    indent: Boolean = false,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(start = if (indent) 28.dp else 0.dp, top = 2.dp, bottom = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(checked = checked, onCheckedChange = { onClick() })
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge,
                color = MmrBrandColors.dialogTitleText(),
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = MmrBrandColors.dialogMessageText(),
                )
            }
        }
    }
}

private fun accountTypeLabel(type: AccountType): String =
    when (type) {
        AccountType.BANK -> "Bank"
        AccountType.CASH -> "Cash"
        AccountType.CREDIT -> "Card"
        AccountType.DEBIT -> "Debit"
        AccountType.WALLET -> "Wallet"
        AccountType.LOAN -> "Loan"
        AccountType.OTHER -> "Other"
    }
