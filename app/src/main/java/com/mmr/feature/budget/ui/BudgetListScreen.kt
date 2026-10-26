package com.mmr.feature.budget.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.mmr.app.testing.MmrTestTags
import com.mmr.app.ui.adaptive.MmrAdaptiveWidthBox
import com.mmr.app.ui.adaptive.mmrScreenHorizontalPadding
import com.mmr.app.ui.components.MmrAddListFab
import com.mmr.app.ui.components.MmrBudgetProgressBar
import com.mmr.app.ui.components.MmrHeaderBar
import com.mmr.app.ui.components.MmrNestedScaffoldInsets
import com.mmr.app.ui.state.MmrEmptyState
import com.mmr.app.ui.state.MmrErrorState
import com.mmr.app.ui.state.MmrLoadingState
import com.mmr.app.ui.theme.MmrBrandColors
import com.mmr.feature.budget.model.BudgetListItemUi
import com.mmr.feature.budget.model.BudgetListUiEvent
import com.mmr.feature.budget.viewmodel.BudgetListViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BudgetListScreen(
    onNavigateBack: () -> Unit,
    onNavigateToAddBudget: () -> Unit,
    onNavigateToEditBudget: (Long) -> Unit,
    showBackNavigation: Boolean = false,
    viewModel: BudgetListViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val horizontalPadding = mmrScreenHorizontalPadding()

    Scaffold(
        modifier = Modifier.testTag(MmrTestTags.budgetListScreen),
        containerColor = MmrBrandColors.ListBackground,
        contentWindowInsets = MmrNestedScaffoldInsets,
        topBar = {
            MmrHeaderBar(
                title = "Budgets",
                showBack = showBackNavigation,
                onBack = if (showBackNavigation) onNavigateBack else null,
            )
        },
        floatingActionButton = {
            MmrAddListFab(
                onClick = onNavigateToAddBudget,
                contentDescription = "Add budget",
                modifier = Modifier.testTag(MmrTestTags.budgetFabAdd),
            )
        },
    ) { innerPadding ->
        when {
            uiState.isLoading ->
                MmrLoadingState(
                    Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                )

            uiState.errorMessage != null ->
                MmrErrorState(
                    message = uiState.errorMessage ?: "Failed to load budgets.",
                    onRetry = { viewModel.onEvent(BudgetListUiEvent.OnRetryClick) },
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .padding(innerPadding),
                )

            uiState.isEmpty ->
                MmrEmptyState(
                    message = "No budgets yet.\nTap + to set a limit for a category.",
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                            .padding(horizontal = horizontalPadding),
                )

            else ->
                MmrAdaptiveWidthBox(
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .padding(innerPadding),
                ) {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = horizontalPadding, vertical = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        items(uiState.budgets, key = { it.budgetId }) { item ->
                            BudgetProgressCard(
                                item = item,
                                onClick = { onNavigateToEditBudget(item.budgetId) },
                            )
                        }
                    }
                }
        }
    }
}

@Composable
private fun BudgetProgressCard(
    item: BudgetListItemUi,
    onClick: () -> Unit,
) {
    val percentLabel = "${item.percentUsed}%"
    Card(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .testTag(MmrTestTags.budgetRow(item.budgetId)),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MmrBrandColors.SurfaceWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.targetLabel,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MmrBrandColors.Reversal,
                    )
                    Text(
                        text = "${item.periodType.label} · ${item.periodLabel}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MmrBrandColors.TextMedium,
                    )
                }
                Text(
                    text = percentLabel,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color =
                        if (item.isOverBudget) {
                            MmrBrandColors.AmountNegative
                        } else {
                            MmrBrandColors.Reversal
                        },
                )
            }
            MmrBudgetProgressBar(
                progressFraction = item.progressFraction,
                modifier = Modifier.fillMaxWidth(),
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = "${item.spentFormatted} spent",
                    style = MaterialTheme.typography.bodySmall,
                    color = MmrBrandColors.TextMedium,
                )
                Text(
                    text = "Limit ${item.limitFormatted}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MmrBrandColors.TextMedium,
                )
            }
            if (item.alertOnExceeded) {
                Text(
                    text = "Alert when exceeded",
                    style = MaterialTheme.typography.labelSmall,
                    color = MmrBrandColors.Active,
                )
            }
        }
    }
}
