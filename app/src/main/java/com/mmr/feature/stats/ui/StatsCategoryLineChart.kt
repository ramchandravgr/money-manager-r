package com.mmr.feature.stats.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mmr.feature.stats.model.MonthlyChartPointUi
import kotlin.math.max

@Composable
fun StatsCategoryLineChart(
    points: List<MonthlyChartPointUi>,
    lineColor: Color,
    modifier: Modifier = Modifier,
) {
    if (points.isEmpty()) return
    val textMeasurer = rememberTextMeasurer()
    val labelStyle = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp)
    val maxAmount = max(points.maxOf { it.amount }, 1.0)
    Canvas(
        modifier =
            modifier
                .fillMaxWidth()
                .height(com.mmr.app.ui.components.MmrDimens.statsDetailLineChartHeight)
                .padding(horizontal = 8.dp, vertical = 4.dp),
    ) {
        val chartBottom = size.height - 24f
        val chartTop = 8f
        val chartLeft = 28f
        val chartRight = size.width
        val stepX = if (points.size <= 1) 0f else (chartRight - chartLeft) / (points.size - 1)

        val path = Path()
        points.forEachIndexed { index, point ->
            val x = chartLeft + stepX * index
            val y = chartBottom - ((point.amount / maxAmount) * (chartBottom - chartTop)).toFloat()
            if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
            drawCircle(color = lineColor, radius = 4f, center = Offset(x, y))
        }
        drawPath(path, color = lineColor, style = Stroke(width = 3f))

        points.forEachIndexed { index, point ->
            val x = chartLeft + stepX * index
            val label = textMeasurer.measure(text = point.label, style = labelStyle)
            drawText(
                textLayoutResult = label,
                topLeft = Offset(x - label.size.width / 2f, chartBottom + 4f),
            )
        }
    }
}
