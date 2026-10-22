package com.mmr.feature.approval.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mmr.app.ui.state.MmrUiMessages
import com.mmr.core.database.entity.AccountEntity
import com.mmr.core.database.entity.CategoryEntity
import com.mmr.core.common.validation.MmrValidation
import com.mmr.core.database.entity.TransactionApprovalEntity
import com.mmr.core.domain.repository.AccountRepository
import com.mmr.core.domain.repository.ApprovalRepository
import com.mmr.core.domain.repository.CategoryRepository
import com.mmr.core.model.enums.SmsApprovalState
import com.mmr.core.model.enums.TransactionType
import com.mmr.core.data.preferences.SmsScanSettingsStore
import com.mmr.core.parser.scanner.LocalSmsScannerService
import dagger.hilt.android.qualifiers.ApplicationContext
import android.content.Context
import com.mmr.core.parser.model.SmsInboxScanSummary
import com.mmr.core.parser.model.SmsInstrument
import com.mmr.core.parser.model.SmsPaymentChannel
import com.mmr.core.parser.model.buildParseSourceLabel
import com.mmr.core.parser.permission.SmsPermissionHelper
import com.mmr.core.domain.model.ManualSmsDuplicateResolution
import com.mmr.core.domain.model.ManualSmsSubmitResult
import com.mmr.feature.approval.model.ApprovalCandidateUi
import com.mmr.feature.approval.model.ApprovalCenterUiEvent
import com.mmr.feature.approval.model.ApprovalCenterUiState
import com.mmr.feature.approval.model.PendingManualSmsDraft
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class ApprovalCenterViewModel @Inject constructor(
    private val approvalRepository: ApprovalRepository,
    private val accountRepository: AccountRepository,
    private val categoryRepository: CategoryRepository,
    private val smsScannerService: LocalSmsScannerService,
    private val smsScanSettingsStore: SmsScanSettingsStore,
    @ApplicationContext private val appContext: Context
) : ViewModel() {

    private val _retryKey = MutableStateFlow(0)
    private val _isLoading = MutableStateFlow(true)
    private val _isScanning = MutableStateFlow(false)
    private val _scanResultMessage = MutableStateFlow<String?>(null)
    private val _errorMessage = MutableStateFlow<String?>(null)
    private val _showManualEntryDialog = MutableStateFlow(false)
    private val _manualEntryError = MutableStateFlow<String?>(null)
    private val _pendingManualDuplicate = MutableStateFlow<PendingManualSmsDraft?>(null)

    private val pendingApprovals: Flow<List<TransactionApprovalEntity>> = _retryKey.flatMapLatest {
        approvalRepository.getApprovalsByState(SmsApprovalState.PENDING)
            .onEach {
                _errorMessage.value = null
                _isLoading.value = false
            }
            .catch { throwable ->
                _errorMessage.value = MmrUiMessages.friendly(throwable.message, MmrUiMessages.GENERIC_LOAD)
                _isLoading.value = false
                emit(emptyList())
            }
    }

    val uiState: StateFlow<ApprovalCenterUiState> = combine(
        pendingApprovals,
        categoryRepository.getActiveCategories(),
        accountRepository.getAccounts(),
        combine(_isLoading, _isScanning, _scanResultMessage, _errorMessage) { loading, scanning, scanMsg, error ->
            ApprovalCenterMeta(loading, scanning, scanMsg, error)
        },
        combine(_showManualEntryDialog, _manualEntryError, _pendingManualDuplicate) { showManual, manualErr, dup ->
            ManualEntryMeta(showManual, manualErr, dup)
        }
    ) { approvals, categories, accounts, meta, manualMeta ->
        val loading = meta.loading
        val scanning = meta.scanning
        val scanMsg = meta.scanMessage
        val error = meta.errorMessage

        val categoriesById = categories.associateBy { it.id }
        val accountsById = accounts.associateBy { it.id }
        val candidateUis = approvals.map { it.toUi(categoriesById, accountsById) }

        ApprovalCenterUiState(
            isLoading = loading,
            isScanning = scanning,
            candidates = candidateUis,
            selectedTab = SmsApprovalState.PENDING,
            pendingCount = candidateUis.size,
            scanResultMessage = scanMsg,
            errorMessage = error,
            isEmpty = !loading && error == null && candidateUis.isEmpty(),
            showManualEntryDialog = manualMeta.showDialog,
            manualEntryError = manualMeta.error,
            pendingManualDuplicate = manualMeta.duplicateDraft
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ApprovalCenterUiState(isLoading = true)
    )

    fun onEvent(event: ApprovalCenterUiEvent) {
        when (event) {
            is ApprovalCenterUiEvent.OnTabSelected -> Unit

            is ApprovalCenterUiEvent.OnEditClick -> Unit

            is ApprovalCenterUiEvent.OnApproveQuick -> {
                approveCandidate(event.id)
            }

            is ApprovalCenterUiEvent.OnRejectQuick -> {
                viewModelScope.launch {
                    try {
                        approvalRepository.rejectCandidate(event.id)
                    } catch (e: Exception) {
                        _scanResultMessage.value = MmrUiMessages.friendly(e.message, MmrUiMessages.GENERIC_ACTION)
                    }
                }
            }

            is ApprovalCenterUiEvent.OnMarkDuplicateQuick -> {
                viewModelScope.launch {
                    approvalRepository.markDuplicate(event.id)
                }
            }

            is ApprovalCenterUiEvent.OnUpdateCandidate -> {
                viewModelScope.launch {
                    approvalRepository.updateApproval(
                        id = event.id,
                        amount = event.amount,
                        type = event.type,
                        categoryId = event.categoryId,
                        accountId = event.accountId,
                        note = event.note
                    )
                }
            }

            ApprovalCenterUiEvent.OnScanInboxClick -> {
                scanDeviceSms()
            }

            ApprovalCenterUiEvent.OnClearScanMessage -> {
                _scanResultMessage.value = null
            }

            ApprovalCenterUiEvent.OnRetryClick -> {
                _errorMessage.value = null
                _isLoading.value = true
                _retryKey.value = _retryKey.value + 1
            }

            ApprovalCenterUiEvent.OnAddManualSmsClick -> {
                _manualEntryError.value = null
                _showManualEntryDialog.value = true
            }

            ApprovalCenterUiEvent.OnDismissManualEntry -> {
                _showManualEntryDialog.value = false
                _manualEntryError.value = null
            }

            is ApprovalCenterUiEvent.OnSubmitManualSms -> {
                submitManualSms(
                    sender = event.sender,
                    body = event.body,
                    timestampMillis = event.timestampMillis,
                    resolution = null,
                    existingApprovalIdForRevert = null
                )
            }

            ApprovalCenterUiEvent.OnDismissManualDuplicate -> {
                _pendingManualDuplicate.value = null
            }

            ApprovalCenterUiEvent.OnManualDuplicateRevertPrior -> {
                val draft = _pendingManualDuplicate.value ?: return
                submitManualSms(
                    sender = draft.sender,
                    body = draft.body,
                    timestampMillis = draft.timestampMillis,
                    resolution = ManualSmsDuplicateResolution.REVERT_PRIOR_AND_USE_NEW,
                    existingApprovalIdForRevert = draft.duplicateMatch.approvalId
                )
            }

            ApprovalCenterUiEvent.OnManualDuplicateAddAsNew -> {
                val draft = _pendingManualDuplicate.value ?: return
                submitManualSms(
                    sender = draft.sender,
                    body = draft.body,
                    timestampMillis = draft.timestampMillis,
                    resolution = ManualSmsDuplicateResolution.ADD_AS_NEW_TRANSACTION,
                    existingApprovalIdForRevert = null
                )
            }
        }
    }

    private fun submitManualSms(
        sender: String,
        body: String,
        timestampMillis: Long,
        resolution: ManualSmsDuplicateResolution?,
        existingApprovalIdForRevert: Long?
    ) {
        viewModelScope.launch {
            try {
                when (
                    val result = approvalRepository.submitManualSms(
                        sender = sender,
                        body = body,
                        timestampMillis = timestampMillis,
                        resolution = resolution,
                        existingApprovalIdForRevert = existingApprovalIdForRevert
                    )
                ) {
                    is ManualSmsSubmitResult.ParseFailed -> {
                        if (resolution == null && _showManualEntryDialog.value) {
                            _manualEntryError.value = result.reason
                        } else {
                            _scanResultMessage.value = result.reason
                        }
                    }

                    is ManualSmsSubmitResult.NeedsDuplicateDecision -> {
                        _showManualEntryDialog.value = false
                        _manualEntryError.value = null
                        _pendingManualDuplicate.value = PendingManualSmsDraft(
                            sender = sender,
                            body = body,
                            timestampMillis = timestampMillis,
                            duplicateMatch = result.match
                        )
                    }

                    is ManualSmsSubmitResult.Added -> {
                        _showManualEntryDialog.value = false
                        _manualEntryError.value = null
                        _pendingManualDuplicate.value = null
                        _scanResultMessage.value = "Manual SMS added to pending list."
                    }
                }
            } catch (e: Exception) {
                _scanResultMessage.value = MmrUiMessages.friendly(e.message, MmrUiMessages.GENERIC_ACTION)
            }
        }
    }

    private fun approveCandidate(id: Long) {
        viewModelScope.launch {
            try {
                val accounts = accountRepository.getAccounts().firstOrNull() ?: emptyList()
                val fallbackAccountId = accounts.firstOrNull()?.id ?: 1L
                val categories = categoryRepository.getActiveCategories().firstOrNull() ?: emptyList()
                val fallbackCategoryId = categories.firstOrNull()?.id

                approvalRepository.approveCandidate(
                    id = id,
                    fallbackAccountId = fallbackAccountId,
                    fallbackCategoryId = fallbackCategoryId
                )
            } catch (e: Exception) {
                _scanResultMessage.value = MmrUiMessages.friendly(e.message, MmrUiMessages.GENERIC_ACTION)
            }
        }
    }

    private fun scanDeviceSms() {
        viewModelScope.launch {
            _isScanning.value = true
            _scanResultMessage.value = null
            try {
                if (!SmsPermissionHelper.hasSmsPermission(appContext)) {
                    _scanResultMessage.value =
                        "SMS permission is required. Allow READ_SMS to scan your inbox."
                    return@launch
                }
                val months = smsScanSettingsStore.getScanMonths()
                val scanSummary = smsScannerService.scanInbox(monthsBack = months)
                val parsedCandidates = scanSummary.transactions
                if (parsedCandidates.isEmpty() && scanSummary.informationalCount == 0) {
                    _scanResultMessage.value =
                        "No transaction SMS found in the last $months month(s)."
                } else {
                    val added = approvalRepository.insertCandidates(parsedCandidates)
                    _scanResultMessage.value = buildScanResultMessage(
                        added = added,
                        txnCount = parsedCandidates.size,
                        summary = scanSummary
                    )
                }
            } catch (e: Exception) {
                _scanResultMessage.value = "Error scanning SMS: ${e.localizedMessage}"
            } finally {
                _isScanning.value = false
            }
        }
    }

    private fun buildScanResultMessage(
        added: Int,
        txnCount: Int,
        summary: SmsInboxScanSummary
    ): String {
        val parts = mutableListOf<String>()
        when {
            added > 0 -> parts.add("Added $added new pending transaction(s).")
            txnCount > 0 -> parts.add("All ${txnCount} matched transaction(s) were already imported.")
            else -> parts.add("No new transactions to import.")
        }
        if (summary.mandateNotices > 0) {
            parts.add("${summary.mandateNotices} upcoming mandate notice(s) skipped.")
        }
        if (summary.statementNotices > 0) {
            parts.add("${summary.statementNotices} statement notice(s) skipped.")
        }
        if (summary.investmentNotices > 0) {
            parts.add("${summary.investmentNotices} IPO/SIP hold notice(s) skipped.")
        }
        if (summary.duplicateSuppressed > 0) {
            parts.add("${summary.duplicateSuppressed} duplicate HP Pay/card SMS merged.")
        }
        return parts.joinToString(" ")
    }

    private fun TransactionApprovalEntity.toUi(
        categoriesById: Map<Long, CategoryEntity>,
        accountsById: Map<Long, AccountEntity>
    ): ApprovalCandidateUi {
        val dateFormat = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
        val isIncome = extractedType == TransactionType.INCOME
        val sign = if (isIncome) "+" else "-"
        val confidence = when {
            confidenceScore >= 80 -> "High (${confidenceScore}%)"
            confidenceScore >= 50 -> "Medium (${confidenceScore}%)"
            else -> "Low (${confidenceScore}%)"
        }
        val typeLabel = extractedType.name.lowercase(Locale.getDefault())
            .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
        val statusLabel = approvalState.name.lowercase(Locale.getDefault())
            .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }

        val channel = extractedPaymentChannel?.let {
            runCatching { SmsPaymentChannel.valueOf(it) }.getOrNull()
        }
        val instrument = extractedInstrument?.let {
            runCatching { SmsInstrument.valueOf(it) }.getOrNull()
        }
        val parseSourceLabel = buildParseSourceLabel(
            templateId = extractedTemplateId,
            paymentChannel = channel,
            instrument = instrument
        )

        return ApprovalCandidateUi(
            id = id,
            sender = extractedSender,
            amountFormatted = "$sign₹${"%.2f".format(extractedAmount)}",
            amount = extractedAmount,
            type = extractedType,
            typeLabel = typeLabel,
            merchant = extractedMerchant,
            note = extractedNote ?: extractedMerchant ?: extractedSender,
            dateLabel = if (MmrValidation.isValidEpochMillis(extractedDateTime)) {
                dateFormat.format(Date(extractedDateTime))
            } else {
                "Unknown date"
            },
            categoryName = extractedCategoryId?.let { categoriesById[it]?.name } ?: "Uncategorized",
            accountName = extractedAccountId?.let { accountsById[it]?.name } ?: "Not assigned",
            statusLabel = statusLabel,
            confidenceLabel = confidence,
            parseSourceLabel = parseSourceLabel,
            rawSnippet = rawBodySnippet,
            state = approvalState
        )
    }
}

private data class ApprovalCenterMeta(
    val loading: Boolean,
    val scanning: Boolean,
    val scanMessage: String?,
    val errorMessage: String?
)

private data class ManualEntryMeta(
    val showDialog: Boolean,
    val error: String?,
    val duplicateDraft: PendingManualSmsDraft?
)
