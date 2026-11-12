package com.rrparallelstudio.miapp.feature.home.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import com.rrparallelstudio.miapp.feature.home.model.HomeCategoryFilterRow
import com.rrparallelstudio.miapp.testing.MmrTestTags
import com.rrparallelstudio.miapp.ui.components.MmrDimens
import com.rrparallelstudio.miapp.ui.components.MmrFilterChip
import com.rrparallelstudio.miapp.ui.copy.mmrStaticLabelCase
import com.rrparallelstudio.miapp.ui.theme.MmrBrandColors
import com.rrparallelstudio.miapp.ui.theme.MmrExpressiveThemeFlags
import kotlin.math.roundToInt

/** Menu below chip; panel is centered on the screen (chip sits at the end of the scroll row). */
private class PinnedBelowChipPositionProvider(
    private val chipInWindow: IntRect,
    private val verticalGapPx: Int,
    private val horizontalInsetPx: Int,
) : PopupPositionProvider {
    override fun calculatePosition(
        anchorBounds: IntRect,
        windowSize: IntSize,
        layoutDirection: LayoutDirection,
        popupContentSize: IntSize,
    ): IntOffset {
        val maxWidth = (windowSize.width - horizontalInsetPx * 2).coerceAtLeast(0)
        val contentWidth = popupContentSize.width.coerceAtMost(maxWidth)
        val maxX = (windowSize.width - contentWidth).coerceAtLeast(0)
        val x = ((windowSize.width - contentWidth) / 2).coerceIn(horizontalInsetPx, maxX)
        val y = chipInWindow.bottom + verticalGapPx
        return IntOffset(x, y)
    }
}

@Composable
private fun categoryFilterMenuShellColor(): Color = MmrBrandColors.dialogBodySurface()

@Composable
private fun categoryFilterHeaderBandColor(): Color =
    if (MmrExpressiveThemeFlags.ENABLED) {
        lerp(
            categoryFilterMenuShellColor(),
            MmrBrandColors.Active.copy(alpha = 0.22f),
            0.38f,
        )
    } else {
        MmrBrandColors.expressiveFooterBandBackground()
    }

@Composable
private fun categoryFilterRowFill(checked: Boolean): Color {
    val base = MmrBrandColors.settingsSheetGroupedListFill()
    return if (checked) {
        lerp(base, MmrBrandColors.Active.copy(alpha = 0.28f), 0.42f)
    } else {
        base
    }
}

@Composable
fun HomeCategoryFilterDropdown(
    rows: List<HomeCategoryFilterRow>,
    selected: Set<Long>,
    onToggleCategory: (Long) -> Unit,
    onClearCategories: () -> Unit,
    modifier: Modifier = Modifier,
    emptyCategoriesMessage: String = "No categories yet.",
) {
    var expanded by remember { mutableStateOf(false) }
    var chipInWindow by remember { mutableStateOf(IntRect.Zero) }
    var pinnedChipInWindow by remember { mutableStateOf<IntRect?>(null) }
    val choiceLabels =
        remember(rows) {
            rows.filterIsInstance<HomeCategoryFilterRow.Choice>().associate { it.option.categoryId to it.option.label }
        }
    val triggerLabel =
        when {
            selected.isEmpty() -> "Categories".mmrStaticLabelCase()
            selected.size == 1 -> choiceLabels[selected.first()] ?: "Categories".mmrStaticLabelCase()
            else -> "${"Categories".mmrStaticLabelCase()} (${selected.size})"
        }
    val optionCount = remember(rows) { rows.count { it is HomeCategoryFilterRow.Choice } }
    val maxMenuHeight = (LocalConfiguration.current.screenHeightDp / 2).dp
    val menuShape = RoundedCornerShape(16.dp)
    val density = LocalDensity.current
    val gapBelowChipPx = remember(density) { with(density) { 6.dp.roundToPx() } }
    val horizontalInsetPx = remember(density) { with(density) { MmrDimens.space2xl.roundToPx() } }
    val listMaxHeight = maxMenuHeight - 52.dp
    val menuMaxWidth =
        (LocalConfiguration.current.screenWidthDp.dp - MmrDimens.space2xl * 2).coerceAtMost(300.dp)
    val shellColor = categoryFilterMenuShellColor()
    val headerBandColor = categoryFilterHeaderBandColor()
    val dividerColor =
        remember(shellColor) {
            lerp(shellColor, MmrBrandColors.BorderMain, 0.55f)
        }
    val positionProvider =
        remember(pinnedChipInWindow, gapBelowChipPx, horizontalInsetPx) {
            val chip = pinnedChipInWindow
            if (chip != null) {
                PinnedBelowChipPositionProvider(chip, gapBelowChipPx, horizontalInsetPx)
            } else {
                null
            }
        }

    Box(modifier = modifier) {
        MmrFilterChip(
            label = triggerLabel,
            selected = selected.isNotEmpty(),
            onClick = {
                pinnedChipInWindow = chipInWindow
                expanded = true
            },
            showDropdownCaret = true,
            modifier =
                Modifier
                    .testTag(MmrTestTags.homeCategoryFilterTrigger)
                    .onGloballyPositioned { coords ->
                        val pos = coords.positionInWindow()
                        chipInWindow =
                            IntRect(
                                pos.x.roundToInt(),
                                pos.y.roundToInt(),
                                (pos.x + coords.size.width).roundToInt(),
                                (pos.y + coords.size.height).roundToInt(),
                            )
                    },
        )
        if (expanded && positionProvider != null) {
            Popup(
                onDismissRequest = {
                    expanded = false
                    pinnedChipInWindow = null
                },
                popupPositionProvider = positionProvider,
                properties = PopupProperties(focusable = true, dismissOnBackPress = true),
            ) {
                Surface(
                    modifier =
                        Modifier
                            .widthIn(max = menuMaxWidth)
                            .wrapContentWidth(Alignment.CenterHorizontally)
                            .wrapContentHeight()
                            .heightIn(max = maxMenuHeight),
                    shape = menuShape,
                    color = shellColor,
                    shadowElevation = 10.dp,
                    tonalElevation = 0.dp,
                    border = BorderStroke(1.dp, MmrBrandColors.settingsSheetSwatchOutline()),
                ) {
                    Column(modifier = Modifier.widthIn(max = menuMaxWidth)) {
                        Surface(
                            onClick = {
                                onClearCategories()
                                expanded = false
                                pinnedChipInWindow = null
                            },
                            enabled = selected.isNotEmpty(),
                            color = headerBandColor,
                            shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .testTag(MmrTestTags.homeCategoryFilterClear),
                        ) {
                            Text(
                                text = "Clear Categories".mmrStaticLabelCase(),
                                modifier =
                                    Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 14.dp, vertical = 8.dp),
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.SemiBold,
                                color =
                                    if (selected.isNotEmpty()) {
                                        MmrBrandColors.dialogTitleText()
                                    } else {
                                        MmrBrandColors.dialogFooterDismissText()
                                    },
                            )
                        }
                        HorizontalDivider(color = dividerColor)
                        if (optionCount == 0) {
                            Text(
                                text = emptyCategoriesMessage,
                                modifier =
                                    Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = MmrDimens.space2xl),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MmrBrandColors.dialogMessageText(),
                            )
                        } else {
                            LazyColumn(
                                modifier = Modifier.heightIn(max = listMaxHeight),
                            ) {
                                items(
                                    items = rows,
                                    key = { row ->
                                        when (row) {
                                            is HomeCategoryFilterRow.Section -> "section-${row.title}"
                                            is HomeCategoryFilterRow.Choice -> "cat-${row.option.categoryId}"
                                        }
                                    },
                                ) { row ->
                                    when (row) {
                                        is HomeCategoryFilterRow.Section -> {
                                            Text(
                                                text = row.title.mmrStaticLabelCase(),
                                                modifier =
                                                    Modifier
                                                        .widthIn(max = menuMaxWidth)
                                                        .padding(start = 14.dp, end = 14.dp, top = 6.dp, bottom = 2.dp),
                                                style = MaterialTheme.typography.labelLarge,
                                                fontWeight = FontWeight.SemiBold,
                                                color = MmrBrandColors.modalSheetTitleText(),
                                            )
                                        }
                                        is HomeCategoryFilterRow.Choice -> {
                                            val option = row.option
                                            val checked = option.categoryId in selected
                                            val rowFill = categoryFilterRowFill(checked)
                                            Row(
                                                modifier =
                                                    Modifier
                                                        .widthIn(max = menuMaxWidth)
                                                        .background(rowFill)
                                                        .clickable { onToggleCategory(option.categoryId) }
                                                        .padding(start = 4.dp, end = 14.dp)
                                                        .testTag(
                                                            MmrTestTags.homeCategoryFilterOption(option.categoryId),
                                                        ),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.Start,
                                            ) {
                                                CompositionLocalProvider(
                                                    LocalMinimumInteractiveComponentSize provides 0.dp,
                                                ) {
                                                    Checkbox(
                                                        checked = checked,
                                                        onCheckedChange = null,
                                                        modifier = Modifier.size(22.dp),
                                                        colors =
                                                            CheckboxDefaults.colors(
                                                                checkedColor = MmrBrandColors.Active,
                                                                uncheckedColor = MmrBrandColors.dialogFieldMutedText(),
                                                            ),
                                                    )
                                                }
                                                Text(
                                                    text = option.label,
                                                    modifier = Modifier.weight(1f, fill = false),
                                                    style =
                                                        com.rrparallelstudio.miapp.ui.theme.mmrFormFieldStyle(
                                                            MmrBrandColors.dialogFieldValueText(),
                                                        ),
                                                    maxLines = 2,
                                                    overflow = TextOverflow.Ellipsis,
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
