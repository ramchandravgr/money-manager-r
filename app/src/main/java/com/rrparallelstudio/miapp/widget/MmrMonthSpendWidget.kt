package com.rrparallelstudio.miapp.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.PreviewSizeMode
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.ColumnScope
import androidx.glance.layout.Row
import androidx.glance.layout.fillMaxHeight
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.rrparallelstudio.miapp.core.data.preferences.AppDisplaySettingsSnapshot
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.EntryPointAccessors

class MmrMonthSpendWidget : GlanceAppWidget() {
    override val sizeMode: SizeMode = MmrWidgetSizes.monthSpendResponsive

    override val previewSizeMode: PreviewSizeMode =
        SizeMode.Responsive(setOf(MmrWidgetSizes.rect(2, 1)))

    override suspend fun providePreview(
        context: Context,
        widgetCategory: Int,
    ) {
        val data = MmrWidgetPreviewSamples.monthSpend(context)
        provideContent {
            MonthSpendContent(context = context, data = data)
        }
    }

    override suspend fun provideGlance(
        context: Context,
        id: GlanceId,
    ) {
        val loader =
            EntryPointAccessors
                .fromApplication(context.applicationContext, MmrWidgetEntryPoint::class.java)
                .widgetSnapshotLoader()
        val initial =
            runCatching { loader.loadMonthSpendTillToday() }.getOrElse {
                MmrMonthSpendWidgetData(
                    periodTitle = "Current month",
                    incomeFormatted = "—",
                    expenseFormatted = "—",
                    totalFormatted = "—",
                    incomeColorArgb = Color(0xFF059669).toArgb(),
                    expenseColorArgb = Color(0xFFEF4444).toArgb(),
                    chrome = MmrWidgetChrome.resolve(context, AppDisplaySettingsSnapshot()),
                )
            }
        val updates = loader.monthSpendFlow()
        provideContent {
            val data by updates.collectAsState(initial)
            MonthSpendContent(context = context, data = data)
        }
    }
}

@Composable
private fun MonthSpendContent(
    context: Context,
    data: MmrMonthSpendWidgetData,
) {
    val text = rememberMonthSpendWidgetTextScale()
    val chrome = data.chrome
    val labelStyle =
        TextStyle(
            fontSize = text.subtitle,
            fontWeight = FontWeight.Normal,
            color = chrome.onSurfaceVariant(),
            textAlign = TextAlign.Center,
        )
    val amountStyle =
        TextStyle(
            fontSize = text.amount,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center,
        )
    val totalAmountStyle =
        amountStyle.copy(
            fontWeight = FontWeight.Bold,
        )
    val titleStyle =
        TextStyle(
            fontSize = text.header,
            fontWeight = FontWeight.Bold,
            color = chrome.onSurface(),
            textAlign = TextAlign.Center,
        )
    Column(
        modifier =
            GlanceModifier
                .fillMaxSize()
                .background(chrome.background())
                .clickable(actionStartActivity(homeIntent(context)))
                .padding(horizontal = text.contentPadding, vertical = text.gapSmall),
        horizontalAlignment = Alignment.Horizontal.CenterHorizontally,
    ) {
        MonthSpendSection(modifier = GlanceModifier.defaultWeight()) {
            Text(
                text = data.periodTitle,
                maxLines = 1,
                modifier = GlanceModifier.fillMaxWidth(),
                style = titleStyle,
            )
        }
        WidgetHorizontalSeparator(chrome = chrome)
        MonthSpendSection(modifier = GlanceModifier.defaultWeight()) {
            Row(
                modifier = GlanceModifier.fillMaxWidth().fillMaxHeight(),
                verticalAlignment = Alignment.Vertical.CenterVertically,
            ) {
                MonthSummaryCell(
                    label = "Income",
                    amount = data.incomeFormatted,
                    amountColor = ColorProvider(Color(data.incomeColorArgb)),
                    labelStyle = labelStyle,
                    amountStyle = amountStyle,
                    gapSmall = text.gapSmall,
                    modifier = GlanceModifier.defaultWeight(),
                )
                WidgetVerticalSeparator(chrome = chrome)
                MonthSummaryCell(
                    label = "Expense",
                    amount = data.expenseFormatted,
                    amountColor = ColorProvider(Color(data.expenseColorArgb)),
                    labelStyle = labelStyle,
                    amountStyle = amountStyle,
                    gapSmall = text.gapSmall,
                    modifier = GlanceModifier.defaultWeight(),
                )
            }
        }
        WidgetHorizontalSeparator(chrome = chrome)
        MonthSpendSection(modifier = GlanceModifier.defaultWeight()) {
            MonthSummaryCell(
                label = "Total",
                amount = data.totalFormatted,
                amountColor = chrome.onSurface(),
                labelStyle = labelStyle,
                amountStyle = totalAmountStyle,
                gapSmall = text.gapSmall,
                modifier = GlanceModifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun MonthSpendSection(
    modifier: GlanceModifier = GlanceModifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = GlanceModifier.fillMaxWidth(),
            horizontalAlignment = Alignment.Horizontal.CenterHorizontally,
            verticalAlignment = Alignment.Vertical.CenterVertically,
            content = content,
        )
    }
}

@Composable
private fun WidgetHorizontalSeparator(
    chrome: MmrWidgetChromeColors,
    modifier: GlanceModifier = GlanceModifier,
) {
    Box(
        modifier =
            modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(chrome.separator()),
    ) {}
}

@Composable
private fun WidgetVerticalSeparator(chrome: MmrWidgetChromeColors) {
    Box(
        modifier =
            GlanceModifier
                .width(1.dp)
                .fillMaxHeight()
                .background(chrome.separator()),
    ) {}
}

@Composable
private fun MonthSummaryCell(
    label: String,
    amount: String,
    amountColor: ColorProvider,
    labelStyle: TextStyle,
    amountStyle: TextStyle,
    gapSmall: Dp,
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
            modifier = GlanceModifier.padding(top = gapSmall),
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
