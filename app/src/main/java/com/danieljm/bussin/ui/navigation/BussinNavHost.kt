package com.danieljm.bussin.ui.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.danieljm.bussin.ui.screens.home.HomeScreen
import com.danieljm.bussin.ui.screens.more.MoreScreen
import com.danieljm.bussin.ui.screens.plan.PlanScreen
import com.danieljm.bussin.ui.screens.stop.StopScreen
import com.danieljm.bussin.ui.screens.stopdetails.StopDetailsScreen

@Composable
fun BussinNavHost(
    navController: NavHostController = rememberNavController()
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            // Only show bottom navigation on main screens, not on detail screens
            // Check if current route contains the stop_details pattern
            val isStopDetailsScreen = currentRoute?.contains("stop_details") == true
            if (!isStopDetailsScreen) {
                CustomBottomNavBar(
                    currentRoute = currentRoute,
                    onItemClick = { route ->
                        navController.navigate(route) {
                            // Pop up to start destination and save state
                            popUpTo(navController.graph.startDestinationId) {
                                saveState = true
                            }
                            // Avoid multiple copies of the same destination
                            launchSingleTop = true
                            // Restore state when navigating back to a previously selected item
                            restoreState = true
                        }
                    }
                )
            }
        }
    ) { paddingValues ->
        NavHost(
            navController = navController,
            startDestination = NavRoutes.HOME,
            modifier = Modifier.padding(paddingValues)
        ) {
            composable(NavRoutes.HOME) {
                HomeScreen(
                    onNavigate = { route ->
                        navController.navigate(route)
                    }
                )
            }

            composable(NavRoutes.STOPS) {
                StopScreen(
                    onStopClick = { stopId ->
                        // Navigate to stop details when a stop marker is pressed
                        navController.navigate("${NavRoutes.STOP_DETAILS}/$stopId")
                    }
                )
            }

            composable(NavRoutes.PLAN) {
                PlanScreen(
                    onNavigate = { route ->
                        navController.navigate(route)
                    }
                )
            }

            composable(NavRoutes.MORE) {
                MoreScreen(
                    onNavigate = { route ->
                        navController.navigate(route)
                    }
                )
            }

            composable("${NavRoutes.STOP_DETAILS}/{stopId}") { backStackEntry ->
                val stopId = backStackEntry.arguments?.getString("stopId") ?: ""
                StopDetailsScreen(
                    stopId = stopId,
                    onBack = {
                        navController.popBackStack()
                    }
                )
            }
        }
    }
}
