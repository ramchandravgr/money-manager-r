package com.rrparallelstudio.miapp.ui.theme

import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.TimePickerDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/**
 * [DatePickerDialog] + [DatePicker] colors for **expressive dark canvases** (Ocean / Midnight in light
 * appearance): Material date pickers otherwise keep theme "light" colors and read poorly on the modal band.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun mmrDatePickerColors() =
    if (MmrBrandColors.canvasIsDark()) {
        val surface = MmrBrandColors.modalChromeSurface()
        val on = MmrBrandColors.onCanvas()
        val muted = MmrBrandColors.onCanvasMuted()
        DatePickerDefaults.colors(
            containerColor = surface,
            titleContentColor = on,
            headlineContentColor = on,
            weekdayContentColor = muted,
            subheadContentColor = muted,
            navigationContentColor = on,
            yearContentColor = on,
            disabledYearContentColor = muted.copy(alpha = muted.alpha * 0.55f),
            currentYearContentColor = MmrBrandColors.Active,
            selectedYearContentColor = Color.White,
            disabledSelectedYearContentColor = muted,
            selectedYearContainerColor = MmrBrandColors.Active,
            disabledSelectedYearContainerColor = muted.copy(alpha = 0.35f),
            dayContentColor = on,
            disabledDayContentColor = muted.copy(alpha = muted.alpha * 0.45f),
            selectedDayContentColor = Color.White,
            disabledSelectedDayContentColor = muted,
            selectedDayContainerColor = MmrBrandColors.Active,
            disabledSelectedDayContainerColor = muted.copy(alpha = 0.35f),
            todayContentColor = MmrBrandColors.Active,
            todayDateBorderColor = MmrBrandColors.Active,
            dividerColor = MmrBrandColors.BorderMain,
            dateTextFieldColors =
                OutlinedTextFieldDefaults.colors(
                    focusedTextColor = on,
                    unfocusedTextColor = on,
                    disabledTextColor = muted,
                    focusedBorderColor = MmrBrandColors.Active,
                    unfocusedBorderColor = MmrBrandColors.BorderMain,
                    cursorColor = MmrBrandColors.Active,
                    focusedLabelColor = muted,
                    unfocusedLabelColor = muted,
                ),
        )
    } else {
        DatePickerDefaults.colors()
    }

/** Label / icon tint for [androidx.compose.material3.TextButton] actions on date picker dialogs. */
@Composable
fun mmrDatePickerDialogActionContentColor(): Color =
    if (MmrBrandColors.canvasIsDark()) {
        MmrBrandColors.Active
    } else {
        MaterialTheme.colorScheme.primary
    }

/** [TimePicker] colors when embedded in [MmrContentDialog] on a dark expressive canvas. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun mmrTimePickerColors() =
    if (MmrBrandColors.canvasIsDark()) {
        val dial = MmrExpressiveDarkCanvasChrome.chipFill
        val on = MmrBrandColors.onCanvas()
        val muted = MmrBrandColors.onCanvasMuted()
        TimePickerDefaults.colors(
            clockDialColor = dial,
            clockDialSelectedContentColor = Color.White,
            clockDialUnselectedContentColor = muted,
            selectorColor = MmrBrandColors.Active,
            containerColor = Color.Transparent,
            periodSelectorBorderColor = MmrBrandColors.BorderMain,
            periodSelectorSelectedContainerColor = MmrBrandColors.Active,
            periodSelectorUnselectedContainerColor = MmrExpressiveDarkCanvasChrome.chipFill,
            periodSelectorSelectedContentColor = Color.White,
            periodSelectorUnselectedContentColor = on,
            timeSelectorSelectedContainerColor = MmrBrandColors.Active,
            timeSelectorUnselectedContainerColor = MmrExpressiveDarkCanvasChrome.chipFill,
            timeSelectorSelectedContentColor = Color.White,
            timeSelectorUnselectedContentColor = on,
        )
    } else {
        TimePickerDefaults.colors()
    }
