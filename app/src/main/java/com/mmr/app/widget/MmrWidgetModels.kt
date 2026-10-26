package com.mmr.app.widget

data class MmrApprovalTriageWidgetData(
    val pendingCount: Int,
    val isScanning: Boolean,
    val headline: MmrApprovalTriageHeadline?,
    val chrome: MmrWidgetChromeColors,
)

data class MmrApprovalTriageHeadline(
    val approvalId: Long,
    val amountFormatted: String,
    val subtitle: String,
    val metaLine: String,
    /** Normalized SMS body for multi-line preview in extra widget height. */
    val smsPreview: String,
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
    val chrome: MmrWidgetChromeColors,
)

data class MmrBudgetWidgetRow(
    val budgetId: Long,
    val name: String,
    val periodKind: String,
    val percentUsed: Int,
    val progressFraction: Float,
    val isOverBudget: Boolean,
)

data class MmrBudgetsWidgetData(
    val rows: List<MmrBudgetWidgetRow>,
    val chrome: MmrWidgetChromeColors,
)
