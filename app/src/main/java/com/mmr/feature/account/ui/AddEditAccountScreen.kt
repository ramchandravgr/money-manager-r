package com.mmr.feature.account.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.mmr.app.ui.adaptive.MmrAdaptiveWidthBox
import com.mmr.app.ui.adaptive.mmrScreenHorizontalPadding
import com.mmr.app.ui.components.MmrAppBarSaveIcon
import com.mmr.app.ui.components.MmrTopAppBar
import com.mmr.core.model.enums.AccountType
import com.mmr.feature.account.model.AddEditAccountUiEvent
import com.mmr.feature.account.viewmodel.AddEditAccountViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditAccountScreen(
    onNavigateBack: () -> Unit,
    viewModel: AddEditAccountViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    val accountId = state.accountId
    val isEdit = accountId != null && accountId > 0L

    LaunchedEffect(state.isSaved) {
        if (state.isSaved) onNavigateBack()
    }

    Scaffold(
        topBar = {
            MmrTopAppBar(
                title = if (isEdit) "Edit account" else "New account",
                showBackNavigation = true,
                onNavigateBack = onNavigateBack,
                useMmHeaderStyle = true,
                actions = {
                    MmrAppBarSaveIcon(
                        onClick = { viewModel.onEvent(AddEditAccountUiEvent.OnSave) },
                        enabled = state.canSave && !state.isSaving,
                    )
                },
            )
        },
    ) { padding ->
        val horizontalPadding = mmrScreenHorizontalPadding()
        MmrAdaptiveWidthBox(Modifier.fillMaxSize().padding(padding)) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontalPadding, vertical = 16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                OutlinedTextField(
                    value = state.name,
                    onValueChange = { viewModel.onEvent(AddEditAccountUiEvent.OnNameChanged(it)) },
                    label = { Text("Account name") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )
                Text("Type", style = MaterialTheme.typography.titleSmall)
                FlowAccountTypeChips(state.type) {
                    viewModel.onEvent(AddEditAccountUiEvent.OnTypeSelected(it))
                }
                OutlinedTextField(
                    value = state.openingBalance,
                    onValueChange = { viewModel.onEvent(AddEditAccountUiEvent.OnOpeningBalanceChanged(it)) },
                    label = { Text("Opening balance (₹)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )
                OutlinedTextField(
                    value = state.cardLastFour,
                    onValueChange = { viewModel.onEvent(AddEditAccountUiEvent.OnCardLastFourChanged(it)) },
                    label = { Text("Card last 4 digits (optional)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("Primary account", style = MaterialTheme.typography.bodyLarge)
                        Text(
                            "Used for loan repayments from your main cash/bank account.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Switch(
                        checked = state.isPrimary,
                        onCheckedChange = { viewModel.onEvent(AddEditAccountUiEvent.OnPrimaryChanged(it)) },
                    )
                }
                if (isEdit) {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text("Active")
                        Switch(
                            checked = state.isActive,
                            onCheckedChange = { viewModel.onEvent(AddEditAccountUiEvent.OnActiveChanged(it)) },
                        )
                    }
                }
                state.errorMessage?.let {
                    Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FlowAccountTypeChips(
    selected: AccountType,
    onSelect: (AccountType) -> Unit,
) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        AccountType.entries.forEach { type ->
            FilterChip(
                selected = selected == type,
                onClick = { onSelect(type) },
                label = { Text(type.name) },
            )
        }
    }
}
