package com.mmr.app.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Autorenew
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.AccountBalance
import androidx.compose.material.icons.outlined.Autorenew
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.ui.graphics.vector.ImageVector

/** Outline when idle, filled when selected — finance-app tab bar style. */
data class MmrNavIconPair(
    val outlined: ImageVector,
    val filled: ImageVector,
)

internal fun mmrDestinationIconPair(route: String): MmrNavIconPair =
    when (route) {
        Screen.Home.route ->
            MmrNavIconPair(
                outlined = Icons.Outlined.Description,
                filled = Icons.Filled.Description,
            )
        Screen.Stats.route ->
            MmrNavIconPair(
                outlined = Icons.Outlined.BarChart,
                filled = Icons.Filled.BarChart,
            )
        Screen.Loans.route ->
            MmrNavIconPair(
                outlined = Icons.Outlined.AccountBalance,
                filled = Icons.Filled.AccountBalance,
            )
        Screen.Recurring.route ->
            MmrNavIconPair(
                outlined = Icons.Outlined.Autorenew,
                filled = Icons.Filled.Autorenew,
            )
        Screen.Settings.route ->
            MmrNavIconPair(
                outlined = Icons.Outlined.Settings,
                filled = Icons.Filled.Settings,
            )
        else ->
            MmrNavIconPair(
                outlined = Icons.Outlined.Settings,
                filled = Icons.Filled.Settings,
            )
    }
