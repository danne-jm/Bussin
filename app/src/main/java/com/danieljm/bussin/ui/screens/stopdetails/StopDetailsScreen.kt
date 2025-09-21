package com.danieljm.bussin.ui.screens.stopdetails

import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.danieljm.bussin.ui.screens.stop.StopDetailScreen

@Composable
fun StopDetailsScreen(
    modifier: Modifier = Modifier,
    stopId: String?,
    stopName: String? = null,
    viewModel: StopDetailsViewModel = androidx.hilt.navigation.compose.hiltViewModel(),
    onBack: () -> Unit = {}
) {
    // Reuse the StopDetailScreen implementation which provides the map + bottom sheet detail UI.
    Log.d("StopDetailsScreen", "Rendering StopDetailsScreen for stopId: $stopId, stopName: $stopName")
    StopDetailScreen(modifier = modifier, stopId = stopId, viewModel = viewModel, mapViewModel = androidx.hilt.navigation.compose.hiltViewModel(), onBack = onBack, stopName = stopName)
}
