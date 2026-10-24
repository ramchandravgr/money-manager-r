package com.mmr.feature.approval.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.mmr.app.testing.MmrTestTags
import com.mmr.app.ui.components.MmrDimens
import com.mmr.app.ui.components.MmrFormCompactSearchField
import com.mmr.app.ui.components.MmrPeriodSwipeBox
import com.mmr.app.ui.state.MmrEmptyState
import com.mmr.app.ui.state.MmrLoadingState
import com.mmr.app.ui.theme.MmrBrandColors
import com.mmr.feature.approval.model.ApprovalCenterUiEvent
import com.mmr.feature.approval.model.PendingSmsMergePrompt
import com.mmr.feature.home.ui.GroupedTransactionList

@Composable
fun ApprovalLinkTransactionDialog(
    prompt: PendingSmsMergePrompt,
    onEvent: (ApprovalCenterUiEvent) -> Unit,
    onMergeInto: (Long) -> Unit,
    onApproveAsNew: () -> Unit,
    onDismiss: () -> Unit,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onNoteQueryChange: (String) -> Unit,
) {
    var expandedMergeCategory by remember { mutableStateOf(false) }

    val smsCount = prompt.approvalIds.size
    val title =
        when {
            smsCount > 1 -> "Link $smsCount SMS to one transaction"
            else -> "Link to existing transaction"
        }
    val periodKey = "${prompt.pickerYear}-${prompt.pickerMonth}-${prompt.noteQuery}"

    Dialog(
        onDismissRequest = onDismiss,
        properties =
            DialogProperties(
                usePlatformDefaultWidth = false,
                dismissOnBackPress = true,
                dismissOnClickOutside = false,
            ),
    ) {
        Surface(
            modifier =
                Modifier
                    .fillMaxWidth(0.96f)
                    .fillMaxHeight(0.92f),
            shape = RoundedCornerShape(16.dp),
            color = MmrBrandColors.ListBackground,
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .background(MmrBrandColors.Header)
                            .padding(horizontal = MmrDimens.spaceSm, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(MmrDimens.topBarIconButton),
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = "Close",
                            tint = MmrBrandColors.HeaderIcon,
                            modifier = Modifier.size(MmrDimens.iconMd),
                        )
                    }
                    Text(
                        text = title,
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MmrBrandColors.HeaderIcon,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (prompt.allowCreateNew) {
                        IconButton(
                            onClick = onApproveAsNew,
                            modifier = Modifier.size(MmrDimens.topBarIconButton),
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Add,
                                contentDescription = "Create new transaction",
                                tint = MmrBrandColors.Active,
                                modifier = Modifier.size(MmrDimens.iconMd),
                            )
                        }
                    } else {
                        IconButton(
                            onClick = {},
                            enabled = false,
                            modifier = Modifier.size(MmrDimens.topBarIconButton),
                        ) {}
                    }
                }

                Column(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .background(MmrBrandColors.SurfaceWhite)
                            .padding(horizontal = MmrDimens.spaceMd, vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Row(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(vertical = 0.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        IconButton(
                            onClick = onPreviousMonth,
                            modifier = Modifier.size(36.dp),
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                                contentDescription = "Previous month",
                                modifier = Modifier.size(22.dp),
                            )
                        }
                        Text(
                            text = prompt.monthTitle,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 4.dp),
                        )
                        IconButton(
                            onClick = onNextMonth,
                            modifier = Modifier.size(36.dp),
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                contentDescription = "Next month",
                                modifier = Modifier.size(22.dp),
                            )
                        }
                    }
                    MmrFormCompactSearchField(
                        value = prompt.noteQuery,
                        onValueChange = onNoteQueryChange,
                        placeholder = "Search note",
                        modifier = Modifier.testTag(MmrTestTags.approvalLinkPickerSearch),
                    )
                    Text(
                        text = "Search applies to ${prompt.monthTitle} only. Change month above to search another period.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = MmrDimens.spaceXs),
                    )
                    ApprovalLinkMergeCategoryField(
                        prompt = prompt,
                        expanded = expandedMergeCategory,
                        onExpandedChange = { expandedMergeCategory = it },
                        onEvent = onEvent,
                    )
                }

                HorizontalDivider(color = MmrBrandColors.BorderMain, thickness = 0.5.dp)

                MmrPeriodSwipeBox(
                    periodKey = periodKey,
                    enabled = !prompt.isLoadingCandidates,
                    onPrevious = onPreviousMonth,
                    onNext = onNextMonth,
                    modifier =
                        Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                ) {
                    when {
                        prompt.isLoadingCandidates ->
                            MmrLoadingState(modifier = Modifier.fillMaxSize())
                        prompt.isEmptyMonth ->
                            MmrEmptyState(
                                message =
                                    if (prompt.noteQuery.isBlank()) {
                                        "No transactions this month."
                                    } else {
                                        "No transactions match your note search this month."
                                    },
                                modifier = Modifier.fillMaxSize(),
                            )
                        else ->
                            GroupedTransactionList(
                                groups = prompt.groupedTransactions,
                                onItemClick = onMergeInto,
                                modifier = Modifier.fillMaxSize(),
                                contentBottomPadding = MmrDimens.spaceLg,
                            )
                    }
                }
            }
        }
    }
}
