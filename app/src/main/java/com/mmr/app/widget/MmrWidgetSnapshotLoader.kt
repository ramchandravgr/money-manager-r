package com.mmr.app.widget

import android.content.Context
import androidx.compose.ui.graphics.toArgb
import com.mmr.app.ui.theme.MmrSystemThemeAppearance
import com.mmr.app.ui.theme.MmrThemePresets
import com.mmr.core.common.display.MmrMoneyFormat
import com.mmr.core.data.preferences.AppDisplaySettingsSnapshot
import com.mmr.core.data.preferences.AppDisplaySettingsStore
import com.mmr.core.domain.model.BudgetProgressSnapshot
import com.mmr.core.domain.model.PendingApprovalWithSms
import com.mmr.core.domain.repository.ApprovalRepository
import com.mmr.core.domain.repository.BudgetRepository
import com.mmr.core.domain.repository.TransactionRepository
import com.mmr.core.model.enums.SmsApprovalState
import com.mmr.feature.home.HomePeriodUtils
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import java.time.YearMonth
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MmrWidgetSnapshotLoader
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
        private val approvalRepository: ApprovalRepository,
        private val transactionRepository: TransactionRepository,
        private val budgetRepository: BudgetRepository,
        private val displaySettingsStore: AppDisplaySettingsStore,
        private val deferStore: MmrWidgetDeferStore,
        private val scanStateStore: MmrWidgetScanStateStore,
    ) {
        private val zoneId: ZoneId = ZoneId.systemDefault()

        suspend fun loadApprovalTriage(): MmrApprovalTriageWidgetData {
            val count = approvalRepository.getPendingCountOnce()
            val pending =
                approvalRepository
                    .getApprovalsByStateWithSms(SmsApprovalState.PENDING)
                    .first()
            val display = displaySettingsStore.snapshot()
            return triageData(count, pending, scanStateStore.isScanning(), display)
        }

        /**
         * Live snapshot for an open Glance session. [GlanceAppWidget.update] does not re-run
         * [provideGlance] while that session is alive, so the composition must observe this flow.
         */
        fun approvalTriageFlow(): Flow<MmrApprovalTriageWidgetData> =
            combine(
                approvalRepository.getPendingCount(),
                approvalRepository.getApprovalsByStateWithSms(SmsApprovalState.PENDING),
                scanStateStore.scanning,
                deferStore.revision,
                displaySettingsStore.settings,
            ) { count, pending, scanning, _, display ->
                triageData(count, pending, scanning, display)
            }

        suspend fun loadMonthSpendTillToday(): MmrMonthSpendWidgetData {
            val display = displaySettingsStore.snapshot()
            val yearMonth = YearMonth.now(zoneId)
            val (periodStart, periodEnd) = periodRange(display, yearMonth)
            val income =
                transactionRepository.getTotalIncomeBetween(periodStart, periodEnd).first() ?: 0.0
            val expense =
                transactionRepository.getTotalExpenseBetween(periodStart, periodEnd).first() ?: 0.0
            return monthSpendData(display, yearMonth, income, expense)
        }

        fun monthSpendFlow(): Flow<MmrMonthSpendWidgetData> =
            displaySettingsStore.settings.flatMapLatest { display ->
                val yearMonth = YearMonth.now(zoneId)
                val (periodStart, periodEnd) = periodRange(display, yearMonth)
                combine(
                    transactionRepository.getTotalIncomeBetween(periodStart, periodEnd),
                    transactionRepository.getTotalExpenseBetween(periodStart, periodEnd),
                ) { income, expense ->
                    monthSpendData(display, yearMonth, income ?: 0.0, expense ?: 0.0)
                }
            }

        suspend fun loadBudgets(): MmrBudgetsWidgetData {
            val display = displaySettingsStore.snapshot()
            val snapshots = budgetRepository.observeActiveBudgetProgress().first()
            return budgetsData(display, snapshots)
        }

        fun budgetsFlow(): Flow<MmrBudgetsWidgetData> =
            combine(
                budgetRepository.observeActiveBudgetProgress(),
                displaySettingsStore.settings,
            ) { snapshots, display ->
                budgetsData(display, snapshots)
            }

        private fun triageData(
            count: Int,
            pending: List<PendingApprovalWithSms>,
            scanning: Boolean,
            display: AppDisplaySettingsSnapshot,
        ): MmrApprovalTriageWidgetData {
            val symbol = display.mainCurrencySymbol
            val row = MmrWidgetApprovalMapper.pickHeadlineRow(pending, deferStore)
            return MmrApprovalTriageWidgetData(
                pendingCount = count,
                isScanning = scanning,
                headline = row?.let { MmrWidgetApprovalMapper.toHeadline(it, symbol) },
                chrome = MmrWidgetChrome.resolve(context, display),
            )
        }

        private fun periodRange(
            display: AppDisplaySettingsSnapshot,
            yearMonth: YearMonth,
        ): Pair<Long, Long> =
            HomePeriodUtils.epochRangeMillis(
                yearMonth,
                zoneId,
                display.monthStartDay,
            )

        private fun monthSpendData(
            display: AppDisplaySettingsSnapshot,
            yearMonth: YearMonth,
            income: Double,
            expense: Double,
        ): MmrMonthSpendWidgetData {
            val symbol = display.mainCurrencySymbol
            val appearance = MmrSystemThemeAppearance.resolve(display.themeMode, context)
            val palette = MmrThemePresets.resolve(display.appColorTheme, appearance)
            val amountScheme = display.amountColorScheme
            return MmrMonthSpendWidgetData(
                periodTitle = HomePeriodUtils.widgetMonthLabel(yearMonth),
                incomeFormatted = MmrMoneyFormat.formatWithSpace(income, symbol),
                expenseFormatted = MmrMoneyFormat.formatWithSpace(expense, symbol),
                totalFormatted = MmrMoneyFormat.formatWithSpace(income - expense, symbol),
                incomeColorArgb = palette.incomeColor(amountScheme).toArgb(),
                expenseColorArgb = palette.expenseColor(amountScheme).toArgb(),
                chrome = MmrWidgetChrome.resolve(context, display),
            )
        }

        private fun budgetsData(
            display: AppDisplaySettingsSnapshot,
            snapshots: List<BudgetProgressSnapshot>,
        ): MmrBudgetsWidgetData {
            val rows =
                snapshots.map { snapshot ->
                    MmrBudgetWidgetRow(
                        budgetId = snapshot.budgetId,
                        name = snapshot.widgetDisplayName(),
                        periodKind = snapshot.periodType.widgetPeriodKindLabel(),
                        percentUsed = snapshot.percentUsed,
                        progressFraction = snapshot.progressFraction,
                        isOverBudget = snapshot.isOverBudget,
                    )
                }
            return MmrBudgetsWidgetData(
                rows = rows,
                chrome = MmrWidgetChrome.resolve(context, display),
            )
        }
    }
