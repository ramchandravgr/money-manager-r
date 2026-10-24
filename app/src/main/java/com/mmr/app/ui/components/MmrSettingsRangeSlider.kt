package com.mmr.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Inbox
import androidx.compose.material.icons.outlined.Smartphone
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mmr.app.ui.theme.MmrBrandColors

@Composable
private fun mmrSettingsRangeSliderColors() =
    SliderDefaults.colors(
        thumbColor = MaterialTheme.colorScheme.primary,
        activeTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.42f),
        inactiveTrackColor = MmrBrandColors.BorderMain,
        activeTickColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.42f),
        inactiveTickColor = MmrBrandColors.BorderMain,
        disabledThumbColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
        disabledActiveTrackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.32f),
        disabledInactiveTrackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f),
    )

/**
 * Range slider for settings (inbox scan period, etc.): caption, flanking icons, min/max labels.
 * Uses [MaterialTheme.colorScheme.primary] so app color themes apply to track and thumb.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MmrSettingsRangeSlider(
    caption: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    steps: Int,
    minLabel: String,
    maxLabel: String,
    modifier: Modifier = Modifier,
    startIcon: ImageVector = Icons.Outlined.Inbox,
    endIcon: ImageVector = Icons.Outlined.Smartphone,
    sliderTestTag: String? = null,
) {
    val sliderColors = mmrSettingsRangeSliderColors()
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(
            text = caption,
            fontSize = 16.sp,
            color = MmrBrandColors.Reversal,
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = startIcon,
                contentDescription = null,
                tint = MmrBrandColors.TextLight,
                modifier = Modifier.size(22.dp),
            )
            Spacer(Modifier.width(6.dp))
            Slider(
                value = value,
                onValueChange = onValueChange,
                valueRange = valueRange,
                steps = steps,
                modifier =
                    Modifier
                        .weight(1f)
                        .height(32.dp)
                        .then(if (sliderTestTag != null) Modifier.testTag(sliderTestTag) else Modifier),
                colors = sliderColors,
                track = { sliderState ->
                    SliderDefaults.Track(
                        sliderState = sliderState,
                        modifier = Modifier.height(6.dp),
                        colors = sliderColors,
                        thumbTrackGapSize = 0.dp,
                    )
                },
            )
            Spacer(Modifier.width(6.dp))
            Icon(
                imageVector = endIcon,
                contentDescription = null,
                tint = MmrBrandColors.TextLight,
                modifier = Modifier.size(22.dp),
            )
        }
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(start = 2.dp, end = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = minLabel,
                fontSize = 12.sp,
                color = MmrBrandColors.TextMedium,
            )
            Spacer(Modifier.weight(1f))
            Text(
                text = maxLabel,
                fontSize = 12.sp,
                color = MmrBrandColors.TextMedium,
            )
        }
    }
}
