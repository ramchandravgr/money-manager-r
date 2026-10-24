package com.mmr.app.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.EntryPointAccessors

class MmrMonthSpendWidget : GlanceAppWidget() {
    override suspend fun provideGlance(
        context: Context,
        id: GlanceId,
    ) {
        val loader =
            EntryPointAccessors
                .fromApplication(context.applicationContext, MmrWidgetEntryPoint::class.java)
                .widgetSnapshotLoader()
        val data =
            runCatching { loader.loadMonthSpendTillToday() }.getOrElse {
                MmrMonthSpendWidgetData(
                    periodTitle = "This mo.",
                    incomeFormatted = "—",
                    expenseFormatted = "—",
                    totalFormatted = "—",
                    incomeColorArgb = Color(0xFF059669).toArgb(),
                    expenseColorArgb = Color(0xFFEF4444).toArgb(),
                    totalColorArgb = Color(0xFF1F2937).toArgb(),
                )
            }
        provideContent {
            GlanceTheme {
                MonthSpendContent(context = context, data = data)
            }
        }
    }
}

@Composable
private fun MonthSpendContent(
    context: Context,
    data: MmrMonthSpendWidgetData,
) {
    val labelStyle =
        TextStyle(
            fontSize = 9.sp,
            fontWeight = FontWeight.Normal,
            color = GlanceTheme.colors.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    val amountStyle =
        TextStyle(
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center,
        )
    Row(
        modifier =
            GlanceModifier
                .fillMaxSize()
                .background(GlanceTheme.colors.widgetBackground)
                .clickable(actionStartActivity(homeIntent(context)))
                .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.Vertical.CenterVertically,
    ) {
        Text(
            text = data.periodTitle,
            maxLines = 2,
            modifier = GlanceModifier.padding(end = 6.dp),
            style =
                TextStyle(
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium,
                    color = GlanceTheme.colors.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                ),
        )
        Row(modifier = GlanceModifier.defaultWeight()) {
            MonthSummaryCell(
                label = "Income",
                amount = data.incomeFormatted,
                amountColor = ColorProvider(Color(data.incomeColorArgb)),
                labelStyle = labelStyle,
                amountStyle = amountStyle,
                modifier = GlanceModifier.defaultWeight(),
            )
            MonthSummaryCell(
                label = "Expenses",
                amount = data.expenseFormatted,
                amountColor = ColorProvider(Color(data.expenseColorArgb)),
                labelStyle = labelStyle,
                amountStyle = amountStyle,
                modifier = GlanceModifier.defaultWeight(),
            )
            MonthSummaryCell(
                label = "Total",
                amount = data.totalFormatted,
                amountColor = ColorProvider(Color(data.totalColorArgb)),
                labelStyle = labelStyle,
                amountStyle = amountStyle,
                modifier = GlanceModifier.defaultWeight(),
            )
        }
    }
}

@Composable
private fun MonthSummaryCell(
    label: String,
    amount: String,
    amountColor: ColorProvider,
    labelStyle: TextStyle,
    amountStyle: TextStyle,
    modifier: GlanceModifier = GlanceModifier,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.Horizontal.CenterHorizontally,
        verticalAlignment = Alignment.Vertical.CenterVertically,
    ) {
        Text(text = label, maxLines = 1, style = labelStyle)
        Text(
            text = amount,
            modifier = GlanceModifier.padding(top = 1.dp),
            maxLines = 1,
            style = amountStyle.copy(color = amountColor),
        )
    }
}

@AndroidEntryPoint
class MmrMonthSpendWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = MmrMonthSpendWidget()

    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        EntryPointAccessors
            .fromApplication(context.applicationContext, MmrWidgetEntryPoint::class.java)
            .widgetRefresher()
            .refreshAll()
    }
}
