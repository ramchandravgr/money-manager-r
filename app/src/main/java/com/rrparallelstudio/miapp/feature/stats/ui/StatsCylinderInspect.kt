package com.rrparallelstudio.miapp.feature.stats.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.rrparallelstudio.miapp.R
import com.rrparallelstudio.miapp.feature.stats.model.CategoryBreakdownUi
import com.rrparallelstudio.miapp.ui.adaptive.mmrScreenHorizontalPadding
import com.rrparallelstudio.miapp.ui.components.MmrDimens
import com.rrparallelstudio.miapp.ui.theme.MmrBrandColors

internal fun categoryFocusKey(item: CategoryBreakdownUi): Long =
    item.categoryId ?: item.categoryName.hashCode().toLong()

@Composable
fun StatsCylinderInspectSection(
    breakdowns: List<CategoryBreakdownUi>,
    onNavigateToCategoryDetail: (CategoryBreakdownUi) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (breakdowns.isEmpty()) return

    var focusedKey by remember { mutableStateOf<String?>(null) }

    fun onCategoryTap(item: CategoryBreakdownUi) {
        val key = categoryBridgeKey(item)
        if (focusedKey == key) {
            onNavigateToCategoryDetail(item)
        } else {
            focusedKey = key
        }
    }

    val segmentColors =
        StatsCylinderPaint.resolve(
            breakdowns = breakdowns,
            themeAccent = MmrBrandColors.Active,
        )
    val horizontal = mmrScreenHorizontalPadding()

    Column(modifier = modifier.fillMaxSize()) {
        Row(
            modifier =
                Modifier
                    .weight(1f, fill = true)
                    .fillMaxWidth()
                    .padding(
                        start = horizontal,
                        end = horizontal,
                        top = MmrDimens.spaceMd,
                        bottom = MmrDimens.spaceSm,
                    ),
        ) {
            StatsCategoryPillBar(
                breakdowns = breakdowns,
                segmentColors = segmentColors,
                focusedCategoryKey = focusedKey,
                onSegmentClick = { onCategoryTap(it) },
                modifier =
                    Modifier
                        .fillMaxHeight()
                        .padding(end = MmrDimens.spaceMd),
            )
            Column(
                modifier =
                    Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(MmrDimens.spaceSm),
            ) {
                breakdowns.forEach { item ->
                    val key = categoryBridgeKey(item)
                    StatsCylinderCategoryCard(
                        breakdown = item,
                        accent = segmentColors[key] ?: parseStatsCategoryColor(item.colorHex),
                        focused = focusedKey == key,
                        dimmed = focusedKey != null && focusedKey != key,
                        onClick = { onCategoryTap(item) },
                    )
                }
            }
        }
        StatsCylinderNativeFooter(
            hasFocus = focusedKey != null,
            onClearFocus = { focusedKey = null },
        )
        HorizontalDivider(
            color = MmrBrandColors.TransactionListBorder,
            thickness = MmrDimens.transactionListDividerThickness,
        )
    }
}

@Composable
private fun StatsCylinderCategoryCard(
    breakdown: CategoryBreakdownUi,
    accent: Color,
    focused: Boolean,
    dimmed: Boolean,
    onClick: () -> Unit,
) {
    val contentAlpha = if (dimmed) 0.38f else 1f
    val borderColor = if (focused) accent else MmrBrandColors.BorderMain.copy(alpha = 0.5f)
    val bg = if (focused) accent.copy(alpha = 0.1f) else MmrBrandColors.SurfaceWhite
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(bg.copy(alpha = contentAlpha))
                .border(
                    width = if (focused) 2.dp else 1.dp,
                    color = borderColor,
                    shape = RoundedCornerShape(12.dp),
                ).clickable(onClick = onClick)
                .padding(horizontal = MmrDimens.spaceMd, vertical = MmrDimens.spaceSm),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier =
                    Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(accent.copy(alpha = contentAlpha)),
            )
            Spacer(Modifier.width(MmrDimens.spaceSm))
            Text(
                text = breakdown.categoryName,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = if (focused) FontWeight.Bold else FontWeight.Medium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
                color = MmrBrandColors.TextStrong.copy(alpha = contentAlpha),
            )
            Text(
                text = breakdown.percentageFormatted,
                style =
                    com.rrparallelstudio.miapp.ui.theme
                        .mmrStatsChartLabel10Style(
                            accent.copy(alpha = contentAlpha),
                        ).copy(fontWeight = FontWeight.Bold),
                modifier =
                    Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(accent.copy(alpha = 0.12f * contentAlpha))
                        .padding(horizontal = 6.dp, vertical = 2.dp),
            )
        }
        Text(
            text = breakdown.amountFormatted,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(start = 18.dp, top = 2.dp),
            color = MmrBrandColors.TextStrong.copy(alpha = contentAlpha),
        )
    }
}

@Composable
private fun StatsCylinderNativeFooter(
    hasFocus: Boolean,
    onClearFocus: () -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .background(MmrBrandColors.SurfaceWhite)
                .padding(horizontal = MmrDimens.spaceLg, vertical = MmrDimens.spaceMd),
        verticalAlignment = Alignment.Top,
    ) {
        Text(
            text =
                if (hasFocus) {
                    stringResource(R.string.stats_cylinder_hint_focused)
                } else {
                    stringResource(R.string.stats_cylinder_hint_default)
                },
            style = MaterialTheme.typography.labelSmall,
            color = MmrBrandColors.TextMedium,
            modifier = Modifier.weight(1f),
        )
        if (hasFocus) {
            TextButton(onClick = onClearFocus) {
                Text(
                    text = stringResource(R.string.stats_cylinder_clear_focus),
                    style =
                        com.rrparallelstudio.miapp.ui.theme
                            .mmrStatsChartLabel12Style(MmrBrandColors.Active),
                )
            }
        }
    }
}
