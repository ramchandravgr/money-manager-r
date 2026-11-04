package com.mmr.app.navigation

data class MmrMainDestination(
    val navId: String,
    val route: String,
    val label: String,
    val shortLabel: String = label,
    val icons: MmrNavIconPair,
    /** When true, user cannot hide this tab (Transactions, Settings). */
    val alwaysVisible: Boolean = false,
)

val MmrMainDestinations: List<MmrMainDestination> =
    listOf(
        MmrMainDestination(
            navId = MmrMainNavIds.HOME,
            route = Screen.Home.route,
            label = "Transactions",
            shortLabel = "Ledger",
            icons = mmrDestinationIconPair(Screen.Home.route),
            alwaysVisible = true,
        ),
        MmrMainDestination(
            navId = MmrMainNavIds.STATS,
            route = Screen.Stats.route,
            label = "Stats",
            shortLabel = "Stats",
            icons = mmrDestinationIconPair(Screen.Stats.route),
        ),
        MmrMainDestination(
            navId = MmrMainNavIds.BUDGETS,
            route = Screen.Budgets.route,
            label = "Budgets",
            shortLabel = "Budgets",
            icons = mmrDestinationIconPair(Screen.Budgets.route),
        ),
        MmrMainDestination(
            navId = MmrMainNavIds.ASSETS,
            route = Screen.Assets.route,
            label = "Assets",
            shortLabel = "Assets",
            icons = mmrDestinationIconPair(Screen.Assets.route),
        ),
        MmrMainDestination(
            navId = MmrMainNavIds.SCHEDULED,
            route = Screen.Scheduled.createRoute("loans"),
            label = "Scheduled",
            shortLabel = "Scheduled",
            icons = mmrDestinationIconPair(Screen.Scheduled.route),
        ),
        MmrMainDestination(
            navId = MmrMainNavIds.SETTINGS,
            route = Screen.Settings.route,
            label = "Settings",
            shortLabel = "Settings",
            icons = mmrDestinationIconPair(Screen.Settings.route),
            alwaysVisible = true,
        ),
    )

/** Destinations the user may show or hide in Configuration. */
val MmrToggleableMainDestinations: List<MmrMainDestination> =
    MmrMainDestinations.filter { !it.alwaysVisible }

fun isMmrMainTabRoute(route: String?): Boolean {
    if (route == null) return false
    return mainNavIdForRoute(route) != null
}
