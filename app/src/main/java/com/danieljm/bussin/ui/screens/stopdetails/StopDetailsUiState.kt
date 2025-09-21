package com.danieljm.bussin.ui.screens.stopdetails

import com.danieljm.bussin.domain.model.Arrival
import com.danieljm.bussin.domain.model.Stop

/**
 * Simple UI state for the Stop Details screen.
 * Keeps only UI-related data (selected stop + loading + error) — no business logic.
 */
data class StopDetailsUiState(
    val isLoading: Boolean = false,
    val selectedStop: Stop? = null,
    val error: String? = null,

    // Final schedule arrivals for the selected stop
    val arrivalsLoading: Boolean = false,
    val arrivals: List<Arrival> = emptyList(),
    val arrivalsError: String? = null,

    // For debugging: raw JSON response from final-schedule endpoint
    val arrivalsRawJson: String? = null,
)
