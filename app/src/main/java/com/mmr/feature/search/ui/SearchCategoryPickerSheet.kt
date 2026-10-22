package com.mmr.feature.search.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
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
import com.mmr.feature.search.model.TransactionSearchUiEvent
import com.mmr.feature.search.model.TransactionSearchUiState
import com.mmr.feature.transaction.model.CategoryUi
import com.mmr.feature.transaction.model.SubcategoryUi

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchCategoryPickerSheet(
    state: TransactionSearchUiState,
    onEvent: (TransactionSearchUiEvent) -> Unit,
    sheetState: SheetState,
    onDismiss: () -> Unit
) {
    val highlightedId = state.pickerHighlightedCategoryId
    val highlightedCategory = state.categories.firstOrNull { it.id == highlightedId }
    val subcategories = highlightedCategory?.subcategories ?: emptyList()

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
                Row {
                    if (highlightedCategory != null && subcategories.isNotEmpty()) {
                        TextButton(onClick = { onEvent(TransactionSearchUiEvent.OnPickerConfirmParentOnly) }) {
                            Text("Parent only", color = Color.White)
                        }
                    }
                    TextButton(onClick = onDismiss) {
                        Text("Close", color = Color.White)
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 280.dp, max = 420.dp)
            ) {
                LazyColumn(
                    modifier = Modifier
                        .weight(0.42f)
                        .fillMaxHeight()
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    items(state.categories, key = { it.id }) { category ->
                        SearchParentRow(
                            category = category,
                            selected = category.id == highlightedId,
                            onClick = {
                                onEvent(TransactionSearchUiEvent.OnPickerParentSelected(category.id))
                            }
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
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else if (subcategories.isEmpty()) {
                        item {
                            Text(
                                text = highlightedCategory.name,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                textAlign = TextAlign.Center,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    } else {
                        items(subcategories, key = { it.id }) { sub ->
                            SearchSubRow(
                                subcategory = sub,
                                parentColorHex = highlightedCategory.colorHex,
                                onClick = {
                                    onEvent(TransactionSearchUiEvent.OnPickerSubcategorySelected(sub.id))
                                }
                            )
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun SearchParentRow(
    category: CategoryUi,
    selected: Boolean,
    onClick: () -> Unit
) {
    val background = if (selected) {
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f)
    } else {
        Color.Transparent
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(background)
            .clickable(onClick = onClick)
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
            textAlign = TextAlign.Center,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
        )
    }
}

@Composable
private fun SearchSubRow(
    subcategory: SubcategoryUi,
    parentColorHex: String,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
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
        Text(text = subcategory.name, textAlign = TextAlign.Center)
    }
}
