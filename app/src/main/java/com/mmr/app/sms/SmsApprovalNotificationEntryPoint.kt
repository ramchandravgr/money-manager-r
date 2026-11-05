package com.mmr.app.sms

import com.mmr.app.widget.MmrWidgetActionExecutor
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@EntryPoint
@InstallIn(SingletonComponent::class)
interface SmsApprovalNotificationEntryPoint {
    fun widgetActionExecutor(): MmrWidgetActionExecutor

    fun smsApprovalSummaryNotifier(): SmsApprovalSummaryNotifier
}
