package com.mmr.feature.approval.ui

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.mmr.app.testing.MmrTestTags
import com.mmr.app.ui.adaptive.MmrAdaptiveWidthBox
import com.mmr.app.ui.adaptive.mmrScreenHorizontalPadding
import com.mmr.app.ui.components.MmrDimens
import com.mmr.app.ui.components.MmrTopAppBar
import com.mmr.app.ui.state.MmrEmptyState
import com.mmr.app.ui.state.MmrErrorState
import com.mmr.app.ui.state.MmrLoadingState
import com.mmr.core.model.enums.TransactionType
import com.mmr.core.parser.permission.SmsPermissionHelper
import com.mmr.feature.approval.model.ApprovalCandidateUi
import com.mmr.feature.approval.model.ApprovalCenterUiEvent
import com.mmr.feature.approval.viewmodel.ApprovalCenterViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ApprovalCenterScreen(
    onNavigateBack: () -> Unit,
    onNavigateToEditPending: (Long) -> Unit,
    showBackNavigation: Boolean = true,
    viewModel: ApprovalCenterViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    var smsPreviewCandidate by remember { mutableStateOf<ApprovalCandidateUi?>(null) }

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

    state.pendingSmsMerge?.let { merge ->
        SmsMergeDialog(
            prompt = merge,
            categoryOptions = state.mergeCategoryOptions,
            onCategorySelected = { viewModel.onEvent(ApprovalCenterUiEvent.OnMergeCategorySelected(it)) },
            onUseLedgerCategory = { viewModel.onEvent(ApprovalCenterUiEvent.OnMergeUseLedgerCategory) },
            onMergeInto = { viewModel.onEvent(ApprovalCenterUiEvent.OnMergeSmsIntoTransaction(it)) },
            onApproveAsNew = { viewModel.onEvent(ApprovalCenterUiEvent.OnApproveAsNewTransaction) },
            onDismiss = { viewModel.onEvent(ApprovalCenterUiEvent.OnDismissSmsMerge) },
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
        floatingActionButton = {
            FloatingActionButton(
                modifier = Modifier.testTag(MmrTestTags.approvalFabManualSms),
                onClick = { viewModel.onEvent(ApprovalCenterUiEvent.OnAddManualSmsClick) },
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add SMS manually")
            }
        },
        topBar = {
            MmrTopAppBar(
                title = "Approval Center",
                showBackNavigation = showBackNavigation,
                onNavigateBack = onNavigateBack,
                actions = {
                    TextButton(
                        onClick = { onScanSmsClicked() },
                        enabled = !state.isScanning,
                        modifier = Modifier.testTag(MmrTestTags.approvalScanSms),
                    ) {
                        Text(if (state.isScanning) "Scanning…" else "Scan SMS")
                    }
                },
            )
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
                        message = "No pending transactions.\nTap 'Scan SMS' or + to add an SMS manually.",
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
                                    onViewSms = { smsPreviewCandidate = candidate },
                                    onEdit = {
                                        viewModel.onEvent(ApprovalCenterUiEvent.OnEditClick(candidate.id))
                                        onNavigateToEditPending(candidate.id)
                                    },
                                    onApprove = {
                                        viewModel.onEvent(ApprovalCenterUiEvent.OnApproveQuick(candidate.id))
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

@Composable
fun ApprovalCandidateCard(
    candidate: ApprovalCandidateUi,
    onViewSms: () -> Unit,
    onEdit: () -> Unit,
    onApprove: () -> Unit,
    onReject: () -> Unit,
) {
    val amountColor =
        if (candidate.type == TransactionType.INCOME) {
            Color(0xFF10B981)
        } else {
            Color(0xFFEF4444)
        }
    val neutralIconBg = MaterialTheme.colorScheme.surfaceContainerHighest
    val approveBg = Color(0xFF1B3D2F)
    val rejectBg = Color(0xFF3D1F24)

    Card(
        modifier =
            Modifier
                .testTag(MmrTestTags.approvalCard(candidate.id))
                .fillMaxWidth(),
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
                Column {
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
                    ApprovalCardIconAction(
                        onClick = onApprove,
                        contentDescription = "Approve",
                        containerColor = approveBg,
                        icon = Icons.Filled.Check,
                        iconTint = Color(0xFF22C55E),
                        modifier = Modifier.testTag(MmrTestTags.approvalQuickApprove(candidate.id)),
                    )
                    ApprovalCardIconAction(
                        onClick = onReject,
                        contentDescription = "Reject",
                        containerColor = rejectBg,
                        icon = Icons.Filled.Close,
                        iconTint = Color(0xFFEF4444),
                        modifier = Modifier.testTag(MmrTestTags.approvalQuickReject(candidate.id)),
                    )
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
