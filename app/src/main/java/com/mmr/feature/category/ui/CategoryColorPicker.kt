package com.mmr.feature.category.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.mmr.feature.category.model.CategoryColorPresets

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CategoryColorPicker(
    selectedColorHex: String,
    onColorSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Text(
        text = "Color",
        style = MaterialTheme.typography.titleMedium,
        modifier = modifier
    )
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        CategoryColorPresets.presets.forEach { hex ->
            val color = parseColorHex(hex)
            val selected = hex.equals(selectedColorHex, ignoreCase = true)
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(color)
                    .border(
                        width = if (selected) 3.dp else 1.dp,
                        color = if (selected) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.outlineVariant
                        },
                        shape = CircleShape
                    )
                    .clickable { onColorSelected(hex) }
            )
        }
    }
}

fun parseColorHex(hex: String): Color {
    val normalized = hex.removePrefix("#")
    if (normalized.length != 6) return Color.Gray
    return Color(
        alpha = 1f,
        red = normalized.substring(0, 2).toInt(16) / 255f,
        green = normalized.substring(2, 4).toInt(16) / 255f,
        blue = normalized.substring(4, 6).toInt(16) / 255f
    )
}
