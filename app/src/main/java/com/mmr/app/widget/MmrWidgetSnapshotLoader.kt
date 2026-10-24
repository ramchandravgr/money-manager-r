package com.mmr.app.widget

import androidx.compose.ui.graphics.toArgb
import com.mmr.app.ui.theme.MmrThemePresets
import com.mmr.core.common.display.MmrMoneyFormat
import com.mmr.core.data.preferences.AppDisplaySettingsStore
import com.mmr.core.domain.repository.ApprovalRepository
import com.mmr.core.domain.repository.TransactionRepository
import com.mmr.core.model.enums.SmsApprovalState
import com.mmr.feature.home.HomePeriodUtils
import kotlinx.coroutines.flow.first
import java.time.YearMonth
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MmrWidgetSnapshotLoader
    @Inject
    constructor(
        private val approvalRepository: ApprovalRepository,
        private val transactionRepository: TransactionRepository,
        private val displaySettingsStore: AppDisplaySettingsStore,
        private val deferStore: MmrWidgetDeferStore,
        private val scanStateStore: MmrWidgetScanStateStore,
    ) {
        private val zoneId: ZoneId = ZoneId.systemDefault()

        suspend fun loadApprovalTriage(): MmrApprovalTriageWidgetData {
            val count = approvalRepository.getPendingCountOnce()
            val symbol = displaySettingsStore.snapshot().mainCurrencySymbol
            val pending =
                approvalRepository
                    .getApprovalsByStateWithSms(SmsApprovalState.PENDING)
                    .first()
            val row = MmrWidgetApprovalMapper.pickHeadlineRow(pending, deferStore)
            val headline = row?.let { MmrWidgetApprovalMapper.toHeadline(it, symbol) }
            return MmrApprovalTriageWidgetData(
                pendingCount = count,
                isScanning = scanStateStore.isScanning(),
                headline = headline,
            )
        }

        suspend fun loadMonthSpendTillToday(): MmrMonthSpendWidgetData {
            val display = displaySettingsStore.snapshot()
            val symbol = display.mainCurrencySymbol
            val palette = MmrThemePresets.resolve(display.appColorTheme)
            val amountScheme = display.amountColorScheme
            val yearMonth = YearMonth.now(zoneId)
            val (periodStart, periodEnd) =
                HomePeriodUtils.epochRangeMillis(
                    yearMonth,
                    zoneId,
                    display.monthStartDay,
                )
            val income =
                transactionRepository.getTotalIncomeBetween(periodStart, periodEnd).first() ?: 0.0
            val expense =
                transactionRepository.getTotalExpenseBetween(periodStart, periodEnd).first() ?: 0.0
            val net = income - expense
            return MmrMonthSpendWidgetData(
                periodTitle = HomePeriodUtils.widgetMonthLabel(yearMonth),
                incomeFormatted = MmrMoneyFormat.formatWithSpace(income, symbol),
                expenseFormatted = MmrMoneyFormat.formatWithSpace(expense, symbol),
                totalFormatted = MmrMoneyFormat.formatWithSpace(net, symbol),
                incomeColorArgb = palette.incomeColor(amountScheme).toArgb(),
                expenseColorArgb = palette.expenseColor(amountScheme).toArgb(),
                totalColorArgb = palette.reversal.toArgb(),
            )
        }
    }
