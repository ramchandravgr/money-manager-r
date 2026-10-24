package com.mmr.feature.account.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import com.mmr.app.testing.MmrTestTags
import com.mmr.app.ui.components.MmrIosSettingsListEmptySection
import com.mmr.app.ui.components.MmrIosSettingsNavRow
import com.mmr.app.ui.components.MmrIosSettingsSection
import com.mmr.app.ui.components.MmrSettingsListAddTopBar
import com.mmr.app.ui.components.MmrSettingsListScaffold
import com.mmr.app.ui.state.MmrErrorState
import com.mmr.app.ui.state.MmrLoadingState
import com.mmr.feature.account.viewmodel.AccountListViewModel

@Composable
fun AccountListScreen(
    onNavigateBack: () -> Unit,
    onNavigateToAddAccount: () -> Unit,
    onNavigateToEditAccount: (Long) -> Unit,
    viewModel: AccountListViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { androidx.compose.material3.SnackbarHostState() }

    LaunchedEffect(state.statusMessage) {
        state.statusMessage?.let { snackbarHostState.showSnackbar(it) }
    }

    MmrSettingsListScaffold(
        title = "Accounts",
        onNavigateBack = onNavigateBack,
        snackbarHostState = snackbarHostState,
        insetGrouped = true,
        topBarTrailing = {
            MmrSettingsListAddTopBar(
                onAdd = onNavigateToAddAccount,
                testTag = MmrTestTags.settingsListAddAccounts,
            )
        },
    ) { padding ->
        when {
            state.isLoading -> MmrLoadingState(Modifier.padding(padding))
            state.errorMessage != null ->
                Box(Modifier.fillMaxSize().padding(padding)) {
                    MmrErrorState(message = state.errorMessage ?: "Error", onRetry = { })
                }
            state.accounts.isEmpty() ->
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = padding,
                ) {
                    item {
                        MmrIosSettingsListEmptySection(
                            message = "No accounts yet. Tap + in the toolbar to add one.",
                        )
                    }
                }
            else ->
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = padding,
                ) {
                    item {
                        MmrIosSettingsSection(
                            title = "Your accounts",
                            isFirstInScreen = true,
                            footer = "Tap a row to edit, set primary, or remove an account.",
                        ) {
                            state.accounts.forEachIndexed { index, account ->
                                val value =
                                    buildString {
                                        if (account.isPrimary) append("Primary · ")
                                        if (!account.isActive) append("Inactive · ")
                                        append(account.balanceFormatted)
                                    }
                                MmrIosSettingsNavRow(
                                    label = account.name,
                                    value = value,
                                    onClick = { onNavigateToEditAccount(account.id) },
                                    showDividerBelow = index < state.accounts.lastIndex,
                                )
                            }
                        }
                    }
                }
        }
    }
}
