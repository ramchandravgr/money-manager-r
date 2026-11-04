package com.mmr.app.navigation

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
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val visibleDestinations = visibleMainDestinations(hiddenMainNavIds)
    val showMainNav = isMmrMainTabRoute(currentRoute) && isMainNavRouteVisible(currentRoute, hiddenMainNavIds)

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

    val onMainDestinationSelected: (String) -> Unit = { route ->
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
