package com.rrparallelstudio.miapp.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.PreviewSizeMode
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.rrparallelstudio.miapp.core.data.preferences.AppDisplaySettingsSnapshot
import com.rrparallelstudio.miapp.core.parser.permission.SmsPermissionHelper
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.EntryPointAccessors

/** Disabled in the Play manifest; re-enable with [com.rrparallelstudio.miapp.core.parser.policy.MmrSmsTelephonyPolicy]. */
class MmrPendingApprovalsWidget : GlanceAppWidget() {
    override val sizeMode: SizeMode = MmrWidgetSizes.pendingResponsive

    override val previewSizeMode: PreviewSizeMode =
        SizeMode.Responsive(setOf(MmrWidgetSizes.square(2)))

    override suspend fun providePreview(
        context: Context,
        widgetCategory: Int,
    ) {
        val data = MmrWidgetPreviewSamples.pendingApprovals(context)
        provideContent {
            ApprovalTriageContent(
                context = context,
                data = data,
                canScanSms = true,
            )
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
            runCatching { loader.loadApprovalTriage() }.getOrElse {
                val chrome = MmrWidgetChrome.resolve(context, AppDisplaySettingsSnapshot())
                MmrApprovalTriageWidgetData(
                    pendingCount = 0,
                    isScanning = false,
                    headline = null,
                    chrome = chrome,
                )
            }
        val updates = loader.approvalTriageFlow()
        val canScanSms = SmsPermissionHelper.hasSmsPermission(context)
        provideContent {
            val data by updates.collectAsState(initial)
            ApprovalTriageContent(
                context = context,
                data = data,
                canScanSms = canScanSms,
            )
        }
    }
}

@Composable
private fun ApprovalTriageContent(
    context: Context,
    data: MmrApprovalTriageWidgetData,
    canScanSms: Boolean,
) {
    val text = rememberPendingWidgetTextScale()
    val chrome = data.chrome
    val approveGreen = ColorProvider(Color(0xFF2E7D32))
    val rejectRed = ColorProvider(Color(0xFFC62828))
    val accent = ColorProvider(Color(0xFF5C6BC0))
    Column(
        modifier =
            GlanceModifier
                .fillMaxSize()
                .background(chrome.background())
                .padding(text.contentPadding),
    ) {
        val scanAction =
            if (canScanSms) {
                actionRunCallback<MmrWidgetScanActionCallback>()
            } else {
                actionStartActivity(approvalCenterIntent(context))
            }
        Row(
            modifier = GlanceModifier.fillMaxWidth(),
            verticalAlignment = Alignment.Vertical.CenterVertically,
        ) {
            Text(
                text =
                    when {
                        data.isScanning -> "Scanning…"
                        data.pendingCount == 0 -> "No pending"
                        data.pendingCount == 1 -> "1 pending"
                        else -> "${data.pendingCount} pending"
                    },
                modifier = GlanceModifier.defaultWeight(),
                style =
                    TextStyle(
                        fontSize = text.header,
                        fontWeight = FontWeight.Medium,
                        color = chrome.onSurface(),
                    ),
            )
            Text(
                text = "↻",
                modifier =
                    GlanceModifier
                        .clickable(scanAction)
                        .padding(horizontal = text.gapMedium, vertical = text.gapSmall),
                style = TextStyle(fontSize = text.icon, fontWeight = FontWeight.Bold, color = accent),
            )
        }
        val headline = data.headline
        if (headline == null) {
            Column(
                modifier =
                    GlanceModifier
                        .defaultWeight()
                        .fillMaxWidth()
                        .clickable(actionStartActivity(approvalCenterIntent(context))),
                verticalAlignment = Alignment.Vertical.CenterVertically,
                horizontalAlignment = Alignment.Horizontal.CenterHorizontally,
            ) {
                Text(
                    text = if (data.pendingCount == 0) "All caught up" else "Open Approval Center",
                    style = TextStyle(fontSize = text.body, color = chrome.onSurfaceVariant()),
                )
            }
        } else {
            ApprovalTriageBody(
                context = context,
                headline = headline,
                pendingCount = data.pendingCount,
                chrome = chrome,
                approveGreen = approveGreen,
                rejectRed = rejectRed,
                text = text,
                modifier =
                    GlanceModifier
                        .defaultWeight()
                        .fillMaxWidth()
                        .padding(top = text.gapSmall),
            )
        }
    }
}

@Composable
private fun ApprovalTriageBody(
    context: Context,
    headline: MmrApprovalTriageHeadline,
    pendingCount: Int,
    chrome: MmrWidgetChromeColors,
    approveGreen: ColorProvider,
    rejectRed: ColorProvider,
    text: MmrWidgetTextScale,
    modifier: GlanceModifier = GlanceModifier,
) {
    val approvalId = headline.approvalId
    val params = approvalActionParams(approvalId)
    val previewMaxLines = rememberPendingSmsPreviewMaxLines()
    val previewText =
        headline.smsPreview.ifBlank {
            if (pendingCount > 1) {
                val others = pendingCount - 1
                if (others == 1) "1 more pending after this one." else "$others more pending after this one."
            } else {
                ""
            }
        }
    Column(
        modifier = modifier.fillMaxSize(),
    ) {
        Text(
            text = headline.amountFormatted,
            maxLines = 1,
            style = TextStyle(fontSize = text.amount, fontWeight = FontWeight.Bold, color = chrome.onSurface()),
        )
        Text(
            text = headline.subtitle,
            modifier = GlanceModifier.padding(top = text.gapSmall),
            maxLines = 1,
            style = TextStyle(fontSize = text.subtitle, color = chrome.onSurface()),
        )
        Text(
            text = headline.metaLine,
            modifier = GlanceModifier.padding(top = text.gapSmall),
            maxLines = 1,
            style = TextStyle(fontSize = text.meta, color = chrome.onSurfaceVariant()),
        )
        if (previewText.isNotBlank()) {
            Text(
                text = previewText,
                modifier =
                    GlanceModifier
                        .defaultWeight()
                        .fillMaxWidth()
                        .padding(top = text.gapMedium),
                maxLines = previewMaxLines,
                style = TextStyle(fontSize = text.body, color = chrome.onSurfaceVariant()),
            )
        } else {
            Spacer(GlanceModifier.defaultWeight())
        }
        Row(
            modifier =
                GlanceModifier
                    .fillMaxWidth()
                    .padding(top = text.gapMedium),
            horizontalAlignment = Alignment.Horizontal.CenterHorizontally,
        ) {
            TriageActionChip(
                label = "Reject",
                background = rejectRed,
                onClick = actionRunCallback<MmrWidgetRejectActionCallback>(params),
                text = text,
                modifier = GlanceModifier.defaultWeight(),
            )
            Spacer(GlanceModifier.width(text.gapMedium))
            val approveClick =
                if (headline.canQuickApprove) {
                    actionRunCallback<MmrWidgetApproveActionCallback>(params)
                } else {
                    actionStartActivity(
                        approvalCenterIntent(
                            context,
                            approvalId = approvalId,
                            action = ApprovalWidgetLaunch.LINK,
                        ),
                    )
                }
            TriageActionChip(
                label = if (headline.isCardRefund) "Apply" else "Approve",
                background = approveGreen,
                onClick = approveClick,
                text = text,
                modifier = GlanceModifier.defaultWeight(),
            )
        }
        Row(
            modifier =
                GlanceModifier
                    .fillMaxWidth()
                    .padding(top = text.gapSmall),
        ) {
            TriageTextAction(
                label = "Later",
                onClick = actionRunCallback<MmrWidgetLaterActionCallback>(params),
                text = text,
                modifier = GlanceModifier.defaultWeight(),
            )
            TriageTextAction(
                label = "View",
                onClick =
                    actionStartActivity(
                        approvalCenterIntent(
                            context,
                            approvalId = approvalId,
                            action = ApprovalWidgetLaunch.VIEW,
                        ),
                    ),
                text = text,
                modifier = GlanceModifier.defaultWeight(),
            )
            TriageTextAction(
                label = "Edit",
                onClick =
                    actionStartActivity(
                        approvalCenterIntent(
                            context,
                            approvalId = approvalId,
                            action = ApprovalWidgetLaunch.EDIT,
                        ),
                    ),
                text = text,
                modifier = GlanceModifier.defaultWeight(),
            )
            TriageTextAction(
                label = "Link",
                onClick =
                    actionStartActivity(
                        approvalCenterIntent(
                            context,
                            approvalId = approvalId,
                            action = ApprovalWidgetLaunch.LINK,
                        ),
                    ),
                text = text,
                modifier = GlanceModifier.defaultWeight(),
            )
        }
    }
}

@Composable
private fun TriageActionChip(
    label: String,
    background: ColorProvider,
    onClick: androidx.glance.action.Action,
    text: MmrWidgetTextScale,
    modifier: GlanceModifier = GlanceModifier,
) {
    Text(
        text = label,
        modifier =
            modifier
                .clickable(onClick)
                .background(background)
                .padding(vertical = text.chipVerticalPadding),
        maxLines = 1,
        style =
            TextStyle(
                fontSize = text.chip,
                fontWeight = FontWeight.Medium,
                color = ColorProvider(Color.White),
                textAlign = TextAlign.Center,
            ),
    )
}

@Composable
private fun TriageTextAction(
    label: String,
    onClick: androidx.glance.action.Action,
    text: MmrWidgetTextScale,
    modifier: GlanceModifier = GlanceModifier,
) {
    Text(
        text = label,
        modifier =
            modifier
                .clickable(onClick)
                .padding(vertical = text.gapSmall),
        maxLines = 1,
        style =
            TextStyle(
                fontSize = text.textAction,
                fontWeight = FontWeight.Medium,
                color = ColorProvider(Color(0xFF5C6BC0)),
                textAlign = TextAlign.Center,
            ),
    )
}

private fun approvalActionParams(approvalId: Long): ActionParameters =
    actionParametersOf(MmrWidgetActionKeys.approvalId to approvalId)

@AndroidEntryPoint
class MmrPendingApprovalsWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = MmrPendingApprovalsWidget()

    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        EntryPointAccessors
            .fromApplication(context.applicationContext, MmrWidgetEntryPoint::class.java)
            .widgetRefresher()
            .refreshAll()
    }
}
