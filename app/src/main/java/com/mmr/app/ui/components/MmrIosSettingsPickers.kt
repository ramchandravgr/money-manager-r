package com.mmr.app.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mmr.app.ui.theme.MmrBrandColors
import com.mmr.core.backup.BackupImportMode

/**
 * iOS Settings–style single-choice picker as a **bottom sheet** (not centered dialog).
 * Use from nav rows on inset-grouped settings screens.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MmrIosSettingsSingleChoiceSheet(
    title: String,
    onDismiss: () -> Unit,
    sheetState: SheetState,
    modifier: Modifier = Modifier,
    message: String? = null,
    confirmLabel: String? = null,
    onConfirm: (() -> Unit)? = null,
    confirmEnabled: Boolean = true,
    confirmTestTag: String? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        modifier = modifier,
        containerColor = MmrBrandColors.ListBackground,
        dragHandle = { BottomSheetDefaults.DragHandle() },
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(
                        start = MmrDimens.iosSettingsHorizontalInset,
                        end = MmrDimens.iosSettingsHorizontalInset,
                        bottom = MmrDimens.spaceLg,
                    ),
        ) {
            Text(
                text = title,
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold,
                color = MmrBrandColors.Reversal,
                modifier = Modifier.padding(bottom = if (message != null) 6.dp else 12.dp),
            )
            message?.let {
                MmrHelperText(
                    text = it,
                    modifier = Modifier.padding(bottom = 12.dp),
                )
            }
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(MmrDimens.iosSettingsGroupCorner),
                color = MmrBrandColors.SurfaceWhite,
            ) {
                Column(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .heightIn(max = 420.dp)
                            .verticalScroll(rememberScrollState()),
                    content = content,
                )
            }
            if (confirmLabel != null && onConfirm != null) {
                MmrPrimaryActionButton(
                    text = confirmLabel,
                    onClick = onConfirm,
                    enabled = confirmEnabled,
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp)
                            .then(if (confirmTestTag != null) Modifier.testTag(confirmTestTag) else Modifier),
                )
            }
        }
    }
}

/** Replace vs merge backup restore/import (settings flows). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MmrIosSettingsBackupImportModeSheet(
    title: String,
    fileLabel: String,
    selectedMode: BackupImportMode,
    onSelectMode: (BackupImportMode) -> Unit,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    sheetState: SheetState,
    confirmLabel: String,
    confirmTestTag: String? = null,
    replaceModeTestTag: String? = null,
    mergeModeTestTag: String? = null,
) {
    val helper =
        when (selectedMode) {
            BackupImportMode.REPLACE -> "Replace removes current data on this device and restores the backup."
            BackupImportMode.MERGE ->
                "Merge adds records from the backup that are not already on this device (same IDs are skipped)."
        }
    MmrIosSettingsSingleChoiceSheet(
        title = title,
        onDismiss = onDismiss,
        sheetState = sheetState,
        message = "File: $fileLabel\n\n$helper",
        confirmLabel = confirmLabel,
        onConfirm = onConfirm,
        confirmTestTag = confirmTestTag,
    ) {
        MmrIosSettingsChoiceRow(
            label = "Replace device data",
            selected = selectedMode == BackupImportMode.REPLACE,
            onClick = { onSelectMode(BackupImportMode.REPLACE) },
            testTag = replaceModeTestTag,
        )
        MmrIosSettingsChoiceRow(
            label = "Merge with existing data",
            selected = selectedMode == BackupImportMode.MERGE,
            onClick = { onSelectMode(BackupImportMode.MERGE) },
            showDividerBelow = false,
            testTag = mergeModeTestTag,
        )
    }
}

/** Convenience overload for enum-like option lists. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <T> MmrIosSettingsSingleChoiceSheet(
    title: String,
    onDismiss: () -> Unit,
    sheetState: SheetState,
    options: List<T>,
    selectedOption: T,
    onOptionSelected: (T) -> Unit,
    optionLabel: (T) -> String,
    message: String? = null,
    optionEquals: (T, T) -> Boolean = { a, b -> a == b },
) {
    MmrIosSettingsSingleChoiceSheet(
        title = title,
        onDismiss = onDismiss,
        sheetState = sheetState,
        message = message,
    ) {
        options.forEachIndexed { index, option ->
            MmrIosSettingsChoiceRow(
                label = optionLabel(option),
                selected = optionEquals(option, selectedOption),
                onClick = {
                    onOptionSelected(option)
                    onDismiss()
                },
                showDividerBelow = index < options.lastIndex,
            )
        }
    }
}

/** Checkmark row inside [MmrIosSettingsSingleChoiceSheet] grouped list. */
@Composable
fun MmrIosSettingsChoiceRow(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    showDividerBelow: Boolean = true,
    testTag: String? = null,
    labelContent: (@Composable () -> Unit)? = null,
) {
    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .then(if (testTag != null) Modifier.testTag(testTag) else Modifier)
                .clickable(onClick = onClick),
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = MmrDimens.iosSettingsRowHorizontalPadding,
                        vertical = 11.dp,
                    ),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (labelContent != null) {
                Box(modifier = Modifier.weight(1f)) {
                    labelContent()
                }
            } else {
                Text(
                    text = label,
                    modifier = Modifier.weight(1f),
                    color = MmrBrandColors.Reversal,
                    fontSize = 16.sp,
                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (selected) {
                Icon(
                    imageVector = Icons.Filled.Check,
                    contentDescription = null,
                    tint = MmrBrandColors.Active,
                )
            }
        }
        if (showDividerBelow) {
            MmrIosSettingsRowDivider()
        }
    }
}
