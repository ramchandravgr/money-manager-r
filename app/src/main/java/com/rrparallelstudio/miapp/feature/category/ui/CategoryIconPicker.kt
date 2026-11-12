package com.rrparallelstudio.miapp.feature.category.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.rrparallelstudio.miapp.core.model.category.MmrCategoryIconCatalog
import com.rrparallelstudio.miapp.feature.category.model.CategoryColorPresets
import com.rrparallelstudio.miapp.ui.theme.MmrBrandColors

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CategoryIconPicker(
    selectedIconName: String,
    onIconSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
    accentHex: String = CategoryColorPresets.DEFAULT_HEX,
) {
    val accent = parseColorHex(accentHex)
    Text(
        text = "Icon",
        style =
            com.rrparallelstudio.miapp.ui.theme
                .mmrSettingsRowPrimaryStyle(MmrBrandColors.TextMedium),
        modifier = modifier,
    )
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        MmrCategoryIconCatalog.pickerOptions.forEach { iconName ->
            val selected = iconName == selectedIconName
            Box(
                modifier =
                    Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(accent.copy(alpha = if (selected) 1f else 0.35f))
                        .border(
                            width = if (selected) 2.dp else 1.dp,
                            color =
                                if (selected) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.outlineVariant
                                },
                            shape = CircleShape,
                        ).clickable { onIconSelected(iconName) },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = MmrCategoryIconCatalog.resolve(iconName),
                    contentDescription = null,
                    tint = if (selected) MmrBrandColors.Reversal else accent,
                    modifier = Modifier.size(22.dp),
                )
            }
        }
    }
}

@Composable
fun CategoryIconGlyph(
    iconName: String,
    colorHex: String,
    modifier: Modifier = Modifier,
    sizeDp: androidx.compose.ui.unit.Dp = 22.dp,
) {
    val accent = parseColorHex(colorHex)
    Box(
        modifier =
            modifier
                .size(sizeDp)
                .clip(CircleShape)
                .background(accent.copy(alpha = 0.2f)),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = MmrCategoryIconCatalog.resolve(iconName),
            contentDescription = null,
            tint = accent,
            modifier = Modifier.size(sizeDp * 0.55f),
        )
    }
}
