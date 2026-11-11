package com.rrparallelstudio.miapp.feature.account.ui

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
import com.rrparallelstudio.miapp.feature.account.viewmodel.AccountListViewModel
import com.rrparallelstudio.miapp.testing.MmrTestTags
import com.rrparallelstudio.miapp.ui.components.MmrIosSettingsListEmptySection
import com.rrparallelstudio.miapp.ui.components.MmrIosSettingsNavRow
import com.rrparallelstudio.miapp.ui.components.MmrIosSettingsSection
import com.rrparallelstudio.miapp.ui.components.MmrSettingsListAddTopBar
import com.rrparallelstudio.miapp.ui.components.MmrSettingsListScaffold
import com.rrparallelstudio.miapp.ui.state.MmrErrorState
import com.rrparallelstudio.miapp.ui.state.MmrLoadingState

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
                                    labelSubtitle = account.smsOriginLabel,
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
