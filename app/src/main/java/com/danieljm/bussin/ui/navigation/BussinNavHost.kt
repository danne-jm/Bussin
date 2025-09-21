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
import java.net.URLDecoder

@Composable
fun BussinNavHost(
    navController: NavHostController = rememberNavController()
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            // Show bottom navigation on main app screens and also on stop details so the detail
            // screen keeps the same bottom nav as the Stops screen and the sheet appears above it.
            val isMainScreen = currentRoute == NavRoutes.HOME || currentRoute == NavRoutes.STOPS || currentRoute == NavRoutes.PLAN || currentRoute == NavRoutes.MORE || (currentRoute?.contains(NavRoutes.STOP_DETAILS) == true)
            if (isMainScreen) {
                // For stop details screen, treat it as if we're on the stops screen for navigation highlighting
                val effectiveRoute = if (currentRoute?.contains(NavRoutes.STOP_DETAILS) == true) {
                    NavRoutes.STOPS
                } else {
                    currentRoute
                }

                CustomBottomNavBar(
                    currentRoute = effectiveRoute,
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
                    onStopClick = { stopId, _stopName ->
                        // Navigate to stop details when a stop marker is pressed, pass both id and encoded name
                        val encodedName = java.net.URLEncoder.encode(_stopName ?: "", "UTF-8")
                        navController.navigate("${NavRoutes.STOP_DETAILS}/$stopId/$encodedName")
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

            composable("${NavRoutes.STOP_DETAILS}/{stopId}/{stopName}") { backStackEntry ->
                val stopId = backStackEntry.arguments?.getString("stopId") ?: ""
                val rawName = backStackEntry.arguments?.getString("stopName") ?: ""
                val stopName = try { URLDecoder.decode(rawName, "UTF-8") } catch (_: Throwable) { rawName }
                StopDetailsScreen(
                    stopId = stopId,
                    onBack = {
                        navController.popBackStack()
                    },
                    stopName = stopName
                )
            }
        }
    }
}
