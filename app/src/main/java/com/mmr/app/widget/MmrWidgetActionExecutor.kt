package com.mmr.app.widget

import android.content.Context
import com.mmr.core.common.widget.HomeScreenWidgetRefreshTrigger
import com.mmr.core.data.preferences.SmsScanSettingsStore
import com.mmr.core.domain.repository.AccountRepository
import com.mmr.core.domain.repository.ApprovalRepository
import com.mmr.core.domain.repository.CategoryRepository
import com.mmr.core.model.enums.SmsApprovalKind
import com.mmr.core.parser.permission.SmsPermissionHelper
import com.mmr.core.parser.scanner.LocalSmsScannerService
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MmrWidgetActionExecutor
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
        private val approvalRepository: ApprovalRepository,
        private val accountRepository: AccountRepository,
        private val categoryRepository: CategoryRepository,
        private val smsScannerService: LocalSmsScannerService,
        private val smsScanSettingsStore: SmsScanSettingsStore,
        private val deferStore: MmrWidgetDeferStore,
        private val scanStateStore: MmrWidgetScanStateStore,
        private val widgetRefreshTrigger: HomeScreenWidgetRefreshTrigger,
    ) {
        suspend fun approve(approvalId: Long) {
            val approval = approvalRepository.getApprovalById(approvalId) ?: return
            if (approval.approvalKind == SmsApprovalKind.CARD_REFUND) return
            val accounts = accountRepository.getAccounts().first()
            val categories = categoryRepository.getActiveCategories().first()
            val fallbackAccountId = accounts.firstOrNull()?.id ?: return
            val fallbackCategoryId = categories.firstOrNull()?.id
            approvalRepository.approveCandidate(
                id = approvalId,
                fallbackAccountId = fallbackAccountId,
                fallbackCategoryId = fallbackCategoryId,
            )
            deferStore.clearDefer(approvalId)
            widgetRefreshTrigger.requestRefresh()
        }

        suspend fun reject(approvalId: Long) {
            approvalRepository.rejectCandidate(approvalId)
            deferStore.clearDefer(approvalId)
            widgetRefreshTrigger.requestRefresh()
        }

        fun defer(approvalId: Long) {
            deferStore.defer(approvalId)
            widgetRefreshTrigger.requestRefresh()
        }

        suspend fun scanInbox() {
            if (!SmsPermissionHelper.hasSmsPermission(context)) {
                return
            }
            scanStateStore.setScanning(true)
            widgetRefreshTrigger.requestRefresh()
            try {
                val months = smsScanSettingsStore.getScanMonths()
                val scanSummary = smsScannerService.scanInbox(monthsBack = months)
                approvalRepository.reconcilePendingWithCurrentParser()
                approvalRepository.insertCandidates(scanSummary.transactions)
                approvalRepository.insertCandidates(scanSummary.cardRefunds)
            } finally {
                scanStateStore.setScanning(false)
                widgetRefreshTrigger.requestRefresh()
            }
        }
    }
