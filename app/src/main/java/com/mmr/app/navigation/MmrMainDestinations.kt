package com.mmr.app.navigation

data class MmrMainDestination(
    val route: String,
    val label: String,
    val shortLabel: String = label,
    val icons: MmrNavIconPair,
)

val MmrMainDestinations: List<MmrMainDestination> =
    listOf(
        MmrMainDestination(
            route = Screen.Home.route,
            label = "Transactions",
            shortLabel = "Ledger",
            icons = mmrDestinationIconPair(Screen.Home.route),
        ),
        MmrMainDestination(
            route = Screen.Stats.route,
            label = "Stats",
            shortLabel = "Stats",
            icons = mmrDestinationIconPair(Screen.Stats.route),
        ),
        MmrMainDestination(
            route = Screen.Loans.route,
            label = "Loans",
            shortLabel = "Loans",
            icons = mmrDestinationIconPair(Screen.Loans.route),
        ),
        MmrMainDestination(
            route = Screen.Recurring.route,
            label = "Recurring",
            shortLabel = "Recurring",
            icons = mmrDestinationIconPair(Screen.Recurring.route),
        ),
        MmrMainDestination(
            route = Screen.Settings.route,
            label = "Settings",
            shortLabel = "Settings",
            icons = mmrDestinationIconPair(Screen.Settings.route),
        ),
    )

fun isMmrMainTabRoute(route: String?): Boolean {
    if (route == null) return false
    return MmrMainDestinations.any { it.route == route }
}
