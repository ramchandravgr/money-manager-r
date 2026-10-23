package com.mmr.feature.home.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mmr.app.R
import com.mmr.app.testing.MmrTestTags
import com.mmr.app.ui.components.MmrDimens
import com.mmr.app.ui.components.MmrMmHomeTopBar
import com.mmr.app.ui.components.MmrMmSummaryStrip
import com.mmr.app.ui.components.MmrMmTabRow
import com.mmr.app.ui.theme.MmrMmColors
import com.mmr.core.model.settings.MmrAmountColorScheme
import com.mmr.feature.home.model.HomeSummaryUi
import com.mmr.feature.home.model.HomeViewTab
import com.mmr.feature.home.ui.formatRupeeAmount

private val HomeMonthTitleStyle
    @Composable get() =
        MaterialTheme.typography.labelMedium.copy(
            fontSize = 12.sp,
            lineHeight = 14.sp,
            fontWeight = FontWeight.SemiBold,
        )

@Composable
fun HomeMonthTopBar(
    monthTitle: String,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onMonthTitleClick: () -> Unit,
    onSearchClick: () -> Unit,
    pendingApprovalCount: Int,
    onApprovalsClick: () -> Unit,
) {
    MmrMmHomeTopBar(
        monthTitle = monthTitle,
        onPreviousMonth = onPreviousMonth,
        onNextMonth = onNextMonth,
        onMonthTitleClick = onMonthTitleClick,
        onSearchClick = onSearchClick,
        onApprovalsClick = onApprovalsClick,
        pendingApprovalCount = pendingApprovalCount,
    )
}

@Composable
private fun HomeMonthPickerControl(
    monthTitle: String,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onMonthTitleClick: () -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
        modifier = Modifier.height(MmrDimens.homeTopBarIconButton),
    ) {
        IconButton(
            onClick = onPreviousMonth,
            modifier = Modifier.size(MmrDimens.homeTopBarIconButton),
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                contentDescription = "Previous month",
                modifier = Modifier.size(MmrDimens.homeIconSm),
            )
        }
        Text(
            text = monthTitle,
            style = HomeMonthTitleStyle,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier =
                Modifier
                    .width(MmrDimens.homeMonthTitleWidth)
                    .clickable(onClick = onMonthTitleClick),
        )
        IconButton(
            onClick = onNextMonth,
            modifier = Modifier.size(MmrDimens.homeTopBarIconButton),
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = "Next month",
                modifier = Modifier.size(MmrDimens.homeIconSm),
            )
        }
    }
}

@Composable
private fun HomeAppBrandIcon(modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(MmrDimens.spaceSm),
        color = Color(0xFF0F3468),
    ) {
        Image(
            painter = painterResource(R.drawable.ic_launcher_foreground),
            contentDescription = "MMR",
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(2.dp),
        )
    }
}

@Composable
private fun HomeTopBarIconButton(
    onClick: () -> Unit,
    contentDescription: String,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    IconButton(
        onClick = onClick,
        modifier = modifier.size(MmrDimens.homeTopBarIconButton),
        content = content,
    )
}

@Composable
private fun HomeTopBarApprovalsButton(
    pendingCount: Int,
    onClick: () -> Unit,
) {
    val badgeLabel =
        when {
            pendingCount <= 0 -> null
            pendingCount > 99 -> "99+"
            else -> pendingCount.toString()
        }
    Box(
        modifier =
            Modifier
                .width(MmrDimens.homeTopBarIconButton + 10.dp)
                .height(MmrDimens.homeTopBarIconButton),
        contentAlignment = Alignment.Center,
    ) {
        IconButton(
            onClick = onClick,
            modifier = Modifier.size(MmrDimens.homeTopBarIconButton),
        ) {
            Icon(
                imageVector = Icons.Outlined.Email,
                contentDescription = "Pending approvals",
                modifier = Modifier.size(MmrDimens.homeIconSm),
            )
        }
        if (badgeLabel != null) {
            Surface(
                modifier =
                    Modifier
                        .align(Alignment.TopEnd)
                        .offset(x = 0.dp, y = 0.dp),
                shape = RoundedCornerShape(6.dp),
                color = MaterialTheme.colorScheme.error,
            ) {
                Text(
                    text = badgeLabel,
                    modifier = Modifier.padding(horizontal = 3.dp, vertical = 1.dp),
                    style =
                        MaterialTheme.typography.labelSmall.copy(
                            fontSize = 8.sp,
                            lineHeight = 9.sp,
                            fontWeight = FontWeight.Bold,
                        ),
                    color = MaterialTheme.colorScheme.onError,
                    maxLines = 1,
                )
            }
        }
    }
}

@Composable
fun HomeViewTabRow(
    selectedTab: HomeViewTab,
    onTabSelected: (HomeViewTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    val tabs = HomeViewTab.entries
    MmrMmTabRow(
        tabs = tabs.map { it.mmLabel },
        selectedIndex = tabs.indexOf(selectedTab).coerceAtLeast(0),
        onTabSelected = { index -> onTabSelected(tabs[index]) },
        modifier = modifier,
        testTagForIndex = { index -> MmrTestTags.homeViewTab(tabs[index]) },
    )
}

@Composable
fun HomePeriodSummaryStrip(
    summary: HomeSummaryUi,
    amountColorScheme: MmrAmountColorScheme = MmrAmountColorScheme.SET_B,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        MmrMmSummaryStrip(
            incomeLabel = "Income",
            incomeAmount = formatRupeeAmount(summary.totalIncome),
            expenseLabel = "Expenses",
            expenseAmount = formatRupeeAmount(summary.totalExpense),
            totalLabel = "Total",
            totalAmount = formatRupeeAmount(summary.netBalance),
            amountColorScheme = amountColorScheme,
            incomeAmountTestTag = MmrTestTags.homeSummaryIncome,
            expenseAmountTestTag = MmrTestTags.homeSummaryExpense,
            totalAmountTestTag = MmrTestTags.homeSummaryTotal,
        )
        if (summary.carriedForward != 0.0) {
            Text(
                text = "Includes ${formatRupeeAmount(summary.carriedForward)} carried forward",
                style = MaterialTheme.typography.labelSmall,
                color = MmrMmColors.TextMedium,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(bottom = 6.dp, start = 16.dp, end = 16.dp),
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun SummaryInlineItem(
    label: String,
    amount: Double,
    color: Color,
    prefix: String,
) {
    Text(
        text = "$label $prefix₹${"%.2f".format(kotlin.math.abs(amount))}",
        style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp, lineHeight = 13.sp),
        color = color,
        fontWeight = FontWeight.Medium,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        textAlign = TextAlign.Center,
    )
}
