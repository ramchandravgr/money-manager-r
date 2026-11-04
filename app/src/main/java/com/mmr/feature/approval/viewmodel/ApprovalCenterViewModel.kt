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
import com.mmr.core.domain.model.ManualSmsParseConstants
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
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.YearMonth
import java.time.ZoneId
import java.util.Date
import java.util.Locale
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
        private val approvalRetryKey = MutableStateFlow(0)
        private val approvalsLoading = MutableStateFlow(true)
        private val smsScanning = MutableStateFlow(false)
        private val scanResultMessageState = MutableStateFlow<String?>(null)
        private val approvalsErrorMessage = MutableStateFlow<String?>(null)
        private val showManualEntryDialogState = MutableStateFlow(false)
        private val manualEntryErrorState = MutableStateFlow<String?>(null)
        private val pendingManualDuplicateState = MutableStateFlow<PendingManualSmsDraft?>(null)
        private val pendingSmsMergeState = MutableStateFlow<PendingSmsMergePrompt?>(null)
        private val pendingCardRefundApplyState = MutableStateFlow<PendingCardRefundApplyPrompt?>(null)
        private val selectionModeState = MutableStateFlow(false)
        private val selectedApprovalIdsState = MutableStateFlow<Set<Long>>(emptySet())
        private val openManualEntryForApprovalIdFlow = MutableSharedFlow<Long>(extraBufferCapacity = 1)

        val openManualEntryForApprovalId = openManualEntryForApprovalIdFlow.asSharedFlow()

        private val pendingApprovals: Flow<List<com.mmr.core.domain.model.PendingApprovalWithSms>> =
            approvalRetryKey.flatMapLatest {
                approvalRepository
                    .getApprovalsByStateWithSms(SmsApprovalState.PENDING)
                    .onEach {
                        approvalsErrorMessage.value = null
                        approvalsLoading.value = false
                    }.catch { throwable ->
                        approvalsErrorMessage.value =
                            MmrUiMessages.friendly(
                                throwable.message,
                                MmrUiMessages.GENERIC_LOAD,
                            )
                        approvalsLoading.value = false
                        emit(emptyList())
                    }
            }

        val uiState: StateFlow<ApprovalCenterUiState> =
            combine(
                pendingApprovals,
                categoryRepository.getActiveCategories(),
                accountRepository.getAccounts(),
                combine(
                    approvalsLoading,
                    smsScanning,
                    scanResultMessageState,
                    approvalsErrorMessage,
                ) { loading, scanning, scanMsg, error ->
                    ApprovalCenterMeta(loading, scanning, scanMsg, error)
                },
                combine(
                    combine(
                        showManualEntryDialogState,
                        manualEntryErrorState,
                        pendingManualDuplicateState,
                        pendingSmsMergeState,
                        pendingCardRefundApplyState,
                    ) { showManual, manualErr, dup, smsMerge, cardRefund ->
                        ManualEntryMeta(showManual, manualErr, dup, smsMerge, cardRefund)
                    },
                    combine(selectionModeState, selectedApprovalIdsState) { selectionMode, selectedIds ->
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
                    pendingCardRefundApplyState.value = null
                }

                is ApprovalCenterUiEvent.OnCardRefundSelectPurchase -> {
                    pendingCardRefundApplyState.update { current ->
                        current?.copy(
                            step = CardRefundApplyStep.CONFIRM_AMOUNT,
                            selectedPurchaseId = event.transactionId,
                        )
                    }
                }

                ApprovalCenterUiEvent.OnCardRefundBackToPicker -> {
                    pendingCardRefundApplyState.update { current ->
                        current?.copy(step = CardRefundApplyStep.PICK_PURCHASE)
                    }
                }

                is ApprovalCenterUiEvent.OnCardRefundToggleFull -> {
                    pendingCardRefundApplyState.update { current ->
                        current?.copy(applyFullRefund = event.full)
                    }
                }

                is ApprovalCenterUiEvent.OnCardRefundPartialAmountChanged -> {
                    pendingCardRefundApplyState.update { current ->
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
                    val current = pendingCardRefundApplyState.value ?: return
                    viewModelScope.launch {
                        val next = current.copy(noteQuery = event.query, isLoadingCandidates = true)
                        pendingCardRefundApplyState.value = next
                        pendingCardRefundApplyState.value = refreshCardRefundPickerLedger(next)
                    }
                }

                ApprovalCenterUiEvent.OnDismissSmsMerge -> {
                    pendingSmsMergeState.value = null
                }

                ApprovalCenterUiEvent.OnApproveAsNewTransaction -> {
                    val approvalId = pendingSmsMergeState.value?.approvalIds?.singleOrNull() ?: return
                    pendingSmsMergeState.value = null
                    approveCandidate(approvalId)
                }

                is ApprovalCenterUiEvent.OnMergeSmsIntoTransaction -> {
                    val prompt = pendingSmsMergeState.value ?: return
                    pendingSmsMergeState.value = null
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
                    if (!selectionModeState.value) {
                        selectionModeState.value = true
                        selectedApprovalIdsState.value = setOf(event.id)
                    }
                }

                is ApprovalCenterUiEvent.OnApprovalSelectionToggle -> {
                    if (!selectionModeState.value) return
                    selectedApprovalIdsState.update { current ->
                        if (event.id in current) current - event.id else current + event.id
                    }
                }

                ApprovalCenterUiEvent.OnExitApprovalSelection -> clearApprovalSelection()

                ApprovalCenterUiEvent.OnApprovalSelectAll -> {
                    if (!selectionModeState.value) return
                    val allIds =
                        uiState.value.candidates
                            .map { it.id }
                            .toSet()
                    selectedApprovalIdsState.value =
                        if (selectedApprovalIdsState.value.size == allIds.size) {
                            emptySet()
                        } else {
                            allIds
                        }
                }

                ApprovalCenterUiEvent.OnLinkSelectedApprovals -> {
                    val ids = selectedApprovalIdsState.value.toList()
                    if (ids.isEmpty()) return
                    beginLinkToExisting(ids, userInitiated = true)
                }

                ApprovalCenterUiEvent.OnApproveSelectedApprovals -> bulkApproveSelected()

                ApprovalCenterUiEvent.OnRejectSelectedApprovals -> bulkRejectSelected()

                is ApprovalCenterUiEvent.OnMergePickerParentSelected -> {
                    pendingSmsMergeState.value =
                        pendingSmsMergeState.value?.copy(
                            mergePickerHighlightedCategoryId = event.categoryId,
                            useLedgerCategory = false,
                        )
                }

                is ApprovalCenterUiEvent.OnMergePickerSubcategorySelected -> {
                    pendingSmsMergeState.value =
                        pendingSmsMergeState.value?.let { current ->
                            current.copy(
                                selectedCategoryId = current.mergePickerHighlightedCategoryId,
                                selectedSubcategoryId = event.subcategoryId,
                                useLedgerCategory = false,
                            )
                        }
                }

                ApprovalCenterUiEvent.OnMergePickerConfirmParentOnly -> {
                    pendingSmsMergeState.value =
                        pendingSmsMergeState.value?.let { current ->
                            current.copy(
                                selectedCategoryId = current.mergePickerHighlightedCategoryId,
                                selectedSubcategoryId = null,
                                useLedgerCategory = false,
                            )
                        }
                }

                ApprovalCenterUiEvent.OnMergeUseLedgerCategory -> {
                    pendingSmsMergeState.value =
                        pendingSmsMergeState.value?.copy(
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
                    val current = pendingSmsMergeState.value ?: return
                    viewModelScope.launch {
                        val next = current.copy(noteQuery = event.query, isLoadingCandidates = true)
                        pendingSmsMergeState.value = next
                        pendingSmsMergeState.value = refreshLinkPickerLedger(next)
                    }
                }

                is ApprovalCenterUiEvent.OnRejectQuick -> {
                    viewModelScope.launch {
                        try {
                            approvalRepository.rejectCandidate(event.id)
                        } catch (e: Exception) {
                            scanResultMessageState.value =
                                MmrUiMessages.friendly(e.message, MmrUiMessages.GENERIC_ACTION)
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
                    scanResultMessageState.value = null
                }

                ApprovalCenterUiEvent.OnRetryClick -> {
                    approvalsErrorMessage.value = null
                    approvalsLoading.value = true
                    approvalRetryKey.value = approvalRetryKey.value + 1
                }

                ApprovalCenterUiEvent.OnAddManualSmsClick -> {
                    manualEntryErrorState.value = null
                    showManualEntryDialogState.value = true
                }

                ApprovalCenterUiEvent.OnDismissManualEntry -> {
                    showManualEntryDialogState.value = false
                    manualEntryErrorState.value = null
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
                    pendingManualDuplicateState.value = null
                }

                ApprovalCenterUiEvent.OnManualDuplicateRevertPrior -> {
                    val draft = pendingManualDuplicateState.value ?: return
                    submitManualSms(
                        sender = draft.sender,
                        body = draft.body,
                        timestampMillis = draft.timestampMillis,
                        resolution = ManualSmsDuplicateResolution.REVERT_PRIOR_AND_USE_NEW,
                        existingApprovalIdForRevert = draft.duplicateMatch.approvalId,
                    )
                }

                ApprovalCenterUiEvent.OnManualDuplicateAddAsNew -> {
                    val draft = pendingManualDuplicateState.value ?: return
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
                            if (resolution == null && showManualEntryDialogState.value) {
                                manualEntryErrorState.value = result.reason
                            } else {
                                scanResultMessageState.value = result.reason
                            }
                        }

                        is ManualSmsSubmitResult.RequiresManualEntry -> {
                            showManualEntryDialogState.value = false
                            manualEntryErrorState.value = null
                            pendingManualDuplicateState.value = null
                            openManualEntryForApprovalIdFlow.tryEmit(result.approvalId)
                        }

                        is ManualSmsSubmitResult.NeedsDuplicateDecision -> {
                            showManualEntryDialogState.value = false
                            manualEntryErrorState.value = null
                            pendingManualDuplicateState.value =
                                PendingManualSmsDraft(
                                    sender = sender,
                                    body = body,
                                    timestampMillis = timestampMillis,
                                    duplicateMatch = result.match,
                                )
                        }

                        is ManualSmsSubmitResult.Added -> {
                            showManualEntryDialogState.value = false
                            manualEntryErrorState.value = null
                            pendingManualDuplicateState.value = null
                            scanResultMessageState.value = "Manual SMS added to pending list."
                        }
                    }
                } catch (e: Exception) {
                    scanResultMessageState.value = MmrUiMessages.friendly(e.message, MmrUiMessages.GENERIC_ACTION)
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
                    scanResultMessageState.value = MmrUiMessages.friendly(e.message, MmrUiMessages.GENERIC_ACTION)
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
            pendingSmsMergeState.value = loading
            pendingSmsMergeState.value = refreshLinkPickerLedger(loading)
        }

        private fun shiftLinkPickerMonth(delta: Int) {
            val current = pendingSmsMergeState.value ?: return
            val yearMonth = YearMonth.of(current.pickerYear, current.pickerMonth).plusMonths(delta.toLong())
            viewModelScope.launch {
                val loading =
                    current.copy(
                        pickerYear = yearMonth.year,
                        pickerMonth = yearMonth.monthValue,
                        monthTitle = HomePeriodUtils.monthTitle(yearMonth),
                        isLoadingCandidates = true,
                    )
                pendingSmsMergeState.value = loading
                pendingSmsMergeState.value = refreshLinkPickerLedger(loading)
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
                    scanResultMessageState.value =
                        if (count > 1) {
                            "$count SMS linked to the same transaction."
                        } else {
                            "SMS linked to existing transaction."
                        }
                } catch (e: Exception) {
                    scanResultMessageState.value = MmrUiMessages.friendly(e.message, MmrUiMessages.GENERIC_ACTION)
                }
            }
        }

        private fun clearApprovalSelection() {
            selectionModeState.value = false
            selectedApprovalIdsState.value = emptySet()
        }

        private fun approveCandidate(id: Long) {
            viewModelScope.launch {
                try {
                    approveCandidateOnce(id)
                } catch (e: Exception) {
                    scanResultMessageState.value = MmrUiMessages.friendly(e.message, MmrUiMessages.GENERIC_ACTION)
                }
            }
        }

        private suspend fun approveCandidateOnce(id: Long) {
            val accounts = accountRepository.getAccounts().firstOrNull() ?: emptyList()
            val fallbackAccountId =
                accounts.firstOrNull()?.id
                    ?: throw IllegalStateException("Add an account before approving SMS.")
            approvalRepository.approveCandidate(
                id = id,
                fallbackAccountId = fallbackAccountId,
                fallbackCategoryId = null,
            )
            scanResultMessageState.value = "Approved — added to your transaction list."
        }

        private fun bulkApproveSelected() {
            val ids = selectedApprovalIdsState.value.toList()
            if (ids.isEmpty()) return
            val candidatesById = uiState.value.candidates.associateBy { it.id }
            viewModelScope.launch {
                clearApprovalSelection()
                var approved = 0
                var skippedCardRefunds = 0
                var failed = 0
                for (id in ids) {
                    if (candidatesById[id]?.isCardRefund == true) {
                        skippedCardRefunds++
                        continue
                    }
                    try {
                        approveCandidateOnce(id)
                        approved++
                    } catch (_: Exception) {
                        failed++
                    }
                }
                scanResultMessageState.value = bulkApprovalResultMessage(approved, skippedCardRefunds, failed)
            }
        }

        private fun bulkRejectSelected() {
            val ids = selectedApprovalIdsState.value.toList()
            if (ids.isEmpty()) return
            viewModelScope.launch {
                clearApprovalSelection()
                var rejected = 0
                var failed = 0
                for (id in ids) {
                    try {
                        approvalRepository.rejectCandidate(id)
                        rejected++
                    } catch (_: Exception) {
                        failed++
                    }
                }
                scanResultMessageState.value =
                    when {
                        failed > 0 && rejected > 0 ->
                            "Rejected $rejected; $failed could not be rejected."
                        failed > 0 -> MmrUiMessages.GENERIC_ACTION
                        rejected == 1 -> "1 item rejected."
                        rejected > 1 -> "$rejected items rejected."
                        else -> null
                    }
            }
        }

        private fun bulkApprovalResultMessage(
            approved: Int,
            skippedCardRefunds: Int,
            failed: Int,
        ): String? {
            if (approved == 0 && skippedCardRefunds == 0 && failed == 0) return null
            val parts = mutableListOf<String>()
            if (approved > 0) {
                parts.add(if (approved == 1) "1 approved" else "$approved approved")
            }
            if (skippedCardRefunds > 0) {
                parts.add(
                    if (skippedCardRefunds == 1) {
                        "1 card refund skipped (use Apply refund)"
                    } else {
                        "$skippedCardRefunds card refunds skipped (use Apply refund)"
                    },
                )
            }
            if (failed > 0) {
                parts.add(if (failed == 1) "1 failed" else "$failed failed")
            }
            return parts.joinToString(". ") + "."
        }

        private fun scanDeviceSms() {
            viewModelScope.launch {
                smsScanning.value = true
                scanResultMessageState.value = null
                try {
                    if (!SmsPermissionHelper.hasSmsPermission(appContext)) {
                        scanResultMessageState.value =
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
                        scanResultMessageState.value =
                            "No transaction SMS found in the last $months month(s)."
                    } else {
                        val cleared = approvalRepository.reconcilePendingWithCurrentParser()
                        val addedTxn = approvalRepository.insertCandidates(parsedCandidates)
                        val addedRefund = approvalRepository.insertCandidates(scanSummary.cardRefunds)
                        scanResultMessageState.value =
                            buildScanResultMessage(
                                added = addedTxn + addedRefund,
                                txnCount = parsedCandidates.size + scanSummary.cardRefunds.size,
                                summary = scanSummary,
                                clearedPending = cleared,
                            )
                    }
                } catch (e: Exception) {
                    scanResultMessageState.value = "Error scanning SMS: ${e.localizedMessage}"
                } finally {
                    smsScanning.value = false
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
                    if (entity.extractedTemplateId == ManualSmsParseConstants.UNPARSED_TEMPLATE_ID) {
                        "—"
                    } else if (isCardRefund) {
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
            return categories
                .find { it.name.equals(com.mmr.core.data.catalog.MmrUncategorisedCategory.NAME, ignoreCase = true) }
                ?.id
                ?: categories.firstOrNull()?.id
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
            pendingCardRefundApplyState.value = loading
            pendingCardRefundApplyState.value = refreshCardRefundPickerLedger(loading)
        }

        private fun shiftCardRefundPickerMonth(delta: Int) {
            val current = pendingCardRefundApplyState.value ?: return
            val yearMonth = YearMonth.of(current.pickerYear, current.pickerMonth).plusMonths(delta.toLong())
            viewModelScope.launch {
                val loading =
                    current.copy(
                        pickerYear = yearMonth.year,
                        pickerMonth = yearMonth.monthValue,
                        monthTitle = HomePeriodUtils.monthTitle(yearMonth),
                        isLoadingCandidates = true,
                    )
                pendingCardRefundApplyState.value = loading
                pendingCardRefundApplyState.value = refreshCardRefundPickerLedger(loading)
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
            val prompt = pendingCardRefundApplyState.value ?: return
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
                            scanResultMessageState.value = "Enter a valid refund amount."
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
                    pendingCardRefundApplyState.value = null
                    scanResultMessageState.value = "Refund applied to purchase."
                } catch (e: Exception) {
                    scanResultMessageState.value = MmrUiMessages.friendly(e.message, MmrUiMessages.GENERIC_ACTION)
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
