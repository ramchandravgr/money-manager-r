package com.mmr.app.widget

import com.mmr.core.approval.ApprovalInboxSmsScanCoordinator
import com.mmr.core.common.widget.HomeScreenWidgetRefreshTrigger
import com.mmr.core.domain.repository.AccountRepository
import com.mmr.core.domain.repository.ApprovalRepository
import com.mmr.core.model.enums.SmsApprovalKind
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MmrWidgetActionExecutor
    @Inject
    constructor(
        private val approvalRepository: ApprovalRepository,
        private val accountRepository: AccountRepository,
        private val approvalInboxSmsScanCoordinator: ApprovalInboxSmsScanCoordinator,
        private val deferStore: MmrWidgetDeferStore,
        private val scanStateStore: MmrWidgetScanStateStore,
        private val widgetRefreshTrigger: HomeScreenWidgetRefreshTrigger,
    ) {
        suspend fun approve(approvalId: Long) {
            val approval = approvalRepository.getApprovalById(approvalId) ?: return
            if (approval.approvalKind == SmsApprovalKind.CARD_REFUND) return
            val accounts = accountRepository.getAccounts().first()
            val fallbackAccountId = accounts.firstOrNull()?.id ?: return
            approvalRepository.approveCandidate(
                id = approvalId,
                fallbackAccountId = fallbackAccountId,
                fallbackCategoryId = null,
            )
            deferStore.clearDefer(approvalId)
            widgetRefreshTrigger.refreshNow()
        }

        suspend fun reject(approvalId: Long) {
            approvalRepository.rejectCandidate(approvalId)
            deferStore.clearDefer(approvalId)
            widgetRefreshTrigger.refreshNow()
        }

        suspend fun defer(approvalId: Long) {
            deferStore.defer(approvalId)
            widgetRefreshTrigger.refreshNow()
        }

        suspend fun scanInbox() {
            scanStateStore.setScanning(true)
            widgetRefreshTrigger.refreshNow()
            try {
                approvalInboxSmsScanCoordinator.scanInboxForUserAction()
            } finally {
                scanStateStore.setScanning(false)
                widgetRefreshTrigger.refreshNow()
            }
        }
    }
