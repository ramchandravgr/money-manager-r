package com.mmr.feature.home.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.mmr.app.ui.theme.MmrBrandColors
import com.mmr.feature.home.model.HomeExcelExportOptionUi

@Composable
fun HomeExcelExportPeriodDialog(
    visible: Boolean,
    title: String,
    options: List<HomeExcelExportOptionUi>,
    onDismiss: () -> Unit,
    onOptionSelected: (HomeExcelExportOptionUi) -> Unit,
) {
    if (!visible || options.isEmpty()) return

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth(0.92f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.White),
        ) {
            Text(
                text = title,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .background(MmrBrandColors.DialogHeader)
                        .padding(horizontal = 20.dp, vertical = 14.dp),
                color = Color.White,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            options.forEachIndexed { index, option ->
                if (index > 0) {
                    HorizontalDivider(color = MmrBrandColors.BorderMain)
                }
                Text(
                    text = option.label,
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .clickable { onOptionSelected(option) }
                            .padding(horizontal = 20.dp, vertical = 16.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MmrBrandColors.TextStrong,
                )
            }
        }
    }
}
