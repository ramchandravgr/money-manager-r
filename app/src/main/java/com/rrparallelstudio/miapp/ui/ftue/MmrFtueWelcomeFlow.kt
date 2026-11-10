package com.rrparallelstudio.miapp.ui.ftue

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.rrparallelstudio.miapp.testing.MmrTestTags
import com.rrparallelstudio.miapp.ui.components.MmrDimens
import com.rrparallelstudio.miapp.ui.components.MmrFooterCtaBar
import com.rrparallelstudio.miapp.ui.theme.MmrBrandColors
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun MmrFtueWelcomeFlow(
    onComplete: () -> Unit,
    onSkip: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val steps = FtueWelcomeCatalog.steps
    val pagerState = rememberPagerState(pageCount = { steps.size })
    val scope = rememberCoroutineScope()
    val lastPage = steps.lastIndex
    val pageSurface = MmrBrandColors.dialogBodySurface()

    Scaffold(
        modifier =
            modifier
                .fillMaxSize()
                .testTag(MmrTestTags.ftueScreen)
                .background(pageSurface),
        containerColor = pageSurface,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Introduction",
                        color = MmrBrandColors.dialogTitleText(),
                    )
                },
                actions = {
                    TextButton(
                        onClick = onSkip,
                        modifier = Modifier.testTag(MmrTestTags.ftueSkip),
                    ) {
                        Text("Skip", color = MmrBrandColors.Active)
                    }
                },
                colors =
                    TopAppBarDefaults.topAppBarColors(
                        containerColor = pageSurface,
                        titleContentColor = MmrBrandColors.dialogTitleText(),
                        actionIconContentColor = MmrBrandColors.Active,
                    ),
                modifier = Modifier.statusBarsPadding(),
            )
        },
        bottomBar = {
            val onLastPage = pagerState.currentPage >= lastPage
            MmrFooterCtaBar(
                modifier = Modifier.navigationBarsPadding(),
                bandBackground = pageSurface,
                primaryLabel = if (onLastPage) "Get started" else "Next",
                onPrimary = {
                    if (onLastPage) {
                        onComplete()
                    } else {
                        scope.launch {
                            pagerState.animateScrollToPage(pagerState.currentPage + 1)
                        }
                    }
                },
                primaryTestTag = if (onLastPage) MmrTestTags.ftueDone else MmrTestTags.ftueNext,
            )
        },
    ) { innerPadding ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = MmrDimens.cardPadding),
        ) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.weight(1f),
                userScrollEnabled = true,
            ) { page ->
                FtueStepPage(step = steps[page])
            }
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                repeat(steps.size) { index ->
                    val selected = pagerState.currentPage == index
                    Box(
                        modifier =
                            Modifier
                                .padding(horizontal = 4.dp)
                                .size(if (selected) 8.dp else 6.dp)
                                .clip(CircleShape)
                                .background(
                                    if (selected) {
                                        MmrBrandColors.Active
                                    } else {
                                        MmrBrandColors.dialogMessageText().copy(alpha = 0.35f)
                                    },
                                ),
                    )
                }
            }
        }
    }
}

@Composable
private fun FtueStepPage(step: FtueWelcomeStep) {
    val scroll = rememberScrollState()
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .verticalScroll(scroll)
                .padding(top = 4.dp, bottom = 8.dp),
        verticalArrangement = Arrangement.Top,
    ) {
        Text(
            text = step.title,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.SemiBold,
            color = MmrBrandColors.dialogTitleText(),
        )
        Text(
            text = step.body,
            style = MaterialTheme.typography.bodyLarge,
            color = MmrBrandColors.dialogMessageText(),
            modifier = Modifier.padding(top = 12.dp),
        )
        if (step.bullets.isNotEmpty()) {
            Surface(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp),
                shape = RoundedCornerShape(12.dp),
                color = MmrBrandColors.settingsSheetGroupedListFill(),
                tonalElevation = 0.dp,
                shadowElevation = 0.dp,
            ) {
                Column(
                    modifier =
                        Modifier.padding(
                            horizontal = MmrDimens.cardPadding,
                            vertical = 12.dp,
                        ),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    step.bullets.forEach { bullet ->
                        FtueBulletRow(bullet = bullet)
                    }
                }
            }
        }
    }
}

@Composable
private fun FtueBulletRow(bullet: FtueBullet) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top,
    ) {
        val icon = bullet.icon
        if (icon != null) {
            val visual = ftueBulletIconVisual(icon)
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = visual.chipBackground,
            ) {
                Icon(
                    imageVector = visual.imageVector,
                    contentDescription = null,
                    tint = visual.tint,
                    modifier =
                        Modifier
                            .padding(6.dp)
                            .size(22.dp),
                )
            }
        } else {
            Box(modifier = Modifier.size(34.dp))
        }
        Text(
            text = bullet.text,
            style = MaterialTheme.typography.bodyMedium,
            color = MmrBrandColors.settingsSheetListRowText(),
            modifier = Modifier.weight(1f),
        )
    }
}
