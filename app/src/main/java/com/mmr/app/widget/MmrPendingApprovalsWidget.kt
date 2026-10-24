package com.mmr.app.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
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
import com.mmr.core.parser.permission.SmsPermissionHelper
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.EntryPointAccessors

class MmrPendingApprovalsWidget : GlanceAppWidget() {
    override suspend fun provideGlance(
        context: Context,
        id: GlanceId,
    ) {
        val loader =
            EntryPointAccessors
                .fromApplication(context.applicationContext, MmrWidgetEntryPoint::class.java)
                .widgetSnapshotLoader()
        val data =
            runCatching { loader.loadApprovalTriage() }.getOrElse {
                MmrApprovalTriageWidgetData(pendingCount = 0, isScanning = false, headline = null)
            }
        val canScanSms = SmsPermissionHelper.hasSmsPermission(context)
        provideContent {
            GlanceTheme {
                ApprovalTriageContent(
                    context = context,
                    data = data,
                    canScanSms = canScanSms,
                )
            }
        }
    }
}

@Composable
private fun ApprovalTriageContent(
    context: Context,
    data: MmrApprovalTriageWidgetData,
    canScanSms: Boolean,
) {
    val approveGreen = ColorProvider(Color(0xFF2E7D32))
    val rejectRed = ColorProvider(Color(0xFFC62828))
    val accent = ColorProvider(Color(0xFF5C6BC0))
    Column(
        modifier =
            GlanceModifier
                .fillMaxSize()
                .background(GlanceTheme.colors.widgetBackground)
                .padding(8.dp),
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
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = GlanceTheme.colors.onSurface,
                    ),
            )
            Text(
                text = "↻",
                modifier =
                    GlanceModifier
                        .clickable(scanAction)
                        .padding(horizontal = 6.dp, vertical = 2.dp),
                style = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Bold, color = accent),
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
                    style = TextStyle(fontSize = 13.sp, color = GlanceTheme.colors.onSurfaceVariant),
                )
            }
        } else {
            ApprovalTriageBody(
                context = context,
                headline = headline,
                approveGreen = approveGreen,
                rejectRed = rejectRed,
                modifier =
                    GlanceModifier
                        .defaultWeight()
                        .fillMaxWidth()
                        .padding(top = 4.dp),
            )
        }
    }
}

@Composable
private fun ApprovalTriageBody(
    context: Context,
    headline: MmrApprovalTriageHeadline,
    approveGreen: ColorProvider,
    rejectRed: ColorProvider,
    modifier: GlanceModifier = GlanceModifier,
) {
    val approvalId = headline.approvalId
    val params = approvalActionParams(approvalId)
    Column(
        modifier = modifier,
    ) {
        Text(
            text = headline.amountFormatted,
            maxLines = 1,
            style = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.Bold, color = GlanceTheme.colors.onSurface),
        )
        Text(
            text = headline.subtitle,
            modifier = GlanceModifier.padding(top = 2.dp),
            maxLines = 1,
            style = TextStyle(fontSize = 11.sp, color = GlanceTheme.colors.onSurface),
        )
        Text(
            text = headline.metaLine,
            modifier = GlanceModifier.padding(top = 1.dp),
            maxLines = 1,
            style = TextStyle(fontSize = 10.sp, color = GlanceTheme.colors.onSurfaceVariant),
        )
        Row(
            modifier =
                GlanceModifier
                    .fillMaxWidth()
                    .padding(top = 6.dp),
            horizontalAlignment = Alignment.Horizontal.CenterHorizontally,
        ) {
            TriageActionChip(
                label = "Reject",
                background = rejectRed,
                onClick = actionRunCallback<MmrWidgetRejectActionCallback>(params),
                modifier = GlanceModifier.defaultWeight(),
            )
            Spacer(GlanceModifier.width(6.dp))
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
                modifier = GlanceModifier.defaultWeight(),
            )
        }
        Row(
            modifier =
                GlanceModifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
        ) {
            TriageTextAction(
                label = "Later",
                onClick = actionRunCallback<MmrWidgetLaterActionCallback>(params),
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
    modifier: GlanceModifier = GlanceModifier,
) {
    Text(
        text = label,
        modifier =
            modifier
                .clickable(onClick)
                .background(background)
                .padding(vertical = 6.dp),
        maxLines = 1,
        style =
            TextStyle(
                fontSize = 12.sp,
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
    modifier: GlanceModifier = GlanceModifier,
) {
    Text(
        text = label,
        modifier =
            modifier
                .clickable(onClick)
                .padding(vertical = 2.dp),
        maxLines = 1,
        style =
            TextStyle(
                fontSize = 10.sp,
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
