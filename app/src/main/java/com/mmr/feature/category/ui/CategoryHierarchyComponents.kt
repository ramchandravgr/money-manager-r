package com.mmr.feature.category.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mmr.app.ui.components.MmrIosSettingsNavRow
import com.mmr.app.ui.components.MmrIosSettingsSection
import com.mmr.app.ui.components.MmrIosSettingsValueRow
import com.mmr.feature.category.model.SubcategoryItemUi

@Composable
fun ParentCategoryHeader(
    parentName: String,
    parentColorHex: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier =
                Modifier
                    .size(12.dp)
                    .clip(CircleShape)
                    .background(parseColorHex(parentColorHex)),
        )
        Spacer(modifier = Modifier.width(10.dp))
        Column {
            Text(
                text = "Parent: $parentName",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
            )
            subtitle?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
fun SubcategoryListSection(
    parentName: String,
    @Suppress("UNUSED_PARAMETER") parentColorHex: String,
    subcategories: List<SubcategoryItemUi>,
    onAddSubcategory: () -> Unit,
    onEditSubcategory: (Long) -> Unit,
    @Suppress("UNUSED_PARAMETER") onDeleteSubcategory: (SubcategoryItemUi) -> Unit,
    modifier: Modifier = Modifier,
) {
    MmrIosSettingsSection(
        modifier = modifier,
        title = "Subcategories",
        footer = "Tap a subcategory to edit. Remove it from the edit screen.",
    ) {
        if (subcategories.isEmpty()) {
            MmrIosSettingsValueRow(
                label = "No subcategories yet",
                value = "",
                showDividerBelow = true,
            )
        } else {
            subcategories.forEachIndexed { index, sub ->
                MmrIosSettingsNavRow(
                    label = sub.name,
                    value = "Order ${sub.sortOrder}",
                    onClick = { onEditSubcategory(sub.id) },
                    showDividerBelow = index < subcategories.lastIndex,
                )
            }
        }
        MmrIosSettingsNavRow(
            label = "Add subcategory",
            onClick = onAddSubcategory,
            showDividerBelow = false,
        )
    }
}
