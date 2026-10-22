package com.mmr.feature.category.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mmr.feature.category.model.SubcategoryItemUi

@Composable
fun ParentCategoryHeader(
    parentName: String,
    parentColorHex: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(12.dp)
                .clip(CircleShape)
                .background(parseColorHex(parentColorHex))
        )
        Spacer(modifier = Modifier.width(10.dp))
        Column {
            Text(
                text = "Parent: $parentName",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
            subtitle?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun SubcategoryHierarchyRow(
    subcategory: SubcategoryItemUi,
    parentColorHex: String,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onEdit)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .padding(start = 8.dp)
                .width(20.dp)
                .height(2.dp)
                .background(MaterialTheme.colorScheme.outlineVariant)
        )
        Box(
            modifier = Modifier
                .padding(horizontal = 10.dp)
                .size(8.dp)
                .clip(CircleShape)
                .background(parseColorHex(parentColorHex))
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = subcategory.name,
                style = MaterialTheme.typography.bodyLarge
            )
            Text(
                text = "Order ${subcategory.sortOrder}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        TextButton(onClick = onDelete) {
            Text("Remove", color = MaterialTheme.colorScheme.error)
        }
    }
}

@Composable
fun SubcategoryListSection(
    parentName: String,
    parentColorHex: String,
    subcategories: List<SubcategoryItemUi>,
    onAddSubcategory: () -> Unit,
    onEditSubcategory: (Long) -> Unit,
    onDeleteSubcategory: (SubcategoryItemUi) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
        Text(
            text = "Subcategories",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
        ParentCategoryHeader(
            parentName = parentName,
            parentColorHex = parentColorHex,
            subtitle = "Child items appear under this category in pickers.",
            modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
        )
        if (subcategories.isEmpty()) {
            Text(
                text = "No subcategories yet.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 28.dp, top = 4.dp, bottom = 8.dp)
            )
        } else {
            subcategories.forEach { sub ->
                SubcategoryHierarchyRow(
                    subcategory = sub,
                    parentColorHex = parentColorHex,
                    onEdit = { onEditSubcategory(sub.id) },
                    onDelete = { onDeleteSubcategory(sub) },
                    modifier = Modifier.padding(start = 12.dp)
                )
            }
        }
        TextButton(onClick = onAddSubcategory) {
            Text("+ Add subcategory")
        }
    }
}
