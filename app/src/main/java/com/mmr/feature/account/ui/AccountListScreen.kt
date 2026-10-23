package com.mmr.feature.account.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.hilt.navigation.compose.hiltViewModel
import com.mmr.app.ui.adaptive.MmrAdaptiveWidthBox
import com.mmr.app.ui.adaptive.mmrScreenHorizontalPadding
import com.mmr.app.ui.components.MmrDimens
import com.mmr.app.ui.components.MmrTopAppBar
import com.mmr.app.ui.state.MmrEmptyState
import com.mmr.app.ui.state.MmrErrorState
import com.mmr.app.ui.state.MmrLoadingState
import com.mmr.feature.account.model.AccountItemUi
import com.mmr.feature.account.model.AccountListUiEvent
import com.mmr.feature.account.viewmodel.AccountListViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountListScreen(
    onNavigateBack: () -> Unit,
    onNavigateToAddAccount: () -> Unit,
    onNavigateToEditAccount: (Long) -> Unit,
    viewModel: AccountListViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    val horizontalPadding = mmrScreenHorizontalPadding()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(state.statusMessage) {
        state.statusMessage?.let { snackbarHostState.showSnackbar(it) }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            MmrTopAppBar(
                title = "Accounts",
                showBackNavigation = true,
                onNavigateBack = onNavigateBack,
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onNavigateToAddAccount) {
                Text("+", style = MaterialTheme.typography.titleLarge)
            }
        },
    ) { padding ->
        when {
            state.isLoading -> MmrLoadingState(Modifier.padding(padding))
            state.errorMessage != null ->
                Box(Modifier.fillMaxSize().padding(padding)) {
                    MmrErrorState(message = state.errorMessage ?: "Error", onRetry = { })
                }
            state.accounts.isEmpty() ->
                Box(Modifier.fillMaxSize().padding(padding)) {
                    MmrEmptyState(message = "No accounts yet.\nTap + to add one.")
                }
            else -> {
                MmrAdaptiveWidthBox(Modifier.fillMaxSize().padding(padding)) {
                    LazyColumn(
                        contentPadding = PaddingValues(horizontalPadding, vertical = MmrDimens.spaceLg),
                        verticalArrangement = Arrangement.spacedBy(MmrDimens.spaceMd),
                    ) {
                        items(state.accounts, key = { it.id }) { account ->
                            AccountRowCard(
                                account = account,
                                onClick = { onNavigateToEditAccount(account.id) },
                                onSetPrimary = {
                                    viewModel.onEvent(AccountListUiEvent.OnSetPrimary(account.id))
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AccountRowCard(
    account: AccountItemUi,
    onClick: () -> Unit,
    onSetPrimary: () -> Unit,
) {
    Card(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick),
    ) {
        Row(
            Modifier.padding(MmrDimens.cardPadding),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(account.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text("${account.typeLabel} • ${account.balanceFormatted}", style = MaterialTheme.typography.bodySmall)
                account.cardLastFour?.let {
                    Text("Card • $it", style = MaterialTheme.typography.labelSmall)
                }
                if (!account.isActive) {
                    Text(
                        "Inactive",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.labelSmall,
                    )
                }
                if (account.isPrimary) {
                    Text(
                        "Primary",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
            IconButton(onClick = onSetPrimary) {
                Icon(
                    imageVector = if (account.isPrimary) Icons.Filled.Star else Icons.Outlined.StarOutline,
                    contentDescription = if (account.isPrimary) "Primary account" else "Set as primary",
                    tint =
                        if (account.isPrimary) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                )
            }
        }
    }
}
