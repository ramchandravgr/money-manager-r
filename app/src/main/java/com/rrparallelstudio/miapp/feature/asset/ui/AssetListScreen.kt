package com.rrparallelstudio.miapp.feature.asset.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.rrparallelstudio.miapp.core.model.enums.AssetStatus
import com.rrparallelstudio.miapp.feature.asset.model.AssetListItemUi
import com.rrparallelstudio.miapp.feature.asset.model.AssetListUiEvent
import com.rrparallelstudio.miapp.feature.asset.model.AssetStatusFilter
import com.rrparallelstudio.miapp.feature.asset.viewmodel.AssetListViewModel
import com.rrparallelstudio.miapp.ui.adaptive.mmrScreenHorizontalPadding
import com.rrparallelstudio.miapp.ui.components.MmrAddListFab
import com.rrparallelstudio.miapp.ui.components.MmrHeaderBar
import com.rrparallelstudio.miapp.ui.components.MmrNestedScaffoldInsets
import com.rrparallelstudio.miapp.ui.state.MmrEmptyState
import com.rrparallelstudio.miapp.ui.state.MmrErrorState
import com.rrparallelstudio.miapp.ui.state.MmrLoadingState
import com.rrparallelstudio.miapp.ui.theme.MmrBrandColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AssetListScreen(
    onNavigateToAssetDetail: (Long) -> Unit,
    onNavigateToAddAsset: () -> Unit,
    showBackNavigation: Boolean = false,
    onNavigateBack: () -> Unit = {},
    viewModel: AssetListViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val horizontalPadding = mmrScreenHorizontalPadding()

    Scaffold(
        containerColor = MmrBrandColors.ScreenContainer,
        contentWindowInsets = MmrNestedScaffoldInsets,
        topBar = {
            MmrHeaderBar(
                title = "Assets",
                showBack = showBackNavigation,
                onBack = if (showBackNavigation) onNavigateBack else null,
            )
        },
        floatingActionButton = {
            MmrAddListFab(
                onClick = onNavigateToAddAsset,
                contentDescription = "Add asset",
            )
        },
    ) { innerPadding ->
        when {
            uiState.isLoading ->
                MmrLoadingState(Modifier.fillMaxSize().padding(innerPadding))

            uiState.errorMessage != null ->
                MmrErrorState(
                    message = uiState.errorMessage ?: "Failed to load assets.",
                    onRetry = { viewModel.onEvent(AssetListUiEvent.OnRetryClick) },
                    modifier = Modifier.fillMaxSize().padding(innerPadding),
                )

            uiState.isEmpty ->
                MmrEmptyState(
                    message = "No assets yet.\nTap + to track property, gold, vehicles, and more.",
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                            .padding(horizontal = horizontalPadding),
                )

            else ->
                Column(
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .padding(innerPadding),
                ) {
                    Card(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(horizontal = horizontalPadding, vertical = 8.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    ) {
                        Column(Modifier.padding(16.dp)) {
                            Text("Active assets total", style = MaterialTheme.typography.labelMedium)
                            Text(
                                uiState.totalActiveValueFormatted,
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }
                    Row(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState())
                                .padding(horizontal = horizontalPadding, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        FilterChip(
                            selected = uiState.selectedStatusFilter == AssetStatusFilter.ACTIVE,
                            onClick = {
                                viewModel.onEvent(AssetListUiEvent.OnStatusFilterSelected(AssetStatusFilter.ACTIVE))
                            },
                            label = { Text("Active (${uiState.activeCount})") },
                        )
                        FilterChip(
                            selected = uiState.selectedStatusFilter == AssetStatusFilter.ALL,
                            onClick = {
                                viewModel.onEvent(AssetListUiEvent.OnStatusFilterSelected(AssetStatusFilter.ALL))
                            },
                            label = { Text("All") },
                        )
                        FilterChip(
                            selected = uiState.selectedStatusFilter == AssetStatusFilter.SOLD,
                            onClick = {
                                viewModel.onEvent(AssetListUiEvent.OnStatusFilterSelected(AssetStatusFilter.SOLD))
                            },
                            label = { Text("Sold (${uiState.soldCount})") },
                        )
                    }
                    if (uiState.isFilteredEmpty) {
                        MmrEmptyState(
                            message = "No assets match this filter.",
                            modifier = Modifier.fillMaxSize().padding(horizontal = horizontalPadding),
                        )
                    } else {
                        LazyColumn(
                            contentPadding = PaddingValues(horizontal = horizontalPadding, vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            items(uiState.assets, key = { it.id }) { item ->
                                AssetListCard(item = item, onClick = { onNavigateToAssetDetail(item.id) })
                            }
                        }
                    }
                }
        }
    }
}

@Composable
private fun AssetListCard(
    item: AssetListItemUi,
    onClick: () -> Unit,
) {
    Card(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(item.name, fontWeight = FontWeight.SemiBold)
                Text(item.typeLabel, style = MaterialTheme.typography.bodySmall)
                item.ownedByName?.let {
                    Text("Owned by $it", style = MaterialTheme.typography.labelSmall)
                }
                if (item.status != AssetStatus.ACTIVE) {
                    Text(item.statusLabel, style = MaterialTheme.typography.labelSmall)
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(item.currentValueFormatted, fontWeight = FontWeight.Bold)
                item.changePercentLabel?.let {
                    Text(it, style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}
