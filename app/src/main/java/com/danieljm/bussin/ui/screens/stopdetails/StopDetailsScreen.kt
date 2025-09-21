package com.danieljm.bussin.ui.screens.stopdetails

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun StopDetailsScreen(
    modifier: Modifier = Modifier,
    stopId: String?,
    viewModel: StopDetailsViewModel = hiltViewModel(),
    onBack: () -> Unit = {}
) {
    val ui by viewModel.uiState.collectAsState()

    LaunchedEffect(stopId) {
        if (!stopId.isNullOrEmpty()) {
            viewModel.loadStopDetails(stopId)
        }
    }

    Column(
        modifier = modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        when {
            ui.isLoading -> CircularProgressIndicator()
            ui.error != null -> Text(text = "Error: ${ui.error}")
            ui.selectedStop != null -> {
                val s = ui.selectedStop
                val idStr = s?.id ?: "unknown"
                val nameStr = s?.name ?: "-"
                val latStr = s?.latitude?.toString() ?: "-"
                val lonStr = s?.longitude?.toString() ?: "-"

                Text(text = "Stop: $idStr")
                Text(text = "Name: $nameStr")
                Text(text = "Lat: $latStr Lon: $lonStr")
                Button(onClick = onBack, modifier = Modifier.padding(top = 12.dp)) {
                    Text("Back")
                }
            }
            else -> Text(text = "No stop selected")
        }
    }
}
