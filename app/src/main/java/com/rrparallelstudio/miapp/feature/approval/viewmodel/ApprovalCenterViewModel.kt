package com.rrparallelstudio.miapp.feature.approval.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rrparallelstudio.miapp.R
import com.rrparallelstudio.miapp.core.approval.ApprovalSmsIntakeCoordinator
import com.rrparallelstudio.miapp.core.common.validation.MmrValidation
import com.rrparallelstudio.miapp.core.data.preferences.AppDisplaySettingsStore
import com.rrparallelstudio.miapp.core.data.preferences.SmsBackupFolderStore
import com.rrparallelstudio.miapp.core.database.entity.AccountEntity
import com.rrparallelstudio.miapp.core.database.entity.CategoryEntity
import com.rrparallelstudio.miapp.core.database.relation.TransactionWithDetails
import com.rrparallelstudio.miapp.core.domain.model.ManualSmsDuplicateResolution
import com.rrparallelstudio.miapp.core.domain.model.ManualSmsParseConstants
import com.rrparallelstudio.miapp.core.domain.model.ManualSmsSubmitResult
import com.rrparallelstudio.miapp.core.domain.repository.AccountRepository
import com.rrparallelstudio.miapp.core.domain.repository.ApprovalRepository
import com.rrparallelstudio.miapp.core.domain.repository.CategoryRepository
import com.rrparallelstudio.miapp.core.domain.repository.TransactionRepository
import com.rrparallelstudio.miapp.core.model.category.CategoryWithChildren
import com.rrparallelstudio.miapp.core.model.enums.CategoryType
import com.rrparallelstudio.miapp.core.model.enums.SmsApprovalKind
import com.rrparallelstudio.miapp.core.model.enums.SmsApprovalState
import com.rrparallelstudio.miapp.core.model.enums.TransactionType
import com.rrparallelstudio.miapp.core.parser.model.SmsInstrument
import com.rrparallelstudio.miapp.core.parser.model.SmsPaymentChannel
import com.rrparallelstudio.miapp.core.parser.model.buildParseSourceLabel
import com.rrparallelstudio.miapp.core.parser.permission.SmsIntakeCapabilities
import com.rrparallelstudio.miapp.feature.approval.model.ApprovalCandidateUi
import com.rrparallelstudio.miapp.feature.approval.model.ApprovalCategoryOptionUi
import com.rrparallelstudio.miapp.feature.approval.model.ApprovalCenterUiEvent
import com.rrparallelstudio.miapp.feature.approval.model.ApprovalCenterUiState
import com.rrparallelstudio.miapp.feature.approval.model.CardRefundApplyStep
import com.rrparallelstudio.miapp.feature.approval.model.PendingCardRefundApplyPrompt
import com.rrparallelstudio.miapp.feature.approval.model.PendingManualSmsDraft
import com.rrparallelstudio.miapp.feature.approval.model.PendingSmsMergePrompt
import com.rrparallelstudio.miapp.feature.home.HomePeriodUtils
import com.rrparallelstudio.miapp.feature.home.TransactionGrouping
import com.rrparallelstudio.miapp.feature.home.TransactionRowMapping
import com.rrparallelstudio.miapp.feature.transaction.model.CategoryUi
import com.rrparallelstudio.miapp.feature.transaction.model.SubcategoryUi
import com.rrparallelstudio.miapp.ui.state.MmrUiMessages
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
        @ApplicationContext private val context: Context,
        private val approvalRepository: ApprovalRepository,
        private val accountRepository: AccountRepository,
        private val categoryRepository: CategoryRepository,
        private val transactionRepository: TransactionRepository,
        private val approvalSmsIntakeCoordinator: ApprovalSmsIntakeCoordinator,
        private val displaySettingsStore: AppDisplaySettingsStore,
        private val smsBackupFolderStore: SmsBackupFolderStore,
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

        private val pendingApprovals: Flow<List<com.rrparallelstudio.miapp.core.domain.model.PendingApprovalWithSms>> =
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

        private val approvalScreenCore =
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
                ApprovalScreenCore(approvals, categories, accounts, meta, overlay)
            }

        val uiState: StateFlow<ApprovalCenterUiState> =
            combine(
                approvalScreenCore,
                smsBackupFolderStore.folderTreeUri,
            ) { core, backupFolderUri ->
                val approvals = core.approvals
                val categories = core.categories
                val accounts = core.accounts
                val meta = core.meta
                val overlay = core.overlay
                val manualMeta = overlay.manual
                val selectionMeta = overlay.selection
                val loading = meta.loading
                val scanning = meta.scanning
                val scanMsg = meta.scanMessage
                val error = meta.errorMessage

                val categoriesById = categories.associateBy { it.id }
                val accountsById = accounts.associateBy { it.id }
                val candidateUis = approvals.map { it.toUi(categoriesById, accountsById) }

                val useXmlIntake = !SmsIntakeCapabilities.inboxScanAllowed(context)
                val folderConfigured = backupFolderUri != null

                ApprovalCenterUiState(
                    isLoading = loading,
                    isScanning = scanning,
                    candidates = candidateUis,
                    selectedTab = SmsApprovalState.PENDING,
                    pendingCount = candidateUis.size,
                    scanResultMessage = scanMsg,
                    errorMessage = error,
                    isEmpty = !loading && error == null && candidateUis.isEmpty(),
                    useSmsBackupXmlIntake = useXmlIntake,
                    smsBackupFolderConfigured = folderConfigured,
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
                        pendingSmsMergeState.value?.let { current ->
                            if (current.mergeCategories.none { it.id == event.categoryId }) {
                                return@let current
                            }
                            current.copy(
                                mergePickerHighlightedCategoryId = event.categoryId,
                                selectedCategoryId = event.categoryId,
                                selectedSubcategoryId = null,
                                useLedgerCategory = false,
                            )
                        }
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
                accounts.firstOrNull { it.isDefault }?.id
                    ?: accounts.firstOrNull()?.id
                    ?: throw IllegalStateException("Add an account before approving SMS.")
            approvalRepository.approveCandidate(
                id = id,
                fallbackAccountId = fallbackAccountId,
                fallbackCategoryId = null,
                provisionAccountFromSms = true,
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
                    if (!SmsIntakeCapabilities.inboxScanAllowed(context) && !smsBackupFolderStore.isConfigured()) {
                        scanResultMessageState.value = context.getString(R.string.approval_refresh_sms_setup_required)
                        return@launch
                    }
                    val result = approvalSmsIntakeCoordinator.refreshForUserAction()
                    scanResultMessageState.value = result.userMessage
                } catch (e: Exception) {
                    scanResultMessageState.value = "Error scanning SMS: ${e.localizedMessage}"
                } finally {
                    smsScanning.value = false
                }
            }
        }

        private fun com.rrparallelstudio.miapp.core.domain.model.PendingApprovalWithSms.toUi(
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
                        com.rrparallelstudio.miapp.core.common.display.MmrMoneyFormat
                            .format(entity.extractedAmount, "₹")
                    } else {
                        "$sign${com.rrparallelstudio.miapp.core.common.display.MmrMoneyFormat.format(
                            entity.extractedAmount,
                            "₹",
                        )}"
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
                            SubcategoryUi(id = sub.id.value, name = sub.name, iconName = sub.iconName)
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
                .find {
                    it.name.equals(
                        com.rrparallelstudio.miapp.core.data.catalog.MmrUncategorisedCategory.NAME,
                        ignoreCase = true,
                    )
                }?.id
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
                        com.rrparallelstudio.miapp.core.common.display.MmrMoneyFormat
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

private data class ApprovalScreenCore(
    val approvals: List<com.rrparallelstudio.miapp.core.domain.model.PendingApprovalWithSms>,
    val categories: List<CategoryEntity>,
    val accounts: List<AccountEntity>,
    val meta: ApprovalCenterMeta,
    val overlay: OverlayMeta,
)
