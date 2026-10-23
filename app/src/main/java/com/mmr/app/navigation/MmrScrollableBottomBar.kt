package com.mmr.app.navigation

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ListAlt
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Autorenew
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.mmr.app.testing.MmrTestTags
import com.mmr.app.ui.components.MmrDimens
import com.mmr.app.ui.theme.MmrMmColors

@Composable
fun MmrScrollableBottomBar(
    destinations: List<MmrMainDestination>,
    currentRoute: String?,
    onDestinationSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val showLabels = maxWidth >= 380.dp
        NavigationBar(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .height(MmrDimens.mmBottomNavHeight),
            containerColor = MmrMmColors.NavBackground,
            tonalElevation = 0.dp,
            windowInsets = WindowInsets(0, 0, 0, 0),
        ) {
            destinations.forEach { destination ->
                val selected = currentRoute == destination.route
                NavigationBarItem(
                    modifier = Modifier.testTag(MmrTestTags.navTab(destination.route)),
                    selected = selected,
                    onClick = { onDestinationSelected(destination.route) },
                    icon = {
                        Icon(
                            imageVector = destination.icon,
                            contentDescription = destination.label,
                            modifier = Modifier.size(MmrDimens.iconSm),
                        )
                    },
                    label = {
                        Text(
                            text = destination.shortLabel,
                            style = MaterialTheme.typography.labelSmall,
                            maxLines = 1,
                        )
                    },
                    alwaysShowLabel = showLabels,
                    colors =
                        NavigationBarItemDefaults.colors(
                            selectedIconColor = MmrMmColors.NavIconSelected,
                            selectedTextColor = MmrMmColors.NavIconSelected,
                            indicatorColor = MmrMmColors.NavBackground,
                            unselectedIconColor = MmrMmColors.NavIcon,
                            unselectedTextColor = MmrMmColors.NavIcon,
                        ),
                )
            }
        }
    }
}

internal fun mmrDestinationIcon(route: String): ImageVector =
    when (route) {
        Screen.Home.route -> Icons.AutoMirrored.Filled.ListAlt
        Screen.Stats.route -> Icons.Filled.BarChart
        Screen.Loans.route -> Icons.Filled.AccountBalance
        Screen.Recurring.route -> Icons.Filled.Autorenew
        Screen.Settings.route -> Icons.Filled.Settings
        else -> Icons.Filled.Settings
    }
