package com.rrparallelstudio.miapp.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState

@Composable
fun MmrNavHost(
    navController: NavHostController,
    hiddenMainNavIds: Set<String> = emptySet(),
    homeLedgerTabReselectNotifier: HomeLedgerTabReselectNotifier,
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val visibleDestinations = visibleMainDestinations(hiddenMainNavIds)
    val showMainNav =
        (isMmrMainTabRoute(currentRoute) || isLedgerOverlayRoute(currentRoute)) &&
            isMainNavRouteVisible(currentRoute, hiddenMainNavIds)

    LaunchedEffect(currentRoute, hiddenMainNavIds) {
        if (!isMainNavRouteVisible(currentRoute, hiddenMainNavIds)) {
            navController.navigate(Screen.Home.route) {
                popUpTo(navController.graph.findStartDestination().id) {
                    saveState = true
                }
                launchSingleTop = true
                restoreState = true
            }
        }
    }

    val homeDestination =
        visibleDestinations.firstOrNull { it.navId == MmrMainNavIds.HOME }
    val onMainDestinationSelected: (String) -> Unit = { route ->
        val reselectingLedger =
            route == Screen.Home.route &&
                homeDestination != null &&
                isLedgerNavSelected(currentRoute, homeDestination)
        if (reselectingLedger) {
            homeLedgerTabReselectNotifier.notifyLedgerTabReselected()
            if (isLedgerOverlayRoute(currentRoute)) {
                navController.popBackStack(Screen.Home.route, inclusive = false)
            }
        }
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    if (showMainNav) {
        MmrMainNavigationScaffold(
            destinations = visibleDestinations,
            currentRoute = currentRoute,
            onDestinationSelected = onMainDestinationSelected,
        ) { contentModifier ->
            MmrNavGraph(navController = navController, modifier = contentModifier)
        }
    } else {
        MmrSecondaryScaffold { contentModifier ->
            MmrNavGraph(navController = navController, modifier = contentModifier)
        }
    }
}
