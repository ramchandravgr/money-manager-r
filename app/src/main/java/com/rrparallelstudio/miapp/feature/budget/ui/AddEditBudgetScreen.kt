package com.rrparallelstudio.miapp.feature.budget.ui

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.rrparallelstudio.miapp.core.model.enums.BudgetPeriodType
import com.rrparallelstudio.miapp.feature.budget.model.AddEditBudgetUiEvent
import com.rrparallelstudio.miapp.feature.budget.model.AddEditBudgetUiState
import com.rrparallelstudio.miapp.feature.budget.viewmodel.AddEditBudgetViewModel
import com.rrparallelstudio.miapp.feature.stats.ui.StatsDatePickerDialog
import com.rrparallelstudio.miapp.feature.transaction.ui.TransactionAmountField
import com.rrparallelstudio.miapp.feature.transaction.ui.TransactionFormPickerField
import com.rrparallelstudio.miapp.ui.components.MmrAppBarSaveIcon
import com.rrparallelstudio.miapp.ui.components.MmrImeAwareScrollColumn
import com.rrparallelstudio.miapp.ui.components.MmrNestedScaffoldInsets
import com.rrparallelstudio.miapp.ui.components.MmrSettingsGroup
import com.rrparallelstudio.miapp.ui.components.MmrSettingsGroupDestructiveSection
import com.rrparallelstudio.miapp.ui.components.MmrSettingsGroupSwitchRow
import com.rrparallelstudio.miapp.ui.components.MmrTopAppBar
import com.rrparallelstudio.miapp.ui.components.MmrTransactionFormIcons
import com.rrparallelstudio.miapp.ui.components.MmrTransactionFormScreenBackground
import com.rrparallelstudio.miapp.ui.components.MmrTransactionFormSectionLabel
import com.rrparallelstudio.miapp.ui.copy.mmrStaticLabelCase
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditBudgetScreen(
    onNavigateBack: () -> Unit,
    viewModel: AddEditBudgetViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    var expandedField by remember { mutableStateOf(BudgetFormExpandedField.None) }
    var showStartPicker by remember { mutableStateOf(false) }
    var showEndPicker by remember { mutableStateOf(false) }
    val isEdit = uiState.budgetId != null
    val dateFormat = remember { SimpleDateFormat("dd MMM yyyy", Locale.getDefault()) }

    LaunchedEffect(uiState.isSaved) {
        if (uiState.isSaved) {
            onNavigateBack()
        }
    }

    if (showStartPicker) {
        StatsDatePickerDialog(
            initialMillis = uiState.customStartMillis,
            onDismiss = { showStartPicker = false },
            onConfirm = {
                viewModel.onEvent(AddEditBudgetUiEvent.OnCustomStartSelected(it))
                showStartPicker = false
            },
        )
    }
    if (showEndPicker) {
        StatsDatePickerDialog(
            initialMillis = uiState.customEndMillis,
            onDismiss = { showEndPicker = false },
            onConfirm = {
                viewModel.onEvent(AddEditBudgetUiEvent.OnCustomEndSelected(it))
                showEndPicker = false
            },
        )
    }

    Scaffold(
        contentWindowInsets = MmrNestedScaffoldInsets,
        topBar = {
            MmrTopAppBar(
                title = if (isEdit) "Edit budget" else "New budget",
                showBackNavigation = true,
                onNavigateBack = onNavigateBack,
                useBrandHeaderStyle = true,
                actions = {
                    MmrAppBarSaveIcon(
                        onClick = { viewModel.onEvent(AddEditBudgetUiEvent.OnSaveClick) },
                        enabled = uiState.canSave && !uiState.isSaving,
                    )
                },
            )
        },
    ) { innerPadding ->
        MmrTransactionFormScreenBackground(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
        ) {
            MmrImeAwareScrollColumn(
                horizontalPadding = 16.dp,
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                uiState.errorMessage?.let { message ->
                    Text(text = message, color = com.rrparallelstudio.miapp.ui.theme.MmrBrandColors.AmountNegative)
                }

                MmrTransactionFormSectionLabel("Category")
                val categoryLabel = categoryDisplayLabel(uiState)
                BudgetExpandableCategoryField(
                    state = uiState,
                    displayValue = categoryLabel,
                    expanded = expandedField == BudgetFormExpandedField.Category,
                    onExpandedChange = { open ->
                        expandedField =
                            if (open) {
                                BudgetFormExpandedField.Category
                            } else {
                                BudgetFormExpandedField.None
                            }
                    },
                    onEvent = viewModel::onEvent,
                )

                MmrTransactionFormSectionLabel("Budget limit")
                TransactionAmountField(
                    amount = uiState.amount,
                    onAmountChange = { viewModel.onEvent(AddEditBudgetUiEvent.OnAmountChanged(it)) },
                )

                MmrTransactionFormSectionLabel("Period")
                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    BudgetPeriodType.entries.forEach { type ->
                        FilterChip(
                            selected = uiState.periodType == type,
                            onClick = { viewModel.onEvent(AddEditBudgetUiEvent.OnPeriodTypeSelected(type)) },
                            label = { Text(type.label.mmrStaticLabelCase()) },
                        )
                    }
                }

                if (uiState.periodType == BudgetPeriodType.CUSTOM) {
                    TransactionFormPickerField(
                        label = "Period start",
                        value = uiState.customStartMillis?.let { dateFormat.format(Date(it)) } ?: "",
                        emptyHint = "Select start date",
                        onClick = { showStartPicker = true },
                        icon = MmrTransactionFormIcons.date,
                    )
                    TransactionFormPickerField(
                        label = "Period end",
                        value = uiState.customEndMillis?.let { dateFormat.format(Date(it)) } ?: "",
                        emptyHint = "Select end date",
                        onClick = { showEndPicker = true },
                        icon = MmrTransactionFormIcons.date,
                    )
                }

                MmrSettingsGroup(
                    title = "Notifications",
                    footer = "One alert per budget period when spending crosses the limit after you add a transaction.",
                ) {
                    MmrSettingsGroupSwitchRow(
                        label = "Alert When Exceeded",
                        checked = uiState.alertOnExceeded,
                        onCheckedChange = { viewModel.onEvent(AddEditBudgetUiEvent.OnAlertToggled(it)) },
                        showDividerBelow = false,
                    )
                }

                if (isEdit) {
                    MmrSettingsGroupDestructiveSection(
                        label = "Remove Budget",
                        onClick = { viewModel.onEvent(AddEditBudgetUiEvent.OnDeleteClick) },
                    )
                }
            }
        }
    }
}

private fun categoryDisplayLabel(state: AddEditBudgetUiState): String {
    val catId = state.categoryId ?: return ""
    val cat = state.categories.firstOrNull { it.id == catId } ?: return ""
    val sub = state.subcategoryId?.let { id -> cat.subcategories.firstOrNull { it.id == id } }
    return if (sub != null) "${cat.name} · ${sub.name}" else cat.name
}
