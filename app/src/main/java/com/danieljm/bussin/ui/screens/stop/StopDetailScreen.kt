package com.danieljm.bussin.ui.screens.stop

import android.Manifest
import android.content.pm.PackageManager
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.danieljm.bussin.domain.model.Stop
import com.danieljm.bussin.ui.components.map.MapViewModel
import com.danieljm.bussin.ui.components.map.OpenStreetMap
import com.danieljm.bussin.ui.screens.stopdetails.StopDetailsViewModel
import com.danieljm.bussin.ui.theme.TransparentSystemBars
import com.danieljm.delijn.ui.components.stops.BottomSheet

/**
 * Stop detail screen which shows the same full-screen map as `StopScreen` but without the FAB.
 * The bottom sheet header shows the stop name and id; body content is intentionally empty.
 * This composable accepts a stopId and optionally a stopName (passed from navigation).
 */
@Composable
fun StopDetailScreen(
    modifier: Modifier = Modifier,
    stopId: String?,
    stopName: String? = null,
    viewModel: StopDetailsViewModel = hiltViewModel(),
    mapViewModel: MapViewModel = hiltViewModel(),
    onBack: () -> Unit = {}
) {
    TransparentSystemBars()

    // Load stop details when stopId changes
    LaunchedEffect(stopId) {
        if (!stopId.isNullOrEmpty()) {
            viewModel.loadStopDetails(stopId)
        }
    }

    val ui by viewModel.uiState.collectAsState()

    val ctx = LocalContext.current

    // Start location updates only when permission is granted so the map can show user location.
    var hasLocationPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                ctx,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    LaunchedEffect(hasLocationPermission) {
        if (hasLocationPermission) {
            mapViewModel.startLocationUpdates(ctx)
        }
    }

    DisposableEffect(Unit) {
        onDispose { mapViewModel.stopLocationUpdates() }
    }

    val userLocation by mapViewModel.location.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val listState = rememberLazyListState()

    // The stop to display on the map / header
    val selectedStop: Stop? = ui.selectedStop

    // pendingCenterStop is used to request a one-time center-on-stop from the map.
    // OpenStreetMap will call onCenterHandled when it has animated to the stop, at
    // which point we clear this state so the map won't keep forcing the center.
    var pendingCenterStop by remember { mutableStateOf<Stop?>(selectedStop) }

    // Update the pending center when the selected stop changes (e.g. initial load or navigation)
    LaunchedEffect(selectedStop?.id) {
        // Only set if we actually have a stop to center on
        if (selectedStop != null) pendingCenterStop = selectedStop
    }

    Box(modifier = modifier.fillMaxSize()) {
        OpenStreetMap(
            modifier = Modifier.fillMaxSize(),
            userLocation = userLocation,
            stops = if (selectedStop != null) listOf(selectedStop) else emptyList(),
            onStopClick = { /* no-op in detail screen */ },
            recenterTrigger = 0,
            onMapCenterChanged = { _, _ -> /* no-op */ },
            onVisibleStopIdsChanged = { /* no-op */ },
            // Pass the one-time pending center request instead of always passing selectedStop.
            centerOnStop = pendingCenterStop,
            // When the map handled the center request, clear it so we don't re-center again.
            onCenterHandled = { pendingCenterStop = null },
        )

        BottomSheet(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .align(Alignment.BottomCenter),
            stops = if (selectedStop != null) listOf(selectedStop) else emptyList(),
            userLat = userLocation?.latitude,
            userLon = userLocation?.longitude,
            onStopClick = { /* no-op */ },
            onRefresh = null,
            isLoading = ui.isLoading,
            shouldAnimateRefresh = false,
            onRefreshAnimationComplete = {},
            listState = listState,
            scrollToStopId = selectedStop?.id,
            onScrollHandled = null,
            headerContent = { _rotation, _isRefreshing, _onRefresh ->
                // Prefer the navigation-provided name (stopName), then the loaded stop name, then a hint
                val headerName = stopName ?: selectedStop?.name ?: "Loading..."
                val headerId = selectedStop?.id ?: stopId ?: "-"

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // Stop name - centered
                    Text(
                        text = headerName,
                        fontSize = 20.sp,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )
                    Spacer(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(
                                4.dp
                            )
                    )
                    Text(
                        text = "ID: $headerId",
                        color = Color.White.copy(0.8f),
                        fontSize = 14.sp,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Start
                    )
                }
            },
            bodyContent = { _listState, _bottomContentPadding ->
                // Intentionally empty body for detail screen. The sheet header communicates identity.
            },
            expanded = false,
            highlightedStopId = selectedStop?.id
        )

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .windowInsetsPadding(WindowInsets.safeDrawing)
        )
    }
}
