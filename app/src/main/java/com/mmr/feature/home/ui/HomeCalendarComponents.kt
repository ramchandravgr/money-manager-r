package com.mmr.feature.home.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mmr.app.testing.MmrTestTags
import com.mmr.app.ui.components.MmrDimens
import com.mmr.app.ui.theme.MmrBrandColors
import com.mmr.feature.home.HomePeriodUtils
import com.mmr.feature.home.model.CalendarDayUi
import com.mmr.feature.home.model.TransactionGroupUi
import java.text.NumberFormat
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun HomeCalendarTabContent(
    cells: List<CalendarDayUi>,
    weekStart: DayOfWeek,
    selectedDate: LocalDate?,
    selectedDayGroup: TransactionGroupUi?,
    onDateSelected: (LocalDate) -> Unit,
    onPreviousDay: () -> Unit,
    onNextDay: () -> Unit,
    onAddForDay: () -> Unit,
    onTransactionClick: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = MmrDimens.fabListBottomInset),
    ) {
        item(key = "weekday-header") {
            CalendarWeekdayHeaderRow(weekStart = weekStart)
        }
        item(key = "month-grid") {
            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = MmrDimens.listItemPaddingHorizontal),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                cells.chunked(7).forEach { week ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(2.dp),
                    ) {
                        week.forEach { cell ->
                            CalendarDayCell(
                                cell = cell,
                                selected = cell.date == selectedDate,
                                onClick = { onDateSelected(cell.date) },
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                }
            }
        }
        if (selectedDate != null) {
            item(key = "day-nav") {
                CalendarDayNavigationBar(
                    date = selectedDate,
                    onPreviousDay = onPreviousDay,
                    onNextDay = onNextDay,
                    onAddForDay = onAddForDay,
                    modifier =
                        Modifier.padding(
                            top = MmrDimens.spaceMd,
                            start = MmrDimens.listItemPaddingHorizontal,
                            end = MmrDimens.listItemPaddingHorizontal,
                        ),
                )
            }
            if (selectedDayGroup != null) {
                item(key = "day-header") {
                    TransactionDayHeaderRow(
                        group = selectedDayGroup,
                        modifier = Modifier.padding(top = MmrDimens.spaceXs),
                    )
                }
                items(
                    count = selectedDayGroup.items.size,
                    key = { selectedDayGroup.items[it].id },
                ) { index ->
                    val txn = selectedDayGroup.items[index]
                    TransactionCompactRow(
                        item = txn,
                        onClick = { onTransactionClick(txn.id) },
                        onLongClick = { onTransactionClick(txn.id) },
                        isLastInDayGroup = index == selectedDayGroup.items.lastIndex,
                    )
                }
            } else {
                item(key = "day-empty") {
                    Text(
                        text = "No transactions this day.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier =
                            Modifier.padding(
                                horizontal = MmrDimens.listItemPaddingHorizontal,
                                vertical = MmrDimens.spaceLg,
                            ),
                    )
                }
            }
        }
    }
}

@Composable
private fun CalendarWeekdayHeaderRow(weekStart: DayOfWeek) {
    val locale = Locale.getDefault()
    val labels =
        (0 until 7).map { offset ->
            weekStart.plus(offset.toLong()).getDisplayName(TextStyle.SHORT, locale)
        }
    Row(
        modifier =
            Modifier
                .testTag(MmrTestTags.homeCalendarWeekdayHeader)
                .fillMaxWidth()
                .padding(
                    horizontal = MmrDimens.listItemPaddingHorizontal,
                    vertical = MmrDimens.spaceSm,
                ),
    ) {
        labels.forEach { label ->
            val color =
                when (label.lowercase(locale).take(3)) {
                    "sat" -> MmrBrandColors.CalendarSaturday
                    "sun" -> MmrBrandColors.CalendarSunday
                    else -> MmrBrandColors.TextMedium
                }
            Text(
                text = label,
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                color = color,
                fontWeight = FontWeight.Medium,
            )
        }
    }
}

@Composable
private fun CalendarDayCell(
    cell: CalendarDayUi,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val bg =
        when {
            selected -> Color.White
            cell.isCurrentMonth -> Color.White
            else -> MmrBrandColors.ListBackground.copy(alpha = 0.65f)
        }
    val borderColor = if (selected) MmrBrandColors.CalendarSelectedDay else MmrBrandColors.CalendarCellBorder
    val borderWidth = if (selected) 1.5.dp else 0.5.dp

    Box(
        modifier =
            modifier
                .aspectRatio(1f)
                .clip(RoundedCornerShape(4.dp))
                .background(bg)
                .border(borderWidth, borderColor, RoundedCornerShape(4.dp))
                .clickable(onClick = onClick)
                .padding(4.dp),
    ) {
        Box(
            modifier =
                Modifier
                    .align(Alignment.TopEnd)
                    .then(
                        if (selected) {
                            Modifier
                                .background(MmrBrandColors.CalendarSelectedDay, RoundedCornerShape(3.dp))
                                .padding(horizontal = 5.dp, vertical = 1.dp)
                        } else {
                            Modifier
                        },
                    ),
        ) {
            Text(
                text = cell.dayOfMonth.toString(),
                style = MaterialTheme.typography.labelMedium.copy(fontSize = 12.sp),
                fontWeight = FontWeight.SemiBold,
                color =
                    if (selected) {
                        Color.White
                    } else {
                        if (cell.isCurrentMonth) {
                            MaterialTheme.colorScheme.onSurface
                        } else {
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f)
                        }
                    },
            )
        }
        if (cell.expenseTotal > 0.0) {
            Text(
                text = formatCalendarAmount(cell.expenseTotal),
                modifier =
                    Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth(),
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, lineHeight = 11.sp),
                color = MmrBrandColors.CalendarExpense,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun CalendarDayNavigationBar(
    date: LocalDate,
    onPreviousDay: () -> Unit,
    onNextDay: () -> Unit,
    onAddForDay: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val title = HomePeriodUtils.formatShortDate(date)
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onPreviousDay) {
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Previous day")
        }
        Text(
            text = title,
            modifier = Modifier.weight(1f),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        IconButton(onClick = onNextDay) {
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Next day")
        }
        TextButton(onClick = onAddForDay) {
            Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.padding(end = 4.dp))
            Text("Add")
        }
    }
    HorizontalDivider(
        modifier = Modifier.padding(top = MmrDimens.spaceXs),
        color = MmrBrandColors.BorderMain.copy(alpha = 0.55f),
    )
}

private fun formatCalendarAmount(amount: Double): String {
    val formatter =
        NumberFormat.getNumberInstance(Locale.getDefault()).apply {
            minimumFractionDigits = 0
            maximumFractionDigits = 2
        }
    return formatter.format(amount)
}
