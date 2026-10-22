package com.mmr.feature.category.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.mmr.app.ui.state.MmrErrorState
import com.mmr.app.ui.state.MmrLoadingState
import com.mmr.feature.category.model.AddEditSubcategoryUiEvent
import com.mmr.feature.category.viewmodel.AddEditSubcategoryViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditSubcategoryScreen(
    onNavigateBack: () -> Unit,
    viewModel: AddEditSubcategoryViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(uiState.isSaved) {
        if (uiState.isSaved) {
            onNavigateBack()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (uiState.isEditMode) "Edit subcategory" else "New subcategory",
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    TextButton(onClick = onNavigateBack) {
                        Text("Cancel")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ),
                actions = {
                    TextButton(
                        onClick = { viewModel.onEvent(AddEditSubcategoryUiEvent.OnSaveClick) },
                        enabled = uiState.canSave && !uiState.isSaving && uiState.loadErrorMessage == null
                    ) {
                        Text(
                            text = if (uiState.isSaving) "Saving…" else "Save",
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        when {
            uiState.isLoading -> {
                MmrLoadingState(modifier = Modifier.padding(innerPadding))
            }

            uiState.loadErrorMessage != null -> {
                MmrErrorState(
                    message = uiState.loadErrorMessage!!,
                    onRetry = null,
                    modifier = Modifier.padding(innerPadding)
                )
            }

            else -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .padding(16.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    uiState.errorMessage?.let { message ->
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = message,
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                    modifier = Modifier.weight(1f)
                                )
                                TextButton(
                                    onClick = { viewModel.onEvent(AddEditSubcategoryUiEvent.OnDismissError) }
                                ) {
                                    Text("Dismiss")
                                }
                            }
                        }
                    }

                    ParentCategoryHeader(
                        parentName = uiState.parentCategoryName,
                        parentColorHex = uiState.parentColorHex,
                        subtitle = "This subcategory will be nested under the parent above."
                    )

                    OutlinedTextField(
                        value = uiState.name,
                        onValueChange = { viewModel.onEvent(AddEditSubcategoryUiEvent.OnNameChanged(it)) },
                        label = { Text("Subcategory name") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = uiState.sortOrder,
                        onValueChange = {
                            viewModel.onEvent(AddEditSubcategoryUiEvent.OnSortOrderChanged(it))
                        },
                        label = { Text("Sort order") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        supportingText = {
                            Text("Order among siblings under the same parent.")
                        }
                    )
                }
            }
        }
    }
}
