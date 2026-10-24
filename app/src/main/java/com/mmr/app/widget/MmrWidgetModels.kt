package com.mmr.app.widget

data class MmrApprovalTriageWidgetData(
    val pendingCount: Int,
    val isScanning: Boolean,
    val headline: MmrApprovalTriageHeadline?,
)

data class MmrApprovalTriageHeadline(
    val approvalId: Long,
    val amountFormatted: String,
    val subtitle: String,
    val metaLine: String,
    val isCardRefund: Boolean,
    val canQuickApprove: Boolean,
)

data class MmrMonthSpendWidgetData(
    val periodTitle: String,
    val incomeFormatted: String,
    val expenseFormatted: String,
    val totalFormatted: String,
    val incomeColorArgb: Int,
    val expenseColorArgb: Int,
    val totalColorArgb: Int,
)
