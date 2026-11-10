package com.rrparallelstudio.miapp.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rrparallelstudio.miapp.ui.theme.MmrBrandColors

/** Small circles for theme / amount previews in settings pickers. */
@Composable
fun MmrSettingsColorSwatchStrip(
    colors: List<Color>,
    modifier: Modifier = Modifier,
    swatchSize: Dp = 18.dp,
    spacing: Dp = 5.dp,
    /** Use [MmrBrandColors.settingsSheetSwatchOutline] inside dark modal pickers; default = nav rows on white cards. */
    outlineColor: Color = MmrBrandColors.BorderMain,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(spacing),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        colors.forEach { color ->
            Box(
                modifier =
                    Modifier
                        .size(swatchSize)
                        .clip(CircleShape)
                        .background(color)
                        .border(0.5.dp, outlineColor, CircleShape),
            )
        }
    }
}

@Composable
fun MmrSettingsChoiceLabelWithSwatches(
    label: String,
    swatchColors: List<Color>,
    modifier: Modifier = Modifier,
    labelFontWeight: FontWeight = FontWeight.Normal,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(
            text = label,
            modifier = Modifier.weight(1f),
            color = MmrBrandColors.settingsSheetListRowText(),
            fontSize = 16.sp,
            fontWeight = labelFontWeight,
            maxLines = 2,
        )
        MmrSettingsColorSwatchStrip(
            colors = swatchColors,
            modifier = Modifier.padding(end = 4.dp),
            outlineColor = MmrBrandColors.settingsSheetSwatchOutline(),
        )
    }
}
