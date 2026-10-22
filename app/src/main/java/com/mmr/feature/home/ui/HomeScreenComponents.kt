package com.mmr.feature.home.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mmr.app.R
import com.mmr.feature.home.model.HomeSummaryUi
import com.mmr.feature.home.model.HomeViewTab

private val TopBarActionSize = 40.dp
private val TopBarIconSize = 22.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeMonthTopBar(
    monthTitle: String,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onMonthTitleClick: () -> Unit,
    onSearchClick: () -> Unit,
    pendingApprovalCount: Int,
    onApprovalsClick: () -> Unit
) {
    CenterAlignedTopAppBar(
        navigationIcon = {
            HomeAppBrandIcon(modifier = Modifier.padding(start = 4.dp))
        },
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                TextButton(
                    onClick = onPreviousMonth,
                    modifier = Modifier.size(TopBarActionSize),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Text("‹", style = MaterialTheme.typography.titleLarge)
                }
                Text(
                    text = monthTitle,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .clickable(onClick = onMonthTitleClick)
                        .padding(horizontal = 4.dp, vertical = 4.dp)
                )
                TextButton(
                    onClick = onNextMonth,
                    modifier = Modifier.size(TopBarActionSize),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Text("›", style = MaterialTheme.typography.titleLarge)
                }
            }
        },
        actions = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                modifier = Modifier.padding(end = 4.dp)
            ) {
                HomeTopBarIconButton(
                    onClick = onSearchClick,
                    contentDescription = "Search"
                ) {
                    Icon(
                        imageVector = Icons.Filled.Search,
                        contentDescription = null,
                        modifier = Modifier.size(TopBarIconSize)
                    )
                }
                HomeTopBarApprovalsButton(
                    pendingCount = pendingApprovalCount,
                    onClick = onApprovalsClick
                )
            }
        },
        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface,
            titleContentColor = MaterialTheme.colorScheme.onSurface
        )
    )
}

@Composable
private fun HomeAppBrandIcon(modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.size(36.dp),
        shape = RoundedCornerShape(10.dp),
        color = Color(0xFF0F3468)
    ) {
        Image(
            painter = painterResource(R.drawable.ic_launcher_foreground),
            contentDescription = "MMR",
            modifier = Modifier
                .fillMaxSize()
                .padding(4.dp)
        )
    }
}

@Composable
private fun HomeTopBarIconButton(
    onClick: () -> Unit,
    contentDescription: String,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    IconButton(
        onClick = onClick,
        modifier = modifier.size(TopBarActionSize),
        content = content
    )
}

@Composable
private fun HomeTopBarApprovalsButton(
    pendingCount: Int,
    onClick: () -> Unit
) {
    val badgeLabel = when {
        pendingCount <= 0 -> null
        pendingCount > 99 -> "99+"
        else -> pendingCount.toString()
    }
    BadgedBox(
        badge = {
            if (badgeLabel != null) {
                Badge(
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onError
                ) {
                    Text(
                        text = badgeLabel,
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp)
                    )
                }
            }
        }
    ) {
        IconButton(
            onClick = onClick,
            modifier = Modifier.size(TopBarActionSize)
        ) {
            Icon(
                imageVector = Icons.Outlined.Email,
                contentDescription = "Pending approvals",
                modifier = Modifier.size(TopBarIconSize)
            )
        }
    }
}

@Composable
fun HomeViewTabRow(
    selectedTab: HomeViewTab,
    onTabSelected: (HomeViewTab) -> Unit,
    modifier: Modifier = Modifier
) {
    val tabs = HomeViewTab.entries
    ScrollableTabRow(
        selectedTabIndex = tabs.indexOf(selectedTab).coerceAtLeast(0),
        modifier = modifier.fillMaxWidth(),
        edgePadding = 8.dp,
        divider = {}
    ) {
        tabs.forEach { tab ->
            Tab(
                selected = selectedTab == tab,
                onClick = { onTabSelected(tab) },
                text = {
                    Text(
                        text = tab.name.lowercase().replaceFirstChar { it.uppercase() },
                        fontWeight = if (selectedTab == tab) FontWeight.Bold else FontWeight.Normal
                    )
                }
            )
        }
    }
}

@Composable
fun HomePeriodSummaryStrip(
    summary: HomeSummaryUi,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            SummaryColumn(
                label = "Income",
                amount = summary.totalIncome,
                color = Color(0xFF2563EB),
                prefix = ""
            )
            SummaryColumn(
                label = "Expenses",
                amount = summary.totalExpense,
                color = Color(0xFFEF4444),
                prefix = ""
            )
            SummaryColumn(
                label = "Total",
                amount = summary.netBalance,
                color = MaterialTheme.colorScheme.onSurface,
                prefix = if (summary.netBalance >= 0) "" else "-"
            )
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
    }
}

@Composable
private fun SummaryColumn(
    label: String,
    amount: Double,
    color: Color,
    prefix: String
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = "$prefix${"%.2f".format(kotlin.math.abs(amount))}",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = color,
            textAlign = TextAlign.Center
        )
    }
}
