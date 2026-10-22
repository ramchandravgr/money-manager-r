package com.mmr.app.navigation

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.ui.unit.dp

@Composable
fun MmrScrollableBottomBar(
    destinations: List<MmrMainDestination>,
    currentRoute: String?,
    onDestinationSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val showLabels = maxWidth >= 380.dp
        NavigationBar(
            modifier = Modifier.fillMaxWidth(),
            containerColor = MaterialTheme.colorScheme.surface,
            tonalElevation = 3.dp
        ) {
            destinations.forEach { destination ->
                val selected = currentRoute == destination.route
                NavigationBarItem(
                    selected = selected,
                    onClick = { onDestinationSelected(destination.route) },
                    icon = {
                        Icon(
                            imageVector = destination.icon,
                            contentDescription = destination.label
                        )
                    },
                    label = {
                        Text(
                            text = destination.shortLabel,
                            style = MaterialTheme.typography.labelSmall,
                            maxLines = 1
                        )
                    },
                    alwaysShowLabel = showLabels,
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.primary,
                    selectedTextColor = MaterialTheme.colorScheme.primary,
                    indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
            }
        }
    }
}

internal fun mmrDestinationIcon(route: String): ImageVector = when (route) {
    Screen.Home.route -> Icons.AutoMirrored.Filled.ListAlt
    Screen.Stats.route -> Icons.Filled.BarChart
    Screen.Loans.route -> Icons.Filled.AccountBalance
    Screen.Recurring.route -> Icons.Filled.Autorenew
    Screen.Settings.route -> Icons.Filled.Settings
    else -> Icons.Filled.Settings
}
