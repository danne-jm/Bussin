package com.danieljm.bussin.ui.screens.stop

import android.Manifest
import android.content.pm.PackageManager
import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Icon
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.RefreshCw
import com.danieljm.bussin.domain.model.Stop
import com.danieljm.bussin.ui.components.map.MapViewModel
import com.danieljm.bussin.ui.components.map.OpenStreetMap
import com.danieljm.bussin.ui.components.stopdetails.BusCard
import com.danieljm.bussin.ui.components.stopdetails.LineBadge
import com.danieljm.bussin.ui.screens.stopdetails.StopDetailsViewModel
import com.danieljm.delijn.ui.components.stops.BottomSheet
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Stop detail screen which shows the same full-screen map as `StopScreen` but without the FAB.
 * The bottom sheet header shows the stop name and id; body content is intentionally empty.
 * This composable accepts a stopId and optionally a stopName (passed from navigation).
 */
@Suppress("UNUSED_PARAMETER", "UNUSED_VARIABLE")
@Composable
fun StopDetailScreen(
    modifier: Modifier = Modifier,
    stopId: String?,
    stopName: String? = null,
    viewModel: StopDetailsViewModel = hiltViewModel(),
    mapViewModel: MapViewModel = hiltViewModel(),
    stopViewModel: StopViewModel = hiltViewModel(),
    onBack: () -> Unit = {}
) {
    // TransparentSystemBars() removed — system bar handling is centralized in BussinNavHost

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
    val coroutineScope = rememberCoroutineScope()
    // Trigger for BottomSheet refresh animation; used to enable/animate the refresh icon
    val refreshAnimRequested = remember { mutableStateOf(false) }
    val minAnimationDurationMs = 1000L // Minimum 1 second animation
    val animationStartTime = remember { mutableStateOf(0L) }
    // Keep last map-center triggered fetch time to avoid spamming the server when user pans repeatedly
    val lastCenterFetchMs = remember { mutableStateOf(0L) }
    // Keep last cached-fetch time to rate-limit cache reads and UI churn
    val lastCenterCachedMs = remember { mutableStateOf(0L) }
    // For scheduling at-most-one pending network fetch when panning rapidly
    val pendingNetworkFetchTargetMs = remember { mutableStateOf(0L) }

    // The stop to display on the map / header
    val selectedStop: Stop? = ui.selectedStop

    // Local immediate highlight id so tapping another stop provides instant visual feedback
    var immediateHighlightedId by remember { mutableStateOf(selectedStop?.id) }

    // Keep immediateHighlightedId in sync when the authoritative selectedStop changes
    LaunchedEffect(selectedStop?.id) {
        immediateHighlightedId = selectedStop?.id
    }

    // Immediate header name to show optimistically when the user taps a stop (cleared when details load)
    var immediateHeaderName by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(selectedStop?.id) {
        // clear optimistic header when the authoritative selectedStop changes
        immediateHeaderName = null
    }

    // Track timestamp (millis) of the last executed arrivals refresh. Manual taps and
    // network-driven refreshes should update this to avoid duplicate refreshes. If 20s
    // elapse since the last execution we will auto-trigger a refresh and reset this.
    val lastRefreshExecutedMs = remember { mutableStateOf(0L) }

    // Observe nearby stops from StopViewModel so we can render them on the map.
    // Declare this before the auto-refresh LaunchedEffect so the ticker can check loader state.
    val stopUi by stopViewModel.uiState.collectAsState()

    // Auto-refresh loop: when the selected stop changes, reset the last-executed timestamp
    // and run a ticker that will trigger a refresh once 20s have elapsed since the last
    // execution. Manual refreshes must update `lastRefreshExecutedMs` to reset the timer.
    LaunchedEffect(selectedStop?.id) {
        lastRefreshExecutedMs.value = System.currentTimeMillis()
        while (true) {
            val now = System.currentTimeMillis()
            val elapsed = now - lastRefreshExecutedMs.value
            val interval = 20_000L
            if (elapsed >= interval) {
                // Only auto-trigger when arrivals are not currently loading to avoid overlapping requests.
                if (!ui.arrivalsLoading) {
                    try {
                        viewModel.refreshArrivals()
                        refreshAnimRequested.value = true
                    } catch (_: Throwable) {}
                    // update last executed time to now so the next auto-refresh waits again
                    lastRefreshExecutedMs.value = System.currentTimeMillis()
                }
            }
            // Sleep a short interval so we can react shortly after 20s elapses or when timestamp resets
            delay(1000L)
        }
    }

    // When nearby stop loading or stop details loading starts, show the refresh animation; when both
    // loaders are idle, stop the animation. This ensures animation runs while data is being fetched.
    LaunchedEffect(ui.arrivalsLoading) {
        try {
            if (ui.arrivalsLoading) {
                refreshAnimRequested.value = true
                animationStartTime.value = System.currentTimeMillis()
            }
        } catch (_: Throwable) { }
    }

    // Control refresh animation stopping: only stop after loading is complete AND minimum animation duration has passed.
    LaunchedEffect(ui.arrivalsLoading, animationStartTime.value) {
        if (!ui.arrivalsLoading && refreshAnimRequested.value) {
            val elapsed = System.currentTimeMillis() - animationStartTime.value
            val remainingDelay = (minAnimationDurationMs - elapsed).coerceAtLeast(0L)
            if (remainingDelay > 0) {
                delay(remainingDelay)
            }
            refreshAnimRequested.value = false
        }
    }

    // When the selected stop is loaded, request nearby stops centered on it so the map
    // can render other nearby markers. We only trigger this when the selectedStop id or coords change.
    LaunchedEffect(selectedStop?.id, selectedStop?.latitude, selectedStop?.longitude) {
        val lat = selectedStop?.latitude
        val lon = selectedStop?.longitude
        if (lat != null && lon != null) {
            try {
                stopViewModel.loadNearbyStops(stop = "", lat = lat, lon = lon)
                lastRefreshExecutedMs.value = System.currentTimeMillis()
            } catch (_: Throwable) {}
        }
    }

    // Build the combined stops list for the map: include the selected stop (highlighted) and
    // any nearby stops from StopViewModel (deduplicated, using the selected stop as the highlight).
    val mapStops = remember(selectedStop, stopUi.stops) {
        val out = mutableListOf<Stop>()
        selectedStop?.let { out.add(it) }
        for (s in stopUi.stops) {
            if (s.id != selectedStop?.id) out.add(s)
        }
        out
    }

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
            stops = mapStops,
            onStopClick = { stop ->
                // If user taps a different stop while on the detail screen, load its details
                // and request a one-time center on it. Also kick off nearby stops load immediately.
                try {
                    // immediate visual feedback: highlight tapped stop
                    try { immediateHighlightedId = stop.id } catch (_: Throwable) {}
                    // set optimistic header name so UI updates immediately
                    try { immediateHeaderName = stop.name } catch (_: Throwable) {}
                    // Load the new stop details into the StopDetailsViewModel
                    try { viewModel.loadStopDetails(stop.id) } catch (_: Throwable) {}

                    // Request the map to center on the tapped stop
                    pendingCenterStop = stop

                    // Immediately request nearby stops for the tapped location to refresh markers
                    val lat = stop.latitude
                    val lon = stop.longitude
                    if (lat != null && lon != null) {
                        try { stopViewModel.loadNearbyStops(stop = "", lat = lat, lon = lon) } catch (_: Throwable) {}
                        // reset the auto-refresh timer because user manually changed focus
                        lastRefreshExecutedMs.value = System.currentTimeMillis()
                    }
                } catch (_: Throwable) {}
            },
             recenterTrigger = 0,
             onMapCenterChanged = { lat, lon ->
                val now = System.currentTimeMillis()

                val cachedCooldownMs = 500L
                val networkCooldownMs = 2_000L

                // 1) Quick cached display (rate-limited to `cachedCooldownMs`)
                if (now - lastCenterCachedMs.value > cachedCooldownMs) {
                    lastCenterCachedMs.value = now
                    try {
                        stopViewModel.loadCachedNearbyStops(lat, lon)
                    } catch (_: Throwable) {}
                }

                // 2) Network fetch: if allowed now, run immediately; otherwise schedule one after cooldown.
                val sinceLastNetwork = now - lastCenterFetchMs.value
                if (sinceLastNetwork > networkCooldownMs) {
                    lastCenterFetchMs.value = now
                    try {
                        stopViewModel.loadNearbyStops(stop = "", lat = lat, lon = lon)
                        lastRefreshExecutedMs.value = System.currentTimeMillis()
                    } catch (_: Throwable) {}
                } else {
                    // schedule one fetch at time targetMs (only keep latest)
                    val delayMs = networkCooldownMs - sinceLastNetwork
                    val targetMs = now + delayMs
                    pendingNetworkFetchTargetMs.value = targetMs
                    coroutineScope.launch {
                        delay(delayMs)
                        // only run if no newer scheduled fetch replaced this
                        if (pendingNetworkFetchTargetMs.value == targetMs) {
                            lastCenterFetchMs.value = System.currentTimeMillis()
                            try {
                                stopViewModel.loadNearbyStops(stop = "", lat = lat, lon = lon)
                                lastRefreshExecutedMs.value = System.currentTimeMillis()
                            } catch (_: Throwable) {}
                            // clear pending marker
                            pendingNetworkFetchTargetMs.value = 0L
                        }
                     }
                 }
             },
            onVisibleStopIdsChanged = { /* no-op */ },
              // Pass the one-time pending center request instead of always passing selectedStop.
              centerOnStop = pendingCenterStop,
             // When the map handled the center request, clear it so we don't re-center again.
             onCenterHandled = { pendingCenterStop = null },
            // Highlight the selected stop so it renders with the focused drawable; others will use unfocused drawable.
            highlightedStopId = immediateHighlightedId,
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
            onRefresh = {
                // Trigger a nearby stops reload centered on the selected stop (or fallback).
                // Force=true so manual taps always initiate a network request regardless of throttle.
                viewModel.refreshArrivals()
                // manual refresh should always force a refresh, so update the last-executed timestamp
                lastRefreshExecutedMs.value = System.currentTimeMillis()
            },
             isLoading = ui.arrivalsLoading,
            shouldAnimateRefresh = refreshAnimRequested.value,
            onRefreshAnimationComplete = { /* Handled by external LaunchedEffect */ },
             listState = listState,
            scrollToStopId = selectedStop?.id,
            onScrollHandled = null,
            headerContent = { _rotation, _isRefreshing, _onRefresh ->
                // Prefer authoritative selectedStop.name, then optimistic immediateHeaderName, then nav-provided stopName
                val headerName = selectedStop?.name ?: immediateHeaderName ?: stopName ?: "Loading..."
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
                    Row() {
                        Text(
                            text = "ID: $headerId",
                            color = Color.White.copy(0.8f),
                            fontSize = 14.sp,
                            modifier = Modifier.weight(1f),
                            textAlign = TextAlign.Start
                        )
                        Icon(
                            imageVector = Lucide.RefreshCw,
                            contentDescription = "Refresh",
                            tint = Color.White.copy(alpha = 0.8f),
                            modifier = Modifier
                                .size(28.dp)
                                .padding(start = 8.dp)
                                .graphicsLayer { rotationZ = _rotation }
                                .clickable(enabled = _onRefresh != null) {
                                    // manual tap should force a refetch
                                    _onRefresh?.invoke()
                                    // reset the auto-refresh timer when user manually refreshes
                                    lastRefreshExecutedMs.value = System.currentTimeMillis()
                                }
                        )
                    }

                    // Show a row of unique line badges (same visuals as in BusCard) below the ID/refresh row
                    val uniqueArrivals = ui.arrivals.distinctBy { it.lijnNummerPubliek ?: it.lijnnummer ?: "-" }
                    if (uniqueArrivals.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(uniqueArrivals) { arrivalItem ->
                                LineBadge(arrival = arrivalItem)
                            }
                        }
                    }
                }
            },
            bodyContent = { _listState, _bottomContentPadding ->
                // Debug log to help trace UI updates
                LaunchedEffect(ui.arrivals) {
                    Log.d("StopDetailScreen", "UI arrivals count=${ui.arrivals.size}, loading=${ui.arrivalsLoading}, error=${ui.arrivalsError}")
                }

                when {
                    ui.arrivalsLoading -> {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = "Loading arrivals...", color = Color.White.copy(alpha = 0.8f))
                        }
                    }
                    ui.arrivalsError != null -> {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = "Error: ${ui.arrivalsError}", color = Color.White.copy(alpha = 0.8f))
                        }
                    }
                    ui.arrivals.isEmpty() -> {
                        // If the final schedule explicitly had no halteDoorkomsten, show friendly drawable
                        // Ensure the image isn't scaled when the sheet collapses: put it inside a LazyColumn
                        // item with fixed height so the sheet's viewport will clip it out of view instead
                        // of compressing it.
                        if (ui.noHalteDoorkomsten) {
                            LazyColumn(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.Center,
                                state = _listState,
                                contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = _bottomContentPadding)
                            ) {
                                item {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(120.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Image(
                                            painter = painterResource(id = com.danieljm.bussin.R.drawable.sleeping_bus),
                                            contentDescription = "No arrivals",
                                            modifier = Modifier.size(180.dp)
                                        )
                                    }
                                }
                            }
                        } else {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                // If no arrivals, show raw JSON debug data when available
                                val raw = ui.arrivalsRawJson
                                if (!raw.isNullOrEmpty()) {
                                    Text(text = raw, color = Color.White.copy(alpha = 0.7f))
                                } else {
                                    Text(text = "No upcoming arrivals for today.", color = Color.White.copy(alpha = 0.7f))
                                }
                            }
                        }
                    }
                    else -> {
                        LazyColumn(
                            state = _listState,
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = _bottomContentPadding)
                        ) {
                            items(ui.arrivals, key = { it.doorkomstId ?: (it.ritnummer ?: it.vrtnum ?: it.lijnnummer ?: "-") }) { arrival ->
                                BusCard(arrival = arrival)
                            }
                        }
                    }
                }
            },
            // In the stop detail screen we want the sheet expanded so arrivals are visible immediately.
            expanded = false,
            // Track sheet height so callers can react (we'll hide large imagery when small) - currently unused
            onHeightChanged = {}
         )

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .windowInsetsPadding(WindowInsets.safeDrawing)
        )
    }
}