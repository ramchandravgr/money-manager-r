package com.mmr.app.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.mmr.app.testing.MmrTestTags
import com.mmr.app.ui.adaptive.currentMmrWidthSizeClass
import com.mmr.app.ui.adaptive.isCompactWidth
import com.mmr.app.ui.theme.MmrBrandColors

@Composable
fun MmrMainNavigationScaffold(
    currentRoute: String?,
    onDestinationSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable (Modifier) -> Unit,
) {
    val useNavigationRail = !currentMmrWidthSizeClass().isCompactWidth()

    if (useNavigationRail) {
        Row(
            modifier =
                modifier
                    .fillMaxSize()
                    .windowInsetsPadding(WindowInsets.safeDrawing),
        ) {
            NavigationRail(containerColor = MmrBrandColors.NavBackground) {
                Column(modifier = Modifier.padding(vertical = 8.dp)) {
                    MmrMainDestinations.forEach { destination ->
                        val selected = currentRoute == destination.route
                        MmrMainNavItem(
                            modifier =
                                Modifier
                                    .testTag(MmrTestTags.navTab(destination.route))
                                    .padding(vertical = 4.dp),
                            selected = selected,
                            onClick = { onDestinationSelected(destination.route) },
                            iconPair = destination.icons,
                            label = destination.label,
                            showLabel = true,
                        )
                    }
                }
            }
            Box(
                modifier =
                    Modifier
                        .weight(1f)
                        .fillMaxHeight(),
            ) {
                content(Modifier.fillMaxSize())
            }
        }
    } else {
        Scaffold(
            modifier = modifier,
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            bottomBar = {
                MmrScrollableBottomBar(
                    destinations = MmrMainDestinations,
                    currentRoute = currentRoute,
                    onDestinationSelected = onDestinationSelected,
                )
            },
        ) { innerPadding ->
            content(Modifier.padding(innerPadding))
        }
    }
}

@Composable
fun MmrSecondaryScaffold(
    modifier: Modifier = Modifier,
    content: @Composable (Modifier) -> Unit,
) {
    Scaffold(
        modifier = modifier,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
    ) { innerPadding ->
        Box(
            modifier =
                Modifier
                    .padding(innerPadding)
                    .windowInsetsPadding(WindowInsets.navigationBars),
        ) {
            content(Modifier.fillMaxSize())
        }
    }
}
