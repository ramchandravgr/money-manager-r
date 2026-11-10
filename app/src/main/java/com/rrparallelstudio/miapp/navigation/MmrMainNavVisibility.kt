package com.rrparallelstudio.miapp.navigation

/** Stable ids for bottom/rail items (persisted in display settings). */
object MmrMainNavIds {
    const val HOME = "home"
    const val STATS = "stats"
    const val BUDGETS = "budgets"
    const val ASSETS = "assets"
    const val SCHEDULED = "scheduled"
    const val SETTINGS = "settings"
}

/** Child routes that belong to the Ledger tab (bottom bar stays visible; Ledger stays selected). */
private val ledgerOverlayRouteBases =
    setOf(
        Screen.TransactionSearch.route.substringBefore('?'),
        Screen.TransactionDetail.route.substringBefore('/'),
        Screen.AddTransaction.route.substringBefore('?'),
        Screen.ApprovalCenter.route.substringBefore('?'),
    )

fun isLedgerOverlayRoute(route: String?): Boolean {
    val base = route?.substringBefore('?')?.substringBefore('/') ?: return false
    return base in ledgerOverlayRouteBases
}

fun isLedgerNavSelected(
    currentRoute: String?,
    homeDestination: MmrMainDestination,
): Boolean = isMainDestinationSelected(currentRoute, homeDestination) || isLedgerOverlayRoute(currentRoute)

fun mainNavIdForRoute(route: String?): String? {
    val base = route?.substringBefore('?') ?: return null
    return when (base) {
        Screen.Home.route -> MmrMainNavIds.HOME
        Screen.Stats.route -> MmrMainNavIds.STATS
        Screen.Budgets.route -> MmrMainNavIds.BUDGETS
        Screen.Assets.route -> MmrMainNavIds.ASSETS
        Screen.Settings.route -> MmrMainNavIds.SETTINGS
        Screen.Scheduled.route,
        -> MmrMainNavIds.SCHEDULED
        else -> null
    }
}

fun visibleMainDestinations(hiddenNavIds: Set<String>): List<MmrMainDestination> =
    MmrMainDestinations.filter { destination ->
        destination.alwaysVisible || destination.navId !in hiddenNavIds
    }

fun isMainDestinationSelected(
    currentRoute: String?,
    destination: MmrMainDestination,
): Boolean = mainNavIdForRoute(currentRoute) == destination.navId

fun isMainNavRouteVisible(
    route: String?,
    hiddenNavIds: Set<String>,
): Boolean {
    if (isLedgerOverlayRoute(route)) return true
    val navId = mainNavIdForRoute(route) ?: return true
    val destination = MmrMainDestinations.find { it.navId == navId } ?: return true
    if (destination.alwaysVisible) return true
    return navId !in hiddenNavIds
}
