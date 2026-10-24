package com.mmr.feature.approval.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mmr.app.ui.components.MmrDimens
import com.mmr.app.ui.components.MmrTransactionFormExpandablePicker
import com.mmr.app.ui.components.MmrTransactionFormIcons
import com.mmr.app.ui.theme.MmrBrandColors
import com.mmr.feature.approval.model.ApprovalCenterUiEvent
import com.mmr.feature.approval.model.PendingSmsMergePrompt
import com.mmr.feature.transaction.ui.CategoryPickerPanelState
import com.mmr.feature.transaction.ui.CategorySubcategoryPickerPanel
import com.mmr.feature.transaction.ui.mergeCategoryPickerCallbacks

@Composable
internal fun ApprovalLinkMergeCategoryField(
    prompt: PendingSmsMergePrompt,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onEvent: (ApprovalCenterUiEvent) -> Unit,
) {
    val displayValue =
        when {
            prompt.useLedgerCategory -> "Keep ledger category"
            else -> {
                val category = prompt.mergeCategories.firstOrNull { it.id == prompt.selectedCategoryId }
                val sub = category?.subcategories?.firstOrNull { it.id == prompt.selectedSubcategoryId }
                when {
                    category == null -> ""
                    sub != null -> "${category.name} · ${sub.name}"
                    else -> category.name
                }
            }
        }

    MmrTransactionFormExpandablePicker(
        label = "Category on merge",
        value = displayValue,
        emptyHint = "Select category",
        icon = MmrTransactionFormIcons.category,
        expanded = expanded,
        onExpandedChange = onExpandedChange,
        enabled = prompt.mergeCategories.isNotEmpty(),
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "Keep ledger category",
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .clickable {
                            onEvent(ApprovalCenterUiEvent.OnMergeUseLedgerCategory)
                            onExpandedChange(false)
                        }.padding(horizontal = MmrDimens.spaceLg, vertical = 12.dp),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (prompt.useLedgerCategory) FontWeight.SemiBold else FontWeight.Normal,
                color = if (prompt.useLedgerCategory) MmrBrandColors.Active else MmrBrandColors.Reversal,
            )
            CategorySubcategoryPickerPanel(
                panelState =
                    CategoryPickerPanelState(
                        categories = prompt.mergeCategories,
                        pickerHighlightedCategoryId = prompt.mergePickerHighlightedCategoryId,
                    ),
                callbacks = mergeCategoryPickerCallbacks(onEvent),
                maxPanelHeight = 220.dp,
                onPickerFinished = { onExpandedChange(false) },
            )
        }
    }
}
