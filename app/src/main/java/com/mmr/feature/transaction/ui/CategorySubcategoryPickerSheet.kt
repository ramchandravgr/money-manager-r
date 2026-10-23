package com.mmr.feature.transaction.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.mmr.app.ui.components.MmrDimens
import com.mmr.app.ui.theme.MmrMmColors
import com.mmr.feature.category.ui.CategoryPickerInlineAdd
import com.mmr.feature.category.ui.CategoryPickerPaneLabel
import com.mmr.feature.category.ui.CategoryPickerParentRow
import com.mmr.feature.category.ui.CategoryPickerSheetHeader
import com.mmr.feature.category.ui.CategoryPickerSubRow
import com.mmr.feature.transaction.model.AddEditTransactionUiEvent
import com.mmr.feature.transaction.model.AddEditTransactionUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategorySubcategoryPickerSheet(
    state: AddEditTransactionUiState,
    onEvent: (AddEditTransactionUiEvent) -> Unit,
    sheetState: SheetState,
    onDismiss: () -> Unit,
) {
    val highlightedId = state.pickerHighlightedCategoryId
    val highlightedCategory = state.categories.firstOrNull { it.id == highlightedId }
    val subcategories = highlightedCategory?.subcategories ?: emptyList()

    state.pickerNameEditor?.let { editor ->
        TransactionPickerNameEditorDialog(
            editor = editor,
            onNameChange = { onEvent(AddEditTransactionUiEvent.OnPickerNameEditorChanged(it)) },
            onSave = { onEvent(AddEditTransactionUiEvent.OnPickerNameEditorSave) },
            onDismiss = { onEvent(AddEditTransactionUiEvent.OnPickerNameEditorDismiss) },
        )
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MmrMmColors.SurfaceWhite,
        dragHandle = {
            androidx.compose.material3.BottomSheetDefaults
                .DragHandle()
        },
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            CategoryPickerSheetHeader(
                onClose = onDismiss,
                parentOnlyVisible = highlightedCategory != null && subcategories.isNotEmpty(),
                onParentOnly = { onEvent(AddEditTransactionUiEvent.OnPickerConfirmParentOnly) },
            )

            HorizontalDivider(color = MmrMmColors.BorderMain.copy(alpha = 0.6f))

            if (state.categories.isEmpty()) {
                Column(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(MmrDimens.space2xl),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = "No categories for this transaction type.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MmrMmColors.TextMedium,
                        textAlign = TextAlign.Center,
                    )
                    CategoryPickerInlineAdd(
                        label = "Add category",
                        onClick = { onEvent(AddEditTransactionUiEvent.OnPickerAddCategory) },
                    )
                }
            } else {
                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .heightIn(min = 240.dp, max = 380.dp),
                ) {
                    LazyColumn(
                        modifier =
                            Modifier
                                .weight(0.38f)
                                .fillMaxHeight()
                                .background(MmrMmColors.ListBackground),
                    ) {
                        items(state.categories, key = { it.id }) { category ->
                            CategoryPickerParentRow(
                                name = category.name,
                                colorHex = category.colorHex,
                                selected = category.id == highlightedId,
                                onClick = {
                                    onEvent(AddEditTransactionUiEvent.OnPickerParentSelected(category.id))
                                },
                                onLongClick = {
                                    onEvent(AddEditTransactionUiEvent.OnPickerEditCategory(category.id))
                                },
                            )
                        }
                        item {
                            CategoryPickerInlineAdd(
                                label = "Add category",
                                onClick = { onEvent(AddEditTransactionUiEvent.OnPickerAddCategory) },
                            )
                        }
                    }

                    Box(
                        modifier =
                            Modifier
                                .fillMaxHeight()
                                .width(1.dp)
                                .background(MmrMmColors.BorderMain.copy(alpha = 0.5f)),
                    )

                    LazyColumn(
                        modifier =
                            Modifier
                                .weight(0.62f)
                                .fillMaxHeight(),
                    ) {
                        when {
                            highlightedCategory == null -> {
                                item {
                                    Text(
                                        text = "Select a category",
                                        modifier =
                                            Modifier
                                                .fillMaxWidth()
                                                .padding(MmrDimens.space2xl),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MmrMmColors.TextLight,
                                        textAlign = TextAlign.Start,
                                    )
                                }
                            }
                            subcategories.isEmpty() -> {
                                item {
                                    Text(
                                        text = "No subcategories — \"${highlightedCategory.name}\" will be used.",
                                        modifier =
                                            Modifier
                                                .fillMaxWidth()
                                                .padding(
                                                    horizontal = MmrDimens.spaceLg,
                                                    vertical = MmrDimens.spaceMd,
                                                ),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MmrMmColors.TextMedium,
                                    )
                                }
                                item {
                                    CategoryPickerInlineAdd(
                                        label = "Add subcategory",
                                        onClick = { onEvent(AddEditTransactionUiEvent.OnPickerAddSubcategory) },
                                    )
                                }
                            }
                            else -> {
                                item {
                                    CategoryPickerPaneLabel("Subcategory (optional)")
                                }
                                items(subcategories, key = { it.id }) { sub ->
                                    CategoryPickerSubRow(
                                        name = sub.name,
                                        colorHex = highlightedCategory.colorHex,
                                        onClick = {
                                            onEvent(AddEditTransactionUiEvent.OnPickerSubcategorySelected(sub.id))
                                        },
                                        onLongClick = {
                                            onEvent(AddEditTransactionUiEvent.OnPickerEditSubcategory(sub.id))
                                        },
                                    )
                                }
                                item {
                                    CategoryPickerInlineAdd(
                                        label = "Add subcategory",
                                        onClick = { onEvent(AddEditTransactionUiEvent.OnPickerAddSubcategory) },
                                    )
                                }
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(MmrDimens.spaceLg))
        }
    }
}
