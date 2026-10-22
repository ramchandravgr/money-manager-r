package com.mmr.feature.category.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.mmr.app.ui.state.MmrEmptyState
import com.mmr.app.ui.state.MmrErrorState
import com.mmr.app.ui.state.MmrLoadingState
import com.mmr.app.ui.state.MmrStatusBanner
import com.mmr.core.model.enums.CategoryType
import com.mmr.feature.category.model.CategoryTreeItemUi
import com.mmr.feature.category.model.CategoryUiEvent
import com.mmr.feature.category.model.SubcategoryItemUi
import com.mmr.feature.category.viewmodel.CategoryViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryListScreen(
    onNavigateBack: () -> Unit,
    onNavigateToAddCategory: () -> Unit,
    onNavigateToEditCategory: (Long) -> Unit,
    onNavigateToAddSubcategory: (Long) -> Unit,
    onNavigateToEditSubcategory: (Long) -> Unit,
    viewModel: CategoryViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var pendingDeleteCategory by remember { mutableStateOf<CategoryTreeItemUi?>(null) }
    var pendingDeleteSubcategory by remember { mutableStateOf<Pair<CategoryTreeItemUi, SubcategoryItemUi>?>(null) }

    pendingDeleteCategory?.let { item ->
        AlertDialog(
            onDismissRequest = { pendingDeleteCategory = null },
            title = { Text("Remove category?") },
            text = {
                Text(
                    "\"${item.name}\" and its subcategories will be hidden from pickers. " +
                        "Existing transactions keep their references."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.onEvent(CategoryUiEvent.OnDeleteCategory(item.id))
                        pendingDeleteCategory = null
                    },
                    enabled = !uiState.isSaving
                ) {
                    Text("Remove")
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDeleteCategory = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    pendingDeleteSubcategory?.let { (parent, sub) ->
        AlertDialog(
            onDismissRequest = { pendingDeleteSubcategory = null },
            title = { Text("Remove subcategory?") },
            text = {
                Text("\"${sub.name}\" under \"${parent.name}\" will be removed from pickers.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.onEvent(CategoryUiEvent.OnDeleteSubcategory(sub.id))
                        pendingDeleteSubcategory = null
                    },
                    enabled = !uiState.isSaving
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Categories", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    TextButton(onClick = onNavigateBack) {
                        Text("Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onNavigateToAddCategory) {
                Text("+", style = MaterialTheme.typography.titleLarge)
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            uiState.statusMessage?.let { message ->
                MmrStatusBanner(
                    message = message,
                    onDismiss = { viewModel.onEvent(CategoryUiEvent.OnDismissMessage) },
                    isError = false
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = uiState.typeFilter == null,
                    onClick = { viewModel.onEvent(CategoryUiEvent.OnTypeFilterSelected(null)) },
                    label = { Text("All") }
                )
                FilterChip(
                    selected = uiState.typeFilter == CategoryType.EXPENSE,
                    onClick = {
                        viewModel.onEvent(CategoryUiEvent.OnTypeFilterSelected(CategoryType.EXPENSE))
                    },
                    label = { Text("Expense") }
                )
                FilterChip(
                    selected = uiState.typeFilter == CategoryType.INCOME,
                    onClick = {
                        viewModel.onEvent(CategoryUiEvent.OnTypeFilterSelected(CategoryType.INCOME))
                    },
                    label = { Text("Income") }
                )
            }

            when {
                uiState.isLoading && uiState.categories.isEmpty() -> {
                    MmrLoadingState()
                }

                uiState.errorMessage != null && uiState.categories.isEmpty() -> {
                    MmrErrorState(
                        message = uiState.errorMessage!!,
                        onRetry = { viewModel.onEvent(CategoryUiEvent.OnRetryLoad) }
                    )
                }

                uiState.isEmpty -> {
                    MmrEmptyState(message = "No categories yet. Tap + to add one.")
                }

                else -> {
                    if (uiState.errorMessage != null) {
                        MmrStatusBanner(
                            message = uiState.errorMessage!!,
                            onDismiss = { viewModel.onEvent(CategoryUiEvent.OnDismissMessage) }
                        )
                    }
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(uiState.categories, key = { it.id }) { category ->
                            CategoryHierarchyCard(
                                category = category,
                                onEditCategory = { onNavigateToEditCategory(category.id) },
                                onDeleteCategory = { pendingDeleteCategory = category },
                                onAddSubcategory = { onNavigateToAddSubcategory(category.id) },
                                onEditSubcategory = onNavigateToEditSubcategory,
                                onDeleteSubcategory = { sub ->
                                    pendingDeleteSubcategory = category to sub
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CategoryHierarchyCard(
    category: CategoryTreeItemUi,
    onEditCategory: () -> Unit,
    onDeleteCategory: () -> Unit,
    onAddSubcategory: () -> Unit,
    onEditSubcategory: (Long) -> Unit,
    onDeleteSubcategory: (SubcategoryItemUi) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onEditCategory),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(14.dp)
                        .clip(CircleShape)
                        .background(parseColorHex(category.colorHex))
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = category.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = buildString {
                            append(category.type.name.lowercase().replaceFirstChar { it.uppercase() })
                            append(" · Order ")
                            append(category.sortOrder)
                            if (category.isDefault) {
                                append(" · Default")
                            }
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (!category.isDefault) {
                    TextButton(onClick = onDeleteCategory) {
                        Text("Remove", color = MaterialTheme.colorScheme.error)
                    }
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp))

            if (category.subcategories.isEmpty()) {
                Text(
                    text = "No subcategories",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 26.dp, bottom = 4.dp)
                )
            } else {
                category.subcategories.forEach { sub ->
                    SubcategoryHierarchyRow(
                        subcategory = sub,
                        parentColorHex = category.colorHex,
                        onEdit = { onEditSubcategory(sub.id) },
                        onDelete = { onDeleteSubcategory(sub) },
                        modifier = Modifier.padding(start = 4.dp)
                    )
                }
            }

            TextButton(onClick = onAddSubcategory) {
                Text("+ Add subcategory under ${category.name}")
            }
        }
    }
}
