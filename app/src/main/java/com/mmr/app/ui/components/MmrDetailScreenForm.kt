package com.mmr.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mmr.app.ui.adaptive.MmrLabelValueRow
import com.mmr.app.ui.theme.MmrBrandColors

/** White inset card (mockup `.card`). */
@Composable
fun MmrDetailInsetCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MmrBrandColors.SurfaceWhite),
        border = BorderStroke(1.dp, MmrBrandColors.BorderMain),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            content = content,
        )
    }
}

/** Gray overview block (mockup `.overview`). */
@Composable
fun MmrDetailOverviewCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = MmrBrandColors.SettingsSectionHeaderFill,
        border = BorderStroke(1.dp, MmrBrandColors.BorderMain),
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
            content = content,
        )
    }
}

/** Tinted list row card (schedule date, payment item). */
@Composable
fun MmrDetailListItemCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = MmrBrandColors.SurfaceWhite),
        border = BorderStroke(1.dp, MmrBrandColors.BorderMain),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            content = content,
        )
    }
}

@Composable
fun MmrDetailHeroBlock(
    title: String,
    amountFormatted: String,
    modifier: Modifier = Modifier,
    amountIsIncome: Boolean = false,
    subtitle: String? = null,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MmrBrandColors.TextStrong,
            lineHeight = 20.sp,
        )
        Text(
            text = amountFormatted,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color =
                if (amountIsIncome) {
                    MmrBrandColors.AmountPositive
                } else {
                    MmrBrandColors.AmountNegative
                },
            modifier = Modifier.padding(top = 2.dp),
        )
        subtitle?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.labelSmall,
                color = MmrBrandColors.TextMedium,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
    }
}

@Composable
fun MmrDetailSectionTitle(
    text: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = text,
        modifier = modifier.padding(top = 4.dp, bottom = 2.dp),
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Bold,
        color = MmrBrandColors.TextStrong,
    )
}

@Composable
fun MmrDetailSectionHint(
    text: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = text,
        modifier = modifier,
        style = MaterialTheme.typography.bodySmall,
        color = MmrBrandColors.TextMedium,
        lineHeight = 16.sp,
    )
}

@Composable
fun MmrDetailMetaRow(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    valueMaxLines: Int = 3,
) {
    MmrLabelValueRow(
        label = label,
        value = value,
        valueMaxLines = valueMaxLines,
        labelValueGap = MmrDimens.transactionDetailLabelValueGap,
        rowVerticalPadding = MmrDimens.transactionDetailLabelValueRowVertical,
        rowMinHeight = 22.dp,
        modifier = modifier,
    )
}

@Composable
fun MmrDetailMetaCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    MmrDetailInsetCard(modifier) {
        Column(verticalArrangement = Arrangement.spacedBy(MmrDimens.transactionDetailMetaRowSpacing)) {
            content()
        }
    }
}

@Composable
fun MmrDetailLinkedSmsCard(
    sender: String,
    receivedLabel: String,
    snippet: String,
    modifier: Modifier = Modifier,
    amountFormatted: String? = null,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        color = MmrBrandColors.SettingsSectionHeaderFill,
        border = BorderStroke(1.dp, MmrBrandColors.BorderMain),
    ) {
        Column(
            modifier = Modifier.padding(9.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(sender, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = MmrBrandColors.TextStrong)
            Text(receivedLabel, style = MaterialTheme.typography.labelSmall, color = MmrBrandColors.TextMedium)
            amountFormatted?.let {
                Text(it, style = MaterialTheme.typography.labelMedium, color = MmrBrandColors.Active)
            }
            Text(
                snippet,
                style = MaterialTheme.typography.bodySmall,
                color = MmrBrandColors.TextStrong,
                lineHeight = 16.sp,
            )
        }
    }
}

@Composable
fun MmrDetailTextLink(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Text(
        text = text,
        modifier = modifier.clickable(onClick = onClick),
        color = MmrBrandColors.Active,
        fontSize = 14.sp,
        fontWeight = FontWeight.Medium,
    )
}

@Composable
fun MmrDetailStatusBadge(
    text: String,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(6.dp),
        color = MmrBrandColors.HeaderSelection,
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = MmrBrandColors.Header,
        )
    }
}

@Composable
fun MmrDetailProgressStrip(
    progress: Float,
    modifier: Modifier = Modifier,
    color: androidx.compose.ui.graphics.Color = MmrBrandColors.AmountNegative,
) {
    LinearProgressIndicator(
        progress = { progress.coerceIn(0f, 1f) },
        modifier =
            modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(4.dp)),
        color = color,
        trackColor = MmrBrandColors.BorderMain,
    )
}

@Composable
fun MmrDetailKeyValueRow(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    valueColor: androidx.compose.ui.graphics.Color = MmrBrandColors.Reversal,
    valueFontWeight: FontWeight = FontWeight.Medium,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top,
    ) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MmrBrandColors.TextMedium)
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = valueFontWeight,
            color = valueColor,
            textAlign = TextAlign.End,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.fillMaxWidth(0.58f),
        )
    }
}
