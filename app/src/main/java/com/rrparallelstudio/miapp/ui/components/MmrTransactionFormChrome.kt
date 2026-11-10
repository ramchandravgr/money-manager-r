package com.rrparallelstudio.miapp.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.CallMade
import androidx.compose.material.icons.outlined.AccountBalance
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.Category
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Numbers
import androidx.compose.material.icons.outlined.Percent
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rrparallelstudio.miapp.core.model.enums.TransactionType
import com.rrparallelstudio.miapp.ui.copy.mmrStaticLabelCase
import com.rrparallelstudio.miapp.ui.theme.MmrBrandColors
import com.rrparallelstudio.miapp.ui.theme.MmrChipPaints

private val compactSearchShape = RoundedCornerShape(50)

/** Compact pill search used inside expandable transaction pickers. */
@Composable
fun MmrFormCompactSearchField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    /**
     * When true, the field sits on a **white** [MmrTransactionFormCard] while the scene uses a **dark canvas**
     * (Ocean / Midnight / Coral in light appearance, or any theme in dark mode). Use canvas-contrast chrome there
     * and the pill becomes unreadable; this path keeps dark text and a tinted fill so it stays visible on the card.
     */
    onLightFormCard: Boolean = false,
) {
    val useDarkCanvasChrome = MmrBrandColors.canvasIsDark() && !onLightFormCard
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        singleLine = true,
        textStyle =
            TextStyle(
                fontSize = 14.sp,
                color =
                    when {
                        useDarkCanvasChrome -> MmrBrandColors.onCanvas()
                        onLightFormCard -> MmrBrandColors.formCardPrimaryText()
                        else -> MmrBrandColors.Reversal
                    },
            ),
        modifier =
            modifier
                .fillMaxWidth()
                .padding(horizontal = MmrDimens.spaceLg, vertical = 4.dp)
                .clip(compactSearchShape)
                .border(
                    1.dp,
                    if (useDarkCanvasChrome) {
                        MmrBrandColors.onCanvasMuted().copy(alpha = 0.35f)
                    } else {
                        MmrBrandColors.BorderMain.copy(alpha = 0.85f)
                    },
                    compactSearchShape,
                ).background(
                    if (useDarkCanvasChrome) {
                        Color.White.copy(alpha = 0.08f)
                    } else if (onLightFormCard) {
                        MmrBrandColors.SettingsSectionHeaderFill
                    } else {
                        MmrBrandColors.SurfaceWhite
                    },
                ).padding(horizontal = 14.dp, vertical = 9.dp),
        decorationBox = { inner ->
            if (value.isEmpty()) {
                Text(
                    text = placeholder,
                    style = MaterialTheme.typography.bodyMedium,
                    color =
                        if (useDarkCanvasChrome) {
                            MmrBrandColors.onCanvasMuted()
                        } else {
                            MmrBrandColors.TextMedium
                        },
                )
            }
            inner()
        },
    )
}

private val typePillShape = RoundedCornerShape(12.dp)
private val chipShape = RoundedCornerShape(10.dp)

/** Standard leading icons for transaction / recurring / loan entry rows. */
object MmrTransactionFormIcons {
    val date: ImageVector = Icons.Outlined.CalendarToday
    val category: ImageVector = Icons.Outlined.Category
    val account: ImageVector = Icons.Outlined.AccountBalanceWallet
    val lender: ImageVector = Icons.Outlined.AccountBalance
    val interestRate: ImageVector = Icons.Outlined.Percent
    val transferFrom: ImageVector = account
    val transferTo: ImageVector = Icons.AutoMirrored.Outlined.CallMade
    val reminder: ImageVector = Icons.Outlined.Notifications

    /** Currency-neutral (user may use INR, USD, etc.). */
    val amount: ImageVector = Icons.Outlined.Numbers
    val name: ImageVector = Icons.Outlined.Edit
    val note: ImageVector = Icons.Outlined.Edit
    val attachPhoto: ImageVector = Icons.Outlined.PhotoCamera
}

/** Light screen background for transaction / recurring entry (floating white field cards). */
@Composable
fun MmrTransactionFormScreenBackground(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Box(modifier = modifier.background(MmrBrandColors.ListAreaBackground)) {
        content()
    }
}

/** Single white elevated card wrapping one or more form controls. */
@Composable
fun MmrTransactionFormCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    MmrGroupedFormCard(modifier = modifier, content = content)
}

/** Small caption above a card group (e.g. "Frequency"). */
@Composable
fun MmrTransactionFormSectionLabel(
    text: String,
    modifier: Modifier = Modifier,
) {
    MmrFormSectionLabel(text = text, modifier = modifier)
}

/**
 * Income / Expense / Transfer pill row (reference finance-app entry style).
 * [testTagForType] supplies UI-test tags per type (transaction vs recurring).
 */
@Composable
fun MmrTransactionTypeSegment(
    selected: TransactionType,
    onTypeSelected: (TransactionType) -> Unit,
    modifier: Modifier = Modifier,
    testTagForType: (TransactionType) -> String? = { null },
) {
    val types = listOf(TransactionType.INCOME, TransactionType.EXPENSE, TransactionType.TRANSFER)
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .clip(typePillShape)
                .background(MmrBrandColors.SurfaceWhite)
                .border(1.dp, MmrBrandColors.TransactionListBorder, typePillShape)
                .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        types.forEach { type ->
            val selectedType = selected == type
            val label =
                when (type) {
                    TransactionType.INCOME -> "Income"
                    TransactionType.EXPENSE -> "Expense"
                    TransactionType.TRANSFER -> "Transfer"
                    else -> type.name
                }
            Box(
                modifier =
                    Modifier
                        .weight(1f)
                        .defaultMinSize(minHeight = 40.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(
                            if (selectedType) {
                                MmrBrandColors.BorderMain.copy(alpha = 0.35f)
                            } else {
                                MmrBrandColors.SurfaceWhite
                            },
                        ).then(
                            testTagForType(type)?.let { tag ->
                                Modifier.testTag(tag)
                            } ?: Modifier,
                        ).clickable { onTypeSelected(type) },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = label,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 10.dp),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = if (selectedType) FontWeight.SemiBold else FontWeight.Medium,
                    color = MmrBrandColors.formCardPrimaryText(),
                )
            }
        }
    }
}

fun isTransactionFormPickerPlaceholder(
    value: String,
    emptyHint: String,
): Boolean = value.isBlank() || value == emptyHint || value.startsWith("Select", ignoreCase = true)

/**
 * Picker row that expands downward in-place (search + options in [expandedContent]).
 */
@Composable
fun MmrTransactionFormExpandablePicker(
    label: String,
    value: String,
    emptyHint: String,
    icon: ImageVector,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    expandedContent: @Composable ColumnScope.() -> Unit,
) {
    if (!enabled) {
        MmrTransactionFormPickerRow(
            icon = icon,
            placeholder = label,
            value = value.ifBlank { emptyHint },
            emptyHint = emptyHint,
            onClick = null,
            modifier = modifier,
            enabled = false,
        )
        return
    }
    val isPlaceholder = isTransactionFormPickerPlaceholder(value, emptyHint)
    val displayText = if (isPlaceholder) emptyHint.mmrStaticLabelCase() else value
    MmrTransactionFormCard(modifier = modifier) {
        Column {
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .clickable { onExpandedChange(!expanded) }
                        .padding(horizontal = MmrDimens.spaceLg, vertical = MmrDimens.spaceLg),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(MmrDimens.spaceMd),
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(MmrDimens.iconMd),
                    tint = MmrBrandColors.TextMedium,
                )
                Text(
                    text = displayText,
                    style = MaterialTheme.typography.bodyLarge,
                    color =
                        if (isPlaceholder) {
                            MmrBrandColors.TextMedium
                        } else {
                            MmrBrandColors.formCardPrimaryText()
                        },
                    modifier = Modifier.weight(1f),
                )
                Icon(
                    imageVector =
                        if (expanded) {
                            Icons.Outlined.KeyboardArrowUp
                        } else {
                            Icons.Outlined.KeyboardArrowDown
                        },
                    contentDescription = null,
                    tint = MmrBrandColors.TextMedium,
                )
            }
            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically(),
                exit = shrinkVertically(),
            ) {
                Column {
                    HorizontalDivider(color = MmrBrandColors.BorderMain.copy(alpha = 0.5f))
                    expandedContent()
                }
            }
        }
    }
}

/**
 * Tappable row with caption label + value (expense entry category / account / transfer).
 */
@Composable
fun MmrTransactionFormPickerRowLabeled(
    label: String,
    value: String,
    emptyHint: String,
    icon: ImageVector,
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val interactive = enabled && onClick != null
    val isPlaceholder = isTransactionFormPickerPlaceholder(value, emptyHint)
    val displayValue = if (isPlaceholder) emptyHint.mmrStaticLabelCase() else value
    MmrTransactionFormCard(
        modifier =
            modifier.then(
                if (interactive) {
                    Modifier.clickable(onClick = onClick!!)
                } else {
                    Modifier
                },
            ),
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = MmrDimens.spaceLg, vertical = MmrDimens.spaceMd),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(MmrDimens.spaceMd),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(MmrDimens.iconMd),
                tint = MmrBrandColors.TextMedium,
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = label.mmrStaticLabelCase(),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MmrBrandColors.TextMedium,
                )
                Text(
                    text = displayValue,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = if (isPlaceholder) FontWeight.Normal else FontWeight.SemiBold,
                    color =
                        if (isPlaceholder) {
                            MmrBrandColors.TextMedium
                        } else {
                            MmrBrandColors.formCardPrimaryText()
                        },
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
            if (interactive) {
                Icon(
                    Icons.Outlined.KeyboardArrowDown,
                    contentDescription = null,
                    tint = MmrBrandColors.TextMedium,
                )
            }
        }
    }
}

/**
 * Tappable row: leading icon, value or placeholder, trailing chevron.
 */
@Composable
fun MmrTransactionFormPickerRow(
    icon: ImageVector?,
    placeholder: String,
    value: String,
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    emptyHint: String = placeholder,
) {
    val interactive = enabled && onClick != null
    val isPlaceholder = isTransactionFormPickerPlaceholder(value, emptyHint)
    val displayText =
        if (!isPlaceholder) {
            value
        } else if (placeholder.isNotBlank()) {
            placeholder.mmrStaticLabelCase()
        } else {
            emptyHint.mmrStaticLabelCase()
        }
    MmrTransactionFormCard(
        modifier =
            modifier.then(
                if (interactive) {
                    Modifier.clickable(onClick = onClick!!)
                } else {
                    Modifier
                },
            ),
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = MmrDimens.spaceLg, vertical = MmrDimens.spaceLg),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(MmrDimens.spaceMd),
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(MmrDimens.iconMd),
                    tint = MmrBrandColors.TextMedium,
                )
            }
            Text(
                text = displayText,
                style = MaterialTheme.typography.bodyLarge,
                color =
                    if (isPlaceholder) {
                        MmrBrandColors.TextMedium
                    } else {
                        MmrBrandColors.formCardPrimaryText()
                    },
                modifier = Modifier.weight(1f),
            )
            if (interactive) {
                Icon(
                    Icons.Outlined.KeyboardArrowDown,
                    contentDescription = null,
                    tint = MmrBrandColors.TextMedium,
                )
            }
        }
    }
}

/**
 * Single-line or multi-line text in a card with optional leading icon and trailing action (e.g. camera).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MmrTransactionFormTextRow(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    leadingIcon: ImageVector? = null,
    testTag: String? = null,
    keyboardType: KeyboardType = KeyboardType.Text,
    singleLine: Boolean = true,
    minLines: Int = 1,
    trailingContent: (@Composable () -> Unit)? = null,
    groupedAmountDisplay: Boolean = false,
    amountCalculatorEnabled: Boolean = groupedAmountDisplay,
) {
    var calculatorOpen by remember { mutableStateOf(false) }
    val amountTransformation =
        if (groupedAmountDisplay) {
            remember { MmrLocaleAmountVisualTransformation() }
        } else {
            null
        }
    MmrTransactionFormCard(modifier = modifier) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(
                        start = if (leadingIcon != null) MmrDimens.spaceLg else MmrDimens.spaceXl,
                        end = MmrDimens.spaceSm,
                        top = MmrDimens.spaceLg,
                        bottom = MmrDimens.spaceLg,
                    ),
            verticalAlignment = if (singleLine) Alignment.CenterVertically else Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(MmrDimens.spaceMd),
        ) {
            if (leadingIcon != null) {
                Icon(
                    imageVector = leadingIcon,
                    contentDescription = null,
                    modifier = Modifier.size(MmrDimens.iconMd),
                    tint = MmrBrandColors.TextMedium,
                )
            }
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                modifier =
                    Modifier
                        .weight(1f)
                        .then(if (testTag != null) Modifier.testTag(testTag) else Modifier),
                textStyle =
                    TextStyle(
                        fontSize = MaterialTheme.typography.bodyLarge.fontSize,
                        color = MmrBrandColors.formCardPrimaryText(),
                    ),
                keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
                visualTransformation =
                    if (groupedAmountDisplay && value.isNotEmpty() && amountTransformation != null) {
                        amountTransformation
                    } else {
                        VisualTransformation.None
                    },
                singleLine = singleLine,
                minLines = minLines,
                decorationBox = { inner ->
                    if (value.isEmpty()) {
                        Text(
                            text = placeholder.mmrStaticLabelCase(),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MmrBrandColors.TextMedium,
                        )
                    }
                    inner()
                },
            )
            if (amountCalculatorEnabled) {
                MmrAmountCalculatorIconButton(onClick = { calculatorOpen = true })
            }
            trailingContent?.invoke()
        }
    }
    if (calculatorOpen) {
        MmrAmountCalculatorSheet(
            initialAmountFieldValue = value,
            onDismiss = { calculatorOpen = false },
            onApply = onValueChange,
        )
    }
}

@Composable
fun MmrTransactionFormAttachIconButton(
    onClick: () -> Unit,
    enabled: Boolean = true,
    contentDescription: String = "Add photo",
) {
    IconButton(onClick = onClick, enabled = enabled) {
        Icon(
            imageVector = MmrTransactionFormIcons.attachPhoto,
            contentDescription = contentDescription,
            tint = if (enabled) MmrBrandColors.Active else MmrBrandColors.TextLight,
        )
    }
}

data class MmrTransactionFormChipOption<T>(
    val value: T,
    val label: String,
)

/** Single selectable chip (e.g. liability account in a [FlowRow]). */
@Composable
fun MmrTransactionFormFilterChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    /**
     * Use on white [MmrGroupedFormCard] / [MmrTransactionFormCard] when the palette may be dark-appearance
     * (unselected label must not use [MmrBrandColors.Reversal]).
     */
    onWhiteGroupedCard: Boolean = false,
) {
    val paint =
        if (onWhiteGroupedCard) {
            MmrChipPaints.accentOnWhiteGroupedCard(selected)
        } else {
            MmrChipPaints.accentOnCard(selected)
        }
    val fill = paint.fill
    val borderColor = paint.border
    val labelColor = paint.content
    Box(
        modifier =
            modifier
                .clip(chipShape)
                .background(fill)
                .border(width = 1.dp, color = borderColor, shape = chipShape)
                .clickable(onClick = onClick)
                .padding(vertical = 10.dp, horizontal = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label.mmrStaticLabelCase(),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            color = labelColor,
        )
    }
}

/** Horizontal choice chips (e.g. Daily / Weekly / Monthly / Yearly). */
@Composable
fun <T> MmrTransactionFormChoiceChips(
    options: List<MmrTransactionFormChipOption<T>>,
    selected: T,
    onSelected: (T) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        options.forEach { option ->
            val isSelected = option.value == selected
            val fill = if (isSelected) MmrBrandColors.Active else MmrBrandColors.SurfaceWhite
            val borderColor =
                if (isSelected) {
                    MmrBrandColors.Active
                } else {
                    MmrBrandColors.TransactionListBorder
                }
            val labelColor =
                if (isSelected) {
                    MmrBrandColors.SurfaceWhite
                } else {
                    MmrBrandColors.formCardPrimaryText()
                }
            Box(
                modifier =
                    Modifier
                        .weight(1f)
                        .clip(chipShape)
                        .background(fill)
                        .border(width = 1.dp, color = borderColor, shape = chipShape)
                        .clickable { onSelected(option.value) }
                        .padding(vertical = 10.dp, horizontal = 4.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = option.label.mmrStaticLabelCase(),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                    color = labelColor,
                )
            }
        }
    }
}
