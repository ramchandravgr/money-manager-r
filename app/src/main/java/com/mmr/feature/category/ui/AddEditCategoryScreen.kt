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
import com.mmr.core.model.enums.CategoryType
import com.mmr.feature.category.model.AddEditCategoryUiEvent
import com.mmr.feature.category.model.SubcategoryItemUi
import com.mmr.feature.category.viewmodel.AddEditCategoryViewModel
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditCategoryScreen(
    onNavigateBack: () -> Unit,
    onNavigateToAddSubcategory: (Long) -> Unit,
    onNavigateToEditSubcategory: (Long) -> Unit,
    viewModel: AddEditCategoryViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var pendingDeleteSubcategory by remember { mutableStateOf<SubcategoryItemUi?>(null) }

    pendingDeleteSubcategory?.let { sub ->
        AlertDialog(
            onDismissRequest = { pendingDeleteSubcategory = null },
            title = { Text("Remove subcategory?") },
            text = {
                Text("\"${sub.name}\" under \"${uiState.name}\" will be removed from pickers.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteSubcategory(sub.id)
                        pendingDeleteSubcategory = null
                    }
                ) {
                    Text("Remove")
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDeleteSubcategory = null }) {
                    Text("Cancel")
                }
            }
        )
    }

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
                        text = if (uiState.isEditMode) "Edit category" else "New category",
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
                        onClick = { viewModel.onEvent(AddEditCategoryUiEvent.OnSaveClick) },
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
                                    onClick = { viewModel.onEvent(AddEditCategoryUiEvent.OnDismissError) }
                                ) {
                                    Text("Dismiss")
                                }
                            }
                        }
                    }

                    OutlinedTextField(
                        value = uiState.name,
                        onValueChange = { viewModel.onEvent(AddEditCategoryUiEvent.OnNameChanged(it)) },
                        label = { Text("Name") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Text("Type", style = MaterialTheme.typography.titleMedium)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = uiState.type == CategoryType.EXPENSE,
                            onClick = {
                                viewModel.onEvent(AddEditCategoryUiEvent.OnTypeSelected(CategoryType.EXPENSE))
                            },
                            label = { Text("Expense") },
                            enabled = !uiState.isDefaultCategory
                        )
                        FilterChip(
                            selected = uiState.type == CategoryType.INCOME,
                            onClick = {
                                viewModel.onEvent(AddEditCategoryUiEvent.OnTypeSelected(CategoryType.INCOME))
                            },
                            label = { Text("Income") },
                            enabled = !uiState.isDefaultCategory
                        )
                    }
                    if (uiState.isDefaultCategory) {
                        Text(
                            text = "Built-in categories keep their type.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    CategoryColorPicker(
                        selectedColorHex = uiState.colorHex,
                        onColorSelected = {
                            viewModel.onEvent(AddEditCategoryUiEvent.OnColorSelected(it))
                        }
                    )

                    OutlinedTextField(
                        value = uiState.sortOrder,
                        onValueChange = {
                            viewModel.onEvent(AddEditCategoryUiEvent.OnSortOrderChanged(it))
                        },
                        label = { Text("Sort order") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        supportingText = {
                            Text("Lower numbers appear first in lists.")
                        }
                    )

                    if (uiState.isEditMode && uiState.categoryId > 0L) {
                        SubcategoryListSection(
                            parentName = uiState.name,
                            parentColorHex = uiState.colorHex,
                            subcategories = uiState.subcategories,
                            onAddSubcategory = { onNavigateToAddSubcategory(uiState.categoryId) },
                            onEditSubcategory = onNavigateToEditSubcategory,
                            onDeleteSubcategory = { pendingDeleteSubcategory = it }
                        )
                    }
                }
            }
        }
    }
}
