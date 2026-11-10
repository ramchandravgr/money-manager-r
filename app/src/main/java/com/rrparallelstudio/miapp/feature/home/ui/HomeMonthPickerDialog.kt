package com.rrparallelstudio.miapp.feature.home.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.rrparallelstudio.miapp.R
import com.rrparallelstudio.miapp.feature.home.MmrLedgerYearBounds
import com.rrparallelstudio.miapp.ui.components.MmrSnapWheelColumn
import com.rrparallelstudio.miapp.ui.theme.MmrBrandColors
import java.time.Month
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeMonthPickerDialog(
    visible: Boolean,
    selectedYear: Int,
    selectedMonth: Int,
    onDismiss: () -> Unit,
    onPeriodSelected: (year: Int, month: Int) -> Unit,
    onThisMonth: () -> Unit,
) {
    if (!visible) return

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val now = remember { YearMonth.now() }
    val yearRange = remember { MmrLedgerYearBounds.selectableYears(now.year) }
    val yearLabels = remember { yearRange.map { it.toString() } }
    val monthLabels =
        remember {
            Month.entries.map { month ->
                month.getDisplayName(TextStyle.FULL_STANDALONE, Locale.getDefault())
            }
        }

    var draftYear by remember { mutableIntStateOf(selectedYear) }
    var draftMonth by remember { mutableIntStateOf(selectedMonth) }

    LaunchedEffect(visible, selectedYear, selectedMonth) {
        if (visible) {
            draftYear = selectedYear
            draftMonth = selectedMonth.coerceIn(1, 12)
        }
    }

    val yearIndex =
        remember(draftYear, yearLabels) {
            yearLabels.indexOf(draftYear.toString()).coerceAtLeast(0)
        }
    val monthIndex = (draftMonth - 1).coerceIn(0, 11)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MmrBrandColors.modalChromeSurface(),
        dragHandle = { BottomSheetDefaults.DragHandle() },
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 24.dp),
        ) {
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = stringResource(R.string.home_month_picker_title),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    color =
                        if (MmrBrandColors.canvasIsDark()) {
                            MmrBrandColors.onCanvas()
                        } else {
                            MmrBrandColors.TextStrong
                        },
                )
                TextButton(onClick = onThisMonth) {
                    Text(
                        text = stringResource(R.string.home_month_picker_this_month),
                        fontWeight = FontWeight.Bold,
                        color = MmrBrandColors.Active,
                    )
                }
            }

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = MmrBrandColors.settingsSheetGroupedListFill(),
            ) {
                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp, horizontal = 12.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                ) {
                    MmrSnapWheelColumn(
                        items = monthLabels,
                        selectedIndex = monthIndex,
                        onSelectedIndexChange = { draftMonth = it + 1 },
                        modifier = Modifier.weight(1f),
                    )
                    MmrSnapWheelColumn(
                        items = yearLabels,
                        selectedIndex = yearIndex,
                        onSelectedIndexChange = { index ->
                            draftYear = yearLabels[index].toInt()
                        },
                        modifier = Modifier.weight(1f),
                    )
                }
            }

            Button(
                onClick = { onPeriodSelected(draftYear, draftMonth) },
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp),
                shape = RoundedCornerShape(12.dp),
            ) {
                Text(
                    text = stringResource(R.string.home_month_picker_done),
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }
}
