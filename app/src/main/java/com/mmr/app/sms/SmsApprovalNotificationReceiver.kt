package com.mmr.app.sms

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SmsApprovalNotificationReceiver : BroadcastReceiver() {
    override fun onReceive(
        context: Context,
        intent: Intent,
    ) {
        when (intent.action) {
            ACTION_APPROVE -> {
                val approvalId = intent.getLongExtra(EXTRA_APPROVAL_ID, -1L)
                if (approvalId <= 0L) return
                val pendingResult = goAsync()
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val entry =
                            EntryPointAccessors.fromApplication(
                                context.applicationContext,
                                SmsApprovalNotificationEntryPoint::class.java,
                            )
                        entry.widgetActionExecutor().approve(approvalId)
                        entry.smsApprovalSummaryNotifier().refreshAfterPendingQueueChanged()
                    } finally {
                        pendingResult.finish()
                    }
                }
            }
        }
    }

    companion object {
        const val ACTION_APPROVE = "com.mmr.app.action.APPROVE_PENDING_SMS"
        const val EXTRA_APPROVAL_ID = "approval_id"
    }
}
