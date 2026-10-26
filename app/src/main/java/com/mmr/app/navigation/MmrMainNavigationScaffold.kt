package com.mmr.app.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.sp
import com.mmr.app.testing.MmrTestTags
import com.mmr.app.ui.adaptive.currentMmrWidthSizeClass
import com.mmr.app.ui.adaptive.isCompactWidth
import com.mmr.app.ui.components.MmrDimens
import com.mmr.app.ui.theme.MmrBrandColors
import com.mmr.app.ui.theme.MmrFontFamily

@Composable
fun MmrMainNavigationScaffold(
    destinations: List<MmrMainDestination>,
    currentRoute: String?,
    onDestinationSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable (Modifier) -> Unit,
) {
    val useNavigationRail = !currentMmrWidthSizeClass().isCompactWidth()

    if (useNavigationRail) {
        Row(modifier = modifier.fillMaxSize()) {
            NavigationRail(containerColor = MmrBrandColors.NavBackground) {
                destinations.forEach { destination ->
                    val selected = isMainDestinationSelected(currentRoute, destination)
                    val iconPair = destination.icons
                    NavigationRailItem(
                        modifier = Modifier.testTag(MmrTestTags.navTab(destination.route)),
                        selected = selected,
                        onClick = { onDestinationSelected(destination.route) },
                        alwaysShowLabel = true,
                        icon = {
                            Icon(
                                imageVector = if (selected) iconPair.filled else iconPair.outlined,
                                contentDescription = destination.label,
                                modifier = Modifier.size(MmrDimens.bottomNavIconSize),
                            )
                        },
                        label = {
                            Text(
                                text = destination.label,
                                fontFamily = MmrFontFamily,
                                fontSize = 11.sp,
                                lineHeight = 13.sp,
                                fontWeight = if (selected) FontWeight.Medium else FontWeight.Normal,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                            )
                        },
                        colors =
                            NavigationRailItemDefaults.colors(
                                selectedIconColor = MmrBrandColors.NavIconSelected,
                                selectedTextColor = MmrBrandColors.NavIconSelected,
                                unselectedIconColor = MmrBrandColors.NavIcon,
                                unselectedTextColor = MmrBrandColors.NavIcon,
                                indicatorColor = MmrBrandColors.NavIconSelected.copy(alpha = 0.14f),
                            ),
                    )
                }
            }
            Box(
                modifier =
                    Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .windowInsetsPadding(
                            WindowInsets.navigationBars.only(WindowInsetsSides.Horizontal),
                        ),
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
                    destinations = destinations,
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
