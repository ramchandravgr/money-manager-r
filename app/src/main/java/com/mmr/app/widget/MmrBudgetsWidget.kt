package com.mmr.app.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.LocalSize
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.PreviewSizeMode
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.appwidget.lazy.items
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.fillMaxHeight
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.unit.ColorProvider
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import com.mmr.core.data.preferences.AppDisplaySettingsSnapshot
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.EntryPointAccessors

class MmrBudgetsWidget : GlanceAppWidget() {
    override val sizeMode: SizeMode = MmrWidgetSizes.budgetsResponsive

    override val previewSizeMode: PreviewSizeMode =
        SizeMode.Responsive(
            setOf(
                MmrWidgetSizes.rect(3, 1),
                MmrWidgetSizes.rect(4, 2),
                MmrWidgetSizes.rect(5, 3),
            ),
        )

    override suspend fun providePreview(
        context: Context,
        widgetCategory: Int,
    ) {
        val data = MmrWidgetPreviewSamples.budgets(context)
        provideContent {
            BudgetsWidgetContent(context = context, data = data)
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
            runCatching { loader.loadBudgets() }.getOrElse {
                MmrBudgetsWidgetData(
                    rows = emptyList(),
                    chrome = MmrWidgetChrome.resolve(context, AppDisplaySettingsSnapshot()),
                )
            }
        val updates = loader.budgetsFlow()
        provideContent {
            val data by updates.collectAsState(initial)
            BudgetsWidgetContent(context = context, data = data)
        }
    }
}

@Composable
private fun BudgetsWidgetContent(
    context: Context,
    data: MmrBudgetsWidgetData,
) {
    val rowCount = data.rows.size.coerceAtLeast(1)
    val text = rememberBudgetsWidgetTextScale(rowCount)
    val rowSlotHeight = rememberBudgetRowSlotHeight(rowCount)
    val chrome = data.chrome
    val muted =
        TextStyle(
            fontSize = text.meta,
            fontWeight = FontWeight.Normal,
            color = chrome.onSurfaceVariant(),
            textAlign = TextAlign.Center,
        )
    val rowStyle =
        TextStyle(
            fontSize = text.body,
            fontWeight = FontWeight.Normal,
            color = chrome.onSurface(),
        )
    val percentStyle =
        rowStyle.copy(
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.End,
        )
    val openBudgets = actionStartActivity(budgetsIntent(context))
    Column(
        modifier =
            GlanceModifier
                .fillMaxSize()
                .background(chrome.background())
                .padding(text.contentPadding),
    ) {
        if (data.rows.isEmpty()) {
            Text(
                text = "No budgets yet",
                modifier =
                    GlanceModifier
                        .fillMaxSize()
                        .clickable(openBudgets),
                style =
                    TextStyle(
                        fontSize = text.body,
                        color = chrome.onSurfaceVariant(),
                        textAlign = TextAlign.Center,
                    ),
            )
        } else {
            LazyColumn(
                modifier =
                    GlanceModifier
                        .fillMaxSize()
                        .clickable(openBudgets),
            ) {
                items(
                    items = data.rows,
                    itemId = { it.budgetId },
                ) { row ->
                    BudgetWidgetRow(
                        row = row,
                        chrome = chrome,
                        muted = muted,
                        rowStyle = rowStyle,
                        percentStyle = percentStyle,
                        contentPadding = text.contentPadding,
                        slotHeight = rowSlotHeight,
                        progressBarHeight = text.progressBarHeight,
                        gapSmall = text.gapSmall,
                    )
                }
            }
        }
    }
}

@Composable
private fun BudgetWidgetRow(
    row: MmrBudgetWidgetRow,
    chrome: MmrWidgetChromeColors,
    muted: TextStyle,
    rowStyle: TextStyle,
    percentStyle: TextStyle,
    contentPadding: Dp,
    slotHeight: Dp,
    progressBarHeight: Dp,
    gapSmall: Dp,
) {
    val bodySp = rowStyle.fontSize?.value ?: 14f
    val metaSp = muted.fontSize?.value ?: bodySp
    val innerWidth =
        budgetRowInnerWidthDp(
            widgetWidthDp = LocalSize.current.width.value,
            horizontalPaddingDp = contentPadding.value,
        )
    val columns = budgetWidgetRowColumnWidths(innerWidth, bodySp, metaSp)
    val labelRowHeight =
        (slotHeight - progressBarHeight - gapSmall).coerceAtLeast(12.dp)
    val separatorHeight =
        labelRowHeight
            .value
            .coerceIn(12f, 28f)
            .dp

    Column(
        modifier =
            GlanceModifier
                .fillMaxWidth()
                .height(slotHeight),
    ) {
        Row(
            modifier =
                GlanceModifier
                    .fillMaxWidth()
                    .height(labelRowHeight),
            verticalAlignment = Alignment.Vertical.CenterVertically,
        ) {
            Text(
                text = row.name,
                modifier = GlanceModifier.width(columns.category),
                maxLines = 1,
                style = rowStyle,
            )
            BudgetWidgetColumnSeparator(chrome = chrome, height = separatorHeight)
            Text(
                text = row.periodKind,
                modifier = GlanceModifier.width(columns.period),
                maxLines = 1,
                style = muted,
            )
            BudgetWidgetColumnSeparator(chrome = chrome, height = separatorHeight)
            Text(
                text = "${row.percentUsed}%",
                modifier = GlanceModifier.width(columns.percent),
                maxLines = 1,
                style = percentStyle,
            )
        }
        BudgetWidgetProgressBar(
            fraction = row.progressFraction,
            isOverBudget = row.isOverBudget,
            chrome = chrome,
            barHeight = progressBarHeight,
            modifier = GlanceModifier.padding(top = gapSmall),
        )
    }
}

@Composable
private fun BudgetWidgetProgressBar(
    fraction: Float,
    isOverBudget: Boolean,
    chrome: MmrWidgetChromeColors,
    barHeight: Dp,
    modifier: GlanceModifier = GlanceModifier,
) {
    val progress = fraction.coerceIn(0f, 1f)
    val fillColor =
        ColorProvider(
            if (isOverBudget) Color(0xFFEF4444) else Color(0xFF5C6BC0),
        )
    val trackWidthDp = LocalSize.current.width.value.coerceAtLeast(160f)
    val fillWidthDp = (trackWidthDp * progress).coerceAtLeast(0f).dp
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .height(barHeight)
                .background(chrome.separator()),
    ) {
        if (progress > 0f) {
            Box(
                modifier =
                    GlanceModifier
                        .width(fillWidthDp)
                        .fillMaxHeight()
                        .background(fillColor),
            ) {}
        }
    }
}

@Composable
private fun BudgetWidgetColumnSeparator(
    chrome: MmrWidgetChromeColors,
    height: Dp,
) {
    val separatorHeight = height.coerceAtLeast(12.dp)
    Box(
        modifier =
            GlanceModifier
                .padding(horizontal = budgetWidgetColumnSeparatorMargin)
                .width(budgetWidgetColumnSeparatorWidth)
                .height(separatorHeight)
                .background(chrome.separator()),
    ) {}
}

@AndroidEntryPoint
class MmrBudgetsWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = MmrBudgetsWidget()

    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        EntryPointAccessors
            .fromApplication(context.applicationContext, MmrWidgetEntryPoint::class.java)
            .widgetRefresher()
            .refreshAll()
    }
}
