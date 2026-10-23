package com.mmr.feature.search.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import com.mmr.app.ui.components.MmrDimens
import com.mmr.app.ui.theme.MmrMmColors
import com.mmr.feature.home.HomePeriodUtils
import com.mmr.feature.search.SearchPeriodPresets
import java.time.LocalDate
import java.time.Month
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun SearchDateRangeDialog(
    initialStart: LocalDate?,
    initialEnd: LocalDate?,
    onConfirm: (LocalDate, LocalDate) -> Unit,
    onDismiss: () -> Unit,
) {
    val today = remember { LocalDate.now() }
    val defaultRange =
        remember {
            SearchPeriodPresets.rangeFor(SearchPeriodPresets.QuickPick.LAST_30_DAYS, today)
        }
    val initialFrom = initialStart ?: defaultRange.start
    val initialTo = initialEnd ?: defaultRange.end

    var startDate by remember { mutableStateOf(initialFrom) }
    var endDate by remember { mutableStateOf(initialTo) }
    var selectedQuick by remember { mutableStateOf<SearchPeriodPresets.QuickPick?>(null) }

    fun applyRange(
        range: SearchPeriodPresets.Range,
        quick: SearchPeriodPresets.QuickPick?,
    ) {
        startDate = range.start
        endDate = range.end
        selectedQuick = quick
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
        modifier =
            Modifier
                .widthIn(max = 400.dp)
                .padding(horizontal = MmrDimens.spaceLg),
        title = {
            Text("Date range", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        },
        text = {
            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(MmrDimens.spaceMd),
            ) {
                Text(
                    text = "Quick select",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Medium,
                    color = MmrMmColors.TextStrong,
                )
                val quickPicks = SearchPeriodPresets.QuickPick.entries
                Column(verticalArrangement = Arrangement.spacedBy(MmrDimens.spaceSm)) {
                    quickPicks.chunked(2).forEach { rowPicks ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(MmrDimens.spaceSm),
                        ) {
                            rowPicks.forEach { pick ->
                                SearchQuickPickOption(
                                    label = pick.label,
                                    selected = selectedQuick == pick,
                                    onClick = {
                                        applyRange(SearchPeriodPresets.rangeFor(pick, today), pick)
                                    },
                                    modifier = Modifier.weight(1f),
                                )
                            }
                            if (rowPicks.size == 1) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }

                HorizontalDivider(color = MmrMmColors.BorderMain, thickness = 0.5.dp)

                CompactDateSelector(
                    label = "From",
                    date = startDate,
                    onDateChange = {
                        startDate = it
                        selectedQuick = null
                    },
                )
                CompactDateSelector(
                    label = "To",
                    date = endDate,
                    onDateChange = {
                        endDate = it
                        selectedQuick = null
                    },
                )

                val preview = SearchPeriodPresets.normalizeRange(startDate, endDate)
                Text(
                    text = "${HomePeriodUtils.formatShortDate(
                        preview.start,
                    )} — ${HomePeriodUtils.formatShortDate(preview.end)}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MmrMmColors.Active,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,
                )
                Text(
                    text = "FY = 1 Apr – 31 Mar",
                    style = MaterialTheme.typography.labelSmall,
                    color = MmrMmColors.TextMedium,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val normalized = SearchPeriodPresets.normalizeRange(startDate, endDate)
                    onConfirm(normalized.start, normalized.end)
                },
            ) {
                Text("Apply", color = MmrMmColors.Active)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = MmrMmColors.TextMedium)
            }
        },
    )
}

@Composable
private fun SearchQuickPickOption(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(8.dp)
    val background =
        if (selected) {
            MmrMmColors.Active.copy(alpha = 0.12f)
        } else {
            MmrMmColors.SurfaceWhite
        }
    val borderColor = if (selected) MmrMmColors.Active else MmrMmColors.BorderMain
    val textColor = if (selected) MmrMmColors.Active else MmrMmColors.TextStrong

    Text(
        text = label,
        modifier =
            modifier
                .clip(shape)
                .border(1.dp, borderColor, shape)
                .background(background)
                .clickable(onClick = onClick)
                .padding(vertical = 12.dp, horizontal = MmrDimens.spaceSm),
        color = textColor,
        style = MaterialTheme.typography.labelLarge,
        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
        textAlign = TextAlign.Center,
        maxLines = 1,
    )
}

@Composable
private fun CompactDateSelector(
    label: String,
    date: LocalDate,
    onDateChange: (LocalDate) -> Unit,
) {
    val todayYear = remember { LocalDate.now().year }
    val years = remember { (todayYear - 20..todayYear).toList().reversed() }
    val months = remember { Month.entries }

    var year by remember(date) { mutableIntStateOf(date.year) }
    var month by remember(date) { mutableIntStateOf(date.monthValue) }
    var day by remember(date) { mutableIntStateOf(date.dayOfMonth) }

    fun emit() {
        onDateChange(SearchPeriodPresets.clampDay(year, month, day))
    }

    Column(verticalArrangement = Arrangement.spacedBy(MmrDimens.spaceXs)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Medium,
            color = MmrMmColors.TextStrong,
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(MmrDimens.spaceSm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            MmrCompactDropdown(
                modifier = Modifier.weight(1f),
                label = "Year",
                options = years.map { it.toString() },
                selectedIndex = years.indexOf(year).coerceAtLeast(0),
                onSelected = { index ->
                    year = years[index]
                    day = day.coerceAtMost(LocalDate.of(year, month, 1).lengthOfMonth())
                    emit()
                },
            )
            MmrCompactDropdown(
                modifier = Modifier.weight(1f),
                label = "Month",
                options =
                    months.map {
                        it.getDisplayName(TextStyle.SHORT, Locale.getDefault())
                    },
                selectedIndex = month - 1,
                onSelected = { index ->
                    month = index + 1
                    day = day.coerceAtMost(LocalDate.of(year, month, 1).lengthOfMonth())
                    emit()
                },
            )
            MmrCompactDropdown(
                modifier = Modifier.weight(1f),
                label = "Day",
                options = (1..LocalDate.of(year, month, 1).lengthOfMonth()).map { it.toString() },
                selectedIndex = (day - 1).coerceIn(0, LocalDate.of(year, month, 1).lengthOfMonth() - 1),
                onSelected = { index ->
                    day = index + 1
                    emit()
                },
            )
        }
    }
}
