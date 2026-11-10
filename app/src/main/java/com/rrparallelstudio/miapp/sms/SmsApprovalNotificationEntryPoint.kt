package com.rrparallelstudio.miapp.sms

import com.rrparallelstudio.miapp.widget.MmrWidgetActionExecutor
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@EntryPoint
@InstallIn(SingletonComponent::class)
interface SmsApprovalNotificationEntryPoint {
    fun widgetActionExecutor(): MmrWidgetActionExecutor

    fun smsApprovalSummaryNotifier(): SmsApprovalSummaryNotifier
}
