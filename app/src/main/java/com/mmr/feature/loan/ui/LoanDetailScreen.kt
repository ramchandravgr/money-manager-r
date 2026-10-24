package com.mmr.feature.loan.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Pause
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.mmr.app.testing.MmrTestTags
import com.mmr.app.ui.components.MmrConfirmDialog
import com.mmr.app.ui.components.MmrContentDialog
import com.mmr.app.ui.components.MmrDeleteIcon
import com.mmr.app.ui.components.MmrDetailOverviewCard
import com.mmr.app.ui.components.MmrDetailProgressStrip
import com.mmr.app.ui.components.MmrDetailScreenScaffold
import com.mmr.app.ui.components.MmrDetailSectionTitle
import com.mmr.app.ui.components.MmrDetailStatusBadge
import com.mmr.app.ui.components.MmrDialogBodyNote
import com.mmr.app.ui.components.MmrDialogDetailLine
import com.mmr.app.ui.components.MmrDimens
import com.mmr.app.ui.components.MmrInputDialog
import com.mmr.app.ui.state.MmrStatusBanner
import com.mmr.app.ui.theme.MmrBrandColors
import com.mmr.core.loan.LoanDisplayFormat
import com.mmr.core.model.enums.LoanPaymentType
import com.mmr.core.model.enums.LoanRepaymentMode
import com.mmr.core.model.enums.LoanStatus
import com.mmr.core.model.enums.LoanType
import com.mmr.feature.loan.model.*
import com.mmr.feature.loan.viewmodel.LoanDetailViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoanDetailScreen(
    onNavigateBack: () -> Unit,
    onNavigateToEditLoan: (Long) -> Unit = {},
    onNavigateToEditTransaction: (Long) -> Unit = {},
    viewModel: LoanDetailViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    var showRepaymentDialog by remember { mutableStateOf(false) }
    var showDeleteLoanDialog by remember { mutableStateOf(false) }
    var overflowMenuExpanded by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.isLoanDeleted) {
        if (uiState.isLoanDeleted) {
            onNavigateBack()
        }
    }

    uiState.revisionDetail?.let { detail ->
        MmrContentDialog(
            title = "Rate change",
            onDismiss = { viewModel.onEvent(LoanDetailUiEvent.OnDismissRevisionDetail) },
            confirmLabel = "Close",
            onConfirm = { viewModel.onEvent(LoanDetailUiEvent.OnDismissRevisionDetail) },
            showDismissButton = false,
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                MmrDialogDetailLine("Before", detail.beforeRateLabel)
                MmrDialogDetailLine("After", detail.afterRateLabel)
                detail.emiBeforeFormatted?.let { MmrDialogDetailLine("EMI before", it) }
                detail.emiAfterFormatted?.let { MmrDialogDetailLine("EMI after", it) }
                detail.note?.let { MmrDialogBodyNote(it) }
            }
        }
    }

    LaunchedEffect(uiState.isRepaymentSuccess) {
        if (uiState.isRepaymentSuccess) {
            showRepaymentDialog = false
            viewModel.onEvent(LoanDetailUiEvent.OnClearRepaymentState)
        }
    }

    val loan = uiState.loan
    val showLoanFooter =
        loan != null &&
            loan.status != LoanStatus.CLOSED &&
            loan.outstandingAmount > 0.0 &&
            !uiState.isLoading

    MmrDetailScreenScaffold(
        title = loan?.name?.let { LoanDisplayFormat.displayName(it) } ?: "Loan details",
        onNavigateBack = onNavigateBack,
        useCompactHeaderTitle = true,
        isLoading = uiState.isLoading,
        errorMessage = if (loan == null) uiState.errorMessage else null,
        onRetry = { viewModel.onEvent(LoanDetailUiEvent.OnRetryClick) },
        footerPrimaryLabel = if (showLoanFooter) "Record repayment" else null,
        onFooterPrimary =
            if (showLoanFooter) {
                { showRepaymentDialog = true }
            } else {
                null
            },
        footerPrimaryTestTag = MmrTestTags.loanDetailFabRepayment,
        footerSecondaryLabel =
            if (showLoanFooter && loan?.status != LoanStatus.CLOSED) {
                "Add top-up"
            } else {
                null
            },
        onFooterSecondary =
            if (showLoanFooter) {
                { viewModel.onEvent(LoanDetailUiEvent.OnOpenTopUpDialog) }
            } else {
                null
            },
        topBarActions = {
            loan?.let { loanEntity ->
                Box {
                    IconButton(onClick = { overflowMenuExpanded = true }) {
                        Icon(
                            imageVector = Icons.Filled.MoreVert,
                            contentDescription = "Loan options",
                            tint = MmrBrandColors.HeaderIcon,
                        )
                    }
                    DropdownMenu(
                        expanded = overflowMenuExpanded,
                        onDismissRequest = { overflowMenuExpanded = false },
                        shape = RoundedCornerShape(12.dp),
                        containerColor = MmrBrandColors.SurfaceWhite,
                        tonalElevation = 6.dp,
                        shadowElevation = 6.dp,
                        modifier = Modifier.widthIn(min = 220.dp),
                    ) {
                        if (loanEntity.status != LoanStatus.CLOSED) {
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        "Edit loan",
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = MmrBrandColors.Reversal,
                                    )
                                },
                                leadingIcon = {
                                    Icon(Icons.Outlined.Edit, contentDescription = null)
                                },
                                onClick = {
                                    overflowMenuExpanded = false
                                    onNavigateToEditLoan(loanEntity.id)
                                },
                            )
                        }
                        when (loanEntity.status) {
                            LoanStatus.ACTIVE -> {
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            "Pause loan",
                                            style = MaterialTheme.typography.bodyLarge,
                                            color = MmrBrandColors.Reversal,
                                        )
                                    },
                                    leadingIcon = {
                                        Icon(Icons.Outlined.Pause, contentDescription = null)
                                    },
                                    onClick = {
                                        overflowMenuExpanded = false
                                        viewModel.onEvent(LoanDetailUiEvent.OnStatusChanged(LoanStatus.PAUSED))
                                    },
                                )
                            }
                            LoanStatus.PAUSED -> {
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            "Resume loan",
                                            style = MaterialTheme.typography.bodyLarge,
                                            color = MmrBrandColors.Reversal,
                                        )
                                    },
                                    leadingIcon = {
                                        Icon(Icons.Outlined.PlayArrow, contentDescription = null)
                                    },
                                    onClick = {
                                        overflowMenuExpanded = false
                                        viewModel.onEvent(LoanDetailUiEvent.OnStatusChanged(LoanStatus.ACTIVE))
                                    },
                                )
                            }
                            else -> Unit
                        }
                        HorizontalDivider(color = MmrBrandColors.BorderMain)
                        DropdownMenuItem(
                            text = {
                                Text(
                                    "Delete loan",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.error,
                                )
                            },
                            leadingIcon = {
                                MmrDeleteIcon(contentDescription = null)
                            },
                            onClick = {
                                overflowMenuExpanded = false
                                showDeleteLoanDialog = true
                            },
                        )
                    }
                }
            }
        },
    ) {
        if (loan != null) {
            val isBorrowed = loan.loanType == LoanType.BORROWED
            val directionColor = if (isBorrowed) MmrBrandColors.AmountNegative else MmrBrandColors.AmountPositive
            uiState.statusMessage?.let { message ->
                item {
                    MmrStatusBanner(
                        message = message,
                        onDismiss = { viewModel.onEvent(LoanDetailUiEvent.OnDismissStatus) },
                    )
                }
            }

            item {
                MmrDetailOverviewCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column {
                            Text(
                                text = if (isBorrowed) "I Owe (Borrowed)" else "Owed to Me (Lent)",
                                style = MaterialTheme.typography.labelMedium,
                                color = directionColor,
                                fontWeight = FontWeight.Bold,
                            )
                            Text(
                                text = loan.lenderName,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }

                        MmrDetailStatusBadge(text = loan.statusLabel)
                    }

                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 12.dp),
                        color = MaterialTheme.colorScheme.outlineVariant,
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Column {
                            Text("Outstanding", style = MaterialTheme.typography.labelSmall)
                            Text(
                                text = loan.outstandingFormatted,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = directionColor,
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("Principal", style = MaterialTheme.typography.labelSmall)
                            Text(
                                text = loan.principalFormatted,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    MmrDetailProgressStrip(
                        progress = loan.progressPercentage,
                        color = directionColor,
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            text = "${loan.progressFormatted} repaid",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Medium,
                        )
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "Disbursed ${loan.disbursalDateFormatted}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.outline,
                            )
                            Text(
                                text = loan.dueDateFormatted?.let { "Due: $it" } ?: "No due date set",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.outline,
                            )
                        }
                    }
                }
            }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "Reducing-balance interest at ${loan.interestRateFormatted} p.a. (floating revisions keep past payments unchanged).",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    uiState.fixedEmiFormatted?.let { emi ->
                        Text(
                            text = "Current EMI: $emi • ${uiState.remainingTenureMonths ?: "?"} month(s) remaining",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                    if (uiState.repaymentMode == LoanRepaymentMode.FLEXIBLE) {
                        uiState.flexibleInterestPendingFormatted?.let { pending ->
                            Text(
                                text = "Accrued interest (pending): $pending",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                        uiState.flexibleInterestAccrualFromLabel?.let { from ->
                            Text(
                                text = "Accruing since $from on current outstanding (actual/365). Payments clear interest first, then principal.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(MmrDimens.spaceMd)) {
                        Text(
                            text = "Change rate",
                            modifier =
                                Modifier.clickable {
                                    viewModel.onEvent(LoanDetailUiEvent.OnOpenRateChangeDialog)
                                },
                            style = MaterialTheme.typography.labelLarge,
                            color = MmrBrandColors.Active,
                            fontWeight = FontWeight.Medium,
                        )
                        if (uiState.repaymentMode == LoanRepaymentMode.FIXED_EMI) {
                            Text(
                                text = "Change EMI",
                                modifier =
                                    Modifier.clickable {
                                        viewModel.onEvent(LoanDetailUiEvent.OnOpenEmiChangeDialog)
                                    },
                                style = MaterialTheme.typography.labelLarge,
                                color = MmrBrandColors.Active,
                                fontWeight = FontWeight.Medium,
                            )
                        }
                    }
                }
            }

            if (uiState.interestRevisions.isNotEmpty()) {
                item {
                    MmrDetailSectionTitle("Interest rate history")
                }
                items(uiState.interestRevisions, key = { "revision-${it.id}" }) { revision ->
                    Card(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.onEvent(
                                        LoanDetailUiEvent.OnInterestRevisionClick(revision.id),
                                    )
                                },
                    ) {
                        Column(Modifier.padding(12.dp)) {
                            Text(revision.effectiveDateLabel, fontWeight = FontWeight.SemiBold)
                            Text(revision.rateLabel, style = MaterialTheme.typography.bodySmall)
                            revision.note?.let {
                                Text(it, style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }
            }

            if (uiState.upcomingSchedule.isNotEmpty()) {
                item {
                    MmrDetailSectionTitle("Upcoming EMIs")
                }
                items(
                    uiState.upcomingSchedule.size,
                    key = { index -> "upcoming-$index" },
                ) { index ->
                    val entry = uiState.upcomingSchedule[index]
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Column {
                                Text(entry.dueDateLabel, fontWeight = FontWeight.SemiBold)
                                Text(
                                    "Principal ${entry.principalFormatted} • Interest ${entry.interestFormatted}",
                                    style = MaterialTheme.typography.labelSmall,
                                )
                            }
                            Text(entry.emiFormatted, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // 2. Repayment History Section Header
            item {
                MmrDetailSectionTitle("Repayment History")
            }

            // 3. Payment history list or empty
            if (uiState.payments.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors =
                            CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface,
                            ),
                    ) {
                        Text(
                            text = "No repayments recorded yet.\nTap 'Record Payment' to add one.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(MmrDimens.cardPadding),
                        )
                    }
                }
            } else {
                items(uiState.payments, key = { "payment-${it.id}" }) { payment ->
                    PaymentHistoryCard(
                        payment = payment,
                        onClick =
                            payment.linkedTransactionId?.let { txnId ->
                                { onNavigateToEditTransaction(txnId) }
                            },
                    )
                }
            }
        }
    }

    if (uiState.showRateChangeDialog) {
        MmrInputDialog(
            title = "Change interest rate",
            message = "Annual rate in percent. Future EMIs use the new rate; past payments stay unchanged.",
            label = "Rate",
            suffix = "% p.a.",
            placeholder = "e.g. 7.75",
            value = uiState.rateChangeInput,
            onValueChange = { viewModel.onEvent(LoanDetailUiEvent.OnRateChangeInputChanged(it)) },
            confirmLabel = "Update",
            onConfirm = { viewModel.onEvent(LoanDetailUiEvent.OnSubmitRateChange) },
            onDismiss = { viewModel.onEvent(LoanDetailUiEvent.OnDismissRateChangeDialog) },
            keyboardType = KeyboardType.Decimal,
        )
    }

    if (uiState.showEmiChangeDialog) {
        MmrInputDialog(
            title = "Change monthly EMI",
            message = "Applies to upcoming scheduled EMIs from the next due date.",
            label = "EMI amount",
            suffix = "₹",
            placeholder = "e.g. 219471.28",
            value = uiState.emiChangeInput,
            onValueChange = { viewModel.onEvent(LoanDetailUiEvent.OnEmiChangeInputChanged(it)) },
            confirmLabel = "Apply",
            onDismiss = { viewModel.onEvent(LoanDetailUiEvent.OnDismissEmiChangeDialog) },
            onConfirm = { viewModel.onEvent(LoanDetailUiEvent.OnSubmitEmiChange) },
            keyboardType = KeyboardType.Decimal,
        )
    }

    // Repayment Entry Dialog
    if (showDeleteLoanDialog) {
        MmrConfirmDialog(
            title = "Delete loan?",
            message =
                "This removes the loan, repayment history, and EMI schedule. " +
                    "Transactions in your ledger are kept but will no longer be linked to this loan.",
            confirmLabel = "Delete",
            onConfirm = {
                showDeleteLoanDialog = false
                viewModel.onEvent(LoanDetailUiEvent.OnDeleteLoan)
            },
            onDismiss = { showDeleteLoanDialog = false },
            confirmIsDestructive = true,
        )
    }

    if (uiState.showTopUpDialog) {
        val loan = uiState.loan
        if (loan != null) {
            LoanTopUpDialog(
                outstandingFormatted = loan.outstandingFormatted,
                repaymentMode = uiState.repaymentMode,
                amountInput = uiState.topUpAmountInput,
                notesInput = uiState.topUpNotesInput,
                topUpDateMillis = uiState.topUpDateMillis,
                topUpAdjustment = uiState.topUpAdjustment,
                isSubmitting = uiState.isTopUpSubmitting,
                errorMessage = uiState.errorMessage,
                onAmountChanged = { viewModel.onEvent(LoanDetailUiEvent.OnTopUpAmountChanged(it)) },
                onNotesChanged = { viewModel.onEvent(LoanDetailUiEvent.OnTopUpNotesChanged(it)) },
                onTopUpDateChanged = { viewModel.onEvent(LoanDetailUiEvent.OnTopUpDateChanged(it)) },
                onTopUpAdjustmentSelected = {
                    viewModel.onEvent(LoanDetailUiEvent.OnTopUpAdjustmentSelected(it))
                },
                onSubmit = { viewModel.onEvent(LoanDetailUiEvent.OnSubmitTopUp) },
                onDismiss = { viewModel.onEvent(LoanDetailUiEvent.OnDismissTopUpDialog) },
            )
        }
    }

    if (showRepaymentDialog) {
        val loan = uiState.loan
        if (loan != null) {
            LoanRepaymentDialog(
                outstandingFormatted = loan.outstandingFormatted,
                nextEmiHint = uiState.upcomingSchedule.firstOrNull()?.emiFormatted,
                flexibleAccruedInterestHint = uiState.flexibleRepaymentAccruedInterestHint,
                amountInput = uiState.repaymentAmountInput,
                notesInput = uiState.repaymentNotesInput,
                paymentDateMillis = uiState.repaymentPaymentDateMillis,
                selectedType = uiState.repaymentType,
                repaymentMode = uiState.repaymentMode,
                partPaymentAdjustment = uiState.partPaymentAdjustment,
                isSubmitting = uiState.isRepaymentSubmitting,
                errorMessage = uiState.errorMessage,
                onAmountChanged = { viewModel.onEvent(LoanDetailUiEvent.OnRepaymentAmountChanged(it)) },
                onNotesChanged = { viewModel.onEvent(LoanDetailUiEvent.OnRepaymentNotesChanged(it)) },
                onPaymentDateChanged = {
                    viewModel.onEvent(LoanDetailUiEvent.OnRepaymentPaymentDateChanged(it))
                },
                onTypeSelected = { viewModel.onEvent(LoanDetailUiEvent.OnRepaymentTypeSelected(it)) },
                onPartPaymentAdjustmentSelected = {
                    viewModel.onEvent(LoanDetailUiEvent.OnPartPaymentAdjustmentSelected(it))
                },
                onSubmit = { viewModel.onEvent(LoanDetailUiEvent.OnSubmitRepayment) },
                onDismiss = {
                    showRepaymentDialog = false
                    viewModel.onEvent(LoanDetailUiEvent.OnClearRepaymentState)
                },
            )
        }
    }
}

private fun LoanPaymentItemUi.repaymentBreakdownLabel(): String? {
    if (principalComponentFormatted == null && interestComponentFormatted == null) return null
    return when (paymentType) {
        LoanPaymentType.TOP_UP ->
            "Principal increase ${principalComponentFormatted ?: paymentAmountFormatted}"
        else ->
            "Principal ${principalComponentFormatted ?: "₹0.00"} • Interest ${interestComponentFormatted ?: "₹0.00"}"
    }
}

@Composable
private fun PaymentHistoryCard(
    payment: LoanPaymentItemUi,
    onClick: (() -> Unit)? = null,
) {
    Card(
        modifier =
            Modifier
                .fillMaxWidth()
                .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text(
                        text = payment.paymentAmountFormatted,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MmrBrandColors.AmountPositive,
                    )
                    Text(
                        text = payment.paymentDateFormatted,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline,
                    )
                }

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer,
                ) {
                    Text(
                        text = payment.paymentTypeLabel,
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            payment.repaymentBreakdownLabel()?.let { breakdown ->
                Text(
                    text = breakdown,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.height(4.dp))
            }

            Text(
                text = "Remaining balance: ${payment.outstandingAfterFormatted}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            if (payment.linkedTransactionId != null) {
                Text(
                    text = "Tap to edit transaction",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Medium,
                )
            }

            payment.notes?.takeIf { it.isNotBlank() }?.let { note ->
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Note: $note",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline,
                )
            }
        }
    }
}
