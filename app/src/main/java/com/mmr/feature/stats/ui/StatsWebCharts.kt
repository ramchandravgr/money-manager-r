package com.mmr.feature.stats.ui

import android.annotation.SuppressLint
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.viewinterop.AndroidView
import com.mmr.app.ui.components.MmrDimens
import com.mmr.feature.stats.model.CategoryBreakdownUi
import com.mmr.feature.stats.model.MonthlyChartPointUi
import android.graphics.Color as AndroidColor

private const val CHART_PIE_ASSET = "chart/pie.html"
private const val CHART_LINE_ASSET = "chart/line.html"

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun StatsWebPieChart(
    breakdowns: List<CategoryBreakdownUi>,
    modifier: Modifier = Modifier,
    height: Dp = MmrDimens.statsPieHeight,
    noDataMessage: String = "No data",
) {
    val chartJson =
        remember(breakdowns, noDataMessage) {
            StatsChartJsonBuilder.pieChartJson(breakdowns, noDataMessage)
        }
    MmrHighchartsWebView(
        assetHtmlPath = CHART_PIE_ASSET,
        chartJson = chartJson,
        modifier = modifier,
        height = height,
    )
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun StatsWebLineChart(
    points: List<MonthlyChartPointUi>,
    lineColor: Color,
    modifier: Modifier = Modifier,
    height: Dp = MmrDimens.statsDetailLineChartHeight,
    noDataMessage: String = "No data",
) {
    val lineColorHex =
        remember(lineColor) {
            val rgb = lineColor.toArgb() and 0xFFFFFF
            String.format("#%06X", rgb)
        }
    val chartJson =
        remember(points, lineColorHex, noDataMessage) {
            StatsChartJsonBuilder.lineChartJson(points, lineColorHex, noDataMessage)
        }
    MmrHighchartsWebView(
        assetHtmlPath = CHART_LINE_ASSET,
        chartJson = chartJson,
        modifier = modifier,
        height = height,
    )
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun MmrHighchartsWebView(
    assetHtmlPath: String,
    chartJson: String,
    modifier: Modifier = Modifier,
    height: Dp,
) {
    val jsonState = rememberUpdatedState(chartJson)
    val backgroundArgb = remember { AndroidColor.WHITE }

    AndroidView(
        modifier =
            modifier
                .fillMaxWidth()
                .height(height),
        factory = { ctx ->
            WebView(ctx).apply {
                setLayerType(WebView.LAYER_TYPE_SOFTWARE, null)
                setBackgroundColor(backgroundArgb)
                isVerticalScrollBarEnabled = false
                isHorizontalScrollBarEnabled = false
                isLongClickable = false
                setOnLongClickListener { true }
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                settings.allowFileAccess = true
                addJavascriptInterface(
                    object {
                        @JavascriptInterface
                        fun drawChart() {
                            post { injectChart(this@apply, jsonState.value) }
                        }
                    },
                    "androidActivity",
                )
                webViewClient =
                    object : WebViewClient() {
                        override fun onPageFinished(
                            view: WebView?,
                            url: String?,
                        ) {
                            super.onPageFinished(view, url)
                            view?.post { injectChart(view, jsonState.value) }
                        }
                    }
                loadUrl("file:///android_asset/$assetHtmlPath")
            }
        },
        update = { webView ->
            webView.post { injectChart(webView, chartJson) }
        },
    )
}

private fun injectChart(
    webView: WebView,
    chartJson: String,
) {
    webView.evaluateJavascript(
        "if(typeof drawChart==='function'){drawChart($chartJson);}",
        null,
    )
}
