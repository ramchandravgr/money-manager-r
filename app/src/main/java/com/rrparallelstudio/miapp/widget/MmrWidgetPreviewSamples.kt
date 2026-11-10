package com.rrparallelstudio.miapp.widget

import android.content.Context
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.rrparallelstudio.miapp.R
import com.rrparallelstudio.miapp.core.data.preferences.AppDisplaySettingsSnapshot

/** Static picker / generated-preview data (no DB, no flows). */
internal object MmrWidgetPreviewSamples {
    fun monthSpend(context: Context): MmrMonthSpendWidgetData {
        val chrome = MmrWidgetChrome.resolve(context, AppDisplaySettingsSnapshot())
        return MmrMonthSpendWidgetData(
            periodTitle = context.getString(R.string.widget_preview_month_period),
            incomeFormatted = context.getString(R.string.widget_preview_sample_income),
            expenseFormatted = context.getString(R.string.widget_preview_sample_expense),
            totalFormatted = context.getString(R.string.widget_preview_sample_total),
            incomeColorArgb = Color(0xFF059669).toArgb(),
            expenseColorArgb = Color(0xFFEF4444).toArgb(),
            chrome = chrome,
        )
    }

    fun budgets(context: Context): MmrBudgetsWidgetData {
        val chrome = MmrWidgetChrome.resolve(context, AppDisplaySettingsSnapshot())
        return MmrBudgetsWidgetData(
            rows =
                listOf(
                    MmrBudgetWidgetRow(
                        budgetId = 1L,
                        name = context.getString(R.string.widget_preview_budget_name_travel),
                        periodKind = context.getString(R.string.widget_preview_budget_period_custom),
                        percentUsed = 45,
                        progressFraction = 0.45f,
                        isOverBudget = false,
                    ),
                    MmrBudgetWidgetRow(
                        budgetId = 2L,
                        name = context.getString(R.string.widget_preview_budget_name_food),
                        periodKind = context.getString(R.string.widget_preview_budget_period_month),
                        percentUsed = 79,
                        progressFraction = 0.79f,
                        isOverBudget = false,
                    ),
                ),
            chrome = chrome,
        )
    }

    fun pendingApprovals(context: Context): MmrApprovalTriageWidgetData {
        val chrome = MmrWidgetChrome.resolve(context, AppDisplaySettingsSnapshot())
        return MmrApprovalTriageWidgetData(
            pendingCount = 2,
            isScanning = false,
            headline =
                MmrApprovalTriageHeadline(
                    approvalId = 0L,
                    amountFormatted = context.getString(R.string.widget_preview_sample_amount),
                    subtitle = context.getString(R.string.widget_preview_sample_merchant),
                    metaLine = context.getString(R.string.widget_preview_sample_meta),
                    smsPreview = context.getString(R.string.widget_preview_sample_sms),
                    isCardRefund = false,
                    canQuickApprove = true,
                ),
            chrome = chrome,
        )
    }
}
