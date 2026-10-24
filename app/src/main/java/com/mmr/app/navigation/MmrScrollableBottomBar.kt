package com.mmr.app.navigation

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.mmr.app.testing.MmrTestTags
import com.mmr.app.ui.components.MmrDimens
import com.mmr.app.ui.theme.MmrBrandColors

@Composable
fun MmrScrollableBottomBar(
    destinations: List<MmrMainDestination>,
    currentRoute: String?,
    onDestinationSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val showLabels = maxWidth >= 360.dp
        Surface(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.navigationBars),
            color = MmrBrandColors.NavBackground,
            shadowElevation = 10.dp,
            tonalElevation = 0.dp,
        ) {
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(MmrDimens.chromeBottomNavHeight),
            ) {
                destinations.forEach { destination ->
                    val selected = currentRoute == destination.route
                    MmrMainNavItem(
                        modifier =
                            Modifier
                                .weight(1f)
                                .testTag(MmrTestTags.navTab(destination.route)),
                        selected = selected,
                        onClick = { onDestinationSelected(destination.route) },
                        iconPair = destination.icons,
                        label = destination.shortLabel,
                        showLabel = showLabels,
                    )
                }
            }
        }
    }
}
