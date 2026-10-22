package com.mmr.feature.transaction.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.mmr.feature.category.ui.parseColorHex
import com.mmr.feature.transaction.model.AddEditTransactionUiEvent
import com.mmr.feature.transaction.model.AddEditTransactionUiState
import com.mmr.feature.transaction.model.CategoryUi
import com.mmr.feature.transaction.model.SubcategoryUi

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun CategorySubcategoryPickerSheet(
    state: AddEditTransactionUiState,
    onEvent: (AddEditTransactionUiEvent) -> Unit,
    sheetState: SheetState,
    onDismiss: () -> Unit
) {
    val highlightedId = state.pickerHighlightedCategoryId
    val highlightedCategory = state.categories.firstOrNull { it.id == highlightedId }
    val subcategories = highlightedCategory?.subcategories ?: emptyList()

    state.pickerNameEditor?.let { editor ->
        TransactionPickerNameEditorDialog(
            editor = editor,
            onNameChange = { onEvent(AddEditTransactionUiEvent.OnPickerNameEditorChanged(it)) },
            onSave = { onEvent(AddEditTransactionUiEvent.OnPickerNameEditorSave) },
            onDismiss = { onEvent(AddEditTransactionUiEvent.OnPickerNameEditorDismiss) }
        )
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF1A1A1A))
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Category",
                    color = Color.White,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    if (highlightedCategory != null && subcategories.isNotEmpty()) {
                        TextButton(onClick = { onEvent(AddEditTransactionUiEvent.OnPickerConfirmParentOnly) }) {
                            Text("Parent only", color = Color.White)
                        }
                    }
                    TextButton(onClick = onDismiss) {
                        Text("Close", color = Color.White)
                    }
                }
            }

            if (state.categories.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "No categories for this transaction type.",
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    PickerAddActionButton(
                        label = "Add category",
                        onClick = { onEvent(AddEditTransactionUiEvent.OnPickerAddCategory) }
                    )
                }
            } else {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 300.dp, max = 440.dp)
                ) {
                    LazyColumn(
                        modifier = Modifier
                            .weight(0.42f)
                            .fillMaxHeight()
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        items(state.categories, key = { it.id }) { category ->
                            ParentPickerRow(
                                category = category,
                                selected = category.id == highlightedId,
                                onClick = {
                                    onEvent(AddEditTransactionUiEvent.OnPickerParentSelected(category.id))
                                },
                                onLongClick = {
                                    onEvent(AddEditTransactionUiEvent.OnPickerEditCategory(category.id))
                                }
                            )
                        }
                        item {
                            PickerAddActionButton(
                                label = "Add category",
                                onClick = { onEvent(AddEditTransactionUiEvent.OnPickerAddCategory) }
                            )
                        }
                    }

                    VerticalDivider(modifier = Modifier.fillMaxHeight())

                    LazyColumn(
                        modifier = Modifier
                            .weight(0.58f)
                            .fillMaxHeight(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        if (highlightedCategory == null) {
                            item {
                                Text(
                                    text = "Select a category",
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center
                                )
                            }
                        } else if (subcategories.isEmpty()) {
                            item {
                                Text(
                                    text = "No subcategories.\n\"${highlightedCategory.name}\" will be used.",
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center
                                )
                            }
                            item {
                                PickerAddActionButton(
                                    label = "Add subcategory",
                                    onClick = { onEvent(AddEditTransactionUiEvent.OnPickerAddSubcategory) }
                                )
                            }
                        } else {
                            item {
                                Text(
                                    text = "Optional subcategory",
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 8.dp),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center
                                )
                            }
                            items(subcategories, key = { it.id }) { sub ->
                                SubcategoryPickerRow(
                                    subcategory = sub,
                                    parentColorHex = highlightedCategory.colorHex,
                                    onClick = {
                                        onEvent(AddEditTransactionUiEvent.OnPickerSubcategorySelected(sub.id))
                                    },
                                    onLongClick = {
                                        onEvent(AddEditTransactionUiEvent.OnPickerEditSubcategory(sub.id))
                                    }
                                )
                            }
                            item {
                                PickerAddActionButton(
                                    label = "Add subcategory",
                                    onClick = { onEvent(AddEditTransactionUiEvent.OnPickerAddSubcategory) }
                                )
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun PickerAddActionButton(
    label: String,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        FilledTonalIconButton(onClick = onClick) {
            Icon(Icons.Filled.Add, contentDescription = label)
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ParentPickerRow(
    category: CategoryUi,
    selected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val background = if (selected) {
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f)
    } else {
        Color.Transparent
    }
    val textColor = if (selected) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.onSurface
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(background)
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
            .padding(horizontal = 12.dp, vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(parseColorHex(category.colorHex))
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = category.name,
            style = MaterialTheme.typography.bodyLarge,
            color = textColor,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            textAlign = TextAlign.Center
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SubcategoryPickerRow(
    subcategory: SubcategoryUi,
    parentColorHex: String,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(parseColorHex(parentColorHex))
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = subcategory.name,
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center
        )
    }
    HorizontalDivider(
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
    )
}
