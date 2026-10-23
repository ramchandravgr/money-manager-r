package com.mmr.feature.stats.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun StatsPullToRefreshContainer(
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val pullState = rememberPullRefreshState(isRefreshing, onRefresh)
    Box(
        modifier =
            modifier
                .fillMaxSize()
                .pullRefresh(pullState),
    ) {
        content()
        PullRefreshIndicator(
            refreshing = isRefreshing,
            state = pullState,
            modifier = Modifier.align(Alignment.TopCenter),
        )
    }
}
