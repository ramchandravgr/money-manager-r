package com.mmr.feature.approval.ui

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.CallMerge
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.SelectAll
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material3.*
import androidx.compose.material3.Checkbox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.mmr.app.R
import com.mmr.app.testing.MmrTestTags
import com.mmr.app.ui.adaptive.MmrAdaptiveWidthBox
import com.mmr.app.ui.adaptive.mmrScreenHorizontalPadding
import com.mmr.app.ui.components.MmrAddListFab
import com.mmr.app.ui.components.MmrDimens
import com.mmr.app.ui.components.MmrNestedScaffoldInsets
import com.mmr.app.ui.components.MmrSelectionModeBackHandler
import com.mmr.app.ui.components.MmrTopAppBar
import com.mmr.app.ui.state.MmrEmptyState
import com.mmr.app.ui.state.MmrErrorState
import com.mmr.app.ui.state.MmrLoadingState
import com.mmr.app.ui.theme.MmrBrandColors
import com.mmr.core.model.enums.TransactionType
import com.mmr.core.parser.permission.SmsPermissionHelper
import com.mmr.feature.approval.model.ApprovalCandidateUi
import com.mmr.feature.approval.model.ApprovalCenterUiEvent
import com.mmr.feature.approval.model.ApprovalWidgetLaunchAction
import com.mmr.feature.approval.viewmodel.ApprovalCenterViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ApprovalCenterScreen(
    onNavigateBack: () -> Unit,
    onNavigateToEditPending: (Long) -> Unit,
    showBackNavigation: Boolean = true,
    launchApprovalId: Long = 0L,
    launchAction: String = "",
    viewModel: ApprovalCenterViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    var widgetLaunchConsumed by remember { mutableStateOf(false) }
    MmrSelectionModeBackHandler(
        enabled = state.isSelectionMode,
        onExitSelectionMode = { viewModel.onEvent(ApprovalCenterUiEvent.OnExitApprovalSelection) },
    )
    val context = LocalContext.current
    var smsPreviewCandidate by remember { mutableStateOf<ApprovalCandidateUi?>(null) }

    LaunchedEffect(launchApprovalId, launchAction, state.candidates, state.isLoading) {
        if (widgetLaunchConsumed || launchApprovalId <= 0L || launchAction.isBlank() || state.isLoading) {
            return@LaunchedEffect
        }
        val candidate = state.candidates.find { it.id == launchApprovalId } ?: return@LaunchedEffect
        widgetLaunchConsumed = true
        when (launchAction) {
            ApprovalWidgetLaunchAction.VIEW -> smsPreviewCandidate = candidate
            ApprovalWidgetLaunchAction.EDIT -> {
                viewModel.onEvent(ApprovalCenterUiEvent.OnEditClick(candidate.id))
                onNavigateToEditPending(candidate.id)
            }
            ApprovalWidgetLaunchAction.LINK -> {
                if (candidate.isCardRefund) {
                    viewModel.onEvent(ApprovalCenterUiEvent.OnApplyRefundQuick(candidate.id))
                } else {
                    viewModel.onEvent(ApprovalCenterUiEvent.OnLinkToExisting(candidate.id))
                }
            }
        }
    }

    smsPreviewCandidate?.let { candidate ->
        ApprovalSmsPreviewDialog(
            sender = candidate.sender,
            body = candidate.smsFullBody,
            onDismiss = { smsPreviewCandidate = null },
        )
    }

    val smsPermissionLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestPermission(),
        ) { granted ->
            if (granted) {
                viewModel.onEvent(ApprovalCenterUiEvent.OnScanInboxClick)
            }
        }

    fun onScanSmsClicked() {
        if (SmsPermissionHelper.hasSmsPermission(context)) {
            viewModel.onEvent(ApprovalCenterUiEvent.OnScanInboxClick)
        } else {
            smsPermissionLauncher.launch(Manifest.permission.READ_SMS)
        }
    }

    if (state.showManualEntryDialog) {
        ManualSmsEntryDialog(
            errorMessage = state.manualEntryError,
            onDismiss = { viewModel.onEvent(ApprovalCenterUiEvent.OnDismissManualEntry) },
            onSubmit = { sender, body, timestamp ->
                viewModel.onEvent(ApprovalCenterUiEvent.OnSubmitManualSms(sender, body, timestamp))
            },
        )
    }

    state.pendingCardRefundApply?.let { refund ->
        ApplyCardRefundDialog(
            prompt = refund,
            onDismiss = { viewModel.onEvent(ApprovalCenterUiEvent.OnDismissCardRefundApply) },
            onPreviousMonth = { viewModel.onEvent(ApprovalCenterUiEvent.OnCardRefundPickerPreviousMonth) },
            onNextMonth = { viewModel.onEvent(ApprovalCenterUiEvent.OnCardRefundPickerNextMonth) },
            onNoteQueryChange = { viewModel.onEvent(ApprovalCenterUiEvent.OnCardRefundPickerNoteQueryChanged(it)) },
            onSelectPurchase = { viewModel.onEvent(ApprovalCenterUiEvent.OnCardRefundSelectPurchase(it)) },
            onBackToPicker = { viewModel.onEvent(ApprovalCenterUiEvent.OnCardRefundBackToPicker) },
            onToggleFullRefund = { viewModel.onEvent(ApprovalCenterUiEvent.OnCardRefundToggleFull(it)) },
            onPartialAmountChange = { viewModel.onEvent(ApprovalCenterUiEvent.OnCardRefundPartialAmountChanged(it)) },
            onConfirmApply = { viewModel.onEvent(ApprovalCenterUiEvent.OnCardRefundConfirmApply) },
        )
    }

    state.pendingSmsMerge?.let { merge ->
        ApprovalLinkTransactionDialog(
            prompt = merge,
            onEvent = viewModel::onEvent,
            onMergeInto = { viewModel.onEvent(ApprovalCenterUiEvent.OnMergeSmsIntoTransaction(it)) },
            onApproveAsNew = { viewModel.onEvent(ApprovalCenterUiEvent.OnApproveAsNewTransaction) },
            onDismiss = { viewModel.onEvent(ApprovalCenterUiEvent.OnDismissSmsMerge) },
            onPreviousMonth = { viewModel.onEvent(ApprovalCenterUiEvent.OnLinkPickerPreviousMonth) },
            onNextMonth = { viewModel.onEvent(ApprovalCenterUiEvent.OnLinkPickerNextMonth) },
            onNoteQueryChange = { viewModel.onEvent(ApprovalCenterUiEvent.OnLinkPickerNoteQueryChanged(it)) },
        )
    }

    state.pendingManualDuplicate?.let { draft ->
        ManualSmsDuplicateDialog(
            priorActionLabel = draft.duplicateMatch.priorActionLabel,
            approvalId = draft.duplicateMatch.approvalId,
            onRevertPrior = { viewModel.onEvent(ApprovalCenterUiEvent.OnManualDuplicateRevertPrior) },
            onAddAsNew = { viewModel.onEvent(ApprovalCenterUiEvent.OnManualDuplicateAddAsNew) },
            onDismiss = { viewModel.onEvent(ApprovalCenterUiEvent.OnDismissManualDuplicate) },
        )
    }

    Scaffold(
        containerColor = MmrBrandColors.ListBackground,
        contentWindowInsets = MmrNestedScaffoldInsets,
        floatingActionButton = {
            if (!state.isSelectionMode) {
                MmrAddListFab(
                    modifier = Modifier.testTag(MmrTestTags.approvalFabManualSms),
                    contentDescription = "Add SMS manually",
                    onClick = { viewModel.onEvent(ApprovalCenterUiEvent.OnAddManualSmsClick) },
                )
            }
        },
        topBar = {
            if (state.isSelectionMode) {
                MmrTopAppBar(
                    title = "${state.selectedApprovalIds.size} selected",
                    showBackNavigation = true,
                    onNavigateBack = { viewModel.onEvent(ApprovalCenterUiEvent.OnExitApprovalSelection) },
                    useBrandHeaderStyle = true,
                    useCompactHeaderTitle = true,
                    actions = {
                        IconButton(
                            onClick = { viewModel.onEvent(ApprovalCenterUiEvent.OnApprovalSelectAll) },
                            modifier = Modifier.testTag(MmrTestTags.approvalSelectionSelectAll),
                        ) {
                            Icon(
                                imageVector =
                                    if (state.allPendingSelected) {
                                        Icons.Filled.CheckBox
                                    } else {
                                        Icons.Outlined.SelectAll
                                    },
                                contentDescription = "Select all",
                            )
                        }
                        IconButton(
                            onClick = { viewModel.onEvent(ApprovalCenterUiEvent.OnLinkSelectedApprovals) },
                            enabled = state.canLinkSelection,
                            modifier = Modifier.testTag(MmrTestTags.approvalSelectionLink),
                        ) {
                            Icon(Icons.AutoMirrored.Outlined.CallMerge, contentDescription = "Link to transaction")
                        }
                    },
                )
            } else {
                MmrTopAppBar(
                    title = "Approval Center",
                    subtitle = stringResource(R.string.app_tagline),
                    showBackNavigation = showBackNavigation,
                    onNavigateBack = onNavigateBack,
                    useBrandHeaderStyle = true,
                    actions = {
                        if (state.isScanning) {
                            Box(
                                modifier =
                                    Modifier
                                        .size(MmrDimens.topBarIconButton)
                                        .testTag(MmrTestTags.approvalScanSms),
                                contentAlignment = Alignment.Center,
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(22.dp),
                                    strokeWidth = 2.dp,
                                    color = MmrBrandColors.HeaderIcon,
                                )
                            }
                        } else {
                            IconButton(
                                onClick = { onScanSmsClicked() },
                                modifier =
                                    Modifier
                                        .size(MmrDimens.topBarIconButton)
                                        .testTag(MmrTestTags.approvalScanSms),
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Refresh,
                                    contentDescription = "Scan SMS inbox",
                                    tint = MmrBrandColors.HeaderIcon,
                                    modifier = Modifier.size(MmrDimens.iconMd),
                                )
                            }
                        }
                    },
                )
            }
        },
    ) { padding ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(padding),
        ) {
            state.scanResultMessage?.let { msg ->
                Card(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = mmrScreenHorizontalPadding(), vertical = MmrDimens.spaceSm),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                ) {
                    Row(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(MmrDimens.spaceMd),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = msg,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.weight(1f),
                        )
                        TextButton(onClick = { viewModel.onEvent(ApprovalCenterUiEvent.OnClearScanMessage) }) {
                            Text("Dismiss")
                        }
                    }
                }
            }

            when {
                state.isLoading -> MmrLoadingState()

                state.errorMessage != null -> {
                    MmrErrorState(
                        message = state.errorMessage ?: "Something went wrong.",
                        onRetry = { viewModel.onEvent(ApprovalCenterUiEvent.OnRetryClick) },
                    )
                }

                state.isEmpty -> {
                    MmrEmptyState(
                        message = "No pending transactions.\nTap refresh or + to scan or add an SMS manually.",
                    )
                }

                else -> {
                    val horizontalPadding = mmrScreenHorizontalPadding()
                    MmrAdaptiveWidthBox(modifier = Modifier.fillMaxSize()) {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(horizontalPadding, vertical = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            items(state.candidates, key = { it.id }) { candidate ->
                                ApprovalCandidateCard(
                                    candidate = candidate,
                                    isSelectionMode = state.isSelectionMode,
                                    isSelected = candidate.id in state.selectedApprovalIds,
                                    onLongPress = {
                                        viewModel.onEvent(ApprovalCenterUiEvent.OnApprovalLongPress(candidate.id))
                                    },
                                    onToggleSelect = {
                                        viewModel.onEvent(ApprovalCenterUiEvent.OnApprovalSelectionToggle(candidate.id))
                                    },
                                    onViewSms = { smsPreviewCandidate = candidate },
                                    onEdit = {
                                        viewModel.onEvent(ApprovalCenterUiEvent.OnEditClick(candidate.id))
                                        onNavigateToEditPending(candidate.id)
                                    },
                                    onLink = {
                                        if (candidate.isCardRefund) {
                                            viewModel.onEvent(ApprovalCenterUiEvent.OnApplyRefundQuick(candidate.id))
                                        } else {
                                            viewModel.onEvent(ApprovalCenterUiEvent.OnLinkToExisting(candidate.id))
                                        }
                                    },
                                    onApprove = {
                                        if (candidate.isCardRefund) {
                                            viewModel.onEvent(ApprovalCenterUiEvent.OnApplyRefundQuick(candidate.id))
                                        } else {
                                            viewModel.onEvent(ApprovalCenterUiEvent.OnApproveQuick(candidate.id))
                                        }
                                    },
                                    onReject = {
                                        viewModel.onEvent(ApprovalCenterUiEvent.OnRejectQuick(candidate.id))
                                    },
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private val ApprovalCardLabelWidth = 88.dp
private val ApprovalCardIconSize = 29.dp
private val ApprovalCardIconGlyph = 14.dp

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ApprovalCandidateCard(
    candidate: ApprovalCandidateUi,
    isSelectionMode: Boolean = false,
    isSelected: Boolean = false,
    onLongPress: () -> Unit = {},
    onToggleSelect: () -> Unit = {},
    onViewSms: () -> Unit,
    onEdit: () -> Unit,
    onLink: () -> Unit = {},
    onApprove: () -> Unit,
    onReject: () -> Unit,
) {
    val amountColor =
        when {
            candidate.isCardRefund -> MaterialTheme.colorScheme.primary
            candidate.type == TransactionType.INCOME -> MmrBrandColors.AmountPositive
            else -> MmrBrandColors.AmountNegative
        }
    val neutralIconBg = MaterialTheme.colorScheme.surfaceContainerHighest
    val approveBg = MmrBrandColors.ApprovalApproveBackground
    val rejectBg = MmrBrandColors.ApprovalRejectBackground

    Card(
        modifier =
            Modifier
                .testTag(MmrTestTags.approvalCard(candidate.id))
                .fillMaxWidth()
                .combinedClickable(
                    onClick = {
                        if (isSelectionMode) onToggleSelect()
                    },
                    onLongClick = onLongPress,
                ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
        ) {
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(bottom = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                if (isSelectionMode) {
                    Checkbox(
                        checked = isSelected,
                        onCheckedChange = { onToggleSelect() },
                    )
                }
                Column(
                    modifier = if (isSelectionMode) Modifier.weight(1f) else Modifier,
                ) {
                    Text(
                        text = candidate.amountFormatted,
                        fontSize = 14.sp,
                        lineHeight = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = amountColor,
                    )
                    Text(
                        text = candidate.statusLabel,
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 9.sp,
                        lineHeight = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 1.dp),
                    )
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(1.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (!isSelectionMode) {
                        ApprovalCardIconAction(
                            onClick = onViewSms,
                            contentDescription = "View SMS",
                            containerColor = neutralIconBg,
                            icon = Icons.Outlined.Visibility,
                            iconTint = MaterialTheme.colorScheme.onSurface,
                        )
                        ApprovalCardIconAction(
                            onClick = onEdit,
                            contentDescription = "Edit",
                            containerColor = neutralIconBg,
                            icon = Icons.Filled.Edit,
                            iconTint = MaterialTheme.colorScheme.onSurface,
                        )
                        if (!candidate.isCardRefund) {
                            ApprovalCardIconAction(
                                onClick = onLink,
                                contentDescription = "Link to transaction",
                                containerColor = neutralIconBg,
                                icon = Icons.AutoMirrored.Outlined.CallMerge,
                                iconTint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.testTag(MmrTestTags.approvalLink(candidate.id)),
                            )
                        }
                        ApprovalCardIconAction(
                            onClick = onApprove,
                            contentDescription = if (candidate.isCardRefund) "Apply refund" else "Approve",
                            containerColor = approveBg,
                            icon = Icons.Filled.Check,
                            iconTint = MmrBrandColors.ApprovalApproveIcon,
                            modifier = Modifier.testTag(MmrTestTags.approvalQuickApprove(candidate.id)),
                        )
                        ApprovalCardIconAction(
                            onClick = onReject,
                            contentDescription = "Reject",
                            containerColor = rejectBg,
                            icon = Icons.Filled.Close,
                            iconTint = MmrBrandColors.ApprovalRejectIcon,
                            modifier = Modifier.testTag(MmrTestTags.approvalQuickReject(candidate.id)),
                        )
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                ApprovalCompactDetailRow(label = "Type", value = candidate.typeLabel)
                candidate.parseSourceLabel?.let { source ->
                    ApprovalCompactDetailRow(label = "Parse source", value = source)
                }
                ApprovalCompactDetailRow(label = "Confidence", value = candidate.confidenceLabel)
                ApprovalCompactDetailRow(label = "Date / time", value = candidate.dateLabel)
                ApprovalCompactDetailRow(label = "Sender", value = candidate.sender)
                ApprovalCompactDetailRow(label = "Category", value = candidate.categoryName)
                ApprovalCompactDetailRow(label = "Account", value = candidate.accountName)
                ApprovalCompactDetailRow(label = "Note", value = candidate.note, valueMaxLines = 3)
            }
        }
    }
}

@Composable
private fun ApprovalCardIconAction(
    onClick: () -> Unit,
    contentDescription: String,
    containerColor: Color,
    icon: ImageVector,
    iconTint: Color,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(6.dp),
        color = containerColor,
        modifier = modifier.size(ApprovalCardIconSize),
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                modifier = Modifier.size(ApprovalCardIconGlyph),
                tint = iconTint,
            )
        }
    }
}

@Composable
private fun ApprovalCompactDetailRow(
    label: String,
    value: String,
    valueMaxLines: Int = 2,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top,
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            lineHeight = 16.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(ApprovalCardLabelWidth),
        )
        Text(
            text = value,
            fontSize = 12.sp,
            lineHeight = 16.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
            maxLines = valueMaxLines,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
