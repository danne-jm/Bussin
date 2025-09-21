package com.danieljm.bussin.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.ui.graphics.vector.ImageVector

object NavRoutes {
    const val HOME = "home"
    const val STOPS = "stops"
    const val PLAN = "plan"
    const val MORE = "more"
    const val STOP_DETAILS = "stop_details"
}

data class BottomNavItem(
    val route: String,
    val title: String,
    val icon: ImageVector
)

val bottomNavItems = listOf(
    BottomNavItem(
        route = NavRoutes.HOME,
        title = "Home",
        icon = Icons.Default.Home
    ),
    BottomNavItem(
        route = NavRoutes.STOPS,
        title = "Stops",
        icon = Icons.Default.LocationOn
    ),
    BottomNavItem(
        route = NavRoutes.PLAN,
        title = "Plan",
        icon = Icons.Default.Search
    ),
    BottomNavItem(
        route = NavRoutes.MORE,
        title = "More",
        icon = Icons.Default.MoreVert
    )
)
