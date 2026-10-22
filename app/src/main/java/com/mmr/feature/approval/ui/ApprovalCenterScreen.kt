package com.mmr.feature.approval.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.mmr.core.parser.permission.SmsPermissionHelper
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.mmr.core.model.enums.TransactionType
import com.mmr.feature.approval.model.ApprovalCandidateUi
import com.mmr.feature.approval.model.ApprovalCenterUiEvent
import com.mmr.app.ui.state.MmrEmptyState
import com.mmr.app.ui.state.MmrErrorState
import com.mmr.app.ui.state.MmrLoadingState
import com.mmr.app.ui.adaptive.MmrAdaptiveWidthBox
import com.mmr.app.ui.adaptive.MmrLabelValueRow
import com.mmr.app.ui.adaptive.mmrScreenHorizontalPadding
import com.mmr.feature.approval.viewmodel.ApprovalCenterViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ApprovalCenterScreen(
    onNavigateBack: () -> Unit,
    onNavigateToEditPending: (Long) -> Unit,
    showBackNavigation: Boolean = true,
    viewModel: ApprovalCenterViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    val smsPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
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
            }
        )
    }

    state.pendingManualDuplicate?.let { draft ->
        ManualSmsDuplicateDialog(
            priorActionLabel = draft.duplicateMatch.priorActionLabel,
            approvalId = draft.duplicateMatch.approvalId,
            onRevertPrior = { viewModel.onEvent(ApprovalCenterUiEvent.OnManualDuplicateRevertPrior) },
            onAddAsNew = { viewModel.onEvent(ApprovalCenterUiEvent.OnManualDuplicateAddAsNew) },
            onDismiss = { viewModel.onEvent(ApprovalCenterUiEvent.OnDismissManualDuplicate) }
        )
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { viewModel.onEvent(ApprovalCenterUiEvent.OnAddManualSmsClick) }
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add SMS manually")
            }
        },
        topBar = {
            TopAppBar(
                title = { Text("Approval Center", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    if (showBackNavigation) {
                        TextButton(onClick = onNavigateBack) {
                            Text("Back")
                        }
                    }
                },
                actions = {
                    TextButton(
                        onClick = { onScanSmsClicked() },
                        enabled = !state.isScanning
                    ) {
                        Text(if (state.isScanning) "Scanning..." else "Scan SMS")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            state.scanResultMessage?.let { msg ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = msg,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.weight(1f)
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
                        onRetry = { viewModel.onEvent(ApprovalCenterUiEvent.OnRetryClick) }
                    )
                }

                state.isEmpty -> {
                    MmrEmptyState(
                        message = "No pending transactions.\nTap 'Scan SMS' or + to add an SMS manually."
                    )
                }

                else -> {
                    val horizontalPadding = mmrScreenHorizontalPadding()
                    MmrAdaptiveWidthBox(modifier = Modifier.fillMaxSize()) {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(horizontalPadding, vertical = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(state.candidates, key = { it.id }) { candidate ->
                                ApprovalCandidateCard(
                                    candidate = candidate,
                                    onEdit = {
                                        viewModel.onEvent(ApprovalCenterUiEvent.OnEditClick(candidate.id))
                                        onNavigateToEditPending(candidate.id)
                                    },
                                    onApprove = {
                                        viewModel.onEvent(ApprovalCenterUiEvent.OnApproveQuick(candidate.id))
                                    },
                                    onReject = {
                                        viewModel.onEvent(ApprovalCenterUiEvent.OnRejectQuick(candidate.id))
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ApprovalCandidateCard(
    candidate: ApprovalCandidateUi,
    onEdit: () -> Unit,
    onApprove: () -> Unit,
    onReject: () -> Unit
) {
    val amountColor = if (candidate.type == TransactionType.INCOME) {
        Color(0xFF10B981)
    } else {
        Color(0xFFEF4444)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = candidate.amountFormatted,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = amountColor
                )
                AssistChip(
                    onClick = {},
                    label = { Text(candidate.statusLabel, style = MaterialTheme.typography.labelSmall) }
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            MmrLabelValueRow(label = "Type", value = candidate.typeLabel)
            candidate.parseSourceLabel?.let { label ->
                MmrLabelValueRow(label = "Parse source", value = label)
            }
            MmrLabelValueRow(label = "Confidence", value = candidate.confidenceLabel)
            MmrLabelValueRow(label = "Date / time", value = candidate.dateLabel)
            MmrLabelValueRow(label = "Sender / source", value = candidate.sender)
            MmrLabelValueRow(label = "Category", value = candidate.categoryName)
            MmrLabelValueRow(label = "Account", value = candidate.accountName)
            MmrLabelValueRow(label = "Note", value = candidate.note)

            Spacer(modifier = Modifier.height(12.dp))

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TextButton(onClick = onEdit) {
                    Text("Edit")
                }
                Button(onClick = onApprove) {
                    Text("Approve")
                }
                OutlinedButton(onClick = onReject) {
                    Text("Reject", color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

