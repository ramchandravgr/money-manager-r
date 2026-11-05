package com.mmr.core.approval

import android.content.Context
import com.mmr.core.data.preferences.SmsScanSettingsStore
import com.mmr.core.domain.repository.ApprovalRepository
import com.mmr.core.parser.model.SmsInboxScanSummary
import com.mmr.core.parser.permission.SmsPermissionHelper
import com.mmr.core.parser.scanner.LocalSmsScannerService
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ApprovalInboxSmsScanCoordinator
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
        private val approvalRepository: ApprovalRepository,
        private val smsScannerService: LocalSmsScannerService,
        private val smsScanSettingsStore: SmsScanSettingsStore,
    ) {
        private val coldStartScanAttempted = AtomicBoolean(false)

        /**
         * Runs at most once per app process when [READ_SMS] is granted (cold start / post-install permission flow).
         */
        suspend fun scanInboxOnColdStartIfPermitted() {
            if (!SmsPermissionHelper.hasSmsPermission(context)) return
            if (!coldStartScanAttempted.compareAndSet(false, true)) return
            runInboxScan(updateUserMessage = false)
        }

        suspend fun scanInboxForUserAction(): ScanResult {
            if (!SmsPermissionHelper.hasSmsPermission(context)) {
                return ScanResult(
                    userMessage =
                        "SMS permission is required. Allow READ_SMS to scan your inbox.",
                )
            }
            return runInboxScan(updateUserMessage = true)
        }

        private suspend fun runInboxScan(updateUserMessage: Boolean): ScanResult {
            val months = smsScanSettingsStore.getScanMonths()
            val scanSummary = smsScannerService.scanInbox(monthsBack = months)
            val parsedCandidates = scanSummary.transactions
            if (
                parsedCandidates.isEmpty() &&
                scanSummary.cardRefunds.isEmpty() &&
                scanSummary.informationalCount == 0
            ) {
                val emptyMsg =
                    if (updateUserMessage) {
                        "No transaction SMS found in the last $months month(s)."
                    } else {
                        null
                    }
                return ScanResult(userMessage = emptyMsg, summary = scanSummary)
            }
            val cleared = approvalRepository.reconcilePendingWithCurrentParser()
            val addedTxn = approvalRepository.insertCandidates(parsedCandidates)
            val addedRefund = approvalRepository.insertCandidates(scanSummary.cardRefunds)
            val message =
                if (updateUserMessage) {
                    buildScanResultMessage(
                        added = addedTxn + addedRefund,
                        txnCount = parsedCandidates.size + scanSummary.cardRefunds.size,
                        summary = scanSummary,
                        clearedPending = cleared,
                    )
                } else {
                    null
                }
            return ScanResult(
                userMessage = message,
                summary = scanSummary,
                addedCount = addedTxn + addedRefund,
                clearedPending = cleared,
            )
        }

        data class ScanResult(
            val userMessage: String? = null,
            val summary: SmsInboxScanSummary? = null,
            val addedCount: Int = 0,
            val clearedPending: Int = 0,
        )

        companion object {
            fun buildScanResultMessage(
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
        }
    }
