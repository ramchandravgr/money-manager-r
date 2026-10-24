package com.mmr.feature.approval.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mmr.app.ui.state.MmrUiMessages
import com.mmr.core.common.validation.MmrValidation
import com.mmr.core.data.preferences.AppDisplaySettingsStore
import com.mmr.core.data.preferences.SmsScanSettingsStore
import com.mmr.core.database.entity.AccountEntity
import com.mmr.core.database.entity.CategoryEntity
import com.mmr.core.database.relation.TransactionWithDetails
import com.mmr.core.domain.model.ManualSmsDuplicateResolution
import com.mmr.core.domain.model.ManualSmsSubmitResult
import com.mmr.core.domain.repository.AccountRepository
import com.mmr.core.domain.repository.ApprovalRepository
import com.mmr.core.domain.repository.CategoryRepository
import com.mmr.core.domain.repository.TransactionRepository
import com.mmr.core.model.category.CategoryWithChildren
import com.mmr.core.model.enums.CategoryType
import com.mmr.core.model.enums.SmsApprovalKind
import com.mmr.core.model.enums.SmsApprovalState
import com.mmr.core.model.enums.TransactionType
import com.mmr.core.parser.model.SmsInboxScanSummary
import com.mmr.core.parser.model.SmsInstrument
import com.mmr.core.parser.model.SmsPaymentChannel
import com.mmr.core.parser.model.buildParseSourceLabel
import com.mmr.core.parser.permission.SmsPermissionHelper
import com.mmr.core.parser.scanner.LocalSmsScannerService
import com.mmr.feature.approval.model.ApprovalCandidateUi
import com.mmr.feature.approval.model.ApprovalCategoryOptionUi
import com.mmr.feature.approval.model.ApprovalCenterUiEvent
import com.mmr.feature.approval.model.ApprovalCenterUiState
import com.mmr.feature.approval.model.CardRefundApplyStep
import com.mmr.feature.approval.model.PendingCardRefundApplyPrompt
import com.mmr.feature.approval.model.PendingManualSmsDraft
import com.mmr.feature.approval.model.PendingSmsMergePrompt
import com.mmr.feature.home.HomePeriodUtils
import com.mmr.feature.home.TransactionGrouping
import com.mmr.feature.home.TransactionRowMapping
import com.mmr.feature.transaction.model.CategoryUi
import com.mmr.feature.transaction.model.SubcategoryUi
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.YearMonth
import java.time.ZoneId
import java.util.*
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class ApprovalCenterViewModel
    @Inject
    constructor(
        private val approvalRepository: ApprovalRepository,
        private val accountRepository: AccountRepository,
        private val categoryRepository: CategoryRepository,
        private val transactionRepository: TransactionRepository,
        private val smsScannerService: LocalSmsScannerService,
        private val smsScanSettingsStore: SmsScanSettingsStore,
        private val displaySettingsStore: AppDisplaySettingsStore,
        @ApplicationContext private val appContext: Context,
    ) : ViewModel() {
        private val zoneId = ZoneId.systemDefault()
        private val _retryKey = MutableStateFlow(0)
        private val _isLoading = MutableStateFlow(true)
        private val _isScanning = MutableStateFlow(false)
        private val _scanResultMessage = MutableStateFlow<String?>(null)
        private val _errorMessage = MutableStateFlow<String?>(null)
        private val _showManualEntryDialog = MutableStateFlow(false)
        private val _manualEntryError = MutableStateFlow<String?>(null)
        private val _pendingManualDuplicate = MutableStateFlow<PendingManualSmsDraft?>(null)
        private val _pendingSmsMerge = MutableStateFlow<PendingSmsMergePrompt?>(null)
        private val _pendingCardRefundApply = MutableStateFlow<PendingCardRefundApplyPrompt?>(null)
        private val _isSelectionMode = MutableStateFlow(false)
        private val _selectedApprovalIds = MutableStateFlow<Set<Long>>(emptySet())

        private val pendingApprovals: Flow<List<com.mmr.core.domain.model.PendingApprovalWithSms>> =
            _retryKey.flatMapLatest {
                approvalRepository
                    .getApprovalsByStateWithSms(SmsApprovalState.PENDING)
                    .onEach {
                        _errorMessage.value = null
                        _isLoading.value = false
                    }.catch { throwable ->
                        _errorMessage.value = MmrUiMessages.friendly(throwable.message, MmrUiMessages.GENERIC_LOAD)
                        _isLoading.value = false
                        emit(emptyList())
                    }
            }

        val uiState: StateFlow<ApprovalCenterUiState> =
            combine(
                pendingApprovals,
                categoryRepository.getActiveCategories(),
                accountRepository.getAccounts(),
                combine(
                    _isLoading,
                    _isScanning,
                    _scanResultMessage,
                    _errorMessage,
                ) { loading, scanning, scanMsg, error ->
                    ApprovalCenterMeta(loading, scanning, scanMsg, error)
                },
                combine(
                    combine(
                        _showManualEntryDialog,
                        _manualEntryError,
                        _pendingManualDuplicate,
                        _pendingSmsMerge,
                        _pendingCardRefundApply,
                    ) { showManual, manualErr, dup, smsMerge, cardRefund ->
                        ManualEntryMeta(showManual, manualErr, dup, smsMerge, cardRefund)
                    },
                    combine(_isSelectionMode, _selectedApprovalIds) { selectionMode, selectedIds ->
                        SelectionMeta(selectionMode, selectedIds)
                    },
                ) { manual, selection ->
                    OverlayMeta(manual = manual, selection = selection)
                },
            ) { approvals, categories, accounts, meta, overlay ->
                val manualMeta = overlay.manual
                val selectionMeta = overlay.selection
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
                    pendingManualDuplicate = manualMeta.duplicateDraft,
                    pendingSmsMerge = manualMeta.smsMerge,
                    pendingCardRefundApply = manualMeta.cardRefundApply,
                    mergeCategoryOptions = categories.map { ApprovalCategoryOptionUi(it.id, it.name) },
                    isSelectionMode = selectionMeta.isSelectionMode,
                    selectedApprovalIds = selectionMeta.selectedIds,
                    canLinkSelection = selectionMeta.isSelectionMode && selectionMeta.selectedIds.isNotEmpty(),
                    allPendingSelected =
                        selectionMeta.isSelectionMode &&
                            candidateUis.isNotEmpty() &&
                            selectionMeta.selectedIds.size == candidateUis.size,
                )
            }.stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = ApprovalCenterUiState(isLoading = true),
            )

        fun onEvent(event: ApprovalCenterUiEvent) {
            when (event) {
                is ApprovalCenterUiEvent.OnTabSelected -> Unit

                is ApprovalCenterUiEvent.OnEditClick -> Unit

                is ApprovalCenterUiEvent.OnApproveQuick -> {
                    viewModelScope.launch {
                        val approval = approvalRepository.getApprovalById(event.id)
                        if (approval?.approvalKind == SmsApprovalKind.CARD_REFUND) {
                            beginApplyRefund(event.id)
                        } else {
                            beginApprove(event.id)
                        }
                    }
                }

                is ApprovalCenterUiEvent.OnApplyRefundQuick -> {
                    viewModelScope.launch { beginApplyRefund(event.id) }
                }

                ApprovalCenterUiEvent.OnDismissCardRefundApply -> {
                    _pendingCardRefundApply.value = null
                }

                is ApprovalCenterUiEvent.OnCardRefundSelectPurchase -> {
                    _pendingCardRefundApply.update { current ->
                        current?.copy(
                            step = CardRefundApplyStep.CONFIRM_AMOUNT,
                            selectedPurchaseId = event.transactionId,
                        )
                    }
                }

                ApprovalCenterUiEvent.OnCardRefundBackToPicker -> {
                    _pendingCardRefundApply.update { current ->
                        current?.copy(step = CardRefundApplyStep.PICK_PURCHASE)
                    }
                }

                is ApprovalCenterUiEvent.OnCardRefundToggleFull -> {
                    _pendingCardRefundApply.update { current ->
                        current?.copy(applyFullRefund = event.full)
                    }
                }

                is ApprovalCenterUiEvent.OnCardRefundPartialAmountChanged -> {
                    _pendingCardRefundApply.update { current ->
                        current?.copy(partialRefundAmountText = event.text)
                    }
                }

                ApprovalCenterUiEvent.OnCardRefundConfirmApply -> {
                    confirmCardRefundApply()
                }

                ApprovalCenterUiEvent.OnCardRefundPickerPreviousMonth -> {
                    shiftCardRefundPickerMonth(-1)
                }

                ApprovalCenterUiEvent.OnCardRefundPickerNextMonth -> {
                    shiftCardRefundPickerMonth(1)
                }

                is ApprovalCenterUiEvent.OnCardRefundPickerNoteQueryChanged -> {
                    val current = _pendingCardRefundApply.value ?: return
                    viewModelScope.launch {
                        val next = current.copy(noteQuery = event.query, isLoadingCandidates = true)
                        _pendingCardRefundApply.value = next
                        _pendingCardRefundApply.value = refreshCardRefundPickerLedger(next)
                    }
                }

                ApprovalCenterUiEvent.OnDismissSmsMerge -> {
                    _pendingSmsMerge.value = null
                }

                ApprovalCenterUiEvent.OnApproveAsNewTransaction -> {
                    val approvalId = _pendingSmsMerge.value?.approvalIds?.singleOrNull() ?: return
                    _pendingSmsMerge.value = null
                    approveCandidate(approvalId)
                }

                is ApprovalCenterUiEvent.OnMergeSmsIntoTransaction -> {
                    val prompt = _pendingSmsMerge.value ?: return
                    _pendingSmsMerge.value = null
                    clearApprovalSelection()
                    mergeApprovals(
                        approvalIds = prompt.approvalIds,
                        transactionId = event.transactionId,
                        categoryId = prompt.selectedCategoryId ?: prompt.defaultCategoryId,
                        subcategoryId = prompt.selectedSubcategoryId,
                        useLedgerCategory = prompt.useLedgerCategory,
                    )
                }

                is ApprovalCenterUiEvent.OnLinkToExisting -> {
                    viewModelScope.launch {
                        val approval = approvalRepository.getApprovalById(event.id)
                        if (approval?.approvalKind == SmsApprovalKind.CARD_REFUND) {
                            beginApplyRefund(event.id)
                        } else {
                            beginLinkToExisting(listOf(event.id), userInitiated = true)
                        }
                    }
                }

                is ApprovalCenterUiEvent.OnApprovalLongPress -> {
                    if (!_isSelectionMode.value) {
                        _isSelectionMode.value = true
                        _selectedApprovalIds.value = setOf(event.id)
                    }
                }

                is ApprovalCenterUiEvent.OnApprovalSelectionToggle -> {
                    if (!_isSelectionMode.value) return
                    _selectedApprovalIds.update { current ->
                        if (event.id in current) current - event.id else current + event.id
                    }
                }

                ApprovalCenterUiEvent.OnExitApprovalSelection -> clearApprovalSelection()

                ApprovalCenterUiEvent.OnApprovalSelectAll -> {
                    if (!_isSelectionMode.value) return
                    val allIds =
                        uiState.value.candidates
                            .map { it.id }
                            .toSet()
                    _selectedApprovalIds.value =
                        if (_selectedApprovalIds.value.size == allIds.size) {
                            emptySet()
                        } else {
                            allIds
                        }
                }

                ApprovalCenterUiEvent.OnLinkSelectedApprovals -> {
                    val ids = _selectedApprovalIds.value.toList()
                    if (ids.isEmpty()) return
                    beginLinkToExisting(ids, userInitiated = true)
                }

                is ApprovalCenterUiEvent.OnMergePickerParentSelected -> {
                    _pendingSmsMerge.value =
                        _pendingSmsMerge.value?.copy(
                            mergePickerHighlightedCategoryId = event.categoryId,
                            useLedgerCategory = false,
                        )
                }

                is ApprovalCenterUiEvent.OnMergePickerSubcategorySelected -> {
                    _pendingSmsMerge.value =
                        _pendingSmsMerge.value?.let { current ->
                            current.copy(
                                selectedCategoryId = current.mergePickerHighlightedCategoryId,
                                selectedSubcategoryId = event.subcategoryId,
                                useLedgerCategory = false,
                            )
                        }
                }

                ApprovalCenterUiEvent.OnMergePickerConfirmParentOnly -> {
                    _pendingSmsMerge.value =
                        _pendingSmsMerge.value?.let { current ->
                            current.copy(
                                selectedCategoryId = current.mergePickerHighlightedCategoryId,
                                selectedSubcategoryId = null,
                                useLedgerCategory = false,
                            )
                        }
                }

                ApprovalCenterUiEvent.OnMergeUseLedgerCategory -> {
                    _pendingSmsMerge.value =
                        _pendingSmsMerge.value?.copy(
                            useLedgerCategory = true,
                        )
                }

                ApprovalCenterUiEvent.OnLinkPickerPreviousMonth -> {
                    shiftLinkPickerMonth(-1)
                }

                ApprovalCenterUiEvent.OnLinkPickerNextMonth -> {
                    shiftLinkPickerMonth(1)
                }

                is ApprovalCenterUiEvent.OnLinkPickerNoteQueryChanged -> {
                    val current = _pendingSmsMerge.value ?: return
                    viewModelScope.launch {
                        val next = current.copy(noteQuery = event.query, isLoadingCandidates = true)
                        _pendingSmsMerge.value = next
                        _pendingSmsMerge.value = refreshLinkPickerLedger(next)
                    }
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
                            note = event.note,
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
                        existingApprovalIdForRevert = null,
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
                        existingApprovalIdForRevert = draft.duplicateMatch.approvalId,
                    )
                }

                ApprovalCenterUiEvent.OnManualDuplicateAddAsNew -> {
                    val draft = _pendingManualDuplicate.value ?: return
                    submitManualSms(
                        sender = draft.sender,
                        body = draft.body,
                        timestampMillis = draft.timestampMillis,
                        resolution = ManualSmsDuplicateResolution.ADD_AS_NEW_TRANSACTION,
                        existingApprovalIdForRevert = null,
                    )
                }
            }
        }

        private fun submitManualSms(
            sender: String,
            body: String,
            timestampMillis: Long,
            resolution: ManualSmsDuplicateResolution?,
            existingApprovalIdForRevert: Long?,
        ) {
            viewModelScope.launch {
                try {
                    when (
                        val result =
                            approvalRepository.submitManualSms(
                                sender = sender,
                                body = body,
                                timestampMillis = timestampMillis,
                                resolution = resolution,
                                existingApprovalIdForRevert = existingApprovalIdForRevert,
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
                            _pendingManualDuplicate.value =
                                PendingManualSmsDraft(
                                    sender = sender,
                                    body = body,
                                    timestampMillis = timestampMillis,
                                    duplicateMatch = result.match,
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

        private fun beginApprove(id: Long) {
            approveCandidate(id)
        }

        private fun beginLinkToExisting(
            approvalIds: List<Long>,
            userInitiated: Boolean,
        ) {
            viewModelScope.launch {
                try {
                    openSmsLinkPicker(approvalIds = approvalIds, userInitiated = userInitiated)
                } catch (e: Exception) {
                    _scanResultMessage.value = MmrUiMessages.friendly(e.message, MmrUiMessages.GENERIC_ACTION)
                }
            }
        }

        private suspend fun openSmsLinkPicker(
            approvalIds: List<Long>,
            userInitiated: Boolean,
        ) {
            val primaryId = approvalIds.firstOrNull() ?: return
            val approval = approvalRepository.getApprovalById(primaryId)
            val defaultCategoryId = approval?.extractedCategoryId
            val mergeType = approval?.extractedType ?: TransactionType.EXPENSE
            val tree = categoryRepository.observeActiveCategoryTree().first()
            val mergeCategories = categoriesForType(tree, mergeType)
            val resolvedCategoryId = resolveCategorySelection(mergeCategories, defaultCategoryId)
            val defaultCategoryName =
                mergeCategories.firstOrNull { it.id == resolvedCategoryId }?.name ?: "Uncategorized"
            val anchorMillis =
                approval?.extractedDateTime?.takeIf { MmrValidation.isValidEpochMillis(it) }
                    ?: System.currentTimeMillis()
            val yearMonth = YearMonth.from(Instant.ofEpochMilli(anchorMillis).atZone(zoneId).toLocalDate())
            val loading =
                PendingSmsMergePrompt(
                    approvalIds = approvalIds,
                    defaultCategoryId = defaultCategoryId,
                    defaultCategoryName = defaultCategoryName,
                    selectedCategoryId = resolvedCategoryId,
                    selectedSubcategoryId = null,
                    mergePickerHighlightedCategoryId = resolvedCategoryId,
                    mergeCategories = mergeCategories,
                    userInitiatedLink = userInitiated,
                    isLoadingCandidates = true,
                    pickerYear = yearMonth.year,
                    pickerMonth = yearMonth.monthValue,
                    monthTitle = HomePeriodUtils.monthTitle(yearMonth),
                )
            _pendingSmsMerge.value = loading
            _pendingSmsMerge.value = refreshLinkPickerLedger(loading)
        }

        private fun shiftLinkPickerMonth(delta: Int) {
            val current = _pendingSmsMerge.value ?: return
            val yearMonth = YearMonth.of(current.pickerYear, current.pickerMonth).plusMonths(delta.toLong())
            viewModelScope.launch {
                val loading =
                    current.copy(
                        pickerYear = yearMonth.year,
                        pickerMonth = yearMonth.monthValue,
                        monthTitle = HomePeriodUtils.monthTitle(yearMonth),
                        isLoadingCandidates = true,
                    )
                _pendingSmsMerge.value = loading
                _pendingSmsMerge.value = refreshLinkPickerLedger(loading)
            }
        }

        private suspend fun refreshLinkPickerLedger(prompt: PendingSmsMergePrompt): PendingSmsMergePrompt {
            val display = displaySettingsStore.snapshot()
            val yearMonth = YearMonth.of(prompt.pickerYear, prompt.pickerMonth)
            val (start, end) =
                HomePeriodUtils.epochRangeMillis(
                    yearMonth,
                    zoneId,
                    display.monthStartDay,
                )
            val items =
                transactionRepository
                    .getFilteredTransactions(
                        startDate = start,
                        endDate = end,
                        accountId = null,
                        categoryId = null,
                        subcategoryId = null,
                        type = null,
                    ).first()
            val needle = prompt.noteQuery.trim().lowercase(Locale.getDefault())
            val filtered = items.filter { matchesLinkPickerNote(it, needle) }
            val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())
            val grouped =
                TransactionGrouping.groupByDate(
                    items = filtered,
                    zoneId = zoneId,
                    withinDayAscending = display.timeDisplay.ascending,
                    rowMapper = { item ->
                        TransactionRowMapping.fromDetails(
                            item = item,
                            timeFormat = timeFormat,
                            showDescription = display.showDescription,
                            showTime = display.timeDisplay.showTime,
                            currencySymbol = display.mainCurrencySymbol,
                        )
                    },
                )
            return prompt.copy(
                groupedTransactions = grouped,
                isEmptyMonth = grouped.isEmpty(),
                isLoadingCandidates = false,
                monthTitle = HomePeriodUtils.monthTitle(yearMonth),
            )
        }

        private fun matchesLinkPickerNote(
            item: TransactionWithDetails,
            needle: String,
        ): Boolean {
            if (needle.isEmpty()) return true
            val hay =
                buildString {
                    append(item.transaction.note.orEmpty())
                    append(' ')
                    append(item.category?.name.orEmpty())
                    append(' ')
                    append(item.account?.name.orEmpty())
                }.lowercase(Locale.getDefault())
            return hay.contains(needle)
        }

        private fun mergeApprovals(
            approvalIds: List<Long>,
            transactionId: Long,
            categoryId: Long?,
            subcategoryId: Long?,
            useLedgerCategory: Boolean,
        ) {
            viewModelScope.launch {
                try {
                    approvalRepository.mergeApprovalsWithTransaction(
                        approvalIds = approvalIds,
                        transactionId = transactionId,
                        categoryId = categoryId,
                        subcategoryId = subcategoryId,
                        useLedgerCategory = useLedgerCategory,
                    )
                    val count = approvalIds.size
                    _scanResultMessage.value =
                        if (count > 1) {
                            "$count SMS linked to the same transaction."
                        } else {
                            "SMS linked to existing transaction."
                        }
                } catch (e: Exception) {
                    _scanResultMessage.value = MmrUiMessages.friendly(e.message, MmrUiMessages.GENERIC_ACTION)
                }
            }
        }

        private fun clearApprovalSelection() {
            _isSelectionMode.value = false
            _selectedApprovalIds.value = emptySet()
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
                        fallbackCategoryId = fallbackCategoryId,
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
                    if (
                        parsedCandidates.isEmpty() &&
                        scanSummary.cardRefunds.isEmpty() &&
                        scanSummary.informationalCount == 0
                    ) {
                        _scanResultMessage.value =
                            "No transaction SMS found in the last $months month(s)."
                    } else {
                        val cleared = approvalRepository.reconcilePendingWithCurrentParser()
                        val addedTxn = approvalRepository.insertCandidates(parsedCandidates)
                        val addedRefund = approvalRepository.insertCandidates(scanSummary.cardRefunds)
                        _scanResultMessage.value =
                            buildScanResultMessage(
                                added = addedTxn + addedRefund,
                                txnCount = parsedCandidates.size + scanSummary.cardRefunds.size,
                                summary = scanSummary,
                                clearedPending = cleared,
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
            summary: SmsInboxScanSummary,
            clearedPending: Int = 0,
        ): String {
            val parts = mutableListOf<String>()
            if (clearedPending > 0) {
                parts.add(
                    "Removed $clearedPending outdated pending item(s) (declined, mandate, or non-transaction SMS).",
                )
            }
            when {
                added > 0 -> parts.add("Added $added new pending transaction(s).")
                txnCount > 0 -> parts.add("All $txnCount matched transaction(s) were already imported.")
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
            if (summary.cardPaymentNotices > 0) {
                parts.add("${summary.cardPaymentNotices} credit card payment(s) skipped.")
            }
            if (summary.cardRefundNotices > 0) {
                parts.add("${summary.cardRefundNotices} card refund(s) queued to apply.")
            }
            if (summary.duplicateSuppressed > 0) {
                parts.add("${summary.duplicateSuppressed} duplicate HP Pay/card SMS merged.")
            }
            return parts.joinToString(" ")
        }

        private fun com.mmr.core.domain.model.PendingApprovalWithSms.toUi(
            categoriesById: Map<Long, CategoryEntity>,
            accountsById: Map<Long, AccountEntity>,
        ): ApprovalCandidateUi {
            val entity = approval
            val dateFormat = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
            val isIncome = entity.extractedType == TransactionType.INCOME
            val sign = if (isIncome) "+" else "-"
            val confidence =
                when {
                    entity.confidenceScore >= 80 -> "High (${entity.confidenceScore}%)"
                    entity.confidenceScore >= 50 -> "Medium (${entity.confidenceScore}%)"
                    else -> "Low (${entity.confidenceScore}%)"
                }
            val isCardRefund = entity.approvalKind == SmsApprovalKind.CARD_REFUND
            val typeLabel =
                if (isCardRefund) {
                    "Card refund"
                } else {
                    entity.extractedType.name
                        .lowercase(Locale.getDefault())
                        .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
                }
            val statusLabel =
                entity.approvalState.name
                    .lowercase(Locale.getDefault())
                    .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }

            val channel =
                entity.extractedPaymentChannel?.let {
                    runCatching { SmsPaymentChannel.valueOf(it) }.getOrNull()
                }
            val instrument =
                entity.extractedInstrument?.let {
                    runCatching { SmsInstrument.valueOf(it) }.getOrNull()
                }
            val parseSourceLabel =
                buildParseSourceLabel(
                    templateId = entity.extractedTemplateId,
                    paymentChannel = channel,
                    instrument = instrument,
                )

            return ApprovalCandidateUi(
                id = entity.id,
                sender = entity.extractedSender,
                amountFormatted =
                    if (isCardRefund) {
                        com.mmr.core.common.display.MmrMoneyFormat
                            .format(entity.extractedAmount, "₹")
                    } else {
                        "$sign${com.mmr.core.common.display.MmrMoneyFormat.format(entity.extractedAmount, "₹")}"
                    },
                amount = entity.extractedAmount,
                type = entity.extractedType,
                isCardRefund = isCardRefund,
                typeLabel = typeLabel,
                merchant = entity.extractedMerchant,
                note = entity.extractedNote ?: entity.extractedMerchant ?: entity.extractedSender,
                dateLabel =
                    if (MmrValidation.isValidEpochMillis(entity.extractedDateTime)) {
                        dateFormat.format(Date(entity.extractedDateTime))
                    } else {
                        "Unknown date"
                    },
                categoryName = entity.extractedCategoryId?.let { categoriesById[it]?.name } ?: "Uncategorized",
                accountName = entity.extractedAccountId?.let { accountsById[it]?.name } ?: "Not assigned",
                statusLabel = statusLabel,
                confidenceLabel = confidence,
                parseSourceLabel = parseSourceLabel,
                rawSnippet = entity.rawBodySnippet,
                smsFullBody = smsBody,
                state = entity.approvalState,
            )
        }

        private fun categoriesForType(
            tree: List<CategoryWithChildren>,
            transactionType: TransactionType,
        ): List<CategoryUi> {
            val filtered =
                when (transactionType) {
                    TransactionType.EXPENSE -> tree.filter { it.category.type == CategoryType.EXPENSE }
                    TransactionType.INCOME -> tree.filter { it.category.type == CategoryType.INCOME }
                    TransactionType.TRANSFER -> tree.filter { it.category.type == CategoryType.EXPENSE }
                    else -> tree
                }
            return filtered.map { node ->
                CategoryUi(
                    id = node.category.id.value,
                    name = node.category.name,
                    colorHex = node.category.color.hex,
                    iconName = node.category.iconName,
                    subcategories =
                        node.subcategories.map { sub ->
                            SubcategoryUi(id = sub.id.value, name = sub.name)
                        },
                )
            }
        }

        private fun resolveCategorySelection(
            categories: List<CategoryUi>,
            preferredId: Long?,
        ): Long? {
            if (preferredId != null && categories.any { it.id == preferredId }) return preferredId
            return categories.firstOrNull()?.id
        }

        private suspend fun beginApplyRefund(approvalId: Long) {
            val approval = approvalRepository.getApprovalById(approvalId) ?: return
            if (approval.approvalKind != SmsApprovalKind.CARD_REFUND) return
            val anchorMillis =
                approval.extractedDateTime.takeIf { MmrValidation.isValidEpochMillis(it) }
                    ?: System.currentTimeMillis()
            val yearMonth = YearMonth.from(Instant.ofEpochMilli(anchorMillis).atZone(zoneId).toLocalDate())
            val loading =
                PendingCardRefundApplyPrompt(
                    approvalId = approvalId,
                    refundAmount = approval.extractedAmount,
                    refundAmountLabel =
                        com.mmr.core.common.display.MmrMoneyFormat
                            .format(approval.extractedAmount, "₹"),
                    smsSnippet = approval.rawBodySnippet,
                    pickerYear = yearMonth.year,
                    pickerMonth = yearMonth.monthValue,
                    monthTitle = HomePeriodUtils.monthTitle(yearMonth),
                    partialRefundAmountText = approval.extractedAmount.toString(),
                    isLoadingCandidates = true,
                )
            _pendingCardRefundApply.value = loading
            _pendingCardRefundApply.value = refreshCardRefundPickerLedger(loading)
        }

        private fun shiftCardRefundPickerMonth(delta: Int) {
            val current = _pendingCardRefundApply.value ?: return
            val yearMonth = YearMonth.of(current.pickerYear, current.pickerMonth).plusMonths(delta.toLong())
            viewModelScope.launch {
                val loading =
                    current.copy(
                        pickerYear = yearMonth.year,
                        pickerMonth = yearMonth.monthValue,
                        monthTitle = HomePeriodUtils.monthTitle(yearMonth),
                        isLoadingCandidates = true,
                    )
                _pendingCardRefundApply.value = loading
                _pendingCardRefundApply.value = refreshCardRefundPickerLedger(loading)
            }
        }

        private suspend fun refreshCardRefundPickerLedger(
            prompt: PendingCardRefundApplyPrompt,
        ): PendingCardRefundApplyPrompt {
            val display = displaySettingsStore.snapshot()
            val yearMonth = YearMonth.of(prompt.pickerYear, prompt.pickerMonth)
            val (start, end) =
                HomePeriodUtils.epochRangeMillis(
                    yearMonth,
                    zoneId,
                    display.monthStartDay,
                )
            val items =
                transactionRepository
                    .getFilteredTransactions(
                        startDate = start,
                        endDate = end,
                        accountId = null,
                        categoryId = null,
                        subcategoryId = null,
                        type = TransactionType.EXPENSE,
                    ).first()
            val needle = prompt.noteQuery.trim().lowercase(Locale.getDefault())
            val filtered = items.filter { matchesLinkPickerNote(it, needle) }
            val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())
            val grouped =
                TransactionGrouping.groupByDate(
                    items = filtered,
                    zoneId = zoneId,
                    withinDayAscending = display.timeDisplay.ascending,
                    rowMapper = { item ->
                        TransactionRowMapping.fromDetails(
                            item = item,
                            timeFormat = timeFormat,
                            showDescription = display.showDescription,
                            showTime = display.timeDisplay.showTime,
                            currencySymbol = display.mainCurrencySymbol,
                        )
                    },
                )
            return prompt.copy(
                groupedTransactions = grouped,
                isEmptyMonth = grouped.isEmpty(),
                isLoadingCandidates = false,
                monthTitle = HomePeriodUtils.monthTitle(yearMonth),
            )
        }

        private fun confirmCardRefundApply() {
            val prompt = _pendingCardRefundApply.value ?: return
            val purchaseId = prompt.selectedPurchaseId ?: return
            val amount =
                if (prompt.applyFullRefund) {
                    prompt.refundAmount
                } else {
                    prompt.partialRefundAmountText
                        .replace(",", "")
                        .trim()
                        .toDoubleOrNull()
                        ?: run {
                            _scanResultMessage.value = "Enter a valid refund amount."
                            return
                        }
                }
            viewModelScope.launch {
                try {
                    approvalRepository.applyCardRefund(
                        approvalId = prompt.approvalId,
                        purchaseTransactionId = purchaseId,
                        refundAmount = amount,
                    )
                    _pendingCardRefundApply.value = null
                    _scanResultMessage.value = "Refund applied to purchase."
                } catch (e: Exception) {
                    _scanResultMessage.value = MmrUiMessages.friendly(e.message, MmrUiMessages.GENERIC_ACTION)
                }
            }
        }
    }

private data class ApprovalCenterMeta(
    val loading: Boolean,
    val scanning: Boolean,
    val scanMessage: String?,
    val errorMessage: String?,
)

private data class ManualEntryMeta(
    val showDialog: Boolean,
    val error: String?,
    val duplicateDraft: PendingManualSmsDraft?,
    val smsMerge: PendingSmsMergePrompt?,
    val cardRefundApply: PendingCardRefundApplyPrompt?,
)

private data class SelectionMeta(
    val isSelectionMode: Boolean,
    val selectedIds: Set<Long>,
)

private data class OverlayMeta(
    val manual: ManualEntryMeta,
    val selection: SelectionMeta,
)
