package com.mmr.app.sms

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.mmr.app.widget.MmrWidgetRefresher
import com.mmr.core.data.preferences.SmsInboundSettingsStore
import com.mmr.core.domain.repository.ApprovalRepository
import com.mmr.core.parser.inbound.IncomingSmsIngestionCoordinator
import com.mmr.core.parser.model.RawSmsMessage
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class ProcessIncomingSmsWorker
    @AssistedInject
    constructor(
        @Assisted context: Context,
        @Assisted params: WorkerParameters,
        private val smsInboundSettingsStore: SmsInboundSettingsStore,
        private val ingestionCoordinator: IncomingSmsIngestionCoordinator,
        private val approvalRepository: ApprovalRepository,
        private val summaryNotifier: SmsApprovalSummaryNotifier,
        private val widgetRefresher: MmrWidgetRefresher,
    ) : CoroutineWorker(context, params) {
        override suspend fun doWork(): Result {
            if (!smsInboundSettingsStore.isNotifyOnNewPendingEnabled()) {
                return Result.success()
            }

            val sender = inputData.getString(KEY_SENDER)?.trim().orEmpty()
            val body = inputData.getString(KEY_BODY)?.trim().orEmpty()
            val timestamp = inputData.getLong(KEY_TIMESTAMP, 0L)
            if (sender.isEmpty() || body.isEmpty() || timestamp <= 0L) {
                return Result.failure()
            }

            val ingestResult =
                ingestionCoordinator.ingest(
                    RawSmsMessage(
                        id = "inbound:$timestamp:$sender",
                        sender = sender,
                        body = body,
                        timestamp = timestamp,
                    ),
                )
            if (ingestResult.addedCount <= 0) {
                return Result.success()
            }

            val pendingCount = approvalRepository.getPendingCountOnce()
            summaryNotifier.showSummary(
                pendingCount = pendingCount,
                latestLine = ingestResult.summaryLine,
            )
            widgetRefresher.refreshAll()
            return Result.success()
        }

        companion object {
            const val KEY_SENDER = "sender"
            const val KEY_BODY = "body"
            const val KEY_TIMESTAMP = "timestamp"
        }
    }
