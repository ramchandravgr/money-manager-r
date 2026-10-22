package com.mmr.app.navigation

import androidx.compose.ui.graphics.vector.ImageVector

data class MmrMainDestination(
    val route: String,
    val label: String,
    val shortLabel: String = label,
    val icon: ImageVector
)

val MmrMainDestinations: List<MmrMainDestination> = listOf(
    MmrMainDestination(
        route = Screen.Home.route,
        label = "Transactions",
        shortLabel = "Trans.",
        icon = mmrDestinationIcon(Screen.Home.route)
    ),
    MmrMainDestination(
        route = Screen.Stats.route,
        label = "Stats",
        shortLabel = "Stats",
        icon = mmrDestinationIcon(Screen.Stats.route)
    ),
    MmrMainDestination(
        route = Screen.Loans.route,
        label = "Loans",
        shortLabel = "Loans",
        icon = mmrDestinationIcon(Screen.Loans.route)
    ),
    MmrMainDestination(
        route = Screen.Recurring.route,
        label = "Recurring",
        shortLabel = "Recurring",
        icon = mmrDestinationIcon(Screen.Recurring.route)
    ),
    MmrMainDestination(
        route = Screen.Settings.route,
        label = "Settings",
        shortLabel = "Settings",
        icon = mmrDestinationIcon(Screen.Settings.route)
    )
)

fun isMmrMainTabRoute(route: String?): Boolean {
    if (route == null) return false
    return MmrMainDestinations.any { it.route == route }
}
