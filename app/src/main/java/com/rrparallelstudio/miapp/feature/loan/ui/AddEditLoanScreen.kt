package com.rrparallelstudio.miapp.feature.loan.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.rrparallelstudio.miapp.feature.loan.model.AddEditLoanUiEvent
import com.rrparallelstudio.miapp.feature.loan.viewmodel.AddEditLoanViewModel
import com.rrparallelstudio.miapp.ui.components.MmrAppBarSaveIcon
import com.rrparallelstudio.miapp.ui.components.MmrImeAwareScrollColumn
import com.rrparallelstudio.miapp.ui.components.MmrNestedScaffoldInsets
import com.rrparallelstudio.miapp.ui.components.MmrPrimaryActionButton
import com.rrparallelstudio.miapp.ui.components.MmrTopAppBar
import com.rrparallelstudio.miapp.ui.components.MmrTransactionFormScreenBackground

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditLoanScreen(
    onNavigateBack: () -> Unit,
    viewModel: AddEditLoanViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val editLoanId = uiState.loanId
    val isEdit = editLoanId != null && editLoanId > 0L

    LaunchedEffect(uiState.isSaved) {
        if (uiState.isSaved) {
            onNavigateBack()
        }
    }

    Scaffold(
        contentWindowInsets = MmrNestedScaffoldInsets,
        topBar = {
            MmrTopAppBar(
                title = if (isEdit) "Edit loan" else "New loan",
                showBackNavigation = true,
                onNavigateBack = onNavigateBack,
                useBrandHeaderStyle = true,
                actions = {
                    MmrAppBarSaveIcon(
                        onClick = { viewModel.onEvent(AddEditLoanUiEvent.OnSaveClick) },
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
                LoanFormFields(
                    uiState = uiState,
                    isEdit = isEdit,
                    onEvent = viewModel::onEvent,
                    modifier = Modifier.padding(top = 4.dp),
                )
                MmrPrimaryActionButton(
                    text =
                        when {
                            uiState.isSaving -> "Saving…"
                            isEdit -> "Save changes"
                            else -> "Create loan"
                        },
                    onClick = { viewModel.onEvent(AddEditLoanUiEvent.OnSaveClick) },
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                    enabled = uiState.canSave && !uiState.isSaving,
                )
            }
        }
    }
}
